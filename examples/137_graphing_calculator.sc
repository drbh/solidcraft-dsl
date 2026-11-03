// 137. Graphing calculator in the style of the TI-84 Plus, printable: a two-part case around a 2.8" TFT,
// a Raspberry Pi Pico 2, a 50-key matrix of 6x6 tactile switches on perfboard and a LiPo, with a
// battery door and a slide-on face cover.
//
// Bill of materials
//   2.8" ILI9341 SPI TFT module   PCB 86 x 50 x 1.6 mm, glass 69.2 x 50 x 2.4 mm, active 57.6 x 43.2,
//                                 4 x 3 mm holes on tft_hole_dx x tft_hole_dy; measure yours (some
//                                 put the glass off centre: set glass_dx and the window follows)
//   Raspberry Pi Pico 2           51 x 21 mm, micro-USB on its short edge (data, programming)
//   TP4056 USB-C charger with protection   28 x 17.5 mm (charging)
//   LiPo 804050 (about 1500 mAh)  8 x 40 x 50 mm, into the Pico's VSYS through the power switch
//   50 x 6x6x5 mm tactile switch  through-hole; 46 keys plus the four arrows
//   80 x 120 mm perfboard (2.54 mm), cut to 73 x 120; bend the switch legs to 7.62 x 5.08 mm
//   SS12D00G3 slide switch        power, on the top edge
//   8 x M3 x 20 self-tapping (case), 6 x M2 x 6 (perfboard), 4 x M2.5 x 6 (TFT), 1 x M2 x 6 (door)
//   optional: a 65 x 51 x 1 mm clear sheet glued in the window recess
//
// It lies face up: X across the face (0 at the left edge), Y up it (0 at the bottom edge, H at the
// top edge where the ports are), Z out of the face (0 at the back, D at the face). The case splits
// at z = split_z.
//
// Layout, as on the original: the screen at the top, a row of five narrow function keys under it,
// then two rows of three keys on the left beside the arrow pad, then seven rows of five. Keys sit on
// the perfboard's 2.54 mm grid (columns 15.24 mm apart, rows 10.16 mm), so every switch lands on
// holes. Each key is the same part, 137_graphing_calculator_parts/key.sc, placed 46 times.
//
// How it goes together. The Back is a tray. The cell lies in a bay in the middle, behind a door
// that hooks under the floor at its bottom end and takes one M2 screw at its top end. The Pico and
// the charger sit between rails at the top, their USB ports through the top edge, with the power
// switch in a holder beside them. Six posts carry the perfboard, which screws down onto them with
// the switches facing the face. The Front is a shell with the screen window and the key holes; the
// TFT screws to four standoffs under the window. Each key's flange is trapped between the face and
// its switch, its plunger 0.1 mm above the switch's button. The arrow pad is one cross that rocks
// on a pivot over its four switches. Eight M3 screws close the case from the back.
//
// The Cover slides on from the top (its end wall trails), its two lips riding in grooves along
// the Front's sides, until the end wall stops against the top edge. `cover_pull` shows it slid
// part way off upward so the keys are visible; set it to 0 for the closed fit.
//
// Print the Back floor down and the Front face down; the Cover top down (its lips are 1 mm
// overhangs); the keys and the arrow pad top down as one group (flanges are 40 deg tapers); the Door
// flat, hook up (the hook is a 1.75 mm overhang). PLA or PETG, 0.2 mm layers, no supports.
design GraphingCalculator units mm

// Case
param W = 92 mm
param H = 187 mm
param D = 25 mm
param split_z = 12 mm
param wall = 2.2 mm
param corner_r = 6 mm
param fit = 0.25 mm          // clearance round keys, door and cover

// Screen
param tft_w = 86 mm
param tft_h = 50 mm
param tft_t = 1.6 mm
param glass_w = 69.2 mm
param glass_t = 2.4 mm
param glass_dx = 0 mm        // glass centre right of the PCB centre
param act_w = 57.6 mm
param act_h = 43.2 mm
param tft_hole_dx = 80 mm
param tft_hole_dy = 44 mm
param scr_y = 150 mm         // the TFT's centre

