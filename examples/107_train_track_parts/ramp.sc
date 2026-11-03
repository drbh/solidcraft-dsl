// The ramp of 107_train_track: a straight whose top rises by `rise` over its length, solid
// below, so it stands on the floor with no pier. Socket at the low start, peg at the high end.
design TrackRamp units mm

// Where the piece goes: it places itself (a used part can't be moved from outside once its
// own peg and socket helpers are used up), turning a0 about Z, then moving to (x0, y0)
param x0 = 0 mm
param y0 = 0 mm
param a0 = 0 deg

param L = 144 mm
param rise = 36 mm
param w = 40 mm
param t = 12 mm
param gw = 6 mm
param gd = 3 mm
param gc = 26 mm
param gap = 0.5 mm
param clearance = 0.3 mm
param ph = 8.5 mm

let slope = rise/(L - gap)
let z_lo = t - slope*(gap/2 + 1 mm)        // the top line, carried 1 mm past each end
let z_hi = t + slope*(L - gap/2 + 1 mm)

// Side profile on XZ (sketch v is -z), extruded across the width
sketch ProfileSk on XZ offset (-w/2) {
  polyline closed p = (gap/2, 0) (L - gap/2, 0) (L - gap/2, -(t + rise)) (gap/2, -t)
  fix p
}
extrude Body from ProfileSk distance w new -> body Ramp

// Each groove: a band under the sloping top, cut across its width
sketch GrooveASk on XZ offset (gc/2 - gw/2) {
  polyline closed p = (-1 mm, -(z_lo + 1 mm)) (L + 1 mm, -(z_hi + 1 mm)) (L + 1 mm, -(z_hi - gd)) (-1 mm, -(z_lo - gd))
  fix p
}
extrude GrooveA from GrooveASk distance gw cut targets Ramp
sketch GrooveBSk on XZ offset (-gc/2 - gw/2) {
  polyline closed p = (-1 mm, -(z_lo + 1 mm)) (L + 1 mm, -(z_hi + 1 mm)) (L + 1 mm, -(z_hi - gd)) (-1 mm, -(z_lo - gd))
  fix p
}
extrude GrooveB from GrooveBSk distance gw cut targets Ramp

use Sock from "dogbone.sc" with (grow = clearance, z0 = -1 mm, h = t + rise + 2 mm) at (-gap/2, 0, 0) -> body SockTool
combine SockCut target Ramp tools SockTool cut
use Peg from "dogbone.sc" with (h = ph, z0 = rise) at (L - gap/2, 0, 0) join targets Ramp
color Ramp "#d9b382"
move Turn bodies Ramp axis (0,0,1) angle a0
move Place bodies Ramp to (x0, y0, 0)
