// 81. Printable laptop (cyberdeck, thin-laptop shape): a Raspberry Pi 5 with a 10.1 in HDMI panel in
// the lid, a compact Bluetooth keyboard in a well, a USB touchpad, and a two-cell 18650 UPS, on two
// printed friction hinges. Every part fits a 256 x 256 mm bed.
//
// Bill of materials (measure yours; every module size is a parameter):
//   Raspberry Pi 5                     85 x 56 mm, M2.5 holes 58 x 49 mm apart
//   10.1 in 1280x800 IPS panel         235 x 143 x 4 mm, active 217 x 136 mm, with its HDMI driver
//                                      board 60 x 45 x 10 mm; a 30 cm micro-HDMI to HDMI cable
//   Logitech K380 Bluetooth keyboard   279 x 124 x 16 mm, lying in the deck's well
//   USB touchpad module                120 x 90 x 12 mm (any bare USB laptop pad), taped under the deck
//   2 x 18650 cells in a holder        77 x 41 x 21 mm, with a 5 V 5 A USB-C UPS board 60 x 34 mm
//   M3 x 8 heat-set inserts and M3 x 8 screws for the decks (16), M3 x 12 bolts and nuts across
//   the tray seam (3), M2.5 x 5 for the Pi (4), two M4 x 50 bolts with nylon nuts and washers
//   for the hinges, eight M3 x 10 screws from the back of the lid into the bezel posts
//
// Z is up, the user sits at -Y, X runs across (centred on x = 0), the base's bottom is at z = 0.
// Base: a tray split at x = 0 into two halves; low seam walls (which also carry the keyboard)
// are bolted together with three M3 bolts. The deck lies on top in two pieces split at
// x = deck_split, so its seam doesn't line up with the tray's, and 16 M3 screws into heat-set
// inserts in bosses tie it all together. The keyboard lies in the deck's well on three ribs
// with its keys flush with the deck. The touchpad is taped under its window. Under the palm rest,
// the Pi stands on the left with its USB and Ethernet through the left wall, and the cells and
// UPS on the right with USB-C through the right wall. The micro-HDMI cable runs under the keyboard
// and up through the slot between the hinges. Vents are cut under the Pi; four recesses take
// 10 mm rubber feet.
// Lid: a back shell (split at x = lid_split) and a bezel (split at x = bezel_split) screwed together from
// the outside. The panel lies in the bezel's window, pressed by pads on the shell; the driver
// board stands in the chin.
// Hinges: two, at x = +-hinge_x. Each has two knuckles on the base and one on the lid, on webs
// that run to each part's outer face so they print on the bed. An M4 bolt runs through all three;
// tighten its nylon nut until the lid holds its angle. The lid is built closed, then turned about
// the hinge axis (move the axis to the X axis, rotate by lid_angle, move it back), so lid_angle
// stays live. Bodies named Ref* stand in for the bought parts and are left out of the print.
//
// Print (examples/81_macbook.print.json): PLA (PETG is stiffer in a hot bag), 0.2 mm layers, no supports, one part per
// plate. Trays and lid shells print open side up (the lid shell outer face down), decks and
// bezels face down.
design Laptop units mm

param W = 300 mm
param D = 236 mm
param H = 26 mm                // base height, deck top
param corner_r = 12 mm
param wall = 2 mm
param deck_t = 2.5 mm
param bezel_t = 2.5 mm
param lid_h = 12.5 mm          // lid back shell height
param gap = 1.5 mm             // between deck and bezel, closed
param lid_angle = 110 deg      // 0 closed
param deck_split = -40 mm
param bezel_split = 40 mm
param lid_split = -55 mm

// Bought parts
param kb_w = 279 mm
param kb_d = 124 mm
param kb_t = 16 mm
param tp_w = 120 mm
param tp_d = 90 mm
param tp_t = 12 mm
param tp_view_w = 104 mm
param tp_view_d = 70 mm
param pi_y0 = 22 mm
param cell_w = 77 mm
param cell_d = 41 mm
param ups_w = 60 mm
param ups_d = 34 mm
param panel_w = 235 mm
param panel_d = 143 mm
param panel_t = 4 mm
param view_w = 217 mm
param view_d = 136 mm
param panel_y0 = 12 mm         // panel's edge nearest the lid's free edge

// Hinges
param hinge_x = 100 mm
param knuckle_r = 7 mm
param bolt_d = 4.3 mm

