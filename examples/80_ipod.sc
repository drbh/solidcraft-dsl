// 80. Pocket music player in the iPod classic style, printable: an ESP32-S3 MP3 player with a click wheel.
//
// The player lies face up: X across its width, Y up its length (y = 0 is the bottom edge with the
// USB-C port, y = H the top with the power switch and headphone jack), Z out of the front (z = 0 is
// the back, z = T the face).
//
// Bill of materials (check the module sizes against yours; they are all params):
//   1  Seeed XIAO ESP32-S3 (21 x 17.8 mm), its USB-C at the bottom edge; it also charges the LiPo
//   1  DFPlayer Mini MP3 module (20.6 x 20.4 mm) with a micro SD card
//   1  2.0 in ST7789 240 x 320 IPS SPI display module, used landscape (56 x 36 mm board,
//      41.5 x 31.5 mm window)
//   1  402535 LiPo, 3.7 V about 300 mAh (25 x 35 x 4 mm)
//   1  PJ-320A 3.5 mm headphone jack (12 x 6 x 5 mm body)
//   1  SS12D00 mini slide switch (power)
//   5  6 x 6 x 5 mm tactile switches on a 42 x 42 mm piece of 2.54 mm perfboard
//   8  M2 x 6 self-tapping screws: 4 close the case, 4 hold the wheel board
//
// How it goes together. The case is one rounded block, hollowed, then split at z = split_z into a
// Front (white, with the window, the wheel hole and posts for the wheel board) and a Back
// (silver, with the USB-C, jack and switch openings and the bays for the modules). The Wheel ring
// drops into its hole from inside; its flange is wider than the hole, so the face holds it. The
// Button sits in the ring's centre hole and its flange sits under the ring. The wheel board then
// screws to the Front's four posts with its switches facing the face: four switches under the
// ring's nubs (menu, skip back, skip forward, play) and one under the button's nub. Display, LiPo
// and DFPlayer go in with foam tape between their locating ribs. Four M2 screws from the back
// close the case into the Front's bosses.
//
// Printing, no supports: the Front face down (its face is the bed), the Back open side up, the Wheel
// and Button face down. The wheel's marks are engraved into its face, so they print on the bed.
design PocketPlayer units mm

// Case
param W = 62 mm
param H = 104 mm
param T = 19 mm
param corner_r = 8 mm
param wall = 2 mm
param floor_t = 2 mm
param back_round = 2.5 mm        // fillet round the back's rear edge
param split_z = 9 mm           // where the Front and Back meet
param fit = 0.25 mm            // clearance round moving parts

// Display module, landscape, centred across the case
param disp_w = 56 mm
param disp_h = 36 mm
param disp_t = 3.6 mm          // glass plus board
param window_w = 41.5 mm
param window_h = 31.5 mm
param window_top = 12 mm       // from the top edge to the window

// Click wheel
param wheel_d = 38.5 mm
param wheel_y = 30 mm          // centre, from the bottom edge
param button_d = 14.5 mm
param flange_d = 44 mm         // the ring's retaining flange, under the face
param nub_r = 13 mm            // the four ring nubs' radius, over the four switches
param switch_h = 5 mm          // tactile switch, base to top of the plunger
param board_w = 42 mm          // the perfboard, square

// Modules in the back
param xiao_w = 17.8 mm
param xiao_l = 21 mm
param bat_w = 25 mm
param bat_l = 35 mm
param bat_t = 4 mm
param dfp_w = 20.6 mm

let front_in = T - floor_t                    // the face's inside
let wheel_r = wheel_d/2
let button_r = button_d/2
let nub_z = front_in - 3.2 mm                 // bottom of every nub
let board_top = nub_z - 0.3 mm - switch_h     // 0.3 mm of free travel before a switch clicks
let window_y = H - window_top - window_h
let disp_y = window_y + window_h/2 - disp_h/2 // display board's lower edge
let boss_in = 6.5 mm                          // screw bosses, in from each corner
let usb_z = floor_t + 2.8 mm                  // XIAO's USB-C centre, the board on the floor
let jack_z = floor_t + 3.6 mm                 // the jack sits on a 1.1 mm pad, so its hole clears the floor

