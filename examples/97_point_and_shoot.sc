// 97. Compact digital camera of the mid-2000s, ELPH-style, printable and built round a XIAO ESP32S3 Sense.
//
// The camera stands upright: X across its width (seen from the front, x = 0 is the left end),
// Z up (z = 0 the base), Y through it from the front (y = 0, the lens side, facing -Y) to the
// back (y = D, the screen side).
//
// Bill of materials
//   1  Seeed XIAO ESP32S3 Sense (21 x 17.8 mm, 15 mm deep with the camera board and OV2640),
//      lying with its camera looking out through the lens barrel
//   1  2.0 in ST7789 240x320 SPI IPS module, about 56 x 34.5 x 4 mm (measure yours: tft_*)
//   1  503035 LiPo, 500 mAh, 5 x 30 x 35 mm, on the XIAO's BAT pads
//   1  short USB-C male to USB-C female panel extension (right angle, 10 cm): the female end
//      sits in the right end, the male end in the XIAO; it charges and programs through it
//   1  5 mm white LED (flash) and 3 mm amber LED (focus-assist / self-timer), with resistors
//   8  6 x 6 x 5 mm tactile switches: shutter and power on a 38 x 10 mm perfboard under the top,
//      four-way, centre, A and B on a 22 x 40 mm perfboard behind the back
//   4  M2 x 20 self-tapping screws (case), 4 M2 x 6 (back board), 2 M2 x 6 (top board)
//
// How it goes together
//   Front      the front shell: the lens collar's hole, the flash window, the focus lamp hole, the
//              LiPo pocket, and a cradle that holds the XIAO square behind the lens
//   Back       the back shell: screen window and TFT frame, the button board on standoffs, the top
//              board under the shutter, the USB-C socket in the right end and the strap lug.
//              Four M2 screws from the back pull the shells together.
//   Collar     glued into the front from inside, its flange against the wall. Stage1-3 nest inside
//              it, each with a flange at its base and a lip at its front, so they slide out but
//              can't fall out: drop Stage3 into Stage2, Stage2 into Stage1, Stage1 into the Collar,
//              all from behind, before gluing the collar in. `zoom` (0 to 1) slides them out; it
//              is a live parameter (the stages' extrude offsets). With the OV2640's standard 66 deg
//              lens the stages frame the picture at zoom 0; extended, they act as a lens hood.
//   Flash      a thin diffuser plate pressed into the flash window from inside
//   Shutter, Power, KeyUp/Down/Left/Right, Func, KeyA, KeyB   button caps, each with a flange inside
//   Xiao, Tft, Battery, ButtonBoard, TopBoard   the bought parts, for checking fit (not printed)
//
// Printing (PLA, 0.2 mm layers, no supports): Front on its front face, Back on its back face,
// the collar and stages standing on their flanges, the caps flange down. The front and back
// edges are chamfered so they print on the bed.
design PointAndShoot units mm

// Body
param W = 90 mm
param H = 57 mm
param D = 28 mm
param corner_r = 8 mm
param edge_c = 2 mm
param wall = 1.8 mm
param split_y = 9 mm           // where the front shell meets the back shell
param fit = 0.25 mm
param pilot_d = 1.6 mm         // M2 self-tapping
param clear_d = 2.4 mm
param head_d = 4.2 mm
param boss_r = 2.75 mm

// Lens, centred on the front
param lens_x = 58 mm
param lens_z = 28 mm
param zoom = 0                 // 0 retracted, 1 every stage slid out by stage_run
param stage_run = 4.5 mm
param stage_l = 7.5 mm
param r1 = 16.5 mm             // stage 1's outside radius; each next stage is 2.4 mm smaller

// XIAO ESP32S3 Sense behind the lens
param xiao_w = 21 mm           // along X
param xiao_h = 17.8 mm         // along Z
param xiao_d = 15 mm           // along Y, camera at the front

// TFT on the back, long side along X
param tft_l = 56 mm
param tft_w = 34.5 mm
param tft_t = 4 mm
param view_l = 41.8 mm
param view_w = 31.6 mm
param tft_x = 54 mm

