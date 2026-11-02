// 95. Pocket camcorder in the late-2000s one-button style, printable and built round a XIAO ESP32S3 Sense.
//
// The camcorder lies on its back: X across its width (0 to W), Y up its length (y = 0 the bottom
// end, y = L the top end with the lens), Z out of the front (z = 0 the back, z = T the front face).
//
// Bill of materials
//   1  Seeed XIAO ESP32S3 Sense (21 x 17.8 mm, 15 mm tall with the camera board and OV2640)
//   1  2.0 in ST7789 240x320 SPI IPS module, board about 34.5 x 56 x 4 mm (measure yours: tft_*)
//   1  503035 LiPo, 500 mAh, 5 x 30 x 35 mm, on the XIAO's BAT pads (it charges over the USB-C)
//   1  12 x 12 x 4.3 mm tactile switch (record) and 6 x 6 x 5 mm tactile switches (6)
//   1  perfboard 40 x 40 mm carrying the seven switches (the button board)
//   1  USB-A male PCB plug (12 x 4.5 x 13 mm shell, board tail 1.6 mm) wired to the XIAO's 5V/GND
//      for charging from a laptop; data stays on the XIAO's own USB-C in the top end
//   4  M2 x 12 self-tapping screws (case), 4 M2 x 6 (button board)
//   1  25 mm of 1.75 mm filament: the USB arm's hinge pin
//
// How it goes together
//   Back       the tray: the XIAO sits in a cradle at the top end with its camera against the
//              back wall, looking out through the lens hole; the LiPo sits in a rib pocket below
//   Front      the cover: the TFT sits in a rib frame behind the screen window, the button board
//              hangs from four standoffs behind the record ring, the XIAO's USB-C shows through
//              the top end. Four M2 screws from the back pull the two together.
//   Record, KeyUp/Down/Left/Right, KeyPlay, KeyDelete   button caps through the cover, each with a
//              flange inside so it can't fall out; they rest on their switches
//   UsbArm     swings out of the channel down the left side on a filament pin (usb_angle: 0 is
//              stowed, 90 deg sticks straight out sideways); the plug's board slides into its slot
//   LensRing   pressed into the recess round the lens hole
//   Xiao, Tft, Battery, ButtonBoard, UsbPlug   the bought parts, for checking fit (not printed)
//
// Printing (PLA or PETG, 0.2 mm layers, no supports): Front face down, Back back down, the caps
// flange down, UsbArm and LensRing as modelled. The bed-side edges are chamfered so they print.
design PocketCamcorder units mm

// Case
param W = 55 mm
param L = 100 mm
param T = 23 mm
param corner_r = 7 mm
param edge_c = 2.5 mm          // chamfer on the front and back edges
param wall = 1.8 mm
param split_z = 10 mm          // where the back tray meets the front cover
param fit = 0.25 mm            // clearance round caps, the arm and the modules
param pilot_d = 1.6 mm         // M2 self-tapping
param clear_d = 2.4 mm
param head_d = 4.2 mm
param boss_r = 2.75 mm

// XIAO ESP32S3 Sense, its USB-C toward the top end
param xiao_w = 17.8 mm
param xiao_l = 21 mm
param xiao_h = 15 mm
param lens_d = 9 mm            // hole for the OV2640's lens barrel

// TFT, its long side along Y
param tft_w = 34.5 mm
param tft_l = 56 mm
param tft_t = 4 mm
param view_w = 31.6 mm         // visible area plus a little
param view_l = 41.8 mm
param screen_top = 9 mm        // window's top edge from the top end

// LiPo
param bat_w = 30 mm
param bat_l = 35 mm
param bat_t = 5 mm

// Buttons
param pad_y = 27 mm            // centre of the record ring
param rec_r = 7 mm
param key_r = 2.4 mm
param ring_r = 11.5 mm         // the four small keys round the record cap
param key_out = 1.2 mm         // how far the caps stand proud of the face
param board_t = 1.6 mm

// USB arm in the left side
param usb_angle = 0 deg
param arm_l = 44 mm            // pivot to plug tip
param arm_y = 24 mm            // pivot height up the side
param chan_d = 5 mm            // channel depth into the side
param chan_h = 12.6 mm         // channel height (Z), round the 12 mm plug
param pin_d = 1.75 mm

