// 101. Cantilever tube chair: one bent steel tube per side, no back legs, canvas slings.
//
// Z is up, the sitter faces -Y, the chair is centred on x = 0. Each side frame is one length of
// tube bent four times in a vertical plane: a floor runner from the back to the front, a bend up
// into the front upright, which leans back to the seat front, a bend back into the seat rail, and a
// bend up into the back post. The seat hangs off the front uprights with nothing under its back
// edge, which is what makes it a cantilever: the floor runners carry on back under the seat so the
// chair can't tip. Two straight cross tubes, one at the back of the runners and one across the top
// of the back, tie the sides into one welded frame. Canvas slings wrap the seat rails and back posts.
//
// Make it: 25.4 x 1.65 mm (1 in, 16 ga) mild steel or 304 stainless tube. Each side is one piece
// cut to about 1770 mm (the centre-line length at the default sizes, plus 100 mm for the bender's grip, trimmed after bending), bent on a manual bender with a
// 76 mm (3 x OD) centre-line die for the seat and back bends and a 100 mm die for the floor bend,
// bending in order from the floor runner up and checking each angle against a full-size drawing of
// this side view. The cross tubes are cut to about 455 mm (`width` minus one OD), coped to 25.4 mm and TIG or MIG
// welded on; grind and powder coat or polish. The slings are 10 oz canvas or 2 mm leather, sewn
// into sleeves that slide over the rails before the top cross tube is welded (or laced closed after).
// Four printed glides (PETG or TPU) clip onto the floor runners; they have a print recipe.
//
// BOM: 2 x side tube about 1.87 m before trimming, 2 x cross tube `width - 25.4` mm (all 25.4 x 1.65 tube);
// seat sling and back sling (canvas or leather, sizes from `seat_w`, the rail and post lengths);
// 4 printed glides; welding wire, primer, paint.
design CantileverChair units mm

param width = 480 mm           // between tube centres, side to side
param seat_h = 450 mm          // top of the seat sling at its front edge
param tube_od = 25.4 mm
param tube_wall = 1.65 mm
param glide_t = 3 mm           // glide floor pad under the runners

// Side view (y forward is -, z up), tube centre line
param front_y = -300 mm        // floor runner's front bend
param rear_y = 260 mm          // back end of the runner, under the seat's back edge
param lean_y = -250 mm         // seat front: the upright leans back from the floor bend to here
param seat_back_y = 190 mm
param seat_drop = 30 mm        // the seat rail falls this much toward the back
param back_top_y = 260 mm
param back_top_z = 860 mm
param floor_bend = 100 mm      // centre-line bend radii
param seat_bend = 76 mm
param back_bend = 76 mm

let tube_r = tube_od/2
let floor_z = glide_t + tube_r
let rail_z = seat_h - 3 mm - tube_r
let seat_back_z = rail_z - seat_drop

