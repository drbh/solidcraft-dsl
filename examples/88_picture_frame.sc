// 88. Picture frame for a standard 4 x 6 in photo: bevelled face, rabbet, turn buttons, kickstand, keyholes.
//
// The frame lies face down, as it prints: X across the photo's width, Y up its height (portrait),
// Z up from the front face (z = 0) to the back (z = frame_t), centred on the origin. Everything is
// sized from the photo: the window shows all but `overlap` of each edge, and the rabbet behind it
// is the photo plus `fit` a side, deep enough for the glazing (2 mm glass or acrylic, not
// printed), the photo with a mat, and the back panel, which then sits flush with the frame's back.
//
// Face: a 45 deg bevel round the window (the sight edge) and a chamfer round the outside, a small
// groove round the sides and a rounded back edge. Back: four turn buttons on #2 (2.2 mm) screws
// swing over the panel to hold it in; turn them along the rail to take the panel out. Two
// keyholes in the back take a wall screw head (up to 8 mm): one in the top rail for hanging
// portrait, one in the +X rail for landscape (+X up). The kickstand hinges on the panel, on a
// 1.75 mm filament pin through two lugs; its tail is cut back so it stops against the panel
// when the leg has swung out about stop_angle, and the frame stands portrait.
//
// Print: the frame face down (the bevels lean out at 45 deg, the keyhole slots bridge 2 mm), the
// panel lugs up, the leg upside down (tail slope up), the buttons flat. No supports.
design PictureFrame units mm

// The photo
param photo_w = 101.6 mm      // 4 in
param photo_h = 152.4 mm      // 6 in
param overlap = 5 mm          // photo hidden behind the lip, each edge
param fit = 0.5 mm            // rabbet clearance round the photo, each side

// The moulding
param rail = 16 mm            // frame width outside the rabbet
param frame_t = 14 mm
param bevel = 4 mm            // 45 deg sight edge round the window
param edge = 3 mm             // 45 deg chamfer round the front outside
param groove_d = 1 mm
param groove_h = 1.5 mm

// What goes in the rabbet, front to back
param glass_t = 2 mm
param stack_t = 1 mm          // photo plus mat
param panel_t = 3 mm
param panel_gap = 0.3 mm      // panel clearance in the rabbet, each side

// Turn buttons and hanging
param button_l = 20 mm
param button_w = 8 mm
param button_t = 2.4 mm
param screw_clear = 2.4 mm
param screw_pilot = 1.6 mm
param head_d = 8 mm           // keyhole for a wall screw head
param shank_w = 4.2 mm

// Kickstand
param leg_w = 24 mm
param leg_l = 95 mm
param knuckle_r = 3 mm
param lug_w = 6 mm
param pin_d = 2.1 mm          // 1.75 mm filament pin
param stop_angle = 35 deg
param tail_l = 6 mm

let rab_w = photo_w + 2*fit
let rab_h = photo_h + 2*fit
let win_w = photo_w - 2*overlap
let win_h = photo_h - 2*overlap
let out_w = rab_w + 2*rail
let out_h = rab_h + 2*rail
let rabbet_d = glass_t + stack_t + panel_t
let pane_w = rab_w - 2*panel_gap
let pane_h = rab_h - 2*panel_gap
let pivot_x = rab_w/2 + 5 mm   // button pivots, on the side rails
let pivot_y = rab_h/4
let hinge_y = rab_h/2 - 25 mm
let hinge_z = frame_t + knuckle_r
let top_y = rab_h/2 + rail/2   // keyholes on the rails' centre lines
let side_x = rab_w/2 + rail/2

// ==== Frame
box Blank size out_w out_h frame_t corner (-out_w/2, -out_h/2, 0) new -> body Frame
box Window size win_w win_h (frame_t + 2 mm) corner (-win_w/2, -win_h/2, -1 mm) cut targets Frame
chamfer SightEdge edges (0, -win_h/2, 0) (0, win_h/2, 0) (-win_w/2, 0, 0) (win_w/2, 0, 0) distance bevel body Frame
chamfer FrontEdge edges (0, -out_h/2, 0) (0, out_h/2, 0) (-out_w/2, 0, 0) (out_w/2, 0, 0) distance edge body Frame
fillet BackEdge edges (0, -out_h/2, frame_t) (0, out_h/2, frame_t) (-out_w/2, 0, frame_t) (out_w/2, 0, frame_t) radius 1.5 mm body Frame
box Rabbet size rab_w rab_h (rabbet_d + 1 mm) corner (-rab_w/2, -rab_h/2, frame_t - rabbet_d) cut targets Frame

// A groove round the sides at half depth: the band between the outline and an inset of it
sketch GrooveSk on XY offset (frame_t/2 - groove_h/2) {
  rect outside = (-out_w/2 - 5 mm, -out_h/2 - 5 mm) (out_w/2 + 5 mm, out_h/2 + 5 mm)
  rect inside = (-out_w/2 + groove_d, -out_h/2 + groove_d) (out_w/2 - groove_d, out_h/2 - groove_d)
  fix outside inside
}
extrude Groove from GrooveSk distance groove_h profiles (out_w/2 + 2 mm, 0) cut targets Frame

