# Ten progressive CAD stress cases

All ten are executable `.sc` designs. Each adds a different combination of geometry and
expression behavior. `tests/evaluate.rs` checks body count and analytic volume to within
0.03%, allowing curved-surface integration error. Compilation is tested without an engine.

| # | Example | Exercises | Expected volume (mm³) |
| --- | --- | --- | --- |
| 1 | [Spacer](../examples/01_spacer.sc) | Mixed units, cylinder | 432π |
| 2 | [Centered plate](../examples/02_centered_plate.sc) | Calculated coordinates, centered constrained rectangle | 3840 |
| 3 | [Bolt plate](../examples/03_bolt_plate.sc) | Forward helpers, holes, rectangular pattern | 24000 − 150π |
| 4 | [Stepped shaft](../examples/04_stepped_shaft.sc) | Calculated closed profile, XZ coordinates, revolution | 1320π |
| 5 | [Trigonometric gusset](../examples/05_trigonometric_gusset.sc) | Angles, trigonometry, powers, area helpers | 4000/√3 |
| 6 | [Bearing flange](../examples/06_bearing_flange.sc) | Joined boss, bore, circular bolt pattern | 7540π |
| 7 | [Slotted bracket](../examples/07_slotted_bracket.sc) | Joined solids, calculated capsule cut | 12000 − 45π |
| 8 | [Enclosure and lid](../examples/08_enclosure_and_lid.sc) | Shell selection, independent bodies, calculated placement | 26808 (two bodies) |
| 9 | [Heat sink](../examples/09_heat_sink.sc) | Computed integer count, centered fin array | 45600 |
| 10 | [Rotating link](../examples/10_rotating_link.sc) | Persistent rotated points, shared endpoints, counterbores | 6000 − 118π |

Case 10 requires persistent point expressions, available in the pinned 0.19.0 release; see
[expression semantics and engine setup](expressions.md). Its integration test saves and
reopens the model, deletes the source, changes the angle from 30° to 135° and length from
60 to 80 mm, and checks both volume and rotated bounds. It also recompiles decompiled source
and verifies the geometry is unchanged.

Run the full suite:

```sh
SOLIDCRAFT_CLI=/path/to/development/solidcraft-cli cargo test --workspace --locked
```

Build review evidence with the normal agent interface:

```sh
solidcraft-dsl evaluate examples/09_heat_sink.sc --output-dir build/heat-sink --json
solidcraft-dsl observe build/heat-sink/last-success.json --requests docs/agent-requests.json --json
```

The request file is an example: customize body names and camera targets for the selected model.
The local `build/expression-stress/` runs contain reports, default multiview sheets and GLBs
for all ten examples; generated artifacts are ignored by Git.
