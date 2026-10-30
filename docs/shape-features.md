# Sketch copies, curves, imports, planes, shaping, lofts, sweeps, threads, text and sheet metal

These statements map one to one onto SolidCraft engine commands. Each section names an example
that uses it.

## Sketch entities

```sc
sketch Tag on XY {
  ellipse e = (0,0) major (30,0) minor 15     // major: the end of the major axis; e.center
}
sketch Label on XY offset 2 {
  text "SOLIDCRAFT" at (L/2, 10 mm) height 7 mm align center   // also: angle, font "Name", italic
}
```

- `text` must come last in its sketch. The engine numbers glyph curves from the same counters
  as every other entity, so nothing drawn after the text can be predicted. Put labels in a sketch
  of their own.
- The built-in font (Liberation Sans) has no bold face. Pass `font` for an installed font.
- `at` stays a live expression; `minor` is evaluated when compiling.
- `fix outline` fixes every curve of a polyline, rectangle, slot or polygon named whole.

## Copies in a sketch

```sc
sketch Lid on XY {
  rect rounded o = (0,0) (90,64) radius 6
  circle h = (10,10) diameter 3.2
  construction line mid = (45,-1) (45,65)
  rect n = (1,29) (6,35)
  point ctr = (45,32)
  fix o h mid n ctr
  offset o distance (wall + play) side (45,32)     // also: symmetric
  pattern rectangular h count 2 spacing 70 count2 2 spacing2 44   // also: dir (x,y), dir2
  pattern circular n count 6 center ctr angle 360 deg
  mirror n about mid
}
```

- Name whole entities or single curves. Copies stay associative: the offset keeps its
  distance as a live dimension, and patterns and mirrors follow the original.
- Counts include the original. Pattern counts, spacings and angles are fixed when compiling.
- A circular pattern around coordinates leaves its centre point free. Name a fixed point
  (`center ctr`) to hold the pattern still.
- Copies, like text, come after the drawn entities. Only more copies, text and constraints can
  follow, because the engine numbers what it makes by what it copies. See `52_vented_lid`.

## Equation curves and imported drawings

```sc
sketch Flank on XY {
  curve inv = (rb*(cos(t*1 rad) + t*sin(t*1 rad)), rb*(sin(t*1 rad) - t*cos(t*1 rad))) t from 0 to 0.8 points 12
  curve cam = ((r0 + lift*(1 - cos(t))/2)*cos(t), (r0 + lift*(1 - cos(t))/2)*sin(t)) t from 0 deg to 360 deg closed
  line l = (0,0) inv.start                      // also inv.end, inv.1, inv.2, …
  fix inv cam l
}
sketch Logo on XY offset 3 mm {
  svg "art/logo.svg" at (-22,13)                 // also: scale 0.5; dxf "outline.dxf"
}
```

- A `curve` is sampled when compiling, at `points` evenly spaced values of `t` (24 by default,
  3 through 200), into a fit-point spline. `closed` makes it periodic. `t` keeps its unit, so it
  can run over angles. The curve follows parameters when rebuilt, but it isn't a live engine
  equation. See the involute flanks of `37_involute_gears`.
- `svg` and `dxf` read the file relative to the design file when it is built, and put its
  text into the script. SVG measures y downwards, so `at` places the drawing's top-left corner.
  Imported curves stay unconstrained, and `evaluate` does not warn about them. An import, like
  text, comes after the sketch's drawn entities. See `58_logo_coaster`.

## Construction planes

```sc
plane Up on XY offset 20 mm
plane Face on XY angle slope axis X            // hinged on a world axis
plane Hinge on XY angle 10 deg axis (1,1,0) origin (5,0,0)
plane Side face (10,0,5) offset 2 mm           // from a planar face
plane Slope through (0,0,0) (10,0,5) (0,10,0)
plane Mid between Up, Face
plane Station path Rail.l fraction 0.25        // square to a sketch curve
sketch Lettering on Face { … }
```

A plane can be used by name wherever a plane is taken: in `sketch … on`, in another
`plane … on`, and in a mirror's `plane`. Offsets and angles stay live. See
`53_desk_name_plate`, where the lettering follows the slope parameter.

## Cones

```sc
cone Tip radius 10 mm height 20 mm -> body Tip                      // to an apex
cone Funnel radius 15 mm top_radius 5 mm height 30 mm base (40,0,0) axis X -> body Funnel
```