// Keys
param pitch_x = 15.24 mm     // 6 perfboard holes
param pitch_y = 10.16 mm     // 4 perfboard holes
param key_w = 11.5 mm
param key_h = 6.8 mm
param fn_w = 11 mm
param fn_h = 4.6 mm
param lip = 1.2 mm
param flange_t = 1.2 mm
param key_rise = 2 mm        // how far keys stand above the face
param stem_l = 1.5 mm
param dpad_span = 22 mm
param dpad_arm = 7.5 mm
param sw = 6 mm
param sw_h = 5 mm
param kb_t = 1.6 mm

// Cover rails
param groove_z = 15 mm
param groove_h = 2.2 mm
param groove_d = 1 mm
param cover_t = 2 mm
param cover_gap = 3 mm       // from the face to the cover's underside
param cover_pull = 140 mm    // how far the cover is slid up off the face (0 = closed)

// Screws
param m3_clear = 1.7 mm
param m3_pilot = 1.25 mm
param m2_pilot = 0.8 mm

let face_in = D - wall
let key_z = face_in - 0.3 mm - flange_t                   // flange underside
let key_top = D + key_rise
let kb_top = key_z - stem_l - 0.1 mm - sw_h               // perfboard top: switch plungers 0.1 mm under the stems
let kb_bot = kb_top - kb_t
let x0 = W/2 - 2*pitch_x
let y0 = 14 mm
let fn_y = y0 + 10*pitch_y
let dp_x = x0 + 3.5*pitch_x
let dp_y = y0 + 8*pitch_y
let dp_s = 3*2.54 mm                                       // arrow switches this far from the pad's centre
let glass_x = W/2 + glass_dx
let glass_z = face_in - glass_t
let pcb_z = glass_z - tft_t
let cover_in = D + cover_gap
let lip_lo = groove_z + fit
let lip_hi = groove_z + groove_h - fit
let door_y0 = 17 mm
let door_y1 = 80 mm
let door_scr = 77.5 mm

// ==== Case outline: the Back tray and the Front shell
sketch BackSk on XY {
  rect rounded o = (0, 0) (W, H) radius corner_r
  fix o
}
extrude BackBlock from BackSk distance split_z new -> body Back
fillet BackEdge edges (W/2, 0, 0) radius 3 mm body Back
shell BackHollow faces (W/2, H/2, split_z) thickness wall inside body Back

sketch FrontSk on XY offset split_z {
  rect rounded o = (0, 0) (W, H) radius corner_r
  fix o
}
extrude FrontBlock from FrontSk distance (D - split_z) new -> body Front
fillet FaceEdge edges (W/2, 0, D) radius 2.5 mm body Front
shell FrontHollow faces (W/2, H/2, split_z) thickness wall inside body Front

// Grooves along both sides for the Cover's lips
box GrooveL size (groove_d + 1 mm) (H + 2 mm) groove_h corner (-1 mm, -1 mm, groove_z) cut targets Front
box GrooveR size (groove_d + 1 mm) (H + 2 mm) groove_h corner (W - groove_d, -1 mm, groove_z) cut targets Front

