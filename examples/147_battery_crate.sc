// 147. Stackable battery crate: pockets sized from a cell table (AA, AAA, 18650), stacking with the cells inside.
//
// Z is up, the crates sit on the bed with their corner at the origin. Each cell stands in a top
// pocket that holds `seat` (60%) of its length, so the rest stands proud to grip. The underside
// has the same grid of pockets, deep enough for those proud ends, so the next crate drops over
// the cells below and the stack shuts them in. A lip round the top and a matching step round the
// bottom locate one crate on the next, `stack_fit` apart; notches in the lip at both ends give a
// fingertip under the crate above. The front face has a 0.6 mm recess for a label.
//
// `cell` picks the cell: 0 AA (14.5 x 50.5 mm), 1 AAA (10.5 x 44.5), 2 18650 (18.6 x 65). Pocket
// size, depths and the crate's height all follow, and `cols` x `rows` sets the count (5 x 4 = 20 AA,
// 136 x 107 x 56 mm). The crate is drawn once, in 147_battery_crate_parts/crate.sc; here it is used
// twice, stacked, with a row of cells in the lower one so the checks measure the real fits. The
// upper crate is shown lifted 0.5 mm so the check sees the lip's side play; in use it rests on
// the top face.
//
// Printing: upright as modelled, PLA, 0.2 mm layers, no supports. The underside pockets' roofs are
// flat circles bridged over (14.5 mm for AA, 19 mm for 18650), which bridge cleanly with
// cooling on; if yours sag, print the crate upside down and the top pockets' floors bridge
// instead. Pockets have 0.5 mm play on the diameter (`fit`); cells vary, measure a few.
design BatteryCrateStack units mm

param cell = 0                 // 0 AA, 1 AAA, 2 18650
param cols = 5
param rows = 4
param fit = 0.5 mm
param stack_fit = 0.3 mm
param seat = 0.6
param floor_t = 1.2 mm
param lip_h = 2 mm

// The same table as the part, for placing the upper crate and the cells
let w0 = (1 - cell)*(2 - cell)/2
let w1 = cell*(2 - cell)
let w2 = cell*(cell - 1)/2
let cell_d = 14.5 mm*w0 + 10.5 mm*w1 + 18.6 mm*w2
let cell_l = 50.5 mm*w0 + 44.5 mm*w1 + 65 mm*w2
let pd = cell_d + fit
let pitch = pd + 1.6 mm
let depth = seat*cell_l
let under = cell_l - depth + 1 mm
let H = under + floor_t + depth

use Lower from "147_battery_crate_parts/crate.sc" with (cell = cell, cols = cols, rows = rows, fit = fit, stack_fit = stack_fit, seat = seat, floor_t = floor_t, lip_h = lip_h, z0 = 0 mm)
use Upper from "147_battery_crate_parts/crate.sc" with (cell = cell, cols = cols, rows = rows, fit = fit, stack_fit = stack_fit, seat = seat, floor_t = floor_t, lip_h = lip_h, z0 = (H + 0.5 mm))

// A row of cells in the lower crate (not printed): one per column, shown 0.3 mm above the pocket
// floor they rest on, so the check measures their side play
for k from 0 to 9 {
  if k < cols {
    cylinder Cell{k} diameter cell_d height cell_l base (3.5 mm + pd/2 + k*pitch, 3.5 mm + pd/2, under + floor_t + 0.3 mm) axis Z new -> body Cell{k}
    color Cell{k} "#d9a400"
  }
}

// Fits: a cell has fit/2 all round in its pocket and 1 mm under the next crate's pocket roof
// (0.7 mm as shown, lifted);
// the upper crate's step has stack_fit inside the lower crate's lip
check Cell0 clearance Lower_Crate >= (fit/2 - 0.05 mm)
check Cell0 clearance Upper_Crate >= (fit/2 - 0.05 mm)
check Upper_Crate clearance Lower_Crate >= (stack_fit - 0.05 mm)
check Lower_Crate shells == 1
check Lower_Crate size.z >= (H + lip_h - 0.01 mm)
check Lower_Crate size.z <= (H + lip_h + 0.01 mm)