// Pilot holes for the turn buttons' screws
for k from 0 to 3 {
  cylinder Pilot{k} diameter screw_pilot height 9 mm base ((2*mod(k, 2) - 1)*pivot_x, (2*floor(k/2) - 1)*pivot_y, frame_t - 8 mm) axis Z cut targets Frame
}

// Keyholes: the head goes in at the round end and slides along under the slot's lips
sketch TopPocketSk on XY offset (frame_t - 6 mm) {
  slot s = centers (0, top_y - 3 mm) (0, top_y + 3 mm) width head_d
  fix s
}
extrude TopPocket from TopPocketSk distance 3.5 mm cut targets Frame
sketch TopKeySk on XY offset (frame_t - 2.6 mm) {
  circle entry = (0, top_y - 3 mm) diameter (head_d + 0.4 mm)
  slot s = centers (0, top_y - 3 mm) (0, top_y + 3 mm) width shank_w
  fix entry s
}
extrude TopKey from TopKeySk distance 3.1 mm cut targets Frame
sketch SidePocketSk on XY offset (frame_t - 6 mm) {
  slot s = centers (side_x - 3 mm, 0) (side_x + 3 mm, 0) width head_d
  fix s
}
extrude SidePocket from SidePocketSk distance 3.5 mm cut targets Frame
sketch SideKeySk on XY offset (frame_t - 2.6 mm) {
  circle entry = (side_x - 3 mm, 0) diameter (head_d + 0.4 mm)
  slot s = centers (side_x - 3 mm, 0) (side_x + 3 mm, 0) width shank_w
  fix entry s
}
extrude SideKey from SideKeySk distance 3.1 mm cut targets Frame

// ==== Back panel, flush with the frame's back, with the kickstand's hinge lugs
box PanelPlate size pane_w pane_h panel_t corner (-pane_w/2, -pane_h/2, frame_t - panel_t) new -> body Panel
for k from 0 to 1 {
  box LugBlock{k} size lug_w (2*knuckle_r) (knuckle_r + 0.5 mm) corner ((2*k - 1)*(leg_w/2 + 0.4 mm + lug_w/2) - lug_w/2, hinge_y - knuckle_r, frame_t - 0.5 mm) join targets Panel
  cylinder LugRound{k} radius knuckle_r height lug_w base ((2*k - 1)*(leg_w/2 + 0.4 mm + lug_w/2) - lug_w/2, hinge_y, hinge_z) axis X join targets Panel
}
cylinder LugPin diameter pin_d height (leg_w + 2*lug_w + 4 mm) base (-(leg_w/2 + lug_w + 2 mm), hinge_y, hinge_z) axis X cut targets Panel

// ==== Kickstand, closed flat on the panel: a bar hanging from the pivot, the tail above it
// undercut at stop_angle so it bears on the panel once the leg has swung out that far
sketch LegSk on YZ offset (-leg_w/2) {
  polyline closed o = (hinge_y - leg_l, frame_t) (hinge_y, frame_t) (hinge_y + tail_l, frame_t + tail_l*tan(stop_angle)) (hinge_y + tail_l, frame_t + 2*knuckle_r) (hinge_y - leg_l, frame_t + 2*knuckle_r)
  fix o
}
extrude LegBar from LegSk distance leg_w new -> body Leg
fillet LegFoot edges (0, hinge_y - leg_l, frame_t + 2*knuckle_r) radius 2 mm body Leg
cylinder LegPin diameter pin_d height (leg_w + 2 mm) base (-(leg_w/2 + 1 mm), hinge_y, hinge_z) axis X cut targets Leg

// ==== Turn buttons, swung in over the panel
for k from 0 to 3 {
  sketch Button{k}Sk on XY offset frame_t {
    slot s = centers ((2*mod(k, 2) - 1)*pivot_x, (2*floor(k/2) - 1)*pivot_y) ((2*mod(k, 2) - 1)*(pivot_x - button_l + button_w), (2*floor(k/2) - 1)*pivot_y) width button_w
    fix s
  }
  extrude Button{k}Plate from Button{k}Sk distance button_t new -> body Button{k}
  cylinder Button{k}Hole diameter screw_clear height (button_t + 2 mm) base ((2*mod(k, 2) - 1)*pivot_x, (2*floor(k/2) - 1)*pivot_y, frame_t - 1 mm) axis Z cut targets Button{k}
}

color Frame "#6b4a33"
color Panel, Leg "#d9d4c7"
color Button0, Button1, Button2, Button3 "#3a3a3a"
material Frame, Panel, Leg, Button0, Button1, Button2, Button3 PLA
