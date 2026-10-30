// 52. Vented lid for a project box: a flat plate with a locating lip that drops inside the box
// walls, a field of vent slots, four corner screw holes and two cable notches.
//
// One outline drives everything. The plate is the box's outline; the lip is the band between two
// offsets of it (the wall plus a little play, and the lip thickness further in), so changing the
// box size, wall or play moves the lip with it. The vent slots, the screw holes and the notches
// are drawn once and copied with sketch patterns and a mirror.
//
// Print as modelled, the outer face on the bed: the lip and slot walls stand straight up.
design VentedLid units mm
// The box this lid fits
param box_w = 90 mm
param box_d = 64 mm
param corner_r = 6 mm
param box_wall = 2 mm
param play = 0.3 mm
// Lid
param plate_t = 2.4 mm
param lip_t = 1.6 mm
param lip_h = 4 mm
// Vents: rows of slots in the middle of the lid
param slot_l = 22 mm
param slot_w = 2.4 mm
param slot_pitch = 5 mm
param slots = 7
// Screws into the box's corner bosses, far enough in that the bosses clear the lip
param screw_inset = 10 mm
param screw_d = 3.2 mm
// Cable notches in the lid edge
param notch_w = 6 mm
let inner = box_wall + play
let vent_x = box_w/2 - slot_l/2
let vent_y = box_d/2 - (slots - 1)*slot_pitch/2

sketch Outline on XY {
  rect rounded o = (0,0) (box_w,box_d) radius corner_r
  fix o
}
extrude Plate from Outline distance plate_t -> body Lid

// Lip: the band between the inner wall face (less the play) and the lip thickness further in.
sketch LipBand on XY offset plate_t {
  rect rounded o = (0,0) (box_w,box_d) radius corner_r
  fix o
  offset o distance inner side (box_w/2,box_d/2)
  offset o distance (inner + lip_t) side (box_w/2,box_d/2)
}
extrude Lip from LipBand distance lip_h profiles (box_w/2,inner + lip_t/2) join targets Lid

sketch Vents on XY {
  slot s = ends (vent_x,vent_y) (vent_x + slot_l,vent_y) width slot_w
  fix s
  pattern rectangular s count slots spacing slot_pitch dir (0,1)
}
extrude VentCut from Vents distance plate_t cut targets Lid

sketch Screws on XY {
  circle h = (screw_inset,screw_inset) diameter screw_d
  fix h
  pattern rectangular h count 2 spacing (box_w - 2*screw_inset) count2 2 spacing2 (box_d - 2*screw_inset)
}
extrude ScrewHoles from Screws distance plate_t cut targets Lid

// A notch through the lip at each end, for cables; drawn at one end, mirrored to the other.
sketch Notches on XY offset plate_t {
  construction line mid = (box_w/2,-1 mm) (box_w/2,box_d + 1 mm)
  rect n = (inner - 1 mm,box_d/2 - notch_w/2) (inner + lip_t + 1 mm,box_d/2 + notch_w/2)
  fix mid n
  mirror n about mid
}
extrude NotchCut from Notches distance lip_h cut targets Lid

// Prints without supports, and the outline sets the size.
check Lid overhang <= 45 deg
check Lid size.x == box_w