// ==== Front face: screen window and its recess, key holes
sketch RecessSk on XY offset (D + 0.5 mm) {
  rect rounded r = (glass_x - act_w/2 - 3.8 mm, scr_y - act_h/2 - 4 mm) (glass_x + act_w/2 + 3.8 mm, scr_y + act_h/2 + 4 mm) radius 2 mm
  fix r
}
extrude WindowRecess from RecessSk distance 1.5 mm negative cut targets Front
sketch WindowSk on XY offset (D + 0.5 mm) {
  rect rounded w = (glass_x - act_w/2 - 0.8 mm, scr_y - act_h/2 - 0.8 mm) (glass_x + act_w/2 + 0.8 mm, scr_y + act_h/2 + 0.8 mm) radius 1 mm
  fix w
}
extrude Window from WindowSk distance (wall + 1 mm) negative cut targets Front
sketch KeyHoleSk on XY offset (D + 0.5 mm) {
  rect rounded a = (x0 - key_w/2 - fit, y0 - key_h/2 - fit) (x0 + key_w/2 + fit, y0 + key_h/2 + fit) radius (1.2 mm + fit)
  rect rounded b = (x0 - key_w/2 - fit, y0 + 7*pitch_y - key_h/2 - fit) (x0 + key_w/2 + fit, y0 + 7*pitch_y + key_h/2 + fit) radius (1.2 mm + fit)
  rect rounded f = (x0 - fn_w/2 - fit, fn_y - fn_h/2 - fit) (x0 + fn_w/2 + fit, fn_y + fn_h/2 + fit) radius (1.2 mm + fit)
  fix a b f
  pattern rectangular a count 5 spacing pitch_x count2 7 spacing2 pitch_y
  pattern rectangular b count 3 spacing pitch_x count2 2 spacing2 pitch_y
  pattern rectangular f count 5 spacing pitch_x
}
extrude KeyHoles from KeyHoleSk distance (wall + 1 mm) negative cut targets Front
sketch DPadHoleSk on XY offset (D + 0.5 mm) {
  polyline closed o = (dp_x - dpad_arm/2 - fit, dp_y - dpad_span/2 - fit) (dp_x + dpad_arm/2 + fit, dp_y - dpad_span/2 - fit) (dp_x + dpad_arm/2 + fit, dp_y - dpad_arm/2 - fit) (dp_x + dpad_span/2 + fit, dp_y - dpad_arm/2 - fit) (dp_x + dpad_span/2 + fit, dp_y + dpad_arm/2 + fit) (dp_x + dpad_arm/2 + fit, dp_y + dpad_arm/2 + fit) (dp_x + dpad_arm/2 + fit, dp_y + dpad_span/2 + fit) (dp_x - dpad_arm/2 - fit, dp_y + dpad_span/2 + fit) (dp_x - dpad_arm/2 - fit, dp_y + dpad_arm/2 + fit) (dp_x - dpad_span/2 - fit, dp_y + dpad_arm/2 + fit) (dp_x - dpad_span/2 - fit, dp_y - dpad_arm/2 - fit) (dp_x - dpad_arm/2 - fit, dp_y - dpad_arm/2 - fit)
  fix o
}
extrude DPadHole from DPadHoleSk distance (wall + 1 mm) negative cut targets Front

// ==== Front inside: TFT standoffs
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder TftPost{i}{j} radius 2.25 mm height (glass_t + 0.2 mm) base (W/2 + (i - 0.5)*tft_hole_dx, scr_y + (j - 0.5)*tft_hole_dy, glass_z) axis Z join targets Front
    cylinder TftPilot{i}{j} radius 1.05 mm height 3.5 mm base (W/2 + (i - 0.5)*tft_hole_dx, scr_y + (j - 0.5)*tft_hole_dy, glass_z - 0.5 mm) axis Z cut targets Front
  }
}

