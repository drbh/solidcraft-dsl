// 24. Corner riser for stacking IKEA UPPDATERA boxes (white, 24 x 17 x 10 cm, 9 1/2 x 6 3/4").
//
// One part, print four: it is symmetric about the corner diagonal, so the same piece fits every
// corner. The underside is a channel that clips over the lower box's rim (outer skirt, inner lip);
// the top is a pocket that locates the upper box's base, so the boxes cannot nest or slide.
// `lift` is the floor between them; raise it for a gap you can reach into.
//
// Box measurements are from Donald Hobern's CC0 stacking bracket (printables.com/model/1551200):
// 31 mm corner radius top to bottom, 3 mm walls, sides sloping in at tan 0.09 (about 5 deg), the
// base 9 mm inside the rim outline with a 2 mm deep foot 11 mm inside it.
//
// Coordinates: the lower box's outside corner at the rim is the origin, the box runs along +X and
// +Y, z = 0 is the top of its rim. The riser has straight walls with the slope taken up in the
// clearances; LowerBox and UpperBox are sloped reference corners for checking fit.
// Print as modelled (skirt and lip on the bed); the channel roof bridges lip_in + fit (4.5 mm).
design UppdateraRiser units mm

// Box
param rim_t = 3 mm
param box_r = 31 mm
param slope = 0.09
param draft = 5.14 deg
param base_in = 9 mm
param foot_in = 11 mm
param foot_h = 2 mm

// Riser
param leg = 45 mm
param skirt = 2.5 mm
param lip = 2 mm
param grip = 10 mm
param lift = 3 mm
param pocket_h = 8 mm
param fit = 0.3 mm
let total_h = grip + lift + pocket_h
let g = slope * grip
let lip_in = rim_t + g + fit
let w_in = lip_in + lip
let far = 2 * box_r + 20 mm

// Block with a rounded outside corner, from the bottom of the grip to the top of the pocket
sketch BlockSk on XY offset (-grip) {
  rect rounded blk = (-skirt-fit,-skirt-fit) (far+10 mm,far+10 mm) radius (box_r+fit+skirt)
  fix blk.bottom blk.right blk.top blk.left blk.arc1 blk.arc2 blk.arc3 blk.arc4
}
extrude Block from BlockSk distance total_h -> body Riser

// Channel for the rim: clear everything inside the skirt below the rim top, then add the lip back.
// Straight walls: the skirt fits the rim at the top, the lip fits the wall at the bottom of the
// grip, where the slope has moved it in by g.
sketch ChannelSk on XY offset (-grip) {
  rect rounded ch = (-fit,-fit) (far,far) radius (box_r+fit)
  fix ch.bottom ch.right ch.top ch.left ch.arc1 ch.arc2 ch.arc3 ch.arc4
}
extrude Channel from ChannelSk distance grip cut targets Riser
sketch LipSk on XY offset (-grip) {
  rect rounded l = (lip_in,lip_in) (far,far) radius (box_r-lip_in)
  fix l.bottom l.right l.top l.left l.arc1 l.arc2 l.arc3 l.arc4
}
extrude Lip from LipSk distance grip join targets Riser

// Open the inside of the L below the floor, past the lip
sketch InsideSk on XY offset (-grip-1 mm) {
  rect rounded i = (w_in,w_in) (far,far) radius (box_r-w_in)
  fix i.bottom i.right i.top i.left i.arc1 i.arc2 i.arc3 i.arc4
}
extrude Inside from InsideSk distance (grip+1 mm) cut targets Riser

// Pocket for the upper box's base: the foot on the floor, the side (sloping out) above it
sketch FootSk on XY offset lift {
  rect rounded f = (foot_in-fit,foot_in-fit) (far,far) radius (box_r+fit)
  fix f.bottom f.right f.top f.left f.arc1 f.arc2 f.arc3 f.arc4
}
extrude FootPocket from FootSk distance (foot_h+1 mm) cut targets Riser
let p_in = base_in - slope * (pocket_h - foot_h) - fit
sketch PocketSk on XY offset (lift+foot_h) {
  rect rounded p = (p_in,p_in) (far,far) radius (box_r+fit)
  fix p.bottom p.right p.top p.left p.arc1 p.arc2 p.arc3 p.arc4
}
extrude Pocket from PocketSk distance (pocket_h-foot_h+1 mm) cut targets Riser

// Trim the legs to length
box TrimX size far (2*far) (total_h+2 mm) corner (leg,-far/2,-grip-1 mm) cut targets Riser
box TrimY size (2*far) far (total_h+2 mm) corner (-far/2,leg,-grip-1 mm) cut targets Riser
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
