// 50. Name tag / bag label: a rounded plate with a raised rim, raised lettering and a strap slot.
//
// The plate lies on XY. The face is recessed inside a 2 mm rim and the lettering stands up from the
// recess to the same height as the rim, so the letters are protected and the top prints as one
// flat layer: pause at z = base_t to swap filament and the rim and letters print in a second
// colour. Keep each line inside the face: Liberation Sans capitals run about 0.9 x their
// height wide. Print flat on its back, no supports. Lettering is the built-in Liberation Sans; set
// `font` on the text lines to use an installed font instead.
design NameTag units mm
param L = 90 mm
param W = 30 mm
param corner_r = 5 mm
param base_t = 2 mm
param relief = 1 mm
param rim = 2 mm
param slot_w = 12 mm
param slot_h = 3.5 mm
param name_h = 7 mm
param line2_h = 4.5 mm
let face_x0 = slot_h + 2*rim + 3 mm

// Plate, then the recessed face inside the rim (the strap slot sits at the -X end, outside it)
sketch Outline on XY {
  rect rounded r = (0,0) (L, W) radius corner_r
  fix r
}
extrude Plate from Outline distance (base_t + relief) -> body Tag
sketch Face on XY offset base_t {
  rect rounded f = (face_x0, rim) (L - rim, W - rim) radius (corner_r - rim)
  fix f
}
extrude Recess from Face distance (relief + 1 mm) cut targets Tag

// Strap slot through the plate, across the -X end
sketch Strap on XY {
  slot s = centers (rim + 1 mm + slot_h/2, (W - slot_w)/2 + slot_h/2) (rim + 1 mm + slot_h/2, (W + slot_w)/2 - slot_h/2) width slot_h
  fix s
}
extrude StrapCut from Strap distance (base_t + relief + 1 mm) cut targets Tag

// Lettering, raised from the recess floor to the rim height
sketch Lettering on XY offset base_t {
  text "SOLIDCRAFT" at ((face_x0 + L - rim)/2, W/2 - name_h/2 + 2.5 mm) height name_h align center
  text "if found, call 555 0100" at ((face_x0 + L - rim)/2, rim + 2 mm) height line2_h align center
}
emboss Letters from Lettering depth relief targets Tag

// Prints without supports, in the print recipe's orientation.
check Tag overhang <= 45 deg
