// 33. Wall hook for headphones or a coat: a rounded bar swept along a line-and-arc path.
//
// The wall is the plane y = 0, Y points out of the wall, Z up, X across the hook. A flat wall
// plate (plate_t thick) takes two countersunk screws, one above the arm and one below. The bar is
// a sweep: its profile is drawn in the plane y = 0 where the path starts, and the path (a line out
// from the wall at arm_z, a quarter arc of centreline radius bend_r turning up, then a short
// upright lip) lies in the YZ plane. Whatever hangs on it settles into the bend. The profile is
// a width x bar_h rectangle with its top-side corners rounded and its bed-side corners chamfered,
// so the bar is round to the hand and still flat on the bed. The lip ends in a half round, and a
// gusset under the arm carries the bending load into the plate.
//
// Print lying on its side (the x = 0 face on the bed), no supports: the path is planar and every
// other feature is an extrusion along X, so the hook outline is drawn in each layer and the bar
// bends along its layers, not across them. The bed-side corners are 45 deg chamfers rather than
// fillets so they do not droop. The screw holes then run horizontally; they are small and the
// 90 deg countersinks self-support at 45 deg.
design SweptWallHook units mm
// Screws: M4 or #8 flat head
param screw_clear = 4.5 mm
param head_d = 8.6 mm
param head_angle = 90 deg
// Plate
param width = 20 mm
param plate_t = 5 mm
param plate_h = 100 mm
param plate_round = 2 mm
param screw_inset = 10 mm
// Swept bar: path and profile
param arm_z = 45 mm
param arm_len = 50 mm
param bend_r = 14 mm
param lip_len = 10 mm
param bar_h = 12 mm
param edge_round = 3 mm
param tip_over = 0.4 mm
param bed_chamfer = 1.5 mm
param bar_sink = 1 mm
// Gusset under the arm
param gusset_len = 22 mm
param gusset_drop = 24 mm
// Inset from both faces, clear of the bar's rounds and chamfers
param gusset_inset = 3.5 mm
let h = bar_h/2
let y1 = plate_t + arm_len
let bend_z = arm_z + bend_r
let lip_y = y1 + bend_r
let tip_z = bend_z + lip_len

// Wall plate, front corners rounded
sketch PlateSk on YZ offset 0 {
  polyline closed plate = (0,0) (plate_t,0) (plate_t,plate_h) (0,plate_h)
  fillet plate.2.end radius plate_round
  fillet plate.3.end radius plate_round
  fix plate.1 plate.2 plate.3 plate.4
}
extrude Plate from PlateSk distance width -> body Hook

// Gusset under the arm, its top 0.3 mm up into the bar so they overlap
sketch GussetSk on YZ offset gusset_inset {
  polyline closed g = (plate_t - 1 mm,arm_z - h + 0.3 mm) (plate_t - 1 mm,arm_z - h - gusset_drop) (plate_t,arm_z - h - gusset_drop) (plate_t + gusset_len,arm_z - h + 0.3 mm)
  fix g.1 g.2 g.3 g.4
}
extrude Gusset from GussetSk distance (width - 2*gusset_inset) join targets Hook

// The bar's profile, in the plane where the path starts, bar_sink into the plate so the two
// fuse without sharing a face (XZ sketch: v = -z).
// Top-side corners rounded, bed-side corners chamfered at 45 deg.
sketch BarProfile on XZ offset bar_sink {
  polyline closed p = (bed_chamfer,-(arm_z - h)) (width,-(arm_z - h)) (width,-(arm_z + h)) (bed_chamfer,-(arm_z + h)) (0,-(arm_z + h - bed_chamfer)) (0,-(arm_z - h + bed_chamfer))
  fillet p.1.end radius edge_round
  fillet p.2.end radius edge_round
  fix p.1 p.2 p.3 p.4 p.5 p.6
}
// The path: out from the wall, a quarter turn up, then the lip
sketch BarPath on YZ offset 0 {
  line arm = (bar_sink,arm_z) (y1,arm_z)
  arc bend = center (y1,bend_z) start (y1,arm_z) end (lip_y,bend_z)
  line lip = (lip_y,bend_z) (lip_y,tip_z)
  fix arm bend lip
}
sweep Bar from BarProfile path BarPath.arm, BarPath.bend, BarPath.lip join targets Hook
// Round the end of the lip: a box with a cylinder taken out of it is the cutter. The cylinder is
// tip_over wider than the bar so its sides are not tangent to the lip's (the kernel's booleans
// fail on tangent faces); the arc meets them in a soft crease.
box TipCutter size (width + 2 mm) (bar_h + 2 mm) (h + 1 mm) corner (-1 mm,lip_y - h - 1 mm,tip_z - h) new -> body TipTool
cylinder TipShape radius (h + tip_over) height (width + 4 mm) base (-2 mm,lip_y,tip_z - h) axis X cut targets TipTool
combine TipCut target Hook tools TipTool cut

// Countersunk screw holes through the plate, heads on the front face
hole TopScrew at (width/2,plate_t,plate_h - screw_inset) diameter screw_clear countersink head_d head_angle depth plate_t
hole LowScrew at (width/2,plate_t,screw_inset) diameter screw_clear countersink head_d head_angle depth plate_t
