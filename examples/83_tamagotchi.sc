// 83. Virtual pet keychain you can build: an egg-shaped two-part case around a XIAO ESP32C3, a 0.96" OLED,
// three buttons and a small LiPo.
//
// Bill of materials
//   Seeed XIAO ESP32C3           21 x 17.8 mm, USB-C on its short edge, LiPo charger on board
//   0.96" SSD1306 OLED, I2C      27.3 x 27.8 mm module, 4 x 2 mm holes on 23.3 x 23.8 mm, headers removed
//   3 x 6x6x5 mm tactile switch  through-hole, legs on 6.5 x 4.5 mm
//   LiPo 502030 (250 mAh)        5 x 20 x 30 mm
//   3 x M2 x 10 self-tapping screw, a split ring and a ball chain
//
// It lies face up: X across the egg, Y up it (the egg's middle at y = 0, its narrow end at the
// top), Z out of the face; the back sits on z = 0 and the case splits at z = split_z. The outline is
// a four-arc egg (a semicircle at the bottom, two side arcs side_k times larger and a cap of radius
// top_r, each tangent to the next), scaled up from the toy so the parts fit.
//
// How it goes together. The Back is a tray: the battery drops between four low ribs on its floor,
// the XIAO lies between two rails below it with its USB-C at the slot in the bottom of the egg, and
// the SwitchDeck, glued on two pillars over the XIAO, carries the three switches in pockets, their
// legs through holes for wires.
// The Front is a shell whose face has the OLED window and three button holes. The OLED hangs from
// four pegs through its mounting holes (melt the peg tips or add a drop of glue), its glass against
// the face. Each Cap has a flange under the face, so it can't fall out, and rests on its switch.
// Three M2 screws go in from the back through bosses and bite into the Front's bosses.
//
// Print the Back floor down and the Front face down, both without supports (the window, holes
// and pegs are all vertical). Print the caps flange down and the SwitchDeck flat. PLA or PETG,
// 0.2 mm layers.
design VirtualPet units mm

// Egg
param egg_w = 54 mm
param side_k = 1.5             // side arc radius over the bottom radius: larger is pointier
param top_r = 16 mm
param split_z = 8 mm           // the back shell's height
param case_t = 17 mm           // overall thickness
param wall = 1.8 mm
param back_r = 4 mm            // round of the back edge
param face_r = 5 mm            // round of the face edge

// Parts (measure yours)
param oled_w = 27.3 mm
param oled_h = 27.8 mm
param oled_pcb_t = 1.2 mm
param oled_glass_w = 26.7 mm
param oled_glass_h = 19.3 mm
param oled_glass_t = 1.5 mm
param oled_hole_dx = 23.3 mm
param oled_hole_dy = 23.8 mm
param oled_y = 7.9 mm          // module centre
param win_w = 23 mm
param win_h = 12.4 mm
param win_dy = 0.8 mm          // the active area sits a little above the glass centre
param esp_w = 17.8 mm
param esp_l = 21 mm
param esp_t = 1.2 mm
param bat_w = 30 mm
param bat_h = 20 mm
param bat_t = 5 mm
param bat_y = 7.3 mm           // battery centre
param sw = 6 mm                // switch body
param sw_h = 5 mm              // switch height to the top of the plunger

// Buttons
param btn_r = 2.5 mm
param btn_gap = 11 mm
param btn_y = -19 mm
param fit = 0.25 mm            // clearance round each cap
param cap_rise = 1.5 mm        // how far the caps stand above the face

// Screws
param screw_r = 1.15 mm        // M2 clearance
param pilot_r = 0.8 mm         // M2 self-tapping pilot

// Keychain
param ring_r = 7 mm
param wire_r = 0.8 mm
param ball_d = 2.4 mm

