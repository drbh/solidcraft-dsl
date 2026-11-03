// 143. Screw and thread gauge plate for M2 to M10, with a set of radius gauges from 1 to 10 mm.
//
// X runs along the plate, Y up it, Z up from the bed. One column per size, M2 to M10, holds:
//   - a clearance hole (ISO 273 medium), to try a screw through
//   - a hex pocket a nut drops into, 0.2 mm over its width across flats
//   - a step of the head gauge along the top: one slot whose width steps down by socket head
//     diameter (ISO 4762) plus 0.3 mm, so a cap screw's head rests in the first step it fits
// and an engraved label. The sizes are the table of lets below, one line per size; the columns
// are a loop over it. Beside the plate, three test bolts (M6 x 1, M8 x 1.25 and M10 x 1.5,
// modelled threads on a hex flange) take a nut to tell metric from imperial and coarse from fine.
//
// The radius gauges are ten leaves, R1 to R10. Each has a convex end (both corners rounded to the
// radius) for inside fillets and a concave end (a notch of the radius) for outside rounds, an
// engraved R number and a 4 mm hole to put them on a ring or an M3 screw.
//
// Printing: everything flat, PLA, 0.2 mm layers, no supports; the test bolts stand on their
// flanges, which is the best way to print a thread. Print them at 0.12 mm layers if M6 matters.
// Holes and pockets are drawn true; check one against a screw and adjust `hole_play` for your
// printer.
design ThreadAndRadiusGauges units mm

param plate_t = 5 mm
param pitch = 20 mm            // column spacing
param margin = 12 mm
param pocket_d = 3 mm          // nut pocket depth
param hole_play = 0 mm         // added to every clearance hole's diameter
param nut_play = 0.2 mm        // added across the nut flats
param head_play = 0.3 mm       // added to every head step's width
param step_l = 20 mm           // length of each head step
param label_h = 4.5 mm

// Size table: clearance hole, nut across flats, socket head diameter
let hole0 = 2.4 mm
let nut0 = 4 mm
let head0 = 3.8 mm
let hole1 = 2.9 mm
let nut1 = 5 mm
let head1 = 4.5 mm
let hole2 = 3.4 mm
let nut2 = 5.5 mm
let head2 = 5.5 mm
let hole3 = 4.5 mm
let nut3 = 7 mm
let head3 = 7 mm
let hole4 = 5.5 mm
let nut4 = 8 mm
let head4 = 8.5 mm
let hole5 = 6.6 mm
let nut5 = 10 mm
let head5 = 10 mm
let hole6 = 9 mm
let nut6 = 13 mm
let head6 = 13 mm
let hole7 = 11 mm
let nut7 = 16 mm
let head7 = 16 mm

let plate_l = 2*margin + 7*pitch
let plate_w = 82 mm
let y_hole = 11 mm
let y_nut = 28 mm
let y_label = 43 mm
let y_step = 64 mm
let x_bolt = plate_l + 16 mm

// Radius leaves
param leaf_l = 50 mm
param leaf_w = 24 mm
param leaf_t = 1.6 mm
let leaf_y0 = -12 mm - 10*(leaf_w + 4 mm)

// ==== Gauge plate
box Plate size plate_l plate_w plate_t new -> body Plate
fillet PlateCorners edges (0, 0, plate_t/2) (plate_l, 0, plate_t/2) (0, plate_w, plate_t/2) (plate_l, plate_w, plate_t/2) radius 4 mm body Plate
for k from 0 to 7 {
  cylinder Hole{k} radius ((hole{k} + hole_play)/2) height (plate_t + 2 mm) base (margin + k*pitch, y_hole, -1 mm) axis Z cut targets Plate
  sketch NutSk{k} on XY offset (plate_t - pocket_d) {
    polygon n = (margin + k*pitch, y_nut) radius ((nut{k} + nut_play)/sqrt(3)) sides 6
    fix n
  }
  extrude NutPocket{k} from NutSk{k} distance (pocket_d + 1 mm) cut targets Plate
  // Head steps run widest (M10) to narrowest (M2) from the left; each reaches 0.5 mm back into
  // the wider step before it
  box HeadStep{k} size (step_l + 0.5 mm) (head{k} + head_play) (plate_t + 2 mm) corner (margin - pitch/2 + (7 - k)*step_l - 0.5 mm, y_step - (head{k} + head_play)/2, -1 mm) cut targets Plate
}

