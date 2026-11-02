// 99. USB thumb drive: a printed two-part shell around a stick's PCB, a USB-A plug, a slip-on cap
// and a lanyard loop.
//
// X runs along the drive with the plug at +X, Y across, Z up; the shell splits at z = body_t/2.
// The bottom half has a pocket for the PCB and a 0.8 mm lip standing up round the pocket; the
// top half has a matching groove with `fit` clearance, so the halves press together and locate.
// Both halves have a slot in the +X end for the plug's metal shell and a boss round the lanyard
// hole at the -X end. The plug is a standard USB-A shell (12 x 4.5 mm) with a tongue and two
// contact windows; it and the PCB are bought parts, modelled for the look. The cap is shown
// pulled off the plug; it slides over the shell with `fit` all round.
//
// Print the two halves outside face down (the top half flips) and the cap on its open end, all
// without supports. The lanyard ring is a split ring, not printed.
design UsbThumbDrive units mm

param body_l = 45 mm
param body_w = 19 mm
param body_t = 9 mm
param corner_r = 4 mm
param wall = 1.6 mm
param floor_t = 1.2 mm
param lip_h = 1 mm
param lip_t = 0.8 mm
param fit = 0.15 mm

param plug_w = 12 mm
param plug_h = 4.5 mm
param plug_l = 12 mm
param shell_t = 0.4 mm

param cap_l = 16 mm
param cap_wall = 1.6 mm
param cap_gap = 2 mm           // how far the cap stands off the plug's end, as shown

param hole_x = 4 mm
param hole_r = 2.2 mm
param ring_r = 14 mm
param ring_wire = 0.9 mm

let split_z = body_t/2
let plug_z = split_z - plug_h/2
let cap_x = body_l + plug_l + cap_gap
let cap_in_w = plug_w + 2*fit
let cap_in_h = plug_h + 2*fit

// ==== Bottom half: shell, lip, pocket, plug slot, lanyard boss
sketch OutlineSk on XY {
  rect rounded o = (0, -body_w/2) (body_l, body_w/2) radius corner_r
  fix o
}
sketch LipSk on XY offset (split_z - 0.1 mm) {
  rect rounded o = (lip_t, -body_w/2 + lip_t) (body_l - lip_t, body_w/2 - lip_t) radius (corner_r - lip_t)
  fix o
}
sketch PocketSk on XY offset floor_t {
  rect rounded o = (wall, -body_w/2 + wall) (body_l - wall, body_w/2 - wall) radius (corner_r - wall)
  fix o
}
extrude BottomShell from OutlineSk distance split_z new -> body Bottom
extrude BottomLip from LipSk distance (lip_h + 0.1 mm) join targets Bottom
extrude BottomPocket from PocketSk distance (body_t - floor_t) cut targets Bottom
fillet BottomEdge edges (body_l/2, -body_w/2, 0) radius 1.5 mm body Bottom

// ==== Top half: shell, groove for the lip, pocket
sketch TopSk on XY offset split_z {
  rect rounded o = (0, -body_w/2) (body_l, body_w/2) radius corner_r
  fix o
}
sketch GrooveSk on XY offset (split_z - 0.1 mm) {
  rect rounded o = (lip_t - fit, -body_w/2 + lip_t - fit) (body_l - lip_t + fit, body_w/2 - lip_t + fit) radius (corner_r - lip_t + fit)
  fix o
}
sketch TopPocketSk on XY offset (split_z + 0.5 mm) {
  rect rounded o = (wall, -body_w/2 + wall) (body_l - wall, body_w/2 - wall) radius (corner_r - wall)
  fix o
}
extrude TopShell from TopSk distance split_z new -> body Top
extrude TopGroove from GrooveSk distance (lip_h + fit + 0.1 mm) cut targets Top
extrude TopPocket from TopPocketSk distance (split_z - 0.5 mm - floor_t) cut targets Top
fillet TopEdge edges (body_l/2, -body_w/2, body_t) radius 1.5 mm body Top