// LiPo behind the front, on the left
param bat_l = 35 mm            // along X
param bat_w = 30 mm            // along Z
param bat_t = 5 mm

// Buttons
param key_r = 2.4 mm
param key_out = 1 mm
param pad_x = 13 mm
param pad_z = 31 mm
param shutter_x = 16 mm
param power_x = 33 mm
param top_y = 15.5 mm          // shutter and power, front to back

let in_back = D - wall
let in_top = H - wall
let r2 = r1 - 2.4 mm
let r3 = r2 - 2.4 mm
let xiao_y = 6.5 mm
let tft_y = in_back - tft_t
let board_top = in_back - 1 mm - 5 mm               // the back board's switch face (toward +Y)
let top_board = in_top - 1 mm - 5 mm                // the top board's switch face (up)

// ==== Shells: the front outline extruded front to back, split, edge chamfered, shelled open
sketch OutlineSk on XZ {
  rect rounded o = (0, 0) (W, -H) radius corner_r
  fix o
}
extrude FrontBlank from OutlineSk distance split_y new -> body Front
chamfer FrontEdge edges (W/2, 0, 0) distance edge_c body Front
shell FrontCavity faces (W/2, split_y, H/2) thickness wall inside body Front
extrude BackBlank from OutlineSk distance (D - split_y) offset split_y new -> body Back
chamfer BackEdge edges (W/2, D, 0) distance edge_c body Back
shell BackCavity faces (W/2, split_y, H/2) thickness wall inside body Back

// Screws: bosses in both shells, pilots in the front, clearance and head pockets from the back
sketch BossSk on XZ {
  circle a = (4.5 mm, -4.5 mm) radius boss_r
  circle b = (W - 4.5 mm, -4.5 mm) radius boss_r
  circle c = (4.5 mm, -(H - 4.5 mm)) radius boss_r
  circle d = (W - 4.5 mm, -(H - 4.5 mm)) radius boss_r
  fix a b c d
}
extrude FrontBosses from BossSk distance (split_y - wall + 0.1 mm) offset (wall - 0.2 mm) join targets Front
extrude BackBosses from BossSk distance (in_back - split_y + 0.1 mm) offset (split_y + 0.1 mm) join targets Back
sketch PilotSk on XZ {
  circle a = (4.5 mm, -4.5 mm) diameter pilot_d
  circle b = (W - 4.5 mm, -4.5 mm) diameter pilot_d
  circle c = (4.5 mm, -(H - 4.5 mm)) diameter pilot_d
  circle d = (W - 4.5 mm, -(H - 4.5 mm)) diameter pilot_d
  fix a b c d
}
extrude Pilots from PilotSk distance 6 mm offset (split_y - 5.9 mm) cut targets Front
sketch ClearSk on XZ {
  circle a = (4.5 mm, -4.5 mm) diameter clear_d
  circle b = (W - 4.5 mm, -4.5 mm) diameter clear_d
  circle c = (4.5 mm, -(H - 4.5 mm)) diameter clear_d
  circle d = (W - 4.5 mm, -(H - 4.5 mm)) diameter clear_d
  fix a b c d
}
extrude Clear from ClearSk distance (D - split_y + 2 mm) offset (split_y - 1 mm) cut targets Back
sketch HeadSk on XZ {
  circle a = (4.5 mm, -4.5 mm) diameter head_d
  circle b = (W - 4.5 mm, -4.5 mm) diameter head_d
  circle c = (4.5 mm, -(H - 4.5 mm)) diameter head_d
  circle d = (W - 4.5 mm, -(H - 4.5 mm)) diameter head_d
  fix a b c d
}
extrude Heads from HeadSk distance 4 mm offset (D - 3 mm) cut targets Back

