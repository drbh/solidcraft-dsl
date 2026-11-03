// 25. Corner riser for stacking IKEA UPPDATERA boxes, v2: prints without supports, light and fast.
//
// Same fit as 24 (box measurements from Donald Hobern's CC0 bracket, printables.com/model/1551200:
// 31 mm corner radius, 3 mm walls sloping in at tan 0.09, base 9 mm inside the rim with a 2 mm foot
// 11 mm inside it). What changed for printing, as modelled with the skirt and lip on the bed:
//  - the floor the upper box stands on no longer hangs over the inside of the box: it is carried
//    by a 45 deg shelf that grows out of the inner lip, so every surface is printable as is;
//    the shelf ends shelf_in from the rim, just past the box's foot (foot_in)
//  - the pocket wall is wall_t thick instead of solid out to the skirt; the legs, grip, floor
//    and pocket are cut to what holds, and the shelf is windowed from below into gussets
//  - the skirt and pocket wall are notched into fingers; windows and skirt notches have 45 deg
//    peaked tops, so nothing is left floating
// The only bridge is the channel roof over the rim, rim_t + 2*slot_fit (3.4 mm).
// The slot's lip slopes with the box wall, so the slot is rim_t + 2*slot_fit wide all the way down.
//
// Coordinates: the lower box's outside corner at the rim is the origin, the box runs along +X and
// +Y, z = 0 is the top of its rim. LowerBox and UpperBox are sloped reference corners.
design UppdateraRiserV2 units mm

// Box
param rim_t = 3 mm
param box_r = 31 mm
param slope = 0.09
param draft = 5.14 deg
param base_in = 9 mm
param foot_in = 11 mm
param foot_h = 2 mm

// Riser
param leg = 37 mm
param skirt = 2 mm
param lip = 1.6 mm
param grip = 8 mm
param lift = 3 mm
param pocket_h = 6 mm
param fit = 0.3 mm
param slot_fit = 0.2 mm
param wall_t = 2 mm
param shelf = 45 deg
param shelf_in = 15 mm
param floor_t = 1.6 mm
param win_w = 6 mm
param skirt_win = 7 mm
param wall_win = 6 mm
param band = 2 mm
let total_h = grip + lift + pocket_h
let g = slope * grip
let lip_in = rim_t + g + slot_fit
let w_in = lip_in + lip
let far = 2 * box_r + 20 mm
let p_in = base_in - slope * (pocket_h - foot_h) - fit
let shelf_top = lift + 0.5 mm
let shelf_z = shelf_top - (shelf_in - w_in) / tan(shelf)

// Block with a rounded outside corner, from the bottom of the grip to the floor top
sketch BlockSk on XY offset (-grip) {
  rect rounded blk = (-skirt-fit,-skirt-fit) (far+10 mm,far+10 mm) radius (box_r+fit+skirt)
  fix blk.bottom blk.right blk.top blk.left blk.arc1 blk.arc2 blk.arc3 blk.arc4
}
extrude Block from BlockSk distance (grip+lift) -> body Riser

// Channel for the rim: clear everything inside the skirt below the rim top, then add the lip back.
// The skirt is straight and fits the rim at the top; the lip slopes with the wall (taper draft),
// sketched where the slope has moved the wall in by g at the bottom of the grip.
sketch ChannelSk on XY offset (-grip) {
  rect rounded ch = (-slot_fit,-slot_fit) (far,far) radius (box_r+slot_fit)
  fix ch.bottom ch.right ch.top ch.left ch.arc1 ch.arc2 ch.arc3 ch.arc4
}
extrude Channel from ChannelSk distance grip cut targets Riser
sketch LipSk on XY offset (-grip) {
  rect rounded l = (lip_in,lip_in) (far,far) radius (box_r-lip_in)
  fix l.bottom l.right l.top l.left l.arc1 l.arc2 l.arc3 l.arc4
}
extrude Lip from LipSk distance (grip+0.5 mm) taper draft join targets Riser

