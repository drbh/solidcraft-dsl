// 79. Handheld game console you can build, in the original brick's proportions: a two-part case around
// a 2.4" TFT, an ESP32-S3, eight buttons, a speaker and a LiPo.
//
// Bill of materials
//   2.4" ILI9341 SPI TFT module   PCB 77.2 x 42.7 x 1.6 mm, glass 58 x 42.7 x 2.4 mm, 4 x 3 mm holes
//                                 on tft_hole_dx x tft_hole_dy; measure yours, these vary
//   ESP32-S3 SuperMini            22.5 x 18 mm, USB-C on its short edge (programming)
//   TP4056 USB-C charger with protection   28 x 17.5 mm (charging)
//   LiPo 103450 (1800 mAh)        10 x 34 x 50 mm
//   MAX98357A I2S amplifier       17.8 x 19.3 mm, and a 20 mm 8 ohm speaker, 4.5 mm deep
//   8 x 6x6x5 mm tactile switch   through-hole, legs on 6.5 x 4.5 mm
//   SS12D00G3 slide switch        power, between the cell and the charger output
//   16 mm thumbwheel potentiometer, 10 k   volume
//   6 x M3 x 25 self-tapping screw (case), 4 x M2 x 6 (button plate), 4 x M2.5 x 6 (TFT)
//
// It lies face up: X across the face (0 at the left edge), Y up it (0 at the bottom edge, H at the
// top where the cartridge goes in), Z out of the face (0 at the back, D at the face). The case
// splits at z = split_z.
//
// How it goes together. The Back is a tray. The cell sits between ribs in the middle, the charger
// between rails at the bottom with its USB-C in the bottom edge, the ESP32 between rails on the
// left with its USB-C in the left side, the amp beside the cell, and the volume pot on a cradle with
// its wheel through the right side. A housing at the top takes the Cartridge, a printed blank with
// a label recess that slides in from the top edge. The Front is a shell with the screen window,
// the key holes and the speaker grille. The TFT screws to four standoffs under the window, the
// speaker drops into a ring behind the grille, and the power switch pushes into a holder at the top
// edge with its knob through the slot. The ButtonPlate holds the eight switches in pockets (legs
// through holes for wires) and screws to four standoffs, so each key's flange is trapped between
// it and the face. The D-pad rocks on a post in the middle of its four switches. The Lens is a
// printed bezel glued into the face recess round the window. Six M3 screws close the case from the
// back.
//
// Print the Back floor down and the Front face down (the window, holes and grille are vertical;
// the standoffs and the holder rise straight up). Print the ButtonPlate flat, the keys flange down,
// the Lens face down and the Cartridge flat, all without supports. PLA or PETG, 0.2 mm layers.
design HandheldConsole units mm

// Case
param W = 90 mm
param H = 148 mm
param D = 32 mm
param split_z = 18 mm
param wall = 2 mm
param corner_r = 4 mm
param swoop_r = 22 mm        // the bottom-right corner
param fit = 0.25 mm          // clearance round keys, cartridge and lens

// Screen
param lens_w = 76 mm
param lens_h = 58 mm
param lens_top = 8 mm        // lens top edge below the case top
param lens_swoop = 10 mm
param win_w = 50 mm          // the TFT's active area plus a little
param win_h = 38 mm
param tft_w = 77.2 mm
param tft_h = 42.7 mm
param tft_t = 1.6 mm
param glass_w = 58 mm
param glass_t = 2.4 mm
param glass_dx = 4 mm        // the glass sits this far right of the PCB's centre (header on the left)
param tft_hole_dx = 72.2 mm
param tft_hole_dy = 37.6 mm

// Keys
param dpad_x = 21 mm
param dpad_y = 54 mm
param dpad_span = 23 mm
param dpad_arm = 7.5 mm
param ab_d = 10 mm
param ab_rise = 25 deg
param ab_gap = 16 mm
param pill_l = 12 mm
param pill_w = 4 mm
param pill_y = 33 mm
param key_rise = 2.5 mm      // how far keys stand above the face
param sw = 6 mm
param sw_h = 5 mm

// Speaker grille
param slot_l = 14 mm
param slot_w = 2 mm
param slot_pitch = 4 mm