// ==== Front: collar hole, flash window, focus lamp, viewfinder (sunk, for paint), LiPo pocket
sketch FrontHolesSk on XZ {
  circle lens = (lens_x, -lens_z) radius (19 mm + 0.1 mm)
  rect rounded flash = (8 mm, -(H - 14 mm)) (24 mm, -(H - 7 mm)) radius 1.5 mm
  circle af = (40 mm, -(H - 10 mm)) diameter 3.2 mm
  fix lens flash af
}
extrude FrontHoles from FrontHolesSk distance (wall + 2 mm) offset (-1 mm) cut targets Front
sketch FinderSk on XZ {
  rect rounded v = (28 mm, -(H - 14 mm)) (36 mm, -(H - 8 mm)) radius 1 mm
  fix v
}
extrude Finder from FinderSk distance 1.6 mm offset (-1 mm) cut targets Front
sketch BatRibSk on XZ {
  rect l = (4 mm - fit - 1.2 mm, -14 mm) (4 mm - fit, -(12 mm + bat_w))
  rect r = (4 mm + bat_l + fit, -14 mm) (4 mm + bat_l + fit + 1.2 mm, -(12 mm + bat_w))
  rect b = (8 mm, -(12 mm - fit - 1.2 mm)) (bat_l, -(12 mm - fit))
  fix l r b
}
extrude BatRibs from BatRibSk distance (split_y - wall - 0.1 mm) offset (wall - 0.2 mm) join targets Front

// ==== Back: screen window and frame, button board standoffs, cap holes, top board, USB-C, strap lug
sketch WindowSk on XZ {
  rect rounded w = (tft_x - view_l/2, -(H/2 - view_w/2)) (tft_x + view_l/2, -(H/2 + view_w/2)) radius 1.5 mm
  fix w
}
extrude Window from WindowSk distance (wall + 2 mm) offset (in_back - 1 mm) cut targets Back
sketch BezelSk on XZ {
  rect rounded b = (tft_x - view_l/2 - 2 mm, -(H/2 - view_w/2 - 2 mm)) (tft_x + view_l/2 + 2 mm, -(H/2 + view_w/2 + 2 mm)) radius 2.5 mm
  fix b
}
extrude Bezel from BezelSk distance 1.6 mm offset (D - 0.6 mm) cut targets Back
sketch TftRibSk on XZ {
  rect t = (tft_x - tft_l/2 + 3 mm, -(H/2 + tft_w/2 + fit)) (tft_x + tft_l/2 - 3 mm, -(H/2 + tft_w/2 + fit + 1.2 mm))
  rect b = (tft_x - tft_l/2 + 3 mm, -(H/2 - tft_w/2 - fit - 1.2 mm)) (tft_x + tft_l/2 - 3 mm, -(H/2 - tft_w/2 - fit))
  rect l = (tft_x - tft_l/2 - fit - 1.2 mm, -(H/2 - 8 mm)) (tft_x - tft_l/2 - fit, -(H/2 + 8 mm))
  fix t b l
}
extrude TftRibs from TftRibSk distance (tft_t + 0.2 mm) offset (tft_y) join targets Back
sketch StandoffSk on XZ {
  circle a = (4.5 mm, -10 mm) radius 2.6 mm
  circle b = (21 mm, -10 mm) radius 2.6 mm
  circle c = (4.5 mm, -46 mm) radius 2.6 mm
  circle d = (21 mm, -46 mm) radius 2.6 mm
  fix a b c d
}
extrude Standoffs from StandoffSk distance (in_back - board_top + 0.2 mm) offset board_top join targets Back
sketch StandoffPilotSk on XZ {
  circle a = (4.5 mm, -10 mm) diameter pilot_d
  circle b = (21 mm, -10 mm) diameter pilot_d
  circle c = (4.5 mm, -46 mm) diameter pilot_d
  circle d = (21 mm, -46 mm) diameter pilot_d
  fix a b c d
}
extrude StandoffPilots from StandoffPilotSk distance 5 mm offset (board_top - 0.1 mm) cut targets Back
sketch BackKeySk on XZ {
  circle u = (pad_x, -(pad_z + 7 mm)) radius (key_r + fit)
  circle d = (pad_x, -(pad_z - 7 mm)) radius (key_r + fit)
  circle l = (pad_x + 7 mm, -pad_z) radius (key_r + fit)
  circle r = (pad_x - 7 mm, -pad_z) radius (key_r + fit)
  circle f = (pad_x, -pad_z) radius (key_r + 0.6 mm + fit)
  circle a = (pad_x - 4.5 mm, -14 mm) radius (key_r + fit)
  circle b = (pad_x + 4.5 mm, -14 mm) radius (key_r + fit)
  fix u d l r f a b
}
extrude BackKeys from BackKeySk distance (wall + 2 mm) offset (in_back - 1 mm) cut targets Back

