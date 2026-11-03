// 124. Board enclosure generator: a two-part snap-fit case from a board's size, holes and ports.
//
// Give it a board's outline (board_w x board_d x board_t), its four mounting holes (two x and two
// y positions, which covers most boards) and its port openings, and it makes a tray with
// standoffs and port cutouts, and a vented lid that snaps on. The defaults are a Raspberry Pi 4
// Model B: 85 x 56 mm, holes 3.5 mm in on a 58 x 49 mm rectangle, USB-C, two micro-HDMI and the
// audio jack on the near long edge, Ethernet and two USB stacks on the right short edge, the
// microSD slot under the left short edge, and the 40-pin header along the far edge (a slot in the
// lid gives it a ribbon cable).
//
// X along the board's length, Y across it, Z up; the board's corner is at the origin and the
// tray's floor underside is wall + standoff below the board. The board sits on four
// standoffs with M2.5 pilot holes (2.2 mm) so it screws down; every gap round the board is `gap`.
// The lid is a plate with a lip that drops inside the walls; four bumps on the lip catch in four
// grooves in the walls, so it snaps on and pries off at the notch. Vents are slots from one sketch
// pattern, over the CPU end of the board.
//
// Print: tray open side up, lid upside down (plate on the bed, lip up); no supports (the port
// openings bridge). PLA or PETG, 0.2 mm layers. BOM: the board, 4 x M2.5x6 screws.
design BoardEnclosure units mm

// The board
param board_w = 85 mm
param board_d = 56 mm
param board_t = 1.4 mm
param hole_x0 = 3.5 mm
param hole_x1 = 61.5 mm
param hole_y0 = 3.5 mm
param hole_y1 = 52.5 mm
// The case
param wall = 2 mm
param gap = 1 mm
param standoff = 4 mm
param inner_h = 28 mm          // floor top to lid underside: the tallest port (USB stack) plus room
param lid_t = 2.5 mm
param lip_h = 4 mm
param snap = 0.5 mm            // how far each bump stands proud of the lip
param gpio_slot = 1

let x0 = -gap
let x1 = board_w + gap
let y0 = -gap
let y1 = board_d + gap
let zf = -(wall + standoff)    // floor underside
let zb = 0 mm                  // board underside
let zt = board_t               // board top
let zl = zf + wall + inner_h   // lid underside, the top of the walls
let snap_z = zl - 2 mm

// ==== Tray
sketch TraySk on XY offset zf {
  rect rounded o = (x0 - wall, y0 - wall) (x1 + wall, y1 + wall) radius 3 mm
  fix o
}
extrude TrayBlock from TraySk distance (wall + inner_h) new -> body Tray
shell TrayCavity faces ((x0 + x1)/2, (y0 + y1)/2, zl) thickness wall inside body Tray
sketch StandoffSk on XY offset (zf + wall - 0.3 mm) {
  circle a = (hole_x0, hole_y0) diameter 6 mm
  circle b = (hole_x1, hole_y0) diameter 6 mm
  circle c = (hole_x0, hole_y1) diameter 6 mm
  circle d = (hole_x1, hole_y1) diameter 6 mm
  fix a b c d
}
extrude Standoffs from StandoffSk distance (standoff + 0.3 mm) join targets Tray
sketch PilotSk on XY offset (zb - 5 mm) {
  circle a = (hole_x0, hole_y0) diameter 2.2 mm
  circle b = (hole_x1, hole_y0) diameter 2.2 mm
  circle c = (hole_x0, hole_y1) diameter 2.2 mm
  circle d = (hole_x1, hole_y1) diameter 2.2 mm
  fix a b c d
}
extrude Pilots from PilotSk distance 5.5 mm cut targets Tray

