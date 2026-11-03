// 148. Gravity-locking broom holder: an eccentric cam on a pin grips a handle as its weight pulls down, and lets go when it's lifted.
//
// Z is up, the plate's back is on the wall at y = 0 and Y comes out of the wall. The handle hangs
// between a fixed jaw on the left (its face at x = 0) and an eccentric cam on the right. The cam
// is a disc of radius `Rc` (to its O-ring) turning on a pin `e` off its centre, the pin below
// and to the right of where it touches the handle. Gravity swings the disc's heavy side down onto
// the handle. When the handle's weight drags the contact down, the cam turns the way that
// reaches further, so it wedges harder: the more it pulls, the tighter it holds. Lift the handle
// and the cam turns back, its reach shrinks, and the handle comes out forward.
//
// The cam's reach covers `d_min` to `d_max`. Across that range the disc centre's angle from
// straight up runs from asin(0.9) for the thinnest handle to asin(0.1) for the thickest, so
// e = (d_max - d_min)/0.8. It locks if tan(beta) < mu, where tan(beta) = e*cos/(e*sin + Rc) is the
// lever of the contact force about the pin. With Rc = cam_k*e and cam_k = 1.85 that is 0.51 at the
// thickest handle and less for the rest, so the rim carries a rubber O-ring (mu about 0.7 on wood,
// metal or painted handles). A post under the disc stops it just past the thinnest handle when
// the holder is empty.
//
// To hang a handle, push it straight in. It meets the cam's chamfered front edge, which turns
// the cam up out of the way, and the cam drops back onto it.
//
// Hardware:
//   1 M4 x 25 socket head screw: in from the back before mounting, its head in the plate's counterbore
//   1 M4 washer and 1 M4 nyloc nut: on the sleeve's end, clamping the sleeve, not the cam
//   1 O-ring of cord 3 mm, inner diameter about 2*Rc - 6 mm (40 x 3 for the defaults)
//   2 wood screws, 4 mm countersunk, length to suit the wall
// The cam turns on a printed sleeve: 0.3 mm radial play and 0.4 mm axial play.
//
// The handle shown is a 25 mm pole, standing 0.3 mm off the plate and the jaw. The cam is drawn
// at the angle that leaves 0.4 mm to it, so the checks measure the real gaps.
//
// Printing: two parts, PLA or PETG, no supports. The Holder lies on its back: the jaw, sleeve and
// stop post rise straight up, and the countersinks face up. The Cam lies flat; its groove's
// lower wall is a 1.5 mm horizontal step (a short bridge round the groove).
design GravityBroomHolder units mm

// Handle range
param d_min = 20 mm
param d_max = 30 mm
param d_show = 25 mm           // the pole shown
param cam_k = 1.85             // Rc/e: higher grips more surely, lower turns more per mm of handle

// Plate, jaw and pin
param plate_t = 6 mm
param jaw_w = 10 mm
param sleeve_r = 4 mm
param play = 0.3 mm            // cam bore on the sleeve
param end_play = 0.4 mm
param cam_t = 14 mm
param cord = 3 mm              // O-ring cord
param stop_r = 3 mm

let e = (d_max - d_min)/0.8
let Rc = cam_k*e                       // cam radius to the O-ring's outside
let rr = Rc - cord/3                   // printed rim: the O-ring stands cord/3 proud
let rg = Rc - cord                     // groove floor
let xp = d_min + Rc + 0.9*e            // the pin, from the jaw face; at z = 0
let yg = plate_t + (d_min + d_max)/4   // the O-ring's plane, through a mid-size handle's axis
let y0 = yg - cam_t/2                  // cam back face
let ys = y0 - end_play                 // shoulder end, where the sleeve begins
let ye = y0 + cam_t + end_play         // sleeve end, under the washer

// The pole shown, and the cam angle that leaves 0.4 mm to it
let xc = 0.3 mm + d_show/2
let yc = plate_t + 0.3 mm + d_show/2
let hw = sqrt((d_show/2)^2 - (yg - yc)^2)  // the pole's half width in the O-ring's plane
let s_show = (xp - Rc - (xc + hw + 0.4 mm))/e
let cs_show = sqrt(1 - s_show^2)
let cx = xp - e*s_show
let cz = e*cs_show
// The stop post: under the disc where it sits just past the thinnest handle (sin = 0.95)
let sx = xp - 0.95*e
let sz = e*sqrt(1 - 0.95^2) - Rc - stop_r

