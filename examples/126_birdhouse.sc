// 126. Nest box for small cavity-nesting birds, sized from a species, with a swing-out clean-out door.
//
// X runs across the box (the clean-out door is the +X side), Y from the front (y = 0, the entry
// hole) to the back (the mounting wall), Z up. The body is a front, back, floor and fixed side
// in one piece; its top slopes from front_h at the front up to back_h at the back. The roof is a
// separate plate overhanging the front and sides, screwed down into two rails inside the top of
// the front and back walls. The +X side is a door panel between the front and back walls that
// swings out at the bottom on two pivot pins near its top, and is held shut by a latch pin
// pushed in through the front wall. No perch (it only helps predators). Vent slots under the
// roof, four drain holes in the floor, and climbing grooves inside the front wall below the hole.
//
// species: 1 house wren, 2 chickadee, 3 eastern bluebird. Entry hole, floor, hole height and wall
// heights follow typical nest-box guidance for each; check your local recommendations.
//
// Made from: printed, in PETG or ASA for outdoor use (PLA softens in the sun), not cut from cedar.
// Four printed parts, each within a 256 mm bed:
//   Body  lying on its fixed (-X) side, open side up: every wall then stands vertical; the entry
//         hole's top prints as a short circular overhang, which bridges fine at 0.2 mm.
//   Door  flat on its inside face (the grip up).   Roof  flat on its underside.
// 3-4 perimeters, 20% infill. BOM: 2 x pivot pins and 1 x latch pin (2.5 mm x 40 mm nails, or
// 1.75 mm filament for the pivots), 4 x #4 x 1/2" (M3 x 12) screws for the roof, 2 x #8 wood
// screws through the back wall to mount it (drive them from inside, through the open door).
design Birdhouse units mm

param species = 3
param wall = 4 mm
param roof_t = 5 mm
param roof_angle = 9 deg        // roof slope; the back wall stands taller than the front
param front_over = 45 mm       // roof overhang past the front
param side_over = 12 mm
param gap = 0.6 mm             // door clearance all round
param pin_d = 2.8 mm           // pivot and latch pin holes

if species == 1 {
  let hole_d = 28.6 mm         // 1 1/8 in
  let floor_w = 102 mm         // 4 x 4 in
  let hole_h = 127 mm
  let front_h = 178 mm
}
if species == 2 {
  let hole_d = 28.6 mm
  let floor_w = 102 mm
  let hole_h = 165 mm
  let front_h = 203 mm
}
if species == 3 {
  let hole_d = 38.1 mm         // 1 1/2 in
  let floor_w = 127 mm         // 5 x 5 in
  let hole_h = 165 mm
  let front_h = 229 mm
}

let ext = floor_w + 2*wall     // outside width and depth
let slope = tan(roof_angle)
let back_h = front_h + ext*slope
let pivot_z = front_h - 16 mm
let latch_z = wall + 18 mm

// ==== Body: the side profile extruded across, then shelled open at the top and the door side
sketch SideSk on YZ {
  polyline closed p = (0,0) (ext,0) (ext,back_h) (0,front_h)
  fix p
}
extrude BodyEx from SideSk distance ext new -> body Body
shell Hollow faces (ext/2, ext/2, front_h + slope*ext/2) (ext, ext/2, front_h/2) thickness wall inside body Body

// Roof rails inside the top of the front and back walls, from the fixed side to short of the door
box FrontRail size (floor_w - 0.3 mm) 10.2 mm 11 mm corner (wall - 0.2 mm, wall - 0.2 mm, front_h - 12 mm) join targets Body
box BackRail size (floor_w - 0.3 mm) 10.2 mm 11 mm corner (wall - 0.2 mm, ext - wall - 10 mm, back_h - (wall + 10 mm)*slope - 12 mm) join targets Body

