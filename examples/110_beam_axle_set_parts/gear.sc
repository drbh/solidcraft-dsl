// A spur gear of 110_beam_axle_set: `z` straight-flank teeth of module `m`, `thick` thick, on
// z = 0, centred on the Z axis, with a cross hole to ride an axle. Two gears mesh on the beam
// pitch when m*(z1 + z2)/2 is a whole number of pitches: m = 1.25 with 12 + 20 teeth is 20 mm.
design Gear units mm

param z = 12
param m = 1.25 mm
param thick = 6 mm
param flank = 20 deg
param backlash = 0.12
param span = 5.6 mm
param arm = 2 mm
param axle_clr = 0.15 mm

let rp = m*z/2
let rf = rp - 1.25*m
let ra = rp + m
let half_t = (1 - backlash)*pi*m/4
let w_root = half_t + 1.25*m*tan(flank)
let w_tip = half_t - m*tan(flank)

cylinder Core radius rf height thick new -> body Gear
sketch TeethSk on XY {
  point c = (0, 0)
  polyline closed tooth = (rf - 0.4 mm, -w_root) (ra, -w_tip) (ra, w_tip) (rf - 0.4 mm, w_root)
  fix c tooth
  pattern circular tooth count z center c angle 360 deg
}
extrude Teeth from TeethSk distance thick join targets Gear
sketch HoleSk on XY offset (-0.5 mm) {
  polyline closed o = (span/2 + axle_clr, -arm/2 - axle_clr) (span/2 + axle_clr, arm/2 + axle_clr) (arm/2 + axle_clr, arm/2 + axle_clr) (arm/2 + axle_clr, span/2 + axle_clr) (-arm/2 - axle_clr, span/2 + axle_clr) (-arm/2 - axle_clr, arm/2 + axle_clr) (-span/2 - axle_clr, arm/2 + axle_clr) (-span/2 - axle_clr, -arm/2 - axle_clr) (-arm/2 - axle_clr, -arm/2 - axle_clr) (-arm/2 - axle_clr, -span/2 - axle_clr) (arm/2 + axle_clr, -span/2 - axle_clr) (arm/2 + axle_clr, -arm/2 - axle_clr)
  fix o
}
extrude CrossHole from HoleSk distance (thick + 1 mm) cut targets Gear

color Gear "#f2b02e"
material Gear PLA
