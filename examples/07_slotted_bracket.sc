// 7. Interacting solids and a calculated capsule cut.
design SlottedBracket units mm
param L = 60 mm
param W = 30 mm
param T = 5 mm
param H = 25 mm
param slot_width = 6 mm
let slot_a = L/3
let slot_b = 3*L/4
box Base size L W T -> body BracketBody
box Upright size T W H corner (0,0,T) join targets BracketBody
sketch SlotCut on XY offset T {
  slot aperture = centers (slot_a,W/2) (slot_b,W/2) width slot_width
  fix aperture.line1 aperture.line2 aperture.arc1 aperture.arc2
}
extrude Clearance from SlotCut distance T negative cut targets BracketBody
