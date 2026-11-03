// 92. Widescreen handheld console in the PSP style, printable: a Raspberry Pi Zero 2 W game handheld.
//
// The console lies face up: X across (0 at the centre, the long way), Y up the face (0 at the
// centre), Z out of the face (0 at the back, D at the face). In plan it is a pill: a rectangle
// with fully round ends.
//
// Bill of materials (module sizes are params; check them against yours):
//   1  Raspberry Pi Zero 2 W (65 x 30 mm, M2.5 holes on 58 x 23 mm), on four standoffs
//   1  4.3 in 800 x 480 DSI/DPI display, 105.5 x 67.2 mm board, 6 mm thick, 95 x 53.6 mm view
//   1  LiPo 505060 (5 x 50 x 60 mm, about 2000 mAh), under the Pi
//   1  TP4056 USB-C charger board (28 x 17 mm), on a pad, its port in the left end
//   1  MT3608 boost board set to 5.1 V (36 x 17 mm), in the right wing
//   1  PSP-style analog stick module (20 x 20 mm, 8 mm with its cap) under the left wing
//   12 6 x 6 x 5 mm tactile switches: 4 for the D-pad and 4 face buttons + Start + Select on two
//      pieces of 2.54 mm perfboard (22 x 23 and 22 x 33.5 mm), 2 right-angle ones for L and R
//   1  SS12D00 mini slide switch (power), in the bottom edge
//   4  M2 x 20 screws close the case; 11 M2 x 5 screws hold the boards and the stick
//
// How it goes together. The Shell is a pill-shaped tray, shelled 2 mm with the back edge's big
// round kept, carrying the Pi standoffs, the bays and the openings. The Face is a 2.5 mm plate that
// covers the top: screen window, D-pad, face-button, Start/Select and stick holes, and posts under
// it for the two switch boards and the stick. Every cap has a flange under the plate, so it drops
// in from inside and the board behind it holds it; the D-pad rocks on a centre pivot. L and R are
// caps in slots in the top wall pressing right-angle switches on two blocks. The display sits
// under the plate between four ribs. Four M2 screws from the back close the case.
//
// Printing, no supports: the Shell open side up, the Face face down, every cap face down
// (flange up), L and R on their sides.
design WidescreenHandheld units mm

// Body
param L = 170 mm
param W = 74 mm
param D = 23 mm
param back_r = 4 mm           // the back edge's round (small enough that the screw bosses stand on flat floor)
param wall = 2 mm
param plate_t = 2.5 mm
param fit = 0.25 mm

// Display
param disp_w = 105.5 mm
param disp_h = 67.2 mm
param disp_t = 6 mm
param view_w = 95 mm
param view_h = 53.6 mm

// Controls
param wing_x = 65.5 mm        // centre of the D-pad and of the button diamond, from the centre;
                              // every cap flange stays outside the display's edge
param ctrl_y = 7.5 mm
param dpad_span = 20 mm
param dpad_arm = 7 mm
param btn_d = 8 mm
param btn_gap = 7.5 mm        // diamond centre to each button
param proud = 1.5 mm          // caps stand this far above the face
param switch_h = 5 mm
param stick_x = -63 mm
param stick_y = -14 mm
param stick_hole = 17 mm

// Modules
param bat_w = 50 mm
param bat_l = 60 mm
param bat_t = 5 mm

let end_x = L/2 - W/2
let tray_h = D - plate_t
let front_in = tray_h                         // the plate's underside
let flange_z = front_in - 1 mm                // bottom of every cap flange
let board_top = flange_z - 0.2 mm - switch_h  // 0.2 mm of free travel
let board_z = board_top - 1.6 mm
let disp_z = front_in - disp_t
let pi_z = 9 mm                               // underside of the Pi
let chg_z = wall + 6 mm                       // the charger rides on a pad, its port clear of the back's round
let usb_z = chg_z + 2.6 mm

// ==== Shell: the pill tray, back edge rounded, shelled with the top open
sketch BodySk on XY {
  slot s = centers (-end_x, 0) (end_x, 0) width W
  fix s
}
extrude BodyEx from BodySk distance tray_h new -> body Shell
fillet BackEdge edges (0, W/2, 0) radius back_r body Shell
shell Hollow faces (0, 0, tray_h) thickness wall inside body Shell

