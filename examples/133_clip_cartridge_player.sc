// 133. Keychain cartridge music player in the style of 2000 Hit Clips, printable: a two-part case
// with a speaker grille, three keys and a keyring lug, and a song cartridge that clicks into the top.
//
// Bill of materials
//   Seeed XIAO ESP32S3           21 x 17.8 mm, USB-C, LiPo charger on board (decodes MP3 to I2S)
//   MAX98357A I2S amp breakout   17.8 x 19.4 mm (Adafruit 3006 footprint), wired to the speaker
//   20 mm round speaker          8 ohm 0.5-1 W, 4 mm deep
//   microSD socket breakout      about 18 x 18 mm, a plain push-pull socket (NOT push-push)
//   microSD card                 the cartridge's "chip": its own pads are the edge contacts
//   LiPo 301525 (100 mAh)        3 x 15 x 25 mm, on the XIAO's battery pads
//   3 x 6x6x4.3 mm tactile switch  through-hole, legs trimmed to 1 mm and wired
//   4 x M2 x 12 self-tapping screws, a split keyring
//
// It lies face up: X across its width, Y up its length (y = H is the top edge, where the cartridge
// goes in), Z out of the face (z = 0 is the back face, z = T the front). The original was a 35 mm
// square; this one is scaled up until the modules fit.
//
// How it goes together. The Back is a tray in two layers. On its floor lie the XIAO (USB-C out
// of the bottom edge) and the microSD breakout, its socket facing the top edge, and the bay for
// the cartridge, between two rails. A deck at split height covers them all: the three switches
// sit in pockets at its bottom end, and the battery and amp lie on it over the bay. The Front is
// a shell whose face has the grille, a ring that locates the speaker behind it, and three key
// holes; each Key has a flange under the face, so it can't fall out, and rests on its switch.
// Four screws from the back close the case.
//
// The cartridge carries a microSD card in a pocket in its nose, 11 mm of the card standing out.
// Push it into the top slot and the card enters the socket; near the end a spring arm on each
// side of the cartridge rides over the rail and its bump drops into a notch with a click, the
// cartridge's nose fit_gap short of the stops. Pull it by the lip to change songs. A song set is
// just the files on that card, so one cartridge per album.
//
// Print the Back floor down and the Front face down, both without supports. Print the keys flange
// down and the cartridge flat on its back, so its spring arms are cut cleanly through the layers.
design ClipCartridgePlayer units mm

// Case
param W = 44 mm
param H = 64 mm
param T = 16 mm
param split_z = 9 mm
param wall = 1.6 mm
param corner_r = 5 mm
param edge_r = 2 mm

// Modules (measure yours)
param mcu_w = 17.8 mm
param mcu_l = 21 mm
param amp_w = 17.8 mm
param amp_l = 19.4 mm
param sd_board = 18 mm
param bat_w = 15 mm
param bat_l = 25 mm
param bat_t = 3 mm
param spk_d = 20 mm
param spk_t = 4 mm
param sw = 6 mm
param sw_h = 4.3 mm

// Keys
param key_y = 6 mm
param pitch = 9 mm
param key_r = 2.5 mm
param fit = 0.25 mm

// Cartridge
param cart_w = 24 mm
param cart_t = 5 mm
param lip = 4 mm               // how far it stands out of the top edge
param card_out = 11 mm         // card standing out of the nose, into the socket
param arm_t = 1.2 mm           // spring arm thickness
param arm_l = 10 mm
param bump = 0.6 mm            // how far the latch bump stands proud of the arm
param fit_gap = 0.2 mm

// Screws
param screw_r = 1.15 mm
param pilot_r = 0.8 mm

let cx = W/2
let face_in = T - wall
let deck_t = 1.6 mm
let deck_top = split_z - 0.2 mm
let sw_z = deck_top - 1 mm
let key_z = sw_z + sw_h + 0.1 mm
let spk_y = 22 mm
let floor_z = wall
let sd_y0 = 23.4 mm                          // the microSD board's far end
let nose_y = sd_y0 + sd_board + fit_gap      // the seated cartridge's nose
let cart_z = floor_z + 0.25 mm
let card_z = floor_z + 2.1 mm                // the socket's card plane
let rail_in = cx - cart_w/2 - 0.25 mm        // the left rail's inner face
let latch_y = nose_y + 6 mm                  // the bump, near each arm's free end

