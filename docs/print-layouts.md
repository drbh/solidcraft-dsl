# Reusable export and review layouts

A `.sc` file describes a design. A request recipe describes the outputs you want from that
design, including optional manufacturing layouts. Keep authored `*.print.json` recipes beside
the examples; put generated scripts, reports, meshes and images in the evaluation output folder.

A layout selects evaluated bodies, or a named instance and its descendants, and places them in
an independent document. It never moves the original assembly, breaks its joints, or edits a
shared component definition. The same layout can feed an export, snapshot, sheet, or inspection.

```json
{
  "version": 1,
  "defaults": false,
  "layouts": {
    "plate": {
      "spacing": "5 mm",
      "bed": [220, 220],
      "parts": [
        {
          "id": "housing",
          "select": {"bodies": ["Housing"]}
        },
        {
          "id": "lid",
          "select": {"bodies": ["Lid"]},
          "rotate": [{"axis": [1, 0, 0], "angle": "180 deg"}]
        }
      ]
    }
  },
  "requests": [
    {"id": "print", "kind": "export", "format": "3mf", "layout": "plate"},
    {"id": "preview", "kind": "snapshot", "layout": "plate", "options": {"view": "top"}},
    {"id": "check", "kind": "commands", "layout": "plate", "commands": [
      {"command": "inspect.interference", "params": {}},
      {"command": "inspect.measure", "params": {}}
    ]}
  ]
}
```

Run an existing example:

```sh
export SOLIDCRAFT_CLI="$PWD/../solidcraft/target/debug/solidcraft-cli"
cargo run -- evaluate examples/14_printable_gearbox.sc \
  --requests examples/14_printable_gearbox.print.json \
  --output-dir build/gearbox --no-previews
```

Use `observe saved.solidcraft --requests recipe.json --output-dir build/review` to prepare
outputs from a saved design without its source file. `--no-previews` disables the built-in
views; explicit preview requests still run. Requests without `layout` retain their previous
behavior, including raw engine commands.

## Selection and orientation

Every layout contains `parts`, each with a unique filename-safe `id` and one `select`:

- `{"bodies": ["BodyName"]}` selects exact world body names. Multiple names form a rigid group
  with their relative positions preserved. Missing names fail the request. Give important
  bodies explicit names in `.sc` rather than relying on `BodyN` numbering.
- `{"instance": ["left_module", "bracket"]}` selects an exact occurrence-name path from the
  assembly root, including descendant geometry. DSL child instance names are accepted directly;
  the compiler's internal qualification is resolved automatically. A top-level instance uses
  a one-element array. Missing or ambiguous path segments fail rather than choosing a placement.

Selecting the same body in separate parts makes independent layout copies. To keep bodies
together, select them in one part. Geometry outside all selections is omitted.

`rotate` is an ordered list of axis-angle rotations. Axes are nonzero three-component unitless vectors;
angles use degrees when bare, or explicit units such as `"pi/2 rad"`. Rotations are relative to
the **evaluated world pose**, about the world origin. They are applied before placement; final
bounds are measured from the rotated geometry, not inferred by rotating an old bounding box.
Orientation is explicit; the tool does not guess support-free or strength-optimal print poses.

## Placement and expressions

Parts default to automatic XY arrangement and placement on the Z=0 bed. Layout `spacing`
defaults to 5 mm and must be nonnegative. The arrangement is deterministic, preserves recipe
order among automatic parts, and reserves all explicit placements first.

Optional part fields:

```json
{
  "id": "lid",
  "select": {"bodies": ["Lid"]},
  "at": ["width + 10 mm", 0],
  "on_bed": true,
  "lift": "0 mm"
}
```

`at` is the XY **minimum corner of the rotated bounds**, not a translation delta. Omit it to
auto-arrange. `on_bed` defaults to true; `lift` sets the bottom clearance above the bed. With
`on_bed: false`, the rotated Z placement is preserved and `lift` adds a nonnegative translation.