// Openings: USB-C in the left end, power switch in the bottom edge, L and R in the top edge
sketch UsbSk on YZ offset (-L/2 - 1 mm) {
  slot u = centers (-2.9 mm, usb_z) (2.9 mm, usb_z) width 3.6 mm
  fix u
}
extrude UsbPort from UsbSk distance (wall + 4 mm) cut targets Shell
box PowerSlot size 5.2 mm (wall + 2 mm) 2 mm corner (17.4 mm, -W/2 - 1 mm, wall + 2.5 mm) cut targets Shell
box SlotL size (16 mm + 2*fit) (wall + 2 mm) (5 mm + 2*fit) corner (-40 mm - 8 mm - fit, W/2 - wall - 1 mm, 9 mm - fit) cut targets Shell
box SlotR size (16 mm + 2*fit) (wall + 2 mm) (5 mm + 2*fit) corner (40 mm - 8 mm - fit, W/2 - wall - 1 mm, 9 mm - fit) cut targets Shell

// Pi Zero standoffs (M2.5 pilots), the case screw bosses (M2 clearance, counterbored from the back)
for k from 0 to 3 {
  cylinder PiPost{k} radius 2.5 mm height (pi_z - wall + 0.5 mm) base (-29 mm + mod(k, 2)*58 mm, -11.5 mm + floor(k/2)*23 mm, wall - 0.5 mm) axis Z join targets Shell
  cylinder PiPilot{k} radius 1.05 mm height 5 mm base (-29 mm + mod(k, 2)*58 mm, -11.5 mm + floor(k/2)*23 mm, pi_z - 4.9 mm) axis Z cut targets Shell
}
for k from 0 to 3 {
  cylinder CaseBoss{k} radius 2.8 mm height (front_in - 4 mm - wall + 0.5 mm) base (-56 mm + mod(k, 2)*112 mm, -29 mm + floor(k/2)*56 mm, wall - 0.5 mm) axis Z join targets Shell
  cylinder CaseHole{k} radius 1.15 mm height D base (-56 mm + mod(k, 2)*112 mm, -29 mm + floor(k/2)*56 mm, -1 mm) axis Z cut targets Shell
  cylinder CaseHead{k} radius 2.1 mm height (wall + 1 mm) base (-56 mm + mod(k, 2)*112 mm, -29 mm + floor(k/2)*56 mm, -1 mm) axis Z cut targets Shell
}
// Bays: the LiPo between two ribs, the charger and boost boards between ribs, blocks for L and R
box BatRibL size 1.2 mm (bat_l - 10 mm) 2.5 mm corner (-bat_w/2 - fit - 1.2 mm, -bat_l/2 + 5 mm, wall - 0.2 mm) join targets Shell
box BatRibR size 1.2 mm (bat_l - 10 mm) 2.5 mm corner (bat_w/2 + fit, -bat_l/2 + 5 mm, wall - 0.2 mm) join targets Shell
box ChgPad size 22 mm 15 mm (chg_z - wall + 0.2 mm) corner (-78 mm, -7.5 mm, wall - 0.2 mm) join targets Shell
box ChgRibT size 22 mm 1.2 mm (chg_z - wall + 2.5 mm) corner (-78 mm, 8.5 mm + fit, wall - 0.2 mm) join targets Shell
box ChgRibB size 22 mm 1.2 mm (chg_z - wall + 2.5 mm) corner (-78 mm, -8.5 mm - fit - 1.2 mm, wall - 0.2 mm) join targets Shell
box BoostRibL size 1.2 mm 30 mm 2.5 mm corner (60 mm - fit - 1.2 mm, -15 mm, wall - 0.2 mm) join targets Shell
box BoostRibR size 1.2 mm 30 mm 2.5 mm corner (77 mm + fit, -15 mm, wall - 0.2 mm) join targets Shell
box BlockL size 12 mm 4 mm 12 mm corner (-46 mm, W/2 - wall - 1.2 mm - 3.5 mm - 4 mm, wall - 0.5 mm) join targets Shell
box BlockR size 12 mm 4 mm 12 mm corner (34 mm, W/2 - wall - 1.2 mm - 3.5 mm - 4 mm, wall - 0.5 mm) join targets Shell
// The round disc door of the original, as a 0.4 mm recessed disc on the back
cylinder DoorRecess radius 26 mm height 1.4 mm base (0, 0, -1 mm) axis Z cut targets Shell