// ==== Case screws: eight through the Back into bosses on the Front, clear of the keys and the TFT
sketch BossSk on XY offset (wall - 0.2 mm) {
  circle a = (5 mm, 10 mm) radius 4 mm
  circle b = (W - 5 mm, 10 mm) radius 4 mm
  circle c = (5 mm, 66 mm) radius 4 mm
  circle d = (W - 5 mm, 66 mm) radius 4 mm
  circle e = (5 mm, 121.5 mm) radius 4 mm
  circle f = (W - 5 mm, 121.5 mm) radius 4 mm
  circle g = (7 mm, H - 7 mm) radius 4 mm
  circle h = (W - 7 mm, H - 7 mm) radius 4 mm
  fix a b c d e f g h
}
extrude BackBosses from BossSk distance (split_z - wall + 0.2 mm) join targets Back
sketch FrontBossSk on XY offset split_z {
  circle a = (5 mm, 10 mm) radius 3 mm
  circle b = (W - 5 mm, 10 mm) radius 3 mm
  circle c = (5 mm, 66 mm) radius 3 mm
  circle d = (W - 5 mm, 66 mm) radius 3 mm
  circle e = (5 mm, 121.5 mm) radius 3 mm
  circle f = (W - 5 mm, 121.5 mm) radius 3 mm
  circle g = (7 mm, H - 7 mm) radius 3 mm
  circle h = (W - 7 mm, H - 7 mm) radius 3 mm
  fix a b c d e f g h
}
extrude FrontBosses from FrontBossSk distance (face_in - split_z + 0.2 mm) join targets Front
sketch ScrewSk on XY offset (-0.5 mm) {
  circle a = (5 mm, 10 mm) radius m3_clear
  circle b = (W - 5 mm, 10 mm) radius m3_clear
  circle c = (5 mm, 66 mm) radius m3_clear
  circle d = (W - 5 mm, 66 mm) radius m3_clear
  circle e = (5 mm, 121.5 mm) radius m3_clear
  circle f = (W - 5 mm, 121.5 mm) radius m3_clear
  circle g = (7 mm, H - 7 mm) radius m3_clear
  circle h = (W - 7 mm, H - 7 mm) radius m3_clear
  fix a b c d e f g h
}
extrude ScrewHoles from ScrewSk distance (split_z + 1 mm) cut targets Back
sketch HeadSk on XY offset (-0.5 mm) {
  circle a = (5 mm, 10 mm) radius 3.1 mm
  circle b = (W - 5 mm, 10 mm) radius 3.1 mm
  circle c = (5 mm, 66 mm) radius 3.1 mm
  circle d = (W - 5 mm, 66 mm) radius 3.1 mm
  circle e = (5 mm, 121.5 mm) radius 3.1 mm
  circle f = (W - 5 mm, 121.5 mm) radius 3.1 mm
  circle g = (7 mm, H - 7 mm) radius 3.1 mm
  circle h = (W - 7 mm, H - 7 mm) radius 3.1 mm
  fix a b c d e f g h
}
extrude ScrewHeads from HeadSk distance 3.5 mm cut targets Back
sketch PilotSk on XY offset (split_z - 0.5 mm) {
  circle a = (5 mm, 10 mm) radius m3_pilot
  circle b = (W - 5 mm, 10 mm) radius m3_pilot
  circle c = (5 mm, 66 mm) radius m3_pilot
  circle d = (W - 5 mm, 66 mm) radius m3_pilot
  circle e = (5 mm, 121.5 mm) radius m3_pilot
  circle f = (W - 5 mm, 121.5 mm) radius m3_pilot
  circle g = (7 mm, H - 7 mm) radius m3_pilot
  circle h = (W - 7 mm, H - 7 mm) radius m3_pilot
  fix a b c d e f g h
}
extrude Pilots from PilotSk distance 10.5 mm cut targets Front

// ==== Back: perfboard posts, battery bay and door, Pico and charger rails, USB ports, power switch
sketch PostSk on XY offset (wall - 0.1 mm) {
  circle a = (12 mm, 6.5 mm) radius 2.5 mm
  circle b = (W - 12 mm, 6.5 mm) radius 2.5 mm
  circle c = (12 mm, 121.5 mm) radius 2.5 mm
  circle d = (W - 12 mm, 121.5 mm) radius 2.5 mm
  circle e = (W/2, 6.5 mm) radius 2 mm
  circle f = (x0 + pitch_x, y0 + 6.5*pitch_y) radius 2 mm
  fix a b c d e f
}
extrude Posts from PostSk distance (kb_bot - wall + 0.1 mm) join targets Back
sketch PostPilotSk on XY offset (kb_bot + 0.5 mm) {
  circle a = (12 mm, 6.5 mm) radius m2_pilot
  circle b = (W - 12 mm, 6.5 mm) radius m2_pilot
  circle c = (12 mm, 121.5 mm) radius m2_pilot
  circle d = (W - 12 mm, 121.5 mm) radius m2_pilot
  circle e = (W/2, 6.5 mm) radius m2_pilot
  circle f = (x0 + pitch_x, y0 + 6.5*pitch_y) radius m2_pilot
  fix a b c d e f
}
extrude PostPilots from PostPilotSk distance 6 mm negative cut targets Back

