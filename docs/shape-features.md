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

- Text can come from a text parameter (engine 0.27+): `param who = "ADA LOVELACE"`, then
  `text who at (…)` or `text (who + " - L") at (…)`. A parameter edit in SolidCraft relabels
  the part. An older engine draws the parameters' current words instead: the design builds,
  but relabelling it needs engine 0.27.
- `text` must come last in its sketch. The engine numbers glyph curves from the same counters
  as every other entity, so nothing drawn after the text can be predicted. Put labels in a sketch
  of their own.
- The built-in font (Liberation Sans) has no bold face. Pass `font` for an installed font.
- `at` stays a live expression; `minor` is evaluated when compiling.
- `fix outline` fixes every curve of a polyline, rectangle, slot or polygon named whole.

```sc
sketch Plate on XY {
  polyline closed o = (0,0) (40,0) (40,20) (0,20)
  fillet o.1.end radius 4 mm                     // round a corner
  chamfer o.2.end distance 3 mm                  // or: distance 3 mm distance2 5 mm
  chamfer o.3.end distance 4 mm angle 45 deg     // distance along the first line, angle from it
}
```

- A corner is the point where two lines meet, named by either line's end, such as `o.1.end`.
  A chamfer cuts it with a line, a fillet with an arc. Both may be followed by more entities and
  constraints: the compiler numbers what they add. The engine takes a chamfer's sizes as numbers,
  so they are evaluated when compiling; a fillet's radius stays live.

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

```sc
sketch LidSk on XY offset (H + 2 mm) {
  project face (L/2, W/2, H) body Box             // also: edge (x,y,z), vertex (x,y,z), body Box
}
extrude Lid from LidSk distance 3 mm -> body Lid
```

