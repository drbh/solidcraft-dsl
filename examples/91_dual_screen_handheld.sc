// 91. Printable dual-screen clamshell handheld (DS-style shape): a Raspberry Pi Zero 2 W with a 2.8 in
// resistive-touch TFT below, a 2.4 in TFT above, a D-pad, A / B / X / Y, Start and Select.
//
// Bill of materials (measure yours; every module size is a parameter):
//   Raspberry Pi Zero 2 W                 65 x 30 mm, M2.5 holes 58 x 23 mm apart
//   2.8 in ILI9341 SPI TFT + XPT2046 touch  86 x 50 mm PCB, M3 holes 3.5 mm in from the corners
//   2.4 in ILI9341 SPI TFT                77.2 x 42.7 mm PCB
//   LiPo 503450 (1000 mAh)                5 x 34 x 50 mm, in the lid behind the top screen
//   TP4056 USB-C charger 25 x 19 mm and MT3608 5 V boost 36 x 17 mm, under the button boards
//   two perfboards 32 x 50 mm and 10 tactile switches 6 x 6 x 4.3 mm on their 0.1 in grid
//   SS12D00 slide switch (power), M2 screws: 6 x 14 mm (base), 4 x 8 mm (lid),
//   M2.5 x 4 mm (Pi), M3 x 6 mm (2.8 in TFT); about 12 strands of 30 AWG silicone wire
//
// Z is up and the console lies on its back. X runs across it (centred on x = 0), Y from the front
// edge (y = 0) to the hinge at the back.
// Base: a tray with the Pi on low standoffs in the middle, the 2.8 in TFT on taller ones over
// it, and a button board each side over the charger (left, USB-C and power switch in the front
// wall) and the boost (right). The face plate carries the touch window and the button holes, and
// six posts down to bosses on the tray floor; M2 x 14 screws from the back, counterbored, hold it.
// Buttons are separate caps: a stem through the plate with `clear` all round, a flange under the
// plate so it can't fall out, resting on its switch: the boards stand so_h high so the switch
// tops sit just under the flanges. The D-pad is one cross whose flange covers four switches. A stylus lives in a tube along the back wall, entered from the right.
// Lid: a bezel (inner face) with the top window, a locating frame and pockets for two 15 x 11 mm
// speakers behind the grilles, and a tray (outer face) over the battery, screwed from the outside.
// Hinge: two base knuckles and one long lid knuckle on the parting plane. Two printed axle tubes
// slide in from the sides through the base knuckles (glue them there) into the lid knuckle, whose
// bore is tube_fit for friction. The right tube is hollow and carries the lid's wires: they leave
// the lid through a window in its knuckle, run along the tube and come out of its side window
// into the base through a slot in the right knuckle's web. The lid is built closed, then turned
// about the axis (move it to the X axis, rotate by open_angle, move it back), so open_angle stays
// live. Bodies named Ref* stand in for the bought parts and are left out of the print layouts.
//
// Print (examples/91_dual_screen_handheld.print.json): PLA, 0.2 mm layers, no supports. Trays open
// side up, face plate and bezel face down, caps flange down, tubes and stylus lying down.
design DualScreenHandheld units mm

param W = 156 mm
param D = 84 mm                // tray depth, front to the hinge end
param corner_r = 10 mm
param wall = 1.6 mm
param plate_t = 1.6 mm
param gap = 0.5 mm
param clear = 0.25 mm
param open_angle = 150 deg     // 0 closed

// Electronics placement
param pi_y = 20 mm             // the Pi's front edge
param tft_y = 42 mm            // centre of both screens
param tft_pcb_w = 86 mm
param tft_pcb_d = 50 mm
param tft_t = 5.5 mm           // PCB plus panel and touch layer
param tft_view_w = 58.6 mm
param tft_view_d = 44.2 mm
param tft_view_dx = 6 mm       // panel centre from PCB centre, away from the header end
param top_pcb_w = 77.2 mm
param top_pcb_d = 42.7 mm
param top_t = 4.5 mm
param top_view_w = 50 mm
param top_view_d = 37.7 mm
param top_view_dx = 5 mm
param bat_t = 5 mm
param bat_w = 50 mm
param bat_d = 34 mm