// Test bolts beside the plate: a hex flange (the nut's width across flats) and a stud, one
// body each; the threads are cut last
sketch Flange6Sk on XY {
  polygon f = (x_bolt, 14 mm) radius (10 mm/sqrt(3)) sides 6
  fix f
}
extrude Flange6 from Flange6Sk distance 4 mm new -> body BoltM6
cylinder Stud6 radius 3 mm height 14 mm base (x_bolt, 14 mm, 3.5 mm) axis Z join targets BoltM6
sketch Flange8Sk on XY {
  polygon f = (x_bolt, 40 mm) radius (13 mm/sqrt(3)) sides 6
  fix f
}
extrude Flange8 from Flange8Sk distance 4 mm new -> body BoltM8
cylinder Stud8 radius 4 mm height 14 mm base (x_bolt, 40 mm, 3.5 mm) axis Z join targets BoltM8
sketch Flange10Sk on XY {
  polygon f = (x_bolt, 68 mm) radius (16 mm/sqrt(3)) sides 6
  fix f
}
extrude Flange10 from Flange10Sk distance 4 mm new -> body BoltM10
cylinder Stud10 radius 5 mm height 14 mm base (x_bolt, 68 mm, 3.5 mm) axis Z join targets BoltM10

// Labels: M sizes under each column, and the head gauge
sketch LabelSk on XY offset plate_t {
  text "M2" at (margin, y_label) height label_h align center
  text "M2.5" at (margin + pitch, y_label) height label_h align center
  text "M3" at (margin + 2*pitch, y_label) height label_h align center
  text "M4" at (margin + 3*pitch, y_label) height label_h align center
  text "M5" at (margin + 4*pitch, y_label) height label_h align center
  text "M6" at (margin + 5*pitch, y_label) height label_h align center
  text "M8" at (margin + 6*pitch, y_label) height label_h align center
  text "M10" at (margin + 7*pitch, y_label) height label_h align center
  text "HEAD M10 > M2" at (margin + 3.5*pitch, plate_w - 6.5 mm) height 3.5 mm align center
}
emboss Labels from LabelSk depth 0.6 mm deboss targets Plate

// ==== Radius leaves R1 to R10, below the plate
for k from 1 to 10 {
  box LeafPlate{k} size leaf_l leaf_w leaf_t corner (0, leaf_y0 + (k - 1)*(leaf_w + 4 mm), 0) new -> body Leaf{k}
  fillet Convex{k} edges (0, leaf_y0 + (k - 1)*(leaf_w + 4 mm), leaf_t/2) (0, leaf_y0 + (k - 1)*(leaf_w + 4 mm) + leaf_w, leaf_t/2) radius (k*1 mm) body Leaf{k}
  cylinder Concave{k} radius (k*1 mm) height (leaf_t + 2 mm) base (leaf_l, leaf_y0 + (k - 1)*(leaf_w + 4 mm) + leaf_w/2, -1 mm) axis Z cut targets Leaf{k}
  cylinder RingHole{k} radius 2 mm height (leaf_t + 2 mm) base (leaf_l - 18 mm, leaf_y0 + (k - 1)*(leaf_w + 4 mm) + leaf_w/2, -1 mm) axis Z cut targets Leaf{k}
  sketch LeafLabel{k} on XY offset leaf_t {
    text "R{k}" at (leaf_l/2 - 8 mm, leaf_y0 + (k - 1)*(leaf_w + 4 mm) + leaf_w/2) height 6 mm align center
  }
  emboss LeafEngrave{k} from LeafLabel{k} depth 0.5 mm deboss targets Leaf{k}
}

// ==== Threads last (modelled threads are slow and break later booleans)
thread Thread6 face (x_bolt + 3 mm, 14 mm, 11 mm) designation M6x1 modeled
thread Thread8 face (x_bolt + 4 mm, 40 mm, 11 mm) designation "M8x1.25" modeled
thread Thread10 face (x_bolt + 5 mm, 68 mm, 11 mm) designation "M10x1.5" modeled

color Plate, BoltM6, BoltM8, BoltM10 "#d8dde3"
color Leaf1, Leaf2, Leaf3, Leaf4, Leaf5, Leaf6, Leaf7, Leaf8, Leaf9, Leaf10 "#f28c28"
material Plate, BoltM6, BoltM8, BoltM10, Leaf1, Leaf2, Leaf3, Leaf4, Leaf5, Leaf6, Leaf7, Leaf8, Leaf9, Leaf10 PLA
