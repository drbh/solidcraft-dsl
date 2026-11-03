// A straight of 108_car_track: a U channel (floor floor_t, walls wt by hw), socket at the start, tab
// at the end. It places itself: turns a0 about Z, then moves to (x0, y0, z0).
design CarStraight units mm

param x0 = 0 mm
param y0 = 0 mm
param z0 = 0 mm
param a0 = 0 deg
param L = 200 mm
param W = 44 mm              // outside width; cars run in W - 2*wt
param wt = 2 mm
param floor_t = 2 mm
param hw = 10 mm
param gap = 0.5 mm
param clearance = 0.3 mm

box Body size (L - gap) W hw corner (gap/2, -W/2, 0) new -> body Straight
box Channel size (L + 2 mm) (W - 2*wt) hw corner (-1 mm, -W/2 + wt, floor_t) cut targets Straight
use Sock from "tab.sc" with (grow = clearance, z0 = -1 mm, h = floor_t + 2 mm) at (-gap/2, 0, 0) -> body SockTool
combine SockCut target Straight tools SockTool cut
use Tab from "tab.sc" at (L - gap/2, 0, 0) join targets Straight
color Straight "#f26b1d"
move Turn bodies Straight axis (0,0,1) angle a0
move Place bodies Straight to (x0, y0, z0)