// Buttons
param pad_x = 60 mm            // D-pad and A/B/X/Y centres, either side
param pad_y = 46 mm
param sw_step = 7.62 mm        // switches 3 holes out from each centre
param board_w = 32 mm
param board_d = 50 mm
param board_y0 = 16 mm
param sw_h = 4.3 mm
param btn_r = 4.25 mm
param key_up = 1.6 mm
param ss_y = 22 mm             // Start / Select row

// Hinge
param knuckle_r = 5.5 mm
param tube_od = 6 mm
param tube_id = 4.2 mm
param tube_fit = 6.25 mm
param lid_knuckle = 59.2 mm    // the lid knuckle's length, centred

let tft_z = wall + 9 mm                 // the 2.8 in TFT's PCB underside
let tray_h = tft_z + tft_t + 0.1 mm
let base_t = tray_h + plate_t
let lid_z0 = base_t + gap
let lid_tray_z = lid_z0 + plate_t
let lid_h = wall + bat_t + 0.3 mm + top_t + 0.2 mm
let lid_top = lid_tray_z + lid_h
let zc = base_t + gap/2
let hy = D + knuckle_r + 0.4 mm
let flange_z = tray_h - 1.3 mm          // cap flanges, 0.1 mm under the plate
let sw_top0 = flange_z - 0.1 mm         // the switches' tops, just under the flanges
let so_h = sw_top0 - sw_h - 1.6 mm - wall
let board_z = wall + so_h
let sw_top = board_z + 1.6 mm + sw_h
let sx = W/2 - wall - 4 mm              // screw columns
let sy0 = wall + 4.4 mm
let sy1 = D - wall - 4.4 mm
let tube_len = W/2 - lid_knuckle/2 + 18 mm

// ==== Base tray
sketch BaseSk on XY {
  rect rounded o = (-W/2, 0) (W/2, D) radius corner_r
  fix o
}
extrude BaseBlock from BaseSk distance tray_h new -> body BaseTray
shell BaseCavity faces (0, D/2, tray_h) thickness wall inside body BaseTray
fillet BaseBottom edges (0, 0, 0) radius 1.5 mm body BaseTray

// Screw bosses (six) with clearance and counterbores from the back
for i from 0 to 2 {
  for j from 0 to 1 {
    cylinder Boss{i}{j} radius 3 mm height (8 mm - wall + 0.8 mm) base ((i - 1)*sx, sy0 + j*(sy1 - sy0), wall - 0.8 mm) axis (0,0,1) join targets BaseTray
    cylinder BossHole{i}{j} radius 1.1 mm height 9 mm base ((i - 1)*sx, sy0 + j*(sy1 - sy0), -0.5 mm) axis (0,0,1) cut targets BaseTray
    cylinder BossBore{i}{j} radius 2.1 mm height 1.9 mm base ((i - 1)*sx, sy0 + j*(sy1 - sy0), -0.5 mm) axis (0,0,1) cut targets BaseTray
  }
}
// Pi Zero standoffs (M2.5 pilots) and the 2.8 in TFT's (M3 pilots)
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder PiStand{i}{j} radius 2.6 mm height 2.8 mm base ((2*i - 1)*29 mm, pi_y + 3.5 mm + j*23 mm, wall - 0.8 mm) axis (0,0,1) join targets BaseTray
    cylinder PiPilot{i}{j} radius 1.05 mm height 3 mm base ((2*i - 1)*29 mm, pi_y + 3.5 mm + j*23 mm, wall) axis (0,0,1) cut targets BaseTray
    cylinder TftStand{i}{j} radius 3 mm height (tft_z - wall + 0.8 mm) base ((2*i - 1)*(tft_pcb_w/2 - 3.5 mm), tft_y + (2*j - 1)*(tft_pcb_d/2 - 3.5 mm), wall - 0.8 mm) axis (0,0,1) join targets BaseTray
    cylinder TftPilot{i}{j} radius 1.25 mm height 6 mm base ((2*i - 1)*(tft_pcb_w/2 - 3.5 mm), tft_y + (2*j - 1)*(tft_pcb_d/2 - 3.5 mm), tft_z - 5.9 mm) axis (0,0,1) cut targets BaseTray
  }
}
// Button board standoffs, M2 pilots, at the boards' corners
for s from 0 to 1 {
  for i from 0 to 1 {
    for j from 0 to 1 {
      cylinder BoardStand{s}{i}{j} radius 2.6 mm height (so_h + 0.8 mm) base ((2*s - 1)*(pad_x + (2*i - 1)*(board_w/2 - 3 mm)), board_y0 + 3 mm + j*(board_d - 6 mm), wall - 0.8 mm) axis (0,0,1) join targets BaseTray
      cylinder BoardPilot{s}{i}{j} radius 0.8 mm height 4 mm base ((2*s - 1)*(pad_x + (2*i - 1)*(board_w/2 - 3 mm)), board_y0 + 3 mm + j*(board_d - 6 mm), board_z - 3.9 mm) axis (0,0,1) cut targets BaseTray
    }
  }
}
// Front wall: USB-C for the charger and the power switch slot, both on the left
sketch FrontSk on XZ offset (-0.5 mm) {
  slot u = centers (-pad_x - 3 mm, -(wall + 3 mm)) (-pad_x + 3 mm, -(wall + 3 mm)) width 3.8 mm
  rect p = (-pad_x + 14 mm, -(wall + 1.6 mm)) (-pad_x + 22 mm, -(wall + 4.4 mm))
  fix u p
}
extrude FrontPorts from FrontSk distance (wall + 1 mm) cut targets BaseTray
// Stylus tube along the back wall, open in the right wall
cylinder StylusTube radius 3.2 mm height 88 mm base (W/2 - wall - 86.5 mm, D - wall - 8.5 mm, wall + 3 mm) axis (1,0,0) join targets BaseTray
cylinder StylusBore radius 2.25 mm height 88 mm base (W/2 - 86 mm, D - wall - 8.5 mm, wall + 3 mm) axis (1,0,0) cut targets BaseTray

