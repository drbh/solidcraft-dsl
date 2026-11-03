// 140. Printable bench vise, inspired by the popular printed vises: an M16x2 printed lead screw in a
// printed nut, a moving jaw on a 45 deg dovetail rail, swappable magnetic jaw pads with V grooves,
// and a base that screws down to the bench.
//
// X runs along the screw, from the fixed jaw at the back (x = 0) to the nut block at the front;
// Y across the jaws (0 in the middle), Z up (0 under the base). `open` is the gap between the pads,
// 0 to open_max; the moving jaw, its pad, the screw and the handle are placed for it.
//
// How it works. The screw runs in a hex Nut dropped into a pocket in the nut block (the nut is a
// separate print, so a worn one is replaced, and the block takes no thread). Its tip has a neck and
// a collar that drop into a keyhole slot from the moving jaw's top, so the screw both pushes and
// pulls the jaw. The jaw rides on a dovetail rail with `clear` all round, which keeps it square and
// stops it lifting. The screw's shank is long enough to stay in the nut over the whole travel.
// Each pad is held on its jaw by two 8 x 3 mm disc magnets (four in the jaws, four in the pads,
// glued in their pockets), so smooth, soft (TPU) or grooved pads swap in seconds; these have a
// horizontal and a vertical V groove for round stock.
//
// Thread fit, as 49_bottle_and_cap: the screw is turned thread_play under size and the nut bored
// thread_play over the M16x2 minor diameter, and the nut's thread is opened by thread_clear. The
// two threads are placed in an arbitrary phase, so their flanks overlap in the model; that pair is
// not checked (see docs/engine-performance.md on overlapping threads). Every other pair is.
//
// Bill of materials: 8 disc magnets 8 x 3 mm; 4 wood screws or M5 bolts, 5 mm, for the base; glue.
//
// Print: Base upright (the nut pocket and slots open upward; the 17 mm screw bore is a short
// bridge). MovingJaw upright: the dovetail's 45 deg flanks need no support. Screw standing on its
// knob: the collar is a 45 deg cone, its flat face (which takes the clamping thrust) on top. Nut on
// its end, thread upright. Pads flat on their backs. Handle on its side; its knob is a separate print glued on.
design BenchVise units mm

// Jaws
param jaw_w = 70 mm
param jaw_h = 32 mm            // above the base's top
param fj_t = 16 mm             // fixed jaw thickness
param mj_t = 24 mm             // moving jaw thickness
param pad_t = 5 mm
param open = 30 mm
param open_max = 60 mm

// Base
param base_t = 10 mm
param base_w = 100 mm
param gus = 18 mm              // gussets behind the fixed jaw
param nb_t = 20 mm             // nut block thickness
param mount_d = 5.5 mm

// Rail and fits
param rail_wb = 16 mm          // dovetail width at the base
param rail_h = 4 mm
param clear = 0.3 mm

// Screw (M16x2)
param thread_play = 0.2 mm
param thread_clear = 0.2 mm
param neck_r = 5 mm
param collar_r = 9 mm
param knob_r = 13 mm
param knob_l = 18 mm
param bar_r = 4 mm
param bar_l = 100 mm

// Nut
param nut_af = 26 mm           // across flats

// Magnets
param mag_d = 8 mm
param mag_t = 3 mm