// ==== Back tray
sketch BackSk on XY {
  rect rounded o = (0,0) (W,H) radius corner_r
  fix o
}
extrude BackBlock from BackSk distance split_z new -> body Back
fillet BackEdge edges (cx, 0, 0) radius edge_r body Back
shell BackHollow faces (cx, H/2, split_z) thickness wall inside body Back

// XIAO rails and the microSD board's locating ribs
box McuRailL size 1.2 mm (mcu_l - 6 mm) 1.8 mm corner (cx - mcu_w/2 - 1.5 mm, 4 mm, wall - 0.2 mm) join targets Back
box McuRailR size 1.2 mm (mcu_l - 6 mm) 1.8 mm corner (cx + mcu_w/2 + 0.3 mm, 4 mm, wall - 0.2 mm) join targets Back
box SdRibL size 1.2 mm (sd_board - 6 mm) 1.8 mm corner (cx - sd_board/2 - 1.5 mm, sd_y0 + 3 mm, wall - 0.2 mm) join targets Back
box SdRibR size 1.2 mm (sd_board - 6 mm) 1.8 mm corner (cx + sd_board/2 + 0.3 mm, sd_y0 + 3 mm, wall - 0.2 mm) join targets Back

// Cartridge bay: two rails, each with a latch notch, and two stops for the nose
box BayRailL size 1.2 mm (H - wall - nose_y + 0.4 mm) (split_z - deck_t - wall - 0.4 mm) corner (rail_in - 1.2 mm, nose_y - 0.6 mm, wall - 0.2 mm) join targets Back
box BayRailR size 1.2 mm (H - wall - nose_y + 0.4 mm) (split_z - deck_t - wall - 0.4 mm) corner (W - rail_in, nose_y - 0.6 mm, wall - 0.2 mm) join targets Back
cylinder NotchL radius (bump + fit_gap) height 8 mm base (rail_in + 0.25 mm, latch_y, wall + 0.1 mm) axis Z cut targets Back
cylinder NotchR radius (bump + fit_gap) height 8 mm base (W - rail_in - 0.25 mm, latch_y, wall + 0.1 mm) axis Z cut targets Back
box StopL size 1.5 mm 1 mm 3.4 mm corner (cx - cart_w/2 + 0.5 mm, nose_y - fit_gap - 1 mm, wall - 0.2 mm) join targets Back
box StopR size 1.5 mm 1 mm 3.4 mm corner (cx + cart_w/2 - 2 mm, nose_y - fit_gap - 1 mm, wall - 0.2 mm) join targets Back

// The deck: covers the lower layer, carries the switches, the battery and the amp
box Deck size (W - 2*wall + 0.6 mm) (H - 2*wall + 0.6 mm) deck_t corner (wall - 0.3 mm, wall - 0.3 mm, deck_top - deck_t) join targets Back
box WirePass size 6 mm 5 mm 3 mm corner (3 mm, 28 mm, deck_top - deck_t - 1 mm) cut targets Back
sketch PocketSk on XY offset (deck_top + 0.2 mm) {
  rect a = (cx - pitch - 3.15 mm, key_y - 3.15 mm) (cx - pitch + 3.15 mm, key_y + 3.15 mm)
  rect b = (cx - 3.15 mm, key_y - 3.15 mm) (cx + 3.15 mm, key_y + 3.15 mm)
  rect c = (cx + pitch - 3.15 mm, key_y - 3.15 mm) (cx + pitch + 3.15 mm, key_y + 3.15 mm)
  fix a b c
}
extrude Pockets from PocketSk distance 1.2 mm negative cut targets Back
for k from 0 to 2 {
  sketch LegSk{k} on XY offset (deck_top + 0.2 mm) {
    circle a = (cx + (k - 1)*pitch - 3.25 mm, key_y - 2.25 mm) diameter 1.3 mm
    circle b = (cx + (k - 1)*pitch + 3.25 mm, key_y - 2.25 mm) diameter 1.3 mm
    circle c = (cx + (k - 1)*pitch - 3.25 mm, key_y + 2.25 mm) diameter 1.3 mm
    circle d = (cx + (k - 1)*pitch + 3.25 mm, key_y + 2.25 mm) diameter 1.3 mm
    fix a b c d
  }
  extrude Legs{k} from LegSk{k} distance (deck_t + 1 mm) negative cut targets Back
}