`cone` uses the engine's `solid.cone`, added in 0.27. An older engine refuses the design up
front, with a message naming the missing `solid_cone` capability. On those engines, revolve a
triangle or trapezoid instead.

## Shaping and splitting solids

```sc
rib Gusset from Corner curves g thickness 4 mm          // one connected chain, filled to the faces it meets
web Divide from Floor curves a, b, c thickness 1.2 mm depth 20 mm direction normal flip
draft Walls faces (60,0,15) (60,80,15) angle (-4 deg) neutral XY body Tray
offset_faces Grow faces (126,3,3) distance 1 mm body Peg
scale Big bodies Peg factor 1.5 origin (120,0,0)        // or factors (1, 1, 2)
pattern Teeth features Tooth path Rail.edge count 9 distance 120 mm   // or spacing; identical; flip
split Halves target Shelf origin (160,0,0) normal (1,0,0) -> body ShelfLeft   // or plane Mid
color Shelf "#d9a441"
material Shelf, ShelfLeft PLA
```

- `rib` takes one connected chain. `web` makes one wall per curve. `direction normal` grows
  along the sketch's normal, and `flip` turns it to the other side.
- `draft`: a positive angle narrows the part along the pull, which defaults to the neutral
  plane's normal; a negative angle widens it. Draft a solid before shelling it: the shell keeps
  the walls even. Drafting the faces of a shelled body together can fail (`54_nesting_tray`).
- A pattern along a path turns the copies to follow it unless it is `identical`.
- `split` keeps the target's name on the side the normal points to and names the other piece
  (`-> body`). Check each piece's size against the bed (`55_split_wall_shelf`).
- `color` and `material` set what an export and the measured mass use. Materials come from the
  engine's library: `PLA`, `ABS Plastic`, `Nylon`, `Polycarbonate`, `Steel`, `Aluminum`, ….
- Engines before 0.27 cannot pattern or mirror a rib or web; repeat it instead. Naming a split
  piece needs engine 0.19 or later; 0.15 loses the name when it rebuilds.

## Solid features

```sc
loft Pot sections Base, Middle, Rim -> body Pot                 // 32_lofted_planter
sweep Hook from Profile path Path.a, Path.k, Path.b new -> body Hook   // 33_swept_wall_hook
pipe Tube path Path.a, Path.b diameter 6 wall 1 new -> body Tube
thread NeckThread face (rn, 0, z) designation M28x2 modeled     // 49_bottle_and_cap; `left` for left-handed (engine 0.27+)
coil Spring diameter 20 turns 5 pitch 4 section_size 1.5 base (0,0,0) new -> body Spring
emboss Letters from Label depth 1 targets Tag                   // 50_name_tag (deboss to cut in)
```

- `sweep` options are `twist <angle>`, `scale <factor>` and `parallel`, which keeps the profile
  from turning with the path. Path curves are listed in order and must all be in one sketch.
- `thread` without `modeled` is cosmetic: it is recorded on the face, and the geometry does not
  change. Modelled threads are slow to compute. Put them last, after every other boolean on that body.
- `coil` takes two of `turns`, `pitch` and `height`. Other options are `square`,
  `section_position inside|center|outside`, `axis`, `start_angle` and `clockwise`.

## Sheet metal

```sc
sheet_rule Steel thickness 1.5 mm bend_radius 1.5 mm k_factor 0.44   // becomes the active rule
flange Wall from WallBlank profiles (2 mm, 2 mm) rule Steel -> body Bracket   // base flange
flange Shelf edges (W/2, 0, 0) height 80 mm flip body Bracket     // edge flange, bent from that edge
```

```sc
hem Safe edges (80,0,15) length 5 mm body Grille            // also: gap, flip
perforate Holes diameter 4 mm pitch 6.5 mm margin 8 mm body Grille   // staggered; or square
```

A hem folds an edge back on itself. A perforation is a hole field over the flat sheet: it is in
the flat pattern, the DXF (layer PERFORATION) and the mass, but is not cut into the solid. See
`57_perforated_grille`.

Holes in a sheet must be straight extrude cuts square to the face, so that they survive in the flat
pattern. A `cylinder … cut` is refused. Export the flat pattern from a requests file:

```json
{ "id": "flat-dxf", "kind": "commands", "extension": "dxf",
  "commands": [ { "command": "sheet.export_dxf", "params": { "body": "Bracket", "path": "$output", "bend_notes": true } } ] }
```

See [51_sheet_metal_bracket](../examples/51_sheet_metal_bracket.sc).