// ==== Face plate: the whole top, its edge rounded, holes for the screen and every control
sketch FaceSk on XY offset tray_h {
  slot s = centers (-end_x, 0) (end_x, 0) width W
  fix s
}
extrude FacePlate from FaceSk distance plate_t new -> body Face
fillet FaceEdge edges (0, W/2, D) radius 2 mm body Face
sketch HolesSk on XY offset (D + 1 mm) {
  rect rounded v = (-view_w/2, -view_h/2) (view_w/2, view_h/2) radius 2 mm
  circle a = (wing_x, ctrl_y + btn_gap) radius (btn_d/2 + fit)
  circle b = (wing_x + btn_gap, ctrl_y) radius (btn_d/2 + fit)
  circle c = (wing_x, ctrl_y - btn_gap) radius (btn_d/2 + fit)
  circle d = (wing_x - btn_gap, ctrl_y) radius (btn_d/2 + fit)
  circle st = (wing_x - 3 mm, ctrl_y - 18 mm) radius (2.5 mm + fit)
  circle se = (wing_x + 6 mm, ctrl_y - 18 mm) radius (2.5 mm + fit)
  circle sk = (stick_x, stick_y) radius (stick_hole/2)
  polyline closed dx = (-wing_x - dpad_arm/2 - fit, ctrl_y - dpad_span/2 - fit) (-wing_x + dpad_arm/2 + fit, ctrl_y - dpad_span/2 - fit) (-wing_x + dpad_arm/2 + fit, ctrl_y - dpad_arm/2 - fit) (-wing_x + dpad_span/2 + fit, ctrl_y - dpad_arm/2 - fit) (-wing_x + dpad_span/2 + fit, ctrl_y + dpad_arm/2 + fit) (-wing_x + dpad_arm/2 + fit, ctrl_y + dpad_arm/2 + fit) (-wing_x + dpad_arm/2 + fit, ctrl_y + dpad_span/2 + fit) (-wing_x - dpad_arm/2 - fit, ctrl_y + dpad_span/2 + fit) (-wing_x - dpad_arm/2 - fit, ctrl_y + dpad_arm/2 + fit) (-wing_x - dpad_span/2 - fit, ctrl_y + dpad_arm/2 + fit) (-wing_x - dpad_span/2 - fit, ctrl_y - dpad_arm/2 - fit) (-wing_x - dpad_arm/2 - fit, ctrl_y - dpad_arm/2 - fit)
  fix v a b c d st se sk dx
}
extrude FaceHoles from HolesSk distance (plate_t + 2 mm) negative cut targets Face

// Under the plate: case-screw bosses, posts for the two boards and the stick, ribs round the display
for k from 0 to 3 {
  cylinder FaceBoss{k} radius 2.8 mm height 4.5 mm base (-56 mm + mod(k, 2)*112 mm, -29 mm + floor(k/2)*56 mm, front_in - 4 mm) axis Z join targets Face
  cylinder FacePilot{k} radius 0.8 mm height 4.2 mm base (-56 mm + mod(k, 2)*112 mm, -29 mm + floor(k/2)*56 mm, front_in - 4.1 mm) axis Z cut targets Face
  // D-pad board: a post at each corner, the inner pair kept off the display's edge
  cylinder DBoardPost{k} radius 2.2 mm height (front_in - board_top + 0.5 mm) base (-wing_x - 8.5 mm + mod(k, 2)*17 mm, ctrl_y - 9.5 mm + floor(k/2)*19 mm, board_top) axis Z join targets Face
  cylinder DBoardPilot{k} radius 0.8 mm height 5 mm base (-wing_x - 8.5 mm + mod(k, 2)*17 mm, ctrl_y - 9.5 mm + floor(k/2)*19 mm, board_top - 0.1 mm) axis Z cut targets Face
}
// Button board (22 x 33.5): three posts, clear of the display's edge and the Start and Select caps
cylinder BBoardPost0 radius 2.2 mm height (front_in - board_top + 0.5 mm) base (wing_x - 8.5 mm, ctrl_y + 9.5 mm, board_top) axis Z join targets Face
cylinder BBoardPost1 radius 2.2 mm height (front_in - board_top + 0.5 mm) base (wing_x + 8.5 mm, ctrl_y + 9.5 mm, board_top) axis Z join targets Face
cylinder BBoardPost2 radius 2.2 mm height (front_in - board_top + 0.5 mm) base (wing_x - 8.5 mm, ctrl_y - 22 mm, board_top) axis Z join targets Face
cylinder BBoardPilot0 radius 0.8 mm height 5 mm base (wing_x - 8.5 mm, ctrl_y + 9.5 mm, board_top - 0.1 mm) axis Z cut targets Face
cylinder BBoardPilot1 radius 0.8 mm height 5 mm base (wing_x + 8.5 mm, ctrl_y + 9.5 mm, board_top - 0.1 mm) axis Z cut targets Face
cylinder BBoardPilot2 radius 0.8 mm height 5 mm base (wing_x - 8.5 mm, ctrl_y - 22 mm, board_top - 0.1 mm) axis Z cut targets Face
// Stick module: screwed to two short posts beside it
cylinder StickPostA radius 2.2 mm height 1 mm base (stick_x - 12.5 mm, stick_y + 7 mm, front_in - 0.5 mm - 0.5 mm) axis Z join targets Face
cylinder StickPostB radius 2.2 mm height 1 mm base (stick_x + 12.5 mm, stick_y - 7 mm, front_in - 0.5 mm - 0.5 mm) axis Z join targets Face
box DispRibT size 30 mm 1.2 mm 3.3 mm corner (-15 mm, disp_h/2 + fit, front_in - 3 mm) join targets Face
box DispRibB size 30 mm 1.2 mm 3.3 mm corner (-15 mm, -disp_h/2 - fit - 1.2 mm, front_in - 3 mm) join targets Face
box DispRibL size 1.2 mm 12 mm 3.3 mm corner (-disp_w/2 - fit - 1.2 mm, 3 mm, front_in - 3 mm) join targets Face
box DispRibR size 1.2 mm 12 mm 3.3 mm corner (disp_w/2 + fit, 3 mm, front_in - 3 mm) join targets Face