// XIAO cradle: ribs up from the bottom wall and down from the top wall clamp the stack
sketch CradleSk on XZ {
  rect a = (lens_x - 8.5 mm, -1.6 mm) (lens_x - 7.3 mm, -(lens_z - xiao_h/2 - fit))
  rect b = (lens_x + 7.3 mm, -1.6 mm) (lens_x + 8.5 mm, -(lens_z - xiao_h/2 - fit))
  rect c = (lens_x - 8.5 mm, -(lens_z + xiao_h/2 + fit)) (lens_x - 7.3 mm, -(in_top + 0.2 mm))
  rect d = (lens_x + 7.3 mm, -(lens_z + xiao_h/2 + fit)) (lens_x + 8.5 mm, -(in_top + 0.2 mm))
  fix a b c d
}
extrude Cradle from CradleSk distance 11 mm offset (split_y + 0.1 mm) join targets Back
// Top board under the shutter and power: two posts down from the top wall
cylinder TopPostA radius 2.6 mm height (in_top - top_board + 0.2 mm) base (24.5 mm, top_y + 4.5 mm, top_board) axis Z join targets Back
cylinder TopPostB radius 2.6 mm height (in_top - top_board + 0.2 mm) base (42 mm, top_y, top_board) axis Z join targets Back
cylinder TopPilotA diameter pilot_d height 5 mm base (24.5 mm, top_y + 4.5 mm, top_board - 0.1 mm) axis Z cut targets Back
cylinder TopPilotB diameter pilot_d height 5 mm base (42 mm, top_y, top_board - 0.1 mm) axis Z cut targets Back
sketch ZoomSk on XY offset (H - 0.4 mm) {
  circle r = (shutter_x, top_y) radius 6.2 mm
  rect t = (shutter_x + 4.5 mm, top_y - 1.5 mm) (shutter_x + 8.5 mm, top_y + 1.5 mm)
  fix r t
}
extrude ZoomRing from ZoomSk distance 1.4 mm join targets Back
cylinder ShutterHole radius (4.1 mm + fit) height (wall + 4 mm) base (shutter_x, top_y, in_top - 1 mm) axis Z cut targets Back
sketch PowerHoleSk on XY offset (in_top - 1 mm) {
  rect rounded p = (power_x - 4 mm - fit, top_y - 1.5 mm - fit) (power_x + 4 mm + fit, top_y + 1.5 mm + fit) radius 1.4 mm
  fix p
}
extrude PowerHole from PowerHoleSk distance (wall + 2 mm) cut targets Back

// USB-C socket of the panel extension in the right end, with a pocket rib behind it
sketch UsbSk on YZ offset (W - wall - 1 mm) {
  rect rounded u = (17 mm - 4.7 mm, 14 mm - 1.8 mm) (17 mm + 4.7 mm, 14 mm + 1.8 mm) radius 1.6 mm
  fix u
}
extrude UsbPort from UsbSk distance (wall + 2 mm) cut targets Back
box UsbRibTop size 8.2 mm 10 mm 1.2 mm corner (W - wall - 8 mm, 11 mm, 14 mm + 2.4 mm) join targets Back
box UsbRibBottom size 8.2 mm 10 mm 1.2 mm corner (W - wall - 8 mm, 11 mm, 14 mm - 3.6 mm) join targets Back

// Strap lug on the right end, reaching the back face so it prints standing on the bed
sketch LugSk on YZ offset (W - 0.5 mm) {
  slot s = centers (15 mm, H - 14 mm) (D - 3 mm, H - 14 mm) width 5 mm
  fix s
}
extrude LugBlock from LugSk distance 3.5 mm join targets Back
cylinder LugHole radius 1.2 mm height 8 mm base (W + 1.5 mm, 20 mm, H - 18 mm) axis Z cut targets Back

