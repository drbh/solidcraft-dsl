// 106. Stud-and-tube building bricks: one grid and fit, a brick and plate generator, a slope,
// and a fit-test coupon, stacked into a small build.
//
// Z is up and every piece stands on its cell corner, so a piece at grid cell (i, j), level k sits
// at (i*pitch, j*pitch, k*plate_h). The grid is this design's own: a 10 mm cell, 4 mm plates (a
// brick is 3 plates), 6 mm studs. It is not meant to mate with any commercial system.
//
// How the fit works: a stud is held by the walls inside the brick above and by the tubes between
// every four studs, each touching it `fit` off; neighbouring pieces leave `gap` a side. Every
// piece is one parts file used with its own sizes:
//   106_building_bricks_parts/brick.sc   nx by ny studs, `units` plates tall (1 plate, 3 brick)
//   106_building_bricks_parts/slope.sc   nx wide, two rows deep, sloped to a one-plate lip
// The loops in the parts run up to 8 x 8.
//
// Fit test first: print the coupon (three 2 x 2 plates at fit 0.05, 0.10 and 0.15 mm, in
// white, yellow and green) and press each onto the base plate. Pick the one that holds without
// forcing, and set `fit` to it for everything else. Changing `fit` or `pitch` changes every piece.
//
// Print: PLA, 0.2 mm layers, every piece as it stands (studs up), no supports; the cavities and
// tube bores bridge across the top. Slopes print on their back face (see the print recipe).
// BOM: nothing but filament.
design BuildingBricks units mm

param pitch = 10 mm
param plate_h = 4 mm
param stud_d = 6 mm
param stud_h = 2.2 mm
param fit = 0.1 mm
param gap = 0.1 mm

// ==== The build: a 6 x 4 base plate, two bricks stacked across it, a plate and a slope on top
use Base from "106_building_bricks_parts/brick.sc" with (nx = 6, ny = 4, units = 1, pitch = pitch, plate_h = plate_h, stud_d = stud_d, stud_h = stud_h, fit = fit, gap = gap) at (0, 0, 0) -> body Base
use Wall from "106_building_bricks_parts/brick.sc" with (nx = 4, ny = 2, pitch = pitch, plate_h = plate_h, stud_d = stud_d, stud_h = stud_h, fit = fit, gap = gap) at (pitch, 2*pitch, plate_h) -> body Wall
use Block from "106_building_bricks_parts/brick.sc" with (nx = 2, ny = 2, pitch = pitch, plate_h = plate_h, stud_d = stud_d, stud_h = stud_h, fit = fit, gap = gap) at (0, 0, plate_h) -> body Block
use Cap from "106_building_bricks_parts/brick.sc" with (nx = 1, ny = 4, units = 1, pitch = pitch, plate_h = plate_h, stud_d = stud_d, stud_h = stud_h, fit = fit, gap = gap) at (4*pitch, 0, 4*plate_h) -> body Cap
use Roof from "106_building_bricks_parts/slope.sc" with (nx = 2, pitch = pitch, plate_h = plate_h, stud_d = stud_d, stud_h = stud_h, fit = fit, gap = gap) at (pitch, 2*pitch, 4*plate_h) -> body Roof

// ==== Fit-test coupon, beside the build: the same 2 x 2 plate at three fits
use FitTight from "106_building_bricks_parts/brick.sc" with (nx = 2, ny = 2, units = 1, pitch = pitch, plate_h = plate_h, stud_d = stud_d, stud_h = stud_h, fit = 0.05 mm, gap = gap) at (8*pitch, 0, 0) -> body FitTight
use FitMid from "106_building_bricks_parts/brick.sc" with (nx = 2, ny = 2, units = 1, pitch = pitch, plate_h = plate_h, stud_d = stud_d, stud_h = stud_h, fit = 0.10 mm, gap = gap) at (8*pitch, 3*pitch, 0) -> body FitMid
use FitLoose from "106_building_bricks_parts/brick.sc" with (nx = 2, ny = 2, units = 1, pitch = pitch, plate_h = plate_h, stud_d = stud_d, stud_h = stud_h, fit = 0.15 mm, gap = gap) at (8*pitch, 6*pitch, 0) -> body FitLoose

color Base "#3a8f3a"
color Wall "#d23b2f"
color Block "#f2c12e"
color Cap "#202020"
color Roof "#2f6fd2"
color FitTight "#f4f4f0"
color FitMid "#f2c12e"
color FitLoose "#3a8f3a"
