// 111. Dowel hub connectors: printed corner, tee and cross hubs that turn plain dowels (or pipe)
// into frames, forts and shelving, built here into a cube frame.
//
// Z is up. Every hub is 111_dowel_hubs_parts/hub.sc with its own sockets switched on (px nx py
// ny pz nz), so one part file makes a corner (3 sockets), a tee (3 in a line and one off it), a
// cross (4 in a plane) or anything else. The hub is sized from `dowel_d`: 12.7 mm (1/2 in
// dowel) here; 21.3 mm fits 1/2 in PVC pipe, 15.9 mm a 5/8 in dowel. Each socket has `fit`
// radial play and an M3 set screw, so the frame comes apart again.
//
// The build: a cube frame `frame_l` by `frame_w` by `frame_h` between hub centres, from 8 corner
// hubs and 12 dowels (cut to length; the lengths are printed by evaluate as the dowel bodies'
// sizes). A tee and a cross stand beside it.
//
// Print: PLA or PETG, 0.2 mm layers, 4 walls, no supports; hubs upright, the upper corners
// (sockets pointing down) turned over (the print recipe does this). Ream a tight socket with a
// drill rather than forcing the dowel.
// BOM: 3.4 m of 12.7 mm dowel (12 pieces of 280 mm for the 300 mm frame: frame size minus
// s - 1.5 mm), 24 M3 x 10 self-tapping screws for the frame, 7 more for the tee and cross.
design DowelHubs units mm

param dowel_d = 12.7 mm
param fit = 0.15 mm
param wall = 3 mm
param depth = 19 mm
param frame_l = 300 mm
param frame_w = 300 mm
param frame_h = 300 mm

let ri = dowel_d/2 + fit
let s = 2*ri/cos(22.5 deg) + 2*wall + 1.2 mm
let z0 = s/2                      // the lower hubs' centres, so they stand on the floor
let z1 = z0 + frame_h
let stop = s/2 - 1 mm + 0.25 mm   // a dowel end's distance from its hub centre

// ==== Hubs: lower corners point up, upper corners point down
use LowFL from "111_dowel_hubs_parts/hub.sc" with (dowel_d = dowel_d, fit = fit, wall = wall, depth = depth, px = 1, nx = 0, py = 1, ny = 0, pz = 1, nz = 0) at (0, 0, z0) -> body LowFL
use LowFR from "111_dowel_hubs_parts/hub.sc" with (dowel_d = dowel_d, fit = fit, wall = wall, depth = depth, px = 0, nx = 1, py = 1, ny = 0, pz = 1, nz = 0) at (frame_l, 0, z0) -> body LowFR
use LowBR from "111_dowel_hubs_parts/hub.sc" with (dowel_d = dowel_d, fit = fit, wall = wall, depth = depth, px = 0, nx = 1, py = 0, ny = 1, pz = 1, nz = 0) at (frame_l, frame_w, z0) -> body LowBR
use LowBL from "111_dowel_hubs_parts/hub.sc" with (dowel_d = dowel_d, fit = fit, wall = wall, depth = depth, px = 1, nx = 0, py = 0, ny = 1, pz = 1, nz = 0) at (0, frame_w, z0) -> body LowBL
use TopFL from "111_dowel_hubs_parts/hub.sc" with (dowel_d = dowel_d, fit = fit, wall = wall, depth = depth, px = 1, nx = 0, py = 1, ny = 0, pz = 0, nz = 1) at (0, 0, z1) -> body TopFL
use TopFR from "111_dowel_hubs_parts/hub.sc" with (dowel_d = dowel_d, fit = fit, wall = wall, depth = depth, px = 0, nx = 1, py = 1, ny = 0, pz = 0, nz = 1) at (frame_l, 0, z1) -> body TopFR
use TopBR from "111_dowel_hubs_parts/hub.sc" with (dowel_d = dowel_d, fit = fit, wall = wall, depth = depth, px = 0, nx = 1, py = 0, ny = 1, pz = 0, nz = 1) at (frame_l, frame_w, z1) -> body TopBR
use TopBL from "111_dowel_hubs_parts/hub.sc" with (dowel_d = dowel_d, fit = fit, wall = wall, depth = depth, px = 1, nx = 0, py = 0, ny = 1, pz = 0, nz = 1) at (0, frame_w, z1) -> body TopBL

// ==== Dowels: 4 along X, 4 along Y, 4 upright
for k from 0 to 1 {
  cylinder DowelXLow{k} diameter dowel_d height (frame_l - 2*stop) base (stop, k*frame_w, z0) axis (1,0,0) new -> body DowelXLow{k}
  cylinder DowelXTop{k} diameter dowel_d height (frame_l - 2*stop) base (stop, k*frame_w, z1) axis (1,0,0) new -> body DowelXTop{k}
  cylinder DowelYLow{k} diameter dowel_d height (frame_w - 2*stop) base (k*frame_l, stop, z0) axis (0,1,0) new -> body DowelYLow{k}
  cylinder DowelYTop{k} diameter dowel_d height (frame_w - 2*stop) base (k*frame_l, stop, z1) axis (0,1,0) new -> body DowelYTop{k}
  cylinder PostL{k} diameter dowel_d height (frame_h - 2*stop) base (0, k*frame_w, z0 + stop) axis (0,0,1) new -> body PostL{k}
  cylinder PostR{k} diameter dowel_d height (frame_h - 2*stop) base (frame_l, k*frame_w, z0 + stop) axis (0,0,1) new -> body PostR{k}
}

// ==== A tee and a cross, beside the frame
use Tee from "111_dowel_hubs_parts/hub.sc" with (dowel_d = dowel_d, fit = fit, wall = wall, depth = depth, px = 1, nx = 1, py = 1, ny = 0, pz = 0, nz = 0) at (frame_l + 120 mm, 0, z0) -> body Tee
use Cross from "111_dowel_hubs_parts/hub.sc" with (dowel_d = dowel_d, fit = fit, wall = wall, depth = depth, px = 1, nx = 1, py = 1, ny = 1, pz = 0, nz = 0) at (frame_l + 120 mm, 120 mm, z0) -> body Cross

color LowFL, LowFR, LowBR, LowBL, TopFL, TopFR, TopBR, TopBL, Tee, Cross "#e8762c"
color DowelXLow0, DowelXLow1, DowelXTop0, DowelXTop1, DowelYLow0, DowelYLow1, DowelYTop0, DowelYTop1, PostL0, PostL1, PostR0, PostR1 "#c9a46a"