// Knuckles on webs from the back wall; their bores take the axle tubes
box BaseWebL size (W/2 - lid_knuckle/2 - 12.6 mm) (hy - D + 1.4 mm) (tray_h - 0.1 mm - (zc - knuckle_r + 0.5 mm)) corner (-W/2 + 12.3 mm, D - 1.4 mm, zc - knuckle_r + 0.5 mm) join targets BaseTray
box BaseWebR size (W/2 - lid_knuckle/2 - 12.6 mm) (hy - D + 1.4 mm) (tray_h - 0.1 mm - (zc - knuckle_r + 0.5 mm)) corner (lid_knuckle/2 + 0.3 mm + 0.4 mm, D - 1.4 mm, zc - knuckle_r + 0.5 mm) join targets BaseTray
cylinder BaseKnuckleL radius knuckle_r height (W/2 - lid_knuckle/2 - 12.4 mm) base (-W/2 + 12 mm, hy, zc) axis (1,0,0) join targets BaseTray
cylinder BaseKnuckleR radius knuckle_r height (W/2 - lid_knuckle/2 - 12.4 mm) base (lid_knuckle/2 + 0.4 mm, hy, zc) axis (1,0,0) join targets BaseTray
cylinder BaseTubeBore radius (tube_od/2 + 0.03 mm) height (W - 20 mm) base (-W/2 + 10 mm, hy, zc) axis (1,0,0) cut targets BaseTray
box BaseWireSlot size 8 mm (hy - D + 2.5 mm) 3 mm corner (lid_knuckle/2 + 10 mm, D - 2 mm, zc - 2.5 mm) cut targets BaseTray

