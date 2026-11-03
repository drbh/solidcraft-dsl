// 96. Clip-on Bluetooth media remote in the style of the 2006 aluminium shuffle: a two-part case
// with a five-button pad, an ESP32-C3 and a LiPo, and a printed spring clip.
//
// Bill of materials
//   Seeed XIAO ESP32C3           21 x 17.8 mm, USB-C, LiPo charger on board (BLE HID media keys)
//   LiPo 301525 (100 mAh)        3 x 15 x 25 mm, on the XIAO's battery pads
//   5 x 6x6x4.3 mm tactile switch  through-hole, legs on 6.5 x 4.5 mm, trimmed to 1 mm
//   2 x M2 x 12 and 2 x M2 x 8 self-tapping screws
//
// It lies face up: X across its width, Y up its length (y = H is the top edge with the USB-C),
// Z out of the front (z = 0 is the back face, z = T the front); the clip sits behind, below z = 0.
// It is the shuffle's shape scaled up until the parts fit: W x H x T.
//
// How it goes together. The Back is a tray: the battery sits between ribs at the bottom, the XIAO
// lies above it with its USB-C at the slot in the top edge, and a deck spanning the tray carries
// the five switches in pockets. The Front is a shell whose face has the ring well and five holes;
// each Key has a flange under the face, so it can't fall out, and rests on its switch: Play in the
// middle, Vol+ / Vol- above and below, Prev / Next left and right. Four screws close the case: the
// two long ones at the top go through the Clip's pad first, so they hold the clip as well.
// The clip plate stands clip_gap off the back and its rolled lip comes within lip_gap of it, so
// it grips a pocket or a strap; print it in PETG for a lasting spring.
//
// Print the Back floor down and the Front face down, both without supports. Print the keys flange
// down and the clip as modelled, on its outer face, all without supports.
design ClipRemote units mm

// Case
param W = 34 mm
param H = 52 mm
param T = 14 mm
param split_z = 7 mm
param wall = 1.6 mm
param corner_r = 4 mm
param edge_r = 2 mm

// Parts (measure yours)
param mcu_w = 17.8 mm
param mcu_l = 21 mm
param mcu_t = 1.2 mm
param bat_w = 25 mm
param bat_h = 15 mm
param bat_t = 3 mm
param sw = 6 mm
param sw_h = 4.3 mm

// Pad
param pad_y = 26 mm
param pitch = 9 mm             // from the centre key to the others
param key_r = 2.5 mm
param play_r = 3.5 mm
param ring_r = 14 mm
param fit = 0.25 mm
param key_rise = 0.4 mm

// Clip
param clip_t = 1.6 mm
param clip_w = 14 mm
param clip_gap = 2.6 mm        // the plate's distance off the back
param lip_gap = 1 mm           // what is left between the lip and the back

// Screws
param screw_r = 1.15 mm
param pilot_r = 0.8 mm

let cx = W/2
let face_in = T - wall
let deck_top = split_z - 0.2 mm
let deck_t = 1.6 mm
let sw_z = deck_top - 1 mm
let key_z = sw_z + sw_h + 0.1 mm
let mcu_y0 = H - wall - 0.6 mm - mcu_l      // the XIAO's far end; its USB-C end stops just short of the top wall
let bat_y0 = 7.5 mm

// ==== Back tray
sketch BackSk on XY {
  rect rounded o = (0,0) (W,H) radius corner_r
  fix o
}
extrude BackBlock from BackSk distance split_z new -> body Back
fillet BackEdge edges (cx, 0, 0) radius edge_r body Back      // at least the wall, so the shell can offset it
shell BackHollow faces (cx, H/2, split_z) thickness wall inside body Back

// Battery ribs and XIAO rails
box BatRibB size (bat_w + 2.4 mm) 1.2 mm 2 mm corner (cx - bat_w/2 - 1.2 mm, bat_y0 - 1.5 mm, wall - 0.2 mm) join targets Back
box BatRibT size (bat_w + 2.4 mm) 1.2 mm 2 mm corner (cx - bat_w/2 - 1.2 mm, bat_y0 + bat_h + 0.3 mm, wall - 0.2 mm) join targets Back
box McuRailL size 1.2 mm (mcu_l - 6 mm) 1.8 mm corner (cx - mcu_w/2 - 1.5 mm, mcu_y0 + 3 mm, wall - 0.2 mm) join targets Back
box McuRailR size 1.2 mm (mcu_l - 6 mm) 1.8 mm corner (cx + mcu_w/2 + 0.3 mm, mcu_y0 + 3 mm, wall - 0.2 mm) join targets Back

