// 150. Locking ball joint: a ball stud snapped into a slotted collet socket, locked by a threaded collar nut.
//
// Z is up and the joint stands assembled: the Socket's flange on Z = 0, the Stud's ball seated in
// it with its camera plate on top, and the Nut beside them at x = nut_x, as it prints. The Socket
// is a cup whose two crossed slots make four flexible fingers: the ball snaps past the `snap`
// lip, then the Nut screws down the cup's thread until its inner cone meets the fingers' taper
// and squeezes them onto the ball. On the 12 deg taper a turn (2 mm) closes them 0.43 mm: about
// a third of a turn past contact takes up `ball_fit` and the rest grips. Back it off and the
// ball swivels.
//
// `ball_d` picks the size from a small table (16, 20 or 25 mm), which sets the cup's thread: M24x2,
// M28x2 or M33x2, so the cup's walls are the same 2.5 mm at the ball's equator for every size. The
// Nut's thread `clearance` scales it out about its axis (see 49 and 151), so it is drawn that much
// smaller first. When the Nut first touches the taper its underside is `lock_gap` above the
// flange, which is the most it can travel on (three quarters of a turn, 0.32 mm of squeeze).
//
// Mounting: the Stud's plate takes a 1/4-20 camera screw into a hex nut slid in from the side (a
// 7/16 in nut, the common tripod nut); the Socket's flange has three counterbored M4 holes for a
// desk, a bench or an arm.
//
// Print the Socket on its flange and the Nut on its underside, as drawn; the cavity's roof slopes
// at most 30 deg and the Nut's cone is 12 deg off vertical. Print the Stud upside down, plate on
// the bed: a 45 deg collar carries the neck into the ball so its underside needs no supports.
// PETG suits the fingers best; in PLA keep `snap` at 0.5 mm or below.
design LockingBallJoint units mm

param ball_d = 20 mm           // 16, 20 or 25 mm
param ball_fit = 0.15 mm       // radial play round the ball in the cup
param snap = 0.5 mm            // how far the cup's lip reaches in over the ball
param slot_w = 1.2 mm
param taper = 12 deg           // the fingers' outer cone and the Nut's matching inner cone
param lock_gap = 1.5 mm        // under the Nut at first contact: the squeeze travel
param thread_play = 0.2 mm
param thread_clear = 0.25 mm
param nut_x = 60 mm

// The table: the cup's thread
if ball_d == 16 mm {
  let major = 24 mm
  let pitch = 2 mm
}
if ball_d == 20 mm {
  let major = 28 mm
  let pitch = 2 mm
}
if ball_d == 25 mm {
  let major = 33 mm
  let pitch = 2 mm
}

let R = ball_d/2
let Rs = R + ball_fit
let minor_int = major - 1.0825*pitch
let rt = major/2 - thread_play                  // the cup's threaded outside
let flange_t = 5 mm
let floor_t = 3 mm
let zc = flange_t + floor_t + Rs                // the ball's centre
let r_open = R - snap
let h_lip = sqrt(Rs*Rs - r_open*r_open)          // the lip's height above the centre
let z_top = zc + h_lip
let rk0 = minor_int/2 - 0.3 mm                   // the taper's foot, under the Nut's thread crests
let rk1 = rk0 - h_lip*tan(taper)                 // and its top
let flange_r = major/2 + 12 mm
let hole_r = major/2 + 7.5 mm

// ==== Socket: flange, threaded cup, tapered fingers, ball cavity, slots
cylinder Flange radius flange_r height flange_t base (0, 0, 0) axis Z new -> body Socket
cylinder CupThreaded radius rt height (zc - flange_t + 0.1 mm) base (0, 0, flange_t - 0.1 mm) axis Z join targets Socket
// The taper as a revolved ring (its middle is the cavity, cut next), not a cone primitive: a
// cone's sizes can't name `let`s. It starts inside the cup with no upright step at its foot:
// a short upright face there breaks the cup's thread.
sketch TaperSk on XZ {
  polyline closed t = (2 mm, -(zc - 0.1 mm)) (rk0, -(zc - 0.1 mm)) (rk1, -z_top) (2 mm, -z_top)
  fix t
}
revolve CupTaper from TaperSk axis Z join targets Socket
for k from 0 to 2 {
  cylinder Hole{k} diameter 4.5 mm height (flange_t + 2 mm) base (hole_r*cos(k*120 deg + 30 deg), hole_r*sin(k*120 deg + 30 deg), -1 mm) axis Z cut targets Socket
  cylinder Counterbore{k} diameter 8 mm height 3 mm base (hole_r*cos(k*120 deg + 30 deg), hole_r*sin(k*120 deg + 30 deg), flange_t - 3 mm) axis Z cut targets Socket
}