// ==== Face plate: touch window, button holes, six posts down to the bosses
sketch PlateSk on XY offset tray_h {
  rect rounded o = (-W/2, 0) (W/2, D) radius corner_r
  fix o
}
extrude PlateSlab from PlateSk distance plate_t new -> body FacePlate
fillet PlateTop edges (0, 0, base_t) radius 1 mm body FacePlate
sketch PlateHolesSk on XY offset tray_h {
  rect rounded v = (tft_view_dx - tft_view_w/2, tft_y - tft_view_d/2) (tft_view_dx + tft_view_w/2, tft_y + tft_view_d/2) radius 1 mm
  rect dh = (-pad_x - 11 mm - clear, pad_y - 3.6 mm - clear) (-pad_x + 11 mm + clear, pad_y + 3.6 mm + clear)
  rect dv = (-pad_x - 3.6 mm - clear, pad_y - 11 mm - clear) (-pad_x + 3.6 mm + clear, pad_y + 11 mm + clear)
  circle ba = (pad_x + sw_step, pad_y) radius (btn_r + clear)
  circle bb = (pad_x, pad_y - sw_step) radius (btn_r + clear)
  circle bx = (pad_x, pad_y + sw_step) radius (btn_r + clear)
  circle by = (pad_x - sw_step, pad_y) radius (btn_r + clear)
  slot st = centers (pad_x + sw_step - 2.5 mm, ss_y) (pad_x + sw_step + 2.5 mm, ss_y) width (3.5 mm + 2*clear)
  slot se = centers (pad_x - sw_step - 2.5 mm, ss_y) (pad_x - sw_step + 2.5 mm, ss_y) width (3.5 mm + 2*clear)
  fix v dh dv ba bb bx by st se
}
extrude PlateHoles from PlateHolesSk distance (plate_t + 1 mm) offset (-0.5 mm) cut targets FacePlate
for i from 0 to 2 {
  for j from 0 to 1 {
    cylinder Post{i}{j} radius 2.4 mm height (tray_h - 8.1 mm + 0.3 mm) base ((i - 1)*sx, sy0 + j*(sy1 - sy0), 8.1 mm) axis (0,0,1) join targets FacePlate
    cylinder PostPilot{i}{j} radius 0.8 mm height 6 mm base ((i - 1)*sx, sy0 + j*(sy1 - sy0), 8 mm) axis (0,0,1) cut targets FacePlate
  }
}

// ==== Button caps: a stem through the plate and a flat flange resting on its switch(es)
sketch DPadStemSk on XY offset flange_z {
  rect dh = (-pad_x - 11 mm, pad_y - 3.6 mm) (-pad_x + 11 mm, pad_y + 3.6 mm)
  rect dv = (-pad_x - 3.6 mm, pad_y - 11 mm) (-pad_x + 3.6 mm, pad_y + 11 mm)
  fix dh dv
}
cylinder DPadFlange radius 13.5 mm height 1.2 mm base (-pad_x, pad_y, flange_z) axis (0,0,1) new -> body DPad
extrude DPadStem from DPadStemSk distance (1.2 mm + 0.1 mm + plate_t + key_up) offset 0.5 mm join targets DPad
for k from 0 to 3 {
  cylinder BtnFlange{k} radius (btn_r + 1.8 mm) height 1.2 mm base (pad_x + sw_step*cos(90 deg*k), pad_y + sw_step*sin(90 deg*k), flange_z) axis (0,0,1) new -> body Button{k}
  cylinder BtnStem{k} radius btn_r height (1.2 mm + 0.1 mm + plate_t + key_up) base (pad_x + sw_step*cos(90 deg*k), pad_y + sw_step*sin(90 deg*k), flange_z + 0.5 mm) axis (0,0,1) join targets Button{k}
}
for k from 0 to 1 {
  sketch PillFlangeSk{k} on XY offset flange_z {
    slot f = centers (pad_x + (2*k - 1)*sw_step - 3.5 mm, ss_y) (pad_x + (2*k - 1)*sw_step + 3.5 mm, ss_y) width 6.5 mm
    fix f
  }
  sketch PillStemSk{k} on XY offset flange_z {
    slot f = centers (pad_x + (2*k - 1)*sw_step - 2.5 mm, ss_y) (pad_x + (2*k - 1)*sw_step + 2.5 mm, ss_y) width 3.5 mm
    fix f
  }
  extrude PillFlange{k} from PillFlangeSk{k} distance 1.2 mm new -> body Pill{k}
  extrude PillStem{k} from PillStemSk{k} distance (1.2 mm + 0.1 mm + plate_t + key_up) offset 0.5 mm join targets Pill{k}
}
cylinder StylusRod radius 2 mm height 84 mm base (W/2 - 84 mm, D - wall - 8.5 mm, wall + 3 mm) axis (1,0,0) new -> body Stylus
cylinder StylusCap radius 3 mm height 4 mm base (W/2 - 0.5 mm, D - wall - 8.5 mm, wall + 3 mm) axis (1,0,0) join targets Stylus