// USB-C slot through the bottom edge, the cartridge slot through the top edge
sketch UsbSk on XZ offset (-1 mm) {
  rect rounded u = (cx - 4.9 mm, -(wall + 1.2 mm + 3.6 mm)) (cx + 4.9 mm, -(wall + 1.2 mm - 0.4 mm)) radius 1.4 mm
  fix u
}
extrude UsbSlot from UsbSk distance (wall + 2 mm) cut targets Back
sketch CartSlotSk on XZ offset (H - wall - 1 mm) {
  rect s = (rail_in - 0.3 mm, -(cart_z + cart_t + 0.2 mm)) (W - rail_in + 0.3 mm, -(cart_z - 0.2 mm))
  fix s
}
extrude CartSlot from CartSlotSk distance (wall + 2 mm) cut targets Back

// Keyring lug off the left side, near the top
sketch LugSk on XY {
  rect rounded l = (-7 mm, H - 18 mm) (1 mm, H - 7 mm) radius 2.5 mm
  fix l
}
extrude Lug from LugSk distance 4.5 mm join targets Back
cylinder LugHole radius 2 mm height 6 mm base (-3.5 mm, H - 12.5 mm, -0.5 mm) axis Z cut targets Back

// ==== Front shell: the face with the grille, the speaker ring and the key holes
sketch FrontSk on XY offset split_z {
  rect rounded o = (0,0) (W,H) radius corner_r
  fix o
}
extrude FrontBlock from FrontSk distance (T - split_z) new -> body Front
fillet FaceEdge edges (cx, 0, T) radius edge_r body Front
shell FrontHollow faces (cx, H/2, split_z) thickness wall inside body Front
sketch GrilleSk on XY offset (face_in - 0.5 mm) {
  slot g0 = centers (cx - 5.42 mm, spk_y - 6 mm) (cx + 5.42 mm, spk_y - 6 mm) width 1.2 mm
  slot g1 = centers (cx - 6.9 mm, spk_y - 4 mm) (cx + 6.9 mm, spk_y - 4 mm) width 1.2 mm
  slot g2 = centers (cx - 7.66 mm, spk_y - 2 mm) (cx + 7.66 mm, spk_y - 2 mm) width 1.2 mm
  slot g3 = centers (cx - 7.9 mm, spk_y) (cx + 7.9 mm, spk_y) width 1.2 mm
  slot g4 = centers (cx - 7.66 mm, spk_y + 2 mm) (cx + 7.66 mm, spk_y + 2 mm) width 1.2 mm
  slot g5 = centers (cx - 6.9 mm, spk_y + 4 mm) (cx + 6.9 mm, spk_y + 4 mm) width 1.2 mm
  slot g6 = centers (cx - 5.42 mm, spk_y + 6 mm) (cx + 5.42 mm, spk_y + 6 mm) width 1.2 mm
  fix g0 g1 g2 g3 g4 g5 g6
}
extrude Grille from GrilleSk distance (wall + 1 mm) cut targets Front
cylinder SpkRing radius (spk_d/2 + 1.25 mm) height (spk_t - 0.8 mm) base (cx, spk_y, face_in - spk_t + 1 mm) axis Z join targets Front
cylinder SpkSeat radius (spk_d/2 + 0.25 mm) height (spk_t + 0.5 mm) base (cx, spk_y, face_in - spk_t - 0.5 mm) axis Z cut targets Front
for k from 0 to 2 {
  cylinder KeyHole{k} radius (key_r + fit) height (wall + 1 mm) base (cx + (k - 1)*pitch, key_y, face_in - 0.5 mm) axis Z cut targets Front
}

