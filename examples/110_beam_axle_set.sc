// 110. Beam-and-axle construction set: beams, plates, cross axles, friction pins and gears on
// one 10 mm hole pitch, built into a small gear frame.
//
// Z is up. Every piece is a parts file used with its own sizes:
//   110_beam_axle_set_parts/beam.sc   n round holes, rounded ends, beam_w wide, beam_t thick
//   110_beam_axle_set_parts/plate.sc  nx by ny holes, plate_t thick
//   110_beam_axle_set_parts/axle.sc   a cross axle, turns freely in round holes
//   110_beam_axle_set_parts/pin.sc    a friction pin through `layers` beams, slit sprung ends
//   110_beam_axle_set_parts/gear.sc   straight-flank spur gear with a cross hole
// The interface is the hole: hole_d round holes on `pitch`. Pins are hole_d - 2*pin_clr with
// end ridges `ridge` proud, so they snap in and hold by friction (the ridges overlap the hole
// edge on purpose). Axles are a cross `span` across that spins in a round hole and drives a
// cross hole with `axle_clr` play. Gears mesh on the grid when m*(z1 + z2)/2 is a multiple of
// pitch: here module 1.25 with 12 and 20 teeth is two holes apart.
//
// The build: two 9-hole beams, one above the other, held 7 mm apart at their far ends by a
// 3-hole spacer beam and two 3-layer pins. Between them, a 12-tooth and a 20-tooth gear on
// axles through holes 1 and 3: turn one axle and the other turns at 12:20. A 5 x 3 plate lies
// beside it.
//
// Print: PLA, 0.2 mm layers, no supports. Beams, plates and gears flat; axles on two arms and
// pins on their flat (the print recipe turns them). Test one pin in a beam first and adjust
// pin_clr / ridge if it is too tight or loose. BOM: filament only.
design BeamAxleSet units mm

param pitch = 10 mm
param beam_w = 9 mm
param beam_t = 7 mm
param hole_d = 6.2 mm
param gear_t = 6 mm

let gear_z = beam_t + (beam_t - gear_t)/2

// ==== Frame: lower beam, spacer, upper beam
use Lower from "110_beam_axle_set_parts/beam.sc" with (n = 9, pitch = pitch, beam_w = beam_w, beam_t = beam_t, hole_d = hole_d) at (0, 0, 0) -> body Lower
use Spacer from "110_beam_axle_set_parts/beam.sc" with (n = 3, pitch = pitch, beam_w = beam_w, beam_t = beam_t, hole_d = hole_d) at (6*pitch, 0, beam_t) -> body Spacer
use Upper from "110_beam_axle_set_parts/beam.sc" with (n = 9, pitch = pitch, beam_w = beam_w, beam_t = beam_t, hole_d = hole_d) at (0, 0, 2*beam_t) -> body Upper
use PinA from "110_beam_axle_set_parts/pin.sc" with (layers = 3, beam_t = beam_t, hole_d = hole_d) at (6*pitch, 0, 0.2 mm) -> body PinA
use PinB from "110_beam_axle_set_parts/pin.sc" with (layers = 3, beam_t = beam_t, hole_d = hole_d) at (8*pitch, 0, 0.2 mm) -> body PinB

// ==== Gear train between the beams
use AxleA from "110_beam_axle_set_parts/axle.sc" with (len = 3*beam_t + 4 mm) at (0, 0, -2 mm) -> body AxleA
use AxleB from "110_beam_axle_set_parts/axle.sc" with (len = 3*beam_t + 4 mm) rotate Z 9 deg at (2*pitch, 0, -2 mm) -> body AxleB
use Pinion from "110_beam_axle_set_parts/gear.sc" with (z = 12, thick = gear_t) at (0, 0, gear_z) -> body Pinion
use Wheel from "110_beam_axle_set_parts/gear.sc" with (z = 20, thick = gear_t) rotate Z 9 deg at (2*pitch, 0, gear_z) -> body Wheel

// ==== A plate, beside the build
use Base from "110_beam_axle_set_parts/plate.sc" with (nx = 5, ny = 3, pitch = pitch, beam_w = beam_w, hole_d = hole_d) at (0, 3*pitch, 0) -> body Base

color Lower "#c8352b"
color Spacer "#2b5fc8"
color Upper "#c8352b"
color Pinion "#f2b02e"
color Wheel "#f2b02e"