let cx = W/2
let in_front = T - wall                           // inside face of the cover
let board_top = in_front - 1 mm - 5 mm            // flange 1 mm on a 5 mm-tall 6x6 switch
let board_y0 = 2.5 mm
let xiao_y = L - wall - 0.4 mm - xiao_l/2         // its USB-C end just inside the top wall
let xiao_top = wall + xiao_h
let tft_y = L - screen_top - view_l/2
let tft_z = in_front - tft_t
let chan_z = (T - chan_h)/2
let arm_x = chan_d/2

// ==== Case halves: the outline extruded, the outer edge chamfered, then shelled open at the split
sketch BackSk on XY {
  rect rounded o = (0,0) (W,L) radius corner_r
  fix o
}
extrude BackBlank from BackSk distance split_z new -> body Back
chamfer BackEdge edges (cx, 0, 0) distance edge_c body Back
shell BackCavity faces (cx, L/2, split_z) thickness wall inside body Back

sketch FrontSk on XY offset split_z {
  rect rounded o = (0,0) (W,L) radius corner_r
  fix o
}
extrude FrontBlank from FrontSk distance (T - split_z) new -> body Front
chamfer FrontEdge edges (cx, 0, T) distance edge_c body Front
shell FrontCavity faces (cx, L/2, split_z) thickness wall inside body Front

// The USB arm's channel down the left side: fill a block inside each half, then cut the channel
box ChannelFillBack size (chan_d + wall - 0.5 mm) (arm_l + 7 mm + 2*wall) (split_z - 0.6 mm) corner (0.5 mm, arm_y - 6 mm - wall, 0.5 mm) join targets Back
box ChannelFillFront size (chan_d + wall - 0.5 mm) (arm_l + 7 mm + 2*wall) (T - split_z - 0.6 mm) corner (0.5 mm, arm_y - 6 mm - wall, split_z + 0.1 mm) join targets Front
box ChannelBack size (chan_d + 1 mm) (arm_l + 7 mm) chan_h corner (-1 mm, arm_y - 6 mm, chan_z) cut targets Back
box ChannelFront size (chan_d + 1 mm) (arm_l + 7 mm) chan_h corner (-1 mm, arm_y - 6 mm, chan_z) cut targets Front
cylinder PinHoleBack diameter (pin_d + 0.35 mm) height (split_z + 1 mm) base (arm_x, arm_y, -0.5 mm) axis Z cut targets Back
cylinder PinHoleFront diameter (pin_d + 0.35 mm) height (T - split_z + 1 mm) base (arm_x, arm_y, split_z - 0.5 mm) axis Z cut targets Front
cylinder WireHole diameter 3 mm height (wall + 2 mm) base (chan_d - 0.5 mm, arm_y + 6 mm, split_z + 1.6 mm) axis X cut targets Front

// Screws: four bosses, pilots in the cover, clearance and head pockets from the back
sketch BossSk on XY offset (wall - 0.2 mm) {
  circle b0 = (4.2 mm, 8 mm) radius boss_r
  circle b1 = (W - 4.2 mm, 8 mm) radius boss_r
  circle b2 = (4.2 mm, 80 mm) radius boss_r
  circle b3 = (W - 4.2 mm, 80 mm) radius boss_r
  fix b0 b1 b2 b3
}
extrude BackBosses from BossSk distance (split_z - wall + 0.1 mm) join targets Back
extrude FrontBosses from BossSk distance (in_front - split_z + 0.1 mm) offset (split_z - wall + 0.3 mm) join targets Front
sketch ClearSk on XY offset (-1 mm) {
  circle c0 = (4.2 mm, 8 mm) diameter clear_d
  circle c1 = (W - 4.2 mm, 8 mm) diameter clear_d
  circle c2 = (4.2 mm, 80 mm) diameter clear_d
  circle c3 = (W - 4.2 mm, 80 mm) diameter clear_d
  fix c0 c1 c2 c3
}
extrude Clear from ClearSk distance (split_z + 2 mm) cut targets Back
sketch HeadSk on XY offset (-1 mm) {
  circle h0 = (4.2 mm, 8 mm) diameter head_d
  circle h1 = (W - 4.2 mm, 8 mm) diameter head_d
  circle h2 = (4.2 mm, 80 mm) diameter head_d
  circle h3 = (W - 4.2 mm, 80 mm) diameter head_d
  fix h0 h1 h2 h3
}
extrude Heads from HeadSk distance 3.6 mm cut targets Back
sketch PilotSk on XY offset (split_z - 0.1 mm) {
  circle p0 = (4.2 mm, 8 mm) diameter pilot_d
  circle p1 = (W - 4.2 mm, 8 mm) diameter pilot_d
  circle p2 = (4.2 mm, 80 mm) diameter pilot_d
  circle p3 = (W - 4.2 mm, 80 mm) diameter pilot_d
  fix p0 p1 p2 p3
}
extrude Pilots from PilotSk distance 9 mm cut targets Front

