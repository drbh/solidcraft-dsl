// 153. Under-desk drawer: two screw-on C rails and a drawer that slides in them, with a pull-out stop and a pull.
//
// Z is up and the drawer is drawn shut in its rails, the desk's underside at the rails' top: X
// across (the drawer's left wall at x = 0), Y into the desk (the back of the drawer's front panel
// at y = 0), Z up from the drawer's floor. drawer_w, drawer_d and drawer_h are the inside sizes.
//
// Each rail is a C channel screwed up into the desk through three countersunk holes in its top
// jaw (fit the rails first; the drawer covers the screws). The drawer hangs by a runner along the
// top of each side wall: a flat strip `strip` wide at its outer edge, which rides on the rail's
// bottom lip, carried by a 45 deg slope down to the wall, so the drawer prints upright with only
// the strip as a short overhang. The top jaw sits `lift` over the runner, so the drawer can't
// tip down when pulled out.
//
// The stop: an M3 x 5 screw driven through a pilot hole in each rail's web, `stop_y` along it,
// stands 2 mm into a rebate in the runner's outer edge. The rebate ends 10 mm short of the
// drawer's back, so pulling the drawer out stops it after `travel` (70 % of its depth) with the
// rest still in the rails. Back the two screws out to take the drawer off; a cap at each rail's
// back end stops it going in too far.
//
// Print the drawer upright (the pull's bar is a short bridge) and each rail on the outside face
// of its web, so the jaws stand up and the holes print upright. PLA or PETG, 0.2 mm layers, three
// walls. Hardware: six 4 mm countersunk wood screws (No. 8) 16 mm long, two M3 x 5 screws.
design UnderDeskDrawer units mm

param drawer_w = 200 mm        // inside sizes
param drawer_d = 180 mm
param drawer_h = 50 mm
param wall = 2 mm
param floor_t = 2 mm
param front_t = 5 mm           // the front panel
param fit = 0.4 mm             // side play between runner and rail
param fit_v = 0.3 mm           // runner over the lip, as drawn
param lift = 1 mm              // runner top to the jaw
param rw = 12 mm               // runner width
param strip = 4 mm             // its flat bearing strip
param edge_h = 5 mm            // the runner's outer edge, which takes the stop rebate
param web_t = 3 mm
param lip_t = 3.5 mm
param jaw_t = 5 mm

let Wo = drawer_w + 2*wall
let Do = drawer_d + 2*wall
let Ho = drawer_h + floor_t
let zr_top = Ho - 1 mm                           // runner top, a mm under the rim
let zr = zr_top - edge_h                         // runner's underside (the strip)
let zl = zr - fit_v                              // lip top
let zf = zr_top + lift                           // jaw underside
// The lip's tip, about 1 mm clear of the runner's slope (normal to it), so the lip bears on the
// outer 2.8 mm of the strip. Closer than about 0.5 mm to the corner where strip and slope meet,
// the engine's clearance query reads 0 where the lip's edge crosses the runner's end faces.
let x_tip = rw - strip + 1.2 mm
let travel = 0.7*Do
let rail_l = Do + 1 mm
let z_pin = zr + 3 mm                            // the stop screw's axis
let stop_y = Do - 10 mm - travel - 1.5 mm        // and where it is along the rail

// ==== Drawer: an open box, its front panel wide enough to hide the rails, and a bar pull
box DrawerBox size Wo Do Ho corner (0, 0, 0) new -> body Drawer
box DrawerInside size drawer_w drawer_d (drawer_h + 1 mm) corner (wall, wall, floor_t) cut targets Drawer
box Front size (Wo + 2*(rw + fit + web_t) + 2 mm) (front_t + 0.5 mm) (zf + jaw_t - 1.5 mm) corner (-(rw + fit + web_t) - 1 mm, -front_t, 0) join targets Drawer
for s from 0 to 1 {
  // The runner, one outline along the side, starting just inside the rail's front end: the right
  // side drawn, the left mirrored about the drawer's middle by (1 - 2*s)
  sketch RunnerSk{s} on XZ offset 1.5 mm {
    polyline closed r = (Wo/2 + (1 - 2*s)*(Wo/2 - 0.5 mm), -zr_top) (Wo/2 + (1 - 2*s)*(Wo/2 + rw), -zr_top) (Wo/2 + (1 - 2*s)*(Wo/2 + rw), -zr) (Wo/2 + (1 - 2*s)*(Wo/2 + rw - strip), -zr) (Wo/2 + (1 - 2*s)*(Wo/2 - 0.5 mm), -(zr - (rw - strip) - 0.5 mm))
    fix r
  }
  extrude Runner{s} from RunnerSk{s} distance (Do - 2 mm) join targets Drawer
  // The stop rebate: the outer 2.5 mm of the runner's edge above its lowest mm, from its front to
  // 10 mm short of the back. It is open at the front, the top and the side, so it leaves no face
  // with a hole in it: the engine's clearance query misreads those.
  box Rebate{s} size 3.5 mm (Do - 11 mm) edge_h corner (Wo/2 + (1 - 2*s)*(Wo/2 + rw - 2.5 mm) - s*3.5 mm, 1 mm, zr + 1 mm) cut targets Drawer
}
// The pull: a bar on two wedge posts whose undersides slope 45 deg back to the front, so the
// drawer prints upright with only the bar's short bridge between the posts
for p from 0 to 1 {
  sketch PostSk{p} on YZ offset (Wo/2 - 43.5 mm + p*79 mm) {
    polyline closed w = (-front_t + 0.5 mm, Ho/2 - 25.5 mm) (-front_t - 20 mm, Ho/2 - 5.5 mm) (-front_t - 20 mm, Ho/2 + 3.5 mm) (-front_t + 0.5 mm, Ho/2 + 3.5 mm)
    fix w
  }
  extrude PullPost{p} from PostSk{p} distance 8 mm join targets Drawer
}
box PullBar size 88 mm 8 mm 10 mm corner (Wo/2 - 44 mm, -front_t - 22 mm, Ho/2 - 5 mm) join targets Drawer
fillet PullEdges edges (Wo/2, -front_t - 22 mm, Ho/2 + 5 mm) (Wo/2, -front_t - 22 mm, Ho/2 - 5 mm) radius 2 mm body Drawer