let tray_h = H - deck_t
let kb_y0 = D - wall - 4 mm - kb_d
let kb_z = H - kb_t                     // keyboard underside
let tp_y0 = wall + 5 mm
let lid_z0 = H + gap
let lid_tray_z = lid_z0 + bezel_t
let lid_top = lid_tray_z + lid_h
let zc = H + gap/2
let hy = D + knuckle_r + 0.5 mm
let px0 = -W/2 + wall + 1.5 mm          // Pi's USB edge, at the left wall
let bx = W/2 - 7 mm                     // outer screw columns

// ==== Base tray, split at x = 0
sketch TraySk on XY {
  rect rounded o = (-W/2, 0) (W/2, D) radius corner_r
  fix o
}
extrude TrayBlock from TraySk distance tray_h new -> body BaseTrayR
shell TrayCavity faces (0, D/2, tray_h) thickness wall inside body BaseTrayR
fillet TrayBottom edges (0, 0, 0) radius 2 mm body BaseTrayR
// keyboard ribs, deck bosses, Pi standoffs: all on the floor, before the split
for k from 0 to 2 {
  box KbRib{k} size (W - 2*wall - 6 mm) 3 mm (kb_z - wall + 0.5 mm) corner (-W/2 + wall + 3 mm, kb_y0 + 6 mm + k*(kb_d - 15 mm)/2, wall - 0.5 mm) join targets BaseTrayR
}
for i from 0 to 1 {
  for j from 0 to 2 {
    cylinder DeckBoss{i}{j} radius 3.5 mm height (tray_h - wall + 0.2 mm) base ((2*i - 1)*bx, 7 mm + j*(D - 14 mm)/2, wall - 0.5 mm) axis (0,0,1) join targets BaseTrayR
    cylinder DeckInsert{i}{j} radius 2 mm height 6 mm base ((2*i - 1)*bx, 7 mm + j*(D - 14 mm)/2, tray_h - 5.9 mm) axis (0,0,1) cut targets BaseTrayR
  }
  cylinder MidBoss{i} radius 3.5 mm height (tray_h - wall + 0.2 mm) base ((2*i - 1)*(tp_w/2 + 10 mm), 7 mm, wall - 0.5 mm) axis (0,0,1) join targets BaseTrayR
  cylinder MidInsert{i} radius 2 mm height 6 mm base ((2*i - 1)*(tp_w/2 + 10 mm), 7 mm, tray_h - 5.9 mm) axis (0,0,1) cut targets BaseTrayR
  cylinder SeamBoss{i} radius 3.5 mm height (tray_h - wall + 0.2 mm) base ((2*i - 1)*(tp_w/2 + 10 mm), kb_y0 - 3.5 mm, wall - 0.5 mm) axis (0,0,1) join targets BaseTrayR
  cylinder SeamInsert{i} radius 2 mm height 6 mm base ((2*i - 1)*(tp_w/2 + 10 mm), kb_y0 - 3.5 mm, tray_h - 5.9 mm) axis (0,0,1) cut targets BaseTrayR
  for j from 0 to 1 {
    cylinder PiStand{i}{j} radius 3 mm height 3.5 mm base (px0 + 23.5 mm + i*58 mm, pi_y0 + 3.5 mm + j*49 mm, wall - 0.5 mm) axis (0,0,1) join targets BaseTrayR
    cylinder PiPilot{i}{j} radius 1.05 mm height 4 mm base (px0 + 23.5 mm + i*58 mm, pi_y0 + 3.5 mm + j*49 mm, wall + 0.5 mm) axis (0,0,1) cut targets BaseTrayR
  }
}
box SeamWall size 8 mm (D - 2*wall - 1 mm) (kb_z - wall + 0.3 mm) corner (-4 mm, wall + 0.5 mm, wall - 0.5 mm) join targets BaseTrayR
split TraySplit target BaseTrayR origin (0, 0, 0) normal (1, 0, 0) -> body BaseTrayL

// The seam walls (built across x = 0 before the split) are bolted with three M3s
for k from 0 to 2 {
  cylinder SeamBoltL{k} radius 1.7 mm height 6 mm base (-5 mm, 30 mm + k*(D - 60 mm)/2, wall + 4 mm) axis (1,0,0) cut targets BaseTrayL
  cylinder SeamBoltR{k} radius 1.7 mm height 6 mm base (-1 mm, 30 mm + k*(D - 60 mm)/2, wall + 4 mm) axis (1,0,0) cut targets BaseTrayR
}

