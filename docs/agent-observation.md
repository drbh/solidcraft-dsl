# Agent observation requests

Build once, inspect as needed. `evaluate` compiles `.sc` and runs the engine. `observe` opens a
saved evaluation's native design (or a `.solidcraft` file directly) and runs the same request
system without reading `.sc`. Both produce a machine-readable report and files. Neither edits
the author's source. The engine may recompute geometry when opening a saved design.

```sh
solidcraft-dsl capabilities --json > capabilities.json
solidcraft-dsl evaluate part.sc --output-dir build/part --requests requests.json --json
solidcraft-dsl observe build/part/last-success.json --output-dir build/observations \
  --requests more-evidence.json --json
```

`capabilities` returns a generated JSON Schema, expanded defaults, and command descriptions
queried from the installed engine. Discover parameters there instead of relying on a fixed
catalogue in the DSL. For example, filter `engine_commands` for `model.faces`, `inspect.measure`,
`inspect.section`, `view.apply` or `view.navigate`. Capabilities can differ by engine version.

## Plan and results

```json
{
  "version": 1,
  "defaults": false,
  "requests": [
    {"id": "back", "kind": "snapshot", "options": {"view": "back"}},
    {"id": "mesh", "kind": "export", "format": "glb"}
  ]
}
```

`version` defaults to 1. `defaults` defaults to true: four 1000×800 snapshots (`iso`, `front`,
`top`, `right`) and a labelled 2×2 sheet (`overview`). A custom request with the same id replaces
that default request. Each request is independent: overriding `iso` does not also change the
panels inside `overview`; override the sheet to change its panels. `--no-previews` omits default
requests, but explicit requests always execute. Use `--requests -` to supply JSON on stdin.

Request ids must be unique, 1–80 ASCII letters, numbers, underscores or hyphens. `source`,
`design` and `request_results` are reserved. Unknown request/options fields are rejected so
misspellings cannot silently discard a camera or export option. Native engine parameters
remain open-ended and are validated by the engine.

Every request defaults to `required: true`. A required failure makes the report's `ok` false
and the command exit nonzero; `required: false` records a warning instead. Requests continue
after another request fails. A failed request has no advertised artifact path. A malformed
plan is rejected as a whole. Evaluation still retains the last successful review on failure.

`artifacts[id]` contains each successful request's primary output path. `request_results` is an
ordered array containing the effective request, status, output path, metadata and request
directory (or an error). Native command results are available in metadata and `results.json`.
Sheets include panel rectangles, individual image paths, labels and camera options. Each
request directory retains the request, its private input, invocation arguments, stdout/stderr,
and supporting files, including sidecar files an exporter creates. Evaluation's numeric checks
can address these results too, for example `/request_results/0/metadata/results/0/body_count`.

Observations write `latest-observation.json` in their output directory; they never move
`latest.json` or `last-success.json`. Reports refer to the exact saved input and carry its hash;
when starting from an evaluation report, they also carry the originating source hash.

## Snapshots and cameras

```json
{
  "id": "hole-detail",
  "kind": "snapshot",
  "options": {
    "camera": {"target": [45, 10, 6], "yaw": 0.7, "pitch": 0.9, "distance": 28},
    "width": 800,
    "height": 800,
    "grid": false,
    "axes": false
  }
}
```

Choose either a `view` (standard or saved view name) or an engine-native `camera`. Camera target
and distance are in millimetres; yaw, pitch and optional fov are in radians. Additional camera
fields supported by the installed engine pass through unchanged. Omitting both uses the
engine's default camera or a camera set by preceding commands. Dimensions default to 1000×800
and must be 16–8192 pixels. Grid and axes default to false.

A `commands` array inside `options` executes before capture. This provides sections,
appearances and other engine-native display controls without duplicating them in the DSL:

```json
{
  "id": "section",
  "kind": "snapshot",
  "options": {
    "view": "iso",
    "commands": [
      {"command": "inspect.section", "params": {"plane": "XZ", "at": 10}}
    ]
  }
}
```

For an orbit/zoom/pan loop, first run a `commands` request containing `view.apply` with a named
view. Its result supplies a fitted camera. Pass that camera to `view.navigate` with the desired
motion (discover its schema through `capabilities`), then use the returned camera in a new
snapshot. There is deliberately no implicit mutable camera session or dependency between
requests. An agent chooses its next observation from explicit prior results. For comparisons
at identical scale, reuse an explicit camera rather than auto-fitting a named view.

## Multi-view images