// USB-C slot through the top edge
sketch UsbSk on XZ offset (H - wall - 1 mm) {
  rect rounded u = (cx - 4.9 mm, -(wall + mcu_t + 3.6 mm)) (cx + 4.9 mm, -(wall + mcu_t - 0.4 mm)) radius 1.4 mm
  fix u
}
extrude UsbSlot from UsbSk distance (wall + 2 mm) cut targets Back

// Switch deck across the tray, with a pocket and four leg holes per switch
box Deck size (W - 2*wall + 0.6 mm) (2*pitch + sw + 4 mm) deck_t corner (wall - 0.3 mm, pad_y - pitch - sw/2 - 2 mm, deck_top - deck_t) join targets Back
sketch PocketSk on XY offset (deck_top + 0.2 mm) {
  rect c = (cx - 4.15 mm, pad_y - 3.15 mm) (cx + 4.15 mm, pad_y + 3.15 mm)
  rect u = (cx - 4.15 mm, pad_y + pitch - 3.15 mm) (cx + 4.15 mm, pad_y + pitch + 3.15 mm)
  rect d = (cx - 4.15 mm, pad_y - pitch - 3.15 mm) (cx + 4.15 mm, pad_y - pitch + 3.15 mm)
  rect l = (cx - pitch - 4.15 mm, pad_y - 3.15 mm) (cx - pitch + 4.15 mm, pad_y + 3.15 mm)
  rect r = (cx + pitch - 4.15 mm, pad_y - 3.15 mm) (cx + pitch + 4.15 mm, pad_y + 3.15 mm)
  fix c u d l r
}
extrude Pockets from PocketSk distance 1.2 mm negative cut targets Back
sketch LegSk on XY offset (deck_top + 0.2 mm) {
  circle a = (cx - 3.25 mm, pad_y - 2.25 mm) diameter 1.3 mm
  circle b = (cx + 3.25 mm, pad_y - 2.25 mm) diameter 1.3 mm
  circle c = (cx - 3.25 mm, pad_y + 2.25 mm) diameter 1.3 mm
  circle d = (cx + 3.25 mm, pad_y + 2.25 mm) diameter 1.3 mm
  fix a b c d
}
extrude Legs from LegSk distance (deck_t + 1 mm) negative cut targets Back
for k from 0 to 1 {
  sketch SideLegSk{k} on XY offset (deck_top + 0.2 mm) {
    circle a = (cx + (2*k - 1)*pitch - 3.25 mm, pad_y - 2.25 mm) diameter 1.3 mm
    circle b = (cx + (2*k - 1)*pitch + 3.25 mm, pad_y - 2.25 mm) diameter 1.3 mm
    circle c = (cx + (2*k - 1)*pitch - 3.25 mm, pad_y + 2.25 mm) diameter 1.3 mm
    circle d = (cx + (2*k - 1)*pitch + 3.25 mm, pad_y + 2.25 mm) diameter 1.3 mm
    fix a b c d
  }
  extrude SideLegs{k} from SideLegSk{k} distance (deck_t + 1 mm) negative cut targets Back
  sketch EndLegSk{k} on XY offset (deck_top + 0.2 mm) {
    circle a = (cx - 3.25 mm, pad_y + (2*k - 1)*pitch - 2.25 mm) diameter 1.3 mm
    circle b = (cx + 3.25 mm, pad_y + (2*k - 1)*pitch - 2.25 mm) diameter 1.3 mm
    circle c = (cx - 3.25 mm, pad_y + (2*k - 1)*pitch + 2.25 mm) diameter 1.3 mm
    circle d = (cx + 3.25 mm, pad_y + (2*k - 1)*pitch + 2.25 mm) diameter 1.3 mm
    fix a b c d
  }
  extrude EndLegs{k} from EndLegSk{k} distance (deck_t + 1 mm) negative cut targets Back
}

