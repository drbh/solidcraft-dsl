// 36. Elliptical soap dish with a sloped, slotted floor, drying ribs and four feet.
//
// Z is up, the dish is centred on the origin with its long axis along X. The outline, the cavity
// and the clip for the floor insert are ellipses drawn as periodic sketch splines through 16
// points (rx cos t, ry sin t), which follow a true ellipse to well under 0.1 mm at this size.
// (The sketch `ellipse` entity would be simpler, but its minor radius does not accept parameter
// expressions yet, and these semi-axes follow len, wid and wall.) The walls are
// extruded with an 8 deg outward draft. Inside, the floor is a shallow V that falls
// from both ends toward the minor axis, where a row of drain slots runs through the floor. Raised
// ribs run along X between the slots with flat tops, so the soap rests level and clear of the wet
// floor while water runs down between them to the slots. Underneath, a cross of shallow channels
// splits the base into four feet and lets the drained water run out from under the dish.
//
// Print upright as modelled, no supports: the walls lean out only 8 deg, the ribs and the V floor
// grow straight up from the base, and the only ceilings are the two underside channels, short
// flat bridges of chan_w and chan2_w (18 and 10 mm by default).
design SoapDish units mm
// Size (outline at the base; the draft widens the rim)
param len = 120 mm
param wid = 85 mm
param height = 24 mm
param wall = 2.4 mm
param draft = 8 deg
// Base: feet plus floor
param foot_h = 2.5 mm
param floor_t = 2.4 mm
// Floor slope, ribs and drain slots
param drop = 3 mm
param rib_w = 2 mm
param rib_h = 2.5 mm
param pitch = 8 mm
param slot_len = 16 mm
param slot_w = 3 mm
param n_slots = 9
// Underside channels between the feet
param chan_w = 18 mm
param chan2_w = 10 mm
param rim_r = 1 mm
let rx = len/2
let ry = wid/2
let rxi = rx - wall
let ryi = ry - wall
let base_t = foot_h + floor_t
let rib_top = base_t + drop + rib_h
let slot_y0 = -(n_slots - 1)*pitch/2
let rim_dx = height*tan(draft)

// Shell: drafted elliptical block, hollowed down to the base
sketch OuterSk on XY {
  spline outer = (rx*cos(0 deg),ry*sin(0 deg)) (rx*cos(22.5 deg),ry*sin(22.5 deg)) (rx*cos(45 deg),ry*sin(45 deg)) (rx*cos(67.5 deg),ry*sin(67.5 deg)) (rx*cos(90 deg),ry*sin(90 deg)) (rx*cos(112.5 deg),ry*sin(112.5 deg)) (rx*cos(135 deg),ry*sin(135 deg)) (rx*cos(157.5 deg),ry*sin(157.5 deg)) (rx*cos(180 deg),ry*sin(180 deg)) (rx*cos(202.5 deg),ry*sin(202.5 deg)) (rx*cos(225 deg),ry*sin(225 deg)) (rx*cos(247.5 deg),ry*sin(247.5 deg)) (rx*cos(270 deg),ry*sin(270 deg)) (rx*cos(292.5 deg),ry*sin(292.5 deg)) (rx*cos(315 deg),ry*sin(315 deg)) (rx*cos(337.5 deg),ry*sin(337.5 deg)) periodic
  fix outer.1 outer.2 outer.3 outer.4 outer.5 outer.6 outer.7 outer.8 outer.9 outer.10 outer.11 outer.12 outer.13 outer.14 outer.15 outer.16
}
extrude Outer from OuterSk distance height taper draft new -> body Dish
sketch CavitySk on XY offset base_t {
  spline inner = (rxi*cos(0 deg),ryi*sin(0 deg)) (rxi*cos(22.5 deg),ryi*sin(22.5 deg)) (rxi*cos(45 deg),ryi*sin(45 deg)) (rxi*cos(67.5 deg),ryi*sin(67.5 deg)) (rxi*cos(90 deg),ryi*sin(90 deg)) (rxi*cos(112.5 deg),ryi*sin(112.5 deg)) (rxi*cos(135 deg),ryi*sin(135 deg)) (rxi*cos(157.5 deg),ryi*sin(157.5 deg)) (rxi*cos(180 deg),ryi*sin(180 deg)) (rxi*cos(202.5 deg),ryi*sin(202.5 deg)) (rxi*cos(225 deg),ryi*sin(225 deg)) (rxi*cos(247.5 deg),ryi*sin(247.5 deg)) (rxi*cos(270 deg),ryi*sin(270 deg)) (rxi*cos(292.5 deg),ryi*sin(292.5 deg)) (rxi*cos(315 deg),ryi*sin(315 deg)) (rxi*cos(337.5 deg),ryi*sin(337.5 deg)) periodic
  fix inner.1 inner.2 inner.3 inner.4 inner.5 inner.6 inner.7 inner.8 inner.9 inner.10 inner.11 inner.12 inner.13 inner.14 inner.15 inner.16
}
extrude Cavity from CavitySk distance height taper draft cut targets Dish