// ==== Caps, each with a flange under the plate
// D-pad: a cross with a cross-shaped flange and a centre pivot resting on its board
sketch DPadSk on XY offset flange_z {
  polyline closed o = (-wing_x - dpad_arm/2, ctrl_y - dpad_span/2) (-wing_x + dpad_arm/2, ctrl_y - dpad_span/2) (-wing_x + dpad_arm/2, ctrl_y - dpad_arm/2) (-wing_x + dpad_span/2, ctrl_y - dpad_arm/2) (-wing_x + dpad_span/2, ctrl_y + dpad_arm/2) (-wing_x + dpad_arm/2, ctrl_y + dpad_arm/2) (-wing_x + dpad_arm/2, ctrl_y + dpad_span/2) (-wing_x - dpad_arm/2, ctrl_y + dpad_span/2) (-wing_x - dpad_arm/2, ctrl_y + dpad_arm/2) (-wing_x - dpad_span/2, ctrl_y + dpad_arm/2) (-wing_x - dpad_span/2, ctrl_y - dpad_arm/2) (-wing_x - dpad_arm/2, ctrl_y - dpad_arm/2)
  fix o
}
sketch DPadFlangeSk on XY offset flange_z {
  rect h = (-wing_x - dpad_span/2 - 1.5 mm, ctrl_y - dpad_arm/2 - 1.5 mm) (-wing_x + dpad_span/2 + 1.5 mm, ctrl_y + dpad_arm/2 + 1.5 mm)
  fix h
}
sketch DPadFlangeSk2 on XY offset flange_z {
  rect v = (-wing_x - dpad_arm/2 - 1.5 mm, ctrl_y - dpad_span/2 - 1 mm) (-wing_x + dpad_arm/2 + 1.5 mm, ctrl_y + dpad_span/2 + 1 mm)
  fix v
}
extrude DPadKey from DPadSk distance (D + proud - flange_z) new -> body DPad
extrude DPadFlangeH from DPadFlangeSk distance 1 mm join targets DPad
extrude DPadFlangeV from DPadFlangeSk2 distance 1 mm join targets DPad
cylinder DPadPivot radius 1.5 mm height (flange_z - board_top + 0.2 mm) base (-wing_x, ctrl_y, board_top) axis Z join targets DPad
cylinder DPadDimple radius 2 mm height 0.6 mm base (-wing_x, ctrl_y, D + proud - 0.5 mm) cut targets DPad

