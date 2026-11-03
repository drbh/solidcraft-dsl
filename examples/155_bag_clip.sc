// 155. Bag clip with a print-in-place hinge and a snap latch: two jaws that come off the bed joined.
//
// X runs along the clip, Y across its width, Z up from the bed; the hinge axis runs along Y at
// x = 0, height zh. JawA lies along +X, JawB along -X, both flat on the bed: the clip is printed
// open 180 deg. Fold JawB over and it lands on JawA with `gap_closed` between the faces, its
// groove over JawA's ridge.
//
// Hinge: three knuckles across the width, A B A, with an axial `gap` between them. JawA's outer
// knuckles carry 45 deg cones that point into conical sockets in JawB's middle knuckle; the
// sockets are the cones grown by pin_clear normal to the surface (the cones start 0.1 mm inside
// their knuckle, so no edge lies on the knuckle face), so JawB turns freely but
// cannot come out (the same pin as 47_print_in_place_hinge_box). Each knuckle is a teardrop: round
// above the axis, two 45 deg flanks down to the bed, so it needs no support. The arms start
// clear of the other jaw's knuckles at every angle.
//
// Latch: a flexing plate on JawB's free end with a hooked lip. Closed, the plate passes JawA's
// end with `latch_clear` to spare and the lip catches under JawA. The catch sits `preload` above
// JawA's underside, so snapping it shut bends the jaws together by preload - gap_closed (0.3 mm
// by default): that is the clamping force. The lip's lead-in slope rides over JawA's end as
// you close it. With the defaults the plate bends about 1.2 mm to pass, ~2% strain: fine in
// PETG, OK in PLA for a few hundred cycles.
//
// Printing: flat as modelled, one plate, no supports, PETG preferred (PLA works). 0.2 mm layers,
// 3 perimeters. The latch lip's catch face is a 1.2 mm horizontal overhang off the plate, which
// prints clean without support. Break the hinge free by folding the clip once firmly.
design BagClip units mm

// Clip
param L = 110 mm               // arm length from the hinge axis
param w = 15 mm                // width across (along the hinge axis)
param t = 4 mm                 // jaw thickness
param gap_closed = 0.3 mm      // between the jaw faces when folded (before the latch preloads)
param ridge_r = 1.2 mm         // gripping ridge on JawA, groove in JawB
param groove_clear = 0.3 mm
// Hinge
param rk = 4.5 mm              // knuckle radius
param gap = 0.4 mm             // axial gap between knuckles
param rc = 2 mm                // pin cone radius
param pin_clear = 0.4 mm
// Latch
param lt = 1.6 mm              // latch plate thickness
param hook_d = 1.2 mm          // how far the lip reaches under JawA
param hook_t = 1.5 mm
param latch_clear = 0.3 mm     // plate to JawA's end, closed
param preload = 0.6 mm         // catch height above JawA's underside (squeezes the jaws)

let zh = t + gap_closed/2
let k = (w - 2*gap)/3
let c2 = pin_clear*sqrt(2)
let tip = 0.3 mm
let x0 = zh*sqrt(2) + gap      // arms start here: clear of the other jaw's knuckles swept round
let s = rk*sqrt(2)/2           // the teardrop's tangent point, from the axis
let z_catch = 2*zh - preload   // the catch, in the print pose (closed: z = preload)

// ==== JawA: knuckles 0 and 2, their webs, and the arm along +X
sketch KnuckleSk on XZ offset 0 {
  arc top = start (-s, -(zh - s)) through (0, -(zh + rk)) end (s, -(zh - s))
  line right = top.end (zh, 0)
  line bottom = right.end (-zh, 0)
  line left = bottom.end top.start
  fix top right bottom left
}
extrude KnuckleA0 from KnuckleSk distance k new -> body JawA
box WebA0 size (x0 + 1 mm) k t corner (0, 0, 0) join targets JawA
box ArmA size (L - latch_clear - x0) w t corner (x0, 0, 0) join targets JawA
box WebA2 size (x0 + 1 mm) k t corner (0, 2*k + 2*gap, 0) join targets JawA
extrude KnuckleA2 from KnuckleSk distance k offset (2*k + 2*gap) join targets JawA
cylinder Ridge radius ridge_r height (L - latch_clear - x0 - 6 mm) base (x0 + 3 mm, w/2, t - 0.2 mm) axis (1,0,0) join targets JawA
fillet ArmAEnd edges (L - latch_clear, 0, t/2) (L - latch_clear, w, t/2) radius 2 mm body JawA

// ==== JawB: knuckle 1, its web, the arm along -X, the latch plate and lip
// (arm, latch plate and lip are one side profile, extruded across the width in one go)
sketch JawBSk on XZ offset 0 {
  polyline closed p = (-x0, 0) (-(L + lt), 0) (-(L + lt), -(z_catch + hook_t)) (-L, -(z_catch + hook_t)) (-L + hook_d, -z_catch) (-L, -z_catch) (-L, -t) (-x0, -t)
  fix p
}
extrude ArmB from JawBSk distance w new -> body JawB
box WebB1 size (x0 + 1 mm) k t corner (-(x0 + 1 mm), k + gap, 0) join targets JawB
extrude KnuckleB1 from KnuckleSk distance k offset (k + gap) join targets JawB
cylinder Groove radius (ridge_r + groove_clear) height (L - x0 - 1 mm) base (-L, w/2, t + gap_closed + 0.2 mm) axis (1,0,0) cut targets JawB

// ==== Pin cones on JawA (revolved about the axis; YZ sketch: u = y, v = z) and sockets in JawB
sketch Cone0Sk on YZ offset 0 {
  construction line ax = (0, zh) (w, zh)
  polyline closed c = (k - 0.5 mm, zh) (k - 0.5 mm, zh + rc) (k - 0.1 mm, zh + rc) (k + rc - tip - 0.1 mm, zh + tip) (k + rc - tip - 0.1 mm, zh)
  fix ax c
}
revolve Cone0 from Cone0Sk axis ax new -> body Cone0Tool
combine Cone0Join target JawA tools Cone0Tool join
sketch Cone2Sk on YZ offset 0 {
  construction line ax = (0, zh) (w, zh)
  polyline closed c = (2*k + 2*gap + 0.5 mm, zh) (2*k + 2*gap + 0.5 mm, zh + rc) (2*k + 2*gap + 0.1 mm, zh + rc) (2*k + 2*gap - rc + tip + 0.1 mm, zh + tip) (2*k + 2*gap - rc + tip + 0.1 mm, zh)
  fix ax c
}
revolve Cone2 from Cone2Sk axis ax new -> body Cone2Tool
combine Cone2Join target JawA tools Cone2Tool join
sketch SocketSk on YZ offset 0 {
  construction line ax = (0, zh) (w, zh)
  polyline closed s1 = (k, zh) (k, zh + rc + c2) (k + rc + c2 - tip, zh + tip) (k + rc + c2 - tip, zh)
  polyline closed s2 = (2*k + 2*gap, zh) (2*k + 2*gap, zh + rc + c2) (2*k + 2*gap - rc - c2 + tip, zh + tip) (2*k + 2*gap - rc - c2 + tip, zh)
  fix ax s1 s2
}
revolve Sockets from SocketSk axis ax cut targets JawB

// Printed together, the jaws never touch; JawA needs no support (JawB's lip is a short bridge)
check JawB clearance JawA >= (gap - 0.01 mm)
check JawA overhang <= 45 deg
check JawA size.x == (L - latch_clear + rk)

color JawA "#ffb300"
color JawB "#ffb300"
material JawA, JawB PLA          // PETG preferred; the engine has no PETG material
