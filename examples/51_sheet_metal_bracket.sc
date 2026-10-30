// 51. Bent sheet-metal shelf bracket: a 1.5 mm steel L with a stiffening return, cut flat by
// laser and bent on a press brake.
//
// Modelled with the wall plate lying on XY (in use, XY is the wall and +Z points out of it). The
// wall plate screws on through two slots, so the bracket can be levelled. The shelf leg bends 90
// deg out of the wall along the bottom edge (y = 0) and carries two holes for the shelf screws.
// A short return flange bends down at the shelf's tip to stiffen it against twisting. Bend
// radius, thickness and K-factor come from the sheet rule, so the flat pattern (DXF, see
// 51_sheet_metal_bracket.print.json) is right for the material: change the rule to retarget it.
design SheetMetalBracket units mm
param t = 1.5 mm
param bend_r = 1.5 mm
param k = 0.44
param W = 40 mm
param wall_leg = 60 mm
param shelf_leg = 80 mm
param return_h = 10 mm
param screw_d = 4.5 mm
param slot_len = 8 mm
sheet_rule Steel thickness t bend_radius bend_r k_factor k

// Wall leg: the flat blank, with two vertical slots for the wall screws
sketch WallBlank on XY {
  rect r = (0,0) (W, wall_leg)
  slot s1 = centers (W/2, 15 mm) (W/2, 15 mm + slot_len) width screw_d
  slot s2 = centers (W/2, wall_leg - 25 mm) (W/2, wall_leg - 25 mm + slot_len) width screw_d
  fix r.bottom r.right r.top r.left s1.line1 s1.line2 s2.line1 s2.line2
}
flange Wall from WallBlank profiles (2 mm, 2 mm) rule Steel -> body Bracket

// Shelf leg bends out of the wall along the bottom edge, then the return at its tip
flange Shelf edges (W/2, 0, 0) height shelf_leg flip body Bracket
flange Return edges (W/2, 0, shelf_leg) height return_h body Bracket

// Shelf screw holes: a straight cut square to the shelf leg, so the sheet keeps them as
// cut-outs in its flat pattern. (XZ sketch coordinates are (x, -z).)
sketch ShelfHoles on XZ offset (-1 mm) {
  circle h1 = (W/2, -shelf_leg*0.35) diameter screw_d
  circle h2 = (W/2, -shelf_leg*0.75) diameter screw_d
  fix h1 h2
}
extrude ShelfHoleCut from ShelfHoles distance (t + 2 mm) cut targets Bracket
