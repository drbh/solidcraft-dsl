// 141. Open differential you can turn by hand, inspired by the trending printed differentials: four
// straight bevel gears in a bolted carrier, a spur ring gear driven by a crank, two output shafts
// with dials, on a stand. Hold one dial and the other turns twice as fast; hold the crank and the
// dials turn opposite ways.
//
// X is the output axis (the dials are at the ends), Y across, Z up; the origin is the point where
// all four bevel gears' cone apexes meet, at the carrier's centre. The stand's base is below.
//
// The bevel gears are all the same 45 deg miter gear (14 teeth, module mb): the two side gears on
// the X axis and the two pinions on the cross pin along Y. Each is a loft between two similar tooth
// outlines on planes square to its axis, the front one scaled by kf, so every flank line runs
// through the common apex, as a straight bevel's does. The teeth have straight 20 deg flanks (an
// approximation of the involute that prints and meshes well at this size), 0.15 module thinner at
// the pitch circle (0.35 for the ring and crank, whose 12-tooth pinion needs the room); the side gears have a gap toward each pinion and the pinions a tooth toward each
// side gear, so they are modelled in mesh. The outlines were written out by a small generator, so
// the tooth count is fixed; mb, w and everything else are live.
//
// The carrier is two halves 0.5 mm apart at x = 0, so the bolts clamp them on the cross pin (the
// left half's bridges are 0.2 mm smaller on every side, so no faces of the two are coplanar):
// each a cheek plate and two bridges, bolted by four M3
// x 60 screws (nuts on the ring gear's face). The cross pin lies in half-holes in the bridges and
// is clamped between them; the pinions turn on it. The side gears thrust on the cheeks, the pinions
// on the bridges, and each output shaft is keyed into its side gear by a D flat and turns in the
// cheek and in the stand. The ring gear (40 teeth, module mr) is part of the right half; the crank's
// 12-tooth pinion drives it from below, and the crank arm presses on the crank's D flat.
//
// Assembly: drop the side gears and pinions into one half with the pin, close the other half,
// bolt it; slide the shafts in through the stand's pillars; fit the crank and its arm.
//
// Print: gears on their back faces, teeth up (the 45 deg cones need no support); carrier halves on
// their cheeks; shafts standing on their dials; the crank standing on its pinion; the stand as
// modelled; the arm flat. Printed in PLA at 0.2 mm, the gears mesh with the modelled backlash.
design Differential units mm

// Bevel gears (14 teeth)
param mb = 2.5 mm              // bevel module
param w = 6 mm                 // face width, along the gear's axis
param hub_r = 8 mm
param hub_l = 3.2 mm
param pin_r = 3 mm
param play = 0.2 mm            // pins in bores, radial

// Carrier
param cheek_r = 30 mm
param cheek_t = 4 mm
param bridge_w = 6 mm
param bridge_h = 16 mm
param thrust = 0.3 mm          // gear backs to the carrier

// Ring and crank (40 and 12 teeth)
param mr = 2 mm
param ring_t = 6.5 mm
param arm_l = 22 mm

// Shafts and stand
param shaft_r = 4 mm
param bear = 0.3 mm            // shafts in their bearings, radial
param flat = 3.6 mm            // D flat, from the axis
param dial_r = 15 mm
param base_t = 6 mm

let Rp = 7*mb                             // pitch radius at the back face
let kf = (Rp - w)/Rp
let x_cheek = Rp + hub_l + thrust        // cheek's inner face
let y_bridge = Rp + hub_l + thrust       // bridge's inner face
let cd = (40 + 12)/2*mr                  // ring to crank centre distance
let z_base = -(cd + 12/2*mr + mr + 4 mm) // the stand's top
let x_pillar = x_cheek + cheek_t + ring_t + 5 mm

