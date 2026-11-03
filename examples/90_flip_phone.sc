// 90. Printable flip phone (RAZR-style shape): an ESP32-C3 handset with an 18-key keypad, a 2.0 in
// TFT inside the lid and a 0.91 in OLED on its outer face, on a pin hinge.
//
// Bill of materials (measure yours; every module size is a parameter):
//   ESP32-C3 SuperMini            22.5 x 18 mm, USB-C on one short end
//   2.0 in ST7789 IPS TFT module  35.5 x 45 x 4.5 mm (header not fitted, wires soldered)
//   0.91 in SSD1306 OLED, 128x32  38 x 12 x 3 mm
//   LiPo 402535 (300 mAh)         4 x 25 x 35 mm, with the SuperMini's own charger
//   5 x 7 cm perfboard            1.6 mm, four 3 mm corner holes 2.5 mm in from the edges
//   18 tactile switches 6 x 6 x 4.3 mm, on the perfboard's 0.1 in grid
//   M2 screws: 4 x 12 mm (base), 4 x 6 mm (lid); one 1.75 mm PLA filament pin, 54 mm
//   about 10 strands of 30 AWG silicone wire from the lid to the base, through the hinge gap
//
// Z is up and the phone lies on its back. X runs across it (centred on x = 0), Y from the keypad
// end (y = 0) to the hinge.
// Base: a tray (floor and walls, wall thick) carries the SuperMini on the floor at the keypad end
// with its USB-C through the end wall, the LiPo beside it, and four standoffs for the perfboard.
// The keys sit on a 12.7 x 10.16 mm grid (5 x 4 holes of the perfboard), so the switches solder
// straight in. A TPU key mat, one piece, lies under the face plate: a web too wide to pass the
// holes, the caps standing up through them with `clear` all round, and a nub under each cap on
// its switch. The face plate's four posts reach down to the board, and M2 x 12 screws from the
// back (counterbored flush) clamp tray, board and plate together. Trim the switch legs flush
// under the board: the battery lies 1 mm below it.
// Lid: a bezel plate (the inner face) with the TFT window and a locating frame, and a tray (the
// outer face) with the OLED window, held by M2 x 6 screws from the outer face into the bezel's
// posts. Pads on the bezel press the OLED against its window.
// Hinge: two knuckles on the base and one on the lid, each on a web that runs to the half's outer
// face so it prints standing on the bed, on the parting plane at the back, on a
// 1.75 mm filament pin. The lid's knuckle bore is pin_fit, a little tight, so the lid holds
// where it is put. The wires pass in the 3 mm gap between the lid knuckle and the right base
// knuckle, through notches in both end walls. The lid is built closed, then turned about the pin
// (move the axis to the X axis, rotate by open_angle, move it back), so open_angle stays live.
// The Esp32, Battery, Board, Tft and Oled bodies stand in for the bought parts and are left out
// of the print layouts.
//
// Print (examples/90_flip_phone.print.json): PLA, 0.2 mm layers, no supports. The trays print
// open side up, the face plate and bezel face down, the key mat in TPU 95A caps down.
design FlipPhone units mm

param W = 56 mm
param y_end = 77 mm            // end of the trays at the hinge
param corner_r = 7 mm
param wall = 1.6 mm
param gap = 0.4 mm             // between the halves, closed
param plate_t = 1.6 mm
param clear = 0.25 mm          // key cap to face plate hole, each side
param open_angle = 160 deg     // 0 closed

// Electronics
param so_h = 5 mm              // board standoffs, floor to board
param board_t = 1.6 mm
param board_l = 70 mm
param board_w = 50 mm
param board_y0 = 5 mm
param hole_in = 2.5 mm         // board corner holes, in from each edge
param sw_h = 4.3 mm            // switch height above the board
param esp_l = 22.5 mm
param esp_w = 18 mm
param bat_l = 35 mm
param bat_w = 25 mm
param bat_y0 = 28 mm
param tft_w = 35.5 mm
param tft_l = 45 mm
param tft_t = 4.5 mm
param tft_y0 = 8 mm
param tft_view_w = 31 mm
param tft_view_l = 41 mm
param oled_w = 38 mm
param oled_l = 12 mm
param oled_t = 3 mm
param oled_y = 64 mm
param oled_view_w = 24 mm
param oled_view_l = 7 mm

// Keys: a 3 x 6 grid on the board's 0.1 in pitch
param key_px = 12.7 mm
param key_py = 10.16 mm
param key_w = 11 mm
param key_h = 8.4 mm
param row0_y = 12 mm
param key_up = 1.2 mm          // caps above the face plate
param web_t = 0.8 mm

