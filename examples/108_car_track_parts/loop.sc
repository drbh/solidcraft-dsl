// The vertical loop of 108_car_track: a U channel once round a circle of floor radius Rl, its
// exit shifted sideways by W + 2 mm so the lanes in and out run side by side.
//
// Built as twelve 30 deg revolves of the channel profile about the loop's axis (a Y line at
// z = Rl), each step mm further along +Y and each running 1 deg into the next. Odd segments sit
// 0.15 mm further in, so neighbours never share a surface where they overlap. The car enters at
// the origin heading +X, climbs, runs upside down over the top and comes down to the exit at
// y = W + 2 mm, still heading +X.
//
// It prints in one piece standing as used (2*Rl tall, 180 mm by default, so it fits a 256 mm bed), with tree
// supports inside the upper half; the lower half and the lanes in and out need none.
design CarLoop units mm

param x0 = 0 mm
param y0 = 0 mm
param a0 = 0 deg
param Rl = 90 mm             // floor radius (the car runs on the inside)
param W = 44 mm
param wt = 2 mm
param floor_t = 2 mm
param hw = 10 mm
param gap = 0.5 mm
param clearance = 0.3 mm

let shift = W + 2 mm
let step = shift/11
let ga = (gap/2)/Rl*1 rad

for k from 0 to 11 {
  sketch Seg{k}Sk on YZ {
    construction line ax = (-W, Rl) (shift + W, Rl)
    polyline closed u = (k*step - W/2, 0.15 mm*mod(k, 2)) (k*step + W/2, 0.15 mm*mod(k, 2)) (k*step + W/2, hw + 0.15 mm*mod(k, 2)) (k*step + W/2 - wt, hw + 0.15 mm*mod(k, 2)) (k*step + W/2 - wt, floor_t + 0.15 mm*mod(k, 2)) (k*step - W/2 + wt, floor_t + 0.15 mm*mod(k, 2)) (k*step - W/2 + wt, hw + 0.15 mm*mod(k, 2)) (k*step - W/2, hw + 0.15 mm*mod(k, 2))
    fix ax u
  }
}
// Each segment from its start angle: 31 deg (1 deg into the next); the last stops short by the gap
revolve Seg0 from Seg0Sk axis ax angle 31 deg new -> body Loop
move Seg0Down bodies Loop to (0, 0, -Rl)
move Seg0Turn bodies Loop axis (0,1,0) angle (-ga)
move Seg0Up bodies Loop to (0, 0, Rl)
for k from 1 to 10 {
  revolve Seg{k} from Seg{k}Sk axis ax angle 31 deg new -> body Seg{k}
  move Seg{k}Down bodies Seg{k} to (0, 0, -Rl)
  move Seg{k}Turn bodies Seg{k} axis (0,1,0) angle (-(k*30 deg + ga))
  move Seg{k}Up bodies Seg{k} to (0, 0, Rl)
}
revolve Seg11 from Seg11Sk axis ax angle (30 deg - 2*ga) new -> body Seg11
move Seg11Down bodies Seg11 to (0, 0, -Rl)
move Seg11Turn bodies Seg11 axis (0,1,0) angle (-(330 deg + ga))
move Seg11Up bodies Seg11 to (0, 0, Rl)
combine Ring target Loop tools Seg1, Seg2, Seg3, Seg4, Seg5, Seg6, Seg7, Seg8, Seg9, Seg10, Seg11 join

// Joints: socket in the entry floor, tab on the exit floor
use Sock from "tab.sc" with (grow = clearance, z0 = -1 mm, h = floor_t + 2 mm) at (-gap/2, 0, 0) -> body SockTool
combine SockCut target Loop tools SockTool cut
use Tab from "tab.sc" with (z0 = 0.25 mm, h = 1.8 mm) at (-gap/2, 11*step, 0) join targets Loop

color Loop "#f26b1d"
move Turn bodies Loop axis (0,0,1) angle a0
move Place bodies Loop to (x0, y0, 0)