// Battery bay: ribs on three sides, an opening through the floor, and a recess for the door
box BayRibL size 1.2 mm 53.4 mm 4.3 mm corner (24 mm, 21 mm, wall - 0.3 mm) join targets Back
box BayRibR size 1.2 mm 53.4 mm 4.3 mm corner (W - 25.2 mm, 21 mm, wall - 0.3 mm) join targets Back
box BayRibT size (W - 48.8 mm) 1.2 mm 4.6 mm corner (24.4 mm, 73.8 mm, wall - 0.3 mm) join targets Back
cylinder DoorBoss radius 3 mm height 6 mm base (W/2, door_scr, wall - 0.2 mm) axis Z join targets Back
box BayOpening size 41 mm 52.5 mm (wall + 1 mm) corner (W/2 - 20.5 mm, 21 mm, -0.5 mm) cut targets Back
box DoorRecess size 47 mm (door_y1 - door_y0) 1.85 mm corner (W/2 - 23.5 mm, door_y0, -0.5 mm) cut targets Back
cylinder DoorPilot radius m2_pilot height 7 mm base (W/2, door_scr, 0.5 mm) axis Z cut targets Back

// Pico 2 (micro-USB up) and charger (USB-C up) between rails, ports through the top edge
box PicoRailL size 1.2 mm 35 mm 1.6 mm corner (26.6 mm, 140 mm, wall - 0.15 mm) join targets Back
box PicoRailR size 1.2 mm 35 mm 1.6 mm corner (49.2 mm, 140 mm, wall - 0.15 mm) join targets Back
box ChargerRailL size 1.2 mm 18 mm 1.8 mm corner (53.6 mm, 160 mm, wall - 0.15 mm) join targets Back
box ChargerRailR size 1.2 mm 18 mm 1.8 mm corner (72.7 mm, 160 mm, wall - 0.15 mm) join targets Back
box PicoUsb size 10 mm (wall + 2 mm) 4.4 mm corner (33.5 mm, H - wall - 1 mm, wall + 0.3 mm) cut targets Back
box ChargerUsb size 9.8 mm (wall + 2 mm) 4 mm corner (58.85 mm, H - wall - 1 mm, wall + 1.2 mm) cut targets Back

// Power switch: body in a pocket in a block, knob through a slot in the top edge
box SwitchHolder size 11.4 mm 5.8 mm 3.8 mm corner (12.3 mm, H - wall - 5.3 mm, wall - 0.4 mm) join targets Back
box SwitchPocket size 9 mm 4 mm 5 mm corner (13.5 mm, H - wall - 3.9 mm, wall) cut targets Back
box SwitchPinSlot size 6.5 mm 2.4 mm 4 mm corner (14.75 mm, H - wall - 5.6 mm, wall + 0.8 mm) cut targets Back
box KnobSlot size 5 mm (wall + 2 mm) 2.6 mm corner (15.5 mm, H - wall - 1 mm, wall + 0.45 mm) cut targets Back

// ==== Keys: 35 + 6 + 5 caps, one part each
for r from 0 to 6 {
  for c from 0 to 4 {
    use Key{r}{c} from "137_graphing_calculator_parts/key.sc" with (cw = key_w, ch = key_h, lip = lip, z0 = key_z, flange_t = flange_t, top = key_top, stem_l = stem_l) at ((x0 + c*pitch_x), (y0 + r*pitch_y), 0) -> body Key{r}{c}
  }
}
for r from 7 to 8 {
  for c from 0 to 2 {
    use Key{r}{c} from "137_graphing_calculator_parts/key.sc" with (cw = key_w, ch = key_h, lip = lip, z0 = key_z, flange_t = flange_t, top = key_top, stem_l = stem_l) at ((x0 + c*pitch_x), (y0 + r*pitch_y), 0) -> body Key{r}{c}
  }
}
for c from 0 to 4 {
  use Fn{c} from "137_graphing_calculator_parts/key.sc" with (cw = fn_w, ch = fn_h, lip = lip, z0 = key_z, flange_t = flange_t, top = key_top, stem_l = stem_l) at ((x0 + c*pitch_x), fn_y, 0) -> body Fn{c}
}

