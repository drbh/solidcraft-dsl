// 47. Small box with a print-in-place hinged lid: box and lid come off the bed already joined.
//
// X runs along the hinge, Y from the box front (y = 0) back toward the hinge, Z up; the origin is
// the box's front-left-bottom corner. The hinge axis runs along X behind the box at
// (y_a, zh). Five knuckles alternate box, lid, box, lid, box with an axial gap between them. The
// box knuckles carry 45 deg cones that point into conical sockets in the lid knuckles; the sockets
// are the cones grown by pin_clear (0.4 mm) normal to the surface, so the lid turns freely but
// cannot come out. Every gap is at least 0.4 mm, so the two bodies never touch.
//
// The lid is modelled closed (a plate on the box rim with a locating lip along the front and
// sides and a thumb tab), then swung 180 deg about the hinge axis into its print pose: lying flat
// on the bed behind the box, inside face up. Because the axis sits at half of (box height + lid
// thickness), the closed lid lands exactly on the rim. Print as modelled, no supports: the box
// knuckles stand on 45 deg webs from the back wall, the lid knuckles on solid arms from the bed,
// and the cones and sockets are 45 deg.
design HingeBox units mm
// Box
param W = 60 mm
param D = 40 mm
param H = 25 mm
param wall = 1.6 mm
param floor_t = 1.6 mm
param lid_t = 2 mm
param lip_h = 2 mm
param lip_t = 1.2 mm
param lip_clear = 0.2 mm
// Hinge
param rk = 4 mm
param s = 6 mm
param gap = 0.4 mm
param rc = 2.5 mm
param pin_clear = 0.4 mm
let zh = (H + lid_t)/2
let y_a = D + s
let k = (W - 4*gap)/5
let pitch = k + gap
let c2 = pin_clear * sqrt(2)
let r45 = rk * sqrt(2)/2
// the cones stop tip short of a point (a flat tip keeps the boolean clean)
let tip = 0.3 mm

// Box
box BoxBlock size W D H -> body Box
box BoxCavity size (W - 2*wall) (D - 2*wall) H corner (wall, wall, floor_t) cut targets Box

// Box knuckles 0, 2, 4: a cylinder on a web from the back wall whose underside is 45 deg,
// tangent to the knuckle (YZ sketch: u = y, v = z)
sketch WebSk on YZ offset 0 {
  polyline closed w = (D - 0.5 mm, zh + rk) (y_a, zh + rk) (y_a + r45, zh - r45) (D - 0.5 mm, zh - r45 - (y_a + r45 - D + 0.5 mm))
  fix w.1 w.2 w.3 w.4
}
extrude BoxWeb from WebSk distance k join targets Box
cylinder BoxKnuckle radius rk height k base (0, y_a, zh) axis X join targets Box
pattern BoxKnuckles features BoxWeb, BoxKnuckle dir (1,0,0) count 3 spacing (2*pitch)

// Lid, closed on the rim
box LidPlate size W (D + gap + 1 mm) lid_t corner (0, 0, H) new -> body Lid
box ThumbTab size 20 mm 4 mm lid_t corner (W/2 - 10 mm, -4 mm, H) join targets Lid
box FrontLip size (W - 2*wall - 2*lip_clear) lip_t lip_h corner (wall + lip_clear, wall + lip_clear, H - lip_h) join targets Lid
box LeftLip size lip_t (D/2) lip_h corner (wall + lip_clear, wall + lip_clear, H - lip_h) join targets Lid
box RightLip size lip_t (D/2) lip_h corner (W - wall - lip_clear - lip_t, wall + lip_clear, H - lip_h) join targets Lid
// Lid knuckles 1, 3: an arm from the plate down to the axis behind the box
box LidArm size k (y_a + rk - D - gap) (H + lid_t - zh) corner (pitch, D + gap, zh) join targets Lid
cylinder LidKnuckle radius rk height k base (pitch, y_a, zh) axis X join targets Lid
pattern LidKnuckles features LidArm, LidKnuckle dir (1,0,0) count 2 spacing (2*pitch)

// Swing the lid open 180 deg about the hinge axis: turn about the X axis, then shift
move LidTurn bodies Lid axis (1,0,0) angle 180 deg
move LidOpen bodies Lid to (0, 2*y_a, 2*zh)

// Cones on the box knuckles (XZ sketch at the axis: u = x, v = -z), each revolved about the axis
// and combined on its own (a revolve that joins straight onto Box is refused by the engine)
sketch Cone1Sk on XZ offset y_a {
  construction line ax = (0, -zh) (W, -zh)
  polyline closed c = (k - 0.5 mm, -zh) (k - 0.5 mm, -zh - rc) (k, -zh - rc) (k + rc - tip, -zh - tip) (k + rc - tip, -zh)
  fix ax c.1 c.2 c.3 c.4 c.5
}
revolve Cone1 from Cone1Sk axis ax new -> body Cone1Tool
combine Cone1Join target Box tools Cone1Tool join
sketch Cone2Sk on XZ offset y_a {
  construction line ax = (0, -zh) (W, -zh)
  polyline closed c = (2*pitch + 0.5 mm, -zh) (2*pitch + 0.5 mm, -zh - rc) (2*pitch, -zh - rc) (2*pitch - rc + tip, -zh - tip) (2*pitch - rc + tip, -zh)
  fix ax c.1 c.2 c.3 c.4 c.5
}
revolve Cone2 from Cone2Sk axis ax new -> body Cone2Tool
combine Cone2Join target Box tools Cone2Tool join
sketch Cone3Sk on XZ offset y_a {
  construction line ax = (0, -zh) (W, -zh)
  polyline closed c = (2*pitch + k - 0.5 mm, -zh) (2*pitch + k - 0.5 mm, -zh - rc) (2*pitch + k, -zh - rc) (2*pitch + k + rc - tip, -zh - tip) (2*pitch + k + rc - tip, -zh)
  fix ax c.1 c.2 c.3 c.4 c.5
}
revolve Cone3 from Cone3Sk axis ax new -> body Cone3Tool
combine Cone3Join target Box tools Cone3Tool join
sketch Cone4Sk on XZ offset y_a {
  construction line ax = (0, -zh) (W, -zh)
  polyline closed c = (4*pitch + 0.5 mm, -zh) (4*pitch + 0.5 mm, -zh - rc) (4*pitch, -zh - rc) (4*pitch - rc + tip, -zh - tip) (4*pitch - rc + tip, -zh)
  fix ax c.1 c.2 c.3 c.4 c.5
}
revolve Cone4 from Cone4Sk axis ax new -> body Cone4Tool
combine Cone4Join target Box tools Cone4Tool join
// Sockets in the lid knuckles: the same cones moved pin_clear out along their surface normal
sketch SocketSk on XZ offset y_a {
  construction line ax = (0, -zh) (W, -zh)
  polyline closed s1 = (k, -zh) (k, -zh - rc - c2) (k + rc + c2, -zh)
  polyline closed s2 = (2*pitch, -zh) (2*pitch, -zh - rc - c2) (2*pitch - rc - c2, -zh)
  polyline closed s3 = (2*pitch + k, -zh) (2*pitch + k, -zh - rc - c2) (2*pitch + k + rc + c2, -zh)
  polyline closed s4 = (4*pitch, -zh) (4*pitch, -zh - rc - c2) (4*pitch - rc - c2, -zh)
  fix ax s1.1 s1.2 s1.3 s2.1 s2.2 s2.3 s3.1 s3.2 s3.3 s4.1 s4.2 s4.3
}
revolve Sockets from SocketSk axis ax cut targets Lid
