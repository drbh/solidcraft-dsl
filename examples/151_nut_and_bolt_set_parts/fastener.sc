// One bolt and its nut for 151_nut_and_bolt_set, sized from the ISO metric coarse table by `d`.
//
// The bolt stands on its head at the origin (head on the bed, shaft up), the nut lies flat at
// x = nut_x. `head` picks the bolt head: 1 a hex head (ISO 4017 proportions), 2 a socket cap
// (ISO 4762) whose hex socket ends in a 120 deg drill point, so its ceiling needs no bridge.
design Fastener units mm

param d = 10 mm              // nominal size: 8, 10, 12, 16 or 20 mm
param length = 30 mm         // under the head
param head = 1               // 1 hex head, 2 socket cap
param thread_play = 0.2 mm   // shaft turned under the major diameter, nut drilled over the minor
param thread_clear = 0.25 mm // flank play, made by scaling the nut's thread (as in 49 and 85)
param nut_x = 30 mm

// The table: pitch, width across flats (head and nut), head height, nut height, socket key
if d == 8 mm {
  let pitch = 1.25 mm
  let af = 13 mm
  let k_hex = 5.3 mm
  let nut_m = 6.8 mm
  let key = 6 mm
}
if d == 10 mm {
  let pitch = 1.5 mm
  let af = 16 mm
  let k_hex = 6.4 mm
  let nut_m = 8.4 mm
  let key = 8 mm
}
if d == 12 mm {
  let pitch = 1.75 mm
  let af = 18 mm
  let k_hex = 7.5 mm
  let nut_m = 10.8 mm
  let key = 10 mm
}
if d == 16 mm {
  let pitch = 2 mm
  let af = 24 mm
  let k_hex = 10 mm
  let nut_m = 14.8 mm
  let key = 14 mm
}
if d == 20 mm {
  let pitch = 2.5 mm
  let af = 30 mm
  let k_hex = 12.5 mm
  let nut_m = 18 mm
  let key = 17 mm
}

let minor_int = d - 1.0825*pitch          // the nut's minor (ISO basic D1)
let hex_r = af/sqrt(3)                    // corner radius of a hex `af` across flats
let r_shaft = d/2 - thread_play
let tip = 0.8*pitch                       // lead-in chamfer at the shaft's end
// The nut's thread `clearance` scales the whole nut out about its axis by (d/2 + clear)/(d/2): on
// a small thread that is 5 %, enough to lose most of the engagement and a spanner size. So the
// nut's hex is drawn that much smaller and comes out at `af` across flats. Its bore is drilled
// 0.1 mm under the minor radius, so the thread's crests are cut clean (a bore on the minor leaves
// near-coincident faces the engine can't close); scaled, it ends about 0.1 mm over the minor.
let ns = (d/2)/(d/2 + thread_clear)
let bore_r = minor_int/2 - 0.1 mm
let nut_r = hex_r*ns
let nut_af = af*ns

// ==== Bolt head
if head == 1 {
  let k = k_hex
  sketch HeadSk on XY {
    polygon h = (0, 0) radius hex_r sides 6
    fix h
  }
  extrude Head from HeadSk distance k new -> body Bolt
  // The bearing face is on the bed; a 30 deg chamfer round the top corners, as on a forged head
  sketch HeadChamferSk on XZ {
    polyline closed c = (af/2 - 0.2 mm, -(k + 0.1 mm)) (hex_r + 1 mm, -(k + 0.1 mm)) (hex_r + 1 mm, -(k - (hex_r + 1 mm - af/2 + 0.2 mm)*tan(30 deg)))
    fix c
  }
  revolve HeadChamfer from HeadChamferSk axis Z cut targets Bolt
}
if head == 2 {
  let k = d                  // socket caps are one diameter tall
  cylinder Head diameter (1.5*d + 1 mm) height k base (0, 0, 0) axis Z new -> body Bolt
  chamfer HeadTop edges ((1.5*d + 1 mm)/2, 0, k) distance 0.6 mm body Bolt
  sketch SocketSk on XY offset (-0.1 mm) {
    polygon s = (0, 0) radius ((key + 0.15 mm)/sqrt(3)) sides 6
    fix s
  }
  extrude Socket from SocketSk distance (0.5*d + 0.1 mm) cut targets Bolt
  // The socket's end: a 120 deg drill point on the socket's flats, so the hole closes with only
  // small ledges in its corners and a 0.8 mm flat at the tip instead of a flat bridge
  cone SocketPoint radius (key/2) top_radius 0.4 mm height ((key/2 - 0.4 mm)*tan(30 deg)) base (0, 0, 0.5*d - 0.05 mm) axis Z cut targets Bolt
}