// Ports: Pi USB / Ethernet through the left wall, UPS USB-C through the right
box PiPorts size (wall + 2 mm) 52 mm 16.5 mm corner (-W/2 - 1 mm, pi_y0 + 2 mm, wall + 4.5 mm) cut targets BaseTrayL
box UpsPort size (wall + 2 mm) 9.5 mm 4 mm corner (W/2 - wall - 1 mm, pi_y0 + cell_d + 4 mm + ups_d/2 - 4.75 mm, wall + 6 mm) cut targets BaseTrayR
// Vents under the Pi, and the HDMI cable slot between the hinges
sketch VentSk on XY offset (-0.5 mm) {
  slot v = centers (px0 + 12 mm, pi_y0 + 10 mm) (px0 + 70 mm, pi_y0 + 10 mm) width 2.4 mm
  fix v
  pattern rectangular v count 6 spacing 7 mm dir (0,1)
}
extrude Vents from VentSk distance (wall + 1 mm) cut targets BaseTrayL
box CableSlotL size 12.1 mm (wall + 2 mm) 7 mm corner (-12 mm, D - wall - 1 mm, tray_h - 7 mm) cut targets BaseTrayL
box CableSlotR size 12 mm (wall + 2 mm) 7 mm corner (0, D - wall - 1 mm, tray_h - 7 mm) cut targets BaseTrayR
// Rubber feet
for j from 0 to 1 {
  cylinder FootL{j} radius 5.2 mm height 1.5 mm base (-(W/2 - 22 mm), 22 mm + j*(D - 44 mm), -0.5 mm) axis (0,0,1) cut targets BaseTrayL
  cylinder FootR{j} radius 5.2 mm height 1.5 mm base (W/2 - 22 mm, 22 mm + j*(D - 44 mm), -0.5 mm) axis (0,0,1) cut targets BaseTrayR
}

// Base hinge knuckles: two per hinge, on webs standing on the bed
for k from 0 to 1 {
  box BaseWebL{k} size 14 mm (hy - D + 1.5 mm) (tray_h - 0.11 mm) corner ((-hinge_x) - 24.7 mm + k*35.2 mm, D - 1.5 mm, 0.01 mm) join targets BaseTrayL
  cylinder BaseKnuckleL{k} radius knuckle_r height 14.6 mm base ((-hinge_x) - 25 mm + k*35.4 mm, hy, zc) axis (1,0,0) join targets BaseTrayL
}
cylinder BaseBoltL radius (bolt_d/2) height 52 mm base ((-hinge_x) - 26 mm, hy, zc) axis (1,0,0) cut targets BaseTrayL
cylinder BaseNutL radius 4.1 mm height 3.5 mm base ((-hinge_x) + 25.1 mm - 3.5 mm, hy, zc) axis (1,0,0) cut targets BaseTrayL
for k from 0 to 1 {
  box BaseWebR{k} size 14 mm (hy - D + 1.5 mm) (tray_h - 0.11 mm) corner ((hinge_x) - 24.7 mm + k*35.2 mm, D - 1.5 mm, 0.01 mm) join targets BaseTrayR
  cylinder BaseKnuckleR{k} radius knuckle_r height 14.6 mm base ((hinge_x) - 25 mm + k*35.4 mm, hy, zc) axis (1,0,0) join targets BaseTrayR
}
cylinder BaseBoltR radius (bolt_d/2) height 52 mm base ((hinge_x) - 26 mm, hy, zc) axis (1,0,0) cut targets BaseTrayR
cylinder BaseNutR radius 4.1 mm height 3.5 mm base ((hinge_x) + 25.1 mm - 3.5 mm, hy, zc) axis (1,0,0) cut targets BaseTrayR

// ==== Deck, split at x = deck_split: keyboard well, touchpad window, screw holes
sketch DeckSk on XY offset tray_h {
  rect rounded o = (-W/2, 0) (W/2, D) radius corner_r
  fix o
}
extrude DeckSlab from DeckSk distance deck_t new -> body DeckR
fillet DeckTop edges (0, 0, H) radius 1.2 mm body DeckR
sketch DeckHolesSk on XY offset tray_h {
  rect rounded kb = (-kb_w/2 - 0.6 mm, kb_y0 - 0.6 mm) (kb_w/2 + 0.6 mm, kb_y0 + kb_d + 0.6 mm) radius 6 mm
  rect rounded tp = (-tp_view_w/2, tp_y0 + (tp_d - tp_view_d)/2) (tp_view_w/2, tp_y0 + (tp_d + tp_view_d)/2) radius 3 mm
  circle a = (-bx, 7 mm) radius 1.7 mm
  circle b = (bx, 7 mm) radius 1.7 mm
  circle c = (-bx, D/2) radius 1.7 mm
  circle d = (bx, D/2) radius 1.7 mm
  circle e = (-bx, D - 7 mm) radius 1.7 mm
  circle f = (bx, D - 7 mm) radius 1.7 mm
  circle g = (-tp_w/2 - 10 mm, 7 mm) radius 1.7 mm
  circle h = (tp_w/2 + 10 mm, 7 mm) radius 1.7 mm
  circle m = (-tp_w/2 - 10 mm, kb_y0 - 3.5 mm) radius 1.7 mm
  circle n = (tp_w/2 + 10 mm, kb_y0 - 3.5 mm) radius 1.7 mm
  fix kb tp a b c d e f g h m n
}
extrude DeckHoles from DeckHolesSk distance (deck_t + 1 mm) offset (-0.5 mm) cut targets DeckR
split DeckSplit target DeckR origin (deck_split, 0, 0) normal (1, 0, 0) -> body DeckL

