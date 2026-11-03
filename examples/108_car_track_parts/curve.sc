// A curve of 108_car_track: theta of a U channel of centreline radius R, turning left about
// (0, R). Socket at the start, tab at the end; it places itself like the straight.
design CarCurve units mm

param x0 = 0 mm
param y0 = 0 mm
param z0 = 0 mm
param a0 = 0 deg
param R = 140 mm
param theta = 90 deg
param W = 44 mm
param wt = 2 mm
param floor_t = 2 mm
param hw = 10 mm
param gap = 0.5 mm
param clearance = 0.3 mm

let ri = R - W/2
let ro = R + W/2
let ci = ri + wt
let co = ro - wt
let ga = (gap/2)/R*1 rad

sketch BodySk on XY {
  line s = (ri*sin(ga), R - ri*cos(ga)) (ro*sin(ga), R - ro*cos(ga))
  arc o = start s.end through (ro*sin(theta/2), R - ro*cos(theta/2)) end (ro*sin(theta - ga), R - ro*cos(theta - ga))
  line e = o.end (ri*sin(theta - ga), R - ri*cos(theta - ga))
  arc i = start e.end through (ri*sin(theta/2), R - ri*cos(theta/2)) end s.start
  fix s o e i
}
extrude Body from BodySk distance hw new -> body Curve
// The channel, run 2 deg past each end so its faces miss the end faces
sketch ChannelSk on XY offset floor_t {
  line s = (ci*sin(-2 deg), R - ci*cos(-2 deg)) (co*sin(-2 deg), R - co*cos(-2 deg))
  arc o = start s.end through (co*sin(theta/2), R - co*cos(theta/2)) end (co*sin(theta + 2 deg), R - co*cos(theta + 2 deg))
  line e = o.end (ci*sin(theta + 2 deg), R - ci*cos(theta + 2 deg))
  arc i = start e.end through (ci*sin(theta/2), R - ci*cos(theta/2)) end s.start
  fix s o e i
}
extrude Channel from ChannelSk distance hw cut targets Curve
use Sock from "tab.sc" with (grow = clearance, z0 = -1 mm, h = floor_t + 2 mm) at (-gap/2, 0, 0) -> body SockTool
combine SockCut target Curve tools SockTool cut
use Tab from "tab.sc" rotate Z theta at (R*sin(theta) - gap/2*cos(theta), R - R*cos(theta) - gap/2*sin(theta), 0) join targets Curve
color Curve "#f26b1d"
move Turn bodies Curve axis (0,0,1) angle a0
move Place bodies Curve to (x0, y0, z0)
