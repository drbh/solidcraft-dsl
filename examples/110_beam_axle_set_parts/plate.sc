// A plate of 110_beam_axle_set: nx by ny holes on the pitch, rounded corners, a thin flat
// sheet for bases and gussets. Lies on z = 0, the first hole at the origin.
design HolePlate units mm

param nx = 5
param ny = 3
param pitch = 10 mm
param beam_w = 9 mm
param plate_t = 3.5 mm
param hole_d = 6.2 mm

sketch OutlineSk on XY {
  rect rounded o = (-beam_w/2, -beam_w/2) ((nx - 1)*pitch + beam_w/2, (ny - 1)*pitch + beam_w/2) radius (beam_w/2 - 0.5 mm)
  fix o
}
extrude Sheet from OutlineSk distance plate_t new -> body Plate
sketch HoleSk on XY offset (-0.5 mm) {
  circle h = (0, 0) diameter hole_d
  fix h
  pattern rectangular h count nx spacing pitch count2 ny spacing2 pitch
}
extrude Holes from HoleSk distance (plate_t + 1 mm) cut targets Plate

color Plate "#7a7f86"
material Plate PLA