- `project` draws body geometry onto the sketch plane: a face's edge loops, one edge, one
  vertex, or a whole body's silhouette (`project body Box`). The projection stays linked, so it
  follows the body when a parameter changes it: changing the box's length above also lengthens
  the lid (checked on 0.19 and 0.27). The point picks the face, edge or vertex at the
  parameters' current values; `body` says whose, where bodies meet. Like an import, it comes
  after the sketch's drawn entities.

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
cone Tip radius 10 mm height 20 mm -> body Tip                      // to a point
cone Funnel diameter 30 mm top_radius 5 mm height 30 mm base (40,0,0) axis X -> body Funnel
```

A cone is a revolved profile: a right trapezoid, or a triangle for a point, drawn beside the
axis (X, Y or Z) from `base`. Its radius, top radius and height are live sketch dimensions, so it
builds on any engine and follows parameter edits. Cone faces can be shelled. See
`61_kitchen_funnel`.

## Shaping and splitting solids

```sc
rib Gusset from Corner curves g thickness 4 mm          // one connected chain, filled to the faces it meets
web Divide from Floor curves a, b, c thickness 1.2 mm depth 20 mm direction normal flip
draft Walls faces (60,0,15) (60,80,15) angle (-4 deg) neutral XY body Tray
offset_faces Grow faces (126,3,3) distance 1 mm body Peg
scale Big bodies Peg factor 1.5 origin (120,0,0)        // or factors (1, 1, 2)
pattern Teeth features Tooth path Rail.edge count 9 distance 120 mm   // or spacing; identical; flip
pattern circular Ring features Hole axis_face (38,0,12) count 4 angle 360 deg   // about a boss
split Halves target Shelf origin (160,0,0) normal (1,0,0) -> body ShelfLeft   // or plane Mid
split_face SplitA faces (30,20,20) of Block origin (20,0,0) normal (1,0,0)    // or plane XY / Mid
press_pull Channel face (30,20,20) distance (-5 mm)                            // + adds, - cuts
offset_surface Bore faces (84,20,15) (76,20,15) of Pin distance 0.3 mm -> body BoreSkin   // chain
color Shelf "#d9a441"
material Shelf, ShelfLeft PLA
```

- `split_face` divides faces with a plane without changing the solid, so `press_pull` can move
  just part of a face along its normal: `67_channel_block_and_sleeve` cuts a cable channel into
  the middle strip of a block's top. `offset_surface` copies faces along their normals as a new
  surface body (planes and cylinders exactly), for example a pin's side moved out by a clearance
  and thickened into a sleeve, which keeps the gap exact when the pin changes. A cylinder's side
  is two half-faces: pick both.
- A circular pattern or a revolve can turn about a model's own axis: `axis_face (x,y,z)` on a
  cylinder, cone or torus face, or `axis_edge (x,y,z)` on a straight or circular edge. The axis
  follows the model, so moving the boss moves the ring of holes around it. This needs engine
  0.27; older engines stop with the reason.
- `rib` takes one connected chain. `web` makes one wall per curve. `direction normal` grows
  along the sketch's normal, and `flip` turns it to the other side.
- `draft`: a positive angle narrows the part along the pull, which defaults to the neutral
  plane's normal; a negative angle widens it. Draft a solid before shelling it: the shell keeps
  the walls even. Drafting the faces of a shelled body together can fail (`54_nesting_tray`).
- A pattern along a path turns the copies to follow it unless it is `identical`.
- `split` keeps the target's name on the side the normal points to and names the other piece
  (`-> body`). Check each piece's size against the bed (`55_split_wall_shelf`).
- `color Body "#rrggbb" faces (x,y,z) …` colours only the faces at those points, over the body's
  colour. Use it for lettering tops or trim.
- `color` and `material` set what an export and the measured mass use. Materials come from the
  engine's library: `PLA`, `ABS Plastic`, `Nylon`, `Polycarbonate`, `Steel`, `Aluminum`, ….
- Engines before 0.27 cannot pattern or mirror a rib or web; repeat it instead. Naming a split
  piece needs engine 0.19 or later; 0.15 loses the name when it rebuilds.

## Solid features

```sc
loft Pot sections Base, Middle, Rim -> body Pot                 // 32_lofted_planter
sweep Hook from Profile path Path.a, Path.k, Path.b new -> body Hook   // 33_swept_wall_hook
pipe Tube path Path.a, Path.b diameter 6 wall 1 new -> body Tube
thread NeckThread face (rn, 0, z) designation M28x2 modeled     // 49_bottle_and_cap; `left … body B` for left-handed
coil Spring diameter 20 turns 5 pitch 4 section_size 1.5 base (0,0,0) new -> body Spring
emboss Letters from Label depth 1 targets Tag                   // 50_name_tag (deboss to cut in)
```

- `sweep` options are `twist <angle>`, `scale <factor>` and `parallel`, which keeps the profile
  from turning with the path. Path curves are listed in order and must all be in one sketch.
- A left-hand thread names its body (`left body Cap`). Engines from 0.27 cut it directly. Older
  engines cut it as the mirror image of a right-hand thread: they mirror the body through a plane
  at the face point, thread it and mirror it back, which gives the same surface. The copy is
  named `B (2)` along the way, so don't give another body that name.
- `thread … clearance c internal|external axis X|Y|Z origin (x,y,z) body B` opens the fit: the
  engine cuts the basic ISO profile, so B is scaled about the thread's axis, out (internal) or in
  (external), by c at the major radius, with a live factor. See 49's cap.
- `thread` without `modeled` is cosmetic: it is recorded on the face, and the geometry does not
  change. Modelled threads are slow to compute. Put them last, after every other boolean on that body.
- `coil` takes two of `turns`, `pitch` and `height`. Other options are `square`,
  `section_position inside|center|outside`, `axis`, `start_angle` and `clockwise`.

## Surfaces and thickening

```sc
revolve Skin from Profile axis Z curves s surface -> body Skin   // 63_thin_wall_vase
thicken Wall bodies Skin thickness wall new -> body Vase
remove DropSkin bodies Skin
```

`surface` on an `extrude`, `revolve`, `loft` or `sweep` makes a surface body, walls only and no
caps, instead of a solid. Its profile can be an open chain of sketch curves (`curves a, b`). A
surface is its own body: it takes `-> body <name>`, not `join`, `cut` or `targets`.

`thicken` grows surface bodies into a solid by `thickness`, square to the surface everywhere,
which gives an even wall that `shell` or a second, smaller cut can't. It grows along the surface's
normal; `flip` grows it the other way and `symmetric` half each way. A closed loft's normal points
out of it, and a revolve's depends on the profile's direction. The result is `new -> body <name>`,
or joins or cuts `targets`. Remove the surface afterwards, or it stays in the design as a body.
The wall's ends are square to the surface, so a sloping wall's end rises a little past the
profile.

The engine (0.19 and 0.27) can't thicken across a sharp edge into the inside corner, and it can't
join or cut a thickened loft afterwards, even when the loft is ruled. Thickened revolves, extrudes
and sweeps join and cut as usual.

```sc
loft Side sections Base, Rim surface -> body Knob                  // 65_domed_knob
patch Dome edges (11.3,11.3,18) (-11.3,11.3,18) (-11.3,-11.3,18) (11.3,-11.3,18) of Knob continuity g1 through (0,0,24) -> body DomeTop
patch Bottom from Base -> body BaseFace                             // planar regions of a sketch
stitch Close bodies Knob, DomeTop, BaseFace                         // sewn into Knob: a solid
```

`patch` fills a closed loop with a surface: the regions of a sketch (`from`, with `profiles` to
pick some), or a loop of a body's edges (`edges … of B`). Pick every edge of the loop; a loft's
or revolve's circular rim can be split into several edges, so pick points all the way round.
For an edge loop, `continuity g1` makes the patch meet the faces beside it without a crease
(`g2` also matches their curvature), `through (x,y,z) …` makes it pass through points, and
`crown` makes it fuller. A flat loop with `g0` and no points gives a flat face. Prefer `through`
to shape a dome: with g1 and only `crown`, the patch can bulge out past the loop.

`stitch` sews surfaces into the first body named; when they close, it is a solid that joins and
cuts as usual.

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

```sc
path_flange Z edge (20,30,0) path Zed.up, Zed.out radius 1.5 mm body Bracket
```

A path flange bends flanges along an open chain of sketch lines drawn across a sheet edge,
from the edge out: one flange per line, one bend per corner (a Z, a hat, a return lip). Engines
from 0.27 make it directly. On older ones the compiler writes the same chain as edge flanges,
each bent from the last one's free edge, as you would by hand:

```sc
flange Up edges (20,30,0) height 15 mm flip body Bracket
flange Out edges (20,30,15) height 20 mm body Bracket
```

Each flange of the chain starts at the start of its line, and its height is the line's length.
Whether to `flip` it follows one rule:

- The first flange flips when it folds toward the sheet's top face (the normal of the sketch its
  base flange came from).
- Each later flange flips when it heads opposite to the segment two before it. For the second
  flange, that segment is the plate, running toward the edge (away from the middle of the base
  flange's sketch).

For example, a hat section (up, out, down) from a plate on XY is `flip`, plain, `flip`. The
chain matched 0.27's path flange exactly (area, bounds and centre of mass) on engines 0.15 and
0.19 for a Z, a hat, a return lip, a downward Z, a chain from the plate's other edge, a plate
sketched on XZ and a 3 mm bend radius. For that, the path's lines need coordinates at both ends,
and the sketches must be on XY, XZ or YZ (offset, not flipped); otherwise an older engine stops
with the reason. The heights stay live. As with every flange, the edges are picked at the
coordinates' current values. See `62_hat_channel_mount`.

A solid or shelled body can also become sheet metal after it is modelled:

```sc
convert Sheet body Tray face (L/2, W/2, 0) rule Steel bends (L/2, 0, 0) (L/2, W, 0) (0, W/2, 0) (L, W/2, 0)
```

`face` is the fixed face. The flat faces reached from it across the `bends` edges become the
sheet, and each bend gets the rule's radius. Edges between those faces that aren't bends, such as
a tray's wall corners, are cut open with the rule's gap. A shelled body keeps its wall thickness;
a solid plate takes the rule's thickness. Holes cut square through the faces become cut-outs in
the flat pattern. See `64_bent_tray`, whose DXF has the outline, the holes, the vents and the
four bend lines.

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

## Any other engine command

```sc
engine construct.axis.through_cylinder {"name": "Spin", "face": {"face": [6, 0, 11]}}
pattern circular Ring features Hole axis Spin count 6 angle 360 deg
engine solid.bounding_solid {"bodies": ["Part"], "margin": "2 mm", "name": "Envelope", "body_name": "Box"}
```

`engine <command> {json}` adds any SolidCraft command to the design as written, for what has no
statement of its own (the engine lists them with `solidcraft-cli commands`). The parameters are
one JSON object on the line. Expressions go in strings, as in `"2 mm"` or `"wall/2"`, and the
engine evaluates them. A `name` names the feature for later statements; a `body_name` declares
the body it makes, so checks and later features can use it. Engine errors point at the line.

`ci/engine-sweep.py` checks the whole command list of an engine: every command compiles through
`engine` unchanged (542 of 542 on 0.27.1), and every solid, surface, construction, sheet and
sketch command reaches the engine and is recognised (202 of 202).

A construction plane made this way (`construct.plane.*` with a `name`) can carry sketches like a
`plane` statement's. Inside a sketch block, `engine` runs a sketch command in that sketch; like
text and imports, it comes after the sketch's own entities, and it refers to entities by the
engine's ids (`c1`, `l3.end`). A finished sketch can also be reopened with `engine sketch.edit
{"sketch": "S"}` … `engine sketch.finish {}`. `66_engine_commands` uses seven such commands
(split face, press-pull, surface offset, boundary fill, tangent plane, a three-point circle, sheet
unfold) and checks each result; it passes on 0.27 and 0.19. `68_more_engine_commands` adds eight
more (surface extend, trim, ruled and reverse normal; an axis from two planes driving a pattern;
sheet refold; sketch trim; solid align), 24 checks on 0.27. `69_construction_geometry` covers
construction geometry: offset, midplane and three-point planes, construction points, axes along an
edge and square to a face, plus sheet fold, surface sweep and replace face, 25 checks on 0.27.
The engine picks a construction point by where it is (`{"point": [x,y,z]}`), not by its name. A pattern about a named construction
axis needs 0.27; older engines stop with the reason. Two things it shows: a cylinder's
side is two half-faces, so pick both; and the engine integrates the volume of spline and offset
faces to about 0.01 %, so check those with a range.

The compiler can't check these parameters, so a wrong pick is the engine's to report, or not:
a construction axis through two `vertex` points where the body has no vertex snaps to other
vertices without an error. Measure the result, as with any feature.
