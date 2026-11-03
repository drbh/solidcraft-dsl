// A friction pin of 110_beam_axle_set: joins `layers` stacked beams through one hole. Along Z
// from z = 0, centred on the Z axis. Each end is slit so it springs, with a ridge `ridge` over
// the hole size that snaps past the hole's edge; a flat along one side (-Y) is the print face.
design Pin units mm

param layers = 2
param beam_t = 7 mm
param hole_d = 6.2 mm
param pin_clr = 0.05 mm
param ridge = 0.15 mm

let len = layers*beam_t - 0.4 mm

cylinder Shank diameter (hole_d - 2*pin_clr) height len base (0, 0, 0) new -> body Pin
cylinder RidgeLo diameter (hole_d + 2*ridge) height 0.8 mm base (0, 0, 0.4 mm) join targets Pin
cylinder RidgeHi diameter (hole_d + 2*ridge) height 0.8 mm base (0, 0, len - 1.2 mm) join targets Pin
box SlitLo size (hole_d + 2) 1.2 mm 4.5 mm corner (-(hole_d + 2)/2, -0.6 mm, -0.5 mm) cut targets Pin
box SlitHi size (hole_d + 2) 1.2 mm 4.5 mm corner (-(hole_d + 2)/2, -0.6 mm, len - 4 mm) cut targets Pin
box Flat size (hole_d + 2) 1 mm (len + 2) corner (-(hole_d + 2)/2, -(hole_d/2 + ridge + 1 mm) + 0.45 mm, -1 mm) cut targets Pin

color Pin "#2b5fc8"
material Pin PLA