// Face buttons, Start and Select
cylinder ButtonA radius (btn_d/2) height (D + proud - flange_z) base (wing_x, ctrl_y + btn_gap, flange_z) axis Z new -> body ButtonA
cylinder ButtonAF radius (btn_d/2 + 0.75 mm) height 1 mm base (wing_x, ctrl_y + btn_gap, flange_z) axis Z join targets ButtonA
cylinder ButtonB radius (btn_d/2) height (D + proud - flange_z) base (wing_x + btn_gap, ctrl_y, flange_z) axis Z new -> body ButtonB
cylinder ButtonBF radius (btn_d/2 + 0.75 mm) height 1 mm base (wing_x + btn_gap, ctrl_y, flange_z) axis Z join targets ButtonB
cylinder ButtonC radius (btn_d/2) height (D + proud - flange_z) base (wing_x, ctrl_y - btn_gap, flange_z) axis Z new -> body ButtonC
cylinder ButtonCF radius (btn_d/2 + 0.75 mm) height 1 mm base (wing_x, ctrl_y - btn_gap, flange_z) axis Z join targets ButtonC
cylinder ButtonD radius (btn_d/2) height (D + proud - flange_z) base (wing_x - btn_gap, ctrl_y, flange_z) axis Z new -> body ButtonD
cylinder ButtonDF radius (btn_d/2 + 0.75 mm) height 1 mm base (wing_x - btn_gap, ctrl_y, flange_z) axis Z join targets ButtonD
cylinder StartCap radius 2.5 mm height (D + 0.8 mm - flange_z) base (wing_x - 3 mm, ctrl_y - 18 mm, flange_z) axis Z new -> body Start
cylinder StartF radius 4 mm height 1 mm base (wing_x - 3 mm, ctrl_y - 18 mm, flange_z) axis Z join targets Start
cylinder SelectCap radius 2.5 mm height (D + 0.8 mm - flange_z) base (wing_x + 6 mm, ctrl_y - 18 mm, flange_z) axis Z new -> body Select
cylinder SelectF radius 4 mm height 1 mm base (wing_x + 6 mm, ctrl_y - 18 mm, flange_z) axis Z join targets Select

// Shoulder triggers: a cap through each top-wall slot, a flange inside, pressing a side switch
box TrigL size 16 mm (wall + 1.5 mm) 5 mm corner (-48 mm, W/2 - wall - 0.2 mm, 9 mm) new -> body TriggerL
box TrigLF size 20 mm 1.2 mm 8 mm corner (-50 mm, W/2 - wall - 1.4 mm, 7.5 mm) join targets TriggerL
box TrigR size 16 mm (wall + 1.5 mm) 5 mm corner (32 mm, W/2 - wall - 0.2 mm, 9 mm) new -> body TriggerR
box TrigRF size 20 mm 1.2 mm 8 mm corner (30 mm, W/2 - wall - 1.4 mm, 7.5 mm) join targets TriggerR

// ==== Modules, for checking fit (not printed)
box RefDisplay size disp_w disp_h disp_t corner (-disp_w/2, -disp_h/2, disp_z) new -> body RefDisplay
box RefPi size 65 mm 30 mm 4.5 mm corner (-32.5 mm, -15 mm, pi_z) new -> body RefPi
box RefBattery size bat_w bat_l bat_t corner (-bat_w/2, -bat_l/2, wall) new -> body RefBattery
box RefCharger size 28 mm 17 mm 4 mm corner (-82 mm, -8.5 mm, chg_z) new -> body RefCharger
box RefBoost size 17 mm 30 mm 5 mm corner (60 mm, -15 mm, wall) new -> body RefBoost
box RefDBoard size 22 mm 23 mm 1.6 mm corner (-wing_x - 11 mm, ctrl_y - 11.5 mm, board_z) new -> body RefDBoard
box RefBBoard size 22 mm 33.5 mm 1.6 mm corner (wing_x - 11 mm, ctrl_y - 22.5 mm, board_z) new -> body RefBBoard
box RefStick size 20 mm 20 mm 4.5 mm corner (stick_x - 10 mm, stick_y - 10 mm, front_in - 1 mm - 4.5 mm) new -> body RefStick
cylinder RefStickCap radius 7 mm height 4 mm base (stick_x, stick_y, front_in - 1 mm) axis Z join targets RefStick

color Shell "#2b2d31"
color Face "#0d0d0f"
color DPad, ButtonA, ButtonB, ButtonC, ButtonD, Start, Select, TriggerL, TriggerR "#3a3c40"
color RefDisplay "#16181c"
color RefPi, RefCharger, RefBoost, RefDBoard, RefBBoard "#2f6f3a"
color RefBattery "#c8a13a"
color RefStick "#555555"
material Shell, Face, DPad, ButtonA, ButtonB, ButtonC, ButtonD, Start, Select, TriggerL, TriggerR PLA