// Hinge
param knuckle_r = 4 mm
param pin_d = 1.75 mm
param pin_fit = 1.82 mm        // lid knuckle bore: snug, for friction
param pin_free = 2.0 mm        // base knuckle bores

let board_z = wall + so_h
let sw_top = board_z + board_t + sw_h
let web_z = sw_top + 0.8 mm             // under the web; nubs reach 0.1 mm short of the switches
let tray_h = web_z + web_t              // the face plate sits on the tray's rim here
let base_t = tray_h + plate_t
let lid_z0 = base_t + gap               // bezel's inner face
let lid_tray_z = lid_z0 + plate_t       // lid tray's rim
let lid_h = wall + tft_t + 0.2 mm
let lid_top = lid_tray_z + lid_h
let zc = base_t + gap/2                 // pin axis, on the parting plane
let hy = y_end + knuckle_r + 0.4 mm
let hx = board_w/2 - hole_in
let hy0 = board_y0 + hole_in
let hy1 = board_y0 + board_l - hole_in
let lx = W/2 - wall - 2.9 mm            // lid screw posts
let ly0 = wall + 3.6 mm
let ly1 = y_end - wall - 3.4 mm

// ==== Base tray
sketch BaseSk on XY {
  rect rounded o = (-W/2, 0) (W/2, y_end) radius corner_r
  fix o
}
extrude BaseBlock from BaseSk distance tray_h new -> body BaseTray
shell BaseCavity faces (0, y_end/2, tray_h) thickness wall inside body BaseTray
fillet BaseBottom edges (0, 0, 0) radius 1.2 mm body BaseTray

// Standoffs for the board, with M2 clearance and a counterbore from the back
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder Standoff{i}{j} radius 3 mm height (so_h + 0.8 mm) base ((2*i - 1)*hx, hy0 + j*(hy1 - hy0), wall - 0.8 mm) axis (0,0,1) join targets BaseTray
    cylinder StandoffHole{i}{j} radius 1.1 mm height (board_z + 1 mm) base ((2*i - 1)*hx, hy0 + j*(hy1 - hy0), -0.5 mm) axis (0,0,1) cut targets BaseTray
    cylinder Counterbore{i}{j} radius 2.1 mm height 1.9 mm base ((2*i - 1)*hx, hy0 + j*(hy1 - hy0), -0.5 mm) axis (0,0,1) cut targets BaseTray
  }
}
// Locating rails for the SuperMini and the battery, and the USB-C port in the keypad end
box EspRailL size 1 mm (esp_l - 2 mm) 1.5 mm corner (-esp_w/2 - 1.3 mm, wall + 1 mm, wall - 0.3 mm) join targets BaseTray
box EspRailR size 1 mm (esp_l - 2 mm) 1.5 mm corner (esp_w/2 + 0.3 mm, wall + 1 mm, wall - 0.3 mm) join targets BaseTray
box BatRailL size 1 mm (bat_l - 6 mm) 1.5 mm corner (-bat_w/2 - 1.3 mm, bat_y0 + 3 mm, wall - 0.3 mm) join targets BaseTray
box BatRailR size 1 mm (bat_l - 6 mm) 1.5 mm corner (bat_w/2 + 0.3 mm, bat_y0 + 3 mm, wall - 0.3 mm) join targets BaseTray
sketch UsbSk on XZ offset (-0.5 mm) {
  slot u = centers (-3 mm, -(wall + 2.6 mm)) (3 mm, -(wall + 2.6 mm)) width 3.8 mm
  fix u
}
extrude UsbPort from UsbSk distance (wall + 1 mm) cut targets BaseTray
// Wire notch in the hinge-end wall, over the hinge gap
box BaseWireNotch size 3.4 mm (wall + 1 mm) 2.2 mm corner (8.2 mm, y_end - wall - 0.5 mm, tray_h - 2.2 mm) cut targets BaseTray

// Base knuckles: two, on webs from the end wall that stand on the bed (no supports)
box BaseWebL size 16.2 mm (hy - y_end + 1.4 mm) (tray_h - 0.11 mm) corner (-W/2 + 3.3 mm, y_end - 1.4 mm, 0.01 mm) join targets BaseTray
box BaseWebR size 12.8 mm (hy - y_end + 1.4 mm) (tray_h - 0.11 mm) corner (11.9 mm, y_end - 1.4 mm, 0.01 mm) join targets BaseTray
cylinder BaseKnuckleL radius knuckle_r height 16.8 mm base (-W/2 + 3 mm, hy, zc) axis (1,0,0) join targets BaseTray
cylinder BaseKnuckleR radius knuckle_r height 13.4 mm base (11.6 mm, hy, zc) axis (1,0,0) join targets BaseTray
cylinder BasePinBore radius (pin_free/2) height (W - 4 mm) base (-W/2 + 2 mm, hy, zc) axis (1,0,0) cut targets BaseTray