// ==== Side gears (axis X; YZ sketch: u = y, v = z), the right one first
sketch SideRBack on YZ offset Rp {
  polyline closed o = (5.7486*mb, 0.1249*mb) (7.8692*mb, 1.4409*mb) (7.7150*mb, 2.1161*mb) (5.2336*mb, 2.3817*mb) (5.1251*mb, 2.6068*mb) (6.4647*mb, 4.7125*mb) (6.0329*mb, 5.2540*mb) (3.6819*mb, 4.4166*mb) (3.4865*mb, 4.5724*mb) (3.7798*mb, 7.0507*mb) (3.1558*mb, 7.3513*mb) (1.4010*mb, 5.5767*mb) (1.1574*mb, 5.6323*mb) (0.3463*mb, 7.9925*mb) (-0.3463*mb, 7.9925*mb) (-1.1574*mb, 5.6323*mb) (-1.4010*mb, 5.5767*mb) (-3.1558*mb, 7.3513*mb) (-3.7798*mb, 7.0507*mb) (-3.4865*mb, 4.5724*mb) (-3.6819*mb, 4.4166*mb) (-6.0329*mb, 5.2540*mb) (-6.4647*mb, 4.7125*mb) (-5.1251*mb, 2.6068*mb) (-5.2336*mb, 2.3817*mb) (-7.7150*mb, 2.1161*mb) (-7.8692*mb, 1.4409*mb) (-5.7486*mb, 0.1249*mb) (-5.7486*mb, -0.1249*mb) (-7.8692*mb, -1.4409*mb) (-7.7150*mb, -2.1161*mb) (-5.2336*mb, -2.3817*mb) (-5.1251*mb, -2.6068*mb) (-6.4647*mb, -4.7125*mb) (-6.0329*mb, -5.2540*mb) (-3.6819*mb, -4.4166*mb) (-3.4865*mb, -4.5724*mb) (-3.7798*mb, -7.0507*mb) (-3.1558*mb, -7.3513*mb) (-1.4010*mb, -5.5767*mb) (-1.1574*mb, -5.6323*mb) (-0.3463*mb, -7.9925*mb) (0.3463*mb, -7.9925*mb) (1.1574*mb, -5.6323*mb) (1.4010*mb, -5.5767*mb) (3.1558*mb, -7.3513*mb) (3.7798*mb, -7.0507*mb) (3.4865*mb, -4.5724*mb) (3.6819*mb, -4.4166*mb) (6.0329*mb, -5.2540*mb) (6.4647*mb, -4.7125*mb) (5.1251*mb, -2.6068*mb) (5.2336*mb, -2.3817*mb) (7.7150*mb, -2.1161*mb) (7.8692*mb, -1.4409*mb) (5.7486*mb, -0.1249*mb)
  fix o
}
sketch SideRFront on YZ offset (Rp - w) {
  polyline closed o = (5.7486*mb*kf, 0.1249*mb*kf) (7.8692*mb*kf, 1.4409*mb*kf) (7.7150*mb*kf, 2.1161*mb*kf) (5.2336*mb*kf, 2.3817*mb*kf) (5.1251*mb*kf, 2.6068*mb*kf) (6.4647*mb*kf, 4.7125*mb*kf) (6.0329*mb*kf, 5.2540*mb*kf) (3.6819*mb*kf, 4.4166*mb*kf) (3.4865*mb*kf, 4.5724*mb*kf) (3.7798*mb*kf, 7.0507*mb*kf) (3.1558*mb*kf, 7.3513*mb*kf) (1.4010*mb*kf, 5.5767*mb*kf) (1.1574*mb*kf, 5.6323*mb*kf) (0.3463*mb*kf, 7.9925*mb*kf) (-0.3463*mb*kf, 7.9925*mb*kf) (-1.1574*mb*kf, 5.6323*mb*kf) (-1.4010*mb*kf, 5.5767*mb*kf) (-3.1558*mb*kf, 7.3513*mb*kf) (-3.7798*mb*kf, 7.0507*mb*kf) (-3.4865*mb*kf, 4.5724*mb*kf) (-3.6819*mb*kf, 4.4166*mb*kf) (-6.0329*mb*kf, 5.2540*mb*kf) (-6.4647*mb*kf, 4.7125*mb*kf) (-5.1251*mb*kf, 2.6068*mb*kf) (-5.2336*mb*kf, 2.3817*mb*kf) (-7.7150*mb*kf, 2.1161*mb*kf) (-7.8692*mb*kf, 1.4409*mb*kf) (-5.7486*mb*kf, 0.1249*mb*kf) (-5.7486*mb*kf, -0.1249*mb*kf) (-7.8692*mb*kf, -1.4409*mb*kf) (-7.7150*mb*kf, -2.1161*mb*kf) (-5.2336*mb*kf, -2.3817*mb*kf) (-5.1251*mb*kf, -2.6068*mb*kf) (-6.4647*mb*kf, -4.7125*mb*kf) (-6.0329*mb*kf, -5.2540*mb*kf) (-3.6819*mb*kf, -4.4166*mb*kf) (-3.4865*mb*kf, -4.5724*mb*kf) (-3.7798*mb*kf, -7.0507*mb*kf) (-3.1558*mb*kf, -7.3513*mb*kf) (-1.4010*mb*kf, -5.5767*mb*kf) (-1.1574*mb*kf, -5.6323*mb*kf) (-0.3463*mb*kf, -7.9925*mb*kf) (0.3463*mb*kf, -7.9925*mb*kf) (1.1574*mb*kf, -5.6323*mb*kf) (1.4010*mb*kf, -5.5767*mb*kf) (3.1558*mb*kf, -7.3513*mb*kf) (3.7798*mb*kf, -7.0507*mb*kf) (3.4865*mb*kf, -4.5724*mb*kf) (3.6819*mb*kf, -4.4166*mb*kf) (6.0329*mb*kf, -5.2540*mb*kf) (6.4647*mb*kf, -4.7125*mb*kf) (5.1251*mb*kf, -2.6068*mb*kf) (5.2336*mb*kf, -2.3817*mb*kf) (7.7150*mb*kf, -2.1161*mb*kf) (7.8692*mb*kf, -1.4409*mb*kf) (5.7486*mb*kf, -0.1249*mb*kf)
  fix o
}
loft SideRTeeth sections SideRBack, SideRFront -> body SideR
cylinder SideRHub radius hub_r height (hub_l + 0.5 mm) base (Rp - 0.5 mm, 0, 0) axis X join targets SideR
cylinder SideRBoreTool radius (shaft_r + 0.1 mm) height (w + hub_l + 2 mm) base (Rp - w - 1 mm, 0, 0) axis X new -> body BoreR
box SideRBoreFlat size (w + hub_l + 4 mm) (2*shaft_r + 2 mm) (shaft_r) corner (Rp - w - 2 mm, -shaft_r - 1 mm, flat + 0.1 mm) cut targets BoreR
combine SideRBore target SideR tools BoreR cut