// Screws
param m3_clear = 1.7 mm
param m3_pilot = 1.25 mm
param m2_pilot = 0.8 mm

let lens_x0 = (W - lens_w)/2
let lens_y1 = H - lens_top
let lens_y0 = lens_y1 - lens_h
let scr_x = W/2
let scr_y = lens_y0 + 27.5 mm
let pcb_x = scr_x - glass_dx
let face_in = D - wall
let glass_z = face_in - glass_t
let pcb_z = glass_z - tft_t
let plate_top = face_in - 1 mm - sw_h + 1 mm - 2 mm       // switch seats 1 mm down in the plate
let plate_t = 2.5 mm
let sw_z = plate_top - 1 mm
let key_z = sw_z + sw_h + 0.1 mm                          // key flanges just clear of the plungers
let a_x = 75 mm
let a_y = 58 mm
let b_x = a_x - ab_gap*cos(ab_rise)
let b_y = a_y - ab_gap*sin(ab_rise)
let pc = pill_l/2*cos(ab_rise)
let ps = pill_l/2*sin(ab_rise)

// ==== Case outline, used by both halves
sketch BackSk on XY {
  polyline closed o = (0,0) (W,0) (W,H) (0,H)
  fillet o.1.end radius swoop_r
  fillet o.2.end radius corner_r
  fillet o.3.end radius corner_r
  fillet o.4.end radius corner_r
  fix o
}
extrude BackBlock from BackSk distance split_z new -> body Back
fillet BackEdge edges (W/2, 0, 0) radius 3 mm body Back
shell BackHollow faces (W/2, H/2, split_z) thickness wall inside body Back

sketch FrontSk on XY offset split_z {
  polyline closed o = (0,0) (W,0) (W,H) (0,H)
  fillet o.1.end radius swoop_r
  fillet o.2.end radius corner_r
  fillet o.3.end radius corner_r
  fillet o.4.end radius corner_r
  fix o
}
extrude FrontBlock from FrontSk distance (D - split_z) new -> body Front
fillet FaceEdge edges (W/2, 0, D) radius 2.5 mm body Front
shell FrontHollow faces (W/2, H/2, split_z) thickness wall inside body Front

// ==== Back: cell, charger, ESP32, amp, volume pot, cartridge housing
box CellRibL size 1.2 mm 30 mm 3 mm corner (6.6 mm, 35 mm, wall - 0.2 mm) join targets Back
box CellRibR size 1.2 mm 30 mm 3 mm corner (58.3 mm, 35 mm, wall - 0.2 mm) join targets Back
box CellRibB size 46 mm 1.2 mm 2.8 mm corner (10 mm, 31.6 mm, wall - 0.2 mm) join targets Back
box CellRibT size 46 mm 1.2 mm 2.8 mm corner (10 mm, 67.3 mm, wall - 0.2 mm) join targets Back
box ChargerRailL size 1.2 mm 20 mm 1.8 mm corner (36.6 mm, 7 mm, wall - 0.2 mm) join targets Back
box ChargerRailR size 1.2 mm 20 mm 1.8 mm corner (55.8 mm, 7 mm, wall - 0.2 mm) join targets Back
box ChargerUsb size 9.6 mm (wall + 2 mm) 4 mm corner (42 mm, -1 mm, wall + 1.2 mm) cut targets Back
box EspRailB size 16 mm 1.2 mm 1.6 mm corner (7 mm, 81.6 mm, wall - 0.2 mm) join targets Back
box EspRailT size 16 mm 1.2 mm 1.6 mm corner (7 mm, 101.3 mm, wall - 0.2 mm) join targets Back
box EspUsb size (wall + 2 mm) 9.8 mm 4 mm corner (-1 mm, 87.1 mm, wall + 0.6 mm) cut targets Back
box PotCradle size 9 mm 9 mm (9 mm - wall + 0.2 mm) corner (79.5 mm, 95.5 mm, wall - 0.2 mm) join targets Back
box WheelSlot size 5 mm 17 mm 4.4 mm corner (86 mm, 91.5 mm, 12.8 mm) cut targets Back
box CartHousing size 63 mm 44.6 mm 10 mm corner (13.5 mm, 102 mm, 1.5 mm) join targets Back
box CartSlot size 59 mm 46 mm 7.5 mm corner (15.5 mm, 104 mm, 2.3 mm) cut targets Back

