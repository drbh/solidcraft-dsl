// 32. Lofted plant pot: a hexagonal foot that flows into a round rim, with a matching saucer.
//
// Z is up, the pot's foot sits on z = 0 and its axis is Z. The pot is a loft from a hexagon
// (base_r to its corners) on the bed to a circle (rim_r) at height, so the six flats fade out
// into a round lip. It is hollowed by a second loft set in by the wall on every side and on
// the floor, and drain holes go through the floor: one in the middle and a ring of
// drains around it. The saucer is a revolved shallow dish with radial ribs on its floor; the pot
// stands on the ribs, so water drains out from under the holes and the pot never sits in it.
// The saucer is modelled under the pot, its ribs show_gap below the foot, and its inside clears
// the pot's corners by saucer_gap.
//
// Print both upright, no supports. The pot's sides lean out about 10 deg from vertical (the loft
// is ruled between the hexagon and the circle), the saucer wall leans out about 20 deg, and the
// drains are vertical holes in the floor. Use 2 perimeters at wall = 1.6 mm with a 0.4 nozzle;
// the pot is watertight apart from the drains.
design LoftedPlanter units mm
// Pot
param base_r = 40 mm
param rim_r = 55 mm
param pot_h = 110 mm
param wall = 1.6 mm
param drain_d = 6 mm
param drains = 6
param drain_ring = 20 mm
param centre_drain_d = 8 mm
// Saucer
param saucer_gap = 5 mm
param saucer_wall = 2 mm
param saucer_floor = 2 mm
param saucer_h = 14 mm
param saucer_flare = 5 mm
param rib_h = 3 mm
param rib_w = 2.4 mm
param ribs = 6
// The saucer is shown this far below the pot so the two stay separate bodies
param show_gap = 0.5 mm
// Saucer section: z of the underside, the floor top and the rim
let z_bot = -(show_gap + rib_h + saucer_floor)
let z_floor = -(show_gap + rib_h)
let z_rim = z_floor + saucer_h
let s_in = base_r + saucer_gap
let s_top = s_in + saucer_flare

// Pot: hexagon on the bed lofted to the round rim
sketch Foot on XY {
  polygon hex = (0,0) radius base_r sides 6
  fix hex.1 hex.2 hex.3 hex.4 hex.5 hex.6
}
sketch Rim on XY offset pot_h {
  circle rim = (0,0) radius rim_r
  fix rim
}
loft Pot sections Foot, Rim -> body Pot
// Hollow it with a second loft, wall in from the first and run 1 mm out past the rim.
// (`shell` cannot offset a loft's ruled faces yet, so the cavity is lofted too.)
sketch InFoot on XY offset wall {
  polygon hex = (0,0) radius (base_r - wall/cos(30 deg)) sides 6
  fix hex.1 hex.2 hex.3 hex.4 hex.5 hex.6
}
sketch InRim on XY offset (pot_h + 1 mm) {
  circle rim = (0,0) radius (rim_r - wall + 1 mm*(rim_r - base_r)/pot_h)
  fix rim
}
loft Cavity sections InFoot, InRim cut targets Pot

// Drain holes through the floor
cylinder CentreDrain diameter centre_drain_d height (wall + 2 mm) base (0,0,-1 mm) cut targets Pot
cylinder Drain diameter drain_d height (wall + 2 mm) base (drain_ring,0,-1 mm) cut targets Pot
pattern circular DrainRing features Drain axis Z count drains angle 360 deg

// Saucer: revolved dish under the pot
sketch SaucerSection on XZ {
  polyline closed s = (0,-z_bot) (s_in + saucer_wall,-z_bot) (s_top + saucer_wall,-z_rim) (s_top,-z_rim) (s_in,-z_floor) (0,-z_floor)
  fix s.1 s.2 s.3 s.4 s.5 s.6
}
revolve Saucer from SaucerSection axis Z new -> body Saucer

// Radial ribs the pot stands on, sunk 0.5 mm into the floor so they fuse with it
box Rib size (base_r - 14 mm) rib_w (rib_h + 0.5 mm) corner (10 mm,-rib_w/2,z_floor - 0.5 mm) join targets Saucer
pattern circular RibRing features Rib axis Z count ribs angle 360 deg
