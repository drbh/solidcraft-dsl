# Sketch copies, planes, lofts, sweeps, threads, text and sheet metal

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

## Solid features

```sc
loft Pot sections Base, Middle, Rim -> body Pot                 // 32_lofted_planter
sweep Hook from Profile path Path.a, Path.k, Path.b new -> body Hook   // 33_swept_wall_hook
pipe Tube path Path.a, Path.b diameter 6 wall 1 new -> body Tube
thread NeckThread face (rn, 0, z) designation M28x2 modeled     // 49_bottle_and_cap
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

Holes in a sheet must be straight extrude cuts square to the face, so that they survive in the flat
pattern. A `cylinder … cut` is refused. Export the flat pattern from a requests file:

```json
{ "id": "flat-dxf", "kind": "commands", "extension": "dxf",
  "commands": [ { "command": "sheet.export_dxf", "params": { "body": "Bracket", "path": "$output", "bend_notes": true } } ] }
```

See [51_sheet_metal_bracket](../examples/51_sheet_metal_bracket.sc).
