// A curve of 107_train_track: theta of a circle of centreline radius R, turning left (+Y).
// It starts at the origin heading +X; its centre is (0, R). Socket at the start, peg at the end.
design TrackCurve units mm

// Where the piece goes: it places itself (a used part can't be moved from outside once its
// own peg and socket helpers are used up), turning a0 about Z, then moving to (x0, y0)
param x0 = 0 mm
param y0 = 0 mm
param a0 = 0 deg

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

let ri = R - w/2
let ro = R + w/2
let ga = (gap/2)/R*1 rad          // the end gap, as an angle on the centreline
let ra = R + gc/2 - gw/2
let rb = R + gc/2 + gw/2
let rc = R - gc/2 - gw/2
let rd = R - gc/2 + gw/2

sketch BodySk on XY {
  line s = (ri*sin(ga), R - ri*cos(ga)) (ro*sin(ga), R - ro*cos(ga))
  arc o = start s.end through (ro*sin(theta/2), R - ro*cos(theta/2)) end (ro*sin(theta - ga), R - ro*cos(theta - ga))
  line e = o.end (ri*sin(theta - ga), R - ri*cos(theta - ga))
  arc i = start e.end through (ri*sin(theta/2), R - ri*cos(theta/2)) end s.start
  fix s o e i
}
extrude Body from BodySk distance t new -> body Curve

// Grooves: two arc bands, run 2 deg past each end so no cutter face meets an end face
sketch GrooveSk on XY offset (t - gd) {
  line s1 = (ra*sin(-2 deg), R - ra*cos(-2 deg)) (rb*sin(-2 deg), R - rb*cos(-2 deg))
  arc o1 = start s1.end through (rb*sin(theta/2), R - rb*cos(theta/2)) end (rb*sin(theta + 2 deg), R - rb*cos(theta + 2 deg))
  line e1 = o1.end (ra*sin(theta + 2 deg), R - ra*cos(theta + 2 deg))
  arc i1 = start e1.end through (ra*sin(theta/2), R - ra*cos(theta/2)) end s1.start
  line s2 = (rc*sin(-2 deg), R - rc*cos(-2 deg)) (rd*sin(-2 deg), R - rd*cos(-2 deg))
  arc o2 = start s2.end through (rd*sin(theta/2), R - rd*cos(theta/2)) end (rd*sin(theta + 2 deg), R - rd*cos(theta + 2 deg))
  line e2 = o2.end (rc*sin(theta + 2 deg), R - rc*cos(theta + 2 deg))
  arc i2 = start e2.end through (rc*sin(theta/2), R - rc*cos(theta/2)) end s2.start
  fix s1 o1 e1 i1 s2 o2 e2 i2
}
extrude Grooves from GrooveSk distance (gd + 1 mm) cut targets Curve

use Sock from "dogbone.sc" with (grow = clearance, z0 = -1 mm, h = t + 2 mm) at (-gap/2, 0, 0) -> body SockTool
combine SockCut target Curve tools SockTool cut
use Peg from "dogbone.sc" with (h = ph) rotate Z theta at (R*sin(theta) - gap/2*cos(theta), R - R*cos(theta) - gap/2*sin(theta), 0) join targets Curve
color Curve "#d9b382"
move Turn bodies Curve axis (0,0,1) angle a0
move Place bodies Curve to (x0, y0, 0)