// ==== Front face: lens recess, screen window, key holes, grille
sketch LensSk on XY offset (D + 0.5 mm) {
  polyline closed o = (lens_x0, lens_y0) (lens_x0 + lens_w, lens_y0) (lens_x0 + lens_w, lens_y1) (lens_x0, lens_y1)
  fillet o.1.end radius lens_swoop
  fillet o.2.end radius 2 mm
  fillet o.3.end radius 2 mm
  fillet o.4.end radius 2 mm
  fix o
}
extrude LensRecess from LensSk distance 1.5 mm negative cut targets Front
sketch WindowSk on XY offset (D + 0.5 mm) {
  rect rounded w = (scr_x - win_w/2, scr_y - win_h/2) (scr_x + win_w/2, scr_y + win_h/2) radius 1.5 mm
  fix w
}
extrude WindowCut from WindowSk distance (wall + 1 mm) negative cut targets Front
sketch DPadHoleSk on XY offset (D + 0.5 mm) {
  polyline closed o = (dpad_x - dpad_arm/2 - fit, dpad_y - dpad_span/2 - fit) (dpad_x + dpad_arm/2 + fit, dpad_y - dpad_span/2 - fit) (dpad_x + dpad_arm/2 + fit, dpad_y - dpad_arm/2 - fit) (dpad_x + dpad_span/2 + fit, dpad_y - dpad_arm/2 - fit) (dpad_x + dpad_span/2 + fit, dpad_y + dpad_arm/2 + fit) (dpad_x + dpad_arm/2 + fit, dpad_y + dpad_arm/2 + fit) (dpad_x + dpad_arm/2 + fit, dpad_y + dpad_span/2 + fit) (dpad_x - dpad_arm/2 - fit, dpad_y + dpad_span/2 + fit) (dpad_x - dpad_arm/2 - fit, dpad_y + dpad_arm/2 + fit) (dpad_x - dpad_span/2 - fit, dpad_y + dpad_arm/2 + fit) (dpad_x - dpad_span/2 - fit, dpad_y - dpad_arm/2 - fit) (dpad_x - dpad_arm/2 - fit, dpad_y - dpad_arm/2 - fit)
  fix o
}
extrude DPadHole from DPadHoleSk distance (wall + 1 mm) negative cut targets Front
sketch KeyHoleSk on XY offset (D + 0.5 mm) {
  circle a = (a_x, a_y) radius (ab_d/2 + fit)
  circle b = (b_x, b_y) radius (ab_d/2 + fit)
  slot sel = centers (36 mm - pc, pill_y - ps) (36 mm + pc, pill_y + ps) width (pill_w + 2*fit)
  slot sta = centers (54 mm - pc, pill_y - ps) (54 mm + pc, pill_y + ps) width (pill_w + 2*fit)
  fix a b sel sta
}
extrude KeyHoles from KeyHoleSk distance (wall + 1 mm) negative cut targets Front
sketch SpeakerSk on XY offset (D + 0.5 mm) {
  slot s = centers (66 mm, 8 mm) (66 mm + slot_l*cos(45 deg), 8 mm + slot_l*sin(45 deg)) width slot_w
  fix s
  pattern rectangular s count 5 spacing slot_pitch dir (-0.7071, 0.7071)
}
extrude Grille from SpeakerSk distance (wall + 1 mm) negative cut targets Front

