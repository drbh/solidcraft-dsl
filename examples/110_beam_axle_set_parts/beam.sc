// A beam of 110_beam_axle_set: n round holes on the pitch, rounded ends. Lies on z = 0, along +X,
// the first hole at the origin; holes run through along Z.
design Beam units mm

param n = 5
param pitch = 10 mm
param beam_w = 9 mm
param beam_t = 7 mm
param hole_d = 6.2 mm

sketch OutlineSk on XY {
  slot s = centers (0, 0) ((n - 1)*pitch, 0) width beam_w
  fix s
}
extrude Bar from OutlineSk distance beam_t new -> body Beam
sketch HoleSk on XY offset (-0.5 mm) {
  circle h = (0, 0) diameter hole_d
  fix h
  pattern rectangular h count n spacing pitch
}
extrude Holes from HoleSk distance (beam_t + 1 mm) cut targets Beam

color Beam "#c8352b"
material Beam PLA
