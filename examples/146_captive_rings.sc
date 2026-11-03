// 146. Captive rings spinner: a print-in-place fidget of nested rings that spin freely and can't come apart.
//
// Z is up from the bed and the toy is centred on the origin. A solid core sits inside `rings`
// nested rings, all printed together in one go. Each pair of neighbours meets at a chevron: the
// inner one's outer face bulges out to a point at mid height and the outer one's inner face is
// notched to match, `clr` wider all round. So each ring is trapped above and below by the one
// inside it but turns freely on it. The chevron faces are at 45 deg (depth = height/2), so
// every overhang prints without supports.
//
// The gap is `clr` measured across (radially); square to the 45 deg faces it is clr*0.71. The
// default 0.45 mm gives about 0.32 mm, which frees reliably with a 0.4 mm nozzle at 0.2 mm
// layers. Tighter than 0.35 mm and the rings may fuse; the tolerance coupon (46) is a quick test.
//
// The core has a shallow thumb dimple on top. Spin the outer ring between finger and thumb while
// holding the core, or flick any ring.
//
// Printing: flat on the bed as modelled, one plate, PLA, 0.2 mm layers, 3 perimeters, no
// supports, no brim (a brim can bridge the gaps at the first layer; use elephant-foot
// compensation if your slicer has it). After printing, twist each ring against its neighbour to
// break any first-layer strands.
design CaptiveRings units mm

param rings = 5                // nested rings around the core (1 to 8)
param ring_w = 6 mm            // radial width of each ring at its rim
param height = 6 mm            // print height; the chevron is height/2 deep
param clr = 0.45 mm            // radial clearance between neighbours
param core_r = 8 mm            // core radius to the root of its chevron
param edge = 0.6 mm            // chamfer on the outer ring's rims
param dimple = 1 mm            // thumb dimple depth in the core's top

let d = height/2               // chevron depth (45 deg faces)
let p = ring_w + clr           // radial pitch from one interface to the next
// Interface j (between ring j-1 and ring j, ring 0 the core) has its gap centred at core_r + clr/2 + (j-1)*p
let r_out = core_r + rings*p   // outer radius of the outer ring

// ==== Core: a disc with the first chevron round its edge and a thumb dimple on top
sketch CoreSk on XZ {
  polyline closed o = (0, 0) (core_r, 0) (core_r + d, -d) (core_r, -height) (0, -height)
  fix o
}
revolve CoreRev from CoreSk axis Z new -> body Core
sphere Dimple radius 16 mm center (0, 0, height + 16 mm - dimple) cut targets Core

// ==== Rings: notched inside to the chevron of the one within, bulged outside for the next
for k from 1 to 8 {
  if k < rings {
    sketch Ring{k}Sk on XZ {
      polyline closed o = (core_r + clr + (k-1)*p, 0) (core_r + (k-1)*p + ring_w + clr, 0) (core_r + (k-1)*p + ring_w + clr + d, -d) (core_r + (k-1)*p + ring_w + clr, -height) (core_r + clr + (k-1)*p, -height) (core_r + clr + (k-1)*p + d, -d)
      fix o
    }
    revolve Ring{k}Rev from Ring{k}Sk axis Z new -> body Ring{k}
  }
  // The outer ring has no neighbour: a plain rim, chamfered top and bottom
  if k == rings {
    sketch Ring{k}Sk on XZ {
      polyline closed o = (core_r + clr + (k-1)*p, 0) (r_out - edge, 0) (r_out, -edge) (r_out, -(height - edge)) (r_out - edge, -height) (core_r + clr + (k-1)*p, -height) (core_r + clr + (k-1)*p + d, -d)
      fix o
    }
    revolve Ring{k}Rev from Ring{k}Sk axis Z new -> body Ring{k}
  }
  if k <= rings {
    material Ring{k} PLA
    color Ring{k} "#2a6fdb"
  }
}

// ==== Checks: each ring clears the one inside it (radial clr is clr*0.71 square to the faces)
check Ring1 clearance Core >= (clr*0.7)
for a from 1 to 7 {
  for b from 2 to 8 {
    if b == a + 1 {
      if b <= rings {
        check Ring{b} clearance Ring{a} >= (clr*0.7)
      }
    }
  }
}
check Core size.z >= (height - dimple - 0.01 mm)
check Core size.z <= (height + 0.01 mm)

color Core "#ff7a3d"
material Core PLA