// Plate extents
let px0 = -jaw_w - 6 mm
let px1 = xp + Rc + 6 mm
let pz0 = -Rc - 16 mm
let pz1 = e + Rc + 14 mm

// ==== Holder: wall plate, jaw, pin sleeve and stop post, one print
sketch PlateSk on XZ {
  rect rounded o = (px0, -pz1) (px1, -pz0) radius 6 mm
  fix o
}
extrude Plate from PlateSk distance plate_t new -> body Holder
box Jaw size jaw_w (d_max + 6 mm) (e + 35 mm) corner (-jaw_w, plate_t - 0.2 mm, -15 mm) join targets Holder
chamfer JawLead edges (0, plate_t - 0.2 mm + d_max + 6 mm, 5 mm) distance 4 mm body Holder
cylinder Shoulder radius 7 mm height (ys - plate_t + 0.2 mm) base (xp, plate_t - 0.2 mm, 0) axis (0,1,0) join targets Holder
cylinder Sleeve radius sleeve_r height (ye - ys + 0.2 mm) base (xp, ys - 0.2 mm, 0) axis (0,1,0) join targets Holder
cylinder StopPost radius stop_r height (y0 + cam_t - 1 mm - plate_t + 0.2 mm) base (sx, plate_t - 0.2 mm, sz) axis (0,1,0) join targets Holder
// The axle screw: a 4.5 mm bore and a counterbore for its head in the back
cylinder AxleBore radius 2.25 mm height (ye + 2 mm) base (xp, -1 mm, 0) axis (0,1,0) cut targets Holder
cylinder HeadBore radius 3.8 mm height 5.2 mm base (xp, -1 mm, 0) axis (0,1,0) cut targets Holder
// Wall screws, countersunk, above and below the handle
cylinder WallHoleTop radius 2.2 mm height (plate_t + 2 mm) base (xc, -1 mm, pz1 - 8 mm) axis (0,1,0) cut targets Holder
cylinder WallHoleBot radius 2.2 mm height (plate_t + 2 mm) base (xc, -1 mm, pz0 + 8 mm) axis (0,1,0) cut targets Holder
cone SinkTop radius 1.9 mm top_radius 4.4 mm height 2.6 mm base (xc, plate_t - 2.59 mm, pz1 - 8 mm) axis Y cut targets Holder
cone SinkBot radius 1.9 mm top_radius 4.4 mm height 2.6 mm base (xc, plate_t - 2.59 mm, pz0 + 8 mm) axis Y cut targets Holder

// ==== Cam: an eccentric disc with an O-ring groove round its rim, bored for the sleeve
cylinder CamBack radius rr height ((cam_t - cord)/2 - 0.1 mm) base (cx, y0, cz) axis (0,1,0) new -> body Cam
cylinder CamCore radius rg height (cord + 0.6 mm) base (cx, yg - cord/2 - 0.3 mm, cz) axis (0,1,0) join targets Cam
cylinder CamFront radius rr height ((cam_t - cord)/2 - 0.1 mm) base (cx, y0 + cam_t - ((cam_t - cord)/2 - 0.1 mm), cz) axis (0,1,0) join targets Cam
cylinder CamBore radius (sleeve_r + play) height (cam_t + 2 mm) base (xp, y0 - 1 mm, 0) axis (0,1,0) cut targets Cam
chamfer CamLead edges (cx + rr, y0 + cam_t, cz) distance 2.5 mm body Cam
chamfer CamBackEdge edges (cx + rr, y0, cz) distance 0.8 mm body Cam

// The O-ring (bought), seated in the groove
sketch ORingSk on XY offset cz {
  circle c = (cx + Rc - cord/2, yg) diameter cord
  fix c
}
revolve ORing from ORingSk axis_face (cx + rg, yg, cz) new -> body ORing

// The handle shown (not printed)
cylinder Pole radius (d_show/2) height 140 mm base (xc, yc, -60 mm) axis Z new -> body Pole

// ==== Checks
check Pole clearance Holder >= 0.25 mm
check Pole clearance ORing >= 0.3 mm
check Pole clearance Cam >= 0.3 mm
check Cam clearance Holder >= (play - 0.05 mm)
check ORing clearance Holder >= 1 mm
check Holder shells == 1
check Cam shells == 1

color Holder "#e8e4dc"
color Cam "#2b2d31"
color ORing "#111111"
color Pole "#b98a52"
material Holder, Cam PLA
