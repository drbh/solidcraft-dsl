// 125. Headphone stand: a ballasted round base, a square post with a cable spool, and an arched
// hanger with a channel for a felt or foam pad.
//
// Z is up, the base is centred on the origin, the hanger's arch runs along X and its width along Y.
// The post plugs into a square socket in the base and the hanger drops onto the post's top tenon;
// both tenons are square so the post can't turn and every socket prints as a square hole. Two
// flanges on the post make a spool for the headphone cable; their undersides are 45 deg cones so
// the post prints standing up. The base is hollow: fill the pocket with washers or sand, then close
// it with the cover on three M3 screws.
//
// Print (PLA, 0.2 mm layers, no supports):
//   Base    upside down, its top on the bed (the layout turns it), so the pocket is open upward
//   Cover   flat
//   Post    standing on its foot
//   Hanger  on its side, the arch flat on the bed: every face is then either flat or vertical
//
// BOM: 3 x M3x8 self-tapping screws, about 300 g of ballast (M10 washers or dry sand), a 30 mm
// wide felt or foam strip for the hanger channel, glue for the strip (optional).
design HeadphoneStand units mm

param base_d = 150 mm
param base_h = 18 mm
param skin = 3 mm              // base top and wall thickness over the pocket
param cover_t = 3 mm
param post_w = 26 mm
param post_h = 220 mm
param tenon_w = 18 mm
param tenon_l = 16 mm
param foot_l = 12 mm            // the shorter tenon into the base
param fit = 0.25 mm            // clearance per side in each square socket
param spool_lo = 70 mm         // height of the lower spool flange above the base
param spool_gap = 60 mm        // wound length between the flanges
param flange_r = 32 mm
param arch_r = 62 mm           // outer radius of the hanger arch
param arch_t = 12 mm
param arch_w = 44 mm
param arch_span = 140 deg
param pad_w = 32 mm
param pad_d = 2 mm

let pocket_r = base_d/2 - skin
let recess_r = pocket_r + 2 mm
let pocket_top = base_h - skin
let boss_r = 5 mm
let boss_at = pocket_r - 9 mm
let post_z0 = base_h                       // the post's shoulder sits on the base's top
let post_top = post_z0 + post_h
let arch_c = post_top + tenon_l - arch_r + arch_t + 4 mm   // the arch's centre height
let half = arch_span/2

// ==== Base: a disc with a bottom recess for the cover and a ballast pocket above it
cylinder BaseDisc radius (base_d/2) height base_h new -> body Base
chamfer BaseTop edges (base_d/2, 0, base_h) distance 3 mm body Base
cylinder CoverSeat radius recess_r height (cover_t + 1 mm) base (0, 0, -1 mm) cut targets Base
cylinder Pocket radius pocket_r height (pocket_top - cover_t + 0.1 mm) base (0, 0, cover_t - 0.1 mm) cut targets Base
for k from 0 to 2 {
  cylinder Boss{k} radius boss_r height (pocket_top - cover_t) base (boss_at*cos(90 deg + k*120 deg), boss_at*sin(90 deg + k*120 deg), cover_t + 0.2 mm) join targets Base
  cylinder Pilot{k} radius 1.25 mm height 10 mm base (boss_at*cos(90 deg + k*120 deg), boss_at*sin(90 deg + k*120 deg), cover_t - 0.5 mm) cut targets Base
}
cylinder SocketBoss radius (tenon_w/2 + 8 mm) height (pocket_top - cover_t) base (0, 0, cover_t + 0.2 mm) join targets Base
box Socket size (tenon_w + 2*fit) (tenon_w + 2*fit) (foot_l + 1.5 mm) corner (-(tenon_w/2 + fit), -(tenon_w/2 + fit), base_h - foot_l - 0.5 mm) cut targets Base

// The cover: a plate in the recess, screwed to the bosses
cylinder CoverPlate radius (recess_r - fit) height cover_t new -> body Cover
for k from 0 to 2 {
  cylinder CoverHole{k} radius 1.7 mm height (cover_t + 2 mm) base (boss_at*cos(90 deg + k*120 deg), boss_at*sin(90 deg + k*120 deg), -1 mm) cut targets Cover
}