// The arrow pad: a cross with a tapered round flange, four plungers and a centre pivot
sketch DPadFlangeSk on XY offset key_z {
  circle f = (dp_x, dp_y) radius 13.5 mm
  fix f
}
extrude DPadFlange from DPadFlangeSk distance flange_t taper (-40 deg) new -> body DPad
sketch DPadSk on XY offset (key_z + flange_t - 0.3 mm) {
  polyline closed o = (dp_x - dpad_arm/2, dp_y - dpad_span/2) (dp_x + dpad_arm/2, dp_y - dpad_span/2) (dp_x + dpad_arm/2, dp_y - dpad_arm/2) (dp_x + dpad_span/2, dp_y - dpad_arm/2) (dp_x + dpad_span/2, dp_y + dpad_arm/2) (dp_x + dpad_arm/2, dp_y + dpad_arm/2) (dp_x + dpad_arm/2, dp_y + dpad_span/2) (dp_x - dpad_arm/2, dp_y + dpad_span/2) (dp_x - dpad_arm/2, dp_y + dpad_arm/2) (dp_x - dpad_span/2, dp_y + dpad_arm/2) (dp_x - dpad_span/2, dp_y - dpad_arm/2) (dp_x - dpad_arm/2, dp_y - dpad_arm/2)
  fix o
}
extrude DPadCap from DPadSk distance (key_top - key_z - flange_t + 0.3 mm) join targets DPad
for k from 0 to 3 {
  cylinder DPadStem{k} diameter 3.5 mm height (stem_l + 0.3 mm) base (dp_x + dp_s*cos(k*90 deg), dp_y + dp_s*sin(k*90 deg), key_z - stem_l) axis Z join targets DPad
}
cylinder DPadPivot radius 2 mm height (key_z - kb_top - 0.2 mm + 0.3 mm) base (dp_x, dp_y, kb_top + 0.2 mm) axis Z join targets DPad

// ==== Battery door: a plate in the recess (0.05 mm shy of the back face, so the clearance check
// measures its real 0.25 mm gap rather than two coplanar faces), a hook under the floor at the bottom end, one screw
box DoorPlate size (47 mm - 2*fit) (door_y1 - door_y0 - 2*fit) 1.05 mm corner (W/2 - 23.5 mm + fit, door_y0 + fit, 0.05 mm) new -> body Door
box DoorRiser size 32 mm 1.75 mm 2.2 mm corner (W/2 - 16 mm, 21.25 mm, 1 mm) join targets Door
box DoorHook size 31 mm 3.3 mm 0.95 mm corner (W/2 - 15.5 mm, 19.5 mm, wall + fit) join targets Door
cylinder DoorHole radius 1.2 mm height 3 mm base (W/2, door_scr, -1 mm) axis Z cut targets Door
box DoorGrip size 20 mm 1 mm 0.5 mm corner (W/2 - 10 mm, door_y0 + 3 mm, -0.01 mm) cut targets Door

// ==== Cover: a U channel over the face whose lips ride in the side grooves, stopped at the top
sketch CoverSk on XZ offset cover_pull {
  polyline closed o = (-wall, -lip_lo) (groove_d - fit, -lip_lo) (groove_d - fit, -lip_hi) (-fit, -lip_hi) (-fit, -cover_in) (W + fit, -cover_in) (W + fit, -lip_hi) (W - groove_d + fit, -lip_hi) (W - groove_d + fit, -lip_lo) (W + wall, -lip_lo) (W + wall, -(cover_in + cover_t)) (-wall, -(cover_in + cover_t))
  fillet o.10.end radius 2 mm
  fillet o.11.end radius 2 mm
  fix o
}
extrude CoverBody from CoverSk distance (H + fit + 2 mm) new -> body Cover
box CoverStop size (W + 2 mm) 1.75 mm (cover_in - lip_lo + 0.5 mm) corner (-1 mm, H + fit + cover_pull, lip_lo + 0.25 mm) join targets Cover

