// 100. Tensegrity side table: the top floats on cables, with no leg between it and the base.
//
// Z is up, the table is centred on the origin, the base post is at the back (+Y). The base carries
// a post at its back edge with an arm reaching forward to the centre; the top carries a post at its
// front edge with an arm reaching back to the centre, below the base's arm. A short centre link
// joins the two arm ends, so the top hangs from the base: the link is in tension and carries the
// whole load. Four corner cables, also in tension, stop the top from tipping. Nothing is in
// compression except the plates, posts and arms, and no two of those touch.
//
// `cable_l` is the free length between a corner's two anchors; the table height follows from it,
// so cut the cables to length and the table comes out level and at height. Tension them with the
// turnbuckles until the top stops rocking, then lock the turnbuckle nuts.
//
// Make it: plates, posts and arms are 18 mm birch plywood (cut list below), glued and screwed with
// 4 x 40 mm wood screws through the plates into the posts and through the posts into the arms. The
// ten cable anchors are printed (PETG, 4 walls, 40 % infill; print recipe included) and screwed on
// with two 4 x 20 mm countersunk wood screws each. The cables are 3 mm 7x7 stainless wire rope:
// thread through an anchor, crimp a 3 mm ferrule, which sits in the anchor's pocket against the
// wood, and the same at the other end through an M5 hook-and-eye turnbuckle on each corner cable.
//
// Cut list (18 mm ply, at the default sizes): 2 x plate 450 x 450; 2 x post 60 x 282 (the base post
// stands on the base, the top post hangs from the top); 2 x arm 60 x 197.
// BOM: 5 x 3 mm stainless wire rope (4 x about `cable_l` + 100 mm, 1 x 150 mm), 10 x 3 mm aluminium
// crimp ferrules, 4 x M5 turnbuckles, 20 x 4 x 20 mm and 8 x 4 x 40 mm wood screws, wood glue.
design TensegrityTable units mm

param table_w = 450 mm         // base and top are squares this size
param ply_t = 18 mm
param arm_w = 60 mm
param inset = 40 mm            // posts and corner anchors in from the plate edges
param cable_l = 440 mm         // free length of each corner cable, anchor to anchor
param link_l = 40 mm           // free length of the centre link
param anchor_h = 12 mm
param cable_d = 3 mm

let height = 2*ply_t + 2*anchor_h + cable_l           // floor to the top surface
let top_under = height - ply_t
let top_arm_z = height/2 - link_l/2 - anchor_h        // top of the top arm
let base_arm_z = top_arm_z + 2*anchor_h + link_l      // underside of the base arm
let c = table_w/2 - inset                             // corner anchor offset

// ==== Plywood
box BasePlate size table_w table_w ply_t corner (-table_w/2, -table_w/2, 0) new -> body BasePlate
box BasePost size arm_w ply_t (base_arm_z + ply_t - ply_t) corner (-arm_w/2, table_w/2 - inset - ply_t, ply_t) new -> body BasePost
box BaseArm size arm_w (table_w/2 - inset - ply_t + 30 mm) ply_t corner (-arm_w/2, -30 mm, base_arm_z) new -> body BaseArm
box TopPlate size table_w table_w ply_t corner (-table_w/2, -table_w/2, top_under) new -> body TopPlate
box TopPost size arm_w ply_t (top_under - (top_arm_z - ply_t)) corner (-arm_w/2, -table_w/2 + inset, top_arm_z - ply_t) new -> body TopPost
box TopArm size arm_w (table_w/2 - inset - ply_t + 30 mm) ply_t corner (-arm_w/2, -table_w/2 + inset + ply_t, top_arm_z - ply_t) new -> body TopArm

// ==== Printed anchors: a block with a cable hole, a ferrule pocket on the side against the wood,
// and two countersunk screw holes. k = 0..3 base corners, 4..7 top corners, 8 on the top arm,
// 9 under the base arm.
for k from 0 to 3 {
  box BaseAnchor{k} size 30 mm 30 mm anchor_h corner ((2*mod(k, 2) - 1)*c - 15 mm, (2*floor(k/2) - 1)*c - 15 mm, ply_t) new -> body Anchor{k}
}
for k from 4 to 7 {
  box TopAnchor{k} size 30 mm 30 mm anchor_h corner ((2*mod(k - 4, 2) - 1)*c - 15 mm, (2*floor((k - 4)/2) - 1)*c - 15 mm, top_under - anchor_h) new -> body Anchor{k}
}
box TopArmAnchor size 30 mm 30 mm anchor_h corner (-15 mm, -15 mm, top_arm_z) new -> body Anchor8
box BaseArmAnchor size 30 mm 30 mm anchor_h corner (-15 mm, -15 mm, base_arm_z - anchor_h) new -> body Anchor9

