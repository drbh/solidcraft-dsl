// 44. Stackable drawer unit: a cabinet with one sliding drawer; units stack and key into each other.
//
// X runs across the front, Y from the front (y = 0) to the back, Z up; the origin is the cabinet's
// front-left-bottom corner. The Cabinet is a box open at the front. The Drawer slides in with
// clearance on every side (0.4 mm per side by default) and stops against the back wall; its pull
// is a lip with a flat top and a 45 deg underside. Two rails with 45 deg flanks run along the top
// of the cabinet, and two matching grooves (rail + stack_fit on every face) run the full depth of
// the bottom, so a unit set on another drops onto its rails and can only slide front to back.
//
// Print the Cabinet on its back (front opening up): walls, rails and grooves are then all vertical
// and nothing overhangs. Print the Drawer upright as modelled; the pull's underside is 45 deg.
design StackableDrawers units mm
// Outside size
param W = 120 mm
param D = 160 mm
param H = 60 mm
// Cabinet
param wall = 2 mm
param floor_t = 3.5 mm
// Drawer
param clearance = 0.4 mm
param dwall = 1.6 mm
param dfloor = 1.6 mm
param front_t = 2.4 mm
param pull_w = 40 mm
param pull_out = 10 mm
param pull_lip = 3 mm
// Stacking rails and grooves
param rail_w = 7 mm
param rail_h = 1.5 mm
param rail_inset = 5 mm
param stack_fit = 0.3 mm
let open_w = W - 2*wall
let open_h = H - floor_t - wall
let open_d = D - wall
let dw = open_w - 2*clearance
let dh = open_h - 2*clearance
let dd = open_d - clearance
let dx = wall + clearance
let dz = floor_t + clearance
let rx1 = W/4
let rx2 = 3*W/4
let gw = rail_w + 2*stack_fit
let gh = rail_h + stack_fit

// Cabinet shell, open at the front
box CabinetBlock size W D H -> body Cabinet
box Opening size open_w (open_d + 1 mm) open_h corner (wall, -1 mm, floor_t) cut targets Cabinet

// Rails on top (XZ sketch v = -z), from rail_inset behind the front to the back
sketch RailSk on XZ offset rail_inset {
  polyline closed a = (rx1 - rail_w/2, -H + 0.01 mm) (rx1 + rail_w/2, -H + 0.01 mm) (rx1 + rail_w/2 - rail_h, -H - rail_h) (rx1 - rail_w/2 + rail_h, -H - rail_h)
  polyline closed b = (rx2 - rail_w/2, -H + 0.01 mm) (rx2 + rail_w/2, -H + 0.01 mm) (rx2 + rail_w/2 - rail_h, -H - rail_h) (rx2 - rail_w/2 + rail_h, -H - rail_h)
  fix a.1 a.2 a.3 a.4 b.1 b.2 b.3 b.4
}
extrude Rails from RailSk distance (D - rail_inset) join targets Cabinet

// Grooves under the bottom, full depth, the rail's shape grown by stack_fit
sketch GrooveSk on XZ offset (-1 mm) {
  polyline closed a = (rx1 - gw/2 - 1 mm, 1 mm) (rx1 + gw/2 + 1 mm, 1 mm) (rx1 + gw/2 - gh, -gh) (rx1 - gw/2 + gh, -gh)
  polyline closed b = (rx2 - gw/2 - 1 mm, 1 mm) (rx2 + gw/2 + 1 mm, 1 mm) (rx2 + gw/2 - gh, -gh) (rx2 - gw/2 + gh, -gh)
  fix a.1 a.2 a.3 a.4 b.1 b.2 b.3 b.4
}
extrude Grooves from GrooveSk distance (D + 2 mm) cut targets Cabinet

// Drawer, in place in the cabinet
box DrawerBlock size dw dd dh corner (dx, 0, dz) new -> body Drawer
box DrawerBin size (dw - 2*dwall) (dd - front_t - dwall) dh corner (dx + dwall, front_t, dz + dfloor) cut targets Drawer

// Pull: flat top, 45 deg underside (YZ sketch: u = y, v = z), centred on the front
let pull_top = dz + dh - 4 mm
sketch PullSk on YZ offset (W/2 - pull_w/2) {
  polyline closed p = (0.01 mm, pull_top - pull_out - pull_lip) (-pull_out, pull_top - pull_lip) (-pull_out, pull_top) (0.01 mm, pull_top)
  fix p.1 p.2 p.3 p.4
}
extrude Pull from PullSk distance pull_w join targets Drawer
fillet PullEdges edges (W/2, -pull_out, pull_top) radius 1 mm body Drawer
