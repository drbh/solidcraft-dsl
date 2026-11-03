// 54. Nesting parts tray: an open tray with drafted walls, so empty trays stack inside each other,
// split into compartments by divider webs.
//
// The tray is a box whose sides are drafted out from the bed at `draft_a`, then hollowed from the
// top, so the walls lean out evenly and keep their thickness; a tray then drops into the one below
// until its walls meet. The dividers are webs: lines on the floor, grown
// up to `divider_h` with `direction normal` (`flip` grows them away from the floor).
//
// Print as modelled, on its floor: the walls lean out by `draft_a` and the webs are vertical.
design NestingTray units mm
param W = 120 mm
param D = 80 mm
param H = 30 mm
param wall = 1.6 mm
param floor_t = 1.6 mm
param draft_a = 4 deg
param divider_t = 1.2 mm
param divider_h = 20 mm

box Block size W D H corner (0,0,0) -> body Tray
// Lean the four sides out from the bed, then hollow from the top: the shell offsets the drafted
// faces, so the inner walls lean the same way and the wall keeps its thickness.
draft Walls faces (W/2,0,H/2) (W/2,D,H/2) (0,D/2,H/2) (W,D/2,H/2) angle (-draft_a) neutral XY body Tray
shell Hollow faces (W/2,D/2,H) thickness wall inside body Tray

// Two dividers across the width and one along the length, grown up from the floor.
sketch Dividers on XY offset floor_t {
  line a = (W/3,wall + 2 mm) (W/3,D - wall - 2 mm)
  line b = (2*W/3,wall + 2 mm) (2*W/3,D - wall - 2 mm)
  line c = (wall + 2 mm,D/2) (W/3,D/2)
  fix a b c
}
web Divide from Dividers curves a, b, c thickness divider_t depth divider_h direction normal flip

color Tray "#3a7bd5"
material Tray PLA

check Tray overhang <= 45 deg
check Tray size.z == H