let major = 16 mm
let minor_int = 13.835 mm
let rs = major/2 - thread_play
let z_s = base_t + 20 mm                 // screw axis
let top = base_t + jaw_h
let xm0 = fj_t + 2*pad_t + open          // moving jaw's back face
let xn0 = fj_t + 2*pad_t + open_max + mj_t + 4 mm
let L = xn0 + nb_t
let rail_wt = rail_wb + 2*rail_h          // 45 deg flanks
let yf0 = rail_wb/2 + clear*sqrt(2)
let yf1 = yf0 + rail_h + clear
let shank_l = open_max + nb_t + 4 mm
let xs0 = xm0 + mj_t + 1 mm               // shank start
let xk0 = xs0 + shank_l                   // knob start
// The nut's thread `clearance` scales the whole nut out about the screw axis by (major/2 +
// thread_clear)/(major/2), so it is drawn that much smaller and comes out nut_af across
let nut_k = major/(major + 2*thread_clear)
let nut_R = nut_af/sqrt(3)
let nut_af_d = nut_af*nut_k
let nut_R_d = nut_R*nut_k
let nut_Rc = nut_R + clear/cos(30 deg)
let pad_z0 = base_t + rail_h + 0.5 mm
let mag_z = (pad_z0 + top)/2
let mag_y = 18 mm

// ==== Base: plate, fixed jaw, gussets, rail, nut block
box Plate size (L + gus) base_w base_t corner (-gus, -base_w/2, 0) new -> body Base
box FixedJaw size fj_t jaw_w (jaw_h + 0.5 mm) corner (0, -jaw_w/2, base_t - 0.5 mm) join targets Base
for i from 0 to 1 {
  sketch GussetSk{i} on XZ offset ((2*i - 1)*(jaw_w/2 - 8 mm) - 3 mm) {
    polyline closed g = (0.5 mm, -(top - 4 mm)) (0.5 mm, -(base_t - 0.5 mm)) (-gus + 3 mm, -(base_t - 0.5 mm))
    fix g
  }
  extrude Gusset{i} from GussetSk{i} distance 6 mm join targets Base
}
sketch RailSk on YZ offset (fj_t - 0.5 mm) {
  polyline closed r = (-(rail_wb/2 - 0.2 mm), base_t - 0.2 mm) (rail_wb/2 - 0.2 mm, base_t - 0.2 mm) (rail_wt/2, base_t + rail_h) (-rail_wt/2, base_t + rail_h)
  fix r
}
extrude Rail from RailSk distance (xn0 - fj_t + 1 mm) join targets Base
box NutBlock size nb_t (nut_af + 14 mm) (z_s + nut_R + 4 mm - base_t + 0.5 mm) corner (xn0, -(nut_af + 14 mm)/2, base_t - 0.5 mm) join targets Base
// The nut's pocket: the hex (corners up and down) grown by clear, open to the top
sketch NutPocketSk on YZ offset (xn0 + 3 mm - clear) {
  polyline closed p = (-(nut_af/2 + clear), z_s + nut_R + 5 mm) (nut_af/2 + clear, z_s + nut_R + 5 mm) (nut_af/2 + clear, z_s - nut_Rc/2) (0, z_s - nut_Rc) (-(nut_af/2 + clear), z_s - nut_Rc/2)
  fix p
}
extrude NutPocket from NutPocketSk distance (nb_t - 6 mm + 2*clear) cut targets Base
cylinder ScrewBore radius (major/2 + 0.5 mm) height (nb_t + 2 mm) base (xn0 - 1 mm, 0, z_s) axis X cut targets Base
// Magnet pockets in the fixed jaw's face, mounting holes with counterbores
for i from 0 to 1 {
  cylinder FixedMag{i} diameter (mag_d + 0.2 mm) height (mag_t + 1.2 mm) base (fj_t - mag_t - 0.2 mm, (2*i - 1)*mag_y, mag_z) axis X cut targets Base
}
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder Mount{i}{j} diameter mount_d height (base_t + 2 mm) base (-gus + 8 mm + i*(L + gus - 16 mm), (2*j - 1)*(base_w/2 - 8 mm), -1 mm) axis Z cut targets Base
    cylinder MountCb{i}{j} diameter (2*mount_d) height 4 mm base (-gus + 8 mm + i*(L + gus - 16 mm), (2*j - 1)*(base_w/2 - 8 mm), base_t - 3.99 mm) axis Z cut targets Base
  }
}

