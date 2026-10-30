// 56. Curved cable comb: a half-ring band that sits on a desk, with teeth around its outside
// edge that cables drop between.
//
// The band is a ring cut in half. One tooth is drawn radially at `a0` from the X axis, and a
// pattern along an arc of the band's outer edge copies it: with the copies turning to follow the
// path, every tooth stays radial. `teeth` and the gap between them follow from the arc length.
//
// Print flat, as modelled: every face is vertical or on the bed.
design ArcCableComb units mm
param R = 60 mm
param band_w = 10 mm
param h = 12 mm
param tooth_l = 14 mm
param tooth_t = 4 mm
param teeth = 9
param a0 = 15 deg
// Arc length from the first tooth to the last
let span = R*((180 deg - 2*a0)/(1 rad))
// Tooth corners: inner end 1 mm inside the band, outer end tooth_l beyond it
let ri = R - 1 mm
let ro = R + tooth_l
let c0 = cos(a0)
let s0 = sin(a0)
let ht = tooth_t/2

sketch Ring on XY {
  circle o = (0,0) radius R
  circle i = (0,0) radius (R - band_w)
  fix o i
}
extrude Band from Ring distance h profiles (R - band_w/2,0) -> body Comb
box Half size (2*R + 2*tooth_l) (R + tooth_l) (h + 2 mm) corner (-(R + tooth_l),-(R + tooth_l),-1 mm) cut targets Comb

sketch ToothSk on XY {
  polyline closed t = (ri*c0 + ht*s0,ri*s0 - ht*c0) (ro*c0 + ht*s0,ro*s0 - ht*c0) (ro*c0 - ht*s0,ro*s0 + ht*c0) (ri*c0 - ht*s0,ri*s0 + ht*c0)
  fix t
}
extrude Tooth from ToothSk distance h join targets Comb

sketch Rail on XY {
  arc edge = center (0,0) start (R*c0,R*s0) end (-R*c0,R*s0)
  fix edge
}
pattern Teeth features Tooth path Rail.edge count teeth distance span

check Comb overhang <= 45 deg
check Comb size.y == (R + tooth_l)