// ==== Nut: one revolved ring, drawn shrunk by ns about its axis (its thread's clearance scales it
// back out), a threaded bore under an inner cone that matches the fingers' taper
let ns = (major/2)/(major/2 + thread_clear)
let nut_ro = major/2 + 6 mm
let t_len = zc - flange_t - lock_gap             // the threaded length, under the cone
let nut_h = z_top - flange_t - lock_gap
let bore_r = minor_int/2 - 0.1 mm                // 0.1 mm under the minor so the crests cut clean (151)
sketch NutSk on XZ {
  construction line ax = (nut_x, 0) (nut_x, -10 mm)
  polyline closed n = (nut_x + bore_r, 0) (nut_x + nut_ro*ns, 0) (nut_x + nut_ro*ns, -nut_h) (nut_x + rk1*ns, -nut_h) (nut_x + rk0*ns, -t_len) (nut_x + bore_r, -t_len)
  fix ax n
}
revolve Nut from NutSk axis NutSk.ax new -> body Nut
for k from 0 to 11 {
  cylinder Flute{k} diameter 3 mm height (nut_h + 2 mm) base (nut_x + nut_ro*ns*cos(k*30 deg), nut_ro*ns*sin(k*30 deg), -1 mm) axis Z cut targets Nut
}
chamfer NutLeadIn edges (nut_x + bore_r, 0, 0) distance 0.8 mm body Nut

// ==== Stud: ball, a 45 deg collar, neck and the camera plate with its side-loaded 1/4-20 nut
let neck_r = 0.25*ball_d
let collar_r = 0.7*R                             // where the collar meets the ball, inside its surface
let z_collar = zc + collar_r
let z_plate = zc + R + 6 mm
let plate_r = R + 7 mm
let plate_t = 9 mm
let tn_af = 11.4 mm                              // 7/16 in across flats, plus play
let tn_t = 5.9 mm
sphere Ball radius R center (0, 0, zc) new -> body Stud
sketch CollarSk on XZ {
  polyline closed c = (1 mm, -z_collar) (collar_r, -z_collar) (neck_r - 0.3 mm, -(z_collar + collar_r - neck_r + 0.3 mm)) (1 mm, -(z_collar + collar_r - neck_r + 0.3 mm))
  fix c
}
revolve Collar from CollarSk axis Z join targets Stud
cylinder Neck radius neck_r height (z_plate - zc + 0.5 mm) base (0, 0, zc) axis Z join targets Stud
cylinder Plate radius plate_r height plate_t base (0, 0, z_plate) axis Z join targets Stud
chamfer PlateTop edges (plate_r, 0, z_plate + plate_t) distance 1 mm body Stud
// The nut's pocket and the slot it slides in by, as one outline: half a hex, then straight out to +Y
sketch TripodNutSk on XY offset (z_plate + 1.5 mm) {
  polyline closed n = (tn_af/2, -tn_af/(2*sqrt(3))) (0, -tn_af/sqrt(3)) (-tn_af/2, -tn_af/(2*sqrt(3))) (-tn_af/2, plate_r + 1 mm) (tn_af/2, plate_r + 1 mm)
  fix n
}
extrude TripodNut from TripodNutSk distance tn_t cut targets Stud
cylinder ScrewHole diameter 6.8 mm height (plate_t + 6 mm) base (0, 0, z_plate - 5 mm) axis Z cut targets Stud

// ==== Fits (assembled, ball upright)
check Stud clearance Socket >= (ball_fit - 0.03 mm)
check Nut clearance Socket >= 5 mm
check Socket size.z >= (z_top - 0.01 mm)
check Stud size.z >= (2*R + 6 mm + plate_t - 0.01 mm)

color Socket "#2f3338"
color Nut "#d8792a"
color Stud "#c9cdd2"
material Socket, Nut, Stud PLA

// ==== Threads last, then the cavity and the slots through them
if ball_d == 16 mm {
  thread CupThread face (rt, 0, (flange_t + zc)/2) designation M24x2 modeled
  thread NutThread face (nut_x + bore_r, 0, t_len/2) designation M24x2 modeled clearance thread_clear internal axis Z origin (nut_x, 0, 0) body Nut
}
if ball_d == 20 mm {
  thread CupThread face (rt, 0, (flange_t + zc)/2) designation M28x2 modeled
  thread NutThread face (nut_x + bore_r, 0, t_len/2) designation M28x2 modeled clearance thread_clear internal axis Z origin (nut_x, 0, 0) body Nut
}
if ball_d == 25 mm {
  thread CupThread face (rt, 0, (flange_t + zc)/2) designation M33x2 modeled
  thread NutThread face (nut_x + bore_r, 0, t_len/2) designation M33x2 modeled clearance thread_clear internal axis Z origin (nut_x, 0, 0) body Nut
}
// The ball's cavity after the thread: cut before it, the sphere's facets push the thread's boolean
// past the engine's limit on the 25 mm size ("tree too deep")
sphere Cavity radius Rs center (0, 0, zc) cut targets Socket
chamfer LipEntry edges (r_open, 0, z_top) distance 0.4 mm body Socket
// Four fingers: two slots across the cup, from 0.7 R under the ball's centre up through the lip
box SlotX size (2*major) slot_w (z_top - zc + 0.7*R + 1 mm) corner (-major, -slot_w/2, zc - 0.7*R) cut targets Socket
box SlotY size slot_w (2*major) (z_top - zc + 0.7*R + 1 mm) corner (-slot_w/2, -major, zc - 0.7*R) cut targets Socket
