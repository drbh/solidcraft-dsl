# Reusing parts with `use`, and repeating with `for`

A part designed once, in a file of its own, can be placed in other designs:

```sc
use S1 from "parts/standoff.sc" at (x0, y0, floor_t) join targets Box
use S3 from "parts/standoff.sc" with (h = 9 mm) at (x1, y0, floor_t) join targets Box
use Pin from "parts/peg.sc" rotate X 90 deg at (5, 5, 2) -> body Pin
```

`use` copies the other design's geometry into this one when the design is built:

- **Names are prefixed** with the copy's name, so copies never collide. The part's parameter
  `h` becomes `S3_h`, and its body `Standoff` becomes `S3_Standoff`. A copy's parameters are
  ordinary parameters of the design, so you can still edit them in SolidCraft.
- **`with (p = expr, …)`** overrides the part's `param`s. The expressions are written in this
  design's terms, so they can use its parameters.
- **`rotate`** turns the part about the origin, and **`at`** then moves it. Design parts around
  their own origin. Both are applied when compiling.
- **`join targets B`** joins the part's bodies into `B`. The part's own `check`s are dropped,
  because its bodies no longer exist on their own. **`-> body Name`** names a part's single body
  instead, and keeps its checks.
- A part's `design` and `print` lines are left out. A part can `use` other parts, but not itself.

The path is relative to the file that names it. `run`, `evaluate` and `watch` expand parts;
`compile` does too, given the file. Errors and the source map point at the `use` line, and an
error inside a part names its file and line: `line 22: in parts/standoff.sc:9: …`. `watch`
writes parameter edits from the app back only to designs that use no parts.

For parts that must stay separate, or move, use an assembly with linked parts instead
([assemblies](assemblies.md)): `component Wheel from "wheel.sc"` keeps the part's own document,
timeline and joints. See `59_pcb_box`.

## Repeating with `for`

```sc
for k from 1 to 5 {
  cylinder Hole{k} diameter (pin_d + k*step) height 7 mm base (k*pitch, 15 mm, -1 mm) cut targets Plate
  sketch Label{k} on XY offset T {
    text "0.{k}" at (k*pitch, 3 mm) height 3.5 mm align center
  }
  emboss Mark{k} from Label{k} depth 0.6 mm deboss targets Plate
}
```

A `for` block repeats its statements for each whole number from `from` to `to`, counting down
when `to` is smaller (`step n` changes the stride). Use it when the copies differ in a way a
pattern can't express. In each copy, `{k}` is replaced by the number, which gives each copy its
own name and can go in text. The word `k` on its own in an expression is the number too, and the
expression stays live (`pin_d + 3*step`). Blocks nest, and a block can hold sketches, features,
checks and `use`. The bounds are numbers, not parameters, because the repetition happens when
compiling; a negative value can't go in a name. Errors point at the statement in the block. See
`60_hole_fit_ladder`.