// Ports on the near long edge (y = 0): USB-C, micro-HDMI x 2, audio
box PortUsbC size 10 mm (wall + 2 mm) 4.6 mm corner (11.2 mm - 5 mm, y0 - wall - 1 mm, zt - 0.6 mm) cut targets Tray
box PortHdmi0 size 8 mm (wall + 2 mm) 4.6 mm corner (26 mm - 4 mm, y0 - wall - 1 mm, zt - 0.6 mm) cut targets Tray
box PortHdmi1 size 8 mm (wall + 2 mm) 4.6 mm corner (39.5 mm - 4 mm, y0 - wall - 1 mm, zt - 0.6 mm) cut targets Tray
cylinder PortAudio diameter 7 mm height (wall + 2 mm) base (53.5 mm, y0 - wall - 1 mm, zt + 3 mm) axis Y cut targets Tray
// Ports on the right short edge (x = board_w): Ethernet and two USB stacks
box PortEth size (wall + 2 mm) 17 mm 14.5 mm corner (x1 - 1 mm, 10.25 mm - 8.5 mm, zt - 0.4 mm) cut targets Tray
box PortUsb0 size (wall + 2 mm) 15.5 mm 16.6 mm corner (x1 - 1 mm, 29 mm - 7.75 mm, zt - 0.4 mm) cut targets Tray
box PortUsb1 size (wall + 2 mm) 15.5 mm 16.6 mm corner (x1 - 1 mm, 47 mm - 7.75 mm, zt - 0.4 mm) cut targets Tray
// microSD under the left short edge
box PortSd size (wall + 2 mm) 14 mm 3.4 mm corner (x0 - wall - 1 mm, 28 mm - 7 mm, zb - 3 mm) cut targets Tray
// Snap grooves, one in each wall at mid-length
box GrooveN size 12 mm 0.8 mm 1.4 mm corner ((x0 + x1)/2 - 6 mm, y0 - 0.8 mm, snap_z - 0.7 mm) cut targets Tray
box GrooveS size 12 mm 0.8 mm 1.4 mm corner ((x0 + x1)/2 - 6 mm, y1, snap_z - 0.7 mm) cut targets Tray
box GrooveW size 0.8 mm 12 mm 1.4 mm corner (x0 - 0.8 mm, (y0 + y1)/2 - 6 mm, snap_z - 0.7 mm) cut targets Tray
box GrooveE size 0.8 mm 12 mm 1.4 mm corner (x1, (y0 + y1)/2 - 6 mm, snap_z - 0.7 mm) cut targets Tray
// Pry notch at the top of the near wall
box PryNotch size 10 mm (wall + 2 mm) 1.5 mm corner ((x0 + x1)/2 - 5 mm + 20 mm, y0 - wall - 1 mm, zl - 1.5 mm) cut targets Tray

// ==== Lid: plate on the walls, lip inside them with four snap bumps, vents and a header slot
sketch LidSk on XY offset zl {
  rect rounded o = (x0 - wall, y0 - wall) (x1 + wall, y1 + wall) radius 3 mm
  fix o
}
extrude LidPlate from LidSk distance lid_t new -> body Lid
sketch LipSk on XY offset (zl - lip_h) {
  rect rounded o = (x0 + 0.25 mm, y0 + 0.25 mm) (x1 - 0.25 mm, y1 - 0.25 mm) radius 0.75 mm
  rect i = (x0 + 1.85 mm, y0 + 1.85 mm) (x1 - 1.85 mm, y1 - 1.85 mm)
  fix o i
}
extrude Lip from LipSk distance (lip_h + 0.3 mm) join targets Lid
box BumpN size 10 mm (snap + 0.3 mm) 1 mm corner ((x0 + x1)/2 - 5 mm, y0 + 0.25 mm - snap, snap_z - 0.5 mm) join targets Lid
box BumpS size 10 mm (snap + 0.3 mm) 1 mm corner ((x0 + x1)/2 - 5 mm, y1 - 0.25 mm - 0.3 mm, snap_z - 0.5 mm) join targets Lid
box BumpW size (snap + 0.3 mm) 10 mm 1 mm corner (x0 + 0.25 mm - snap, (y0 + y1)/2 - 5 mm, snap_z - 0.5 mm) join targets Lid
box BumpE size (snap + 0.3 mm) 10 mm 1 mm corner (x1 - 0.25 mm - 0.3 mm, (y0 + y1)/2 - 5 mm, snap_z - 0.5 mm) join targets Lid
sketch VentSk on XY offset (zl - 1 mm) {
  slot v = centers (8 mm, 14 mm) (8 mm, 34 mm) width 2.5 mm
  fix v
  pattern rectangular v count 8 spacing 5 mm
}
extrude Vents from VentSk distance (lid_t + 2 mm) cut targets Lid
if gpio_slot > 0 {
  box HeaderSlot size 53 mm 7 mm (lid_t + 2 mm) corner (6 mm, 49 mm, zl - 1 mm) cut targets Lid
}

// ==== The board, for fit only (not printed)
box RefBoard size board_w board_d board_t corner (0, 0, zb) new -> body RefBoard

color Tray "#2f6f62"
color Lid "#3e8a7a"
color RefBoard "#1f7a3a"
material Tray, Lid PLA
