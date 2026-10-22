// 12. Spoked V-groove pulley: revolved rim and hub, patterned spokes, keyed bore, bolt circle.
design SpokedPulley units mm
param R = 60 mm
param rim_t = 8 mm
param rim_w = 22 mm
param groove_d = 7 mm
param groove_open = 14 mm
param hub_r = 16 mm
param hub_h = 30 mm
param bore_r = 6 mm
let rim_in = R - rim_t - groove_d
let z0 = (hub_h - rim_w)/2
sketch RimSection on XZ {
  polyline closed rim = (rim_in,-z0) (R,-z0) (R,-(z0+rim_w/2-groove_open/2)) (R-groove_d,-(z0+rim_w/2)) (R,-(z0+rim_w/2+groove_open/2)) (R,-(z0+rim_w)) (rim_in,-(z0+rim_w))
  fix rim.1 rim.2 rim.3 rim.4 rim.5 rim.6 rim.7
}
revolve Rim from RimSection axis Z -> body Pulley
sketch HubSection on XZ {
  polyline closed hub = (0,0) (hub_r,0) (hub_r,-hub_h) (0,-hub_h)
  fix hub.1 hub.2 hub.3 hub.4
}
revolve Hub from HubSection axis Z new -> body HubBody
param spokes = 5
param spoke_w = 9 mm
param spoke_h = 12 mm
let spoke_len = rim_in - hub_r + 4 mm
box Spoke size spoke_len spoke_w spoke_h corner (hub_r-2 mm,-spoke_w/2,(hub_h-spoke_h)/2) join targets Pulley
pattern circular Spokes features Spoke axis Z count spokes angle 360 deg
param key_w = 4 mm
param key_d = 2.5 mm
param flange_r = 28 mm
param flange_t = 5 mm
cylinder Flange radius flange_r height flange_t join targets Pulley
cylinder Bore radius bore_r height (hub_h+2 mm) base (0,0,-1 mm) cut targets Pulley
box Keyway size (key_d+1 mm) key_w (hub_h+2 mm) corner (bore_r-1 mm,-key_w/2,-1 mm) cut targets Pulley
param bolt_circle_r = 22 mm
let bolts = spokes
let bolt_a = 180 deg / spokes
sketch BoltSeed on XY offset flange_t {
  point b = (bolt_circle_r*cos(bolt_a),bolt_circle_r*sin(bolt_a))
  fix b
}
hole BoltHole at BoltSeed clearance M4 counterbore 8 mm 2.5 mm depth flange_t
pattern circular BoltRing features BoltHole axis Z count bolts angle 360 deg
chamfer RimEdges edges (-R,0,z0) (-R,0,z0+rim_w) distance 1 mm body Pulley
chamfer BoreEntry edges (-bore_r,0,hub_h) (-bore_r,0,0) distance 0.8 mm body Pulley
fillet HubTop edges (-hub_r,0,hub_h) (-flange_r,0,flange_t) radius 1.5 mm body Pulley
param screw_r = 1.7 mm
let screw_a = 180 deg / spokes
let screw_z = (hub_h + (hub_h+spoke_h)/2)/2
cylinder SetScrew radius screw_r height (hub_r+2 mm) base (0,0,screw_z) axis (cos(screw_a),sin(screw_a),0) cut targets Pulley
