// 20. Table-edge clamp holding a 12 mm pole (phone arm), printed in PLA.
//
// X runs from the table edge (x = 0) in over the table, Z is up, Y runs along the edge.
// The C slips on as a tight fit; two M3 pilots in the lower jaw take self-tapping screws that
// press up on the underside of the table if it ever needs to be locked.
// The tube sits over the tabletop, so the arm's load bears down through the top jaw onto the table.
// Print on its side (one Y face on the bed) with supports under the tube: layers then run across
// the jaws and the tube, so neither the C nor the tube-to-jaw joint bends along a layer line.
design TablePoleClamp units mm
param table_t = 19 mm
param squeeze = 0.15 mm
param jaw_len = 40 mm
param wall = 6 mm
param W = 40 mm
let gap = table_t - squeeze
// C profile: spine at the table edge, lower and upper jaws reaching in over/under the table
box Spine size wall W (gap+2*wall) corner (-wall,0,-wall) -> body Clamp
box LowerJaw size jaw_len W wall corner (0,0,-wall) join targets Clamp
box UpperJaw size jaw_len W wall corner (0,0,gap) join targets Clamp
// Pole socket standing on the upper jaw, blind so the pole bottoms out on the jaw
param pole_d = 12 mm
param pole_fit = 0.25 mm
param tube_od = 22 mm
param tube_h = 35 mm
param tube_x = 20 mm
param base_r = 5 mm
let top_z = gap + wall
let bore_r = (pole_d + pole_fit)/2
cylinder Tube radius (tube_od/2) height tube_h base (tube_x,W/2,top_z) join targets Clamp
fillet TubeBase edges (tube_x+tube_od/2,W/2,top_z) radius base_r body Clamp
cylinder Bore radius bore_r height (tube_h+1 mm) base (tube_x,W/2,top_z) cut targets Clamp
chamfer BoreEntry edges (tube_x+bore_r,W/2,top_z+tube_h) distance 0.8 mm body Clamp
// Backup screws: M3 self-tapping into 2.5 mm pilots through the lower jaw
param screw_x = 28 mm
param pilot_d = 2.5 mm
cylinder Screw0 radius (pilot_d/2) height (wall+2 mm) base (screw_x,W/4,-wall-1 mm) cut targets Clamp
cylinder Screw1 radius (pilot_d/2) height (wall+2 mm) base (screw_x,3*W/4,-wall-1 mm) cut targets Clamp
// Lead-in on the jaw tips so the clamp starts onto the table, and rounded outer corners
param lead_in = 1.5 mm
chamfer LeadIn edges (jaw_len,W/2,0) (jaw_len,W/2,gap) distance lead_in body Clamp
param outer_r = 3 mm
fillet OuterCorners edges (-wall,W/2,-wall) (-wall,W/2,top_z) radius outer_r body Clamp
param inner_r = 1 mm
fillet InnerCorners edges (0,W/2,0) (0,W/2,gap) radius inner_r body Clamp