// ==== Back: lens hole and ring recess, XIAO cradle, LiPo pocket
sketch LensSk on XY offset (-1 mm) {
  circle h = (cx, xiao_y) diameter lens_d
  fix h
}
extrude LensHole from LensSk distance (wall + 2 mm) cut targets Back
cylinder LensRecess radius 7.1 mm height 2 mm base (cx, xiao_y, -1 mm) axis Z cut targets Back
sketch CradleSk on XY offset (wall - 0.2 mm) {
  rect l = (cx - xiao_w/2 - fit - 1.2 mm, xiao_y - xiao_l/2 + 1 mm) (cx - xiao_w/2 - fit, xiao_y + xiao_l/2 - 1 mm)
  rect r = (cx + xiao_w/2 + fit, xiao_y - xiao_l/2 + 1 mm) (cx + xiao_w/2 + fit + 1.2 mm, xiao_y + xiao_l/2 - 1 mm)
  rect e = (cx - xiao_w/2 + 2 mm, xiao_y - xiao_l/2 - fit - 1.2 mm) (cx + xiao_w/2 - 2 mm, xiao_y - xiao_l/2 - fit)
  fix l r e
}
extrude Cradle from CradleSk distance 6 mm join targets Back
sketch BatSk on XY offset (wall - 0.2 mm) {
  rect l = (cx - bat_w/2 - fit - 1.2 mm, 8 mm) (cx - bat_w/2 - fit, bat_l + 2 mm)
  rect r = (cx + bat_w/2 + fit, 8 mm) (cx + bat_w/2 + fit + 1.2 mm, bat_l + 2 mm)
  rect t = (cx - bat_w/2 + 3 mm, 5 mm + bat_l + fit) (cx + bat_w/2 - 3 mm, 5 mm + bat_l + fit + 1.2 mm)
  fix l r t
}
extrude BatRibs from BatSk distance 3.5 mm join targets Back

// ==== Front: screen window, TFT frame, USB-C port, button board standoffs and cap holes
sketch WindowSk on XY offset (in_front - 0.5 mm) {
  rect rounded w = (cx - view_w/2, tft_y - view_l/2) (cx + view_w/2, tft_y + view_l/2) radius 1.5 mm
  fix w
}
extrude Window from WindowSk distance (wall + 1 mm) cut targets Front
sketch TftRibSk on XY offset (in_front - 3 mm) {
  rect l = (cx - tft_w/2 - fit - 1.2 mm, tft_y - tft_l/2 + 2 mm) (cx - tft_w/2 - fit, tft_y + tft_l/2 - 2 mm)
  rect r = (cx + tft_w/2 + fit, tft_y - tft_l/2 + 2 mm) (cx + tft_w/2 + fit + 1.2 mm, tft_y + tft_l/2 - 2 mm)
  rect e = (cx - tft_w/2 + 3 mm, tft_y - tft_l/2 - fit - 1.2 mm) (cx + tft_w/2 - 3 mm, tft_y - tft_l/2 - fit)
  fix l r e
}
extrude TftRibs from TftRibSk distance 3.3 mm join targets Front
box UsbC size 10 mm (wall + 2 mm) 4.2 mm corner (cx - 5 mm, L - wall - 1 mm, xiao_top - 1.6 mm - 2.1 mm) cut targets Front