// ==== Moving jaw: block, dovetail slot, keyhole for the screw's collar and neck, magnet pockets
box MovingBlock size mj_t jaw_w (jaw_h - clear) corner (xm0, -jaw_w/2, base_t + clear) new -> body MovingJaw
sketch SlotSk on YZ offset (xm0 - 1 mm) {
  polyline closed s = (-yf0, base_t) (yf0, base_t) (yf1, base_t + rail_h + clear) (-yf1, base_t + rail_h + clear)
  fix s
}
extrude Slot from SlotSk distance (mj_t + 2 mm) cut targets MovingJaw
box CollarSlot size (4 mm + 2*clear) (2*collar_r + 2*clear) (top - z_s + collar_r + clear + 1 mm) corner (xm0 + 6 mm - clear, -collar_r - clear, z_s - collar_r - clear) cut targets MovingJaw
box NeckSlot size (mj_t - 10 mm + 1 mm) (2*neck_r + 2*clear) (top - z_s + neck_r + clear + 1 mm) corner (xm0 + 10 mm, -neck_r - clear, z_s - neck_r - clear) cut targets MovingJaw
for i from 0 to 1 {
  cylinder MovingMag{i} diameter (mag_d + 0.2 mm) height (mag_t + 1.2 mm) base (xm0 - 1 mm, (2*i - 1)*mag_y, mag_z) axis X cut targets MovingJaw
}

// ==== Pads: the same part on each jaw, magnet pockets behind, V grooves in front
box FixedPadPlate size pad_t jaw_w (top - pad_z0) corner (fj_t, -jaw_w/2, pad_z0) new -> body FixedPad
box MovingPadPlate size pad_t jaw_w (top - pad_z0) corner (xm0 - pad_t, -jaw_w/2, pad_z0) new -> body MovingPad
for i from 0 to 1 {
  cylinder FixedPadMag{i} diameter (mag_d + 0.2 mm) height (mag_t + 1.2 mm) base (fj_t - 1 mm, (2*i - 1)*mag_y, mag_z) axis X cut targets FixedPad
  cylinder MovingPadMag{i} diameter (mag_d + 0.2 mm) height (mag_t + 1.2 mm) base (xm0 - mag_t - 0.2 mm, (2*i - 1)*mag_y, mag_z) axis X cut targets MovingPad
}
// V grooves: horizontal (XZ sketch, v = -z, extruded along +Y) and vertical (XY sketch, along +Z)
sketch FixedVhSk on XZ offset (-jaw_w/2 - 1 mm) {
  polyline closed v = (fj_t + pad_t + 0.5 mm, -(mag_z + 3 mm)) (fj_t + pad_t - 2.5 mm, -mag_z) (fj_t + pad_t + 0.5 mm, -(mag_z - 3 mm))
  fix v
}
extrude FixedVh from FixedVhSk distance (jaw_w + 2 mm) cut targets FixedPad
sketch FixedVvSk on XY offset (pad_z0 - 1 mm) {
  polyline closed v = (fj_t + pad_t + 0.5 mm, -3 mm) (fj_t + pad_t - 2.5 mm, 0) (fj_t + pad_t + 0.5 mm, 3 mm)
  fix v
}
extrude FixedVv from FixedVvSk distance (top - pad_z0 + 2 mm) cut targets FixedPad
sketch MovingVhSk on XZ offset (-jaw_w/2 - 1 mm) {
  polyline closed v = (xm0 - pad_t - 0.5 mm, -(mag_z + 3 mm)) (xm0 - pad_t + 2.5 mm, -mag_z) (xm0 - pad_t - 0.5 mm, -(mag_z - 3 mm))
  fix v
}
extrude MovingVh from MovingVhSk distance (jaw_w + 2 mm) cut targets MovingPad
sketch MovingVvSk on XY offset (pad_z0 - 1 mm) {
  polyline closed v = (xm0 - pad_t - 0.5 mm, -3 mm) (xm0 - pad_t + 2.5 mm, 0) (xm0 - pad_t - 0.5 mm, 3 mm)
  fix v
}
extrude MovingVv from MovingVvSk distance (top - pad_z0 + 2 mm) cut targets MovingPad