sketch SideLBack on YZ offset (-Rp) {
  polyline closed o = (5.7486*mb, 0.1249*mb) (7.8692*mb, 1.4409*mb) (7.7150*mb, 2.1161*mb) (5.2336*mb, 2.3817*mb) (5.1251*mb, 2.6068*mb) (6.4647*mb, 4.7125*mb) (6.0329*mb, 5.2540*mb) (3.6819*mb, 4.4166*mb) (3.4865*mb, 4.5724*mb) (3.7798*mb, 7.0507*mb) (3.1558*mb, 7.3513*mb) (1.4010*mb, 5.5767*mb) (1.1574*mb, 5.6323*mb) (0.3463*mb, 7.9925*mb) (-0.3463*mb, 7.9925*mb) (-1.1574*mb, 5.6323*mb) (-1.4010*mb, 5.5767*mb) (-3.1558*mb, 7.3513*mb) (-3.7798*mb, 7.0507*mb) (-3.4865*mb, 4.5724*mb) (-3.6819*mb, 4.4166*mb) (-6.0329*mb, 5.2540*mb) (-6.4647*mb, 4.7125*mb) (-5.1251*mb, 2.6068*mb) (-5.2336*mb, 2.3817*mb) (-7.7150*mb, 2.1161*mb) (-7.8692*mb, 1.4409*mb) (-5.7486*mb, 0.1249*mb) (-5.7486*mb, -0.1249*mb) (-7.8692*mb, -1.4409*mb) (-7.7150*mb, -2.1161*mb) (-5.2336*mb, -2.3817*mb) (-5.1251*mb, -2.6068*mb) (-6.4647*mb, -4.7125*mb) (-6.0329*mb, -5.2540*mb) (-3.6819*mb, -4.4166*mb) (-3.4865*mb, -4.5724*mb) (-3.7798*mb, -7.0507*mb) (-3.1558*mb, -7.3513*mb) (-1.4010*mb, -5.5767*mb) (-1.1574*mb, -5.6323*mb) (-0.3463*mb, -7.9925*mb) (0.3463*mb, -7.9925*mb) (1.1574*mb, -5.6323*mb) (1.4010*mb, -5.5767*mb) (3.1558*mb, -7.3513*mb) (3.7798*mb, -7.0507*mb) (3.4865*mb, -4.5724*mb) (3.6819*mb, -4.4166*mb) (6.0329*mb, -5.2540*mb) (6.4647*mb, -4.7125*mb) (5.1251*mb, -2.6068*mb) (5.2336*mb, -2.3817*mb) (7.7150*mb, -2.1161*mb) (7.8692*mb, -1.4409*mb) (5.7486*mb, -0.1249*mb)
  fix o
}
sketch SideLFront on YZ offset (-(Rp - w)) {
  polyline closed o = (5.7486*mb*kf, 0.1249*mb*kf) (7.8692*mb*kf, 1.4409*mb*kf) (7.7150*mb*kf, 2.1161*mb*kf) (5.2336*mb*kf, 2.3817*mb*kf) (5.1251*mb*kf, 2.6068*mb*kf) (6.4647*mb*kf, 4.7125*mb*kf) (6.0329*mb*kf, 5.2540*mb*kf) (3.6819*mb*kf, 4.4166*mb*kf) (3.4865*mb*kf, 4.5724*mb*kf) (3.7798*mb*kf, 7.0507*mb*kf) (3.1558*mb*kf, 7.3513*mb*kf) (1.4010*mb*kf, 5.5767*mb*kf) (1.1574*mb*kf, 5.6323*mb*kf) (0.3463*mb*kf, 7.9925*mb*kf) (-0.3463*mb*kf, 7.9925*mb*kf) (-1.1574*mb*kf, 5.6323*mb*kf) (-1.4010*mb*kf, 5.5767*mb*kf) (-3.1558*mb*kf, 7.3513*mb*kf) (-3.7798*mb*kf, 7.0507*mb*kf) (-3.4865*mb*kf, 4.5724*mb*kf) (-3.6819*mb*kf, 4.4166*mb*kf) (-6.0329*mb*kf, 5.2540*mb*kf) (-6.4647*mb*kf, 4.7125*mb*kf) (-5.1251*mb*kf, 2.6068*mb*kf) (-5.2336*mb*kf, 2.3817*mb*kf) (-7.7150*mb*kf, 2.1161*mb*kf) (-7.8692*mb*kf, 1.4409*mb*kf) (-5.7486*mb*kf, 0.1249*mb*kf) (-5.7486*mb*kf, -0.1249*mb*kf) (-7.8692*mb*kf, -1.4409*mb*kf) (-7.7150*mb*kf, -2.1161*mb*kf) (-5.2336*mb*kf, -2.3817*mb*kf) (-5.1251*mb*kf, -2.6068*mb*kf) (-6.4647*mb*kf, -4.7125*mb*kf) (-6.0329*mb*kf, -5.2540*mb*kf) (-3.6819*mb*kf, -4.4166*mb*kf) (-3.4865*mb*kf, -4.5724*mb*kf) (-3.7798*mb*kf, -7.0507*mb*kf) (-3.1558*mb*kf, -7.3513*mb*kf) (-1.4010*mb*kf, -5.5767*mb*kf) (-1.1574*mb*kf, -5.6323*mb*kf) (-0.3463*mb*kf, -7.9925*mb*kf) (0.3463*mb*kf, -7.9925*mb*kf) (1.1574*mb*kf, -5.6323*mb*kf) (1.4010*mb*kf, -5.5767*mb*kf) (3.1558*mb*kf, -7.3513*mb*kf) (3.7798*mb*kf, -7.0507*mb*kf) (3.4865*mb*kf, -4.5724*mb*kf) (3.6819*mb*kf, -4.4166*mb*kf) (6.0329*mb*kf, -5.2540*mb*kf) (6.4647*mb*kf, -4.7125*mb*kf) (5.1251*mb*kf, -2.6068*mb*kf) (5.2336*mb*kf, -2.3817*mb*kf) (7.7150*mb*kf, -2.1161*mb*kf) (7.8692*mb*kf, -1.4409*mb*kf) (5.7486*mb*kf, -0.1249*mb*kf)
  fix o
}
loft SideLTeeth sections SideLBack, SideLFront -> body SideL
cylinder SideLHub radius hub_r height (hub_l + 0.5 mm) base (-(Rp + hub_l), 0, 0) axis X join targets SideL
cylinder SideLBoreTool radius (shaft_r + 0.1 mm) height (w + hub_l + 2 mm) base (-(Rp + hub_l + 1 mm), 0, 0) axis X new -> body BoreL
box SideLBoreFlat size (w + hub_l + 4 mm) (2*shaft_r + 2 mm) (shaft_r) corner (-(Rp + hub_l + 2 mm), -shaft_r - 1 mm, flat + 0.1 mm) cut targets BoreL
combine SideLBore target SideL tools BoreL cut

