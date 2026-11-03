// 116. Self-watering planter: a reservoir, a pot seated in it, a wick cup and a fill spout; stackable.
//
// Z is up, everything is round about the Z axis. The Reservoir is an open tub. The Pot sits in
// it on a 45 deg shoulder that rests on the reservoir's inner rim, so its floor hangs pot_gap
// above the water. The WickCup drops through the pot floor to just above the reservoir floor:
// filled with potting mix it wicks water up into the pot through its slotted wall. Water goes in
// through the Spout, a tube that passes down through a hole in the pot's shoulder to the floor,
// and an overflow hole in the reservoir wall below the pot floor keeps the roots out of the water.
//
// Stacking: the reservoir's foot steps in to fit inside the pot's top rim (with clearance), so a
// second planter stands on the first as a tower, or empty units nest for storage.
//
// Print (PLA or PETG, 0.2 mm layers, no supports): the Reservoir and the Pot upright on their own
// plates (each is 140 mm across); every overhang is 45 deg. The WickCup prints collar down and the
// Spout funnel down. Seal the reservoir with 3 or more perimeters, or a coat of epoxy if it weeps.
// Nothing to buy; use a loose, peat-free mix in the wick cup.
design SelfWateringPlanter units mm

param res_r = 70 mm          // outer radius of reservoir and pot rim
param res_h = 70 mm          // reservoir height
param wall = 2.4 mm
param foot_h = 6 mm          // height of the stepped stacking foot
param fit = 0.4 mm           // radial clearance of the foot in the rim below
param pot_floor_z = 32 mm    // underside of the pot floor
param pot_bottom_r = 45 mm
param pot_top_z = 130 mm
param shoulder = 15 mm       // 45 deg shoulder run below the seat
param wick_r = 15 mm
param spout_r = 57 mm        // radius the spout stands at
param spout_od = 10 mm
param overflow_d = 6 mm

let seat_r = res_r - wall            // the reservoir's inner rim, where the pot sits
let seat_z = res_h + 0.2 mm
let foot_r = res_r - wall - fit

// ==== Reservoir: one revolved wall section; the foot steps in, then 45 deg out to the wall
sketch ResSk on XZ {
  polyline closed o = (0, 0) (foot_r, 0) (foot_r, -foot_h) (res_r, -(foot_h + res_r - foot_r)) (res_r, -res_h) (seat_r, -res_h) (seat_r, -(foot_h + res_r - foot_r + 1 mm)) (foot_r - wall, -(foot_h + 1 mm)) (foot_r - wall, -wall) (0, -wall)
  fix o
}
revolve Reservoir from ResSk axis Z -> body Reservoir
cylinder Overflow radius (overflow_d/2) height (wall + 4 mm) base (res_r - wall - 2 mm, 0, pot_floor_z - 4 mm) axis (1,0,0) cut targets Reservoir

// ==== Pot: floor, flared wall, 45 deg shoulder onto the seat, straight rim up to the top
sketch PotSk on XZ {
  polyline closed o = (0, -pot_floor_z) (pot_bottom_r, -pot_floor_z) (seat_r - shoulder, -(seat_z - shoulder)) (seat_r, -seat_z) (res_r, -(seat_z + wall)) (res_r, -pot_top_z) (res_r - wall, -pot_top_z) (res_r - wall, -(seat_z + wall + 1.4 mm)) (seat_r - shoulder - wall*1.4, -(seat_z - shoulder + 1 mm)) (pot_bottom_r - wall, -(pot_floor_z + wall)) (0, -(pot_floor_z + wall))
  fix o
}
revolve Pot from PotSk axis Z -> body Pot
// wick hole, air holes in the floor, and the spout's way through the shoulder
sketch PotHolesSk on XY offset (pot_floor_z - 1 mm) {
  circle w = (0, 0) radius (wick_r + 0.3 mm)
  circle a = (32 mm, 0) radius 2.5 mm
  point c = (0, 0)
  fix w a c
  pattern circular a count 6 center c angle 360 deg
}
extrude PotHoles from PotHolesSk distance (wall + 2 mm) cut targets Pot
cylinder SpoutHole radius (spout_od/2 + 0.3 mm) height (pot_top_z - pot_floor_z) base (spout_r, 0, pot_floor_z + 10 mm) axis (0,0,1) cut targets Pot

// ==== Wick cup: a slotted tube through the floor, held by a collar on the pot floor
sketch WickSk on XZ {
  polyline closed o = (0, -(wall + 2 mm)) (wick_r, -(wall + 2 mm)) (wick_r, -(pot_floor_z + wall + 0.2 mm)) (wick_r + 6 mm, -(pot_floor_z + wall + 0.2 mm)) (wick_r + 6 mm, -(pot_floor_z + wall + 2.2 mm)) (wick_r - 1.6 mm, -(pot_floor_z + wall + 2.2 mm)) (wick_r - 1.6 mm, -(wall + 3.6 mm)) (0, -(wall + 3.6 mm))
  fix o
}
revolve WickCup from WickSk axis Z -> body WickCup
// four slots: two boxes across the tube, one along X and one along Y
box WickSlotX size (2*wick_r + 4 mm) 3 mm 16 mm corner (-wick_r - 2 mm, -1.5 mm, wall + 6 mm) cut targets WickCup
box WickSlotY size 3 mm (2*wick_r + 4 mm) 16 mm corner (-1.5 mm, -wick_r - 2 mm, wall + 6 mm) cut targets WickCup
sketch WickFloorSk on XY offset (wall + 1 mm) {
  circle h = (7 mm, 0) radius 1.5 mm
  point c = (0, 0)
  fix h c
  pattern circular h count 5 center c angle 360 deg
}
extrude WickFloor from WickFloorSk distance 3 mm cut targets WickCup

// ==== Spout: a tube from just above the reservoir floor up through the pot, a funnel on top
sketch SpoutSk on XZ {
  polyline closed o = (spout_r + spout_od/2 - 1.5 mm, -(wall + 1 mm)) (spout_r + spout_od/2, -(wall + 1 mm)) (spout_r + spout_od/2, -(pot_top_z + 5 mm)) (spout_r + spout_od/2 + 6 mm, -(pot_top_z + 11 mm)) (spout_r + spout_od/2 + 6 mm, -(pot_top_z + 13 mm)) (spout_r + spout_od/2 + 4.4 mm, -(pot_top_z + 13 mm)) (spout_r + spout_od/2 + 4.4 mm, -(pot_top_z + 11.6 mm)) (spout_r + spout_od/2 - 1.5 mm, -(pot_top_z + 5.7 mm))
  construction line ax = (spout_r, 0) (spout_r, -10 mm)
  fix o ax
}
revolve Spout from SpoutSk axis ax -> body Spout

// Fits: nothing touches but the pot's shoulder on the rim edge
check Spout clearance Pot >= 0.2 mm
check Spout clearance Reservoir >= 0.5 mm
check WickCup clearance Pot >= 0.1 mm
check WickCup clearance Reservoir >= 1 mm

color Reservoir "#3d6e5a"
color Pot "#d9cfc0"
color WickCup "#8a6b4e"
color Spout "#3d6e5a"
material Reservoir, Pot, WickCup, Spout PLA
