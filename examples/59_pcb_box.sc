// 59. Box for a small PCB, with four standoffs from a reusable part file.
//
// The standoff is designed once, in 59_pcb_box_parts/standoff.sc, and placed four times with
// `use`: each copy gets its own names (S1_Standoff, …), stands on the floor at its hole's
// position, and is joined to the box. The two at the connector end are taller, through `with`,
// to lift that edge of the board over a cable.
//
// Print open side up, no supports: the walls and posts are vertical and the screw holes open up.
design PcbBox units mm
param board_w = 70 mm
param board_d = 50 mm
// Hole centres, in from the board's corners
param hole_in = 3.5 mm
param clear = 4 mm
param wall = 2 mm
param floor_t = 2 mm
param H = 25 mm
let W = board_w + 2*(clear + wall)
let D = board_d + 2*(clear + wall)
let x0 = wall + clear + hole_in
let y0 = wall + clear + hole_in
let x1 = W - x0
let y1 = D - y0

box Block size W D H corner (0,0,0) -> body Box
shell Open faces (W/2,D/2,H) thickness wall inside body Box

use S1 from "59_pcb_box_parts/standoff.sc" at (x0, y0, floor_t) join targets Box
use S2 from "59_pcb_box_parts/standoff.sc" at (x0, y1, floor_t) join targets Box
use S3 from "59_pcb_box_parts/standoff.sc" with (h = 9 mm) at (x1, y0, floor_t) join targets Box
use S4 from "59_pcb_box_parts/standoff.sc" with (h = 9 mm) at (x1, y1, floor_t) join targets Box

check Box overhang <= 45 deg
check Box size.z == H
