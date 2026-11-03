// 107. Wooden-style train track: five printable pieces on one shared dogbone connector, laid out
// as a closed oval with a spur that climbs a ramp to a buffer stop.
//
// Z is up, the loop lies on the floor (z = 0). Every piece is drawn in its own frame, starting at
// its origin heading +X, with a socket at the start and a dogbone peg at the end (the switch has
// two ends). The connector lives once in 107_train_track_parts/dogbone.sc: a piece joins it as its
// peg, and cuts it out grown by `clearance` as its socket, so every joint has the same fit.
// Neighbouring pieces leave `gap` between their end faces.
//
// The oval: switch (straight road), four 45 deg curves, a straight, four more curves, back to
// the switch. Its branch carries the ramp up `rise` to a buffer stop. The two checks at the end
// measure the closing joint: the last curve's peg must sit in the switch's socket with exactly
// `clearance` all round, which only happens when the loop closes. Change R or L and both
// straights and the curves still close, because the oval is built from the same numbers.
//
// Print each piece flat, top up, no supports: 0.2 mm layers, 3 walls, 20 % infill, PLA. The
// buffer and ramp print solid below their sloping tops. A wooden-track train runs on it (the
// grooves are 6 mm wide, 3 mm deep, 26 mm apart). Bill of materials: just the printed pieces;
// this loop is 1 switch, 8 curves, 1 straight, 1 ramp, 1 buffer.
design TrainTrack units mm

param L = 144 mm             // straight length, end to end
param R = 182 mm             // curve centreline radius (8 curves make a circle)
param rise = 36 mm           // ramp climb
param w = 40 mm
param t = 12 mm
param clearance = 0.3 mm
param gap = 0.5 mm

// The oval's two half circles: the right one turns about (L, R), the left about (0, R)
use Switch from "107_train_track_parts/switch.sc" with (L = L, R = R, w = w, t = t, clearance = clearance, gap = gap, x0 = 0 mm, y0 = 0 mm, a0 = 0 deg)
for k from 0 to 3 {
  use Right{k} from "107_train_track_parts/curve.sc" with (R = R, w = w, t = t, clearance = clearance, gap = gap, x0 = (L + R*sin(k*45 deg)), y0 = (R - R*cos(k*45 deg)), a0 = ((k*45 deg)))
}
use Back from "107_train_track_parts/straight.sc" with (L = L, w = w, t = t, clearance = clearance, gap = gap, x0 = (L), y0 = (2*R), a0 = (180 deg))
for k from 0 to 3 {
  use Left{k} from "107_train_track_parts/curve.sc" with (R = R, w = w, t = t, clearance = clearance, gap = gap, x0 = (-R*sin(k*45 deg)), y0 = (R + R*cos(k*45 deg)), a0 = ((180 deg + k*45 deg)))
}

// The spur off the switch's branch: up the ramp to the buffer stop
use Ramp from "107_train_track_parts/ramp.sc" with (L = L, rise = rise, w = w, t = t, clearance = clearance, gap = gap, x0 = (R*sin(45 deg)), y0 = (R - R*cos(45 deg)), a0 = (45 deg))
use Stop from "107_train_track_parts/buffer.sc" with (rise = rise, w = w, t = t, clearance = clearance, gap = gap, x0 = (R*sin(45 deg) + L*cos(45 deg)), y0 = (R - R*cos(45 deg) + L*sin(45 deg)), a0 = (45 deg))

// The loop closes: the last curve's peg sits in the switch's socket with the designed fit
check Left3_Curve clearance Switch_Switch >= (clearance - 0.01 mm)
check Left3_Curve clearance Switch_Switch <= (clearance + 0.01 mm)
// The spur clears the loop it sits inside
check Stop_Buffer clearance Right1_Curve >= 5 mm