// ==== Front shell: the rounded face with the ring well and the five key holes
sketch FrontSk on XY offset split_z {
  rect rounded o = (0,0) (W,H) radius corner_r
  fix o
}
extrude FrontBlock from FrontSk distance (T - split_z) new -> body Front
fillet FaceEdge edges (cx, 0, T) radius edge_r body Front
shell FrontHollow faces (cx, H/2, split_z) thickness wall inside body Front
cylinder RingWell radius ring_r height 1 mm base (cx, pad_y, T - 0.4 mm) axis Z cut targets Front
cylinder RingHub radius (play_r + 1.4 mm) height 0.6 mm base (cx, pad_y, T - 0.5 mm) axis Z join targets Front
cylinder PlayHole radius (play_r + fit) height (wall + 1 mm) base (cx, pad_y, face_in - 0.5 mm) axis Z cut targets Front
for k from 0 to 3 {
  cylinder KeyHole{k} radius (key_r + fit) height (wall + 1 mm) base (cx + pitch*cos(k*90 deg), pad_y + pitch*sin(k*90 deg), face_in - 0.5 mm) axis Z cut targets Front
}

// ==== Screws: two at the top through the clip, two at the bottom
for k from 0 to 1 {
  cylinder BackBossTop{k} radius 2.4 mm height (split_z - wall + 0.2 mm) base (4.5 mm + k*(W - 9 mm), H - 8 mm, wall - 0.2 mm) axis Z join targets Back
  cylinder BackBossBot{k} radius 2.4 mm height (split_z - wall + 0.2 mm) base (4.5 mm + k*(W - 9 mm), 4.5 mm, wall - 0.2 mm) axis Z join targets Back
  cylinder FrontBossTop{k} radius 2.2 mm height (face_in - split_z + 0.2 mm) base (4.5 mm + k*(W - 9 mm), H - 8 mm, split_z) axis Z join targets Front
  cylinder FrontBossBot{k} radius 2.2 mm height (face_in - split_z + 0.2 mm) base (4.5 mm + k*(W - 9 mm), 4.5 mm, split_z) axis Z join targets Front
}
sketch ScrewSk on XY offset (-0.5 mm) {
  circle a = (4.5 mm, 4.5 mm) radius screw_r
  circle b = (W - 4.5 mm, 4.5 mm) radius screw_r
  circle c = (4.5 mm, H - 8 mm) radius screw_r
  circle d = (W - 4.5 mm, H - 8 mm) radius screw_r
  fix a b c d
}
extrude ScrewHoles from ScrewSk distance (split_z + 1 mm) cut targets Back
sketch HeadSk on XY offset (-0.5 mm) {
  circle a = (4.5 mm, 4.5 mm) radius 2 mm
  circle b = (W - 4.5 mm, 4.5 mm) radius 2 mm
  fix a b
}
extrude ScrewHeads from HeadSk distance 1.6 mm cut targets Back
sketch PilotSk on XY offset (split_z - 0.5 mm) {
  circle a = (4.5 mm, 4.5 mm) radius pilot_r
  circle b = (W - 4.5 mm, 4.5 mm) radius pilot_r
  circle c = (4.5 mm, H - 8 mm) radius pilot_r
  circle d = (W - 4.5 mm, H - 8 mm) radius pilot_r
  fix a b c d
}
extrude Pilots from PilotSk distance 5.5 mm cut targets Front

// ==== Keys: a flange under the face and a stem through it, each with its mark
cylinder PlayFlange radius (play_r + 1.3 mm) height (face_in - key_z - 0.25 mm) base (cx, pad_y, key_z) axis Z new -> body Play
cylinder PlayStem radius play_r height (T + key_rise - face_in + 0.4 mm) base (cx, pad_y, face_in - 0.4 mm) axis Z join targets Play
for k from 0 to 3 {
  cylinder KeyFlange{k} radius (key_r + 1.3 mm) height (face_in - key_z - 0.25 mm) base (cx + pitch*cos(k*90 deg), pad_y + pitch*sin(k*90 deg), key_z) axis Z new -> body Key{k}
  cylinder KeyStem{k} radius key_r height (T + key_rise - face_in + 0.4 mm) base (cx + pitch*cos(k*90 deg), pad_y + pitch*sin(k*90 deg), face_in - 0.4 mm) axis Z join targets Key{k}
}
sketch PlayMark on XY offset (T + key_rise - 0.05 mm) {
  polyline closed t = (cx - 1 mm, pad_y - 1.3 mm) (cx + 1.4 mm, pad_y) (cx - 1 mm, pad_y + 1.3 mm)
  fix t
}
extrude PlayMarkUp from PlayMark distance 0.35 mm join targets Play
sketch PlusMark on XY offset (T + key_rise - 0.05 mm) {
  rect h = (cx - 1.3 mm, pad_y + pitch - 0.3 mm) (cx + 1.3 mm, pad_y + pitch + 0.3 mm)
  rect v = (cx - 0.3 mm, pad_y + pitch - 1.3 mm) (cx + 0.3 mm, pad_y + pitch + 1.3 mm)
  fix h v
}
extrude PlusUp from PlusMark distance 0.35 mm join targets Key1
sketch MinusMark on XY offset (T + key_rise - 0.05 mm) {
  rect h = (cx - 1.3 mm, pad_y - pitch - 0.3 mm) (cx + 1.3 mm, pad_y - pitch + 0.3 mm)
  fix h
}
extrude MinusUp from MinusMark distance 0.35 mm join targets Key3
sketch NextMark on XY offset (T + key_rise - 0.05 mm) {
  polyline closed t = (cx + pitch - 1.2 mm, pad_y - 1 mm) (cx + pitch + 0.6 mm, pad_y) (cx + pitch - 1.2 mm, pad_y + 1 mm)
  fix t
}
extrude NextUp from NextMark distance 0.35 mm join targets Key0
sketch PrevMark on XY offset (T + key_rise - 0.05 mm) {
  polyline closed t = (cx - pitch + 1.2 mm, pad_y - 1 mm) (cx - pitch - 0.6 mm, pad_y) (cx - pitch + 1.2 mm, pad_y + 1 mm)
  fix t
}
extrude PrevUp from PrevMark distance 0.35 mm join targets Key2

