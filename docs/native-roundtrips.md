# Complete native document roundtrips

`decompile` now defaults to a lossless, editable `native` source form. It preserves the full
native document, including feature types without concise DSL syntax. It does not reconstruct
IDs by replaying modeling commands, flatten geometry, or silently discard unfamiliar fields.

```sh
solidcraft-dsl decompile complex.solidcraft -o complex.sc
solidcraft-dsl explain complex.sc --json
solidcraft-dsl pack complex.sc -o rebuilt.solidcraft
solidcraft-dsl evaluate complex.sc --output-dir build/complex --json
```

Decompilation and native packing require no engine. Evaluation uses the engine's existing
native document loader, which owns migrations, reference resolution, validation and geometry.
Use `SOLIDCRAFT_CLI` to select an engine that supports the document's version.

## Representation and ownership

```sc
native {
  "format": "solidcraft-source/1",
  "document": {
    "format": "solidcraft/28",
    "name": "Example",
    "units": "mm",
    "params": [],
    "features": [],
    "next_id": 1
  }
}
```

The block is JSON, with the **complete document as the single authoritative model**. Its schema
is SolidCraft's native schema. Feature fields, parameter expressions, signed bindings, entity
IDs, suppressed features, timeline markers, components, occurrences, joints, configurations,
materials, appearances, metadata, view settings and unknown fields all retain their values.
You can edit these fields directly; `explain` lists features with JSON pointers and inventories
all top-level fields. Native source currently uses strict JSON inside the block, without comments.

A packaged input also has `container` (the package manifest) and `entries` (a map from package
entry name to base64 bytes). STEP imports, meshes, canvas images and unknown binary extensions
remain embedded and portable. They are individual asset payloads, not an opaque copy of the
original file. Entries are never extracted onto filesystem paths. Previews and caches are
preserved as payloads too; the engine checks their document hashes and rebuilds stale caches.

Source is self-contained for **embedded** assets. Linked designs keep their native path references
and cached imported bodies. The converter does not fetch external files or rewrite link paths.
Retain the original relative directory layout if you want Update Links to find the same files;
packing into a different directory does not automatically create a portable linked project.

The concise modeling form (`param`, `sketch`, `extrude`, etc.) remains supported. Choose one
form per file: native fields and concise commands are not two competing definitions of a model.
Concise programs still compile to commands; native programs load the document directly.
In the Rust API, `Compiled.native` contains native data and `Compiled.script` is null for these
programs, deliberately preventing an imported model from being mistaken for an empty script.

## Verification

```sh
solidcraft-dsl roundtrip complex.solidcraft --output-dir build/roundtrip --json
solidcraft-dsl roundtrip complex.solidcraft --output-dir build/roundtrip --engine-check --json
solidcraft-dsl roundtrip complex.solidcraft --output-dir build/roundtrip --probe edits.json --json
```

Each attempt writes `design.sc`, an untouched input copy, a reconstructed file and `report.json`
in a unique directory. Verification reads the written files back and checks:

1. Every document and container-manifest value, including unknown fields and exact numeric values.
2. Every other package entry's bytes.
3. Stable text: decompile → reconstruct → decompile produces identical source.
4. With `--engine-check`, paired native loading/migration, complete saved documents, measurements
   (including bodies, volume, area and bounds) and document inspection using the same engine.
5. With `--probe`, the same additional engine commands applied to each independent copy first.

For example, `edits.json` can contain:

```json
{
  "commands": [
    {"command": "parameters.change", "params": {"name": "L", "expression": "120 mm"}}
  ]
}
```

Probes can exercise configurations, assemblies, suppression or any other engine operation.
Their filesystem capabilities are those of ordinary engine commands. Input copies prevent a
normal save from replacing the original; arbitrary paths explicitly supplied by a probe still
have their normal meaning. Probes that update linked designs need those dependencies available.

Document differences are reported as JSON pointers. Engine logs and probe scripts remain beside
the report. Engine comparison excludes only `document.inspect.path` and timeline execution
`ms`; it does not remove fields from the saved documents. Nondeterministic custom probes can
fail exact result comparison and should be investigated through the retained outputs.

**The guarantees are distinct.** A preservation pass is not proof that a model evaluates, and
paired same-engine measurements are not an independent geometric equivalence proof. A newer
schema can be preserved even when the installed engine cannot evaluate it. Broken native
models can also be preserved without repairing them. Engine failure makes an engine-checked
roundtrip fail; it does not masquerade as a successful geometry check.

ZIP ordering, compression, JSON whitespace and object-key order are not preserved. Document
values, references and binary payloads are. Bounds match the engine's bounded package format:
256 MiB per native file/entry, 512 MiB expanded, 10,000 entries. Text has a 1 GiB bound to allow
base64 expansion. Duplicate JSON keys, ambiguous ZIP entries and malformed encodings are rejected.

## Concise reconstruction

```sh
solidcraft-dsl decompile complex.solidcraft --readable -o readable.sc
```

`--readable` opts into the earlier modeling-operation reconstruction. It is useful for learning
and simpler designs, but has limited feature coverage and does not preserve the full document.
It emits a warning. Packaged inputs are resolved through the engine for this mode.

The library exposes `decompile` for lossless JSON-document conversion and `decompile_readable`
for the old concise reconstruction. Use `native::Native::read` for packaged documents with assets.
The existing interactive `watch` supports concise source only; use `pack`/open or `evaluate`
for native source, then decompile a human-edited saved file to bring those edits back.

## Regression coverage

`tests/native.rs` includes 12 unchanged historical native fixtures covering old schema versions,
assemblies, hinges, sheet metal, plastic features, path patterns, split bodies and face naming.
All are checked for exact preservation and paired engine evaluation. Additional tests cover
unknown future features/fields, deleted-ID gaps, arbitrary-precision numbers, metadata, package
entries, STEP/mesh/image assets after deleting their original files, and native parameter edits.

```sh
cargo test --test native --locked
SOLIDCRAFT_CLI=/path/to/development/solidcraft-cli cargo test --workspace --locked
```

Preservation coverage does not require a per-feature whitelist: newly introduced fields remain
structured data automatically. Add real files and behavior probes as the engine grows so
behavioral coverage expands alongside preservation coverage.