// ==== Screw: a conical collar (flat face back, toward the jaws), neck, threaded shank, knob with a cross hole
cone Collar radius collar_r top_radius (neck_r - 1 mm) height (collar_r - neck_r + 1 mm) base (xm0 + 6 mm, 0, z_s) axis X new -> body Screw
cylinder Neck radius neck_r height (xs0 - xm0 - 9.5 mm + 0.5 mm) base (xm0 + 9.5 mm, 0, z_s) axis X join targets Screw
cylinder Shank radius rs height shank_l base (xs0, 0, z_s) axis X join targets Screw
cylinder Knob radius knob_r height knob_l base (xk0 - 0.5 mm, 0, z_s) axis X join targets Screw
chamfer KnobEnd edges (xk0 - 0.5 mm + knob_l, knob_r, z_s) distance 1.5 mm body Screw
cylinder BarHole radius (bar_r + 0.2 mm) height (2*knob_r + 2 mm) base (xk0 + knob_l/2, -knob_r - 1 mm, z_s) axis Y cut targets Screw

// ==== Handle: a bar through the knob, a ball at one end and a glued-on ball at the other
cylinder Bar radius bar_r height bar_l base (xk0 + knob_l/2, -bar_l/2, z_s) axis Y new -> body Handle
sphere BarEnd radius 7 mm center (xk0 + knob_l/2, bar_l/2 + 4 mm, z_s) join targets Handle
sphere CapBall radius 7 mm center (xk0 + knob_l/2, -bar_l/2 - 1 mm, z_s) new -> body HandleCap
cylinder CapBore radius (bar_r + 0.05 mm) height 12 mm base (xk0 + knob_l/2, -bar_l/2 - 5 mm, z_s) axis Y cut targets HandleCap

// ==== Nut: hex with corners up and down, bored, then threaded
sketch NutSk on YZ offset (xn0 + 3 mm) {
  polyline closed h = (0, z_s + nut_R_d) (-nut_af_d/2, z_s + nut_R_d/2) (-nut_af_d/2, z_s - nut_R_d/2) (0, z_s - nut_R_d) (nut_af_d/2, z_s - nut_R_d/2) (nut_af_d/2, z_s + nut_R_d/2)
  fix h
}
extrude NutBody from NutSk distance (nb_t - 6 mm) new -> body Nut
cylinder NutBore diameter (minor_int + 2*thread_play) height (nb_t - 4 mm) base (xn0 + 2 mm, 0, z_s) axis X cut targets Nut

// Threads last
thread ScrewThread face (xs0 + shank_l/2, 0, z_s + rs) designation M16x2 modeled
thread NutThread face (xn0 + nb_t/2, 0, z_s + minor_int/2 + thread_play) designation M16x2 modeled clearance thread_clear internal axis X origin (0, 0, z_s) body Nut

// ==== Checks (the screw and nut threads are the one pair not checked, see above)
check MovingJaw clearance Base >= (clear - 0.05 mm)
check Screw clearance MovingJaw >= (clear - 0.05 mm)
check Screw clearance Base >= 0.3 mm
check Nut clearance Base >= (clear - 0.05 mm)
check Handle clearance Screw >= 0.15 mm
check HandleCap clearance Handle >= 0 mm
check FixedPad clearance Base >= 0 mm
check MovingPad clearance MovingJaw >= 0 mm
check MovingPad clearance Base >= 0.3 mm
check MovingPad clearance FixedPad >= (open - 0.01 mm)
check Base size.x <= 250 mm

color Base "#2f5d8a"
color MovingJaw "#2f5d8a"
color FixedPad, MovingPad "#f2c14e"
color Screw, Nut "#d9d9d9"
color Handle, HandleCap "#c0392b"
material Base, MovingJaw, FixedPad, MovingPad, Screw, Nut, Handle, HandleCap PLA
