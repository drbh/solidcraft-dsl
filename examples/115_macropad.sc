// 115. Mechanical-keyboard macropad: MX switch plate, case, printable keycaps and an encoder knob.
//
// X runs along the key columns, Y along the rows (y grows away from the typist), Z up; the case
// floor's underside is z = 0. Keys sit on the standard 19.05 mm pitch, `rows` x `cols` of them, and
// the switch plate is 1.5 mm thick with 14.0 mm square cutouts, the MX plate spec, so switches clip
// in. With `encoder` = 1 an extra column on the right holds an EC11 rotary encoder (7 mm hole) and
// its knob.
//
// The case is a rounded tray shelled to `wall`. The plate rests on the wall tops and is held by four
// M2 screws into corner bosses. Under the plate a pocket at the back left holds an RP2040-Zero
// (23.5 x 18 mm) between two rails, its USB-C facing out of a slot in the back wall. Switch pins
// reach down to case_h - 6.8 mm, clear of the board (6.2 mm tall with its port). The matrix is
// hand-wired: one diode per switch, rows and columns to the board's pins.
//
// Keycaps are DSA-like: an 18 mm square base tapering to about 14.8 mm, 7.5 mm tall, hollowed to
// 1.2 mm (an inner tapered pocket), with a 5.5 mm stem carrying the MX cross (4.15 x 1.35 mm arms, about 0.1 mm open for a
// push fit). The knob takes the EC11's 6 mm D shaft.
//
// Print: case open side up; plate flat; keycaps top down (stems up), no supports; knob top down.
// PLA, 0.2 mm layers (0.12 mm for the caps if the cross is tight). Test one cap before printing all.
// BOM: rows*cols MX-style switches and 1N4148 diodes, an RP2040-Zero (or Pro Micro, which fits the
// same pocket at 33 x 18 mm with `mcu_l`), an EC11 encoder, 4 x M2x6 screws, 4 rubber feet, wire.
design Macropad units mm

param rows = 3
param cols = 4
param encoder = 1
param pitch = 19.05 mm
param cutout = 14 mm
param plate_t = 1.5 mm
param wall = 2.4 mm
param case_h = 14 mm
param margin = 1.5 mm
param mcu_w = 18 mm
param mcu_l = 23.5 mm
param cap_h = 7.5 mm

let enc_w = encoder*25 mm
let iw = cols*pitch + enc_w + 2*margin
let id = rows*pitch + 2*margin
let plate_z = case_h
let cap_z = plate_z + plate_t + 5 mm
let mcu_x = 4 mm

// ==== Case: rounded tray, shelled, with screw bosses, board rails and a USB-C slot
sketch CaseSk on XY {
  rect rounded o = (-wall, -wall) (iw + wall, id + wall) radius 4 mm
  fix o
}
extrude CaseBlock from CaseSk distance case_h new -> body Case
shell CaseCavity faces (iw/2, id/2, case_h) thickness wall inside body Case
cylinder Boss0 radius 3 mm height (case_h - wall) base (2 mm, 2 mm, wall - 0.3 mm) axis Z join targets Case
cylinder Boss1 radius 3 mm height (case_h - wall) base (iw - 2 mm, 2 mm, wall - 0.3 mm) axis Z join targets Case
cylinder Boss2 radius 3 mm height (case_h - wall) base (2 mm, id - 2 mm, wall - 0.3 mm) axis Z join targets Case
cylinder Boss3 radius 3 mm height (case_h - wall) base (iw - 2 mm, id - 2 mm, wall - 0.3 mm) axis Z join targets Case
sketch PilotSk on XY offset (case_h - 9 mm) {
  circle a = (2 mm, 2 mm) diameter 1.8 mm
  circle b = (iw - 2 mm, 2 mm) diameter 1.8 mm
  circle c = (2 mm, id - 2 mm) diameter 1.8 mm
  circle d = (iw - 2 mm, id - 2 mm) diameter 1.8 mm
  fix a b c d
}
extrude Pilots from PilotSk distance 10 mm cut targets Case
box RailL size 1.2 mm (mcu_l - 4 mm) 2.3 mm corner (mcu_x - 1.4 mm, id - mcu_l + 2 mm, wall - 0.3 mm) join targets Case
box RailR size 1.2 mm (mcu_l - 4 mm) 2.3 mm corner (mcu_x + mcu_w + 0.2 mm, id - mcu_l + 2 mm, wall - 0.3 mm) join targets Case
sketch UsbSk on XZ offset (id - 1 mm) {
  slot u = centers (mcu_x + mcu_w/2 - 3 mm, -(wall + 2.6 mm)) (mcu_x + mcu_w/2 + 3 mm, -(wall + 2.6 mm)) width 4.4 mm
  fix u
}
extrude UsbSlot from UsbSk distance (wall + 2 mm) cut targets Case