// Trim the legs to length
box TrimX size far (2*far) (total_h+2 mm) corner (leg,-far/2,-grip-1 mm) cut targets Riser
box TrimY size (2*far) far (total_h+2 mm) corner (-far/2,leg,-grip-1 mm) cut targets Riser

// Open the inside of the L past the lip, up to a 45 deg shelf that starts at shelf_z and ends
// shelf_in from the rim, just past the upper box's foot
sketch InsideSk on XY offset (-grip-1 mm) {
  rect rounded i = (w_in,w_in) (far,far) radius (box_r-w_in)
  fix i.bottom i.right i.top i.left i.arc1 i.arc2 i.arc3 i.arc4
}
extrude Inside from InsideSk distance (shelf_z+grip+1.3 mm) cut targets Riser
sketch ShelfSk on XY offset shelf_z {
  rect rounded i = (w_in,w_in) (far,far) radius (box_r-w_in)
  fix i.bottom i.right i.top i.left i.arc1 i.arc2 i.arc3 i.arc4
}
extrude Shelf from ShelfSk distance (shelf_top-shelf_z) taper (-shelf) cut targets Riser

// Windows up into the shelf from below, leaving radial gussets. Their tops are 45 deg peaks, so
// nothing bridges. Tool bodies are built pointing along -X from the origin, turned, then moved
// onto the corner arcs' shared centre (box_r, box_r).
let win_x = w_in + 1 mm - box_r
let win_l = shelf_in - w_in
let win_h = lift - floor_t - shelf_z + 1 mm
box Win1Box size win_l win_w win_h corner (win_x,-win_w/2,shelf_z-1 mm) new -> body Win1
chamfer Win1Peak edges (win_x+win_l/2,-win_w/2,(lift-floor_t)) (win_x+win_l/2,win_w/2,(lift-floor_t)) distance (win_w/2-0.05 mm) body Win1
box Win2Box size win_l win_w win_h corner (win_x,-win_w/2,shelf_z-1 mm) new -> body Win2
chamfer Win2Peak edges (win_x+win_l/2,-win_w/2,(lift-floor_t)) (win_x+win_l/2,win_w/2,(lift-floor_t)) distance (win_w/2-0.05 mm) body Win2
move Win2Turn bodies Win2 axis (0,0,1) angle 30 deg
box Win3Box size win_l win_w win_h corner (win_x,-win_w/2,shelf_z-1 mm) new -> body Win3
chamfer Win3Peak edges (win_x+win_l/2,-win_w/2,(lift-floor_t)) (win_x+win_l/2,win_w/2,(lift-floor_t)) distance (win_w/2-0.05 mm) body Win3
move Win3Turn bodies Win3 axis (0,0,1) angle 60 deg
box Win4Box size win_l win_w win_h corner (win_x,-win_w/2,shelf_z-1 mm) new -> body Win4
chamfer Win4Peak edges (win_x+win_l/2,-win_w/2,(lift-floor_t)) (win_x+win_l/2,win_w/2,(lift-floor_t)) distance (win_w/2-0.05 mm) body Win4
move Win4Turn bodies Win4 axis (0,0,1) angle 90 deg
move WinPlace bodies Win1, Win2, Win3, Win4 to (box_r,box_r,0 mm)
combine Windows target Riser tools Win1, Win2, Win3, Win4 cut

// Pocket wall, wall_t thick, on the floor
sketch WallSk on XY offset lift {
  rect rounded w = (p_in-wall_t,p_in-wall_t) (far+10 mm,far+10 mm) radius (box_r+fit+wall_t)
  fix w.bottom w.right w.top w.left w.arc1 w.arc2 w.arc3 w.arc4
}
extrude PocketWall from WallSk distance pocket_h join targets Riser

