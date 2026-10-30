// 38. Guard for a 120 mm PC fan: concentric rings, radial spokes and four screw lugs.
//
// Z is up and the guard lies flat on z = 0, centred on the fan axis. A 116 mm outer ring, three
// inner rings and a solid centre disc are tied together by radial spokes made from one spoke
// and a circular pattern. One mounting lug is drawn at the +X+Y corner on the fan's 105 mm
// square hole pattern; mirroring it across YZ, across XZ and across the diagonal plane x + y = 0
// gives all four. The lug
// holes take the M4 (or the fan's self-tapping) screws through 4.3 mm clearance holes; the lugs
// sit on the fan frame corners, which reach to 60 mm from the axis.
//
// Print flat as modelled, no supports: it is a single 2.4 mm thick extrusion stack with no
// overhangs. Rings and spokes are wide enough (>= 1.6 mm) for four perimeters at a 0.4 mm nozzle.
design FanGuard units mm
// Fan standard (120 mm fan)
param hole_spacing = 105 mm
param screw_clear = 4.3 mm
param outer_d = 116 mm
// Guard
param thick = 2.4 mm
param outer_w = 3 mm
param ring_w = 1.8 mm
param ring_gap = 10 mm
param hub_r = 10 mm
param n_spokes = 8
param spoke_w = 2 mm
param lug_w = 10 mm
let r_out = outer_d/2
let r1 = r_out - outer_w - ring_gap
let r2 = r1 - ring_w - ring_gap
let r3 = r2 - ring_w - ring_gap
let hp = hole_spacing/2

// Centre disc, then the spokes: one along +X from the disc to the outer ring, patterned around
// Z, so the rings joined next all land on one connected star (rings joined before the spokes
// would be separate lumps, which the engine splits into separate bodies)
cylinder Hub radius hub_r height thick new -> body Guard
sketch SpokeSk on XY {
  rect s = (hub_r - 0.5 mm,-spoke_w/2) (r_out - outer_w/2,spoke_w/2)
  fix s.bottom s.right s.top s.left
}
extrude Spoke from SpokeSk distance thick join targets Guard
pattern circular Spokes features Spoke axis Z count n_spokes angle 360 deg

// Outer ring and three inner rings: concentric circles in one sketch, each
// ring picked as the profile between its two circles
sketch RingsSk on XY {
  circle o1 = (0,0) radius r_out
  circle o2 = (0,0) radius (r_out - outer_w)
  circle a1 = (0,0) radius r1
  circle a2 = (0,0) radius (r1 - ring_w)
  circle b1 = (0,0) radius r2
  circle b2 = (0,0) radius (r2 - ring_w)
  circle c1 = (0,0) radius r3
  circle c2 = (0,0) radius (r3 - ring_w)
  fix o1 o2 a1 a2 b1 b2 c1 c2
}
extrude Rings from RingsSk distance thick profiles (r_out - outer_w/2,0) (r1 - ring_w/2,0) (r2 - ring_w/2,0) (r3 - ring_w/2,0) join targets Guard

// Mounting lug at the +X+Y corner: a round-ended tab from the outer ring out to the hole
sketch LugSk on XY {
  slot lug = centers (r_out*cos(45 deg) - 1 mm,r_out*sin(45 deg) - 1 mm) (hp,hp) width lug_w
  fix lug.line1 lug.line2 lug.arc1 lug.arc2
}
extrude Lug from LugSk distance thick join targets Guard
cylinder LugHole radius (screw_clear/2) height (thick + 2 mm) base (hp,hp,-1 mm) cut targets Guard
mirror LugsX features Lug, LugHole plane YZ
mirror LugsY features Lug, LugHole plane XZ
// A mirror cannot take another mirror as input yet, so the -X-Y lug is the original mirrored
// through the diagonal plane x + y = 0
mirror LugsXY features Lug, LugHole origin (0,0,0) normal (1,1,0)

// Prints without supports, in the print recipe's orientation.
check Guard overhang <= 45 deg
