// 119. Secret puzzle box: a sliding lid held shut by a hidden gravity pin that only a sliding base
// lets go.
//
// X runs along the box, Y across it, Z up; the box stands on z = 0, centred in plan. The lid slides
// in grooves in the long walls and comes off past the +X end. Inside the thick -X end wall a loose
// pin stands in a bore: its foot rests on the base panel and its top rises into a notch in the
// underside of the lid, so the lid can't move. The base panel is a drawer in a slot under the box.
//
// Solving it:
//   1. Hold the box upright and slide the base panel out of the +Y side by `travel` (10 mm) until
//      the pin drops into a pocket in the panel with a click. The pocket now holds the panel too.
//   2. Slide the lid off toward +X.
// Locking it again: close the lid, turn the box upside down so the pin falls back up into the
// lid's notch, push the panel home, and turn the box upright.
//
// Parts (all printed, no hardware): Box, Lid, Panel, Pin. Fits: `clr` (0.25 mm) on every sliding
// face, 0.3 mm radial on the pin.
//
// Printing: Box upright as modelled, Lid and Panel flat (the panel's pocket up), Pin standing on
// its end. No supports: the panel slot's ceiling in the end walls is a 4 mm overhang, the lid
// grooves 2 mm, and the +Y wall bridges 58 mm over the slot's mouth (fine with good cooling).
design PuzzleBox units mm

param L = 80 mm
param W = 60 mm
param H = 40 mm
param end_t = 14 mm          // end walls; the -X one hides the pin
param side_t = 4 mm
param floor_t = 1.75 mm
param panel_t = 5 mm
param slot_depth = 4 mm      // how far the panel's slot runs into each end wall
param lid_t = 2.5 mm
param groove = 2 mm          // lid groove depth into the long walls
param lip = 2 mm             // stop for the lid at the -X end
param clr = 0.25 mm
param pin_r = 2.5 mm
param pin_clr = 0.3 mm
param travel = 10 mm
param drop = 4 mm            // pocket depth: how far the pin falls when unlocked
param notch = 1.5 mm         // how far the pin rises into the lid when locked

let in_x = L/2 - end_t
let in_y = W/2 - side_t
let slot_x = in_x + slot_depth
let panel_z = floor_t + clr
let panel_top = panel_z + panel_t
let lid_z = H - 6 mm + clr
let pin_x = -(in_x + 3.5 mm)       // inside the wall, its bore clear of the inner face
let pin_l = lid_z + notch - panel_top

// ==== Box
box Outer size L W H corner (-L/2, -W/2, 0) new -> body Box
// Panel slot: runs slot_depth into both end walls, out of the +Y side, stops at the -Y wall
box SlotCut size (2*slot_x) (W/2 + in_y + 1 mm) (panel_t + 2*clr) corner (-slot_x, -in_y, floor_t) cut targets Box
// Cavity above the panel, open at the top
box CavityCut size (2*in_x) (2*in_y) (H - panel_top - clr + 1 mm) corner (-in_x, -in_y, panel_top + clr - 0.1 mm) cut targets Box
// Lid grooves, then the space over the end walls under the lid (the -X lip stays full height)
box GrooveCut size (L - lip + 1 mm) (2*(in_y + groove)) 3 mm corner (-L/2 + lip, -(in_y + groove), H - 6 mm) cut targets Box
box TopCut size (L - lip + 1 mm) (2*in_y) 3.1 mm corner (-L/2 + lip, -in_y, H - 3.1 mm) cut targets Box
// The pin's bore in the -X end wall, down into the panel slot and a little below it
cylinder PinBore radius (pin_r + pin_clr) height (H - 6 mm - panel_top + drop + 0.9 mm) base (pin_x, 0, panel_top - drop - 0.5 mm) axis (0,0,1) cut targets Box

// ==== Lid: slides in the grooves, with a notch for the pin and grip ribs at its +X end
box LidPlate size (L - lip - clr) (2*(in_y + groove - clr)) lid_t corner (-L/2 + lip + clr, -(in_y + groove - clr), lid_z) new -> body Lid
cylinder LidNotch radius (pin_r + pin_clr) height (notch + clr + 0.1 mm) base (pin_x, 0, lid_z - 0.1 mm) axis (0,0,1) cut targets Lid
for k from 0 to 2 {
  box Grip{k} size 1.2 mm (2*in_y - 10 mm) 0.8 mm corner (L/2 - 12 mm + 4*k mm, -(in_y - 5 mm), lid_z + lid_t - 0.1 mm) join targets Lid
}

// ==== Panel: the drawer under the box, with the pocket the pin drops into and a finger notch
box PanelPlate size (2*(slot_x - clr)) (W/2 + in_y - 2*clr) panel_t corner (-(slot_x - clr), -in_y + clr, panel_z) new -> body Panel
cylinder PanelPocket radius (pin_r + pin_clr + 0.1 mm) height (drop + 0.1 mm) base (pin_x, -travel, panel_top - drop) axis (0,0,1) cut targets Panel
cylinder PanelGrip radius 6 mm height (panel_t + 2 mm) base (0, W/2 - clr, panel_z - 1 mm) axis (0,0,1) cut targets Panel

// ==== Pin: resting on the panel, its top in the lid's notch
cylinder PinRod radius pin_r height pin_l base (pin_x, 0, panel_top) axis (0,0,1) new -> body Pin

check Box clearance Lid >= (clr - 0.01 mm)
check Box clearance Panel >= (clr - 0.01 mm)
check Pin clearance Box >= (pin_clr - 0.01 mm)
check Pin clearance Lid >= (clr - 0.01 mm)

color Box "#8d6e63"
color Lid "#a1887f"
color Panel "#6d4c41"
color Pin "#ffca28"
material Box, Lid, Panel, Pin PLA