// Pocket for the upper box's base: the foot on the floor, the side (sloping out) above it
sketch FootSk on XY offset lift {
  rect rounded f = (foot_in-fit,foot_in-fit) (far,far) radius (box_r+fit)
  fix f.bottom f.right f.top f.left f.arc1 f.arc2 f.arc3 f.arc4
}
extrude FootPocket from FootSk distance (foot_h+1 mm) cut targets Riser
sketch PocketSk on XY offset (lift+foot_h) {
  // Past `far`, so its far corner arcs miss the foot pocket's: coincident curved faces fail newer engines
  rect rounded p = (p_in,p_in) (far+1 mm,far+1 mm) radius (box_r+fit)
  fix p.bottom p.right p.top p.left p.arc1 p.arc2 p.arc3 p.arc4
}
extrude Pocket from PocketSk distance (pocket_h-foot_h+1 mm) cut targets Riser

// Trim the wall to the legs
box TrimWallX size far (2*far) (total_h+2 mm) corner (leg,-far/2,-grip-1 mm) cut targets Riser
box TrimWallY size (2*far) far (total_h+2 mm) corner (-far/2,leg,-grip-1 mm) cut targets Riser

// Notches up into the skirt from the bed, leaving fingers; 45 deg peaked tops, built like the
// windows. Then notches down into the pocket wall from the top, above the step.
let notch_x = -skirt - fit - 1 mm - box_r
let notch_l = skirt + fit + 1 mm
let notch_h = grip + 1 mm - band
box Notch1Box size notch_l skirt_win notch_h corner (notch_x,-skirt_win/2,-grip-1 mm) new -> body Notch1
chamfer Notch1Peak edges (notch_x+notch_l/2,-skirt_win/2,(-band)) (notch_x+notch_l/2,skirt_win/2,(-band)) distance (skirt_win/2-0.05 mm) body Notch1
box Notch2Box size notch_l skirt_win notch_h corner (notch_x,-skirt_win/2,-grip-1 mm) new -> body Notch2
chamfer Notch2Peak edges (notch_x+notch_l/2,-skirt_win/2,(-band)) (notch_x+notch_l/2,skirt_win/2,(-band)) distance (skirt_win/2-0.05 mm) body Notch2
move Notch2Turn bodies Notch2 axis (0,0,1) angle 22.5 deg
box Notch3Box size notch_l skirt_win notch_h corner (notch_x,-skirt_win/2,-grip-1 mm) new -> body Notch3
chamfer Notch3Peak edges (notch_x+notch_l/2,-skirt_win/2,(-band)) (notch_x+notch_l/2,skirt_win/2,(-band)) distance (skirt_win/2-0.05 mm) body Notch3
move Notch3Turn bodies Notch3 axis (0,0,1) angle 45 deg
box Notch4Box size notch_l skirt_win notch_h corner (notch_x,-skirt_win/2,-grip-1 mm) new -> body Notch4
chamfer Notch4Peak edges (notch_x+notch_l/2,-skirt_win/2,(-band)) (notch_x+notch_l/2,skirt_win/2,(-band)) distance (skirt_win/2-0.05 mm) body Notch4
move Notch4Turn bodies Notch4 axis (0,0,1) angle 67.5 deg
box Notch5Box size notch_l skirt_win notch_h corner (notch_x,-skirt_win/2,-grip-1 mm) new -> body Notch5
chamfer Notch5Peak edges (notch_x+notch_l/2,-skirt_win/2,(-band)) (notch_x+notch_l/2,skirt_win/2,(-band)) distance (skirt_win/2-0.05 mm) body Notch5
move Notch5Turn bodies Notch5 axis (0,0,1) angle 90 deg
move NotchPlace bodies Notch1, Notch2, Notch3, Notch4, Notch5 to (box_r,box_r,0 mm)
combine SkirtNotches target Riser tools Notch1, Notch2, Notch3, Notch4, Notch5 cut
// The pocket follows the upper box's base, so its arc is centred at (p_c, p_c), not (box_r, box_r).
// Its notches are tool bodies built pointing along -X about the origin, turned and moved onto it.
let p_c = p_in + box_r + fit
let wn_z = lift + foot_h + 0.5 mm
box WallNotchA size (wall_t+1.5 mm) wall_win (pocket_h-foot_h+0.5 mm) corner (-box_r-fit-wall_t-1 mm,-wall_win/2,wn_z) new -> body NotchA
box WallNotchB size (wall_t+1.5 mm) wall_win (pocket_h-foot_h+0.5 mm) corner (-box_r-fit-wall_t-1 mm,-wall_win/2,wn_z) new -> body NotchB
box WallNotchC size (wall_t+1.5 mm) wall_win (pocket_h-foot_h+0.5 mm) corner (-box_r-fit-wall_t-1 mm,-wall_win/2,wn_z) new -> body NotchC
move TurnA bodies NotchA axis (0,0,1) angle 22.5 deg
move TurnB bodies NotchB axis (0,0,1) angle 45 deg
move TurnC bodies NotchC axis (0,0,1) angle 67.5 deg
move PlaceNotches bodies NotchA, NotchB, NotchC to (p_c,p_c,0 mm)
combine WallNotches target Riser tools NotchA, NotchB, NotchC cut
// Lead-in on each wall segment between the notches (points on the pocket arc, radius box_r + fit)
let pr = box_r + fit
let ptop = lift + pocket_h
chamfer PocketLeadIn edges (p_c-pr*cos(11.25 deg),p_c-pr*sin(11.25 deg),ptop) (p_c-pr*cos(33.75 deg),p_c-pr*sin(33.75 deg),ptop) (p_c-pr*cos(56.25 deg),p_c-pr*sin(56.25 deg),ptop) (p_c-pr*cos(78.75 deg),p_c-pr*sin(78.75 deg),ptop) distance 1 mm body Riser