// ==== Pinions (axis Y; XZ sketch: u = x, v = -z), on the cross pin
sketch PinionABack on XZ offset Rp {
  polyline closed o = (5.6323*mb, -1.1574*mb) (7.9925*mb, -0.3463*mb) (7.9925*mb, 0.3463*mb) (5.6323*mb, 1.1574*mb) (5.5767*mb, 1.4010*mb) (7.3513*mb, 3.1558*mb) (7.0507*mb, 3.7798*mb) (4.5724*mb, 3.4865*mb) (4.4166*mb, 3.6819*mb) (5.2540*mb, 6.0329*mb) (4.7125*mb, 6.4647*mb) (2.6068*mb, 5.1251*mb) (2.3817*mb, 5.2336*mb) (2.1161*mb, 7.7150*mb) (1.4409*mb, 7.8692*mb) (0.1249*mb, 5.7486*mb) (-0.1249*mb, 5.7486*mb) (-1.4409*mb, 7.8692*mb) (-2.1161*mb, 7.7150*mb) (-2.3817*mb, 5.2336*mb) (-2.6068*mb, 5.1251*mb) (-4.7125*mb, 6.4647*mb) (-5.2540*mb, 6.0329*mb) (-4.4166*mb, 3.6819*mb) (-4.5724*mb, 3.4865*mb) (-7.0507*mb, 3.7798*mb) (-7.3513*mb, 3.1558*mb) (-5.5767*mb, 1.4010*mb) (-5.6323*mb, 1.1574*mb) (-7.9925*mb, 0.3463*mb) (-7.9925*mb, -0.3463*mb) (-5.6323*mb, -1.1574*mb) (-5.5767*mb, -1.4010*mb) (-7.3513*mb, -3.1558*mb) (-7.0507*mb, -3.7798*mb) (-4.5724*mb, -3.4865*mb) (-4.4166*mb, -3.6819*mb) (-5.2540*mb, -6.0329*mb) (-4.7125*mb, -6.4647*mb) (-2.6068*mb, -5.1251*mb) (-2.3817*mb, -5.2336*mb) (-2.1161*mb, -7.7150*mb) (-1.4409*mb, -7.8692*mb) (-0.1249*mb, -5.7486*mb) (0.1249*mb, -5.7486*mb) (1.4409*mb, -7.8692*mb) (2.1161*mb, -7.7150*mb) (2.3817*mb, -5.2336*mb) (2.6068*mb, -5.1251*mb) (4.7125*mb, -6.4647*mb) (5.2540*mb, -6.0329*mb) (4.4166*mb, -3.6819*mb) (4.5724*mb, -3.4865*mb) (7.0507*mb, -3.7798*mb) (7.3513*mb, -3.1558*mb) (5.5767*mb, -1.4010*mb)
  fix o
}
sketch PinionAFront on XZ offset (Rp - w) {
  polyline closed o = (5.6323*mb*kf, -1.1574*mb*kf) (7.9925*mb*kf, -0.3463*mb*kf) (7.9925*mb*kf, 0.3463*mb*kf) (5.6323*mb*kf, 1.1574*mb*kf) (5.5767*mb*kf, 1.4010*mb*kf) (7.3513*mb*kf, 3.1558*mb*kf) (7.0507*mb*kf, 3.7798*mb*kf) (4.5724*mb*kf, 3.4865*mb*kf) (4.4166*mb*kf, 3.6819*mb*kf) (5.2540*mb*kf, 6.0329*mb*kf) (4.7125*mb*kf, 6.4647*mb*kf) (2.6068*mb*kf, 5.1251*mb*kf) (2.3817*mb*kf, 5.2336*mb*kf) (2.1161*mb*kf, 7.7150*mb*kf) (1.4409*mb*kf, 7.8692*mb*kf) (0.1249*mb*kf, 5.7486*mb*kf) (-0.1249*mb*kf, 5.7486*mb*kf) (-1.4409*mb*kf, 7.8692*mb*kf) (-2.1161*mb*kf, 7.7150*mb*kf) (-2.3817*mb*kf, 5.2336*mb*kf) (-2.6068*mb*kf, 5.1251*mb*kf) (-4.7125*mb*kf, 6.4647*mb*kf) (-5.2540*mb*kf, 6.0329*mb*kf) (-4.4166*mb*kf, 3.6819*mb*kf) (-4.5724*mb*kf, 3.4865*mb*kf) (-7.0507*mb*kf, 3.7798*mb*kf) (-7.3513*mb*kf, 3.1558*mb*kf) (-5.5767*mb*kf, 1.4010*mb*kf) (-5.6323*mb*kf, 1.1574*mb*kf) (-7.9925*mb*kf, 0.3463*mb*kf) (-7.9925*mb*kf, -0.3463*mb*kf) (-5.6323*mb*kf, -1.1574*mb*kf) (-5.5767*mb*kf, -1.4010*mb*kf) (-7.3513*mb*kf, -3.1558*mb*kf) (-7.0507*mb*kf, -3.7798*mb*kf) (-4.5724*mb*kf, -3.4865*mb*kf) (-4.4166*mb*kf, -3.6819*mb*kf) (-5.2540*mb*kf, -6.0329*mb*kf) (-4.7125*mb*kf, -6.4647*mb*kf) (-2.6068*mb*kf, -5.1251*mb*kf) (-2.3817*mb*kf, -5.2336*mb*kf) (-2.1161*mb*kf, -7.7150*mb*kf) (-1.4409*mb*kf, -7.8692*mb*kf) (-0.1249*mb*kf, -5.7486*mb*kf) (0.1249*mb*kf, -5.7486*mb*kf) (1.4409*mb*kf, -7.8692*mb*kf) (2.1161*mb*kf, -7.7150*mb*kf) (2.3817*mb*kf, -5.2336*mb*kf) (2.6068*mb*kf, -5.1251*mb*kf) (4.7125*mb*kf, -6.4647*mb*kf) (5.2540*mb*kf, -6.0329*mb*kf) (4.4166*mb*kf, -3.6819*mb*kf) (4.5724*mb*kf, -3.4865*mb*kf) (7.0507*mb*kf, -3.7798*mb*kf) (7.3513*mb*kf, -3.1558*mb*kf) (5.5767*mb*kf, -1.4010*mb*kf)
  fix o
}
loft PinionATeeth sections PinionABack, PinionAFront -> body PinionA
cylinder PinionAHub radius (hub_r - 1 mm) height (hub_l + 0.5 mm) base (0, Rp - 0.5 mm, 0) axis Y join targets PinionA
cylinder PinionABore radius (pin_r + play) height (w + hub_l + 2 mm) base (0, Rp - w - 1 mm, 0) axis Y cut targets PinionA

