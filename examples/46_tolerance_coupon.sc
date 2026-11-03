// 46. Fit test coupon: round holes and sliding slots at five clearances on a nominal 8 mm pin.
//
// X runs along the rows, Y across them, Z up; the plate's lower-left corner is the origin.
// The Plate has a row of round holes (front) and a row of slots (back), five columns each. Column
// i is cut at nominal + i*step (0.1 ... 0.5 mm over 8 mm with the defaults), and its clearance is
// debossed between the rows as "0.1" ... "0.5". The PegStrip carries five plain 8 mm pegs, one per
// tab, joined by thin breakaway bridges: snap a peg off and try it in each hole for a press, push or
// running fit, and slide it along each slot to find a sliding fit. Every hole and slot has a small
// chamfer on both faces so elephant's foot on the first layers does not tighten the fit.
//
// Both parts print flat as modelled, no supports: the holes and slots are vertical, the labels
// are shallow recesses in the top face, and the pegs stand straight up from their tabs with a
// chamfered top.
design ToleranceCoupon units mm
// Fit
param nominal = 8 mm
param step = 0.1 mm
// Plate
param plate_t = 4 mm
param pitch = 16 mm
param slot_len = 14 mm
param edge_chamfer = 0.3 mm
param label_h = 5 mm
param label_depth = 0.6 mm
param label_y = 22 mm
// Pegs
param peg_h = 10 mm
param tab = 12 mm
param tab_t = 2 mm
param bridge_w = 1.2 mm
param bridge_t = 0.8 mm
let margin = 4 mm + pitch/2
let plate_l = 4*pitch + 2*margin
let hole_y = 11 mm
let slot_y0 = 36 mm
let slot_y1 = slot_y0 + slot_len
let plate_w = slot_y1 + nominal/2 + 5 mm
let x1 = margin
let x2 = margin + pitch
let x3 = margin + 2*pitch
let x4 = margin + 3*pitch
let x5 = margin + 4*pitch
let d1 = nominal + step
let d2 = nominal + 2*step
let d3 = nominal + 3*step
let d4 = nominal + 4*step
let d5 = nominal + 5*step

box PlateBox size plate_l plate_w plate_t -> body Plate

// Round holes, one per clearance
sketch HolesSk on XY offset 0 {
  circle h1 = (x1, hole_y) diameter d1
  circle h2 = (x2, hole_y) diameter d2
  circle h3 = (x3, hole_y) diameter d3
  circle h4 = (x4, hole_y) diameter d4
  circle h5 = (x5, hole_y) diameter d5
  fix h1 h2 h3 h4 h5
}
extrude Holes from HolesSk distance plate_t cut targets Plate

// Slots for a sliding fit, same clearances
sketch SlotsSk on XY offset 0 {
  slot s1 = (x1, slot_y0) (x1, slot_y1) width d1
  slot s2 = (x2, slot_y0) (x2, slot_y1) width d2
  slot s3 = (x3, slot_y0) (x3, slot_y1) width d3
  slot s4 = (x4, slot_y0) (x4, slot_y1) width d4
  slot s5 = (x5, slot_y0) (x5, slot_y1) width d5
  fix s1.line1 s1.line2 s1.arc1 s1.arc2 s2.line1 s2.line2 s2.arc1 s2.arc2 s3.line1 s3.line2 s3.arc1 s3.arc2 s4.line1 s4.line2 s4.arc1 s4.arc2 s5.line1 s5.line2 s5.arc1 s5.arc2
}
extrude Slots from SlotsSk distance plate_t cut targets Plate

// Lead-in chamfers on both faces (points on each hole edge and each slot's straight side)
chamfer HoleEdges edges (x1+d1/2, hole_y, 0) (x2+d2/2, hole_y, 0) (x3+d3/2, hole_y, 0) (x4+d4/2, hole_y, 0) (x5+d5/2, hole_y, 0) (x1+d1/2, hole_y, plate_t) (x2+d2/2, hole_y, plate_t) (x3+d3/2, hole_y, plate_t) (x4+d4/2, hole_y, plate_t) (x5+d5/2, hole_y, plate_t) distance edge_chamfer body Plate
chamfer SlotEdges edges (x1+d1/2, slot_y0+slot_len/2, 0) (x2+d2/2, slot_y0+slot_len/2, 0) (x3+d3/2, slot_y0+slot_len/2, 0) (x4+d4/2, slot_y0+slot_len/2, 0) (x5+d5/2, slot_y0+slot_len/2, 0) (x1+d1/2, slot_y0+slot_len/2, plate_t) (x2+d2/2, slot_y0+slot_len/2, plate_t) (x3+d3/2, slot_y0+slot_len/2, plate_t) (x4+d4/2, slot_y0+slot_len/2, plate_t) (x5+d5/2, slot_y0+slot_len/2, plate_t) distance edge_chamfer body Plate

// Clearance labels, debossed into the top face between the rows.
// (Text positions are written with params only: `text` does not resolve `let` helpers yet.)
sketch LabelsSk on XY offset plate_t {
  text "0.1" at (4 mm + pitch/2 + 0*pitch, label_y) height label_h align center
  text "0.2" at (4 mm + pitch/2 + 1*pitch, label_y) height label_h align center
  text "0.3" at (4 mm + pitch/2 + 2*pitch, label_y) height label_h align center
  text "0.4" at (4 mm + pitch/2 + 3*pitch, label_y) height label_h align center
  text "0.5" at (4 mm + pitch/2 + 4*pitch, label_y) height label_h align center
}
emboss Labels from LabelsSk depth label_depth deboss targets Plate

// Peg strip, in front of the plate: square tabs with an 8 mm peg each, joined by thin bridges
let strip_y = -tab - 8 mm
let tab_y = strip_y + tab/2
box Tab1 size tab tab tab_t corner (x1-tab/2, strip_y, 0) new -> body PegStrip
box Bridges size (4*pitch) bridge_w bridge_t corner (x1, tab_y-bridge_w/2, 0) join targets PegStrip
box Tab2 size tab tab tab_t corner (x2-tab/2, strip_y, 0) join targets PegStrip
box Tab3 size tab tab tab_t corner (x3-tab/2, strip_y, 0) join targets PegStrip
box Tab4 size tab tab tab_t corner (x4-tab/2, strip_y, 0) join targets PegStrip
box Tab5 size tab tab tab_t corner (x5-tab/2, strip_y, 0) join targets PegStrip
sketch PegsSk on XY offset tab_t {
  circle p1 = (x1, tab_y) diameter nominal
  circle p2 = (x2, tab_y) diameter nominal
  circle p3 = (x3, tab_y) diameter nominal
  circle p4 = (x4, tab_y) diameter nominal
  circle p5 = (x5, tab_y) diameter nominal
  fix p1 p2 p3 p4 p5
}
extrude Pegs from PegsSk distance peg_h join targets PegStrip
chamfer PegTips edges (x1+nominal/2, tab_y, tab_t+peg_h) (x2+nominal/2, tab_y, tab_t+peg_h) (x3+nominal/2, tab_y, tab_t+peg_h) (x4+nominal/2, tab_y, tab_t+peg_h) (x5+nominal/2, tab_y, tab_t+peg_h) distance 0.8 mm body PegStrip

// Prints without supports, in the print recipe's orientation.
check Plate overhang <= 45 deg
check PegStrip overhang <= 45 deg