let r = egg_w/2
let cap_y = sqrt((side_k*r - top_r)^2 - ((side_k - 1)*r)^2)
let b = (r + cap_y + top_r)/2
let y0 = r - b
let meet_x = -(side_k - 1)*r + side_k*r*(side_k - 1)*r/(side_k*r - top_r)
let meet_y = y0 + side_k*r*cap_y/(side_k*r - top_r)
let side_a = atan2(cap_y, (side_k - 1)*r)/2
let mid_x = -(side_k - 1)*r + side_k*r*cos(side_a)
let mid_y = y0 + side_k*r*sin(side_a)
let face_in = case_t - wall               // underside of the face
let glass_z = face_in - oled_glass_t
let pcb_z = glass_z - oled_pcb_t
let win_y = oled_y + win_dy
let esp_y0 = -b + wall + 3.4 mm           // the XIAO's USB end, clear of the curved wall
let bridge_top = split_z + 0.4 mm
let bridge_t = 1.8 mm
let sw_z = bridge_top - 1 mm              // switch seat, 1 mm down in its pocket
let cap_z = sw_z + sw_h + 0.1 mm          // cap underside, just clear of the plunger
let eye_y = b + 2.8 mm
let ring_y = eye_y + ring_r

// ==== Back: the egg, rounded underneath, hollowed to a tray
sketch BackSk on XY {
  arc bottom = start (-r, y0) through (0, y0 - r) end (r, y0)
  arc right = start bottom.end through (mid_x, mid_y) end (meet_x, meet_y)
  arc cap = start right.end through (0, y0 + cap_y + top_r) end (-meet_x, meet_y)
  arc left = start cap.end through (-mid_x, mid_y) end bottom.start
  fix bottom right cap left
}
extrude BackBlock from BackSk distance split_z new -> body Back
fillet BackEdge edges (0, -b, 0) radius back_r body Back
shell BackHollow faces (0, 0, split_z) thickness wall inside body Back

// Battery bay: four low ribs round the cell
box BatRibL size 1.2 mm (bat_h + 2.4 mm) 2.6 mm corner (-bat_w/2 - 1.5 mm, bat_y - bat_h/2 - 1.2 mm, wall - 0.2 mm) join targets Back
box BatRibR size 1.2 mm (bat_h + 2.4 mm) 2.6 mm corner (bat_w/2 + 0.3 mm, bat_y - bat_h/2 - 1.2 mm, wall - 0.2 mm) join targets Back
box BatRibT size (bat_w + 2.6 mm) 1.2 mm 2.4 mm corner (-bat_w/2 - 1.3 mm, bat_y + bat_h/2 + 0.6 mm, wall - 0.2 mm) join targets Back
box BatRibB size (bat_w + 2.6 mm) 1.2 mm 2.4 mm corner (-bat_w/2 - 1.3 mm, bat_y - bat_h/2 - 1.8 mm, wall - 0.2 mm) join targets Back

// XIAO rails, and the USB-C slot through the bottom of the egg
box EspRailL size 1.5 mm (esp_l - 4 mm) 2 mm corner (-esp_w/2 - 1.8 mm, esp_y0 + 2 mm, wall - 0.2 mm) join targets Back
box EspRailR size 1.5 mm (esp_l - 4 mm) 2 mm corner (esp_w/2 + 0.3 mm, esp_y0 + 2 mm, wall - 0.2 mm) join targets Back
sketch UsbSk on XZ offset (-b - 2 mm) {
  rect rounded u = (-5.1 mm, -(wall + esp_t + 4.4 mm)) (5.1 mm, -(wall + esp_t - 0.4 mm)) radius 1.5 mm
  fix u
}
extrude UsbSlot from UsbSk distance 8 mm cut targets Back