// ==== Case: one rounded block, hollowed, its openings cut, then split into Front and Back
sketch Outline on XY {
  rect rounded o = (0,0) (W,H) radius corner_r
  fix o
}
extrude Block from Outline distance T new -> body Front
fillet BackRound edges (W/2, 0, 0) radius back_round body Front
fillet FaceEdge edges (W/2, 0, T) radius 1.2 mm body Front
sketch CavitySk on XY offset floor_t {
  rect rounded c = (wall, wall) (W - wall, H - wall) radius (corner_r - wall)
  fix c
}
extrude Cavity from CavitySk distance (front_in - floor_t) cut targets Front

// The face: display window and the wheel's hole
sketch WindowSk on XY offset (T + 1 mm) {
  rect rounded w = ((W - window_w)/2, window_y) ((W + window_w)/2, window_y + window_h) radius 1.5 mm
  fix w
}
extrude Window from WindowSk distance (floor_t + 2 mm) negative cut targets Front
cylinder WheelHole radius (wheel_r + fit) height (floor_t + 2 mm) base (W/2, wheel_y, front_in - 1 mm) axis Z cut targets Front

// The edges: USB-C at the bottom, power switch and headphone jack at the top
sketch UsbSk on XZ offset (-1 mm) {
  slot u = centers (W/2 - 3.2 mm, -usb_z) (W/2 + 3.2 mm, -usb_z) width 3.6 mm
  fix u
}
extrude UsbPort from UsbSk distance (wall + 2 mm) cut targets Front
box PowerSlot size 5.2 mm (wall + 2 mm) 2 mm corner (12 mm, H - wall - 1 mm, floor_t + 2 mm) cut targets Front
cylinder JackHole radius 3 mm height (wall + 2 mm) base (W - 16 mm, H - wall - 1 mm, jack_z) axis (0,1,0) cut targets Front

split Halves target Front origin (0, 0, split_z) normal (0, 0, 1) -> body Back

// ==== Back: screw bosses with counterbored clearance holes, and bays for the modules
for k from 0 to 3 {
  cylinder BackBoss{k} radius 2.8 mm height (split_z - floor_t + 0.5 mm) base (boss_in + mod(k, 2)*(W - 2*boss_in), boss_in + floor(k/2)*(H - 2*boss_in), floor_t - 0.5 mm) axis Z join targets Back
  cylinder BackBossHole{k} radius 1.15 mm height (split_z + 2 mm) base (boss_in + mod(k, 2)*(W - 2*boss_in), boss_in + floor(k/2)*(H - 2*boss_in), -1 mm) axis Z cut targets Back
  cylinder BackHead{k} radius 2.1 mm height 2.4 mm base (boss_in + mod(k, 2)*(W - 2*boss_in), boss_in + floor(k/2)*(H - 2*boss_in), -1 mm) axis Z cut targets Back
}
// XIAO: two ribs either side, its USB-C end against the bottom wall
box XiaoRibL size 1.2 mm xiao_l 2 mm corner (W/2 - xiao_w/2 - fit - 1.2 mm, wall + 0.2 mm, floor_t - 0.2 mm) join targets Back
box XiaoRibR size 1.2 mm xiao_l 2 mm corner (W/2 + xiao_w/2 + fit, wall + 0.2 mm, floor_t - 0.2 mm) join targets Back
// LiPo and DFPlayer under the display, each between ribs
box BatRibL size 1.2 mm bat_l 2 mm corner (8 mm - 1.2 mm, disp_y, floor_t - 0.2 mm) join targets Back
box BatRibR size 1.2 mm bat_l 2 mm corner (8 mm + bat_w + 2*fit, disp_y, floor_t - 0.2 mm) join targets Back
box DfpRib size 1.2 mm dfp_w 2 mm corner (36 mm - 1.2 mm, disp_y + 2 mm, floor_t - 0.2 mm) join targets Back
// Jack: a cradle holding its body square behind the hole
box JackPad size 6 mm 11.5 mm 1.3 mm corner (W - 16 mm - 3 mm, H - wall - 12 mm, floor_t - 0.2 mm) join targets Back
box JackRibL size 1.2 mm 12 mm 5 mm corner (W - 16 mm - 3 mm - fit - 1.2 mm, H - wall - 12 mm, floor_t - 0.2 mm) join targets Back
box JackRibR size 1.2 mm 12 mm 5 mm corner (W - 16 mm + 3 mm + fit, H - wall - 12 mm, floor_t - 0.2 mm) join targets Back

