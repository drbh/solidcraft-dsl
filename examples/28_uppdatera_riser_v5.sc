// 28. Corner riser for stacking IKEA UPPDATERA boxes, v5: v3 with the upper box raised by stilt.
//
// v5 changes against v3 (the windowed-shelf version of 25, before v4's skirt and wall notches):
//  - the floor and everything on it sit stilt higher (lift = 3 mm + stilt). The extra height is
//    a stilt_t wall standing on the rim and lip, flush with the inside of the lip: above the
//    channel roof (roof_t) the outside steps in at 45 deg from the skirt to the wall. The shelf,
//    windows and floor are v3's, only higher, so the added layers are a few perimeters
//  - the slot is tighter: slot_fit 0.1 mm instead of 0.2 mm, as v3 rocked on the rim
//  - the shelf windows have 45 deg peaked tops (the floating-cantilever fix from 25), so the
//    only bridge left is the channel roof
//
// Same fit as 24 (box measurements from Donald Hobern's CC0 bracket, printables.com/model/1551200:
// 31 mm corner radius, 3 mm walls sloping in at tan 0.09, base 9 mm inside the rim with a 2 mm foot
// 11 mm inside it). What changed for printing, as modelled with the skirt and lip on the bed:
//  - the floor the upper box stands on no longer hangs over the inside of the box: it is carried
//    by a 45 deg shelf that grows out of the inner lip, so every surface is printable as is;
//    the shelf ends shelf_in from the rim, just past the box's foot (foot_in)
//  - the pocket wall is wall_t thick instead of solid out to the skirt; the legs, grip, floor
//    and pocket are cut to what holds, and the shelf is windowed from below into gussets
// The only bridge is the channel roof over the rim, rim_t + 2*slot_fit (3.4 mm).
// The slot's lip slopes with the box wall, so the slot is rim_t + 2*slot_fit wide all the way down.
//
// Coordinates: the lower box's outside corner at the rim is the origin, the box runs along +X and
// +Y, z = 0 is the top of its rim. LowerBox and UpperBox are sloped reference corners.
design UppdateraRiserV5 units mm

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
param stilt = 20 mm
param pocket_h = 6 mm
param fit = 0.3 mm
param slot_fit = 0.1 mm
param wall_t = 2 mm
param shelf = 45 deg
param shelf_in = 15 mm
param stilt_t = 3 mm
param roof_t = 1.6 mm
param floor_t = 1.6 mm
param win_w = 6 mm
let lift = 3 mm + stilt
let total_h = grip + lift + pocket_h
let g = slope * grip
let lip_in = rim_t + g + slot_fit
let w_in = lip_in + lip
let far = 2 * box_r + 20 mm
let p_in = base_in - slope * (pocket_h - foot_h) - fit
let shelf_top = lift + 0.5 mm
let o_in = w_in - stilt_t
let step_z = roof_t + (o_in + skirt + fit) / tan(shelf)
let shelf_z = shelf_top - (shelf_in - w_in) / tan(shelf)

// Block with a rounded outside corner, from the bottom of the grip to the channel roof top, then
// the stilt: stepping in at 45 deg to o_in from the rim and straight up to the floor top
sketch BlockSk on XY offset (-grip) {
  rect rounded blk = (-skirt-fit,-skirt-fit) (far+10 mm,far+10 mm) radius (box_r+fit+skirt)
  fix blk.bottom blk.right blk.top blk.left blk.arc1 blk.arc2 blk.arc3 blk.arc4
}
extrude Block from BlockSk distance (grip+roof_t) -> body Riser
sketch StepSk on XY offset roof_t {
  rect rounded st = (-skirt-fit,-skirt-fit) (far+10 mm,far+10 mm) radius (box_r+fit+skirt)
  fix st.bottom st.right st.top st.left st.arc1 st.arc2 st.arc3 st.arc4
}
extrude Step from StepSk distance (step_z-roof_t) taper (-shelf) join targets Riser
sketch StiltSk on XY offset step_z {
  rect rounded sw = (o_in,o_in) (far+10 mm,far+10 mm) radius (box_r-o_in)
  fix sw.bottom sw.right sw.top sw.left sw.arc1 sw.arc2 sw.arc3 sw.arc4
}
extrude Stilt from StiltSk distance (lift-step_z) join targets Riser

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
chamfer PocketLeadIn edges (leg-2 mm,p_in,lift+pocket_h) (p_in,leg-2 mm,lift+pocket_h) distance 1 mm body Riser

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