// Reference corners of the two boxes (walls only)
let low_d = 30 mm
let gl = slope * low_d
sketch LowerSk on XY offset (-low_d) {
  rect rounded o = (gl,gl) (far,far) radius (box_r-gl)
  fix o.bottom o.right o.top o.left o.arc1 o.arc2 o.arc3 o.arc4
}
extrude LowerOuter from LowerSk distance low_d taper draft new -> body LowerBox
let gl1 = slope * (low_d + 1 mm)
sketch LowerInSk on XY offset (-low_d-1 mm) {
  rect rounded i = (gl1+rim_t,gl1+rim_t) (far+10 mm,far+10 mm) radius (box_r-rim_t-gl1)
  fix i.bottom i.right i.top i.left i.arc1 i.arc2 i.arc3 i.arc4
}
extrude LowerInner from LowerInSk distance (low_d+2 mm) taper draft cut targets LowerBox
sketch UpperFootSk on XY offset lift {
  rect rounded o = (foot_in,foot_in) (far,far) radius box_r
  fix o.bottom o.right o.top o.left o.arc1 o.arc2 o.arc3 o.arc4
}
extrude UpperFoot from UpperFootSk distance foot_h new -> body UpperBox
sketch UpperSk on XY offset (lift+foot_h) {
  rect rounded o = (base_in,base_in) (far,far) radius box_r
  fix o.bottom o.right o.top o.left o.arc1 o.arc2 o.arc3 o.arc4
}
extrude UpperOuter from UpperSk distance 20 mm taper draft join targets UpperBox
sketch UpperInSk on XY offset (lift+foot_h+rim_t) {
  rect rounded i = (base_in+rim_t,base_in+rim_t) (far+10 mm,far+10 mm) radius (box_r-rim_t)
  fix i.bottom i.right i.top i.left i.arc1 i.arc2 i.arc3 i.arc4
}
extrude UpperInner from UpperInSk distance 20 mm taper draft cut targets UpperBox