// ==== Shaft, threaded full length, with a lead-in chamfer
cylinder Shaft radius r_shaft height (length + 0.2 mm) base (0, 0, k - 0.2 mm) axis Z join targets Bolt
chamfer Tip edges (r_shaft, 0, k + length) distance tip body Bolt

// ==== Nut, flat at nut_x, both ends of its bore countersunk to the major diameter
sketch NutSk on XY {
  polygon n = (nut_x, 0) radius nut_r sides 6
  fix n
}
extrude Nut from NutSk distance nut_m new -> body Nut
cylinder NutBore radius bore_r height (nut_m + 2 mm) base (nut_x, 0, -1 mm) axis Z cut targets Nut
chamfer NutLeadIn edges (nut_x + bore_r, 0, 0) (nut_x + bore_r, 0, nut_m) distance (d/2 + 0.3 mm - bore_r) body Nut

material Bolt, Nut PLA

// Threads last: they are the slow features
if d == 8 mm {
  thread BoltThread face (r_shaft, 0, k + length/2) designation "M8x1.25" modeled
  thread NutThread face (nut_x + bore_r, 0, nut_m/2) designation "M8x1.25" modeled clearance thread_clear internal axis Z origin (nut_x, 0, 0) body Nut
}
if d == 10 mm {
  thread BoltThread face (r_shaft, 0, k + length/2) designation "M10x1.5" modeled
  thread NutThread face (nut_x + bore_r, 0, nut_m/2) designation "M10x1.5" modeled clearance thread_clear internal axis Z origin (nut_x, 0, 0) body Nut
}
if d == 12 mm {
  thread BoltThread face (r_shaft, 0, k + length/2) designation "M12x1.75" modeled
  thread NutThread face (nut_x + bore_r, 0, nut_m/2) designation "M12x1.75" modeled clearance thread_clear internal axis Z origin (nut_x, 0, 0) body Nut
}
if d == 16 mm {
  thread BoltThread face (r_shaft, 0, k + length/2) designation "M16x2" modeled
  thread NutThread face (nut_x + bore_r, 0, nut_m/2) designation "M16x2" modeled clearance thread_clear internal axis Z origin (nut_x, 0, 0) body Nut
}
if d == 20 mm {
  thread BoltThread face (r_shaft, 0, k + length/2) designation "M20x2.5" modeled
  thread NutThread face (nut_x + bore_r, 0, nut_m/2) designation "M20x2.5" modeled clearance thread_clear internal axis Z origin (nut_x, 0, 0) body Nut
}


// The nut's outer chamfers last: cut before the thread they upset the thread's boolean on some sizes
sketch NutChamferSk on XZ {
  construction line ax = (nut_x, 0) (nut_x, -10 mm)
  polyline closed c = (nut_x + nut_af/2 - 0.2 mm, -(nut_m + 0.1 mm)) (nut_x + nut_r + 1 mm, -(nut_m + 0.1 mm)) (nut_x + nut_r + 1 mm, -(nut_m - (nut_r + 1 mm - nut_af/2 + 0.2 mm)*tan(30 deg)))
  polyline closed b = (nut_x + nut_af/2 - 0.2 mm, 0.1 mm) (nut_x + nut_r + 1 mm, 0.1 mm) (nut_x + nut_r + 1 mm, -((nut_r + 1 mm - nut_af/2 + 0.2 mm)*tan(30 deg)))
  fix c b ax
}
revolve NutChamfer from NutChamferSk axis NutChamferSk.ax cut targets Nut