let b1_d1 = hypot((rear_y) - (front_y), (floor_z) - (floor_z))
let b1_d2 = hypot((lean_y) - (front_y), (rail_z) - (floor_z))
let b1_u1y = ((rear_y) - (front_y))/b1_d1
let b1_u1z = ((floor_z) - (floor_z))/b1_d1
let b1_u2y = ((lean_y) - (front_y))/b1_d2
let b1_u2z = ((rail_z) - (floor_z))/b1_d2
let b1_half = acos(b1_u1y*b1_u2y + b1_u1z*b1_u2z)/2
let b1_t = (floor_bend)/tan(b1_half)
let b1_bl = hypot(b1_u1y + b1_u2y, b1_u1z + b1_u2z)
let b1_m = (floor_bend)/sin(b1_half) - (floor_bend)
let b1_ay = (front_y) + b1_u1y*b1_t
let b1_az = (floor_z) + b1_u1z*b1_t
let b1_by = (front_y) + b1_u2y*b1_t
let b1_bz = (floor_z) + b1_u2z*b1_t
let b1_my = (front_y) + (b1_u1y + b1_u2y)/b1_bl*b1_m
let b1_mz = (floor_z) + (b1_u1z + b1_u2z)/b1_bl*b1_m
let b2_d1 = hypot((front_y) - (lean_y), (floor_z) - (rail_z))
let b2_d2 = hypot((seat_back_y) - (lean_y), (seat_back_z) - (rail_z))
let b2_u1y = ((front_y) - (lean_y))/b2_d1
let b2_u1z = ((floor_z) - (rail_z))/b2_d1
let b2_u2y = ((seat_back_y) - (lean_y))/b2_d2
let b2_u2z = ((seat_back_z) - (rail_z))/b2_d2
let b2_half = acos(b2_u1y*b2_u2y + b2_u1z*b2_u2z)/2
let b2_t = (seat_bend)/tan(b2_half)
let b2_bl = hypot(b2_u1y + b2_u2y, b2_u1z + b2_u2z)
let b2_m = (seat_bend)/sin(b2_half) - (seat_bend)
let b2_ay = (lean_y) + b2_u1y*b2_t
let b2_az = (rail_z) + b2_u1z*b2_t
let b2_by = (lean_y) + b2_u2y*b2_t
let b2_bz = (rail_z) + b2_u2z*b2_t
let b2_my = (lean_y) + (b2_u1y + b2_u2y)/b2_bl*b2_m
let b2_mz = (rail_z) + (b2_u1z + b2_u2z)/b2_bl*b2_m
let b3_d1 = hypot((lean_y) - (seat_back_y), (rail_z) - (seat_back_z))
let b3_d2 = hypot((back_top_y) - (seat_back_y), (back_top_z) - (seat_back_z))
let b3_u1y = ((lean_y) - (seat_back_y))/b3_d1
let b3_u1z = ((rail_z) - (seat_back_z))/b3_d1
let b3_u2y = ((back_top_y) - (seat_back_y))/b3_d2
let b3_u2z = ((back_top_z) - (seat_back_z))/b3_d2
let b3_half = acos(b3_u1y*b3_u2y + b3_u1z*b3_u2z)/2
let b3_t = (back_bend)/tan(b3_half)
let b3_bl = hypot(b3_u1y + b3_u2y, b3_u1z + b3_u2z)
let b3_m = (back_bend)/sin(b3_half) - (back_bend)
let b3_ay = (seat_back_y) + b3_u1y*b3_t
let b3_az = (seat_back_z) + b3_u1z*b3_t
let b3_by = (seat_back_y) + b3_u2y*b3_t
let b3_bz = (seat_back_z) + b3_u2z*b3_t
let b3_my = (seat_back_y) + (b3_u1y + b3_u2y)/b3_bl*b3_m
let b3_mz = (seat_back_z) + (b3_u1z + b3_u2z)/b3_bl*b3_m

// ==== Frame: the two bent sides and the two cross tubes, each its own member of the weldment
sketch LeftSk on YZ offset (-width/2) {
  line l1 = (rear_y, floor_z) (b1_ay, b1_az)
  arc a1 = start l1.end through (b1_my, b1_mz) end (b1_by, b1_bz)
  line l2 = a1.end (b2_ay, b2_az)
  arc a2 = start l2.end through (b2_my, b2_mz) end (b2_by, b2_bz)
  line l3 = a2.end (b3_ay, b3_az)
  arc a3 = start l3.end through (b3_my, b3_mz) end (b3_by, b3_bz)
  line l4 = a3.end (back_top_y, back_top_z)
  fix l1 a1 l2 a2 l3 a3 l4
}
pipe LeftTube path LeftSk.l1, LeftSk.a1, LeftSk.l2, LeftSk.a2, LeftSk.l3, LeftSk.a3, LeftSk.l4 diameter tube_od wall tube_wall new -> body LeftSide
sketch CrossSk on XY offset floor_z {
  line rear = (-width/2 + tube_r - 0.3 mm, rear_y - tube_r - 0.5 mm) (width/2 - tube_r + 0.3 mm, rear_y - tube_r - 0.5 mm)
  fix rear
}
pipe RearCross path CrossSk.rear diameter tube_od wall tube_wall new -> body RearCross
sketch TopCrossSk on XY offset (back_top_z - tube_r - 0.5 mm) {
  line top = (-width/2 + tube_r - 0.3 mm, back_top_y) (width/2 - tube_r + 0.3 mm, back_top_y)
  fix top
}
pipe TopCross path TopCrossSk.top diameter tube_od wall tube_wall new -> body TopCross
sketch RightSk on YZ offset (width/2) {
  line l1 = (rear_y, floor_z) (b1_ay, b1_az)
  arc a1 = start l1.end through (b1_my, b1_mz) end (b1_by, b1_bz)
  line l2 = a1.end (b2_ay, b2_az)
  arc a2 = start l2.end through (b2_my, b2_mz) end (b2_by, b2_bz)
  line l3 = a2.end (b3_ay, b3_az)
  arc a3 = start l3.end through (b3_my, b3_mz) end (b3_by, b3_bz)
  line l4 = a3.end (back_top_y, back_top_z)
  fix l1 a1 l2 a2 l3 a3 l4
}
pipe RightTube path RightSk.l1, RightSk.a1, RightSk.l2, RightSk.a2, RightSk.l3, RightSk.a3, RightSk.l4 diameter tube_od wall tube_wall new -> body RightSide

