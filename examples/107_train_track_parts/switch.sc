// The Y-switch of 107_train_track: a straight and a left curve from one shared start socket.
// Both ends carry pegs; the curve's grooves cross the straight's near the start, as on a
// wooden switch, so a wheel picks either road.
design TrackSwitch units mm

// Where the piece goes: it places itself (a used part can't be moved from outside once its
// own peg and socket helpers are used up), turning a0 about Z, then moving to (x0, y0)
param x0 = 0 mm
param y0 = 0 mm
param a0 = 0 deg

param L = 144 mm
param R = 182 mm
param theta = 45 deg
param w = 40 mm
param t = 12 mm
param gw = 6 mm
param gd = 3 mm
param gc = 26 mm
param gap = 0.5 mm
param clearance = 0.3 mm
param ph = 8.5 mm

let ri = R - w/2 + 0.5 mm        // 0.5 mm in, so the branch's inner edge crosses the straight's side, not tangent to it
let ro = R + w/2
let ga = (gap/2)/R*1 rad
let ra = R + gc/2 - gw/2
let rb = R + gc/2 + gw/2
let rc = R - gc/2 - gw/2
let rd = R - gc/2 + gw/2

box Body size (L - gap) w t corner (gap/2, -w/2, 0) new -> body Switch
// The branch: an arc band starting 2 deg in, so its start face is buried in the straight
sketch BranchSk on XY {
  line s = (ri*sin(2 deg), R - ri*cos(2 deg)) (ro*sin(2 deg), R - ro*cos(2 deg))
  arc o = start s.end through (ro*sin(theta/2), R - ro*cos(theta/2)) end (ro*sin(theta - ga), R - ro*cos(theta - ga))
  line e = o.end (ri*sin(theta - ga), R - ri*cos(theta - ga))
  arc i = start e.end through (ri*sin(theta/2), R - ri*cos(theta/2)) end s.start
  fix s o e i
}
extrude Branch from BranchSk distance t join targets Switch

box GrooveA size (L + 2 mm) gw (gd + 1 mm) corner (-1 mm, gc/2 - gw/2, t - gd) cut targets Switch
box GrooveB size (L + 2 mm) gw (gd + 1 mm) corner (-1 mm, -gc/2 - gw/2, t - gd) cut targets Switch
// The branch grooves start 6 deg in, where they have left the straight's grooves (sharing
// their edges at the start would make the cut nearly tangent)
sketch BranchGrooveSk on XY offset (t - gd) {
  line s1 = (ra*sin(6 deg), R - ra*cos(6 deg)) (rb*sin(6 deg), R - rb*cos(6 deg))
  arc o1 = start s1.end through (rb*sin(theta/2), R - rb*cos(theta/2)) end (rb*sin(theta + 2 deg), R - rb*cos(theta + 2 deg))
  line e1 = o1.end (ra*sin(theta + 2 deg), R - ra*cos(theta + 2 deg))
  arc i1 = start e1.end through (ra*sin(theta/2), R - ra*cos(theta/2)) end s1.start
  line s2 = (rc*sin(6 deg), R - rc*cos(6 deg)) (rd*sin(6 deg), R - rd*cos(6 deg))
  arc o2 = start s2.end through (rd*sin(theta/2), R - rd*cos(theta/2)) end (rd*sin(theta + 2 deg), R - rd*cos(theta + 2 deg))
  line e2 = o2.end (rc*sin(theta + 2 deg), R - rc*cos(theta + 2 deg))
  arc i2 = start e2.end through (rc*sin(theta/2), R - rc*cos(theta/2)) end s2.start
  fix s1 o1 e1 i1 s2 o2 e2 i2
}
extrude BranchGrooves from BranchGrooveSk distance (gd + 1 mm) cut targets Switch

use Sock from "dogbone.sc" with (grow = clearance, z0 = -1 mm, h = t + 2 mm) at (-gap/2, 0, 0) -> body SockTool
combine SockCut target Switch tools SockTool cut
use PegMain from "dogbone.sc" with (h = ph) at (L - gap/2, 0, 0) join targets Switch
use PegBranch from "dogbone.sc" with (h = ph) rotate Z theta at (R*sin(theta) - gap/2*cos(theta), R - R*cos(theta) - gap/2*sin(theta), 0) join targets Switch
color Switch "#c9a06a"
move Turn bodies Switch axis (0,0,1) angle a0
move Place bodies Switch to (x0, y0, 0)