`spacing`, `bed`, `at`, `lift`, axis components and angles accept numbers or scalar expression
strings. Expressions use the same dimensional evaluator as `.sc`, reading the **saved model's
current user parameters**. For example, `"wall * 2"`, `"max(width/10, 3 mm)"` and `"opening/2"`
follow parameter edits in SolidCraft. Compile-time `.sc` `let` helpers are not stored model
parameters and cannot be referenced by an external recipe.

`bed: [width, depth]` specifies a positive rectangular bed in mm. Overflow fails the request;
it never silently drops a part or adds an undeclared plate. Make separate named layouts for
multiple beds. Without `bed`, automatic arrangement chooses a roughly square working width
and as much depth as needed. This produces a layout, not a claim that it fits a particular printer.

Packing uses conservative XY bounding rectangles and enforces spacing for manual placements
too. It does not nest concave profiles or minimize material, supports or print time. An explicit
placement that overlaps another part or violates the bed/spacing fails. Raw requests remain
available for arrangements that intentionally overlap or require other engine operations.

## Reuse, artifacts and geometry checks

A request's `layout` may be a named entry in `layouts` or an inline layout object. Identical
layouts are prepared once per evaluate/observe invocation, then copied separately for each
request. Source inventory and geometry snapshots are also reused across different layouts.
A request's own commands run **after** layout preparation. They cannot modify another
request's prepared input through ordinary save or move commands.

Each prepared request retains `prepared.solidcraft` and `layout.json`. Its report metadata
contains selected body names, measured bounds, translations, model parameter values, and the
volume check for each part. The first preparation also retains geometry intermediates, phase
scripts, engine output, and inspection results. These are generated review artifacts, not
files to keep in `examples/`.

The default `"geometry": "mesh"` snapshots the engine's evaluated tessellation through 3MF,
then places independent mesh components. This preserves the geometry used for manufacturing
exports without reconstructing it through STEP. It supports solid and mesh inputs, with 3MF,
STL/STLA, OBJ and GLB outputs. GLB uses the existing metres/Y-up conversion. Prepared documents
retain geometry and placement; source history, constraints and joints remain in the original.

Set `"geometry": "solid"` to transfer B-rep bodies through STEP instead. This is required for
STEP or 3MF face-tag exports. It retains analytic solid geometry where the engine's STEP
roundtrip supports it, but does not retain the original history, identities or material
assignments as design semantics. Some complex solids do not survive STEP reconstruction
faithfully; the volume check rejects such transfers. The servo-arm example exposed this
limitation and uses the default mesh path.

Body counts must survive transfer in either mode. In mesh mode, the reference volume is
computed from the exported 3MF triangles; its difference from the original B-rep volume is
reported separately as tessellation difference. This uses the engine's origin-zero signed
integral: meshes with surface gaps can have origin-dependent volume, so passing this check
does not establish watertightness. In solid mode, the reference is the source
B-rep volume. Import differences are checked against the larger of 0.00001 mm³ and
`volume_relative_tolerance * reference_volume`. The default
relative tolerance is `0.0001` (0.01%); set a finite nonnegative value on the layout to tighten
or relax it for your requirements. This accounts for numerical measurement, tessellation and
STEP reconstruction behavior; it is not an independent solid-validity or printability certificate.
Failed transfer checks and layout errors fail required requests. Optional requests become
warnings, as with all other artifact requests.

The request schema and `capabilities --json` describe these fields. Body layouts need engine
`occurrence.properties`; instance selection additionally needs its `world_name` field, added
by the [compatible engine patch](assemblies.md#engine-compatibility). That field resolves exact export selection without guessing
display names for repeated instances. Unsupported engines produce explicit request errors.

The four migrated print recipes preserve their part selections and rotations. Their plate
coordinates now come from measured arrangement, so layouts adapt when dimensions change.