// ==== Lid bezel: top window, frame, speaker pockets and posts
sketch BezelSk on XY offset lid_z0 {
  rect rounded o = (-W/2, 0) (W/2, D) radius corner_r
  fix o
}
extrude BezelSlab from BezelSk distance plate_t new -> body Bezel
fillet BezelEdge edges (0, 0, lid_z0) radius 1 mm body Bezel
sketch TopViewSk on XY offset lid_z0 {
  rect rounded v = (top_view_dx - top_view_w/2, tft_y - top_view_d/2) (top_view_dx + top_view_w/2, tft_y + top_view_d/2) radius 1 mm
  fix v
}
extrude TopView from TopViewSk distance (plate_t + 1 mm) offset (-0.5 mm) cut targets Bezel
sketch GrilleSk on XY offset lid_z0 {
  circle g = (-pad_x - 4 mm, tft_y - 4 mm) radius 0.8 mm
  circle h = (pad_x - 4 mm, tft_y - 4 mm) radius 0.8 mm
  fix g h
  pattern rectangular g count 3 spacing 4 mm count2 3 spacing2 4 mm
  pattern rectangular h count 3 spacing 4 mm count2 3 spacing2 4 mm
}
extrude Grilles from GrilleSk distance (plate_t + 1 mm) offset (-0.5 mm) cut targets Bezel
sketch BezelFrameSk on XY offset (lid_tray_z - 0.1 mm) {
  rect outer = (-top_pcb_w/2 - 1.3 mm, tft_y - top_pcb_d/2 - 1.3 mm) (top_pcb_w/2 + 1.3 mm, tft_y + top_pcb_d/2 + 1.3 mm)
  rect inner = (-top_pcb_w/2 - 0.3 mm, tft_y - top_pcb_d/2 - 0.3 mm) (top_pcb_w/2 + 0.3 mm, tft_y + top_pcb_d/2 + 0.3 mm)
  rect spl_o = (-pad_x - 8.8 mm, tft_y - 6.8 mm) (-pad_x + 8.8 mm, tft_y + 6.8 mm)
  rect spl_i = (-pad_x - 7.8 mm, tft_y - 5.8 mm) (-pad_x + 7.8 mm, tft_y + 5.8 mm)
  rect spr_o = (pad_x - 8.8 mm, tft_y - 6.8 mm) (pad_x + 8.8 mm, tft_y + 6.8 mm)
  rect spr_i = (pad_x - 7.8 mm, tft_y - 5.8 mm) (pad_x + 7.8 mm, tft_y + 5.8 mm)
  fix outer inner spl_o spl_i spr_o spr_i
}
extrude BezelFrames from BezelFrameSk distance 2 mm profiles (top_pcb_w/2 + 0.8 mm, tft_y) (-pad_x - 8.3 mm, tft_y) (pad_x + 8.3 mm, tft_y) join targets Bezel
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder LidPost{i}{j} radius 2.5 mm height (lid_h - wall - 1.5 mm - 0.1 mm + 0.6 mm) base ((2*i - 1)*sx, sy0 + j*(sy1 - sy0), lid_tray_z - 0.6 mm) axis (0,0,1) join targets Bezel
    cylinder LidPostPilot{i}{j} radius 0.8 mm height 8.5 mm base ((2*i - 1)*sx, sy0 + j*(sy1 - sy0), lid_tray_z + 1 mm) axis (0,0,1) cut targets Bezel
  }
}

// ==== Lid tray: outer face, bosses, wire window in the knuckle
sketch LidSk on XY offset lid_tray_z {
  rect rounded o = (-W/2, 0) (W/2, D) radius corner_r
  fix o
}
extrude LidBlock from LidSk distance lid_h new -> body LidTray
shell LidCavity faces (0, D/2, lid_tray_z) thickness wall inside body LidTray
fillet LidTopEdge edges (0, 0, lid_top) radius 1.5 mm body LidTray
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder LidBoss{i}{j} radius 3 mm height 1.6 mm base ((2*i - 1)*sx, sy0 + j*(sy1 - sy0), lid_top - wall - 1.5 mm) axis (0,0,1) join targets LidTray
    cylinder LidBossHole{i}{j} radius 1.1 mm height (wall + 3 mm) base ((2*i - 1)*sx, sy0 + j*(sy1 - sy0), lid_top - wall - 2 mm) axis (0,0,1) cut targets LidTray
    cylinder LidCounterbore{i}{j} radius 2.1 mm height 1.9 mm base ((2*i - 1)*sx, sy0 + j*(sy1 - sy0), lid_top - 1.4 mm) axis (0,0,1) cut targets LidTray
  }
}
box LidWeb size (lid_knuckle - 0.6 mm) (hy - D + 1.4 mm) (zc + knuckle_r - 0.5 mm - lid_tray_z - 0.1 mm) corner (-lid_knuckle/2 + 0.3 mm, D - 1.4 mm, lid_tray_z + 0.1 mm) join targets LidTray
cylinder LidKnuckle radius knuckle_r height lid_knuckle base (-lid_knuckle/2, hy, zc) axis (1,0,0) join targets LidTray
cylinder LidTubeBore radius (tube_fit/2) height (lid_knuckle + 2 mm) base (-lid_knuckle/2 - 1 mm, hy, zc) axis (1,0,0) cut targets LidTray
box LidWireWindow size 8 mm (hy - D + 2.5 mm) 3 mm corner (2 mm, D - 2 mm, zc + 0.5 mm) cut targets LidTray