// ==== Front: bosses taking the case screws, posts for the wheel board, ribs round the display
for k from 0 to 3 {
  cylinder FrontBoss{k} radius 2.8 mm height (front_in - split_z + 0.5 mm) base (boss_in + mod(k, 2)*(W - 2*boss_in), boss_in + floor(k/2)*(H - 2*boss_in), split_z) axis Z join targets Front
  cylinder FrontPilot{k} radius 0.8 mm height 7 mm base (boss_in + mod(k, 2)*(W - 2*boss_in), boss_in + floor(k/2)*(H - 2*boss_in), split_z - 0.1 mm) axis Z cut targets Front
  cylinder BoardPost{k} radius 2.4 mm height (front_in - board_top + 0.5 mm) base (W/2 - board_w/2 + 2.5 mm + mod(k, 2)*(board_w - 5 mm), wheel_y - board_w/2 + 2.5 mm + floor(k/2)*(board_w - 5 mm), board_top) axis Z join targets Front
  cylinder BoardPilot{k} radius 0.8 mm height 6 mm base (W/2 - board_w/2 + 2.5 mm + mod(k, 2)*(board_w - 5 mm), wheel_y - board_w/2 + 2.5 mm + floor(k/2)*(board_w - 5 mm), board_top - 0.1 mm) axis Z cut targets Front
}
box DispRibB size 12 mm 1.2 mm 3 mm corner (W/2 - 6 mm, disp_y - fit - 1.2 mm, front_in - 3 mm) join targets Front
box DispRibT size 12 mm 1.2 mm 3 mm corner (W/2 - 6 mm, disp_y + disp_h + fit, front_in - 3 mm) join targets Front
box DispRibL size 1.2 mm 12 mm 3 mm corner ((W - disp_w)/2 - fit - 1.2 mm, disp_y + disp_h/2 - 6 mm, front_in - 3 mm) join targets Front
box DispRibR size 1.2 mm 12 mm 3 mm corner ((W + disp_w)/2 + fit, disp_y + disp_h/2 - 6 mm, front_in - 3 mm) join targets Front

