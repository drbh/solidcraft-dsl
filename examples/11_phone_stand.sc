// 11. Phone stand: rounded base, angled back rest, front lip and a cable pass-through.
design PhoneStand units mm
param W = 80 mm
param D = 90 mm
param base_t = 6 mm
param corner_r = 8 mm
sketch Footprint on XY {
  rect rounded base = (0,0) (W,D) radius corner_r
  fix base.bottom base.right base.top base.left base.arc1 base.arc2 base.arc3 base.arc4
}
extrude Base from Footprint distance base_t -> body Stand
param rest_len = 100 mm
param rest_t = 8 mm
param lean = 65 deg
param rest_y = 30 mm
let run = rest_len*cos(lean)
let rise = rest_len*sin(lean)
let shift = rest_t/sin(lean)
sketch Side on YZ {
  polyline closed rest = (rest_y,0) (rest_y+shift,0) (rest_y+shift+run,rise) (rest_y+run,rise)
  fix rest.1 rest.2 rest.3 rest.4
}
extrude Rest from Side distance W join targets Stand
param phone_gap = 14 mm
param lip_t = 5 mm
param lip_h = 14 mm
let lip_y = rest_y - phone_gap - lip_t
box Lip size W lip_t lip_h corner (0,lip_y,0) join targets Stand
// Cut the window before cable details to avoid a shell-classification failure.
param window_w = 16 mm
param window_lo = 40 mm
param window_hi = 76 mm
sketch Window on XZ {
  slot opening = centers (W/2,-window_lo) (W/2,-window_hi) width window_w
  fix opening.line1 opening.line2 opening.arc1 opening.arc2
}
extrude WindowCut from Window distance (2*D) symmetric cut targets Stand
param cable_w = 12 mm
param cable_d = 9 mm
param channel_h = 3 mm
let cable_y = rest_y - phone_gap/2
box Notch size cable_w (lip_t+2 mm) lip_h corner (W/2-cable_w/2,lip_y-1 mm,base_t) cut targets Stand
hole CableHole at (W/2,cable_y,base_t) diameter cable_d depth base_t
box Channel size cable_d (D-cable_y+1 mm) (channel_h+1 mm) corner (W/2-cable_d/2,cable_y,-1 mm) cut targets Stand
param edge_r = 2 mm
fillet Softening edges (W/4,lip_y,lip_h) (W/4,lip_y+lip_t,lip_h) (3*W/4,lip_y,lip_h) (3*W/4,lip_y+lip_t,lip_h) (W/4,rest_y+run,rise) (W/4,rest_y+shift+run,rise) radius edge_r body Stand