// ==== Front inside: TFT standoffs, speaker ring, power switch holder, button plate standoffs
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder TftPost{i}{j} radius 2.25 mm height (glass_t + 0.2 mm) base (pcb_x + (i - 0.5)*tft_hole_dx, scr_y + (j - 0.5)*tft_hole_dy, glass_z) axis Z join targets Front
    cylinder TftPilot{i}{j} radius 1.05 mm height 3.5 mm base (pcb_x + (i - 0.5)*tft_hole_dx, scr_y + (j - 0.5)*tft_hole_dy, glass_z - 0.5 mm) axis Z cut targets Front
  }
}
cylinder SpeakerRing radius 11.5 mm height 4.2 mm base (72 mm, 18 mm, face_in - 4 mm) axis Z join targets Front
cylinder SpeakerSeat radius 10.2 mm height 4 mm base (72 mm, 18 mm, face_in - 4.1 mm) axis Z cut targets Front
box SwitchHolder size 11.6 mm 7.5 mm (face_in - 19 mm + 0.2 mm) corner (5 mm, 139 mm, 19 mm) join targets Front
box SwitchCavity size 9.2 mm 4.2 mm 5.3 mm corner (6.2 mm, 140.6 mm, 18.5 mm) cut targets Front
box KnobSlot size 4.5 mm 6 mm 2.5 mm corner (9 mm, 143 mm, 20.6 mm) cut targets Front
sketch PlatePostSk on XY offset (plate_top) {
  circle a = (10 mm, 24 mm) radius 2.5 mm
  circle b = (46 mm, 22.5 mm) radius 2.5 mm
  circle c = (10 mm, 68 mm) radius 2.5 mm
  circle d = (80 mm, 68 mm) radius 2.5 mm
  fix a b c d
}
extrude PlatePosts from PlatePostSk distance (face_in - plate_top + 0.2 mm) join targets Front
sketch PlatePilotSk on XY offset (plate_top - 0.5 mm) {
  circle a = (10 mm, 24 mm) radius m2_pilot
  circle b = (46 mm, 22.5 mm) radius m2_pilot
  circle c = (10 mm, 68 mm) radius m2_pilot
  circle d = (80 mm, 68 mm) radius m2_pilot
  fix a b c d
}
extrude PlatePilots from PlatePilotSk distance 5 mm cut targets Front

// ==== Case screws: six through the Back into bosses on the Front
sketch BossSk on XY offset (wall - 0.2 mm) {
  circle a = (7.5 mm, 7.5 mm) radius 4 mm
  circle b = (81 mm, 29 mm) radius 4 mm
  circle c = (6.5 mm, 76 mm) radius 4 mm
  circle d = (83.5 mm, 76 mm) radius 4 mm
  circle e = (7.5 mm, 135.5 mm) radius 4 mm
  circle f = (83.5 mm, 118 mm) radius 4 mm
  fix a b c d e f
}
extrude BackBosses from BossSk distance (split_z - wall + 0.2 mm) join targets Back
sketch FrontBossSk on XY offset split_z {
  circle a = (7.5 mm, 7.5 mm) radius 3 mm
  circle b = (81 mm, 29 mm) radius 3 mm
  circle c = (6.5 mm, 76 mm) radius 3 mm
  circle d = (83.5 mm, 76 mm) radius 3 mm
  circle e = (7.5 mm, 135.5 mm) radius 3 mm
  circle f = (83.5 mm, 118 mm) radius 3 mm
  fix a b c d e f
}
extrude FrontBosses from FrontBossSk distance (face_in - split_z + 0.2 mm) join targets Front
sketch ScrewSk on XY offset (-0.5 mm) {
  circle a = (7.5 mm, 7.5 mm) radius m3_clear
  circle b = (81 mm, 29 mm) radius m3_clear
  circle c = (6.5 mm, 76 mm) radius m3_clear
  circle d = (83.5 mm, 76 mm) radius m3_clear
  circle e = (7.5 mm, 135.5 mm) radius m3_clear
  circle f = (83.5 mm, 118 mm) radius m3_clear
  fix a b c d e f
}
extrude ScrewHoles from ScrewSk distance (split_z + 1 mm) cut targets Back
sketch HeadSk on XY offset (-0.5 mm) {
  circle a = (7.5 mm, 7.5 mm) radius 3.1 mm
  circle b = (81 mm, 29 mm) radius 3.1 mm
  circle c = (6.5 mm, 76 mm) radius 3.1 mm
  circle d = (83.5 mm, 76 mm) radius 3.1 mm
  circle e = (7.5 mm, 135.5 mm) radius 3.1 mm
  circle f = (83.5 mm, 118 mm) radius 3.1 mm
  fix a b c d e f
}
extrude ScrewHeads from HeadSk distance 3.5 mm cut targets Back
sketch PilotSk on XY offset (split_z - 0.5 mm) {
  circle a = (7.5 mm, 7.5 mm) radius m3_pilot
  circle b = (81 mm, 29 mm) radius m3_pilot
  circle c = (6.5 mm, 76 mm) radius m3_pilot
  circle d = (83.5 mm, 76 mm) radius m3_pilot
  circle e = (7.5 mm, 135.5 mm) radius m3_pilot
  circle f = (83.5 mm, 118 mm) radius m3_pilot
  fix a b c d e f
}
extrude Pilots from PilotSk distance 10.5 mm cut targets Front