// ==== Rails: a C channel each side, a cap at the back, the stop screw's hole, screw holes
for s from 0 to 1 {
  sketch RailSk{s} on XZ offset 1 mm {
    polyline closed c = (Wo/2 + (1 - 2*s)*(Wo/2 + rw + fit + web_t), -(zl - lip_t)) (Wo/2 + (1 - 2*s)*(Wo/2 + x_tip), -(zl - lip_t)) (Wo/2 + (1 - 2*s)*(Wo/2 + x_tip), -zl) (Wo/2 + (1 - 2*s)*(Wo/2 + rw + fit), -zl) (Wo/2 + (1 - 2*s)*(Wo/2 + rw + fit), -zf) (Wo/2 + (1 - 2*s)*(Wo/2 + 0.5 mm), -zf) (Wo/2 + (1 - 2*s)*(Wo/2 + 0.5 mm), -(zf + jaw_t)) (Wo/2 + (1 - 2*s)*(Wo/2 + rw + fit + web_t), -(zf + jaw_t))
    fix c
  }
  extrude Rail{s} from RailSk{s} distance rail_l new -> body Rail{s}
  // The cap: inside the channel's outline by 0.3 mm on its outer faces, so it shares no face
  sketch CapSk{s} on XZ offset (rail_l + 0.5 mm) {
    polyline closed p = (Wo/2 + (1 - 2*s)*(Wo/2 + x_tip + 0.3 mm), -(zl - lip_t + 0.3 mm)) (Wo/2 + (1 - 2*s)*(Wo/2 + rw + fit + web_t - 0.3 mm), -(zl - lip_t + 0.3 mm)) (Wo/2 + (1 - 2*s)*(Wo/2 + rw + fit + web_t - 0.3 mm), -(zf + jaw_t - 0.3 mm)) (Wo/2 + (1 - 2*s)*(Wo/2 + x_tip + 0.3 mm), -(zf + jaw_t - 0.3 mm))
    fix p
  }
  extrude Cap{s} from CapSk{s} distance 3 mm join targets Rail{s}
  // The stop screw's pilot hole, across the web (an M3 cuts its own thread in it)
  cylinder StopHole{s} radius 1.25 mm height (web_t + 2 mm) base (Wo/2 + (1 - 2*s)*(Wo/2 + rw + fit - 1 mm), stop_y, z_pin) axis (1 - 2*s, 0, 0) cut targets Rail{s}
  // Three countersunk holes up through the jaw
  for h from 0 to 2 {
    cylinder ScrewHole{s}{h} radius 2.25 mm height (jaw_t + 2 mm) base (Wo/2 + (1 - 2*s)*(Wo/2 + 0.5 mm + (rw + fit + web_t - 0.5 mm)/2), 15 mm + h*(rail_l - 30 mm)/2, zf - 1 mm) axis Z cut targets Rail{s}
    // The countersink as a revolved ring round the hole (a cone primitive cut here fails)
    sketch CsSk{s}{h} on XZ offset (15 mm + h*(rail_l - 30 mm)/2) {
      construction line ax = ((Wo/2 + (1 - 2*s)*(Wo/2 + 0.5 mm + (rw + fit + web_t - 0.5 mm)/2)), -(zf - 1 mm)) ((Wo/2 + (1 - 2*s)*(Wo/2 + 0.5 mm + (rw + fit + web_t - 0.5 mm)/2)), -(zf + 3 mm))
      polyline closed k = ((Wo/2 + (1 - 2*s)*(Wo/2 + 0.5 mm + (rw + fit + web_t - 0.5 mm)/2)) + 2 mm, -(zf - 0.2 mm)) ((Wo/2 + (1 - 2*s)*(Wo/2 + 0.5 mm + (rw + fit + web_t - 0.5 mm)/2)) + 4.5 mm, -(zf - 0.2 mm)) ((Wo/2 + (1 - 2*s)*(Wo/2 + 0.5 mm + (rw + fit + web_t - 0.5 mm)/2)) + 2.15 mm, -(zf + 2.15 mm)) ((Wo/2 + (1 - 2*s)*(Wo/2 + 0.5 mm + (rw + fit + web_t - 0.5 mm)/2)) + 2 mm, -(zf + 2.15 mm))
      fix ax k
    }
    revolve Countersink{s}{h} from CsSk{s}{h} axis CsSk{s}{h}.ax cut targets Rail{s}
  }
}

// ==== Fits, drawer shut and resting fit_v over the lips
check Drawer clearance Rail0 >= (min(fit, fit_v) - 0.02 mm)
check Drawer clearance Rail1 >= (min(fit, fit_v) - 0.02 mm)
check Rail0 clearance Rail1 >= (Wo - 1 mm)
check Drawer size.x >= (Wo + 2*(rw + fit + web_t))

color Drawer "#e8e2d4"
color Rail0, Rail1 "#3b3f44"
material Drawer, Rail0, Rail1 PLA