// ==== Plate: rests on the walls, MX cutouts from one sketch pattern
sketch PlateSk on XY offset plate_z {
  rect rounded o = (-wall, -wall) (iw + wall, id + wall) radius 4 mm
  fix o
}
extrude PlateBody from PlateSk distance plate_t new -> body Plate
sketch CutoutSk on XY offset (plate_z - 1 mm) {
  rect k = (margin + (pitch - cutout)/2, margin + (pitch - cutout)/2) (margin + (pitch + cutout)/2, margin + (pitch + cutout)/2)
  circle s0 = (2 mm, 2 mm) diameter 2.4 mm
  circle s1 = (iw - 2 mm, 2 mm) diameter 2.4 mm
  circle s2 = (2 mm, id - 2 mm) diameter 2.4 mm
  circle s3 = (iw - 2 mm, id - 2 mm) diameter 2.4 mm
  fix k s0 s1 s2 s3
  pattern rectangular k count cols spacing pitch count2 rows spacing2 pitch
}
extrude Cutouts from CutoutSk distance (plate_t + 2 mm) cut targets Plate
if encoder > 0 {
  cylinder EncoderHole diameter 7.2 mm height (plate_t + 2 mm) base (margin + cols*pitch + enc_w/2, id - margin - pitch/2, plate_z - 1 mm) axis Z cut targets Plate
}

// ==== Keycaps, one per switch, as seated on the switches
for r from 0 to 3 {
  for c from 0 to 5 {
    if r < rows {
      if c < cols {
        sketch CapSk{r}{c} on XY offset cap_z {
          rect rounded o = (margin + c*pitch + 0.5 mm, margin + r*pitch + 0.5 mm) (margin + (c + 1)*pitch - 0.5 mm, margin + (r + 1)*pitch - 0.5 mm) radius 2 mm
          fix o
        }
        extrude CapBlock{r}{c} from CapSk{r}{c} distance cap_h taper (-12 deg) new -> body Cap{r}{c}
        sketch CapInSk{r}{c} on XY offset (cap_z - 0.3 mm) {
          rect o = (margin + c*pitch + 1.7 mm, margin + r*pitch + 1.7 mm) (margin + (c + 1)*pitch - 1.7 mm, margin + (r + 1)*pitch - 1.7 mm)
          fix o
        }
        extrude CapHollow{r}{c} from CapInSk{r}{c} distance (cap_h - 1.2 mm + 0.3 mm) taper (-12 deg) cut targets Cap{r}{c}
        cylinder CapStem{r}{c} diameter 5.5 mm height (cap_h - 1.2 mm - 0.8 mm + 0.3 mm) base (margin + (c + 0.5)*pitch, margin + (r + 0.5)*pitch, cap_z + 0.8 mm) axis Z join targets Cap{r}{c}
        box CapCrossA{r}{c} size 4.15 mm 1.35 mm 4.1 mm corner (margin + (c + 0.5)*pitch - 2.075 mm, margin + (r + 0.5)*pitch - 0.675 mm, cap_z + 0.7 mm) cut targets Cap{r}{c}
        box CapCrossB{r}{c} size 1.35 mm 4.15 mm 4.1 mm corner (margin + (c + 0.5)*pitch - 0.675 mm, margin + (r + 0.5)*pitch - 2.075 mm, cap_z + 0.7 mm) cut targets Cap{r}{c}
        color Cap{r}{c} "#e8e4da"
      }
    }
  }
}

// ==== Encoder knob on its shaft (6 mm D: a 4.5 mm flat)
if encoder > 0 {
  cylinder KnobBody diameter 18 mm height 14 mm base (margin + cols*pitch + enc_w/2, id - margin - pitch/2, plate_z + plate_t + 7 mm) axis Z new -> body Knob
  cylinder KnobBore diameter 6.1 mm height 10 mm base (margin + cols*pitch + enc_w/2, id - margin - pitch/2, plate_z + plate_t + 6.9 mm) axis Z cut targets Knob
  box KnobFlat size 6 mm 1.7 mm 10 mm corner (margin + cols*pitch + enc_w/2 - 3 mm, id - margin - pitch/2 + 1.5 mm, plate_z + plate_t + 6.9 mm) join targets Knob
  fillet KnobTop edges (margin + cols*pitch + enc_w/2 + 9 mm, id - margin - pitch/2, plate_z + plate_t + 21 mm) radius 2 mm body Knob
  color Knob "#2b2b2e"
}

color Case "#3c4a5c"
color Plate "#9aa3ad"
material Case, Plate PLA