// Entry hole, climbing grooves inside the front, vents under the roof, drain holes, mounting holes
cylinder Entry radius (hole_d/2) height (wall + 2 mm) base (ext/2, -1 mm, hole_h) axis (0,1,0) cut targets Body
for k from 0 to 4 {
  box Groove{k} size (floor_w*0.6) 1.2 mm 2 mm corner (wall + floor_w*0.2, wall - 1 mm, hole_h - hole_d/2 - 20 mm - k*16 mm) cut targets Body
}
box VentFixed size (wall + 2 mm) (floor_w - 40 mm) 8 mm corner (-1 mm, wall + 20 mm, front_h - 30 mm) cut targets Body
box VentBack size (floor_w - 40 mm) (wall + 2 mm) 8 mm corner (wall + 20 mm, ext - wall - 1 mm, back_h - 40 mm) cut targets Body
for k from 0 to 1 {
  for j from 0 to 1 {
    cylinder Drain{k}{j} radius 3 mm height (wall + 2 mm) base (wall + 12 mm + k*(floor_w - 24 mm), wall + 12 mm + j*(floor_w - 24 mm), -1 mm) axis (0,0,1) cut targets Body
  }
}
cylinder MountHi radius 2.4 mm height (wall + 2 mm) base (ext/2, ext - wall - 1 mm, back_h - 45 mm) axis (0,1,0) cut targets Body
cylinder MountLo radius 2.4 mm height (wall + 2 mm) base (ext/2, ext - wall - 1 mm, 40 mm) axis (0,1,0) cut targets Body

// ==== Door: the +X side, between the front and back walls, above the floor, under the roof line
sketch DoorSk on YZ offset (ext - wall) {
  polyline closed d = (wall + gap, wall + gap) (ext - wall - gap, wall + gap) (ext - wall - gap, front_h + slope*(ext - wall - gap) - gap - 1 mm) (wall + gap, front_h + slope*(wall + gap) - gap - 1 mm)
  fix d
}
extrude DoorEx from DoorSk distance wall new -> body Door
box DoorGrip size 3 mm 30 mm 10 mm corner (ext - 0.2 mm, ext/2 - 15 mm, wall + 8 mm) join targets Door

// Pivot pins run front to back through both walls and the door's top; the latch pin goes in
// through the front wall into the door's front edge near the bottom
cylinder Pivot radius (pin_d/2) height (ext + 2 mm) base (ext - wall/2, -1 mm, pivot_z) axis (0,1,0) cut targets Body, Door
cylinder Latch radius (pin_d/2) height (wall + 15 mm) base (ext - wall/2, -1 mm, latch_z) axis (0,1,0) cut targets Body, Door

// ==== Roof: a sloped plate resting on the wall tops, overhanging the front and sides
sketch RoofSk on YZ offset (-side_over) {
  polyline closed r = (-front_over, front_h - front_over*slope) (ext + 3 mm, back_h + 3 mm*slope) (ext + 3 mm, back_h + 3 mm*slope + roof_t) (-front_over, front_h - front_over*slope + roof_t)
  fix r
}
extrude RoofEx from RoofSk distance (ext + 2*side_over) new -> body Roof
for k from 0 to 1 {
  cylinder RoofHoleF{k} radius 1.8 mm height (roof_t + 30 mm) base (wall + floor_w*(0.25 + 0.5*k), wall + 5 mm, front_h - 14 mm) axis (0,0,1) cut targets Roof
  cylinder RoofPilotF{k} radius 1.25 mm height 12 mm base (wall + floor_w*(0.25 + 0.5*k), wall + 5 mm, front_h - 13 mm) axis (0,0,1) cut targets Body
  cylinder RoofHoleB{k} radius 1.8 mm height (roof_t + 40 mm) base (wall + floor_w*(0.25 + 0.5*k), ext - wall - 5 mm, back_h - (wall + 10 mm)*slope - 14 mm) axis (0,0,1) cut targets Roof
  cylinder RoofPilotB{k} radius 1.25 mm height 12 mm base (wall + floor_w*(0.25 + 0.5*k), ext - wall - 5 mm, back_h - (wall + 10 mm)*slope - 13 mm) axis (0,0,1) cut targets Body
}

// The door must swing clear of the body and the roof
check Door clearance Body >= 0.4 mm
check Door clearance Roof >= 0.4 mm

color Body "#c9a978"
color Door "#b8955f"
color Roof "#6b4f37"
material Body, Door, Roof "ABS Plastic"
