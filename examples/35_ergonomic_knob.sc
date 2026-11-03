// 35. Ergonomic clamping knob for an M6 bolt or threaded rod, with a captured hex nut.
//
// Z is the bolt axis, z = 0 the flat underside that bears on the clamped part. The grip is a
// sphere with its bottom and top sliced off: the bottom cut sits above the sphere's 45 deg
// latitude so the underside never overhangs more than 45 deg, and the top is a flat, filleted
// palm face. Vertical cylinders cut a ring of scallops into the equator for the fingers.
// From below, a hex pocket takes an M6 nut (10 mm across flats, 5 mm thick) with nut_play
// clearance; press it in or pull it in with the bolt. A clearance bore continues up from the
// pocket and stops top_skin below the top so the knob has a closed, clean top.
//
// Print upside-up on the flat underside, no supports. The scallops are vertical walls, the
// sphere above the base cut leans out at most 45 deg. The nut pocket and the bore have flat ceilings: two short bridges (11.5 mm
// and 6.6 mm) that any printer handles. Squeeze the nut in after printing.
design ErgonomicKnob units mm
// Hardware: M6 nut and bolt
param nut_af = 10 mm
param nut_t = 5 mm
param nut_play = 0.3 mm
param bolt_clear = 6.6 mm
// Grip
param grip_d = 38 mm
param base_lat = 0.62
param height = 26 mm
param scallops = 7
param scallop_r = 5.5 mm
param scallop_depth = 3.5 mm
param top_fillet = 3 mm
param top_skin = 3 mm
let R = grip_d/2
// Sphere centre height: the base cut is base_lat*R below the centre
let zc = base_lat*R
let top_r = sqrt(R^2 - (height - zc)^2)
// Scallop cylinders: centre radius so each bites scallop_depth into the equator
let sc_c = R - scallop_depth + scallop_r

// Grip sphere, sliced flat top and bottom
sphere Grip diameter grip_d center (0,0,zc) -> body Knob
box CutBelow size (grip_d + 2 mm) (grip_d + 2 mm) (R) corner (-R - 1 mm,-R - 1 mm,-R) cut targets Knob
box CutAbove size (grip_d + 2 mm) (grip_d + 2 mm) (R) corner (-R - 1 mm,-R - 1 mm,height) cut targets Knob

// Finger scallops around the equator
cylinder Scallop radius scallop_r height (height + 2 mm) base (sc_c,0,-1 mm) cut targets Knob
pattern circular Scallops features Scallop axis Z count scallops angle 360 deg

// Palm face fillet
fillet TopRound edges (top_r,0,height) radius top_fillet body Knob

// Captured nut pocket from below, then the bolt bore
sketch NutSk on XY {
  polygon nut = (0,0) radius ((nut_af + nut_play)/2) sides 6 circumscribed
  fix nut.1 nut.2 nut.3 nut.4 nut.5 nut.6
}
extrude NutPocket from NutSk distance (nut_t + nut_play) cut targets Knob
cylinder Bore diameter bolt_clear height (height - top_skin) base (0,0,0) cut targets Knob