// ==== Lens: the collar and three nested stages; every stage's place is a live extrude offset
// (sketch v is -z on XZ, and offsets run along +Y; the lens points to -Y)
sketch CollarSk on XZ {
  circle o = (lens_x, -lens_z) radius 19 mm
  fix o
}
extrude CollarTube from CollarSk distance 8.2 mm offset (-2 mm) new -> body Collar
sketch CollarFlangeSk on XZ {
  circle o = (lens_x, -lens_z) radius 21.5 mm
  fix o
}
extrude CollarFlange from CollarFlangeSk distance 1.2 mm offset wall join targets Collar
sketch CollarLipSk on XZ {
  circle o = (lens_x, -lens_z) radius (r1 + 0.4 mm)
  fix o
}
extrude CollarLip from CollarLipSk distance 10 mm offset (-3 mm) cut targets Collar
sketch CollarBoreSk on XZ {
  circle o = (lens_x, -lens_z) radius (r1 + 1.2 mm)
  fix o
}
extrude CollarBore from CollarBoreSk distance 8 mm offset (-1 mm) cut targets Collar

for k from 1 to 3 {
  sketch Stage{k}Sk on XZ {
    circle o = (lens_x, -lens_z) radius (r1 - (k - 1)*2.4 mm)
    fix o
  }
  extrude Stage{k}Tube from Stage{k}Sk distance stage_l offset (-2 mm - k*0.5 mm - k*zoom*stage_run) new -> body Stage{k}
  sketch Stage{k}FlangeSk on XZ {
    circle o = (lens_x, -lens_z) radius (r1 - (k - 1)*2.4 mm + 0.9 mm)
    fix o
  }
  extrude Stage{k}Flange from Stage{k}FlangeSk distance 1.2 mm offset (-2 mm - k*0.5 mm + stage_l - 1.2 mm - k*zoom*stage_run) join targets Stage{k}
  sketch Stage{k}LipSk on XZ {
    circle o = (lens_x, -lens_z) radius (r1 - (k - 1)*2.4 mm - 2 mm)
    fix o
  }
  extrude Stage{k}Lip from Stage{k}LipSk distance (stage_l + 2 mm) offset (-3 mm - k*0.5 mm - k*zoom*stage_run) cut targets Stage{k}
  sketch Stage{k}BoreSk on XZ {
    circle o = (lens_x, -lens_z) radius (r1 - (k - 1)*2.4 mm - 1.2 mm)
    fix o
  }
  extrude Stage{k}Bore from Stage{k}BoreSk distance stage_l offset (-1 mm - k*0.5 mm - k*zoom*stage_run) cut targets Stage{k}
}

// ==== Flash diffuser, pressed into its window from inside
sketch FlashSk on XZ {
  rect rounded f = (8 mm + fit, -(H - 14 mm + fit)) (24 mm - fit, -(H - 7 mm - fit)) radius 1.3 mm
  fix f
}
extrude FlashPlug from FlashSk distance wall offset 0.2 mm new -> body Flash
sketch FlashFlangeSk on XZ {
  rect rounded f = (6.5 mm, -(H - 15.5 mm)) (25.5 mm, -(H - 5.5 mm)) radius 2 mm
  fix f
}
extrude FlashFlange from FlashFlangeSk distance 0.8 mm offset (wall) join targets Flash

// ==== Caps: stem through the wall, flange inside on the switch
cylinder ShutterStem radius 4.1 mm height (wall + 1.8 mm) base (shutter_x, top_y, in_top - 0.2 mm) axis Z new -> body Shutter
cylinder ShutterFlange radius 5.6 mm height 1 mm base (shutter_x, top_y, in_top - 1 mm) axis Z join targets Shutter
fillet ShutterTop edges (shutter_x + 4.1 mm, top_y, H + 1.6 mm) radius 0.8 mm body Shutter
sketch PowerCapSk on XY offset (in_top - 0.2 mm) {
  rect rounded p = (power_x - 4 mm, top_y - 1.5 mm) (power_x + 4 mm, top_y + 1.5 mm) radius 1.4 mm
  fix p
}
extrude PowerStem from PowerCapSk distance (wall + 1.2 mm) new -> body Power
box PowerFlange size 11 mm 6 mm 1 mm corner (power_x - 5.5 mm, top_y - 3 mm, in_top - 1 mm) join targets Power