// ==== Wheel ring: shows in the face hole, held in by its flange, presses four switches with its nubs
cylinder WheelTop radius wheel_r height floor_t base (W/2, wheel_y, front_in) axis Z new -> body Wheel
cylinder WheelFlange radius (flange_d/2) height 1.2 mm base (W/2, wheel_y, front_in - 1.2 mm) axis Z join targets Wheel
for k from 0 to 3 {
  cylinder WheelNub{k} radius 2 mm height 2.2 mm base (W/2 + nub_r*cos(k*90 deg), wheel_y + nub_r*sin(k*90 deg), nub_z) axis Z join targets Wheel
}
cylinder WheelBore radius (button_r + fit) height (floor_t + 3 mm) base (W/2, wheel_y, front_in - 2 mm) axis Z cut targets Wheel
// Engraved marks: skip back (left), skip forward (right), play/pause (bottom), MENU (top)
let mark_r = (wheel_r + button_r)/2
sketch MarksSk on XY offset (T + 0.1 mm) {
  polyline closed back1 = (W/2 - mark_r + 1.6 mm, wheel_y - 1.4 mm) (W/2 - mark_r + 1.6 mm, wheel_y + 1.4 mm) (W/2 - mark_r, wheel_y)
  polyline closed back2 = (W/2 - mark_r, wheel_y - 1.4 mm) (W/2 - mark_r, wheel_y + 1.4 mm) (W/2 - mark_r - 1.6 mm, wheel_y)
  polyline closed fwd1 = (W/2 + mark_r - 1.6 mm, wheel_y - 1.4 mm) (W/2 + mark_r - 1.6 mm, wheel_y + 1.4 mm) (W/2 + mark_r, wheel_y)
  polyline closed fwd2 = (W/2 + mark_r, wheel_y - 1.4 mm) (W/2 + mark_r, wheel_y + 1.4 mm) (W/2 + mark_r + 1.6 mm, wheel_y)
  polyline closed play = (W/2 - 2.4 mm, wheel_y - mark_r - 1.3 mm) (W/2 - 2.4 mm, wheel_y - mark_r + 1.3 mm) (W/2 - 0.4 mm, wheel_y - mark_r)
  rect pause1 = (W/2 + 0.4 mm, wheel_y - mark_r - 1.3 mm) (W/2 + 1.0 mm, wheel_y - mark_r + 1.3 mm)
  rect pause2 = (W/2 + 1.6 mm, wheel_y - mark_r - 1.3 mm) (W/2 + 2.2 mm, wheel_y - mark_r + 1.3 mm)
  fix back1 back2 fwd1 fwd2 play pause1 pause2
}
extrude Marks from MarksSk distance 0.5 mm negative cut targets Wheel
sketch MenuSk on XY offset (T + 0.1 mm) {
  text "MENU" at (W/2, wheel_y + mark_r - 1.1 mm) height 2.4 mm align center
}
extrude Menu from MenuSk distance 0.5 mm negative cut targets Wheel

// ==== Centre button: cap in the ring's bore, flange under the ring, nub on the middle switch
cylinder ButtonCap radius button_r height (floor_t + 1.2 mm) base (W/2, wheel_y, front_in - 1.2 mm) axis Z new -> body Button
cylinder ButtonFlange radius (button_r + 2 mm) height 1 mm base (W/2, wheel_y, front_in - 2.2 mm) axis Z join targets Button
cylinder ButtonNub radius 1.5 mm height (front_in - 2.2 mm - nub_z + 0.2 mm) base (W/2, wheel_y, nub_z) axis Z join targets Button

// ==== Modules, for checking fit (not printed)
box RefDisplay size disp_w disp_h disp_t corner ((W - disp_w)/2, disp_y, front_in - disp_t) new -> body RefDisplay
box RefBoard size board_w board_w 1.6 mm corner (W/2 - board_w/2, wheel_y - board_w/2, board_top - 1.6 mm) new -> body RefWheelBoard
for k from 0 to 3 {
  box RefSwitch{k} size 6 mm 6 mm switch_h corner (W/2 + nub_r*cos(k*90 deg) - 3 mm, wheel_y + nub_r*sin(k*90 deg) - 3 mm, board_top) join targets RefWheelBoard
}
box RefSwitchC size 6 mm 6 mm switch_h corner (W/2 - 3 mm, wheel_y - 3 mm, board_top) join targets RefWheelBoard
box RefXiao size xiao_w xiao_l 4.4 mm corner (W/2 - xiao_w/2, wall + 0.3 mm, floor_t) new -> body RefXiao
box RefBattery size bat_w bat_l bat_t corner (8 mm + fit, disp_y, floor_t) new -> body RefBattery
box RefDfplayer size dfp_w 20.4 mm 5 mm corner (36 mm + fit, disp_y + 2 mm, floor_t) new -> body RefDfplayer
box RefJack size 6 mm 12 mm 5 mm corner (W - 16 mm - 3 mm, H - wall - 12.2 mm, floor_t + 1.1 mm) new -> body RefJack

color Back "#c9ccd0"
color Front, Button "#f4f4f2"
color Wheel "#ecebe7"
color RefDisplay "#16181c"
color RefWheelBoard, RefXiao, RefDfplayer "#2f6f3a"
color RefBattery "#c8a13a"
color RefJack "#222222"
material Front, Back, Wheel, Button PLA