// Switch deck over the XIAO: two pillars on the Back, and the deck (a part of its own, glued on
// them) with a pocket and leg holes per switch
box PillarL size 2.5 mm 5 mm (bridge_top - bridge_t - wall + 0.2 mm) corner (-13.7 mm, btn_y - 2.5 mm, wall - 0.2 mm) join targets Back
box PillarR size 2.5 mm 5 mm (bridge_top - bridge_t - wall + 0.2 mm) corner (11.2 mm, btn_y - 2.5 mm, wall - 0.2 mm) join targets Back
box Bridge size 31 mm 8 mm bridge_t corner (-15.5 mm, btn_y - 4 mm, bridge_top - bridge_t) new -> body SwitchDeck
for k from 0 to 2 {
  box SwPocket{k} size (sw + 2.3 mm) (sw + 0.3 mm) 1.2 mm corner ((k - 1)*btn_gap - (sw + 2.3 mm)/2, btn_y - (sw + 0.3 mm)/2, sw_z) cut targets SwitchDeck   // wide enough for the legs
  sketch LegSk{k} on XY offset (bridge_top + 0.2 mm) {
    circle a = ((k - 1)*btn_gap - 3.25 mm, btn_y - 2.25 mm) diameter 1.3 mm
    circle b = ((k - 1)*btn_gap + 3.25 mm, btn_y - 2.25 mm) diameter 1.3 mm
    circle c = ((k - 1)*btn_gap - 3.25 mm, btn_y + 2.25 mm) diameter 1.3 mm
    circle d = ((k - 1)*btn_gap + 3.25 mm, btn_y + 2.25 mm) diameter 1.3 mm
    fix a b c d
  }
  extrude Legs{k} from LegSk{k} distance (bridge_t + 1 mm) negative cut targets SwitchDeck
}

// ==== Front: the same egg, rounded at the face, hollowed from below
sketch FrontSk on XY offset split_z {
  arc bottom = start (-r, y0) through (0, y0 - r) end (r, y0)
  arc right = start bottom.end through (mid_x, mid_y) end (meet_x, meet_y)
  arc cap = start right.end through (0, y0 + cap_y + top_r) end (-meet_x, meet_y)
  arc left = start cap.end through (-mid_x, mid_y) end bottom.start
  fix bottom right cap left
}
extrude FrontBlock from FrontSk distance (case_t - split_z) new -> body Front
fillet FaceEdge edges (0, -b, case_t) radius face_r body Front
shell FrontHollow faces (0, 0, split_z) thickness wall inside body Front

// OLED window and the four pegs that carry the module
sketch WindowSk on XY offset (case_t + 0.5 mm) {
  rect rounded w = (-win_w/2, win_y - win_h/2) (win_w/2, win_y + win_h/2) radius 1.5 mm
  fix w
}
extrude Window from WindowSk distance (wall + 1 mm) negative cut targets Front
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder OledPost{i}{j} radius 1.8 mm height (oled_glass_t + 0.2 mm) base ((i - 0.5)*oled_hole_dx, oled_y + (j - 0.5)*oled_hole_dy, glass_z) axis Z join targets Front
    cylinder OledPeg{i}{j} radius 0.9 mm height (oled_pcb_t + 1.2 mm) base ((i - 0.5)*oled_hole_dx, oled_y + (j - 0.5)*oled_hole_dy, pcb_z - 1 mm) axis Z join targets Front
  }
}

// Button holes
for k from 0 to 2 {
  cylinder BtnHole{k} radius (btn_r + fit) height (wall + 1 mm) base ((k - 1)*btn_gap, btn_y, face_in - 0.5 mm) axis Z cut targets Front
}

// ==== Screws: three through the Back into bosses on the Front
cylinder BackBoss0 radius 3 mm height (split_z - wall + 0.2 mm) base (-21 mm, -6 mm, wall - 0.2 mm) axis Z join targets Back
cylinder BackBoss1 radius 3 mm height (split_z - wall + 0.2 mm) base (21 mm, -6 mm, wall - 0.2 mm) axis Z join targets Back
cylinder BackBoss2 radius 3 mm height (split_z - wall + 0.2 mm) base (0, 26 mm, wall - 0.2 mm) axis Z join targets Back
cylinder FrontBoss0 radius 2.8 mm height (face_in - split_z + 0.2 mm) base (-21 mm, -6 mm, split_z) axis Z join targets Front
cylinder FrontBoss1 radius 2.8 mm height (face_in - split_z + 0.2 mm) base (21 mm, -6 mm, split_z) axis Z join targets Front
cylinder FrontBoss2 radius 2.8 mm height (face_in - split_z + 0.2 mm) base (0, 26 mm, split_z) axis Z join targets Front
sketch ScrewSk on XY offset (-0.5 mm) {
  circle a = (-21 mm, -6 mm) radius screw_r
  circle b = (21 mm, -6 mm) radius screw_r
  circle c = (0, 26 mm) radius screw_r
  fix a b c
}
extrude ScrewHoles from ScrewSk distance (split_z + 1 mm) cut targets Back
sketch HeadSk on XY offset (-0.5 mm) {
  circle a = (-21 mm, -6 mm) radius 2.1 mm
  circle b = (21 mm, -6 mm) radius 2.1 mm
  circle c = (0, 26 mm) radius 2.1 mm
  fix a b c
}
extrude ScrewHeads from HeadSk distance 2.5 mm cut targets Back
sketch PilotSk on XY offset (split_z - 0.5 mm) {
  circle a = (-21 mm, -6 mm) radius pilot_r
  circle b = (21 mm, -6 mm) radius pilot_r
  circle c = (0, 26 mm) radius pilot_r
  fix a b c
}
extrude Pilots from PilotSk distance 6 mm cut targets Front