// ==== Face plate, with the key holes and four posts down to the board
sketch PlateSk on XY offset tray_h {
  rect rounded o = (-W/2, 0) (W/2, y_end) radius corner_r
  fix o
}
extrude PlateSlab from PlateSk distance plate_t new -> body FacePlate
fillet PlateTop edges (0, 0, base_t) radius 0.8 mm body FacePlate
sketch KeyHoleSk on XY offset tray_h {
  rect k = (-key_px - key_w/2 - clear, row0_y - key_h/2 - clear) (-key_px + key_w/2 + clear, row0_y + key_h/2 + clear)
  fix k
  pattern rectangular k count 3 spacing key_px count2 6 spacing2 key_py
}
extrude KeyHoles from KeyHoleSk distance (plate_t + 1 mm) cut targets FacePlate
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder Post{i}{j} radius 2.4 mm height (tray_h - board_z - board_t - 0.1 mm + 0.5 mm) base ((2*i - 1)*hx, hy0 + j*(hy1 - hy0), board_z + board_t + 0.1 mm) axis (0,0,1) join targets FacePlate
    cylinder PostPilot{i}{j} radius 0.8 mm height 5.5 mm base ((2*i - 1)*hx, hy0 + j*(hy1 - hy0), board_z + board_t) axis (0,0,1) cut targets FacePlate
  }
}

// ==== Key mat (TPU): web under the plate, caps up through it, a nub down onto each switch
sketch WebSk on XY offset web_z {
  rect rounded w = (-hx + 2.6 mm, row0_y - key_h/2 - 1.5 mm) (hx - 2.6 mm, row0_y + 5*key_py + key_h/2 + 1.5 mm) radius 2 mm
  fix w
}
extrude Web from WebSk distance web_t new -> body KeyMat
sketch CapSk on XY offset (tray_h - 0.1 mm) {
  rect rounded k = (-key_px - key_w/2, row0_y - key_h/2) (-key_px + key_w/2, row0_y + key_h/2) radius 1.5 mm
  fix k
  pattern rectangular k count 3 spacing key_px count2 6 spacing2 key_py
}
extrude Caps from CapSk distance (plate_t + key_up + 0.1 mm) join targets KeyMat
sketch NubSk on XY offset (web_z + 0.1 mm) {
  circle n = (-key_px, row0_y) radius 1.5 mm
  fix n
  pattern rectangular n count 3 spacing key_px count2 6 spacing2 key_py
}
extrude Nubs from NubSk distance 0.8 mm negative join targets KeyMat

// ==== Lid bezel: TFT window, locating frame, screw posts and OLED pads
sketch BezelSk on XY offset lid_z0 {
  rect rounded o = (-W/2, 0) (W/2, y_end) radius corner_r
  fix o
}
extrude BezelSlab from BezelSk distance plate_t new -> body Bezel
fillet BezelEdge edges (0, 0, lid_z0) radius 0.8 mm body Bezel
sketch TftViewSk on XY offset lid_z0 {
  rect rounded v = (-tft_view_w/2, tft_y0 + tft_l/2 - tft_view_l/2) (tft_view_w/2, tft_y0 + tft_l/2 + tft_view_l/2) radius 1 mm
  fix v
}
extrude TftView from TftViewSk distance (plate_t + 1 mm) offset (-0.5 mm) cut targets Bezel
sketch TftFrameSk on XY offset (lid_tray_z - 0.1 mm) {
  rect outer = (-tft_w/2 - 1.3 mm, tft_y0 - 1.3 mm) (tft_w/2 + 1.3 mm, tft_y0 + tft_l + 1.3 mm)
  rect inner = (-tft_w/2 - 0.3 mm, tft_y0 - 0.3 mm) (tft_w/2 + 0.3 mm, tft_y0 + tft_l + 0.3 mm)
  fix outer inner
}
extrude TftFrame from TftFrameSk distance 1.6 mm profiles (tft_w/2 + 0.8 mm, tft_y0 + tft_l/2) join targets Bezel
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder LidPost{i}{j} radius 2.4 mm height (tft_t + 0.2 mm - 1.6 mm + 0.1 mm) base ((2*i - 1)*lx, ly0 + j*(ly1 - ly0), lid_tray_z - 0.1 mm) axis (0,0,1) join targets Bezel
    cylinder LidPostPilot{i}{j} radius 0.8 mm height 4 mm base ((2*i - 1)*lx, ly0 + j*(ly1 - ly0), lid_tray_z) axis (0,0,1) cut targets Bezel
  }
}
box OledPadL size 2.5 mm 6 mm (lid_h - wall - oled_t - 0.1 mm + 0.1 mm) corner (-oled_w/2 + 0.5 mm, oled_y - 3 mm, lid_tray_z - 0.1 mm) join targets Bezel
box OledPadR size 2.5 mm 6 mm (lid_h - wall - oled_t - 0.1 mm + 0.1 mm) corner (oled_w/2 - 3 mm, oled_y - 3 mm, lid_tray_z - 0.1 mm) join targets Bezel

