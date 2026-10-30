// 60. Hole-fit ladder: five holes for a 5 mm pin, each 0.1 mm looser than the last, labelled
// with its clearance, and the pin to try in them.
//
// A `for` block writes the holes and labels: `k` runs from 1 to 5, `{k}` makes each name and label
// its own (Hole1, "0.1", …) and `k` in an expression is the number; the labels are text, so they
// assume the 0.1 mm step while the holes follow `step`. Print the plate and the pin, try
// the pin in each hole, and use the smallest clearance that slides for parts that must move.
//
// Print both flat as modelled, no supports: the holes go straight through, the labels are
// debossed into the top, and the pin stands on its end.
design HoleFitLadder units mm
param pin_d = 5 mm
param step = 0.1 mm
param pitch = 14 mm
param T = 5 mm
param label_h = 3.5 mm
param letter_depth = 0.6 mm
let L = 6*pitch

box Block size L 24 mm T corner (0,0,0) -> body Plate
for k from 1 to 5 {
  cylinder Hole{k} diameter (pin_d + k*step) height (T + 2 mm) base (k*pitch,15 mm,-1 mm) cut targets Plate
  sketch Label{k} on XY offset T {
    text "0.{k}" at (k*pitch,3 mm) height label_h align center
  }
  emboss Mark{k} from Label{k} depth letter_depth deboss targets Plate
}

cylinder Pin diameter pin_d height 20 mm base (L + 12 mm,12 mm,0) -> body Pin
chamfer PinTip edges (L + 12 mm + pin_d/2,12 mm,20 mm) distance 0.5 mm body Pin

check Plate overhang <= 45 deg
check Pin overhang <= 45 deg
check Plate size.x == L