sketch StandoffSk on XY offset board_top {
  circle s0 = (cx - 17.5 mm, 13 mm) radius 2.6 mm
  circle s1 = (cx + 17.5 mm, 13 mm) radius 2.6 mm
  circle s2 = (cx - 17.5 mm, 40 mm) radius 2.6 mm
  circle s3 = (cx + 17.5 mm, 40 mm) radius 2.6 mm
  fix s0 s1 s2 s3
}
extrude Standoffs from StandoffSk distance (in_front - board_top + 0.2 mm) join targets Front
sketch StandoffPilotSk on XY offset (board_top - 0.1 mm) {
  circle p0 = (cx - 17.5 mm, 13 mm) diameter pilot_d
  circle p1 = (cx + 17.5 mm, 13 mm) diameter pilot_d
  circle p2 = (cx - 17.5 mm, 40 mm) diameter pilot_d
  circle p3 = (cx + 17.5 mm, 40 mm) diameter pilot_d
  fix p0 p1 p2 p3
}
extrude StandoffPilots from StandoffPilotSk distance 5 mm cut targets Front

sketch KeyHoleSk on XY offset (in_front - 1 mm) {
  circle rec = (cx, pad_y) radius (rec_r + fit)
  circle k0 = (cx, pad_y + ring_r) radius (key_r + fit)
  circle k1 = (cx, pad_y - ring_r) radius (key_r + fit)
  circle k2 = (cx + ring_r, pad_y) radius (key_r + fit)
  circle k3 = (cx - ring_r, pad_y) radius (key_r + fit)
  circle k4 = (cx - 12 mm, 6.5 mm) radius (key_r + fit)
  circle k5 = (cx + 12 mm, 6.5 mm) radius (key_r + fit)
  fix rec k0 k1 k2 k3 k4 k5
}
extrude KeyHoles from KeyHoleSk distance (wall + 2 mm) cut targets Front
// The ring round the record cap and its marks, sunk into the face (it prints on the bed)
sketch RingSk on XY offset (T - 0.4 mm) {
  circle o = (cx, pad_y) radius (ring_r + 4.6 mm)
  circle i = (cx, pad_y) radius (ring_r + 4 mm)
  fix o i
}
extrude RingGroove from RingSk distance 1 mm profiles (cx + ring_r + 4.3 mm, pad_y) cut targets Front
// ==== Caps: a stem through the cover and a flange inside, resting on the switch
cylinder RecordStem radius rec_r height (T + key_out - board_top - 4.3 mm) base (cx, pad_y, board_top + 4.3 mm) axis Z new -> body Record
cylinder RecordFlange radius (rec_r + 1.6 mm) height (in_front - board_top - 4.3 mm) base (cx, pad_y, board_top + 4.3 mm) axis Z join targets Record
fillet RecordTop edges (cx + rec_r, pad_y, T + key_out) radius 1 mm body Record
cylinder UpStem radius key_r height (wall + key_out + 0.2 mm) base (cx, pad_y + ring_r, in_front - 0.2 mm) axis Z new -> body KeyUp
cylinder UpFlange radius (key_r + 1.5 mm) height 1 mm base (cx, pad_y + ring_r, in_front - 1 mm) axis Z join targets KeyUp
cylinder DownStem radius key_r height (wall + key_out + 0.2 mm) base (cx, pad_y - ring_r, in_front - 0.2 mm) axis Z new -> body KeyDown
cylinder DownFlange radius (key_r + 1.5 mm) height 1 mm base (cx, pad_y - ring_r, in_front - 1 mm) axis Z join targets KeyDown
cylinder RightStem radius key_r height (wall + key_out + 0.2 mm) base (cx + ring_r, pad_y, in_front - 0.2 mm) axis Z new -> body KeyRight
cylinder RightFlange radius (key_r + 1.5 mm) height 1 mm base (cx + ring_r, pad_y, in_front - 1 mm) axis Z join targets KeyRight
cylinder LeftStem radius key_r height (wall + key_out + 0.2 mm) base (cx - ring_r, pad_y, in_front - 0.2 mm) axis Z new -> body KeyLeft
cylinder LeftFlange radius (key_r + 1.5 mm) height 1 mm base (cx - ring_r, pad_y, in_front - 1 mm) axis Z join targets KeyLeft
cylinder PlayStem radius key_r height (wall + key_out + 0.2 mm) base (cx - 12 mm, 6.5 mm, in_front - 0.2 mm) axis Z new -> body KeyPlay
cylinder PlayFlange radius (key_r + 1.5 mm) height 1 mm base (cx - 12 mm, 6.5 mm, in_front - 1 mm) axis Z join targets KeyPlay
cylinder DeleteStem radius key_r height (wall + key_out + 0.2 mm) base (cx + 12 mm, 6.5 mm, in_front - 0.2 mm) axis Z new -> body KeyDelete
cylinder DeleteFlange radius (key_r + 1.5 mm) height 1 mm base (cx + 12 mm, 6.5 mm, in_front - 1 mm) axis Z join targets KeyDelete