sketch PinionBBack on XZ offset (-Rp) {
  polyline closed o = (5.6323*mb, -1.1574*mb) (7.9925*mb, -0.3463*mb) (7.9925*mb, 0.3463*mb) (5.6323*mb, 1.1574*mb) (5.5767*mb, 1.4010*mb) (7.3513*mb, 3.1558*mb) (7.0507*mb, 3.7798*mb) (4.5724*mb, 3.4865*mb) (4.4166*mb, 3.6819*mb) (5.2540*mb, 6.0329*mb) (4.7125*mb, 6.4647*mb) (2.6068*mb, 5.1251*mb) (2.3817*mb, 5.2336*mb) (2.1161*mb, 7.7150*mb) (1.4409*mb, 7.8692*mb) (0.1249*mb, 5.7486*mb) (-0.1249*mb, 5.7486*mb) (-1.4409*mb, 7.8692*mb) (-2.1161*mb, 7.7150*mb) (-2.3817*mb, 5.2336*mb) (-2.6068*mb, 5.1251*mb) (-4.7125*mb, 6.4647*mb) (-5.2540*mb, 6.0329*mb) (-4.4166*mb, 3.6819*mb) (-4.5724*mb, 3.4865*mb) (-7.0507*mb, 3.7798*mb) (-7.3513*mb, 3.1558*mb) (-5.5767*mb, 1.4010*mb) (-5.6323*mb, 1.1574*mb) (-7.9925*mb, 0.3463*mb) (-7.9925*mb, -0.3463*mb) (-5.6323*mb, -1.1574*mb) (-5.5767*mb, -1.4010*mb) (-7.3513*mb, -3.1558*mb) (-7.0507*mb, -3.7798*mb) (-4.5724*mb, -3.4865*mb) (-4.4166*mb, -3.6819*mb) (-5.2540*mb, -6.0329*mb) (-4.7125*mb, -6.4647*mb) (-2.6068*mb, -5.1251*mb) (-2.3817*mb, -5.2336*mb) (-2.1161*mb, -7.7150*mb) (-1.4409*mb, -7.8692*mb) (-0.1249*mb, -5.7486*mb) (0.1249*mb, -5.7486*mb) (1.4409*mb, -7.8692*mb) (2.1161*mb, -7.7150*mb) (2.3817*mb, -5.2336*mb) (2.6068*mb, -5.1251*mb) (4.7125*mb, -6.4647*mb) (5.2540*mb, -6.0329*mb) (4.4166*mb, -3.6819*mb) (4.5724*mb, -3.4865*mb) (7.0507*mb, -3.7798*mb) (7.3513*mb, -3.1558*mb) (5.5767*mb, -1.4010*mb)
  fix o
}
sketch PinionBFront on XZ offset (-(Rp - w)) {
  polyline closed o = (5.6323*mb*kf, -1.1574*mb*kf) (7.9925*mb*kf, -0.3463*mb*kf) (7.9925*mb*kf, 0.3463*mb*kf) (5.6323*mb*kf, 1.1574*mb*kf) (5.5767*mb*kf, 1.4010*mb*kf) (7.3513*mb*kf, 3.1558*mb*kf) (7.0507*mb*kf, 3.7798*mb*kf) (4.5724*mb*kf, 3.4865*mb*kf) (4.4166*mb*kf, 3.6819*mb*kf) (5.2540*mb*kf, 6.0329*mb*kf) (4.7125*mb*kf, 6.4647*mb*kf) (2.6068*mb*kf, 5.1251*mb*kf) (2.3817*mb*kf, 5.2336*mb*kf) (2.1161*mb*kf, 7.7150*mb*kf) (1.4409*mb*kf, 7.8692*mb*kf) (0.1249*mb*kf, 5.7486*mb*kf) (-0.1249*mb*kf, 5.7486*mb*kf) (-1.4409*mb*kf, 7.8692*mb*kf) (-2.1161*mb*kf, 7.7150*mb*kf) (-2.3817*mb*kf, 5.2336*mb*kf) (-2.6068*mb*kf, 5.1251*mb*kf) (-4.7125*mb*kf, 6.4647*mb*kf) (-5.2540*mb*kf, 6.0329*mb*kf) (-4.4166*mb*kf, 3.6819*mb*kf) (-4.5724*mb*kf, 3.4865*mb*kf) (-7.0507*mb*kf, 3.7798*mb*kf) (-7.3513*mb*kf, 3.1558*mb*kf) (-5.5767*mb*kf, 1.4010*mb*kf) (-5.6323*mb*kf, 1.1574*mb*kf) (-7.9925*mb*kf, 0.3463*mb*kf) (-7.9925*mb*kf, -0.3463*mb*kf) (-5.6323*mb*kf, -1.1574*mb*kf) (-5.5767*mb*kf, -1.4010*mb*kf) (-7.3513*mb*kf, -3.1558*mb*kf) (-7.0507*mb*kf, -3.7798*mb*kf) (-4.5724*mb*kf, -3.4865*mb*kf) (-4.4166*mb*kf, -3.6819*mb*kf) (-5.2540*mb*kf, -6.0329*mb*kf) (-4.7125*mb*kf, -6.4647*mb*kf) (-2.6068*mb*kf, -5.1251*mb*kf) (-2.3817*mb*kf, -5.2336*mb*kf) (-2.1161*mb*kf, -7.7150*mb*kf) (-1.4409*mb*kf, -7.8692*mb*kf) (-0.1249*mb*kf, -5.7486*mb*kf) (0.1249*mb*kf, -5.7486*mb*kf) (1.4409*mb*kf, -7.8692*mb*kf) (2.1161*mb*kf, -7.7150*mb*kf) (2.3817*mb*kf, -5.2336*mb*kf) (2.6068*mb*kf, -5.1251*mb*kf) (4.7125*mb*kf, -6.4647*mb*kf) (5.2540*mb*kf, -6.0329*mb*kf) (4.4166*mb*kf, -3.6819*mb*kf) (4.5724*mb*kf, -3.4865*mb*kf) (7.0507*mb*kf, -3.7798*mb*kf) (7.3513*mb*kf, -3.1558*mb*kf) (5.5767*mb*kf, -1.4010*mb*kf)
  fix o
}
loft PinionBTeeth sections PinionBBack, PinionBFront -> body PinionB
cylinder PinionBHub radius (hub_r - 1 mm) height (hub_l + 0.5 mm) base (0, -(Rp + hub_l), 0) axis Y join targets PinionB
cylinder PinionBBore radius (pin_r + play) height (w + hub_l + 2 mm) base (0, -(Rp + hub_l + 1 mm), 0) axis Y cut targets PinionB

cylinder CrossPinRod radius pin_r height (2*(y_bridge + bridge_w) - 1 mm) base (0, -(y_bridge + bridge_w) + 0.5 mm, 0) axis Y new -> body CrossPin