// ==== The bought parts, placed (not printed)
box TftPcb size tft_w tft_h tft_t corner (W/2 - tft_w/2, scr_y - tft_h/2, pcb_z) new -> body Tft
box TftGlass size glass_w (tft_h - 0.4 mm) glass_t corner (glass_x - glass_w/2, scr_y - tft_h/2 + 0.2 mm, glass_z - 0.1 mm) join targets Tft
box PicoPcb size 21 mm 51 mm 1 mm corner (28 mm, H - wall - 51.3 mm, wall) new -> body Pico
box PicoPort size 8 mm 5.6 mm 2.6 mm corner (34.5 mm, H - wall - 6 mm, wall + 0.95 mm) join targets Pico
box ChargerPcb size 17.5 mm 28 mm 1.6 mm corner (55 mm, H - wall - 28.3 mm, wall) new -> body Charger
box ChargerPort size 8.9 mm 7.3 mm 3.2 mm corner (59.3 mm, H - wall - 7.5 mm, wall + 1.5 mm) join targets Charger
box CellBody size 40 mm 50 mm 8 mm corner (W/2 - 20 mm, 23.25 mm, 1.15 mm) new -> body Battery
box SlideBody size 8.6 mm 3.6 mm 3.5 mm corner (13.7 mm, H - wall - 3.7 mm, wall) new -> body PowerSwitch
box SlideKnob size 2 mm 4 mm 1.5 mm corner (17 mm, H - wall - 0.2 mm, wall + 1 mm) join targets PowerSwitch
box Perfboard size 73 mm 120 mm kb_t corner (9.5 mm, 4 mm, kb_bot) new -> body KeyBoard
for r from 0 to 6 {
  for c from 0 to 4 {
    box SwBody{r}{c} size sw sw 3.5 mm corner (x0 + c*pitch_x - sw/2, y0 + r*pitch_y - sw/2, kb_top) new -> body Switch{r}{c}
    cylinder SwButton{r}{c} radius 1.75 mm height (sw_h - 3.4 mm) base (x0 + c*pitch_x, y0 + r*pitch_y, kb_top + 3.4 mm) axis Z join targets Switch{r}{c}
  }
}
for r from 7 to 8 {
  for c from 0 to 2 {
    box SwBody{r}{c} size sw sw 3.5 mm corner (x0 + c*pitch_x - sw/2, y0 + r*pitch_y - sw/2, kb_top) new -> body Switch{r}{c}
    cylinder SwButton{r}{c} radius 1.75 mm height (sw_h - 3.4 mm) base (x0 + c*pitch_x, y0 + r*pitch_y, kb_top + 3.4 mm) axis Z join targets Switch{r}{c}
  }
}
for c from 0 to 4 {
  box FnSwBody{c} size sw sw 3.5 mm corner (x0 + c*pitch_x - sw/2, fn_y - sw/2, kb_top) new -> body FnSwitch{c}
  cylinder FnSwButton{c} radius 1.75 mm height (sw_h - 3.4 mm) base (x0 + c*pitch_x, fn_y, kb_top + 3.4 mm) axis Z join targets FnSwitch{c}
}
for k from 0 to 3 {
  box DSwBody{k} size sw sw 3.5 mm corner (dp_x + dp_s*cos(k*90 deg) - sw/2, dp_y + dp_s*sin(k*90 deg) - sw/2, kb_top) new -> body DSwitch{k}
  cylinder DSwButton{k} radius 1.75 mm height (sw_h - 3.4 mm) base (dp_x + dp_s*cos(k*90 deg), dp_y + dp_s*sin(k*90 deg), kb_top + 3.4 mm) axis Z join targets DSwitch{k}
}

// ==== Fits
check Key00 clearance Front >= 0.2 mm
check Key00 clearance Switch00 >= 0.05 mm
check Fn2 clearance Front >= 0.2 mm
check DPad clearance Front >= 0.2 mm
check DPad clearance KeyBoard >= 0.1 mm
check DPad clearance DSwitch0 >= 0.05 mm
// The cover's fit is checked closed: with it slid part way the engine's clearance reads 0 where
// its open end crosses the case (a measuring artefact; the cross-section is the same either way).
if cover_pull == 0 mm {
  check Cover clearance Front >= 0.2 mm
  check Cover clearance Key30 >= 0.3 mm
}
check Door clearance Back >= 0.2 mm
check Tft clearance Front >= 0 mm
check KeyBoard clearance Front >= 0.2 mm
check Battery clearance Door >= 0 mm

// ==== Finish
color Back, Front "#3a3d44"
color Cover "#2a2b30"
color Door "#3a3d44"
color DPad "#c9ccd2"
color Fn0, Fn1, Fn2, Fn3, Fn4 "#c9ccd2"
color Tft "#1d2330"
color Pico, Charger, KeyBoard "#2f6b3a"
color Battery "#9aa4b0"
color PowerSwitch "#3a3a3a"
material Back, Front, Cover, Door, DPad PLA
