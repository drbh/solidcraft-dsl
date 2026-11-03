// 156. Turbine (cyclone) whistle: one printed piece, a flat round chamber blown tangentially.
//
// Z is up from the bed and the chamber is centred on the origin. Breath enters through a flat
// slot that meets the chamber wall on a tangent, on the +Y side, so the air spins round the
// chamber as a vortex. It leaves through a round outlet at the centre of each face. The vortex
// precesses round the outlets and that is the tone: a smaller chamber spins faster and sounds
// higher. With the defaults (24 mm chamber, 6 mm high) expect a loud tone of about 2-3 kHz.
// Halving `chamber_d` roughly doubles it; the slot and outlets are scaled from `chamber_d` so the
// whistle still speaks when you resize it. Tune by ear, not by formula.
//
// A keyring loop stands off the -Y side, away from the mouthpiece.
//
// Printing: flat on the bed as modelled, PLA or PETG, 0.2 mm layers, no supports. The chamber
// roof is a bridge of chamber_d (24 mm), which prints clean on most printers; the slot roof
// bridges slot_w. Keep the slot crisp: no elephant-foot compensation on the first layers is
// needed, because the slot starts one wall above the bed.
design TurbineWhistle units mm

param chamber_d = 24 mm        // sets the pitch
param chamber_h = 6 mm
param wall = 1.6 mm
param slot_w = (chamber_d*0.12)    // width of the breath slot, across its flow
param mouth_l = 22 mm          // mouthpiece length past the chamber
param outlet_d = (chamber_d*0.3)
param ring_d = 5 mm            // keyring hole

let R = chamber_d/2
let H = chamber_h + 2*wall
let ring_c = -(R + wall + ring_d/2 + 1.2 mm)

// Keyring loop off the -Y side
// (one stadium from the ring's centre into the drum, so it joins in a single boolean)
sketch RingSk on XY {
  slot s = centers (0, ring_c) (0, -R/2) width (ring_d + 4 mm)
  fix s
}
extrude RingLug from RingSk distance (H*0.6) new -> body Whistle

// Body: the chamber drum
cylinder Drum radius (R + wall) height H base (0, 0, 0) axis (0,0,1) join targets Whistle

// Mouthpiece: a flat block running from the chamber along -X, its slot tangent to the wall at +Y
box Mouth size (mouth_l + R) (slot_w + 2*wall) H corner (-(R + mouth_l), R - slot_w - wall, 0) join targets Whistle
fillet MouthEnd edges (-(R + mouth_l), R - slot_w/2, H) (-(R + mouth_l), R - slot_w/2, 0) radius 1 mm body Whistle

cylinder RingHole radius (ring_d/2) height (H + 2 mm) base (0, ring_c, -1 mm) axis (0,0,1) cut targets Whistle

// Chamber and slot as one outline, cut in one go. The slot's outer wall runs 0.3 mm inside the
// chamber's tangent line, so it meets the circle at a shallow angle instead of touching it.
let y_out = R - 0.3 mm
let y_in = R - slot_w
sketch ChamberSk on XY offset wall {
  line outer = (-(R + mouth_l) - 1 mm, y_out) (-sqrt(R^2 - y_out^2), y_out)
  arc wall_arc = start outer.end through (0, -R) end (-sqrt(R^2 - y_in^2), y_in)
  line inner = wall_arc.end (-(R + mouth_l) - 1 mm, y_in)
  line mouth = inner.end outer.start
  fix outer wall_arc inner mouth
}
extrude Chamber from ChamberSk distance chamber_h cut targets Whistle

// Centre outlets, one through each face
cylinder OutletTop radius (outlet_d/2) height (wall + 2 mm) base (0, 0, H - wall - 1 mm) axis (0,0,1) cut targets Whistle
cylinder OutletBottom radius (outlet_d/2) height (wall + 2 mm) base (0, 0, -1 mm) axis (0,0,1) cut targets Whistle

check Whistle size.z == H
check Whistle volume >= 1000 mm3

color Whistle "#2fa4ff"
material Whistle PLA
