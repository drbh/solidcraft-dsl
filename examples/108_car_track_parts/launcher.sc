// The launcher of 108_car_track: the first piece, a straight with a housing at its back end and
// a rubber-band plunger. Pull the knob back, let go, and the pusher shoves the car down the lane.
//
// The square plunger rod slides in a square bore through the housing, above the floor at bumper
// height. A rubber band (about 60 mm, 1.5 mm) runs from the two posts on the housing's front
// corners round the back of the knob's groove. Only an end tab: nothing comes before it.
// The plunger prints flat on its underside (rod, pusher and knob share one bottom face).
design CarLauncher units mm

param x0 = 0 mm
param y0 = 0 mm
param a0 = 0 deg
param L = 160 mm
param W = 44 mm
param wt = 2 mm
param floor_t = 2 mm
param hw = 10 mm
param gap = 0.5 mm
param clearance = 0.3 mm
param housing_l = 36 mm
param housing_h = 22 mm
param rod = 6 mm             // square plunger rod
param rod_z = 5 mm           // rod underside above the floor plane
param travel = 30 mm         // how far the knob pulls back

box Body size (L - gap/2) W hw corner (0, -W/2, 0) new -> body Launcher
box Channel size (L + 2 mm) (W - 2*wt) hw corner (housing_l - 1 mm, -W/2 + wt, floor_t) cut targets Launcher
box Housing size housing_l W (housing_h - hw + 0.5 mm) corner (0, -W/2, hw - 0.5 mm) join targets Launcher
box Bore size (housing_l + 2 mm) (rod + 2*clearance) (rod + 2*clearance) corner (-1 mm, -rod/2 - clearance, rod_z - clearance) cut targets Launcher
cylinder PostA radius 3 mm height 8 mm base (housing_l - 5 mm, W/2 - 5 mm, housing_h - 0.5 mm) axis (0,0,1) join targets Launcher
cylinder PostB radius 3 mm height 8 mm base (housing_l - 5 mm, -W/2 + 5 mm, housing_h - 0.5 mm) axis (0,0,1) join targets Launcher
use Tab from "tab.sc" at (L - gap/2, 0, 0) join targets Launcher

// The plunger, at rest: pusher just in front of the housing, knob behind it
box Rod size (housing_l + travel + 12 mm) rod rod corner (-travel - 4 mm, -rod/2, rod_z) new -> body Plunger
box Pusher size 6 mm 30 mm 12 mm corner (housing_l + 2 mm, -15 mm, rod_z) join targets Plunger
box Knob size 12 mm 16 mm 12 mm corner (-travel - 14 mm, -8 mm, rod_z) join targets Plunger
box KnobGroove size 3 mm 18 mm 2 mm corner (-travel - 11 mm, -9 mm, rod_z + 10.5 mm) cut targets Plunger

color Launcher "#f26b1d"
color Plunger "#2b7bd6"
move Turn bodies Launcher, Plunger axis (0,0,1) angle a0
move Place bodies Launcher, Plunger to (x0, y0, 0)