// ==== Axle tubes: slide in from each side through a base knuckle into the lid knuckle
cylinder TubeL radius (tube_od/2) height tube_len base (-W/2 + 11.5 mm, hy, zc) axis (1,0,0) new -> body AxleL
cylinder TubeLBore radius (tube_id/2) height (tube_len + 2 mm) base (-W/2 + 10.5 mm, hy, zc) axis (1,0,0) cut targets AxleL
cylinder TubeR radius (tube_od/2) height tube_len base (W/2 - 11.5 mm - tube_len, hy, zc) axis (1,0,0) new -> body AxleR
cylinder TubeRBore radius (tube_id/2) height (tube_len + 2 mm) base (W/2 - 10.5 mm - tube_len - 2 mm, hy, zc) axis (1,0,0) cut targets AxleR
box TubeRWindow size 8 mm 4 mm 3 mm corner (lid_knuckle/2 + 10 mm, hy - 4 mm, zc - 1.5 mm) cut targets AxleR

// ==== Stand-ins for the bought parts (not printed)
box RefPi size 65 mm 30 mm 4.6 mm corner (-32.5 mm, pi_y, wall + 2) new -> body RefPi
box RefTft28 size tft_pcb_w tft_pcb_d tft_t corner (-tft_pcb_w/2, tft_y - tft_pcb_d/2, tft_z) new -> body RefTft28
box RefBoardL size board_w board_d 1.6 mm corner (-pad_x - board_w/2, board_y0, board_z) new -> body RefBoardL
box RefBoardR size board_w board_d 1.6 mm corner (pad_x - board_w/2, board_y0, board_z) new -> body RefBoardR
box RefCharger size 19 mm 25 mm 4 mm corner (-pad_x - 9.5 mm, wall + 0.3 mm, wall) new -> body RefCharger
box RefBoost size 17 mm 36 mm 4 mm corner (pad_x - 8.5 mm, wall + 10 mm, wall) new -> body RefBoost
box RefTft24 size top_pcb_w top_pcb_d top_t corner (-top_pcb_w/2, tft_y - top_pcb_d/2, lid_tray_z) new -> body RefTft24
box RefBattery size bat_w bat_d bat_t corner (-bat_w/2, tft_y - bat_d/2, lid_top - wall - bat_t) new -> body RefBattery

// ==== Open the lid about the axle
move HingeIn bodies Bezel, LidTray, RefTft24, RefBattery to (0, -hy, -zc)
move Open bodies Bezel, LidTray, RefTft24, RefBattery axis (-1,0,0) angle open_angle
move HingeOut bodies Bezel, LidTray, RefTft24, RefBattery to (0, hy, zc)

color BaseTray, FacePlate, Bezel, LidTray "#eef0f2"
color DPad, Button0, Button1, Button2, Button3, Pill0, Pill1 "#3b3f46"
color AxleL, AxleR, Stylus "#9aa1aa"
color RefPi, RefBoardL, RefBoardR, RefCharger, RefBoost "#1f6f43"
color RefTft28, RefTft24 "#101418"
color RefBattery "#c9b037"
material BaseTray, FacePlate, Bezel, LidTray, DPad, Button0, Button1, Button2, Button3, Pill0, Pill1, AxleL, AxleR, Stylus PLA