// ==== Lid bezel, split at x = bezel_split: panel window and eight screw posts
sketch BezelSk on XY offset lid_z0 {
  rect rounded o = (-W/2, 0) (W/2, D) radius corner_r
  fix o
}
extrude BezelSlab from BezelSk distance bezel_t new -> body BezelR
fillet BezelEdge edges (0, 0, lid_z0) radius 1.2 mm body BezelR
sketch ViewSk on XY offset lid_z0 {
  rect rounded v = (-view_w/2, panel_y0 + (panel_d - view_d)/2) (view_w/2, panel_y0 + (panel_d + view_d)/2) radius 2 mm
  fix v
}
extrude PanelView from ViewSk distance (bezel_t + 1 mm) offset (-0.5 mm) cut targets BezelR
sketch PanelFrameSk on XY offset (lid_tray_z - 0.2 mm) {
  rect outer = (-panel_w/2 - 2.5 mm, panel_y0 - 2.5 mm) (panel_w/2 + 2.5 mm, panel_y0 + panel_d + 2.5 mm)
  rect inner = (-panel_w/2 - 0.4 mm, panel_y0 - 0.4 mm) (panel_w/2 + 0.4 mm, panel_y0 + panel_d + 0.4 mm)
  fix outer inner
}
extrude PanelFrame from PanelFrameSk distance (panel_t + 0.2 mm) profiles (panel_w/2 + 1.4 mm, panel_y0 + panel_d/2) join targets BezelR
for i from 0 to 3 {
  for j from 0 to 1 {
    cylinder BezelPost{i}{j} radius 3 mm height (lid_h - wall - 2 mm + 0.6 mm) base ((i - 1.5)*(W - 20 mm)/3, 7 mm + j*(D - 14 mm), lid_tray_z - 0.6 mm) axis (0,0,1) join targets BezelR
    cylinder BezelPilot{i}{j} radius 1.25 mm height (lid_h - wall - 2 mm) base ((i - 1.5)*(W - 20 mm)/3, 7 mm + j*(D - 14 mm), lid_tray_z + 0.5 mm) axis (0,0,1) cut targets BezelR
  }
}
split BezelSplit target BezelR origin (bezel_split, 0, 0) normal (1, 0, 0) -> body BezelL

