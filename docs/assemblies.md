# Assemblies in concise `.sc`

Components define shared geometry; instances place it. A component definition creates no
geometry until instantiated. Two instances of one component share its sketches, features and
bodies, and keep independent placements. Root geometry remains supported alongside components.

```sc
param opening = 35 deg
component Leaf {
  box Plate size 30 12 3 corner (0,-6,0) -> body PlateBody
  connector pivot at (0,0,0) axis Z x X
}
instance fixed_leaf = Leaf at (10,20,0) rotate Z 15 deg
instance moving_leaf = Leaf at (10,20,3)
ground fixed_leaf
joint hinge revolute between fixed_leaf.pivot and moving_leaf.pivot value opening offset 3 mm limits (0 deg,160 deg)
```

Use one statement per line, with opening and closing braces as shown. See
[the hinge example](../examples/assembly_hinge.sc) and
[repeated subassemblies](../examples/assembly_nested.sc).

## Definitions, scopes and placement

- Declare all `component Name { … }` definitions at the top level. Definitions can refer
  forward to other definitions with `instance child = Other`. Cycles, excessive nesting and
  more than 100,000 expanded placements are rejected before execution.
- `param` and `let` declarations are global. Every instance shares the same definition;
  per-instance parameter overrides and independent copies are not part of this syntax.
- Geometry names are local to each component. Two definitions can both contain `Outline`,
  `Extrude` and `MainBody`. Give referenced bodies explicit names with `-> body Name`.
  Do not depend on generated `BodyN` names inside components. Geometry references do not
  cross component scopes. The compiler qualifies local names in the native document.
- Instances and connectors have separate local name scopes. Names starting with `@` are
  reserved for compiler-generated names; `root` (case insensitive) is reserved for the design.
- `instance name = Component [at (x,y,z)] [rotate axis angle]` places a component in its
  parent's frame. Rotation is about the component origin, followed by translation. Defaults
  are zero translation and rotation. Axes accept `X`, `Y`, `Z` or a nonzero unitless vector.
  Lengths use millimetres and bare angles use degrees. Group calculations, e.g.
  `at (spacing/2,0,0) rotate (0,0,1) (opening/2)`.
- Repeated subassemblies share their child occurrence definitions. Their child placements
  are identical relative to each parent; they are not independently configurable mechanisms.
- `ground name` fixes a top-level instance. Nested grounding is explicitly rejected.
  When no grounded occurrence or root participates, the engine anchors the first joint’s
  A side automatically; use explicit grounding for predictable mechanism setup.
- Root geometry is compiled first; used definitions are built on their first instance, then
  all placements are applied, grounding is applied, all connectors are captured, and joints
  are created in source order. This prevents a copied instance inheriting another's placement
  or a later connector being captured after an earlier joint has moved it.

## Connectors and joints

`connector name at (x,y,z) [axis Z] [x X]` defines a component-local frame. Its z and x axes
must be nonzero and perpendicular; magnitudes are normalized. For an X-axis connector,
write `axis X x Y`. Coordinates are explicit local locations, not face/edge selections.

A joint endpoint is `instance.connector`. Endpoints currently address top-level instances.
Nested endpoints and joints inside component definitions are rejected: the engine's saved
joint origins identify an occurrence, not a particular placement path through repeated
parents. Static nested assemblies are supported; repeated nested mechanisms need that
engine distinction before concise syntax can represent them faithfully.

`joint name type between a.connector and b.connector` accepts these joint types:

| Type | Values, in order |
| --- | --- |
| `rigid` | none |
| `revolute` | angle |
| `slider` | distance |
| `cylindrical`, `pin_slot` | angle, distance |
| `planar` | x distance, y distance, angle |
| `ball` | yaw, pitch, roll |

Use `value expression` for one value or `values (a,b,…)` for all values. Omitted values start
at zero. Optional `limits (min,max) (min,max) …` constrain successive values; unprovided
trailing limits are unbounded. `offset expression` sets the frame's axial offset and
`angle expression` sets its angular offset. Axes align by default; `opposed` mates their
z directions oppositely. Joints can move ungrounded instances away from their initial
placements. Engine limits may clamp requested values; inspect the reported effective values.

## What stays parametric

Joint values, offsets and angular offsets retain their expressions in the saved document.
`let` references are expanded symbolically, so changing a referenced parameter in SolidCraft
updates the joint without recompiling `.sc`. Geometry retains its existing expression/seed
semantics. Every occurrence uses the updated shared geometry.

Placements, connector coordinates/axes and joint limits are evaluated when compiling. They
are stored as numeric frames and limits. Changing their input parameters in the app does
**not** relocate those frames: rebuild the `.sc` source to update them. In particular, a
connector at `(length,0,0)` does not follow a changed body length in the saved model. Use a
persistent joint offset when it represents the intended relationship. `explain` and evaluation
reports label these sites with `persistent: false`; joint expression sites have `true`.

## Agent feedback and roundtrips

`evaluate` includes `assembly` with joint values, expressions, origins, degrees of freedom,
conflicts, errors and floating groups. Conflicts/errors fail the review; floating groups are
warnings. Measurements include placed instances. Existing custom cameras, contact sheets,
GLB exports and arbitrary observation requests work with assemblies. A one-DOF hinge is not
an error; its freedom is intentional. Interference checking remains an explicit observation
request, since touching or overlapping components can be intentional.

Default `decompile`, `pack` and `roundtrip` preserve native assembly data, including features
outside the concise assembly grammar. `decompile --readable` is still a limited geometry
reconstruction, not an assembly reconstruction. `watch` reflects parameter/dimension edits;
arbitrary browser moves and joint edits are not written back into concise source.

## Engine compatibility

This implementation adds optional `occurrence_name` arguments to engine `component.create`
and `occurrence.copy` in the sibling SolidCraft checkout. This addresses occurrences by authored
names without guessing global IDs. The arguments use the existing saved name field; no file
schema change is needed. Names must be unique, nonnumeric, and different from `root`.

Assembly execution requires an engine/app containing this command addition. Released 0.15 and
0.16.2 binaries do not provide it. `check` and `compile` work without the engine; `run`, `pack`,
`evaluate` and `watch` check capabilities before execution. For direct command-script replay,
use the same compatible engine.

```sh
# After building the sibling checkout with cargo build -p solidcraft-cli:
export SOLIDCRAFT_CLI="$PWD/../solidcraft/target/debug/solidcraft-cli"
cargo run -- evaluate examples/assembly_hinge.sc --output-dir build/hinge --json
cargo run -- explain examples/assembly_hinge.sc --json
```