// ==== Post: square, with a tenon at each end and two 45 deg spool flanges
box PostBar size post_w post_w post_h corner (-post_w/2, -post_w/2, post_z0) new -> body Post
box FootTenon size tenon_w tenon_w (foot_l + 0.5 mm) corner (-tenon_w/2, -tenon_w/2, post_z0 - foot_l) join targets Post
box TopTenon size tenon_w tenon_w (tenon_l + 0.5 mm) corner (-tenon_w/2, -tenon_w/2, post_top - 0.5 mm) join targets Post
cone LowerFlange radius (post_w/2 - 2 mm) top_radius flange_r height (flange_r - post_w/2 + 2 mm) base (0, 0, post_z0 + spool_lo - 2 mm) axis Z join targets Post
cone UpperFlange radius (post_w/2 - 2 mm) top_radius flange_r height (flange_r - post_w/2 + 2 mm) base (0, 0, post_z0 + spool_lo + spool_gap - 2 mm) axis Z join targets Post
cylinder LowerRim radius (flange_r - 0.4 mm) height 3 mm base (0, 0, post_z0 + spool_lo + flange_r - post_w/2 - 0.3 mm) join targets Post
cylinder UpperRim radius (flange_r - 0.4 mm) height 3 mm base (0, 0, post_z0 + spool_lo + spool_gap + flange_r - post_w/2 - 0.3 mm) join targets Post
box CableSlot size 3 mm (flange_r + 2 mm) 4 mm corner (-1.5 mm, post_w/2 - 2 mm, post_z0 + spool_lo + spool_gap + flange_r - post_w/2 - 0.5 mm) cut targets Post

// ==== Hanger: an arch drawn on XZ (sketch v is -z there) and extruded across Y
sketch ArchSk on XZ offset (-arch_w/2) {
  arc outer = center (0, -arch_c) start (arch_r*cos(90 deg + half), -(arch_c + arch_r*sin(90 deg + half))) end (arch_r*cos(90 deg - half), -(arch_c + arch_r*sin(90 deg - half)))
  arc inner = center (0, -arch_c) start ((arch_r - arch_t)*cos(90 deg + half), -(arch_c + (arch_r - arch_t)*sin(90 deg + half))) end ((arch_r - arch_t)*cos(90 deg - half), -(arch_c + (arch_r - arch_t)*sin(90 deg - half)))
  line e1 = outer.start inner.start
  line e2 = outer.end inner.end
  fix outer inner e1 e2
}
extrude Arch from ArchSk distance arch_w new -> body Hanger
// The hub under the crown, with the square socket for the post's top tenon
box Hub size (post_w + 8 mm) arch_w (arch_c + arch_r - arch_t + 1 mm - post_top) corner (-(post_w/2 + 4 mm), -arch_w/2, post_top) join targets Hanger
box TopSocket size (tenon_w + 2*fit) (tenon_w + 2*fit) (tenon_l + 1.5 mm) corner (-(tenon_w/2 + fit), -(tenon_w/2 + fit), post_top - 1 mm) cut targets Hanger
// The pad channel: the arch's outer face stepped down by pad_d over the middle pad_w
sketch ChannelSk on XZ offset (-pad_w/2) {
  arc outer = center (0, -arch_c) start ((arch_r + 1 mm)*cos(90 deg + half + 2 deg), -(arch_c + (arch_r + 1 mm)*sin(90 deg + half + 2 deg))) end ((arch_r + 1 mm)*cos(90 deg - half - 2 deg), -(arch_c + (arch_r + 1 mm)*sin(90 deg - half - 2 deg)))
  arc inner = center (0, -arch_c) start ((arch_r - pad_d)*cos(90 deg + half + 2 deg), -(arch_c + (arch_r - pad_d)*sin(90 deg + half + 2 deg))) end ((arch_r - pad_d)*cos(90 deg - half - 2 deg), -(arch_c + (arch_r - pad_d)*sin(90 deg - half - 2 deg)))
  line e1 = outer.start inner.start
  line e2 = outer.end inner.end
  fix outer inner e1 e2
}
extrude Channel from ChannelSk distance pad_w cut targets Hanger

color Base, Cover "#2b2d31"
color Post "#c9a27a"
color Hanger "#2b2d31"
material Base, Cover, Post, Hanger PLA
