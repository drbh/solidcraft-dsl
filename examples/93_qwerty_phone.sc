// 93. QWERTY phone in the BlackBerry Curve style, printable: an ESP32-S3 pocket terminal with a real keyboard.
//
// The phone lies face up: X across (0 at the left edge), Y up the face (0 at the bottom edge with the
// USB-C port, H at the top), Z out of the face (0 at the back, T at the face).
//
// Bill of materials (module sizes are params; check them against yours):
//   1  Seeed XIAO ESP32-S3 (21 x 17.8 mm), its USB-C at the bottom edge; it charges the LiPo
//   1  2.4 in ILI9341 / ST7789 320 x 240 SPI display, landscape (60 x 42.7 mm board, 49 x 37 view)
//   1  BlackBerry Q10 keyboard on an I2C breakout (BBQ10KB style), 60 x 28 mm, 5 mm deep, its
//      keys standing through the keyboard window
//   1  trackball breakout (Pimoroni style), 20 x 20 mm board, 10 mm ball
//   4  6 x 6 x 5 mm tactile switches, two each on two 18 x 12 mm perfboard pieces: call, menu,
//      back, end
//   1  503450 LiPo (5 x 34 x 50 mm, about 1000 mAh), under the display
//   1  SS12D00 mini slide switch (power), in the right side
//   4  M2 x 10 screws close the case; 4 M2 x 5 hold the key boards
//
// How it goes together. The case is one rounded block, hollowed and split at z = split_z into the
// Front (face, with the screen, keyboard, trackball and key openings, and posts and ribs under it)
// and the Back (with the USB-C and switch openings and the bays). The four nav caps drop in from
// inside, held by their flanges, and their two boards screw to the Front's posts behind them. The
// display, keyboard and trackball sit against the face's underside between ribs (foam tape behind
// them). Four M2 screws from the back close the case.
//
// Printing, no supports: the Front face down, the Back open side up, the caps face down.
design QwertyPhone units mm

// Case
param W = 66 mm
param H = 112 mm
param T = 15 mm
param corner_r = 8 mm
param wall = 2 mm
param floor_t = 2 mm
param back_round = 2.5 mm
param split_z = 7 mm
param fit = 0.25 mm

// Display
param disp_w = 60 mm
param disp_h = 42.7 mm
param disp_t = 4 mm
param view_w = 49 mm
param view_h = 37 mm
param disp_y = 60.5 mm         // the display board's lower edge

// Keyboard
param kb_w = 60 mm
param kb_h = 28 mm
param kb_y = 9 mm              // the keyboard board's lower edge
param kb_win_w = 58 mm
param kb_win_h = 24 mm

// Trackball and nav keys
param tb_size = 20 mm
param tb_ball = 10 mm
param nav_d = 6 mm
param switch_h = 5 mm

// Battery
param bat_w = 34 mm
param bat_l = 50 mm

let front_in = T - floor_t
let flange_z = front_in - 1 mm
let board_top = flange_z - 0.2 mm - switch_h
let tb_y = kb_y + kb_h + 0.5 mm + tb_size/2   // trackball centre
let nav_y = tb_y
let usb_z = floor_t + 2.8 mm
let boss_x = 6.5 mm
let boss_lo = 5 mm
let boss_hi = H - 5 mm

// ==== Case: one rounded block, hollowed, its openings cut, then split into Front and Back
sketch Outline on XY {
  rect rounded o = (0,0) (W,H) radius corner_r
  fix o
}
extrude Block from Outline distance T new -> body Front
fillet BackRound edges (W/2, 0, 0) radius back_round body Front
fillet FaceEdge edges (W/2, 0, T) radius 2 mm body Front
sketch CavitySk on XY offset floor_t {
  rect rounded c = (wall, wall) (W - wall, H - wall) radius (corner_r - wall)
  fix c
}
extrude Cavity from CavitySk distance (front_in - floor_t) cut targets Front

