# Engine performance

Regressions and slow spots in the SolidCraft engine, found while building the examples, kept here
to investigate later, or soon if one becomes a blocker. Each entry has a reproduction.

## Measuring

```sh
cargo build
ci/engine-timings.py OLD/solidcraft-cli NEW/solidcraft-cli --json timings.json > timings.md
```

The script builds every example (or the ones named) on each engine, one at a time, with
`solidcraft-dsl run --timings`, and prints a table of build times, slowest regression first. For
each example that got at least 1.2 times slower, it lists the commands that changed most. These
are engine build times only: `evaluate` also exports meshes and runs checks.

## 0.19.0 → 0.27.1

Measured 2026-10-31 on Apple silicon, all 68 examples, one build at a time.

### Patterns are about 3 times slower (open)

Every regression worth noting is in `solid.pattern.rectangular` and `solid.pattern.circular`.
Summed over all examples, pattern commands took 114 s on 0.19.0 and 339 s on 0.27.1, while every
other command together got slightly faster (452 s → 431 s). The slowdown grows with the number of
copies and the size of the body they join:

| Example | Pattern | 0.19.0 | 0.27.1 | Change |
|---|---|---:|---:|---:|
| 29_thin_sheet | `Grid`, rectangular, 60 × 10 copies of 2 holes | 7.4 s | 157.8 s | ×21 |
| 14_printable_gearbox | `RingSpaces`, circular | 1.5 s | 11.6 s | ×7.5 |
| 18_skeleton_gearbox | `RingSpaces`, circular | 1.6 s | 11.6 s | ×7.5 |
| 19_mini_skeleton_gearbox | `RingSpaces`, circular | 0.7 s | 6.1 s | ×8.7 |
| 06_bearing_flange | `BoltRing`, circular, 6 | 0.05 s | 0.68 s | ×14 |
| 03_bolt_plate | `Holes`, rectangular, 2 × 2 | 0.02 s | 0.28 s | ×12 |
| 35_ergonomic_knob | `Scallops`, circular | 26.2 s | 43.1 s | ×1.6 |
| 32_lofted_planter | `DrainRing`, circular | 3.8 s | 5.8 s | ×1.5 |
| 36_soap_dish | `DrainSlots`, rectangular | 1.1 s | 2.7 s | ×2.6 |

Whole builds: 29_thin_sheet 7.4 s → 158 s, the three gearboxes about 2.8 times slower, 35 1.3
times. Builds without patterns take the same time or less.

Still present in 0.28.0 (released 2026-10-31; all 80 examples and the 95 tests pass on it
unchanged): 29_thin_sheet 158.1 s, 14_printable_gearbox
17.4 s, 06 and 03 unchanged from 0.27.1.

No workaround in the DSL: patterning tool bodies instead (600 pin bodies in 0.08 s on either
engine) and cutting them from the sheet in one `combine` takes longer still, 245 s on 0.19.0 and
265 s on 0.27.1.

Tracked upstream (open, 2026-10-31): tangency-labs/solidcraft #9007 (circular pattern), #6478 and
#8787 (rectangular pattern), #2076 (pattern cost growing about quadratically with the copies).

Smallest reproduction: `examples/03_bolt_plate.sc`, a plate with one hole patterned 2 × 2
(`solidcraft-dsl run examples/03_bolt_plate.sc --timings`, step `Holes`). For a large one,
`examples/29_thin_sheet.sc`.

### Clearance and intersection of overlapping threads don't finish at some angles (open)

`examples/49_bottle_and_cap.sc` with the cap screwed on and turned 295°, 300° or 305°: both
`inspect.clearance` between Cap and Bottle and a boolean intersection of the two run for more
than 30 minutes at full CPU (0.19.0 and 0.27.1). At other angles the same query finishes in about
3 minutes. Those angles were measured on the parts' finest meshes instead, with
`ci/mesh-fit.py` (calibrated on 180 deg, where it finds the engine's 0.088 mm gap): the flanks
overlap 0.49 mm deep, as at 330 deg, so the query stalls on overlapping threads, not touching ones.

## Other engine problems found along the way

- **`surface.fillet` not implemented** (0.27.1 and 0.28.0): listed by the engine, it answers "not
  supported yet"; tracked upstream as tangency-labs/solidcraft #769 (open).

Not performance, but found in the same runs:

- **Tessellation cracks** (0.19.0 and 0.27.1): the meshes exported for some bodies have edges
  used by only one triangle, for example 47's Box (94 such edges in its 3MF). 0.27 refuses to
  measure such a mesh; print layouts now warn about it. Seen in 17, 25, 35, 47, 49 and 61.
- **Thickened lofts** (0.19.0 and 0.27.1): a loft surface thickens into a valid solid, but any
  later join or cut with it fails ("the operation failed: no result"), even a box through its
  wall. Thickened revolves, extrudes and sweeps are fine. A loft with sharp edges can't be
  thickened inward at all.
