// 78. Spider lounge chair: three jointed legs of flat steel bar on pinned hubs, upholstered seat and back.
//
// Z is up, the sitter faces -Y, the seat is centred on the origin. Three arms run out under the
// seat from a node at its centre, 120 deg apart: two to the front corners, one straight back. At
// each corner a hub joins the arm to a strut rising to a knee hub, and from the knee a leg splays
// down to the floor, so every leg is the same zigzag in its own vertical plane. From the front the
// rear leg's plane is seen edge-on, so it reads as one upright behind the seat.
//
// The back pad stands on its own post, which rises from the rear arm just behind the seat, with a
// small ringed joint part way up; it doesn't touch the rear leg.
//
// One leg is drawn once, in 78_spider_chair_parts/leg.sc, and used three times: the front right
// turned to its heading, the front left mirrored from it, the rear turned straight back. Bars and
// hubs are laminated: a core plus 2 mm plates standing proud by `rib` make the ribbed edges. Seat
// and back are a steel pan with a cushion and a dished half-moon pad.
design SpiderChair units mm

// Sitting
param seat_h = 450 mm          // top of the seat cushion
param cushion_t = 60 mm
param pan_t = 4 mm

// Legs (all three the same): arm from the node to the corner hub, then knee and foot measured out
// from the hub. The front right arm points 30 deg below +X, the front left is its mirror, and the
// rear arm points along +Y.
param arm_l = 300 mm
param knee_out = 110 mm
param knee_up = 230 mm
param foot_out = 230 mm

// Bar stock and hubs
param bar_w = 36 mm
param bar_t = 14 mm
param rib = 1.5 mm
param hub_r = 26 mm

// Back pad and its post
param back_w = 440 mm
param back_h = 150 mm
param back_top = 790 mm
param back_t = 46 mm
param back_dish = 16 mm
param post_y = 262 mm          // the post's centre: behind the seat, in front of the rear hub

let frame_z = seat_h - cushion_t - pan_t - bar_w/2
let front_x = arm_l*cos(30 deg)
let front_y = -arm_l*sin(30 deg)
let back_y = post_y - bar_t/2               // the pad's back face, on the post's front face
let post_top = back_top - back_h/2
let joint_z = (frame_z + back_top - back_h)/2

// ==== Legs
use FrontRight from "78_spider_chair_parts/leg.sc" with (frame_z = frame_z, arm_l = arm_l, knee_out = knee_out, knee_up = knee_up, foot_out = foot_out, bar_w = bar_w, bar_t = bar_t, rib = rib, hub_r = hub_r) rotate Z (-30 deg) at (0, 0, 0)
mirror FrontLeft bodies FrontRight_Arm, FrontRight_Strut, FrontRight_Leg, FrontRight_Hub, FrontRight_Knee plane YZ
use Rear from "78_spider_chair_parts/leg.sc" with (frame_z = frame_z, arm_l = arm_l, knee_out = knee_out, knee_up = knee_up, foot_out = foot_out, bar_w = bar_w, bar_t = bar_t, rib = rib, hub_r = hub_r) rotate Z 90 deg at (0, 0, 0)

// Centre node where the three arms meet: an upright hub, ringed like the others
cylinder NodeCore radius (hub_r - rib) height (bar_w + 8 mm) base (0, 0, frame_z - bar_w/2 - 4 mm) axis (0,0,1) new -> body Node
for k from 0 to 10 {
  cylinder NodeRing{k} radius hub_r height 2 mm base (0, 0, frame_z - bar_w/2 - 4 mm + 4*k mm) axis (0,0,1) join targets Node
}

// ==== Back post: a laminated bar standing on the rear arm, ribbed on its sides like the legs
box PostCore size (bar_w - 2*rib) bar_t (post_top - frame_z) corner (-(bar_w - 2*rib)/2, post_y - bar_t/2, frame_z) new -> body BackPost
for k from 0 to 3 {
  box PostRib{k} size bar_w 2 mm (post_top - frame_z) corner (-bar_w/2, post_y - bar_t/2 + 4*k mm, frame_z) join targets BackPost
}
// Its foot: a ringed hub across the rear arm (the arm runs through it, along +Y)
cylinder PostFootCore radius (hub_r - rib) height (bar_w + 6 mm) base (-bar_w/2 - 3 mm, post_y, frame_z) axis (1,0,0) new -> body PostFoot
for k from 0 to 4 {
  cylinder PostFootRing{k} radius hub_r height 2 mm base (-bar_w/2 - 3 mm + 2 mm + 8*k mm, post_y, frame_z) axis (1,0,0) join targets PostFoot
}
// Part way up: a small ringed joint through the post
cylinder PostJointCore radius 15 mm height (bar_t + 8 mm) base (0, post_y - bar_t/2 - 4 mm, joint_z) axis (0,1,0) new -> body PostJoint
for k from 0 to 2 {
  cylinder PostJointRing{k} radius 16.5 mm height 2 mm base (0, post_y - bar_t/2 - 4 mm + 1 mm + 8*k mm, joint_z) axis (0,1,0) join targets PostJoint
}

// ==== Seat: a rounded triangle over the three corner hubs, clear of the back post
sketch SeatSk on XY offset (frame_z + bar_w/2) {
  polyline closed o = (-front_x - 60 mm, front_y - 45 mm) (front_x + 60 mm, front_y - 45 mm) (0, post_y + 10 mm)
  fillet o.1.end radius 90 mm        // front right
  fillet o.2.end radius 110 mm       // back
  fillet o.3.end radius 90 mm        // front left
  fix o
}
extrude SeatPan from SeatSk distance pan_t new -> body SeatPan
extrude SeatCushion from SeatSk distance cushion_t offset pan_t taper (-3 deg) new -> body SeatCushion
fillet SeatTop edges (0, front_y - 45 mm + cushion_t*tan(3 deg), seat_h) radius 24 mm body SeatCushion

// ==== Back: a half-moon pad on the post's top, its front dished to the sitter
sketch BackSk on XZ offset (back_y - back_t) {
  line top = (back_w/2, -back_top) (-back_w/2, -back_top)
  line left = top.end (-back_w/2, -(back_top - back_h*0.3))
  arc bottom = start left.end through (0, -(back_top - back_h)) end (back_w/2, -(back_top - back_h*0.3))
  line right = bottom.end top.start
  fillet top.end radius 30 mm
  fillet right.end radius 30 mm
  fix top left bottom right
}
extrude BackPad from BackSk distance back_t new -> body BackPad
cylinder BackDish radius (back_w*2) height (back_h + 20 mm) base (0, back_y - back_t + back_dish - back_w*2, back_top - back_h - 10 mm) axis (0,0,1) cut targets BackPad
cylinder BackWrap radius (back_w*2 + back_t) height (back_h + 20 mm) base (0, back_y - back_t + back_dish - back_w*2, back_top - back_h - 10 mm) axis (0,0,1) intersect targets BackPad

// ==== Finish (the legs bring their own steel colour)
color Node, BackPost, PostFoot, PostJoint, SeatPan "#b8bbbe"
material Node, BackPost, PostFoot, PostJoint, SeatPan Steel
color SeatCushion, BackPad "#1b1b1c"
