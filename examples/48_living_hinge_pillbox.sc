// 48. Seven-day pill box in one piece: each compartment has its own lid on a living hinge.
//
// X runs along the row of compartments, Y from the tray front (y = 0) back toward the lids, Z up;
// the origin is the tray's front-left-bottom corner. The Pillbox prints flat as one body: the
// tray of seven cells, and behind it seven lids lying inside face up, each tied to the tray by a
// thin band (hinge_t, 0.4 mm = two 0.2 mm layers) on the bed. The band is as long as the tray
// is tall plus the lid thickness, so a lid folds up the back wall and over onto its cell
// (the band bends 90 deg at the tray's bottom edge and again at the lid's edge). Each lid has a
// plug lip that drops into its cell with lip_clear all round; a small 45 deg bump on the front of
// the lip snaps into a groove inside the tray's front wall. The day initials are debossed into
// the tray front.
//
// Print as modelled, no supports: the cells, lips and bands are straight up from the bed, and
// the latch bump and groove are 45 deg. Print in PP or PETG for a hinge that survives many folds.
design LivingHingePillbox units mm
// Compartments
param days = 7
param cell_w = 22 mm
param cell_d = 28 mm
param H = 18 mm
param wall = 1.6 mm
param floor_t = 1.6 mm
// Lids and hinge
param lid_t = 1.6 mm
param lid_gap = 0.6 mm
param tab = 4 mm
param lip_h = 2.5 mm
param lip_t = 1 mm
param lip_clear = 0.2 mm
param hinge_t = 0.4 mm
param hinge_w = 10 mm
param hinge_slack = 0.8 mm
// Latch
param bump = 0.4 mm
param bump_z = 1.2 mm
param groove = 0.5 mm
// Labels
param label_h = 7 mm
param label_z = 5.5 mm
param label_depth = 0.5 mm
let pitch = cell_w + wall
let L = days*pitch + wall
let Dt = cell_d + 2*wall
let band = H + lid_t + hinge_slack
let y_l = Dt + band
let lid_w = pitch - lid_gap
let lip_x = wall + lip_clear
let lip_w = cell_w - 2*lip_clear
let lip_y0 = y_l + wall + lip_clear
let lip_d = cell_d - 2*lip_clear
let bump_y = lip_y0 + lip_d
let bump_zc = lid_t + bump_z
let groove_z = H - bump_z

// Tray with seven cells
box TrayBlock size L Dt H -> body Pillbox
box Cell size cell_w cell_d H corner (wall, wall, floor_t) cut targets Pillbox
pattern Cells features Cell dir (1,0,0) count days spacing pitch

// Latch groove inside the front wall, 45 deg flanks (YZ sketch: u = y, v = z)
sketch GrooveSk on YZ offset wall {
  polyline closed g = (wall + 0.01 mm, groove_z - groove) (wall - groove, groove_z) (wall + 0.01 mm, groove_z + groove)
  fix g.1 g.2 g.3
}
extrude Groove from GrooveSk distance (L - 2*wall) cut targets Pillbox

// One lid on its hinge band, then patterned along the row
box Hinge size hinge_w (band + 2 mm) hinge_t corner (pitch/2 + wall/2 - hinge_w/2, Dt - 1 mm, 0) join targets Pillbox
box LidPlate size lid_w Dt lid_t corner (lid_gap/2, y_l, 0) join targets Pillbox
box LidTab size (lid_w/2) tab lid_t corner (lid_gap/2 + lid_w/4, y_l + Dt, 0) join targets Pillbox
box LipBlock size lip_w lip_d lip_h corner (lip_x, lip_y0, lid_t - 0.01 mm) join targets Pillbox
box LipHollow size (lip_w - 2*lip_t) (lip_d - 2*lip_t) (lip_h + 1 mm) corner (lip_x + lip_t, lip_y0 + lip_t, lid_t) cut targets Pillbox
sketch BumpSk on YZ offset (lip_x + 3 mm) {
  polyline closed b = (bump_y - 0.2 mm, bump_zc - bump) (bump_y + bump, bump_zc) (bump_y - 0.2 mm, bump_zc + bump)
  fix b.1 b.2 b.3
}
extrude Bump from BumpSk distance (lip_w - 6 mm) join targets Pillbox
pattern Lids features Hinge, LidPlate, LidTab, LipBlock, LipHollow, Bump dir (1,0,0) count days spacing pitch

// Day initials on the tray front, Monday first. `flip` turns the XZ sketch to face -Y so the
// letters read from the front (sketch v = +z). Text positions use params only: `text` does
// not resolve `let` helpers.
sketch DaysSk on XZ offset 0 flip {
  text "M" at (wall + cell_w/2, label_z) height label_h align center
  text "T" at (wall + cell_w/2 + 1*(cell_w + wall), label_z) height label_h align center
  text "W" at (wall + cell_w/2 + 2*(cell_w + wall), label_z) height label_h align center
  text "T" at (wall + cell_w/2 + 3*(cell_w + wall), label_z) height label_h align center
  text "F" at (wall + cell_w/2 + 4*(cell_w + wall), label_z) height label_h align center
  text "S" at (wall + cell_w/2 + 5*(cell_w + wall), label_z) height label_h align center
  text "S" at (wall + cell_w/2 + 6*(cell_w + wall), label_z) height label_h align center
}
emboss DayLabels from DaysSk depth label_depth deboss targets Pillbox

// Prints without supports, in the print recipe's orientation.
check Pillbox overhang <= 45 deg