// ==== Screws: two at the bottom corners, two at the top corners
for k from 0 to 1 {
  cylinder BackBossBot{k} radius 2.4 mm height (deck_top - deck_t - wall + 0.4 mm) base (4.5 mm + k*(W - 9 mm), 4.5 mm, wall - 0.2 mm) axis Z join targets Back
  cylinder BackBossTop{k} radius 2.4 mm height (deck_top - deck_t - wall + 0.4 mm) base (4.5 mm + k*(W - 9 mm), H - 4.5 mm, wall - 0.2 mm) axis Z join targets Back
  cylinder FrontBossBot{k} radius 2.2 mm height (face_in - split_z + 0.2 mm) base (4.5 mm + k*(W - 9 mm), 4.5 mm, split_z) axis Z join targets Front
  cylinder FrontBossTop{k} radius 2.2 mm height (face_in - split_z + 0.2 mm) base (4.5 mm + k*(W - 9 mm), H - 4.5 mm, split_z) axis Z join targets Front
}
sketch ScrewSk on XY offset (-0.5 mm) {
  circle a = (4.5 mm, 4.5 mm) radius screw_r
  circle b = (W - 4.5 mm, 4.5 mm) radius screw_r
  circle c = (4.5 mm, H - 4.5 mm) radius screw_r
  circle d = (W - 4.5 mm, H - 4.5 mm) radius screw_r
  fix a b c d
}
extrude ScrewHoles from ScrewSk distance (split_z + 1 mm) cut targets Back
sketch HeadSk on XY offset (-0.5 mm) {
  circle a = (4.5 mm, 4.5 mm) radius 2 mm
  circle b = (W - 4.5 mm, 4.5 mm) radius 2 mm
  circle c = (4.5 mm, H - 4.5 mm) radius 2 mm
  circle d = (W - 4.5 mm, H - 4.5 mm) radius 2 mm
  fix a b c d
}
extrude ScrewHeads from HeadSk distance 1.6 mm cut targets Back
sketch PilotSk on XY offset (split_z - 0.5 mm) {
  circle a = (4.5 mm, 4.5 mm) radius pilot_r
  circle b = (W - 4.5 mm, 4.5 mm) radius pilot_r
  circle c = (4.5 mm, H - 4.5 mm) radius pilot_r
  circle d = (W - 4.5 mm, H - 4.5 mm) radius pilot_r
  fix a b c d
}
extrude Pilots from PilotSk distance 5 mm cut targets Front

// ==== Keys: Vol-, Play, Vol+
for k from 0 to 2 {
  cylinder KeyFlange{k} radius (key_r + 1.3 mm) height (face_in - key_z - 0.25 mm) base (cx + (k - 1)*pitch, key_y, key_z) axis Z new -> body Key{k}
  cylinder KeyStem{k} radius key_r height (T + 0.4 mm - face_in + 0.4 mm) base (cx + (k - 1)*pitch, key_y, face_in - 0.4 mm) axis Z join targets Key{k}
}
sketch PlayMark on XY offset (T + 0.4 mm - 0.05 mm) {
  polyline closed t = (cx - 1 mm, key_y - 1.3 mm) (cx + 1.4 mm, key_y) (cx - 1 mm, key_y + 1.3 mm)
  fix t
}
extrude PlayMarkUp from PlayMark distance 0.35 mm join targets Key1
sketch MinusMark on XY offset (T + 0.4 mm - 0.05 mm) {
  rect h = (cx - pitch - 1.3 mm, key_y - 0.3 mm) (cx - pitch + 1.3 mm, key_y + 0.3 mm)
  fix h
}
extrude MinusUp from MinusMark distance 0.35 mm join targets Key0
sketch PlusMark on XY offset (T + 0.4 mm - 0.05 mm) {
  rect h = (cx + pitch - 1.3 mm, key_y - 0.3 mm) (cx + pitch + 1.3 mm, key_y + 0.3 mm)
  rect v = (cx + pitch - 0.3 mm, key_y - 1.3 mm) (cx + pitch + 0.3 mm, key_y + 1.3 mm)
  fix h v
}
extrude PlusUp from PlusMark distance 0.35 mm join targets Key2

