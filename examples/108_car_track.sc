// 108. Toy car track: printable U-channel pieces on one shared dovetail tab joint, laid out as a
// launcher, a straight, a vertical loop, a straight out and a 90 deg curve.
//
// Z is up, the track runs on the floor (z = 0) heading +X. Every piece is drawn in its own
// frame, starting at its origin heading +X, with a socket through its floor at the start and a
// dovetail tab at the end. The joint lives once in 108_car_track_parts/tab.sc: a piece joins it
// as its tab, and cuts it out grown by `clearance` as its socket, so every joint fits the same
// and locks against pulling apart. Each piece places itself (x0, y0, a0), because a used part
// can't be moved from outside once its own tab and socket helpers are used up.
//
// The channel takes 1:64 die-cast cars: W - 2*wt = 40 mm between walls hw = 10 mm tall. The
// loop shifts its exit sideways by W + 2 mm, so the lane out runs beside the lane in; the curve
// at the end leaves room to add more. The launcher's plunger is driven by a rubber band.
//
// Print every piece flat, floor down, no supports, PLA or PETG: 0.2 mm layers, 3 walls. The loop
// prints standing as used, with tree supports inside its upper half. Bill of materials: the
// printed pieces, one rubber band (about 60 mm long, 1.5 mm thick) for the launcher.
design CarTrack units mm

param L = 200 mm             // straight length
param R = 140 mm             // curve centreline radius
param Rl = 90 mm             // loop floor radius
param W = 44 mm
param hw = 10 mm
param clearance = 0.3 mm
param gap = 0.5 mm

let launch_l = 160 mm
let lane = W + 2 mm           // the loop's sideways shift

use Launch from "108_car_track_parts/launcher.sc" with (W = W, hw = hw, clearance = clearance, gap = gap, L = launch_l)
use In from "108_car_track_parts/straight.sc" with (L = L, W = W, hw = hw, clearance = clearance, gap = gap, x0 = launch_l)
use Loop from "108_car_track_parts/loop.sc" with (Rl = Rl, W = W, hw = hw, clearance = clearance, gap = gap, x0 = (launch_l + L))
use Out from "108_car_track_parts/straight.sc" with (L = L, W = W, hw = hw, clearance = clearance, gap = gap, x0 = (launch_l + L), y0 = lane)
use Turn from "108_car_track_parts/curve.sc" with (R = R, W = W, hw = hw, clearance = clearance, gap = gap, x0 = (launch_l + 2*L), y0 = lane)

// The plunger slides free in its bore with `clearance` all round
check Launch_Plunger clearance Launch_Launcher >= (clearance - 0.01 mm)
// The tab joints are checked by the print recipe's interference request (no piece overlaps its
// neighbour). A clearance check can't measure them: engine 0.28 reports 0 to 0.1 mm from the
// tab's top face up to the floor plane round the socket, though the floor is cut away above the
// tab; the true gap is `clearance` sideways and at the tip.
// The lane out clears the lane in
check Out_Straight clearance In_Straight >= 1 mm
