// 43. Filament spool holder: the spool rim rolls on two rollers, each spinning on a 608 bearing.
//
// X runs between the rollers, Y along the spool axis, Z up; the origin is the middle of the
// base on the bed. A 200 mm spool rests on two rollers placed cradle degrees either side of
// straight down. Each Roller is a tube that spans both spool flanges and carries one 608 bearing
// (8 x 22 x 7) pressed into a bearing_fit seat at its middle; the bearing turns on an 8 mm axle
// (M8 bolt with a nut, or 8 mm rod) that drops into open U-slots in the two cheeks of the Stand.
// The bearing goes in from one end through a slightly larger run-in bore and stops on a ledge.
// The spool's weight keeps the axles seated; lift the spool off to take a roller out.
//
// Print the Stand as modelled, no supports: the cheeks are vertical walls, the U-slots open
// upward and the cheek steps are 45 deg. Print each Roller standing on its ledge end (the -Y end):
// the bores only get wider going up (axle bore, ledge, press seat, run-in), so nothing overhangs.
design SpoolHolder units mm
// Spool
param spool_d = 200 mm
param spool_w = 70 mm
// 608 bearing and axle
param bearing_od = 22 mm
param bearing_w = 7 mm
param bearing_fit = 22.1 mm
param run_in = 22.6 mm
param ledge_d = 18 mm
param axle_slot = 8.3 mm
// Rollers and stand
param roller_od = 30 mm
param roller_play = 3 mm
param cradle = 30 deg
param axle_z = 20 mm
param base_t = 4 mm
param cheek_t = 5 mm
param side_gap = 1 mm
param frame = 12 mm
param tower = 9 mm
param low_h = 12 mm
let roller_len = spool_w + 2*roller_play
let xs = (spool_d/2 + roller_od/2) * sin(cradle)
let cheek_in = roller_len/2 + side_gap
let cheek_out = cheek_in + cheek_t
let stand_l = 2*xs + roller_od + 2*8 mm
let cheek_h = axle_z + 10 mm
let step_x = xs - tower

// Base frame
box Base size stand_l (2*cheek_out) base_t corner (-stand_l/2, -cheek_out, 0) -> body Stand
box BaseWindow size (stand_l - 2*frame) (2*cheek_in - 2*frame + 2*cheek_t) (base_t + 2 mm) corner (-stand_l/2 + frame, -cheek_in + frame - cheek_t, -1 mm) cut targets Stand

// Cheeks: towers under the axles, lower in the middle, 45 deg steps (XZ sketch v = -z)
sketch CheekSk on XZ offset (-cheek_out) {
  polyline closed c = (-stand_l/2, 0) (stand_l/2, 0) (stand_l/2, -cheek_h) (step_x, -cheek_h) (step_x - (cheek_h - low_h), -low_h) (-step_x + (cheek_h - low_h), -low_h) (-step_x, -cheek_h) (-stand_l/2, -cheek_h)
  fix c.1 c.2 c.3 c.4 c.5 c.6 c.7 c.8
}
extrude CheekA from CheekSk distance cheek_t join targets Stand
sketch CheekBSk on XZ offset cheek_in {
  polyline closed c = (-stand_l/2, 0) (stand_l/2, 0) (stand_l/2, -cheek_h) (step_x, -cheek_h) (step_x - (cheek_h - low_h), -low_h) (-step_x + (cheek_h - low_h), -low_h) (-step_x, -cheek_h) (-stand_l/2, -cheek_h)
  fix c.1 c.2 c.3 c.4 c.5 c.6 c.7 c.8
}
extrude CheekB from CheekBSk distance cheek_t join targets Stand

// Open U-slots for the axles, through both cheeks
sketch AxleSlotSk on XZ offset (-cheek_out - 1 mm) {
  slot a = (-xs, -axle_z) (-xs, -cheek_h - 6 mm) width axle_slot
  slot b = (xs, -axle_z) (xs, -cheek_h - 6 mm) width axle_slot
  fix a.line1 a.line2 a.arc1 a.arc2 b.line1 b.line2 b.arc1 b.arc2
}
extrude AxleSlots from AxleSlotSk distance (2*cheek_out + 2 mm) cut targets Stand
// The top edge of each slot wall, picked mid-way through the cheek
chamfer SlotLeadIn edges (-xs - axle_slot/2, -cheek_out + cheek_t/2, cheek_h) (-xs + axle_slot/2, -cheek_out + cheek_t/2, cheek_h) (xs - axle_slot/2, -cheek_out + cheek_t/2, cheek_h) (xs + axle_slot/2, -cheek_out + cheek_t/2, cheek_h) (-xs - axle_slot/2, cheek_out - cheek_t/2, cheek_h) (-xs + axle_slot/2, cheek_out - cheek_t/2, cheek_h) (xs - axle_slot/2, cheek_out - cheek_t/2, cheek_h) (xs + axle_slot/2, cheek_out - cheek_t/2, cheek_h) distance 1 mm body Stand

// Rollers: tube with a bearing seat at the middle; ledge bore on the -Y half, run-in on the +Y half
cylinder RollerATube radius (roller_od/2) height roller_len base (-xs, -roller_len/2, axle_z) axis Y new -> body RollerA
cylinder RollerALedge radius (ledge_d/2) height (roller_len + 2 mm) base (-xs, -roller_len/2 - 1 mm, axle_z) axis Y cut targets RollerA
cylinder RollerASeat radius (bearing_fit/2) height (roller_len/2 + bearing_w/2 + 1 mm) base (-xs, -bearing_w/2, axle_z) axis Y cut targets RollerA
cylinder RollerARunIn radius (run_in/2) height (roller_len/2 - bearing_w/2 + 1 mm) base (-xs, bearing_w/2, axle_z) axis Y cut targets RollerA
cylinder RollerBTube radius (roller_od/2) height roller_len base (xs, -roller_len/2, axle_z) axis Y new -> body RollerB
cylinder RollerBLedge radius (ledge_d/2) height (roller_len + 2 mm) base (xs, -roller_len/2 - 1 mm, axle_z) axis Y cut targets RollerB
cylinder RollerBSeat radius (bearing_fit/2) height (roller_len/2 + bearing_w/2 + 1 mm) base (xs, -bearing_w/2, axle_z) axis Y cut targets RollerB
cylinder RollerBRunIn radius (run_in/2) height (roller_len/2 - bearing_w/2 + 1 mm) base (xs, bearing_w/2, axle_z) axis Y cut targets RollerB
// Small chamfer on the roller ends so the spool flange rides on smoothly
chamfer RollerAEnds edges (-xs, -roller_len/2, axle_z + roller_od/2) (-xs, roller_len/2, axle_z + roller_od/2) distance 1 mm body RollerA
chamfer RollerBEnds edges (xs, -roller_len/2, axle_z + roller_od/2) (xs, roller_len/2, axle_z + roller_od/2) distance 1 mm body RollerB

// Print orientations (also used by the overhang checks); prints without supports.
check Stand overhang <= 45 deg
print RollerA up Y
check RollerA overhang <= 45 deg
print RollerB up Y
check RollerB overhang <= 45 deg