// ==== Carrier halves: cheek plate, two bridges, the pin's half-holes, bolt holes; ring on the right
cylinder CheekR radius cheek_r height cheek_t base (x_cheek, 0, 0) axis X new -> body CarrierR
box BridgeRA size (x_cheek + 0.5 mm - 0.25 mm) bridge_w bridge_h corner (0.25 mm, y_bridge, -bridge_h/2) join targets CarrierR
box BridgeRB size (x_cheek + 0.5 mm - 0.25 mm) bridge_w bridge_h corner (0.25 mm, -y_bridge - bridge_w, -bridge_h/2) join targets CarrierR
sketch RingSk on YZ offset (x_cheek + cheek_t - 0.5 mm) {
  polyline closed o = (0.4072*mr, -18.7456*mr) (1.4019*mr, -20.9532*mr) (1.8932*mr, -20.9145*mr) (2.5302*mr, -18.5785*mr) (3.3347*mr, -18.4511*mr) (4.6624*mr, -20.4759*mr) (5.1416*mr, -20.3608*mr) (5.4054*mr, -17.9539*mr) (6.1800*mr, -17.7023*mr) (7.8081*mr, -19.4944*mr) (8.2635*mr, -19.3058*mr) (8.1475*mr, -16.8873*mr) (8.8732*mr, -16.5176*mr) (10.7616*mr, -18.0330*mr) (11.1818*mr, -17.7755*mr) (10.6889*mr, -15.4049*mr) (11.3478*mr, -14.9261*mr) (13.4501*mr, -16.1275*mr) (13.8249*mr, -15.8074*mr) (12.9672*mr, -13.5431*mr) (13.5431*mr, -12.9672*mr) (15.8074*mr, -13.8249*mr) (16.1275*mr, -13.4501*mr) (14.9261*mr, -11.3478*mr) (15.4049*mr, -10.6889*mr) (17.7755*mr, -11.1818*mr) (18.0330*mr, -10.7616*mr) (16.5176*mr, -8.8732*mr) (16.8873*mr, -8.1475*mr) (19.3058*mr, -8.2635*mr) (19.4944*mr, -7.8081*mr) (17.7023*mr, -6.1800*mr) (17.9539*mr, -5.4054*mr) (20.3608*mr, -5.1416*mr) (20.4759*mr, -4.6624*mr) (18.4511*mr, -3.3347*mr) (18.5785*mr, -2.5302*mr) (20.9145*mr, -1.8932*mr) (20.9532*mr, -1.4019*mr) (18.7456*mr, -0.4072*mr) (18.7456*mr, 0.4072*mr) (20.9532*mr, 1.4019*mr) (20.9145*mr, 1.8932*mr) (18.5785*mr, 2.5302*mr) (18.4511*mr, 3.3347*mr) (20.4759*mr, 4.6624*mr) (20.3608*mr, 5.1416*mr) (17.9539*mr, 5.4054*mr) (17.7023*mr, 6.1800*mr) (19.4944*mr, 7.8081*mr) (19.3058*mr, 8.2635*mr) (16.8873*mr, 8.1475*mr) (16.5176*mr, 8.8732*mr) (18.0330*mr, 10.7616*mr) (17.7755*mr, 11.1818*mr) (15.4049*mr, 10.6889*mr) (14.9261*mr, 11.3478*mr) (16.1275*mr, 13.4501*mr) (15.8074*mr, 13.8249*mr) (13.5431*mr, 12.9672*mr) (12.9672*mr, 13.5431*mr) (13.8249*mr, 15.8074*mr) (13.4501*mr, 16.1275*mr) (11.3478*mr, 14.9261*mr) (10.6889*mr, 15.4049*mr) (11.1818*mr, 17.7755*mr) (10.7616*mr, 18.0330*mr) (8.8732*mr, 16.5176*mr) (8.1475*mr, 16.8873*mr) (8.2635*mr, 19.3058*mr) (7.8081*mr, 19.4944*mr) (6.1800*mr, 17.7023*mr) (5.4054*mr, 17.9539*mr) (5.1416*mr, 20.3608*mr) (4.6624*mr, 20.4759*mr) (3.3347*mr, 18.4511*mr) (2.5302*mr, 18.5785*mr) (1.8932*mr, 20.9145*mr) (1.4019*mr, 20.9532*mr) (0.4072*mr, 18.7456*mr) (-0.4072*mr, 18.7456*mr) (-1.4019*mr, 20.9532*mr) (-1.8932*mr, 20.9145*mr) (-2.5302*mr, 18.5785*mr) (-3.3347*mr, 18.4511*mr) (-4.6624*mr, 20.4759*mr) (-5.1416*mr, 20.3608*mr) (-5.4054*mr, 17.9539*mr) (-6.1800*mr, 17.7023*mr) (-7.8081*mr, 19.4944*mr) (-8.2635*mr, 19.3058*mr) (-8.1475*mr, 16.8873*mr) (-8.8732*mr, 16.5176*mr) (-10.7616*mr, 18.0330*mr) (-11.1818*mr, 17.7755*mr) (-10.6889*mr, 15.4049*mr) (-11.3478*mr, 14.9261*mr) (-13.4501*mr, 16.1275*mr) (-13.8249*mr, 15.8074*mr) (-12.9672*mr, 13.5431*mr) (-13.5431*mr, 12.9672*mr) (-15.8074*mr, 13.8249*mr) (-16.1275*mr, 13.4501*mr) (-14.9261*mr, 11.3478*mr) (-15.4049*mr, 10.6889*mr) (-17.7755*mr, 11.1818*mr) (-18.0330*mr, 10.7616*mr) (-16.5176*mr, 8.8732*mr) (-16.8873*mr, 8.1475*mr) (-19.3058*mr, 8.2635*mr) (-19.4944*mr, 7.8081*mr) (-17.7023*mr, 6.1800*mr) (-17.9539*mr, 5.4054*mr) (-20.3608*mr, 5.1416*mr) (-20.4759*mr, 4.6624*mr) (-18.4511*mr, 3.3347*mr) (-18.5785*mr, 2.5302*mr) (-20.9145*mr, 1.8932*mr) (-20.9532*mr, 1.4019*mr) (-18.7456*mr, 0.4072*mr) (-18.7456*mr, -0.4072*mr) (-20.9532*mr, -1.4019*mr) (-20.9145*mr, -1.8932*mr) (-18.5785*mr, -2.5302*mr) (-18.4511*mr, -3.3347*mr) (-20.4759*mr, -4.6624*mr) (-20.3608*mr, -5.1416*mr) (-17.9539*mr, -5.4054*mr) (-17.7023*mr, -6.1800*mr) (-19.4944*mr, -7.8081*mr) (-19.3058*mr, -8.2635*mr) (-16.8873*mr, -8.1475*mr) (-16.5176*mr, -8.8732*mr) (-18.0330*mr, -10.7616*mr) (-17.7755*mr, -11.1818*mr) (-15.4049*mr, -10.6889*mr) (-14.9261*mr, -11.3478*mr) (-16.1275*mr, -13.4501*mr) (-15.8074*mr, -13.8249*mr) (-13.5431*mr, -12.9672*mr) (-12.9672*mr, -13.5431*mr) (-13.8249*mr, -15.8074*mr) (-13.4501*mr, -16.1275*mr) (-11.3478*mr, -14.9261*mr) (-10.6889*mr, -15.4049*mr) (-11.1818*mr, -17.7755*mr) (-10.7616*mr, -18.0330*mr) (-8.8732*mr, -16.5176*mr) (-8.1475*mr, -16.8873*mr) (-8.2635*mr, -19.3058*mr) (-7.8081*mr, -19.4944*mr) (-6.1800*mr, -17.7023*mr) (-5.4054*mr, -17.9539*mr) (-5.1416*mr, -20.3608*mr) (-4.6624*mr, -20.4759*mr) (-3.3347*mr, -18.4511*mr) (-2.5302*mr, -18.5785*mr) (-1.8932*mr, -20.9145*mr) (-1.4019*mr, -20.9532*mr) (-0.4072*mr, -18.7456*mr)
  fix o
}
extrude Ring from RingSk distance (ring_t + 0.5 mm) join targets CarrierR
cylinder CheekRBore radius (shaft_r + bear) height (cheek_t + ring_t + 2 mm) base (x_cheek - 1 mm, 0, 0) axis X cut targets CarrierR
cylinder PinSeatR radius pin_r height (2*(y_bridge + bridge_w) + 2 mm) base (0, -(y_bridge + bridge_w) - 1 mm, 0) axis Y cut targets CarrierR

