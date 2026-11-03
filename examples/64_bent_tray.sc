// 64. Bent steel electronics tray: a floor with four mounting holes and four walls, one with a
// row of vent slots, cut from one flat blank and bent up on a press brake.
//
// Modelled as a solid (a box shelled open at the top, holes and vents cut through), then
// converted to sheet metal: the floor is the fixed face, and the four floor edges are bends of the
// sheet rule's radius. The wall corners, which are not bends, are cut open with the rule's gap,
// so the walls unfold flat. The flat pattern (DXF with bend notes, see 64_bent_tray.print.json)
// comes from the converted body, so it is right for the rule's thickness, radius and K-factor.
design BentTray units mm
param L = 120 mm
param W = 80 mm
param H = 30 mm
param t = 1.5 mm
param hole_d = 3.5 mm
param hole_inset = 10 mm
param vent_w = 4 mm
param vent_h = 14 mm
param vents = 6
param vent_pitch = 10 mm
sheet_rule Steel thickness t bend_radius t k_factor 0.44

box Blank size L W H corner (0,0,0) -> body Tray
shell Open faces (L/2, W/2, H) thickness t body Tray

// Mounting holes through the floor
sketch Holes on XY offset (-1 mm) {
  circle h1 = (hole_inset, hole_inset) diameter hole_d
  circle h2 = (L - hole_inset, hole_inset) diameter hole_d
  circle h3 = (hole_inset, W - hole_inset) diameter hole_d
  circle h4 = (L - hole_inset, W - hole_inset) diameter hole_d
  fix h1 h2 h3 h4
}
extrude HoleCut from Holes distance (t + 2 mm) cut targets Tray

// Vent slots through the back wall (y = W), straight through and square to it
sketch Vent on XZ offset (W + 1 mm) {
  rect v = (L/2 - (vents - 1)*vent_pitch/2 - vent_w/2, -(H/2 - vent_h/2)) (L/2 - (vents - 1)*vent_pitch/2 + vent_w/2, -(H/2 + vent_h/2))
  fix v
}
extrude VentCut from Vent distance (t + 2 mm) negative cut targets Tray
pattern Vents features VentCut dir (1,0,0) count vents spacing vent_pitch

convert Sheet body Tray face (L/2, W/2, 0) rule Steel bends (L/2, 0, 0) (L/2, W, 0) (0, W/2, 0) (L, W/2, 0)

check Tray size.x == L
check Tray size.z == H
