// Lid for a bowl with a 154 mm outside diameter.
// Continuous top with a 30 mm wide handle opening in the +X side rim only.
// Lightweight cover: 0.8 mm top and wall, 4 mm rim depth, 0.4 mm radial clearance.
// The handle opening removes the full rim height, leaving only the top plate.
// Print top-down: the top plate rests on Z=0 and the rim extends upward.
design BowlLid units mm
param bowl_d = 154 mm
param clearance = 0.4 mm
param thickness = 0.8 mm
param rim_wall = 0.8 mm
param rim_depth = 4 mm
param handle_width = 30 mm
let inside_r = bowl_d/2 + clearance
let lid_r = inside_r + rim_wall
cylinder Lid radius lid_r height thickness -> body BowlLid
cylinder Rim radius lid_r height rim_depth base (0,0,thickness) join targets BowlLid
cylinder Interior radius inside_r height (rim_depth+1 mm) base (0,0,thickness) cut targets BowlLid
box HandleOpening size (lid_r+1 mm) handle_width (rim_depth+1 mm) corner (0,-handle_width/2,thickness) cut targets BowlLid