// ==== Slings: 3 mm canvas wrapping the seat rails and the back posts
sketch SeatSlingSk on YZ offset (-width/2 - tube_r - 1 mm) {
  polyline closed s = (b2_by, b2_bz + tube_r) (b3_ay, b3_az + tube_r) (b3_ay, b3_az + tube_r + 3 mm) (b2_by, b2_bz + tube_r + 3 mm)
  fix s
}
extrude SeatSling from SeatSlingSk distance (width + tube_od + 2 mm) new -> body SeatSling
sketch BackSlingSk on YZ offset (-width/2 - tube_r - 1 mm) {
  polyline closed s = (b3_by - tube_r*b3_u2z/1, b3_bz + tube_r*b3_u2y) (back_top_y - tube_r*b3_u2z, back_top_z - 60 mm + tube_r*b3_u2y) (back_top_y - (tube_r + 3 mm)*b3_u2z, back_top_z - 60 mm + (tube_r + 3 mm)*b3_u2y) (b3_by - (tube_r + 3 mm)*b3_u2z, b3_bz + (tube_r + 3 mm)*b3_u2y)
  fix s
}
extrude BackSling from BackSlingSk distance (width + tube_od + 2 mm) new -> body BackSling

// ==== Glides: a printed saddle clipped round each floor runner, a flat pad under it
for k from 0 to 3 {
  sketch GlideSk{k} on XZ offset (front_y + 90 mm + floor(k/2)*(rear_y - front_y - 160 mm)) {
    circle o = ((2*mod(k, 2) - 1)*width/2, -floor_z) radius (tube_r + 2.5 mm)
    rect pad = ((2*mod(k, 2) - 1)*width/2 - tube_r - 2 mm, 0) ((2*mod(k, 2) - 1)*width/2 + tube_r + 2 mm, -(floor_z - 2 mm))
    fix o pad
  }
  extrude GlideBody{k} from GlideSk{k} distance 40 mm new -> body Glide{k}
  cylinder GlideBore{k} radius (tube_r + 0.15 mm) height 42 mm base ((2*mod(k, 2) - 1)*width/2, front_y + 89 mm + floor(k/2)*(rear_y - front_y - 160 mm), floor_z) axis (0,1,0) cut targets Glide{k}
  box GlideMouth{k} size (tube_od*0.8) 42 mm 20 mm corner ((2*mod(k, 2) - 1)*width/2 - tube_od*0.4, front_y + 89 mm + floor(k/2)*(rear_y - front_y - 160 mm), floor_z + tube_r*0.55) cut targets Glide{k}
  color Glide{k} "#2b2b2b"
}

color LeftSide, RightSide, RearCross, TopCross "#c9ccd0"
material LeftSide, RightSide, RearCross, TopCross Steel
color SeatSling, BackSling "#d8cdb8"