// Lanyard bosses (stopping short of the split) and the hole through both
cylinder BottomBoss radius (hole_r + 1.4 mm) height (split_z - floor_t + 0.1 mm) base (hole_x, 0, floor_t - 0.2 mm) axis Z join targets Bottom
cylinder TopBoss radius (hole_r + 1.4 mm) height (body_t - floor_t - split_z - lip_h - fit) base (hole_x, 0, split_z + lip_h + fit + 0.2 mm) axis Z join targets Top
cylinder BottomHole radius hole_r height (body_t + 2 mm) base (hole_x, 0, -1 mm) axis Z cut targets Bottom
cylinder TopHole radius hole_r height (body_t + 2 mm) base (hole_x, 0, -1 mm) axis Z cut targets Top

// Plug slot through the +X end of both halves
box BottomSlot size (wall + 2 mm) (plug_w + 2*fit) (plug_h + 2*fit) corner (body_l - wall - 1 mm, -plug_w/2 - fit, plug_z - fit) cut targets Bottom
box TopSlot size (wall + 2 mm) (plug_w + 2*fit) (plug_h + 2*fit) corner (body_l - wall - 1 mm, -plug_w/2 - fit, plug_z - fit) cut targets Top

// ==== Bought parts: PCB, plug shell and tongue
box Board size (body_l - 2*wall - 2 mm) (body_w - 2*wall - 1 mm) 1 mm corner (wall + 1 mm, -(body_w - 2*wall - 1 mm)/2, split_z - 0.5 mm) new -> body Pcb
box PlugOuter size (plug_l + wall + 1 mm) plug_w plug_h corner (body_l - wall - 1 mm, -plug_w/2, plug_z) new -> body Plug
box PlugHollow size (plug_l + 1 mm) (plug_w - 2*shell_t) (plug_h - 2*shell_t) corner (body_l, -plug_w/2 + shell_t, plug_z + shell_t) cut targets Plug
for k from 0 to 1 {
  box Window{k} size 2.5 mm 2 mm 1 mm corner (body_l + 5 mm, (2*k - 1)*3 mm - 1 mm, plug_z + plug_h - 0.7 mm) cut targets Plug
}
box TongueBlock size (plug_l - 0.5 mm) (plug_w - 2*shell_t - 0.6 mm) 1.8 mm corner (body_l - 0.2 mm, -(plug_w - 2*shell_t - 0.6 mm)/2, plug_z + plug_h - shell_t - 1.9 mm) new -> body Tongue

// ==== Cap: a rounded sleeve, closed at +X, shown off the plug
box CapBlock size cap_l (cap_in_w + 2*cap_wall) (cap_in_h + 2*cap_wall) corner (cap_x, -(cap_in_w + 2*cap_wall)/2, split_z - cap_in_h/2 - cap_wall) new -> body Cap
fillet CapEdges edges (cap_x + cap_l/2, -(cap_in_w + 2*cap_wall)/2, split_z - cap_in_h/2 - cap_wall) (cap_x + cap_l/2, (cap_in_w + 2*cap_wall)/2, split_z - cap_in_h/2 - cap_wall) (cap_x + cap_l/2, -(cap_in_w + 2*cap_wall)/2, split_z + cap_in_h/2 + cap_wall) (cap_x + cap_l/2, (cap_in_w + 2*cap_wall)/2, split_z + cap_in_h/2 + cap_wall) radius 2 mm body Cap
box CapCavity size (cap_l - 1.6 mm + 1 mm) cap_in_w cap_in_h corner (cap_x - 1 mm, -cap_in_w/2, split_z - cap_in_h/2) cut targets Cap

// ==== Lanyard: a split ring through the hole, in the XZ plane
sketch RingSk on XY offset split_z {
  circle wire = (hole_x, 0) radius ring_wire
  construction line ax = (hole_x - ring_r, -5 mm) (hole_x - ring_r, 5 mm)
  fix wire ax
}
revolve Ring from RingSk axis ax new -> body Lanyard

color Bottom, Top, Cap "#2f6fd6"
color Pcb "#1f6b3a"
color Plug "#c9ccd1"
color Tongue "#f2f2f2"
color Lanyard "#9aa0a8"
material Bottom, Top, Cap PLA
material Plug Steel