// ==== Clip: a pad across the top screws, a spring plate down the back, a rolled lip
box ClipPad size (W - 4 mm) 7 mm (clip_gap + clip_t) corner (2 mm, H - 11.5 mm, -clip_gap - clip_t) new -> body Clip
box ClipPlate size clip_w (H - 14 mm) clip_t corner (cx - clip_w/2, 6 mm, -clip_gap - clip_t) join targets Clip
cylinder ClipLip radius ((clip_gap - lip_gap + clip_t)/2) height clip_w base (cx - clip_w/2, 7.5 mm, -clip_gap - clip_t + (clip_gap - lip_gap + clip_t)/2) axis (1,0,0) join targets Clip
sketch ClipHoleSk on XY offset (-clip_gap - clip_t - 0.5 mm) {
  circle a = (4.5 mm, H - 8 mm) radius screw_r
  circle b = (W - 4.5 mm, H - 8 mm) radius screw_r
  fix a b
}
extrude ClipHoles from ClipHoleSk distance (clip_gap + clip_t + 1 mm) cut targets Clip
sketch ClipHeadSk on XY offset (-clip_gap - clip_t - 0.5 mm) {
  circle a = (4.5 mm, H - 8 mm) radius 2 mm
  circle b = (W - 4.5 mm, H - 8 mm) radius 2 mm
  fix a b
}
extrude ClipHeads from ClipHeadSk distance 2 mm cut targets Clip

// ==== The bought parts, placed (not printed)
box McuPcb size mcu_w mcu_l mcu_t corner (cx - mcu_w/2, mcu_y0, wall) new -> body Xiao
box McuUsb size 8.9 mm 7.4 mm 3.2 mm corner (cx - 4.45 mm, mcu_y0 + mcu_l - 6.8 mm, wall + mcu_t - 0.1 mm) join targets Xiao
box Cell size bat_w bat_h bat_t corner (cx - bat_w/2, bat_y0, wall) new -> body Battery
box SwBodyC size sw sw 3 mm corner (cx - sw/2, pad_y - sw/2, sw_z) new -> body Switch0
cylinder SwPlungerC radius 1.75 mm height (sw_h - 2.9 mm) base (cx, pad_y, sw_z + 2.9 mm) axis Z join targets Switch0
for k from 1 to 4 {
  box SwBody{k} size sw sw 3 mm corner (cx + pitch*cos((k - 1)*90 deg) - sw/2, pad_y + pitch*sin((k - 1)*90 deg) - sw/2, sw_z) new -> body Switch{k}
  cylinder SwPlunger{k} radius 1.75 mm height (sw_h - 2.9 mm) base (cx + pitch*cos((k - 1)*90 deg), pad_y + pitch*sin((k - 1)*90 deg), sw_z + 2.9 mm) axis Z join targets Switch{k}
}

// ==== Finish
color Back, Front "#3f7fd0"
color Play, Key0, Key1, Key2, Key3 "#e9edf2"
color Clip "#c4ccd6"
color Xiao "#2f6b3a"
color Battery "#9aa4b0"
color Switch0, Switch1, Switch2, Switch3, Switch4 "#303030"
material Back, Front, Play, Key0, Key1, Key2, Key3, Clip PLA