// ==== Cartridge: a block with a card pocket in its nose, a spring arm each side, a pull lip
box CartBlock size cart_w (H + lip - nose_y) cart_t corner (cx - cart_w/2, nose_y, cart_z) new -> body Cartridge
fillet CartLip edges (cx, H + lip, cart_z + cart_t) radius 1.5 mm body Cartridge
box CardPocket size 11.2 mm 4.7 mm 1 mm corner (cx - 5.6 mm, nose_y - 0.5 mm, card_z - 0.1 mm) cut targets Cartridge
// Each arm: a slot along it and a cut across its free end, leaving it held at the top end
box ArmSlotL size 0.6 mm (arm_l + 0.6 mm) (cart_t + 1 mm) corner (cx - cart_w/2 + arm_t, latch_y - 2.1 mm, cart_z - 0.5 mm) cut targets Cartridge
box ArmEndL size (arm_t + 1.3 mm) 0.6 mm (cart_t + 1 mm) corner (cx - cart_w/2 - 1 mm, latch_y - 2.1 mm, cart_z - 0.5 mm) cut targets Cartridge
box ArmSlotR size 0.6 mm (arm_l + 0.6 mm) (cart_t + 1 mm) corner (cx + cart_w/2 - arm_t - 0.6 mm, latch_y - 2.1 mm, cart_z - 0.5 mm) cut targets Cartridge
box ArmEndR size (arm_t + 1.3 mm) 0.6 mm (cart_t + 1 mm) corner (cx + cart_w/2 - arm_t - 0.3 mm, latch_y - 2.1 mm, cart_z - 0.5 mm) cut targets Cartridge
cylinder BumpL radius bump height (cart_t - 1 mm) base (cx - cart_w/2, latch_y, cart_z + 0.5 mm) axis Z join targets Cartridge
cylinder BumpR radius bump height (cart_t - 1 mm) base (cx + cart_w/2, latch_y, cart_z + 0.5 mm) axis Z join targets Cartridge
// A grip groove across the lip
cylinder Grip radius 1 mm height (cart_w + 2 mm) base (cx - cart_w/2 - 1 mm, H + lip/2, cart_z + cart_t + 0.5 mm) axis (1,0,0) cut targets Cartridge

// ==== The bought parts, placed (not printed)
box McuPcb size mcu_w mcu_l 1.2 mm corner (cx - mcu_w/2, wall + 0.6 mm, wall) new -> body Xiao
box McuUsb size 8.9 mm 7.4 mm 3.2 mm corner (cx - 4.45 mm, wall + 0.05 mm, wall + 1.1 mm) join targets Xiao
box SdPcb size sd_board sd_board 1.6 mm corner (cx - sd_board/2, sd_y0, wall) new -> body MicroSdBoard
box SdSocket size 14 mm 15 mm 1.8 mm corner (cx - 7 mm, sd_y0 + sd_board - 15 mm, wall + 1.6 mm - 0.1 mm) join targets MicroSdBoard
box SdCardSlot size 11.6 mm 12 mm 1.1 mm corner (cx - 5.8 mm, sd_y0 + sd_board - 11.9 mm, card_z - 0.15 mm) cut targets MicroSdBoard
box CardChip size 11 mm 15 mm 0.8 mm corner (cx - 5.5 mm, nose_y - card_out, card_z) new -> body Card
box Cell size bat_w bat_l bat_t corner (7 mm, 34.5 mm, deck_top) new -> body Battery
box AmpPcb size amp_w amp_l 1.6 mm corner (W - wall - 0.6 mm - amp_w, 37.5 mm, deck_top) new -> body Amp
cylinder SpkBody radius (spk_d/2) height spk_t base (cx, spk_y, face_in - spk_t) axis Z new -> body Speaker
for k from 0 to 2 {
  box SwBody{k} size sw sw 3 mm corner (cx + (k - 1)*pitch - sw/2, key_y - sw/2, sw_z) new -> body Switch{k}
  cylinder SwPlunger{k} radius 1.75 mm height (sw_h - 2.9 mm) base (cx + (k - 1)*pitch, key_y, sw_z + 2.9 mm) axis Z join targets Switch{k}
}

// ==== Fits
check Cartridge clearance Back >= 0.15 mm
check Card clearance Cartridge >= 0.05 mm
check Card clearance MicroSdBoard >= 0.05 mm
check Cartridge clearance MicroSdBoard >= 0.1 mm
check Key0 clearance Front >= 0.2 mm
check Key1 clearance Front >= 0.2 mm
check Key2 clearance Front >= 0.2 mm
check Key1 clearance Switch1 >= 0.05 mm
check Speaker clearance Front >= 0 mm
check Battery clearance Front >= 0.3 mm
check Amp clearance Front >= 0.3 mm
check Xiao clearance Back >= 0 mm
check Front clearance Back >= 0 mm

// ==== Finish
color Back, Front "#f2f2ee"
color Key0, Key1, Key2 "#ff5fa2"
color Cartridge "#7a4fd6"
color Xiao "#2f6b3a"
color MicroSdBoard, Amp "#1f4fa8"
color Card "#202020"
color Battery "#9aa4b0"
color Speaker "#606060"
color Switch0, Switch1, Switch2 "#303030"
material Back, Front, Key0, Key1, Key2, Cartridge PLA