// ==== Button plate: eight switch pockets with leg holes, the D-pad's pivot, screw holes
sketch PlateSk on XY offset (plate_top - plate_t) {
  rect rounded p = (6 mm, 19 mm) (84 mm, 72 mm) radius 3 mm
  fix p
}
extrude PlateBlock from PlateSk distance plate_t new -> body ButtonPlate
sketch PlateHoleSk on XY offset (plate_top + 0.5 mm) {
  circle a = (10 mm, 24 mm) radius 1.2 mm
  circle b = (46 mm, 22.5 mm) radius 1.2 mm
  circle c = (10 mm, 68 mm) radius 1.2 mm
  circle d = (80 mm, 68 mm) radius 1.2 mm
  circle e = (81 mm, 29 mm) radius 3.5 mm
  fix a b c d e
}
extrude PlateHoles from PlateHoleSk distance (plate_t + 1 mm) negative cut targets ButtonPlate
sketch PocketSk on XY offset (plate_top + 0.5 mm) {
  rect p0 = (dpad_x + 7.5 mm - 4.15 mm, dpad_y - 3.15 mm) (dpad_x + 7.5 mm + 4.15 mm, dpad_y + 3.15 mm)
  rect p1 = (dpad_x - 7.5 mm - 4.15 mm, dpad_y - 3.15 mm) (dpad_x - 7.5 mm + 4.15 mm, dpad_y + 3.15 mm)
  rect p2 = (dpad_x - 4.15 mm, dpad_y + 7.5 mm - 3.15 mm) (dpad_x + 4.15 mm, dpad_y + 7.5 mm + 3.15 mm)
  rect p3 = (dpad_x - 4.15 mm, dpad_y - 7.5 mm - 3.15 mm) (dpad_x + 4.15 mm, dpad_y - 7.5 mm + 3.15 mm)
  rect p4 = (a_x - 4.15 mm, a_y - 3.15 mm) (a_x + 4.15 mm, a_y + 3.15 mm)
  rect p5 = (b_x - 4.15 mm, b_y - 3.15 mm) (b_x + 4.15 mm, b_y + 3.15 mm)
  rect p6 = (36 mm - 4.15 mm, pill_y - 3.15 mm) (36 mm + 4.15 mm, pill_y + 3.15 mm)
  rect p7 = (54 mm - 4.15 mm, pill_y - 3.15 mm) (54 mm + 4.15 mm, pill_y + 3.15 mm)
  fix p0 p1 p2 p3 p4 p5 p6 p7
}
extrude Pockets from PocketSk distance 1.5 mm negative cut targets ButtonPlate
sketch LegSk on XY offset (plate_top + 0.5 mm) {
  circle a = (dpad_x + 7.5 mm - 3.25 mm, dpad_y - 2.25 mm) diameter 1.3 mm
  circle b = (dpad_x + 7.5 mm + 3.25 mm, dpad_y - 2.25 mm) diameter 1.3 mm
  circle c = (dpad_x + 7.5 mm - 3.25 mm, dpad_y + 2.25 mm) diameter 1.3 mm
  circle d = (dpad_x + 7.5 mm + 3.25 mm, dpad_y + 2.25 mm) diameter 1.3 mm
  circle e = (dpad_x - 7.5 mm - 3.25 mm, dpad_y - 2.25 mm) diameter 1.3 mm
  circle f = (dpad_x - 7.5 mm + 3.25 mm, dpad_y - 2.25 mm) diameter 1.3 mm
  circle g = (dpad_x - 7.5 mm - 3.25 mm, dpad_y + 2.25 mm) diameter 1.3 mm
  circle h = (dpad_x - 7.5 mm + 3.25 mm, dpad_y + 2.25 mm) diameter 1.3 mm
  circle i = (dpad_x - 3.25 mm, dpad_y + 7.5 mm - 2.25 mm) diameter 1.3 mm
  circle j = (dpad_x + 3.25 mm, dpad_y + 7.5 mm - 2.25 mm) diameter 1.3 mm
  circle k = (dpad_x - 3.25 mm, dpad_y + 7.5 mm + 2.25 mm) diameter 1.3 mm
  circle l = (dpad_x + 3.25 mm, dpad_y + 7.5 mm + 2.25 mm) diameter 1.3 mm
  circle m = (dpad_x - 3.25 mm, dpad_y - 7.5 mm - 2.25 mm) diameter 1.3 mm
  circle n = (dpad_x + 3.25 mm, dpad_y - 7.5 mm - 2.25 mm) diameter 1.3 mm
  circle o = (dpad_x - 3.25 mm, dpad_y - 7.5 mm + 2.25 mm) diameter 1.3 mm
  circle p = (dpad_x + 3.25 mm, dpad_y - 7.5 mm + 2.25 mm) diameter 1.3 mm
  fix a b c d e f g h i j k l m n o p
}
extrude DPadLegs from LegSk distance (plate_t + 1 mm) negative cut targets ButtonPlate
sketch KeyLegSk on XY offset (plate_top + 0.5 mm) {
  circle a = (a_x - 3.25 mm, a_y - 2.25 mm) diameter 1.3 mm
  circle b = (a_x + 3.25 mm, a_y - 2.25 mm) diameter 1.3 mm
  circle c = (a_x - 3.25 mm, a_y + 2.25 mm) diameter 1.3 mm
  circle d = (a_x + 3.25 mm, a_y + 2.25 mm) diameter 1.3 mm
  circle e = (b_x - 3.25 mm, b_y - 2.25 mm) diameter 1.3 mm
  circle f = (b_x + 3.25 mm, b_y - 2.25 mm) diameter 1.3 mm
  circle g = (b_x - 3.25 mm, b_y + 2.25 mm) diameter 1.3 mm
  circle h = (b_x + 3.25 mm, b_y + 2.25 mm) diameter 1.3 mm
  circle i = (36 mm - 3.25 mm, pill_y - 2.25 mm) diameter 1.3 mm
  circle j = (36 mm + 3.25 mm, pill_y - 2.25 mm) diameter 1.3 mm
  circle k = (36 mm - 3.25 mm, pill_y + 2.25 mm) diameter 1.3 mm
  circle l = (36 mm + 3.25 mm, pill_y + 2.25 mm) diameter 1.3 mm
  circle m = (54 mm - 3.25 mm, pill_y - 2.25 mm) diameter 1.3 mm
  circle n = (54 mm + 3.25 mm, pill_y - 2.25 mm) diameter 1.3 mm
  circle o = (54 mm - 3.25 mm, pill_y + 2.25 mm) diameter 1.3 mm
  circle p = (54 mm + 3.25 mm, pill_y + 2.25 mm) diameter 1.3 mm
  fix a b c d e f g h i j k l m n o p
}
extrude KeyLegs from KeyLegSk distance (plate_t + 1 mm) negative cut targets ButtonPlate
cylinder Pivot radius 2 mm height (key_z - plate_top + 0.2 mm) base (dpad_x, dpad_y, plate_top - 0.2 mm) axis Z join targets ButtonPlate