```json
{
  "id": "review",
  "kind": "sheet",
  "columns": 2,
  "views": [
    {"view": "iso", "width": 600, "height": 480, "label": "Isometric"},
    {"view": "top", "width": 600, "height": 480, "label": "Top"},
    {"view": "front", "width": 600, "height": 480, "label": "Front"},
    {"view": "right", "width": 600, "height": 480, "label": "Right"}
  ]
}
```

Each panel accepts all snapshot options, including its own camera and pre-capture commands.
Panels are arranged in row order, with a 24-pixel caption above each. Cells use the largest
panel dimensions without rescaling the images. Captions default to the view name or panel
number; the compact embedded font displays ASCII, with a fallback for unsupported characters.
Full labels remain in metadata. Columns default to 2. Sheets are limited to 64 million pixels;
split larger requests into several sheets. Each panel opens a fresh copy of the saved design.

## Exports

```json
{"id": "cad", "kind": "export", "format": "step", "params": {"bodies": ["Bracket"]}}
```

`format` is forwarded to `file.export`, and `params` can contain any engine export options
except the managed `path` and `format`. A preceding `commands` array can prepare an experimental
export in that request's private session. Formats such as STEP, STL, OBJ, 3MF, JSON and URDF are
engine-dependent; unsupported formats fail that request. Exporters that create sidecars place
them in the request directory alongside the primary artifact.

`format: "glb"` is provided by this tool: it exports binary STL through the engine and encodes
that tessellation as GLB 2.0. It is a single geometry-only mesh with flat normals, without CAD
features, materials, body names or assembly hierarchy. It converts CAD coordinates to
`[x,z,-y]/1000`, so the output uses metres and Y up as specified by
[glTF 2.0](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html#coordinate-system-and-units).
Its precision and curved surfaces are limited by the engine's STL tessellation. Keep the native
`.solidcraft` or STEP for CAD work.

## Native commands and direct CLI access

```json
{
  "id": "topology",
  "kind": "commands",
  "commands": [
    {"command": "model.faces", "params": {"body": "Bracket"}},
    {"command": "model.edges", "params": {"body": "Bracket"}},
    {"command": "sketch.inspect", "params": {"sketch": "Profile"}}
  ]
}
```

Commands within one request execute in order in one engine session. Their complete results are
retained as JSON. Use this for topology, detailed sketch freedom, selection measurements or any
new engine query. Commands can also write an artifact directly:

```json
{
  "id": "profile-drawing",
  "kind": "commands",
  "extension": "dxf",
  "commands": [
    {"command": "sketch.export_dxf", "params": {"sketch": "Profile", "path": "$output"}}
  ]
}
```

When a feature is exposed only through the engine CLI, use `kind: "cli"`:

```json
{
  "id": "render",
  "kind": "cli",
  "required": false,
  "extension": "png",
  "args": ["render", "$design", "--out", "$output", "--width", "1200", "--height", "800", "--timeout", "60"]
}
```

The engine executable is selected by `SOLIDCRAFT_CLI`. CLI arguments are passed as an argument
array, without shell interpretation. Render setup requirements and supported flags belong to
the installed engine. This escape hatch also supports future export and inspection features.

For native command parameters and CLI argument strings, these exact strings are substituted:

| Placeholder | Value |
|---|---|
| `$design` | This request's private native design copy |
| `$dir` | Absolute request output directory |
| `$output` | Managed `output.<extension>` path; requires `extension` for commands/CLI requests |

Substitution is recursive in command JSON, but applies to complete strings only; `$dir/file`
and placeholders embedded inside a JSON-encoded CLI argument are not interpolated. The working
directory is the request directory, so additional relative output paths naturally land there.
If `extension` is absent, the primary artifact is the captured JSON result. If present, success
requires the requested file to exist. File extensions must use the same filename-safe character
set as request ids, without a leading dot.

Private copies isolate ordinary model and view changes; this is not a filesystem sandbox.
Native commands retain their normal filesystem capabilities, including explicit absolute paths.
Treat request files as executable engine instructions.

Standard review views hide construction geometry. Sketch geometry remains visible so sketch-only
designs still have useful previews. [Solid-only review requests](solid-review-requests.json) hide
sketches too; custom snapshots control visibility through their own commands.

## Reusable layouts

Requests can use a named or inline `layout` to prepare selected solids before exporting,
rendering or inspecting them. See [print layouts](print-layouts.md) for measured placement,
parameter expressions, instance selection and geometry-transfer limits.
