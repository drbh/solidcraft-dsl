// 89. Lid organiser for food-storage container lids: a tray of upright dividers, lids filed on edge.
//
// X runs along the row of bays, Y across the tray (the way a lid faces), Z up from the bed; the
// tray's corner is at the origin. A floor with a low wall all round carries `slots + 1` upright
// dividers, `gap` apart, so each bay holds one or a few lids standing on edge like files. Each
// divider has rounded top corners and a scoop in the middle of its top edge for fingers. The
// dividers stand half into the wall at the ends and sides so nothing joins on a shared face.
// A grid of drain holes in every bay lets a washed lid drip dry.
//
// The defaults give five 30 mm bays in a 170 x 150 mm tray with 110 mm dividers; lids wider
// than the tray stand on edge and lean out over the scoops. Set slots, gap and div_h to suit
// your lids; the dividers are a feature pattern, so slots changes when rebuilt.
//
// Print open side up, no supports: the dividers are thin walls straight up from the floor, the
// scoops open upward, and the drain holes go straight through the floor.
design LidHolder units mm

param slots = 5               // bays
param gap = 30 mm             // clear width of a bay
param depth = 150 mm          // across the tray (Y)
param div_t = 3 mm
param div_h = 110 mm          // divider top, from the bed
param scoop = 35 mm           // how far the scoop dips below the top
param top_r = 22 mm           // divider top corners
param floor_t = 3 mm
param wall = 2.4 mm
param rim_h = 18 mm           // wall height above the floor
param corner_r = 3 mm         // small enough that the end dividers stay inside the corners
param drain_d = 6 mm
param drain_rows = 4

let pitch = gap + div_t
let L = slots*pitch + div_t + wall
let x0 = wall/2                       // first divider's -X face
let bay_x = x0 + div_t + gap/2        // centre of the first bay
let drain_y0 = 25 mm
let drain_dy = (depth - 2*drain_y0)/(drain_rows - 1)

// ==== Floor and the low wall round it
sketch FloorSk on XY {
  rect rounded o = (0, 0) (L, depth) radius corner_r
  fix o
}
extrude Floor from FloorSk distance floor_t new -> body Holder
sketch WallSk on XY {
  rect rounded outside = (0, 0) (L, depth) radius corner_r
  rect rounded inside = (wall, wall) (L - wall, depth - wall) radius (corner_r - wall)
  fix outside inside
}
extrude Wall from WallSk distance (floor_t + rim_h) profiles (wall/2, depth/2) join targets Holder

// ==== Dividers: one drawn on the YZ plane at the first position, then patterned along X.
// Outline: up the -Y edge, a rounded corner, along the top to the scoop, the scoop, on to the
// +Y corner and down; it starts inside the floor and the side walls.
sketch DividerSk on YZ offset x0 {
  line bottom = (wall/2, floor_t/2) (depth - wall/2, floor_t/2)
  line right = bottom.end (depth - wall/2, div_h)
  line top_r = right.end (depth/2 + scoop, div_h)
  arc dip = start top_r.end through (depth/2, div_h - scoop) end (depth/2 - scoop, div_h)
  line top_l = dip.end (wall/2, div_h)
  line left = top_l.end bottom.start
  fillet right.end radius top_r
  fillet top_l.end radius top_r
  fillet top_r.end radius 6 mm
  fillet dip.end radius 6 mm
  fix bottom right top_r dip top_l left
}
extrude Divider from DividerSk distance div_t join targets Holder
pattern Dividers features Divider dir (1,0,0) count (slots + 1) spacing pitch

// ==== Drain holes, a column in each bay, from one sketch pattern
sketch DrainSk on XY {
  circle h = (bay_x, drain_y0) diameter drain_d
  fix h
  pattern rectangular h count slots spacing pitch count2 drain_rows spacing2 drain_dy
}
extrude Drains from DrainSk distance (floor_t + 2 mm) offset (-1 mm) cut targets Holder

color Holder "#cfe3e8"
material Holder PLA