// The face: screen, keyboard and trackball windows, nav key holes, earpiece slot
sketch FaceHolesSk on XY offset (T + 1 mm) {
  rect rounded v = ((W - view_w)/2, disp_y + (disp_h - view_h)/2) ((W + view_w)/2, disp_y + (disp_h + view_h)/2) radius 1.5 mm
  rect rounded k = ((W - kb_win_w)/2, kb_y + (kb_h - kb_win_h)/2) ((W + kb_win_w)/2, kb_y + (kb_h + kb_win_h)/2) radius 3 mm
  circle tb = (W/2, tb_y) radius (tb_ball/2 + 1 mm)
  circle n1 = (9 mm, nav_y) radius (nav_d/2 + fit)
  circle n2 = (17.5 mm, nav_y) radius (nav_d/2 + fit)
  circle n3 = (W - 17.5 mm, nav_y) radius (nav_d/2 + fit)
  circle n4 = (W - 9 mm, nav_y) radius (nav_d/2 + fit)
  slot e = centers (W/2 - 6 mm, H - 4.5 mm) (W/2 + 6 mm, H - 4.5 mm) width 1.6 mm
  fix v k tb n1 n2 n3 n4 e
}
extrude FaceHoles from FaceHolesSk distance (floor_t + 2 mm) negative cut targets Front

// The edges: USB-C at the bottom, power switch in the right side
sketch UsbSk on XZ offset (-1 mm) {
  slot u = centers (W/2 - 3.2 mm, -usb_z) (W/2 + 3.2 mm, -usb_z) width 3.6 mm
  fix u
}
extrude UsbPort from UsbSk distance (wall + 2 mm) cut targets Front
box PowerSlot size (wall + 2 mm) 5.2 mm 2 mm corner (W - wall - 1 mm, 77.4 mm, floor_t + 2 mm) cut targets Front

split Halves target Front origin (0, 0, split_z) normal (0, 0, 1) -> body Back

// ==== Back: screw bosses (M2 clearance, counterbored), bays for the XIAO and the LiPo
for k from 0 to 3 {
  cylinder BackBoss{k} radius 2.8 mm height (split_z - floor_t + 0.5 mm) base (boss_x + mod(k, 2)*(W - 2*boss_x), boss_lo + floor(k/2)*(boss_hi - boss_lo), floor_t - 0.5 mm) axis Z join targets Back
  cylinder BackHole{k} radius 1.15 mm height (split_z + 2 mm) base (boss_x + mod(k, 2)*(W - 2*boss_x), boss_lo + floor(k/2)*(boss_hi - boss_lo), -1 mm) axis Z cut targets Back
  cylinder BackHead{k} radius 2.1 mm height 2.4 mm base (boss_x + mod(k, 2)*(W - 2*boss_x), boss_lo + floor(k/2)*(boss_hi - boss_lo), -1 mm) axis Z cut targets Back
}
box XiaoRibL size 1.2 mm 21 mm 2 mm corner (W/2 - 8.9 mm - fit - 1.2 mm, wall + 0.2 mm, floor_t - 0.2 mm) join targets Back
box XiaoRibR size 1.2 mm 21 mm 2 mm corner (W/2 + 8.9 mm + fit, wall + 0.2 mm, floor_t - 0.2 mm) join targets Back
box BatRibL size 1.2 mm (bat_l - 10 mm) 2 mm corner (W/2 - bat_w/2 - fit - 1.2 mm, disp_y - 3 mm + 5 mm, floor_t - 0.2 mm) join targets Back
box BatRibR size 1.2 mm (bat_l - 10 mm) 2 mm corner (W/2 + bat_w/2 + fit, disp_y - 3 mm + 5 mm, floor_t - 0.2 mm) join targets Back