// ==== Lid back shell, split at x = lid_split: bosses, panel pads, driver standoffs, cable slot, knuckle
sketch LidSk on XY offset lid_tray_z {
  rect rounded o = (-W/2, 0) (W/2, D) radius corner_r
  fix o
}
extrude LidBlock from LidSk distance lid_h new -> body LidBackR
shell LidCavity faces (0, D/2, lid_tray_z) thickness wall inside body LidBackR
fillet LidTopEdge edges (0, 0, lid_top) radius 2 mm body LidBackR
for i from 0 to 3 {
  for j from 0 to 1 {
    cylinder LidBoss{i}{j} radius 3.5 mm height 2.1 mm base ((i - 1.5)*(W - 20 mm)/3, 7 mm + j*(D - 14 mm), lid_top - wall - 2 mm) axis (0,0,1) join targets LidBackR
    cylinder LidBossHole{i}{j} radius 1.7 mm height (wall + 4 mm) base ((i - 1.5)*(W - 20 mm)/3, 7 mm + j*(D - 14 mm), lid_top - wall - 3 mm) axis (0,0,1) cut targets LidBackR
    cylinder LidCounterbore{i}{j} radius 3 mm height 2.1 mm base ((i - 1.5)*(W - 20 mm)/3, 7 mm + j*(D - 14 mm), lid_top - 1.6 mm) axis (0,0,1) cut targets LidBackR
  }
}
for i from 0 to 3 {
  box PanelPad{i} size 20 mm 8 mm (lid_h - wall - panel_t - 0.2 mm + 0.5 mm) corner ((i - 1.5)*60 mm - 10 mm, panel_y0 + panel_d/2 - 4 mm, lid_tray_z + panel_t + 0.2 mm) join targets LidBackR
}
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder DriverStand{i}{j} radius 2.6 mm height 3.5 mm base (-26 mm + i*52 mm, panel_y0 + panel_d + 14 mm + j*37 mm, lid_top - wall - 3 mm) axis (0,0,1) join targets LidBackR
    cylinder DriverPilot{i}{j} radius 1.05 mm height 3 mm base (-26 mm + i*52 mm, panel_y0 + panel_d + 14 mm + j*37 mm, lid_top - wall - 3.2 mm) axis (0,0,1) cut targets LidBackR
  }
}
box LidCableSlot size 24 mm (wall + 2 mm) 6 mm corner (-12 mm, D - wall - 1 mm, lid_tray_z) cut targets LidBackR
split LidSplit target LidBackR origin (lid_split, 0, 0) normal (1, 0, 0) -> body LidBackL
box LidWebL size 19.4 mm (hy - D + 1.5 mm) (lid_top - lid_tray_z - 0.11 mm) corner ((-hinge_x) - 9.7 mm, D - 1.5 mm, lid_tray_z + 0.1 mm) join targets LidBackL
cylinder LidKnuckleL radius knuckle_r height 20 mm base ((-hinge_x) - 10 mm, hy, zc) axis (1,0,0) join targets LidBackL
cylinder LidBoltL radius (bolt_d/2) height 22 mm base ((-hinge_x) - 11 mm, hy, zc) axis (1,0,0) cut targets LidBackL
box LidWebR size 19.4 mm (hy - D + 1.5 mm) (lid_top - lid_tray_z - 0.11 mm) corner ((hinge_x) - 9.7 mm, D - 1.5 mm, lid_tray_z + 0.1 mm) join targets LidBackR
cylinder LidKnuckleR radius knuckle_r height 20 mm base ((hinge_x) - 10 mm, hy, zc) axis (1,0,0) join targets LidBackR
cylinder LidBoltR radius (bolt_d/2) height 22 mm base ((hinge_x) - 11 mm, hy, zc) axis (1,0,0) cut targets LidBackR

// ==== Stand-ins for the bought parts (not printed)
box RefKeyboard size kb_w kb_d kb_t corner (-kb_w/2, kb_y0, kb_z) new -> body RefKeyboard
box RefTouchpad size tp_w tp_d tp_t corner (-tp_w/2, tp_y0, tray_h - tp_t) new -> body RefTouchpad
box RefPi size 85 mm 56 mm 17 mm corner (px0, pi_y0, wall + 3 mm) new -> body RefPi
box RefCells size cell_w cell_d 21 mm corner (W/2 - wall - 3 mm - cell_w, pi_y0, wall) new -> body RefCells
box RefUps size ups_w ups_d 6 mm corner (W/2 - wall - 3 mm - ups_w, pi_y0 + cell_d + 4 mm, wall + 1 mm) new -> body RefUps
box RefPanel size panel_w panel_d panel_t corner (-panel_w/2, panel_y0, lid_tray_z) new -> body RefPanel
box RefDriver size 60 mm 45 mm 8 mm corner (-30 mm, panel_y0 + panel_d + 10 mm, lid_top - wall - 0.5 mm - 8 mm - 1 mm) new -> body RefDriver

// ==== Open the lid about the hinge axis
move HingeIn bodies BezelR, BezelL, LidBackR, LidBackL, RefPanel, RefDriver to (0, -hy, -zc)
move Open bodies BezelR, BezelL, LidBackR, LidBackL, RefPanel, RefDriver axis (-1,0,0) angle lid_angle
move HingeOut bodies BezelR, BezelL, LidBackR, LidBackL, RefPanel, RefDriver to (0, hy, zc)

color BaseTrayR, BaseTrayL, DeckR, DeckL, BezelR, BezelL, LidBackR, LidBackL "#c3c7cc"
color RefKeyboard, RefTouchpad "#2a2d33"
color RefPi, RefUps, RefDriver "#1f6f43"
color RefCells "#3b6bd6"
color RefPanel "#101418"
material BaseTrayR, BaseTrayL, DeckR, DeckL, BezelR, BezelL, LidBackR, LidBackL PLA