// Keychain eyelet on top of the Back
cylinder EyeletBoss radius 4 mm height (split_z - 1 mm) base (0, eye_y, 0.5 mm) axis Z join targets Back
cylinder EyeletHole radius 1.8 mm height (split_z + 1 mm) base (0, eye_y, -0.5 mm) axis Z cut targets Back

// ==== Caps: a flange under the face, a domed stem through it
for k from 0 to 2 {
  cylinder CapFlange{k} radius (btn_r + 1.5 mm) height (face_in - cap_z - 0.25 mm) base ((k - 1)*btn_gap, btn_y, cap_z) axis Z new -> body Cap{k}
  cylinder CapStem{k} radius btn_r height (case_t + cap_rise - face_in + 0.4 mm) base ((k - 1)*btn_gap, btn_y, face_in - 0.4 mm) axis Z join targets Cap{k}
  fillet CapDome{k} edges ((k - 1)*btn_gap + btn_r, btn_y, case_t + cap_rise) radius 0.8 mm body Cap{k}
}

// ==== The bought parts, placed (not printed)
box OledPcb size oled_w oled_h oled_pcb_t corner (-oled_w/2, oled_y - oled_h/2, pcb_z) new -> body Oled
box OledGlass size oled_glass_w oled_glass_h oled_glass_t corner (-oled_glass_w/2, oled_y - oled_glass_h/2, glass_z - 0.1 mm) join targets Oled
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder OledHole{i}{j} radius 1 mm height 2 mm base ((i - 0.5)*oled_hole_dx, oled_y + (j - 0.5)*oled_hole_dy, pcb_z - 0.5 mm) axis Z cut targets Oled
  }
}
box EspPcb size esp_w esp_l esp_t corner (-esp_w/2, esp_y0, wall) new -> body Xiao
box EspUsb size 8.9 mm 7.4 mm 3.2 mm corner (-4.45 mm, esp_y0 - 0.6 mm, wall + esp_t - 0.1 mm) join targets Xiao
box Cell size bat_w bat_h bat_t corner (-bat_w/2, bat_y - bat_h/2, wall) new -> body Battery
for k from 0 to 2 {
  box SwBody{k} size sw sw 3.5 mm corner ((k - 1)*btn_gap - sw/2, btn_y - sw/2, sw_z) new -> body Switch{k}
  cylinder SwPlunger{k} radius 1.75 mm height (sw_h - 3.4 mm) base ((k - 1)*btn_gap, btn_y, sw_z + 3.4 mm) axis Z join targets Switch{k}
}
torus RingWire major ring_r minor wire_r center (0, 0, 0) new -> body Ring
move RingUp bodies Ring axis (0,1,0) angle 90 deg
move RingPlace bodies Ring to (0, ring_y, split_z/2)

// ==== Finish
color Back, Front "#e8457a"
color Cap0, Cap1, Cap2 "#f6e04b"
color SwitchDeck "#f2c6d4"
color Oled "#1d2330"
color Xiao "#2f6b3a"
color Battery "#9aa4b0"
color Switch0, Switch1, Switch2 "#303030"
color Ring "#c9ccd1"
material Back, Front, Cap0, Cap1, Cap2, SwitchDeck PLA
material Ring Steel