// ==== Front: case-screw bosses, posts for the nav boards, ribs locating the modules
for k from 0 to 3 {
  cylinder FrontBoss{k} radius 2.8 mm height (front_in - split_z + 0.5 mm) base (boss_x + mod(k, 2)*(W - 2*boss_x), boss_lo + floor(k/2)*(boss_hi - boss_lo), split_z) axis Z join targets Front
  cylinder FrontPilot{k} radius 0.8 mm height 6 mm base (boss_x + mod(k, 2)*(W - 2*boss_x), boss_lo + floor(k/2)*(boss_hi - boss_lo), split_z - 0.1 mm) axis Z cut targets Front
}
// Nav boards: one post at each board's outer end, above the end caps' flanges
cylinder NavPostL radius 2.2 mm height (front_in - board_top + 0.5 mm) base (wall + 2.6 mm, nav_y + 4.5 mm, board_top) axis Z join targets Front
cylinder NavPostR radius 2.2 mm height (front_in - board_top + 0.5 mm) base (W - wall - 2.6 mm, nav_y + 4.5 mm, board_top) axis Z join targets Front
cylinder NavPilotL radius 0.8 mm height 5 mm base (wall + 2.6 mm, nav_y + 4.5 mm, board_top - 0.1 mm) axis Z cut targets Front
cylinder NavPilotR radius 0.8 mm height 5 mm base (W - wall - 2.6 mm, nav_y + 4.5 mm, board_top - 0.1 mm) axis Z cut targets Front
// Ribs: under the display's top edge, beside the keyboard, round the trackball board
box DispRib size 30 mm 1.2 mm 3 mm corner (W/2 - 15 mm, disp_y + disp_h + fit, front_in - 3 mm) join targets Front
box KbRibL size 1.2 mm 14 mm 3 mm corner ((W - kb_w)/2 - fit - 1.2 mm, kb_y + 7 mm, front_in - 3 mm) join targets Front
box KbRibR size 1.2 mm 14 mm 3 mm corner ((W + kb_w)/2 + fit, kb_y + 7 mm, front_in - 3 mm) join targets Front
box TbRibL size 1.2 mm 12 mm 3 mm corner (W/2 - tb_size/2 - fit - 1.2 mm, tb_y - 6 mm, front_in - 3 mm) join targets Front
box TbRibR size 1.2 mm 12 mm 3 mm corner (W/2 + tb_size/2 + fit, tb_y - 6 mm, front_in - 3 mm) join targets Front

// ==== Nav caps: call, menu, back, end, each with a flange under the face
cylinder Nav1 radius (nav_d/2) height (T + 0.8 mm - flange_z) base (9 mm, nav_y, flange_z) axis Z new -> body Call
cylinder Nav1F radius 3.8 mm height 1 mm base (9 mm, nav_y, flange_z) axis Z join targets Call
cylinder Nav2 radius (nav_d/2) height (T + 0.8 mm - flange_z) base (17.5 mm, nav_y, flange_z) axis Z new -> body Menu
cylinder Nav2F radius 3.8 mm height 1 mm base (17.5 mm, nav_y, flange_z) axis Z join targets Menu
cylinder Nav3 radius (nav_d/2) height (T + 0.8 mm - flange_z) base (W - 17.5 mm, nav_y, flange_z) axis Z new -> body BackKey
cylinder Nav3F radius 3.8 mm height 1 mm base (W - 17.5 mm, nav_y, flange_z) axis Z join targets BackKey
cylinder Nav4 radius (nav_d/2) height (T + 0.8 mm - flange_z) base (W - 9 mm, nav_y, flange_z) axis Z new -> body EndKey
cylinder Nav4F radius 3.8 mm height 1 mm base (W - 9 mm, nav_y, flange_z) axis Z join targets EndKey

// ==== Modules, for checking fit (not printed)
box RefDisplay size disp_w disp_h disp_t corner ((W - disp_w)/2, disp_y, front_in - disp_t) new -> body RefDisplay
box RefKeyboard size kb_w kb_h 5 mm corner ((W - kb_w)/2, kb_y, T + 0.3 mm - 5 mm) new -> body RefKeyboard
box RefTrackball size tb_size tb_size 6 mm corner (W/2 - tb_size/2, tb_y - tb_size/2, front_in - 6 mm) new -> body RefTrackball
sphere RefBall radius (tb_ball/2) center (W/2, tb_y, T + 1.5 mm - tb_ball/2) new -> body RefBall
box RefNavL size 18 mm 12 mm 1.6 mm corner (wall + 0.5 mm, nav_y - 6 mm, board_top - 1.6 mm) new -> body RefNavL
box RefNavR size 18 mm 12 mm 1.6 mm corner (W - wall - 18.5 mm, nav_y - 6 mm, board_top - 1.6 mm) new -> body RefNavR
box RefXiao size 17.8 mm 21 mm 4.4 mm corner (W/2 - 8.9 mm, wall + 0.3 mm, floor_t) new -> body RefXiao
box RefBattery size bat_w bat_l 5 mm corner (W/2 - bat_w/2, disp_y - 3 mm, floor_t) new -> body RefBattery

color Front "#1d1e21"
color Back "#9aa0a8"
color Call "#2f8f46"
color EndKey "#b8322b"
color Menu, BackKey "#2a2b2e"
color RefDisplay, RefKeyboard "#16181c"
color RefBall "#d9dde3"
color RefTrackball, RefNavL, RefNavR, RefXiao "#2f6f3a"
color RefBattery "#c8a13a"
material Front, Back, Call, Menu, BackKey, EndKey PLA
