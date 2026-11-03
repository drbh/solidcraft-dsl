// 102. Slot-together plywood stool: two crossed leg panels and a round seat, no glue or hardware.
//
// Z is up, the stool stands on z = 0. LegA lies in the XZ plane and LegB in the YZ plane; they
// cross at the middle on half-lap slots (A's slot comes down from the top, B's up from its foot
// notch), so each panel's slot takes the other's full thickness. Two tabs on each panel's top edge
// pass through slots in the Seat and finish flush with its top, which locks the panels square.
// Every slot is the sheet thickness `t` plus `cnc_fit`, so changing `t` to your measured plywood
// resizes all of them. A round bit can't cut a sharp inside corner, so every inside corner that a
// mating part has to reach gets a dogbone relief of the bit's radius.
//
// Make: cut from 18 mm (or your `t`) plywood on a CNC router with a `bit_d` end mill. The design
// writes one DXF per sketch, each already flat in its panel's own coordinates:
//   LegAOutline + LegAReliefs, LegBOutline + LegBReliefs, SeatOutline + SeatSlots
// (cut the reliefs and slots first, the outlines last, with tabs). A 3MF of the parts laid flat
// on a 1220 x 1220 mm sheet and a STEP of the stool are written too. Sand, round over the seat edge, oil. Knock it
// together with a mallet; a tight `cnc_fit` (0.1-0.2 mm) needs no glue.
// BOM: one 1220 x 1220 x 18 mm sheet of birch plywood (the parts use about half of it).
design PlywoodStool units mm

param t = 18 mm              // measured sheet thickness
param cnc_fit = 0.2 mm       // added to every slot width
param bit_d = 6.35 mm        // end mill diameter (dogbone radius is half)
param seat_h = 450 mm        // floor to seat top
param seat_r = 180 mm
param w_top = 300 mm         // leg panel width under the seat
param w_bot = 400 mm         // leg panel width at the floor
param notch_w = 280 mm       // foot notch width at the floor
param notch_h = 60 mm
param tab_w = 50 mm
param tab_x = 110 mm         // tab centre from the middle

let s = t + cnc_fit                  // slot width
let rb = bit_d/2                     // dogbone radius
let under = seat_h - t               // panel top edge = seat underside
let mid = seat_h/2                   // where the two half-lap slots meet
let ti = tab_x - tab_w/2
let to = tab_x + tab_w/2

// ==== LegA in the XZ plane (sketch v is -z on XZ): slot from the top down to mid height
sketch LegAOutline on XZ offset (-t/2) {
  polyline closed o = (w_top/2, -under) (to, -under) (to, -seat_h) (ti, -seat_h) (ti, -under) (s/2, -under) (s/2, -mid) (-s/2, -mid) (-s/2, -under) (-ti, -under) (-ti, -seat_h) (-to, -seat_h) (-to, -under) (-w_top/2, -under) (-w_bot/2, 0) (-notch_w/2, 0) (-(notch_w/2 - 30 mm), -notch_h) (notch_w/2 - 30 mm, -notch_h) (notch_w/2, 0) (w_bot/2, 0)
  fix o
}
extrude LegABody from LegAOutline distance t new -> body LegA
// reliefs: the slot's closed end, and the inside corners at each tab's root
sketch LegAReliefs on XZ offset (-t/2 - 1 mm) {
  circle a = (s/2, -mid) radius rb
  circle b = (-s/2, -mid) radius rb
  circle c = (to, -under) radius rb
  circle d = (ti, -under) radius rb
  circle e = (-ti, -under) radius rb
  circle f = (-to, -under) radius rb
  fix a b c d e f
}
extrude LegACut from LegAReliefs distance (t + 2 mm) cut targets LegA

// ==== LegB in the YZ plane (sketch v is +z on YZ): slot from the foot notch up to mid height
sketch LegBOutline on YZ offset (-t/2) {
  polyline closed o = (w_bot/2, 0) (w_bot/2 - (w_bot - w_top)/2, under) (to, under) (to, seat_h) (ti, seat_h) (ti, under) (-ti, under) (-ti, seat_h) (-to, seat_h) (-to, under) (-w_top/2, under) (-w_bot/2, 0) (-notch_w/2, 0) (-(notch_w/2 - 30 mm), notch_h) (-s/2, notch_h) (-s/2, mid) (s/2, mid) (s/2, notch_h) (notch_w/2 - 30 mm, notch_h) (notch_w/2, 0)
  fix o
}
extrude LegBBody from LegBOutline distance t new -> body LegB
sketch LegBReliefs on YZ offset (-t/2 - 1 mm) {
  circle a = (s/2, mid) radius rb
  circle b = (-s/2, mid) radius rb
  circle c = (to, under) radius rb
  circle d = (ti, under) radius rb
  circle e = (-ti, under) radius rb
  circle f = (-to, under) radius rb
  fix a b c d e f
}
extrude LegBCut from LegBReliefs distance (t + 2 mm) cut targets LegB

// ==== Seat: a disc with four tab slots, dogboned at every corner
sketch SeatOutline on XY offset under {
  circle o = (0, 0) radius seat_r
  fix o
}
extrude SeatBody from SeatOutline distance t new -> body Seat
sketch SeatSlots on XY offset (under - 1 mm) {
  // LegA's tabs run along X, LegB's along Y
  rect a1 = (ti - cnc_fit/2, -s/2) (to + cnc_fit/2, s/2)
  rect a2 = (-to - cnc_fit/2, -s/2) (-ti + cnc_fit/2, s/2)
  rect b1 = (-s/2, ti - cnc_fit/2) (s/2, to + cnc_fit/2)
  rect b2 = (-s/2, -to - cnc_fit/2) (s/2, -ti + cnc_fit/2)
  fix a1 a2 b1 b2
}
extrude SeatSlotCut from SeatSlots distance (t + 2 mm) cut targets Seat
sketch SeatReliefs on XY offset (under - 1 mm) {
  circle a1 = (ti - cnc_fit/2, -s/2) radius rb
  circle a2 = (ti - cnc_fit/2, s/2) radius rb
  circle a3 = (to + cnc_fit/2, -s/2) radius rb
  circle a4 = (to + cnc_fit/2, s/2) radius rb
  circle a5 = (-ti + cnc_fit/2, -s/2) radius rb
  circle a6 = (-ti + cnc_fit/2, s/2) radius rb
  circle a7 = (-to - cnc_fit/2, -s/2) radius rb
  circle a8 = (-to - cnc_fit/2, s/2) radius rb
  circle b1 = (-s/2, ti - cnc_fit/2) radius rb
  circle b2 = (s/2, ti - cnc_fit/2) radius rb
  circle b3 = (-s/2, to + cnc_fit/2) radius rb
  circle b4 = (s/2, to + cnc_fit/2) radius rb
  circle b5 = (-s/2, -ti + cnc_fit/2) radius rb
  circle b6 = (s/2, -ti + cnc_fit/2) radius rb
  circle b7 = (-s/2, -to - cnc_fit/2) radius rb
  circle b8 = (s/2, -to - cnc_fit/2) radius rb
  fix a1 a2 a3 a4 a5 a6 a7 a8 b1 b2 b3 b4 b5 b6 b7 b8
}
extrude SeatReliefCut from SeatReliefs distance (t + 2 mm) cut targets Seat

// the parts bear on each other (seat on the top edges, slot ends at mid height) but never overlap
check LegA clearance LegB >= 0 mm
check LegA clearance Seat >= 0 mm
check LegB clearance Seat >= 0 mm

color LegA, LegB, Seat "#d8b98a"
