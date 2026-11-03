// 113. French cleat wall system: printable wall cleats and five tool holders that hang on them.
//
// X along the wall, Y out from the wall face (y = 0), Z up. The wall cleat is a 12 x 30 mm bar whose
// top is bevelled 45 deg down toward the wall; it screws to the wall through counterbored holes and
// prints in 240 mm sections (two here, butted end to end). Every holder starts from the same hanging
// cleat, 113_french_cleat_parts/hanger.sc, placed with `use`: a back plate, a hook that drops over
// the bevel with `gap` clearance, and a spacer that keeps it plumb. Because both halves share one
// 45 deg face, any holder hangs anywhere along any cleat and slides sideways to make room.
//
// Holders (left to right): a shelf with a front lip and two gussets; a hook whose arm rises at
// 45 deg to a tip; a bin; a plier rack with open slots the handles drop through; a screwdriver row
// with 8 mm shank holes. Each is built so its lowest part is flat at its zmin, so it prints standing
// up as modelled: the cleat's bevel is a 45 deg overhang, the hook arm too, and nothing else
// overhangs. Wall cleats print on their back face, bevel up.
//
// BOM: wall screws (4 x 40 mm wood screws or drywall anchors per section, 4.5 mm holes, 9 mm heads).
// PLA or PETG, 4 walls / 30 % infill for the shelf and hook, which carry load on the cleat.
design FrenchCleatSystem units mm

param cleat_t = 12 mm
param cleat_h = 30 mm
param gap = 0.4 mm
param plate_t = 5 mm
param section_l = 240 mm
param screw_d = 4.5 mm

let hy1 = cleat_t + gap + plate_t     // front face of every holder's back plate

// ==== Wall cleats: two sections, counterbored screw holes every 60 mm
for k from 0 to 1 {
  sketch CleatSk{k} on YZ offset (k*(section_l + 0.5 mm)) {
    polyline closed p = (0, 0) (cleat_t, 0) (cleat_t, cleat_h) (0, cleat_h - cleat_t)
    fix p
  }
  extrude CleatBar{k} from CleatSk{k} distance section_l new -> body WallCleat{k}
  for j from 0 to 3 {
    cylinder CleatScrew{k}{j} diameter screw_d height (cleat_t + 2 mm) base (k*(section_l + 0.5 mm) + 30 mm + j*60 mm, -1 mm, 9 mm) axis Y cut targets WallCleat{k}
    cylinder CleatHead{k}{j} diameter 9 mm height 4.3 mm base (k*(section_l + 0.5 mm) + 30 mm + j*60 mm, cleat_t - 4 mm, 9 mm) axis Y cut targets WallCleat{k}
  }
  color WallCleat{k} "#8a8f96"
}

// ==== Shelf: 80 wide, 80 deep, front lip and two gussets
use Shelf from "113_french_cleat_parts/hanger.sc" with (w = 80 mm, zmin = -40 mm, cleat_t = cleat_t, cleat_h = cleat_h, gap = gap, plate_t = plate_t) at (0, 0, 0) -> body Shelf
box ShelfBoard size 80 mm 80.3 mm 4 mm corner (0, hy1 - 0.3 mm, -40 mm) join targets Shelf
box ShelfLip size 80 mm 4 mm 8.3 mm corner (0, hy1 + 76 mm, -36.3 mm) join targets Shelf
for g from 0 to 1 {
  sketch GussetSk{g} on YZ offset (g*76 mm) {
    polyline closed p = (hy1 - 0.3 mm, -36.3 mm) (hy1 + 50 mm, -36.3 mm) (hy1 - 0.3 mm, 10 mm)
    fix p
  }
  extrude Gusset{g} from GussetSk{g} distance 4 mm join targets Shelf
}

// ==== Hook: an arm rising at 45 deg to an upturned tip
use Hook from "113_french_cleat_parts/hanger.sc" with (w = 20 mm, zmin = -30 mm, cleat_t = cleat_t, cleat_h = cleat_h, gap = gap, plate_t = plate_t) at (90 mm, 0, 0) -> body Hook
sketch HookArmSk on YZ offset 90 mm {
  polyline closed p = (hy1 - 0.3 mm, -30 mm) (hy1 + 6 mm, -30 mm) (hy1 + 40 mm, 4 mm) (hy1 + 40 mm, 14 mm) (hy1 + 34 mm, 14 mm) (hy1 + 34 mm, 6.5 mm) (hy1 - 0.3 mm, -27.8 mm)
  fix p
}
extrude HookArm from HookArmSk distance 20 mm join targets Hook

// ==== Bin: 70 wide, 50 deep, 60 tall, open top, the back plate is its back wall
use Bin from "113_french_cleat_parts/hanger.sc" with (w = 70 mm, zmin = -40 mm, cleat_t = cleat_t, cleat_h = cleat_h, gap = gap, plate_t = plate_t) at (120 mm, 0, 0) -> body Bin
box BinBlock size 70 mm 50.3 mm 60 mm corner (120 mm, hy1 - 0.3 mm, -40 mm) join targets Bin
box BinCavity size 66 mm 47 mm 60 mm corner (122 mm, hy1 + 1 mm, -38 mm) cut targets Bin

// ==== Plier rack: a bar with four open slots
use Pliers from "113_french_cleat_parts/hanger.sc" with (w = 60 mm, zmin = -20 mm, cleat_t = cleat_t, cleat_h = cleat_h, gap = gap, plate_t = plate_t) at (200 mm, 0, 0) -> body Pliers
box PlierBar size 60 mm 40.3 mm 8 mm corner (200 mm, hy1 - 0.3 mm, -20 mm) join targets Pliers
for s from 0 to 3 {
  box PlierSlot{s} size 8 mm 26 mm 10 mm corner (200 mm + 3.5 mm + s*15 mm, hy1 + 15 mm, -21 mm) cut targets Pliers
}

// ==== Screwdriver row: six 8 mm shank holes
use Drivers from "113_french_cleat_parts/hanger.sc" with (w = 80 mm, zmin = -20 mm, cleat_t = cleat_t, cleat_h = cleat_h, gap = gap, plate_t = plate_t) at (270 mm, 0, 0) -> body Drivers
box DriverBar size 80 mm 30.3 mm 8 mm corner (270 mm, hy1 - 0.3 mm, -20 mm) join targets Drivers
sketch DriverHolesSk on XY offset (-21 mm) {
  circle h = (277.5 mm, hy1 + 16 mm) diameter 8 mm
  fix h
  pattern rectangular h count 6 spacing 13 mm
}
extrude DriverHoles from DriverHolesSk distance 10 mm cut targets Drivers

color Shelf "#c47b2b"
color Hook "#2f6f8f"
color Bin "#6c8f3a"
color Pliers "#b33a3a"
color Drivers "#7a4fa0"
material WallCleat0, WallCleat1, Shelf, Hook, Bin, Pliers, Drivers PLA