// ==== Keys: each a flange under the face and a cap through it
cylinder DPadFlange radius 13 mm height (face_in - key_z - 0.25 mm) base (dpad_x, dpad_y, key_z) axis Z new -> body DPad
sketch DPadSk on XY offset (face_in - 0.4 mm) {
  polyline closed o = (dpad_x - dpad_arm/2, dpad_y - dpad_span/2) (dpad_x + dpad_arm/2, dpad_y - dpad_span/2) (dpad_x + dpad_arm/2, dpad_y - dpad_arm/2) (dpad_x + dpad_span/2, dpad_y - dpad_arm/2) (dpad_x + dpad_span/2, dpad_y + dpad_arm/2) (dpad_x + dpad_arm/2, dpad_y + dpad_arm/2) (dpad_x + dpad_arm/2, dpad_y + dpad_span/2) (dpad_x - dpad_arm/2, dpad_y + dpad_span/2) (dpad_x - dpad_arm/2, dpad_y + dpad_arm/2) (dpad_x - dpad_span/2, dpad_y + dpad_arm/2) (dpad_x - dpad_span/2, dpad_y - dpad_arm/2) (dpad_x - dpad_arm/2, dpad_y - dpad_arm/2)
  fix o
}
extrude DPadCap from DPadSk distance (wall + key_rise + 0.4 mm) join targets DPad
cylinder DPadDimple radius 2.5 mm height 1 mm base (dpad_x, dpad_y, D + key_rise - 0.6 mm) axis Z cut targets DPad