// ==== USB arm: a slotted bar from the pivot, the plug's board in a slot at its end, built stowed
sketch ArmSk on XY offset (chan_z + fit) {
  slot s = centers (arm_x, arm_y) (arm_x, arm_y + arm_l - 13 mm) width (chan_d - 2*fit)
  fix s
}
extrude ArmBody from ArmSk distance (chan_h - 2*fit) new -> body UsbArm
cylinder ArmPin diameter (pin_d + 0.25 mm) height (chan_h + 2 mm) base (arm_x, arm_y, chan_z - 1 mm) axis Z cut targets UsbArm
box PlugSlot size 1.9 mm 10.5 mm 12.3 mm corner (arm_x - 0.95 mm, arm_y + arm_l - 23 mm, T/2 - 6.15 mm) cut targets UsbArm
box PlugShell size 4.5 mm 13 mm 12 mm corner (arm_x - 2.25 mm, arm_y + arm_l - 13 mm, T/2 - 6 mm) new -> body UsbPlug
box PlugMouth size 3.3 mm 10 mm 10.8 mm corner (arm_x - 1.65 mm, arm_y + arm_l - 10 mm, T/2 - 5.4 mm) cut targets UsbPlug
box PlugBoard size 1.6 mm 10 mm 12 mm corner (arm_x - 0.8 mm, arm_y + arm_l - 22.9 mm, T/2 - 6 mm) join targets UsbPlug
move ArmToPivot bodies UsbArm, UsbPlug to (-arm_x, -arm_y, 0)
move ArmSwing bodies UsbArm, UsbPlug axis (0,0,1) angle usb_angle
move ArmBack bodies UsbArm, UsbPlug to (arm_x, arm_y, 0)

// ==== Lens ring, pressed into its recess
cylinder LensRingOuter radius 7 mm height 1 mm base (cx, xiao_y, 0) axis Z new -> body LensRing
cylinder LensRingBore radius (lens_d/2 + 0.6 mm) height 2 mm base (cx, xiao_y, -0.5 mm) axis Z cut targets LensRing

// ==== Bought parts, for fit
box XiaoStack size xiao_w xiao_l xiao_h corner (cx - xiao_w/2, xiao_y - xiao_l/2, wall + 0.05 mm) new -> body Xiao
box TftModule size tft_w tft_l tft_t corner (cx - tft_w/2, tft_y - tft_l/2, tft_z - 0.05 mm) new -> body Tft
box LiPo size bat_w bat_l bat_t corner (cx - bat_w/2, 5 mm, wall + 0.05 mm) new -> body Battery
box Perfboard size 40 mm 40 mm board_t corner (cx - 20 mm, board_y0, board_top - board_t) new -> body ButtonBoard

color Back, Front, UsbArm "#e9e7e2"
color Record "#d8262b"
color KeyUp, KeyDown, KeyLeft, KeyRight, KeyPlay, KeyDelete "#cfd2d6"
color LensRing "#8a8f96"
color UsbPlug "#c9ccd0"
color Xiao, ButtonBoard "#2f7d4a"
color Tft "#15171b"
color Battery "#3b5ba5"
material Back, Front, UsbArm, Record, KeyUp, KeyDown, KeyLeft, KeyRight, KeyPlay, KeyDelete, LensRing PLA
