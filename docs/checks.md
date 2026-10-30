# Checks

A design can build without errors and still be wrong: a join that misses leaves a body in two
pieces, a part needs supports, or two parts that should move collide. A `check` statement makes
that a failure. `evaluate` verifies each check after the build. A failed check is an error with
its line, and the report lists every result under `design_checks`.

```sc
check Holder volume >= 20000 mm3
check Holder mass <= 60 g
check Holder size.z <= 200 mm
check Lid clearance Box >= gap
check Pinion clearance Gear > 0 mm
check Holder overhang <= 45 deg up X
check Bracket shells == 2
```

The form is `check <body> <quantity> <op> <value>`. The comparison is one of `==`, `<=`,
`>=`, `<` or `>`. Values are expressions, so they can use parameters. They are evaluated with
the parameters' current values.

| Quantity | Value | Measured as |
|---|---|---|
| `shells` | a whole number | closed shells in the body |
| `volume` | `mm3`, `cm3`, or a length cubed | volume |
| `area` | `mm2`, or a length squared | surface area |
| `mass` | `g` or `kg` | mass, from the body's material |
| `size.x`, `size.y`, `size.z` | a length | bounding-box extent |
| `clearance <other body>` | a length | the smallest distance between the two bodies |
| `overhang` | an angle | the steepest unsupported face; see below |

Length comparisons allow 0.001 mm, because the engine measures distances a little short.

## One piece by default

Every body gets an implied `check <body> shells == 1`. This catches a join that doesn't
touch the body, a cut that splits it, and the occasional kernel result whose faces are split
into two shells (see `11_phone_stand`). A body with a sealed void also has two shells. If a
body is meant to have more than one, say so with `check <body> shells == 2`.

## Clearance

`check A clearance B >= 0.3 mm` asks the engine for the nearest distance between the bodies
where they stand in the design. When they overlap, the clearance is 0 and the overlap volume
is reported, so any lower bound fails. A failing check names the two closest points.

## Overhang

`check Part overhang <= 45 deg up <direction>` checks that the body prints without supports
when `up` points away from the bed. The default is `up Z`. Use the direction of the print
recipe's orientation: a part rotated -90 deg about Y to print on its x = 0 end has `up X`. A
direction can be a tuple, such as `up (0, 0, -1)`.

The analysis uses the body's mesh. A face that points down more than the limit from vertical
needs support, except in these cases:

- **First layer:** a face within the first 0.3 mm above the bed prints on the bed.
- **Narrow ledge:** a region under 1 mm wide (twice its area over its perimeter) is spanned by
  the extrusion lines.
- **Bridge:** in a bridge, straight lines in one direction cross the region, and every line
  ends on a supporting wall at both ends. Examples are the top of a window, the crown of a
  horizontal hole, and the ceiling of a counterbore. The span is the longest such line, for the
  direction that gives the shortest span. Bridges up to 10 mm are accepted;
  `bridge 15 mm` changes that limit for one check.
- **Mesh slack:** facets may lean 0.5 deg past the limit, because curved and sloped faces are
  tessellated.

A failing check gives the unsupported area and where it is. Each region in `regions` is listed
as either an `overhang` or a `bridge too long`, with its span.