// ==== Lid tray: the outer face, with the OLED window, screw bosses and the lid knuckle
sketch LidSk on XY offset lid_tray_z {
  rect rounded o = (-W/2, 0) (W/2, y_end) radius corner_r
  fix o
}
extrude LidBlock from LidSk distance lid_h new -> body LidTray
shell LidCavity faces (0, y_end/2, lid_tray_z) thickness wall inside body LidTray
fillet LidTopEdge edges (0, 0, lid_top) radius 1.2 mm body LidTray
sketch OledViewSk on XY offset lid_top {
  rect rounded v = (-oled_view_w/2, oled_y - oled_view_l/2) (oled_view_w/2, oled_y + oled_view_l/2) radius 0.8 mm
  fix v
}
extrude OledView from OledViewSk distance (wall + 1 mm) offset 0.5 mm negative cut targets LidTray
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder LidBoss{i}{j} radius 2.4 mm height 1.6 mm base ((2*i - 1)*lx, ly0 + j*(ly1 - ly0), lid_top - wall - 1.5 mm) axis (0,0,1) join targets LidTray
    cylinder LidBossHole{i}{j} radius 1.1 mm height (wall + 3 mm) base ((2*i - 1)*lx, ly0 + j*(ly1 - ly0), lid_top - wall - 2 mm) axis (0,0,1) cut targets LidTray
    cylinder LidCounterbore{i}{j} radius 2.1 mm height 1.9 mm base ((2*i - 1)*lx, ly0 + j*(ly1 - ly0), lid_top - 1.4 mm) axis (0,0,1) cut targets LidTray
  }
}
box LidWireNotch size 3.4 mm (wall + 1 mm) 2.2 mm corner (8.2 mm, y_end - wall - 0.5 mm, lid_tray_z) cut targets LidTray
box LidWeb size 15.0 mm (hy - y_end + 1.4 mm) (lid_top - lid_tray_z - 0.11 mm) corner (-7.5 mm, y_end - 1.4 mm, lid_tray_z + 0.1 mm) join targets LidTray
cylinder LidKnuckle radius knuckle_r height 15.6 mm base (-7.8 mm, hy, zc) axis (1,0,0) join targets LidTray
cylinder LidPinBore radius (pin_fit/2) height 17 mm base (-8.5 mm, hy, zc) axis (1,0,0) cut targets LidTray

// ==== Stand-ins for the bought parts (not printed)
box Esp32 size esp_w esp_l 4.2 mm corner (-esp_w/2, wall + 0.3 mm, wall) new -> body Esp32
box Battery size bat_w bat_l 4 mm corner (-bat_w/2, bat_y0, wall) new -> body Battery
box BoardPcb size board_w board_l board_t corner (-board_w/2, board_y0, board_z) new -> body Board
box Tft size tft_w tft_l tft_t corner (-tft_w/2, tft_y0, lid_tray_z) new -> body Tft
box Oled size oled_w oled_l oled_t corner (-oled_w/2, oled_y - oled_l/2, lid_top - wall - oled_t) new -> body Oled

// ==== Open the lid about the pin
move HingeIn bodies Bezel, LidTray, Tft, Oled to (0, -hy, -zc)
move Open bodies Bezel, LidTray, Tft, Oled axis (-1,0,0) angle open_angle
move HingeOut bodies Bezel, LidTray, Tft, Oled to (0, hy, zc)

color BaseTray, FacePlate, Bezel, LidTray "#b7bcc3"
color KeyMat "#2f5d9e"
color Esp32, Board "#1f6f43"
color Battery "#c9b037"
color Tft, Oled "#101418"
material BaseTray, FacePlate, Bezel, LidTray PLA