cylinder CheekL radius cheek_r height cheek_t base (-(x_cheek + cheek_t), 0, 0) axis X new -> body CarrierL
box BridgeLA size (x_cheek + 0.5 mm - 0.25 mm) (bridge_w - 0.4 mm) (bridge_h - 0.4 mm) corner (-(x_cheek + 0.5 mm), y_bridge + 0.2 mm, -bridge_h/2 + 0.2 mm) join targets CarrierL
box BridgeLB size (x_cheek + 0.5 mm - 0.25 mm) (bridge_w - 0.4 mm) (bridge_h - 0.4 mm) corner (-(x_cheek + 0.5 mm), -y_bridge - bridge_w + 0.2 mm, -bridge_h/2 + 0.2 mm) join targets CarrierL
cylinder CheekLBore radius (shaft_r + bear) height (cheek_t + 2 mm) base (-(x_cheek + cheek_t + 1 mm), 0, 0) axis X cut targets CarrierL
// (0.05 mm looser than the right half's, so the two seats aren't one surface)
cylinder PinSeatL radius (pin_r + 0.05 mm) height (2*(y_bridge + bridge_w) + 2 mm) base (0, -(y_bridge + bridge_w) - 1 mm, 0) axis Y cut targets CarrierL

// M3 bolts along X through both halves, at the bridges' corners
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder BoltR{i}{j} radius 1.7 mm height (x_cheek + cheek_t + ring_t + 2 mm) base (-1 mm, (2*i - 1)*(y_bridge + bridge_w/2), (2*j - 1)*5 mm) axis X cut targets CarrierR
    cylinder BoltL{i}{j} radius 1.7 mm height (x_cheek + cheek_t + 2 mm) base (-(x_cheek + cheek_t + 1 mm), (2*i - 1)*(y_bridge + bridge_w/2), (2*j - 1)*5 mm) axis X cut targets CarrierL
  }
}

// ==== Output shafts: D flat inside the side gear, round through the cheek and the pillar, a dial
cylinder ShaftRRod radius shaft_r height (x_pillar + 10 mm - (Rp - w + 0.5 mm)) base (Rp - w + 0.5 mm, 0, 0) axis X new -> body ShaftR
box ShaftRFlat size (w + hub_l + 0.3 mm) (2*shaft_r + 2 mm) shaft_r corner (Rp - w, -shaft_r - 1 mm, flat) cut targets ShaftR
cylinder DialR radius dial_r height 3 mm base (x_pillar + 10 mm - 0.5 mm, 0, 0) axis X join targets ShaftR
box DialRMark size 3 mm 3 mm 4 mm corner (x_pillar + 9.5 mm, -1.5 mm, dial_r - 1 mm) join targets ShaftR

cylinder ShaftLRod radius shaft_r height (x_pillar + 10 mm - (Rp - w + 0.5 mm)) base (-(x_pillar + 10 mm), 0, 0) axis X new -> body ShaftL
box ShaftLFlat size (w + hub_l + 0.3 mm) (2*shaft_r + 2 mm) shaft_r corner (-(Rp + hub_l + 0.3 mm), -shaft_r - 1 mm, flat) cut targets ShaftL
cylinder DialL radius dial_r height 3 mm base (-(x_pillar + 12.5 mm), 0, 0) axis X join targets ShaftL
box DialLMark size 3 mm 3 mm 4 mm corner (-(x_pillar + 12.5 mm), -1.5 mm, dial_r - 1 mm) join targets ShaftL