cylinder AFlange radius (ab_d/2 + 1.5 mm) height (face_in - key_z - 0.25 mm) base (a_x, a_y, key_z) axis Z new -> body A
cylinder ACap radius (ab_d/2) height (wall + key_rise + 0.4 mm) base (a_x, a_y, face_in - 0.4 mm) axis Z join targets A
fillet ATop edges (a_x + ab_d/2, a_y, D + key_rise) radius 1 mm body A
cylinder BFlange radius (ab_d/2 + 1.5 mm) height (face_in - key_z - 0.25 mm) base (b_x, b_y, key_z) axis Z new -> body B
cylinder BCap radius (ab_d/2) height (wall + key_rise + 0.4 mm) base (b_x, b_y, face_in - 0.4 mm) axis Z join targets B
fillet BTop edges (b_x + ab_d/2, b_y, D + key_rise) radius 1 mm body B

sketch SelectFlangeSk on XY offset key_z {
  slot s = centers (36 mm - pc, pill_y - ps) (36 mm + pc, pill_y + ps) width (pill_w + 3 mm)
  fix s
}
extrude SelectFlange from SelectFlangeSk distance (face_in - key_z - 0.25 mm) new -> body Select
sketch SelectSk on XY offset (face_in - 0.4 mm) {
  slot s = centers (36 mm - pc, pill_y - ps) (36 mm + pc, pill_y + ps) width pill_w
  fix s
}
extrude SelectCap from SelectSk distance (wall + 1.5 mm + 0.4 mm) join targets Select
sketch StartFlangeSk on XY offset key_z {
  slot s = centers (54 mm - pc, pill_y - ps) (54 mm + pc, pill_y + ps) width (pill_w + 3 mm)
  fix s
}
extrude StartFlange from StartFlangeSk distance (face_in - key_z - 0.25 mm) new -> body Start
sketch StartSk on XY offset (face_in - 0.4 mm) {
  slot s = centers (54 mm - pc, pill_y - ps) (54 mm + pc, pill_y + ps) width pill_w
  fix s
}
extrude StartCap from StartSk distance (wall + 1.5 mm + 0.4 mm) join targets Start

// ==== Lens: a printed bezel for the face recess, its window over the screen
sketch LensPlateSk on XY offset (D - 1 mm) {
  polyline closed o = (lens_x0 + fit, lens_y0 + fit) (lens_x0 + lens_w - fit, lens_y0 + fit) (lens_x0 + lens_w - fit, lens_y1 - fit) (lens_x0 + fit, lens_y1 - fit)
  fillet o.1.end radius (lens_swoop - fit)
  fillet o.2.end radius 1.8 mm
  fillet o.3.end radius 1.8 mm
  fillet o.4.end radius 1.8 mm
  fix o
}
extrude LensPlate from LensPlateSk distance 1 mm new -> body Lens
extrude LensWindow from WindowSk distance 2 mm negative cut targets Lens

// ==== Cartridge: a printed blank that slides into the top slot, with a label recess and grip
box CartBody size (59 mm - 2*fit) 56 mm (7.5 mm - 2*fit) corner (15.5 mm + fit, H - 44 mm, 2.3 mm + fit) new -> body Cartridge
box CartGrip size (59 mm - 2*fit) 3 mm 1.2 mm corner (15.5 mm + fit, H + 6 mm, 9.5 mm - fit) join targets Cartridge
box CartLabel size 46 mm 30 mm 1 mm corner (22 mm, H - 26 mm, 2.3 mm + fit - 0.4 mm) cut targets Cartridge