cylinder UpStem radius key_r height (wall + key_out + 0.2 mm) base (pad_x, in_back - 0.2 mm, pad_z + 7 mm) axis Y new -> body KeyUp
cylinder UpFlange radius (key_r + 1.4 mm) height 1 mm base (pad_x, in_back - 1 mm, pad_z + 7 mm) axis Y join targets KeyUp
cylinder DownStem radius key_r height (wall + key_out + 0.2 mm) base (pad_x, in_back - 0.2 mm, pad_z - 7 mm) axis Y new -> body KeyDown
cylinder DownFlange radius (key_r + 1.4 mm) height 1 mm base (pad_x, in_back - 1 mm, pad_z - 7 mm) axis Y join targets KeyDown
cylinder LeftStem radius key_r height (wall + key_out + 0.2 mm) base (pad_x + 7 mm, in_back - 0.2 mm, pad_z) axis Y new -> body KeyLeft
cylinder LeftFlange radius (key_r + 1.4 mm) height 1 mm base (pad_x + 7 mm, in_back - 1 mm, pad_z) axis Y join targets KeyLeft
cylinder RightStem radius key_r height (wall + key_out + 0.2 mm) base (pad_x - 7 mm, in_back - 0.2 mm, pad_z) axis Y new -> body KeyRight
cylinder RightFlange radius (key_r + 1.4 mm) height 1 mm base (pad_x - 7 mm, in_back - 1 mm, pad_z) axis Y join targets KeyRight
cylinder FuncStem radius (key_r + 0.6 mm) height (wall + key_out + 0.2 mm) base (pad_x, in_back - 0.2 mm, pad_z) axis Y new -> body Func
cylinder FuncFlange radius (key_r + 1.7 mm) height 1 mm base (pad_x, in_back - 1 mm, pad_z) axis Y join targets Func
cylinder AStem radius key_r height (wall + key_out + 0.2 mm) base (pad_x - 4.5 mm, in_back - 0.2 mm, 14 mm) axis Y new -> body KeyA
cylinder AFlange radius (key_r + 1.4 mm) height 1 mm base (pad_x - 4.5 mm, in_back - 1 mm, 14 mm) axis Y join targets KeyA
cylinder BStem radius key_r height (wall + key_out + 0.2 mm) base (pad_x + 4.5 mm, in_back - 0.2 mm, 14 mm) axis Y new -> body KeyB
cylinder BFlange radius (key_r + 1.4 mm) height 1 mm base (pad_x + 4.5 mm, in_back - 1 mm, 14 mm) axis Y join targets KeyB

// ==== Bought parts, for fit
box XiaoStack size xiao_w xiao_d xiao_h corner (lens_x - xiao_w/2, xiao_y, lens_z - xiao_h/2) new -> body Xiao
box TftModule size tft_l tft_t tft_w corner (tft_x - tft_l/2, tft_y + 0.05 mm, H/2 - tft_w/2) new -> body Tft
box LiPo size bat_l bat_t bat_w corner (4 mm, wall + 0.05 mm, 12 mm) new -> body Battery
box BackPerf size 22 mm 1.6 mm 40 mm corner (2.5 mm, board_top - 1.6 mm, 8 mm) new -> body ButtonBoard
box TopPerf size 38 mm 10 mm 1.6 mm corner (6 mm, top_y - 5 mm, top_board - 1.6 mm) new -> body TopBoard

color Front, Back "#c8ccd1"
color Collar, Stage1, Stage3 "#b4b9bf"
color Stage2 "#2b2d31"
color Flash "#eef1f4"
color Shutter, Power, Func "#d7dade"
color KeyUp, KeyDown, KeyLeft, KeyRight, KeyA, KeyB "#9aa0a7"
color Xiao, ButtonBoard, TopBoard "#2f7d4a"
color Tft "#101317"
color Battery "#3b5ba5"
material Front, Back, Collar, Stage1, Stage2, Stage3, Flash, Shutter, Power, Func, KeyUp, KeyDown, KeyLeft, KeyRight, KeyA, KeyB PLA