// ==== Crank: 12-tooth pinion under the ring, its shaft through the right pillar's boss
sketch CrankSk on YZ offset (x_cheek + cheek_t - 0.5 mm) {
  polyline closed o = (1.0565*mr, -cd + 4.6310*mr) (0.2464*mr, -cd + 6.9957*mr) (-0.2464*mr, -cd + 6.9957*mr) (-1.0565*mr, -cd + 4.6310*mr) (-1.4006*mr, -cd + 4.5388*mr) (-3.2845*mr, -cd + 6.1816*mr) (-3.7112*mr, -cd + 5.9352*mr) (-3.2304*mr, -cd + 3.4824*mr) (-3.4824*mr, -cd + 3.2304*mr) (-5.9352*mr, -cd + 3.7112*mr) (-6.1816*mr, -cd + 3.2845*mr) (-4.5388*mr, -cd + 1.4006*mr) (-4.6310*mr, -cd + 1.0565*mr) (-6.9957*mr, -cd + 0.2464*mr) (-6.9957*mr, -cd + -0.2464*mr) (-4.6310*mr, -cd + -1.0565*mr) (-4.5388*mr, -cd + -1.4006*mr) (-6.1816*mr, -cd + -3.2845*mr) (-5.9352*mr, -cd + -3.7112*mr) (-3.4824*mr, -cd + -3.2304*mr) (-3.2304*mr, -cd + -3.4824*mr) (-3.7112*mr, -cd + -5.9352*mr) (-3.2845*mr, -cd + -6.1816*mr) (-1.4006*mr, -cd + -4.5388*mr) (-1.0565*mr, -cd + -4.6310*mr) (-0.2464*mr, -cd + -6.9957*mr) (0.2464*mr, -cd + -6.9957*mr) (1.0565*mr, -cd + -4.6310*mr) (1.4006*mr, -cd + -4.5388*mr) (3.2845*mr, -cd + -6.1816*mr) (3.7112*mr, -cd + -5.9352*mr) (3.2304*mr, -cd + -3.4824*mr) (3.4824*mr, -cd + -3.2304*mr) (5.9352*mr, -cd + -3.7112*mr) (6.1816*mr, -cd + -3.2845*mr) (4.5388*mr, -cd + -1.4006*mr) (4.6310*mr, -cd + -1.0565*mr) (6.9957*mr, -cd + -0.2464*mr) (6.9957*mr, -cd + 0.2464*mr) (4.6310*mr, -cd + 1.0565*mr) (4.5388*mr, -cd + 1.4006*mr) (6.1816*mr, -cd + 3.2845*mr) (5.9352*mr, -cd + 3.7112*mr) (3.4824*mr, -cd + 3.2304*mr) (3.2304*mr, -cd + 3.4824*mr) (3.7112*mr, -cd + 5.9352*mr) (3.2845*mr, -cd + 6.1816*mr) (1.4006*mr, -cd + 4.5388*mr)
  fix o
}
extrude CrankGear from CrankSk distance (ring_t + 0.5 mm) new -> body Crank
cylinder CrankShaft radius shaft_r height (x_pillar + 12 mm - (x_cheek + cheek_t)) base (x_cheek + cheek_t + ring_t - 0.5 mm, 0, -cd) axis X join targets Crank
box CrankFlat size 5 mm (2*shaft_r + 2 mm) shaft_r corner (x_pillar + 7.5 mm, -shaft_r - 1 mm, -cd + flat) cut targets Crank

// Crank arm, pressed on the D flat, with a handle peg
sketch ArmSk on YZ offset (x_pillar + 8.3 mm) {
  slot s = centers (0, -cd) (arm_l, -cd) width 12 mm
  fix s
}
extrude ArmPlate from ArmSk distance 3.7 mm new -> body Arm
cylinder ArmPeg radius 4 mm height 18 mm base (x_pillar + 11.5 mm, arm_l, -cd) axis X join targets Arm
cylinder ArmBoreTool radius (shaft_r + 0.1 mm) height 6 mm base (x_pillar + 7.3 mm, 0, -cd) axis X new -> body ArmBore
box ArmBoreFlat size 8 mm (2*shaft_r + 2 mm) shaft_r corner (x_pillar + 6.3 mm, -shaft_r - 1 mm, -cd + flat + 0.1 mm) cut targets ArmBore
combine ArmD target Arm tools ArmBore cut

// ==== Stand: base plate and two pillars; the right one carries the crank's boss
box BasePlate size (2*x_pillar + 30 mm) 60 mm base_t corner (-(x_pillar + 15 mm), -30 mm, z_base - base_t) new -> body Stand
box PillarL size 8 mm 24 mm (-z_base + 10 mm + 0.5 mm) corner (-(x_pillar + 8 mm), -12 mm, z_base - 0.5 mm) join targets Stand
box PillarR size 8 mm 24 mm (-z_base + 10 mm + 0.5 mm) corner (x_pillar, -12 mm, z_base - 0.5 mm) join targets Stand
cylinder CrankBoss radius 9 mm height 8 mm base (x_pillar - 0.01 mm, 0, -cd) axis X join targets Stand
cylinder TopL radius 12 mm height 8 mm base (-(x_pillar + 8 mm), 0, 0) axis X join targets Stand
cylinder TopR radius 12 mm height 8 mm base (x_pillar, 0, 0) axis X join targets Stand
cylinder BearL radius (shaft_r + bear) height 10 mm base (-(x_pillar + 9 mm), 0, 0) axis X cut targets Stand
cylinder BearR radius (shaft_r + bear) height 10 mm base (x_pillar - 1 mm, 0, 0) axis X cut targets Stand
cylinder BearC radius (shaft_r + bear) height 10 mm base (x_pillar - 1 mm, 0, -cd) axis X cut targets Stand

// ==== Checks: gears in mesh, everything that turns clear of what holds it
// The closest point of a bevel pair is a tooth tip passing the other gear's front edge; the
// flanks have more (the backlash)
check SideR clearance PinionA >= 0.05 mm
check SideR clearance PinionB >= 0.05 mm
check SideL clearance PinionA >= 0.05 mm
check SideL clearance PinionB >= 0.05 mm
check SideR clearance SideL >= 1 mm
check PinionA clearance CrossPin >= (play - 0.05 mm)
check PinionB clearance CrossPin >= (play - 0.05 mm)
check SideR clearance CarrierR >= (thrust - 0.05 mm)
check SideL clearance CarrierL >= (thrust - 0.05 mm)
check PinionA clearance CarrierR >= (thrust - 0.05 mm)
check PinionA clearance CarrierL >= (thrust - 0.05 mm)
check PinionB clearance CarrierR >= (thrust - 0.05 mm)
check PinionB clearance CarrierL >= (thrust - 0.05 mm)
check CarrierR clearance CarrierL >= 0.45 mm
check CrossPin clearance CarrierR >= 0 mm
check CrossPin clearance CarrierL >= 0 mm
check ShaftR clearance SideR >= 0.05 mm
check ShaftL clearance SideL >= 0.05 mm
check ShaftR clearance CarrierR >= (bear - 0.05 mm)
check ShaftL clearance CarrierL >= (bear - 0.05 mm)
check ShaftR clearance Stand >= (bear - 0.05 mm)
check ShaftL clearance Stand >= (bear - 0.05 mm)
check Crank clearance CarrierR >= 0.1 mm
check Crank clearance Stand >= (bear - 0.05 mm)
check Arm clearance Stand >= 0.25 mm
check Arm clearance Crank >= 0.05 mm
check CarrierR clearance Stand >= 2 mm
check CarrierL clearance Stand >= 2 mm

color SideR, SideL, PinionA, PinionB "#e0a030"
color CarrierR, CarrierL "#3d6fa5"
color CrossPin, ShaftR, ShaftL, Crank, Arm "#d0d0d0"
color Stand "#2b2b2b"
material SideR, SideL, PinionA, PinionB, CarrierR, CarrierL, CrossPin, ShaftR, ShaftL, Crank, Arm, Stand PLA