// Holes, the same in every anchor: a 3.5 mm cable hole, a 7 mm x 5 mm ferrule pocket on the wood
// side, and two 4.5 mm screw holes with 9 mm x 2.5 mm counterbores on the open side
for k from 0 to 3 {
  cylinder BaseHole{k} radius 1.75 mm height (anchor_h + 2 mm) base ((2*mod(k, 2) - 1)*c, (2*floor(k/2) - 1)*c, ply_t - 1 mm) axis (0,0,1) cut targets Anchor{k}
  cylinder BasePocket{k} radius 3.5 mm height 5.1 mm base ((2*mod(k, 2) - 1)*c, (2*floor(k/2) - 1)*c, ply_t - 0.1 mm) axis (0,0,1) cut targets Anchor{k}
}
for k from 4 to 7 {
  cylinder TopHole{k} radius 1.75 mm height (anchor_h + 2 mm) base ((2*mod(k - 4, 2) - 1)*c, (2*floor((k - 4)/2) - 1)*c, top_under - anchor_h - 1 mm) axis (0,0,1) cut targets Anchor{k}
  cylinder TopPocket{k} radius 3.5 mm height 5.1 mm base ((2*mod(k - 4, 2) - 1)*c, (2*floor((k - 4)/2) - 1)*c, top_under - 5 mm) axis (0,0,1) cut targets Anchor{k}
}
cylinder TopArmHole radius 1.75 mm height (anchor_h + 2 mm) base (0, 0, top_arm_z - 1 mm) axis (0,0,1) cut targets Anchor8
cylinder TopArmPocket radius 3.5 mm height 5.1 mm base (0, 0, top_arm_z - 0.1 mm) axis (0,0,1) cut targets Anchor8
cylinder BaseArmHole radius 1.75 mm height (anchor_h + 2 mm) base (0, 0, base_arm_z - anchor_h - 1 mm) axis (0,0,1) cut targets Anchor9
cylinder BaseArmPocket radius 3.5 mm height 5.1 mm base (0, 0, base_arm_z - 5 mm) axis (0,0,1) cut targets Anchor9
for k from 0 to 3 {
  for s from 0 to 1 {
    cylinder BaseScrew{k}_{s} radius 2.25 mm height (anchor_h + 2 mm) base ((2*mod(k, 2) - 1)*c + (2*s - 1)*10 mm, (2*floor(k/2) - 1)*c, ply_t - 1 mm) axis (0,0,1) cut targets Anchor{k}
    cylinder BaseHead{k}_{s} radius 4.5 mm height 2.6 mm base ((2*mod(k, 2) - 1)*c + (2*s - 1)*10 mm, (2*floor(k/2) - 1)*c, ply_t + anchor_h - 2.5 mm) axis (0,0,1) cut targets Anchor{k}
  }
}
for k from 4 to 7 {
  for s from 0 to 1 {
    cylinder TopScrew{k}_{s} radius 2.25 mm height (anchor_h + 2 mm) base ((2*mod(k - 4, 2) - 1)*c + (2*s - 1)*10 mm, (2*floor((k - 4)/2) - 1)*c, top_under - anchor_h - 1 mm) axis (0,0,1) cut targets Anchor{k}
    cylinder TopHead{k}_{s} radius 4.5 mm height 2.6 mm base ((2*mod(k - 4, 2) - 1)*c + (2*s - 1)*10 mm, (2*floor((k - 4)/2) - 1)*c, top_under - anchor_h - 0.1 mm) axis (0,0,1) cut targets Anchor{k}
  }
}
for s from 0 to 1 {
  cylinder TopArmScrew{s} radius 2.25 mm height (anchor_h + 2 mm) base ((2*s - 1)*10 mm, 0, top_arm_z - 1 mm) axis (0,0,1) cut targets Anchor8
  cylinder TopArmHead{s} radius 4.5 mm height 2.6 mm base ((2*s - 1)*10 mm, 0, top_arm_z + anchor_h - 2.5 mm) axis (0,0,1) cut targets Anchor8
  cylinder BaseArmScrew{s} radius 2.25 mm height (anchor_h + 2 mm) base ((2*s - 1)*10 mm, 0, base_arm_z - anchor_h - 1 mm) axis (0,0,1) cut targets Anchor9
  cylinder BaseArmHead{s} radius 4.5 mm height 2.6 mm base ((2*s - 1)*10 mm, 0, base_arm_z - anchor_h - 0.1 mm) axis (0,0,1) cut targets Anchor9
}

// ==== Cables and turnbuckles
for k from 0 to 3 {
  cylinder Cable{k} radius (cable_d/2) height (cable_l + 2*anchor_h - 2 mm) base ((2*mod(k, 2) - 1)*c, (2*floor(k/2) - 1)*c, ply_t + 1 mm) axis (0,0,1) new -> body Cable{k}
  cylinder Turnbuckle{k} radius 5 mm height 70 mm base ((2*mod(k, 2) - 1)*c, (2*floor(k/2) - 1)*c, ply_t + anchor_h + 40 mm) axis (0,0,1) new -> body Turnbuckle{k}
}
cylinder Link radius (cable_d/2) height (link_l + 2*anchor_h - 2 mm) base (0, 0, top_arm_z + 1 mm) axis (0,0,1) new -> body Link

color BasePlate, BasePost, BaseArm, TopPlate, TopPost, TopArm "#d9b98c"
color Anchor0, Anchor1, Anchor2, Anchor3, Anchor4, Anchor5, Anchor6, Anchor7, Anchor8, Anchor9 "#2e2e30"
color Cable0, Cable1, Cable2, Cable3, Link "#b9bcc0"
color Turnbuckle0, Turnbuckle1, Turnbuckle2, Turnbuckle3 "#8e9196"
material Cable0, Cable1, Cable2, Cable3, Link, Turnbuckle0, Turnbuckle1, Turnbuckle2, Turnbuckle3 Steel
