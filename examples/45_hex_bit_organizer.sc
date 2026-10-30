// 45. Stepped tray for 1/4 in hex driver bits, with a trough for the bit driver.
//
// Z is up, X runs along each row of bits, Y runs from the front edge (y = 0) to the back. A
// trough across the front holds a bit driver lying along X. Behind it the tray climbs in steps,
// one row of hexagonal sockets per step, so each row stands higher than the one in front and
// every bit can be pinched from the front without knocking its neighbours. The sockets are
// sketch hexagons sized across the flats (6.35 mm + clearance), with a 45 deg chamfered lead-in
// cut by a tapered extrude, and are patterned in 2D: along X, and along the stair diagonal
// (row pitch in Y, step height in Z). The steps themselves are one column block patterned along
// the same diagonal, then everything below the bed is cut away.
//
// "1/4 in HEX" is debossed into the front face.
//
// Print flat on its bottom as modelled, no supports: the sockets and the lead-in chamfers open
// upward, the driver trough is a concave half-round open at the top, and the steps rise
// straight from the bed. The label is a shallow recess in a vertical wall.
design HexBitOrganizer units mm
// Bits (1/4 in hex shank)
param bit_af = 6.35 mm
param bit_clear = 0.25 mm
param sock_depth = 10 mm
param lead = 0.8 mm
// Grid and steps
param cols = 8
param rows = 4
param pitch_x = 11 mm
param pitch_y = 11 mm
param step = 5 mm
param margin = 3 mm
param base_h = 14 mm
// Driver trough
param drv_w = 28 mm
param drv_r = 13 mm
param drv_depth = 6 mm
let sock_af = bit_af + bit_clear
let width = cols*pitch_x + 2*margin
let depth = drv_w + rows*pitch_y + margin
let col_h = base_h + rows*step + 1 mm
let diag = sqrt(pitch_y^2 + step^2)
let row0_top = base_h + step
let x0 = margin + pitch_x/2
let y0 = drv_w + pitch_y/2

// Base under the driver trough and the whole tray
box Base size width depth base_h new -> body Tray
// Step columns: the first row's column (with the back margin folded into the last one by
// making each column reach margin past its row), patterned up the stair diagonal. Each column
// overlaps the next by margin so the union has no coincident faces.
sketch StepSk on YZ {
  rect c = (drv_w,row0_top - col_h) (drv_w + pitch_y + margin,row0_top)
  fix c.bottom c.right c.top c.left
}
extrude Step from StepSk distance width join targets Tray
pattern Steps features Step dir (0,pitch_y/step,1) count rows spacing diag
// Trim the columns' lower ends, which hang below the bed for the front rows
box Bed size (width + 2 mm) (depth + 20 mm) col_h corner (-1 mm,-1 mm,-col_h) cut targets Tray
box BackTrim size (width + 2 mm) (pitch_y + 2*step) (col_h + rows*step) corner (-1 mm,depth,-1 mm) cut targets Tray

// Hex sockets with a chamfered lead-in, first row first, patterned along X and up the stairs
sketch SockSk on XY offset row0_top {
  polygon h = (x0,y0) radius (sock_af/2) sides 6 circumscribed
  fix h.1 h.2 h.3 h.4 h.5 h.6
}
extrude Socket from SockSk distance sock_depth negative cut targets Tray
sketch LeadSk on XY offset row0_top {
  polygon h = (x0,y0) radius (sock_af/2 + lead) sides 6 circumscribed
  fix h.1 h.2 h.3 h.4 h.5 h.6
}
extrude LeadIn from LeadSk distance lead negative taper (-45 deg) cut targets Tray
pattern Sockets features Socket, LeadIn dir (1,0,0) count cols spacing pitch_x dir2 (0,pitch_y/step,1) count2 rows spacing2 diag

// Driver trough: a half-round channel across the front, open at both ends
cylinder Trough radius drv_r height (width + 2 mm) base (-1 mm,drv_w/2,base_h + drv_r - drv_depth) axis X cut targets Tray

// Label debossed into the front face (on a face sketch u = X, v = Z; `at` is written with params
// because `let` helpers are not expanded inside text coordinates)
param label_h = 5 mm
sketch LabelSk on face (width/2,0,base_h/2) {
  text "1/4 in HEX" at (cols*pitch_x/2 + margin,3 mm) height label_h align center
}
emboss Label from LabelSk depth 0.6 mm deboss targets Tray

// Prints without supports, in the print recipe's orientation.
check Tray overhang <= 45 deg
