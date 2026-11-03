// 128. Yo-yo: two printed halves on a C-size ball bearing, an M5 axle bolt and adjustable gap shims.
//
// Z is the axle, the string gap is centred on z = 0. Each half is one revolved section: a flat
// inner face with a groove for a response pad, a 45 deg chamfer up to the rim, a rim-weighted cup
// open to the outside, and a solid hub round the axle. The bearing sits in the gap with its inner
// race clamped between two printed shims; the outer race spins free, and the string loops round it.
// Thicker shims widen the gap (unresponsive, for tricks); thinner ones tighten it.
//
// Hardware (BOM):
//   1 C-size bearing, 10 x 5 x 4 mm (KonKan C size or any 10x5x4 deep-groove bearing)
//   1 M5 socket-head bolt, length bolt_l (the header's `bolt_l`, 35 mm at the defaults)
//   1 M5 hex nut (8 mm across flats), sits in a pocket in half 1's outside
//   2 response pads, 19 mm OD x 12.6 mm ID (standard slim pads), or a ring of silicone
//   yo-yo string
//
// Assembly: drop the nut into half 1's pocket, put a shim on the bolt, then the bearing, the
// other shim and half 1. Thread the bolt in from half 0's counterbore and tighten until the
// halves clamp the shims; the outer race should still spin freely.
//
// Printing: both halves face down (half 1 is turned over on the plate), PLA or PETG, 100% infill
// in the rims for weight if you like, no supports: the chamfer and the cup walls are at least
// 45 deg and the pad groove is a 3.2 mm bridge. Shims print flat.
design YoYo units mm

param dia = 56 mm
param half_w = 19 mm          // each half, from its face to its outside
param bearing_od = 10 mm
param bearing_id = 5 mm
param bearing_w = 4 mm
param shim = 0.25 mm          // the gap: each face stands bearing_w/2 + shim off the centre
param pad_od = 19 mm
param pad_id = 12.6 mm
param pad_depth = 1 mm
param hub_r = 8 mm
param axle_clear = 2.7 mm     // M5 clearance radius
param nut_af = 8 mm
param nut_t = 4 mm
param head_d = 9 mm           // M5 socket head with clearance
param head_t = 5 mm

let face_z = bearing_w/2 + shim
let rim = dia/2
let bolt_l = 2*(face_z + half_w) - head_t - 1 mm

// Each half, side 0 above the gap (+Z) and side 1 below it; s = +1 or -1 turns the section over.
// On XZ, sketch v is -z, so a point at height z is written (r, -z).
for side from 0 to 1 {
  sketch HalfSk{side} on XZ {
    polyline closed o = (axle_clear, -(1 - 2*side)*face_z) (pad_id/2, -(1 - 2*side)*face_z) (pad_id/2, -(1 - 2*side)*(face_z + pad_depth)) (pad_od/2, -(1 - 2*side)*(face_z + pad_depth)) (pad_od/2, -(1 - 2*side)*face_z) (rim - 5 mm, -(1 - 2*side)*face_z) (rim, -(1 - 2*side)*(face_z + 5 mm)) (rim, -(1 - 2*side)*(face_z + half_w - 2 mm)) (rim - 2 mm, -(1 - 2*side)*(face_z + half_w)) (rim - 4 mm, -(1 - 2*side)*(face_z + half_w)) (rim - 6.5 mm, -(1 - 2*side)*(face_z + half_w - 7 mm)) (hub_r + 1 mm, -(1 - 2*side)*(face_z + half_w - 7 mm)) (hub_r, -(1 - 2*side)*(face_z + half_w)) (axle_clear, -(1 - 2*side)*(face_z + half_w))
    fix o
  }
  revolve Half{side} from HalfSk{side} axis Z new -> body Half{side}
}

// Half 0: a counterbore for the bolt head in its outside
cylinder HeadBore radius (head_d/2) height (head_t + 1 mm) base (0, 0, face_z + half_w - head_t) axis (0,0,1) cut targets Half0
// Half 1: a hex pocket for the nut in its outside
sketch NutSk on XY offset (-(face_z + half_w) - 0.5 mm) {
  polygon n = (0, 0) radius (nut_af/sqrt(3) + 0.15 mm) sides 6
  fix n
}
extrude NutPocket from NutSk distance (nut_t + 0.5 mm) cut targets Half1

// Shims on the bearing's inner race (a 0.2 mm loose fit on the bolt)
cylinder Shim0Ring radius 3.3 mm height shim base (0, 0, bearing_w/2) axis (0,0,1) new -> body Shim0
cylinder Shim0Hole radius axle_clear height (shim + 1 mm) base (0, 0, bearing_w/2 - 0.5 mm) axis (0,0,1) cut targets Shim0
cylinder Shim1Ring radius 3.3 mm height shim base (0, 0, -bearing_w/2 - shim) axis (0,0,1) new -> body Shim1
cylinder Shim1Hole radius axle_clear height (shim + 1 mm) base (0, 0, -bearing_w/2 - shim - 0.5 mm) axis (0,0,1) cut targets Shim1

// Bought parts, for fit only (left off the print plate)
cylinder RefBearingOuter radius (bearing_od/2) height bearing_w base (0, 0, -bearing_w/2) axis (0,0,1) new -> body RefBearing
cylinder RefBearingBore radius (bearing_id/2) height (bearing_w + 1 mm) base (0, 0, -bearing_w/2 - 0.5 mm) axis (0,0,1) cut targets RefBearing
cylinder RefBoltShank radius 2.5 mm height bolt_l base (0, 0, face_z + half_w - head_t - bolt_l) axis (0,0,1) new -> body RefBolt

check Half0 clearance Half1 >= (2*face_z - 0.01 mm)
check Half0 clearance RefBearing >= (shim - 0.01 mm)
check Half1 clearance RefBearing >= (shim - 0.01 mm)

color Half0, Half1 "#2f80ed"
color Shim0, Shim1 "#f2f2f2"
color RefBearing, RefBolt "#9aa0a6"
material Half0, Half1, Shim0, Shim1 PLA
material RefBearing, RefBolt Steel