// ==== The bought parts, placed (not printed)
box TftPcb size tft_w tft_h tft_t corner (pcb_x - tft_w/2, scr_y - tft_h/2, pcb_z) new -> body Tft
box TftGlass size glass_w tft_h glass_t corner (scr_x - glass_w/2, scr_y - tft_h/2, glass_z - 0.1 mm) join targets Tft
box EspPcb size 22.5 mm 18 mm 1 mm corner (2.3 mm, 83 mm, wall) new -> body Esp32
box EspPort size 7.4 mm 8.9 mm 3.2 mm corner (1.7 mm, 87.55 mm, wall + 0.9 mm) join targets Esp32
box ChargerPcb size 17.5 mm 28 mm 1.6 mm corner (38 mm, 3.1 mm, wall) new -> body Charger
box ChargerPort size 8.9 mm 7.3 mm 3.2 mm corner (42.3 mm, 2.5 mm, wall + 1.5 mm) join targets Charger
box CellBody size 50 mm 34 mm 10 mm corner (8 mm, 33 mm, wall) new -> body Battery
box AmpPcb size 17.8 mm 19.3 mm 1.6 mm corner (62 mm, 40 mm, wall) new -> body Amp
cylinder SpeakerBody radius 10 mm height 4.5 mm base (72 mm, 18 mm, face_in - 4.6 mm) axis Z new -> body Speaker
box PotBody size 8.6 mm 9 mm 4 mm corner (79 mm, 95.5 mm, 9 mm) new -> body VolumePot
cylinder PotWheel radius 8 mm height 3 mm base (84 mm, 100 mm, 12.8 mm) axis Z join targets VolumePot
box SlideBody size 8.6 mm 3.6 mm 3.5 mm corner (6.5 mm, 140.9 mm, 19 mm) new -> body PowerSwitch
box SlideKnob size 2 mm 4 mm 1.5 mm corner (10.3 mm, 144.4 mm, 21.1 mm) join targets PowerSwitch
for k from 0 to 3 {
  box DSwBody{k} size sw sw 3.5 mm corner (dpad_x + 7.5 mm*cos(k*90 deg) - sw/2, dpad_y + 7.5 mm*sin(k*90 deg) - sw/2, sw_z) new -> body DSwitch{k}
  cylinder DSwPlunger{k} radius 1.75 mm height (sw_h - 3.4 mm) base (dpad_x + 7.5 mm*cos(k*90 deg), dpad_y + 7.5 mm*sin(k*90 deg), sw_z + 3.4 mm) axis Z join targets DSwitch{k}
}
box ASwBody size sw sw 3.5 mm corner (a_x - sw/2, a_y - sw/2, sw_z) new -> body ASwitch
box BSwBody size sw sw 3.5 mm corner (b_x - sw/2, b_y - sw/2, sw_z) new -> body BSwitch
box SelSwBody size sw sw 3.5 mm corner (36 mm - sw/2, pill_y - sw/2, sw_z) new -> body SelectSwitch
box StaSwBody size sw sw 3.5 mm corner (54 mm - sw/2, pill_y - sw/2, sw_z) new -> body StartSwitch

// ==== Finish
color Back, Front "#c5c3bd"
color Lens "#4d4d5a"
color ButtonPlate "#e6e2d6"
color DPad "#262628"
color A, B "#8a1c4a"
color Select, Start "#7f7f88"
color Cartridge "#9e9e9e"
color Tft "#1d2330"
color Esp32, Charger, Amp "#2f6b3a"
color Battery "#9aa4b0"
color Speaker, VolumePot, PowerSwitch "#3a3a3a"
color DSwitch0, DSwitch1, DSwitch2, DSwitch3, ASwitch, BSwitch, SelectSwitch, StartSwitch "#303030"
material Back, Front, Lens, ButtonPlate, DPad, A, B, Select, Start, Cartridge PLA
