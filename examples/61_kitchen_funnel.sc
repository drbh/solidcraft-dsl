// 61. Kitchen funnel: a wide cone into a narrow spout, shelled to a thin wall, with a hanging tab.
//
// Both cones are `cone` primitives: each is a revolved profile whose radii and height are live
// dimensions, so it builds on any engine and follows parameter edits. The bowl tapers from
// `mouth_d` to `neck_d`, the spout from `neck_d` to `tip_d`; the shell opens both ends.
//
// Print standing on its rim (mouth down), no supports: the bowl and spout walls lean in at
// atan(radius change / height) from vertical, under 45 deg here (the overhang check confirms it).
design KitchenFunnel units mm
param mouth_d = 110 mm
param neck_d = 18 mm
param tip_d = 11 mm
param bowl_h = 70 mm
param spout_h = 40 mm
param wall = 1.6 mm
param tab_w = 14 mm
param hole_d = 6 mm

cone Bowl diameter mouth_d top_diameter neck_d height bowl_h -> body Funnel
cone Spout diameter neck_d top_diameter tip_d height (spout_h + 1 mm) base (0,0,bowl_h - 1 mm) join targets Funnel
shell Hollow faces (0,0,0) (0,0,bowl_h + spout_h) thickness wall inside body Funnel
// Hanging tab on the rim, with a hole
box Tab size tab_w (tab_w + 6 mm) wall corner (mouth_d/2 - 3 mm,-tab_w/2,0) join targets Funnel
cylinder TabHole diameter hole_d height (wall + 2 mm) base (mouth_d/2 + tab_w/2 + 1 mm,0,-1 mm) cut targets Funnel

check Funnel overhang <= 45 deg
check Funnel size.z == (bowl_h + spout_h)
