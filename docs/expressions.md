# Calculations and persistent geometry

The DSL and the updated SolidCraft engine use the same `solidcraft-expr` scalar core:
operators, precedence, units, functions, dependency resolution and errors. No second math parser
or string-to-JavaScript evaluation is involved. Lengths resolve to mm and angles to radians.

```sc
design Plate units mm
param L = 80 mm
param W = 50 mm
param T = 6 mm
let center_x = L/2
let center_y = W/2
let area = L*W
box Plate size L W T corner (-center_x,-center_y,0) -> body PlateBody
```

`param` creates a native editable length, angle or unitless parameter. `let` defines a local
helper, including area and volume. Helpers are expanded symbolically into native formulas:
`center_x` becomes `(L/2)`, never the constant `40`. Helpers do not clutter the app's parameter
list. Forward references work; cycles, unknown names, invalid units and invalid calculations
are compile errors with source lines. Expansion is bounded to prevent runaway helper graphs.

Use `+ - * / ^ %`, parentheses and functions such as `sqrt`, `sin`, `cos`, `tan`, `atan2`,
`abs`, `min`, `max`, `floor`, `ceil` and `round`. Coordinates accept full expressions, including
nested calls: `(max(L/2, 10 mm), W*sin(30 deg))`. Feature arguments containing spaces should
be grouped: `distance (L/2 + 2 mm)`. Unitless trigonometric inputs are degrees, matching the
engine. A bare number can adopt the expected dimension; incompatible dimensions are rejected.

## What stays live

| Input | Meaning after compilation |
| --- | --- |
| Native parameters, dimensions, feature expressions | Persistent engine formulas |
| Primitive box/cylinder/sphere/torus placement coordinates | Persistent engine formulas |
| Ordinary sketch coordinates, circle/polygon sizes, slot width | Evaluated solver seeds; dimensions/constraints determine the final shape |
| `bind point` coordinates | Persistent, fixed, signed position formulas |
| Selection locations, direction vectors, polygon sides, pattern counts | Evaluated during compilation; rebuild source to change these inputs |

A calculated seed does not become a positional constraint. Use normal dimensions for solver
relationships, or explicit bindings for formula-driven placement:

```sc
sketch Locations on XY {
  point seed = (L/2, W/2)
  bind point center = (L/2, W/2)
  bind point rotated = (L*cos(30 deg), L*sin(30 deg))
}
```

Bound points remain signed through quadrant changes and can be referenced by lines and
polylines. They do not automatically join nearby points. Literal bindings are fixed too.
Do not add conflicting driving constraints to bound geometry.

**Engine requirement:** persistent sketch points exist in the current engine source, but not
in released CLI versions 0.15 or 0.16.2. `evaluate` checks the advertised command capability
and returns an actionable error. Use a development build explicitly:

```sh
cargo build --manifest-path ../solidcraft/Cargo.toml -p solidcraft-cli --bin solidcraft-cli
export SOLIDCRAFT_CLI="$PWD/../solidcraft/target/debug/solidcraft-cli"
cargo run -- evaluate examples/10_rotating_link.sc --json
```

The first nine numbered examples work with the release engine. Persistent point tests print
an explicit skip on older engines; run the suite with the development binary to exercise all ten.

## Inspect and iterate

```sh
solidcraft-dsl explain examples/plate.sc --json
solidcraft-dsl evaluate examples/plate.sc --json
```

`explain` returns parameter/helper values, dimensions, dependencies and coordinate/topology
sites with persistence flags. Evaluation reports contain the same explanation. Area/volume
helpers include dimension exponents (`length: 2` or `3`); their values use mm powers.

Edit `.sc`, evaluate, inspect measurements and [request more artifacts](agent-observation.md),
then repeat. Human changes to native parameters affect persistent formulas. Source edits and
native edits remain distinct revisions: observe the exact saved model when reviewing native
changes. Decompilation preserves explicit bound points and native formulas, but cannot recover
source helper names or original seed expressions after they have been lowered.

## Shared core distribution

`crates/solidcraft-expr` is a standalone crate, extracted from SolidCraft's expression module;
see its [provenance](../crates/solidcraft-expr/README.md). The companion engine change replaces
its old evaluator with a thin document-error adapter and depends on the same crate source at
`crates/expr`. Both repositories contain the package so either checkout builds independently.
Until the crate is published, changes to its implementation must be synchronized and tested
in both repositories; these copies are not independently maintained evaluators.