// Rim: round the top edges
fillet Rim edges (0,ry + rim_dx,height) (0,ryi + rim_dx,height) radius rim_r body Dish

// Floor insert: a V wedge falling toward x = 0, plus ribs along X between the slot positions,
// clipped to the cavity outline and joined to the dish. (XZ sketch coordinates are (x, -z).)
sketch WedgeSk on XZ offset (-ry) {
  polyline closed v = (-rx,-base_t + 0.1 mm) (-rx,-base_t - drop) (0,-base_t) (rx,-base_t - drop) (rx,-base_t + 0.1 mm)
  fix v.1 v.2 v.3 v.4 v.5
}
extrude Wedge from WedgeSk distance wid new -> body Insert
sketch RibSk on YZ offset (-rx + 1 mm) {
  rect r = (slot_y0 - pitch/2 - rib_w/2,base_t - 0.1 mm) (slot_y0 - pitch/2 + rib_w/2,rib_top)
  fix r.bottom r.right r.top r.left
}
extrude Rib from RibSk distance (len - 2 mm) join targets Insert
pattern Ribs features Rib dir (0,1,0) count (n_slots + 1) spacing pitch
sketch ClipSk on XY offset (base_t - 0.2 mm) {
  spline clip = ((rxi + 0.2 mm)*cos(0 deg),(ryi + 0.2 mm)*sin(0 deg)) ((rxi + 0.2 mm)*cos(22.5 deg),(ryi + 0.2 mm)*sin(22.5 deg)) ((rxi + 0.2 mm)*cos(45 deg),(ryi + 0.2 mm)*sin(45 deg)) ((rxi + 0.2 mm)*cos(67.5 deg),(ryi + 0.2 mm)*sin(67.5 deg)) ((rxi + 0.2 mm)*cos(90 deg),(ryi + 0.2 mm)*sin(90 deg)) ((rxi + 0.2 mm)*cos(112.5 deg),(ryi + 0.2 mm)*sin(112.5 deg)) ((rxi + 0.2 mm)*cos(135 deg),(ryi + 0.2 mm)*sin(135 deg)) ((rxi + 0.2 mm)*cos(157.5 deg),(ryi + 0.2 mm)*sin(157.5 deg)) ((rxi + 0.2 mm)*cos(180 deg),(ryi + 0.2 mm)*sin(180 deg)) ((rxi + 0.2 mm)*cos(202.5 deg),(ryi + 0.2 mm)*sin(202.5 deg)) ((rxi + 0.2 mm)*cos(225 deg),(ryi + 0.2 mm)*sin(225 deg)) ((rxi + 0.2 mm)*cos(247.5 deg),(ryi + 0.2 mm)*sin(247.5 deg)) ((rxi + 0.2 mm)*cos(270 deg),(ryi + 0.2 mm)*sin(270 deg)) ((rxi + 0.2 mm)*cos(292.5 deg),(ryi + 0.2 mm)*sin(292.5 deg)) ((rxi + 0.2 mm)*cos(315 deg),(ryi + 0.2 mm)*sin(315 deg)) ((rxi + 0.2 mm)*cos(337.5 deg),(ryi + 0.2 mm)*sin(337.5 deg)) periodic
  fix clip.1 clip.2 clip.3 clip.4 clip.5 clip.6 clip.7 clip.8 clip.9 clip.10 clip.11 clip.12 clip.13 clip.14 clip.15 clip.16
}
extrude Clip from ClipSk distance (rib_top - base_t + 1 mm) taper draft new -> body ClipBody
combine ClipInsert target Insert tools ClipBody intersect
combine JoinInsert target Dish tools Insert join

// Drain slots across the bottom of the V, between the ribs
sketch SlotSk on XY offset (-1 mm) {
  slot s = centers (-slot_len/2 + slot_w/2,slot_y0) (slot_len/2 - slot_w/2,slot_y0) width slot_w
  fix s.line1 s.line2 s.arc1 s.arc2
}
extrude DrainSlot from SlotSk distance (base_t + drop + 1 mm) cut targets Dish
pattern DrainSlots features DrainSlot dir (0,1,0) count n_slots spacing pitch

// Underside: a cross of channels leaves four feet and vents the slots (the cross channel is
// 0.5 mm shallower so the two ceilings are not coplanar)
box ChannelY size chan_w (wid + 10 mm) (foot_h + 0.01 mm) corner (-chan_w/2,-ry - 5 mm,-0.01 mm) cut targets Dish
box ChannelX size (len + 10 mm) chan2_w (foot_h - 0.5 mm) corner (-rx - 5 mm,-chan2_w/2,-0.01 mm) cut targets Dish
