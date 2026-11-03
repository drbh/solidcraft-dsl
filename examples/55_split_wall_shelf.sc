// 55. Wall shelf longer than the print bed: a back plate with screw holes, a shelf, and gussets
// in the corner between them, split into two halves that each fit a 250 mm bed.
//
// Modelled lying in its print pose: X along the shelf, the back plate flat on the bed (z = 0 to
// back_t, y = 0 to back_h) and the shelf standing up from its lower edge (y = 0 to shelf_t,
// z = 0 to depth). Mounted, the back plate is on the wall and the shelf sticks out level. Each
// gusset is a rib from a line across the corner.
// The split runs through the middle, between gussets; the halves butt together on the wall, each
// held by its own screws.
//
// Print each half on its back, no supports: the shelf and the gussets stand straight up.
design SplitWallShelf units mm
param length = 320 mm
param back_h = 70 mm
param back_t = 5 mm
param depth = 110 mm
param shelf_t = 5 mm
param gusset = 45 mm
param gusset_t = 4 mm
param screw_d = 4.5 mm
param bed = 250 mm
let gusset_pitch = 100 mm

box Back size length back_h back_t corner (0,0,0) -> body Shelf
box Ledge size length shelf_t depth corner (0,0,0) join targets Shelf

// Gussets across the corner, one rib each: a line from the back plate to the shelf, filled to
// both. (Engines before 0.27 cannot pattern a rib, so each has its own sketch.)
sketch Corner1 on YZ offset 20 mm {
  line g = (shelf_t + gusset,back_t) (shelf_t,back_t + gusset)
  fix g
}
rib Gusset1 from Corner1 curves g thickness gusset_t
sketch Corner2 on YZ offset (length/2 - gusset_pitch/2) {
  line g = (shelf_t + gusset,back_t) (shelf_t,back_t + gusset)
  fix g
}
rib Gusset2 from Corner2 curves g thickness gusset_t
sketch Corner3 on YZ offset (length/2 + gusset_pitch/2) {
  line g = (shelf_t + gusset,back_t) (shelf_t,back_t + gusset)
  fix g
}
rib Gusset3 from Corner3 curves g thickness gusset_t
sketch Corner4 on YZ offset (length - 20 mm) {
  line g = (shelf_t + gusset,back_t) (shelf_t,back_t + gusset)
  fix g
}
rib Gusset4 from Corner4 curves g thickness gusset_t

// Two screw holes in each half of the back plate, between the gussets.
sketch Screws on XY {
  circle s = (length/8,back_h*0.6) diameter screw_d
  fix s
  pattern rectangular s count 4 spacing (length/4) dir (1,0)
}
extrude ScrewHoles from Screws distance back_t cut targets Shelf

// Halves that fit the bed: the right one keeps the name, the left one becomes `ShelfLeft`.
split Halves target Shelf origin (length/2,0,0) normal (1,0,0) -> body ShelfLeft
color Shelf "#d9a441"
color ShelfLeft "#8a5a1f"

check Shelf size.x <= bed
check ShelfLeft size.x <= bed
check Shelf overhang <= 45 deg
check ShelfLeft overhang <= 45 deg
