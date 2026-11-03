// 85. Mason jar with a two-piece screw lid: a threaded band and a flat disc, printable without supports.
//
// Scaled down from a regular-mouth pint (76 mm across, 70 mm mouth) to a 66 mm jar about 100 mm
// tall with an M48x3 neck, so it prints in a few hours; every size is a parameter. Z is up and
// all three parts stand on Z = 0 the way they print: the jar upright at the origin, the band upside
// down beside it (its top lip on the bed) and the disc flat beyond that.
//
// The jar is one revolved wall section: floor, straight side, a 45 deg shoulder, the bead the band
// stops above, and a thick neck that takes the thread while the rest stays thin. As in
// 49_bottle_and_cap, the neck is turned `thread_play` under size and the band's bore drilled
// `thread_play` over the internal minor diameter, and the band's thread `clearance` scales it out
// about its axis to open the flanks by `thread_clear`. Screwed down, the band's lip presses the
// disc onto the jar's rim; the disc's raised ring sits just inside the rim and centres it.
// Checked on engine 0.28 with the band and disc flipped and seated on the neck, turning the band
// about Z: the threads mesh near 240 deg, where finest meshes (ci/mesh-fit.py) find 0.10 mm clear
// and no overlap from 232.5 to 247.5 deg. At the other angles the flanks overlap (53-500 mm3 by
// inspect.interference), as they would on a band screwed to the wrong height. Near the meshing
// angle the engine's exact query doesn't finish in 900 s, as in 49.
// No supports: the shoulder and bead are 45 deg, and the 60 deg ISO flanks print as 30 deg.
design MasonJar units mm

// Jar
param body_d = 66 mm
param body_h = 78 mm
param wall = 2 mm
param shoulder_r = 10 mm
param bead = 1.5 mm           // the ledge under the thread, proud of the thread's crests
param neck_h = 12 mm
param bore_d = 39 mm
param thread_play = 0.2 mm
param thread_clear = 0.25 mm   // radial play between the flanks, made by scaling the band

// M48x3: major 48 mm, internal minor 48 - 1.0825*3 = 44.7525 mm
let major = 48 mm
let minor_int = 44.7525 mm
let R = body_d/2
let rn = major/2 - thread_play
let rbead = rn + bead
let rb = bore_d/2
let z_sh = body_h + (R - rbead)          // top of the shoulder, foot of the bead
let z_neck = z_sh + 2 mm + bead          // the bead, then a 45 deg step in to the neck
let z_top = z_neck + neck_h
let h_in = body_h - wall*(sqrt(2) - 1)   // inner shoulder parallel to the outer one
let z_sh_in = h_in + (R - wall - rb)

// Jar: the wall section revolved about Z (XZ sketch coordinates are (x, -z))
sketch Section on XZ {
  polyline closed w = (0,0) (R,0) (R,-body_h) (rbead,-z_sh) (rbead,-(z_sh + 2 mm)) (rn,-z_neck) (rn,-z_top) (rb,-z_top) (rb,-z_sh_in) (R - wall,-h_in) (R - wall,-wall) (0,-wall)
  fillet w.1.end radius 3 mm                 // the bottom edge
  fillet w.2.end radius shoulder_r           // the shoulder, outside and in, so the wall stays even
  fillet w.9.end radius (shoulder_r - wall)
  fix w
}
revolve Jar from Section axis Z -> body Jar

// Band, upside down at x = band_x: the lip on the bed, the threaded bore open upward
param band_d = 58 mm
param lip_t = 2 mm
param lip_d = 40 mm            // the opening in the lip, smaller than the disc it holds
param flute_r = 1.5 mm
param flutes = 30
let band_x = R + band_d/2 + 10 mm
let band_depth = neck_h
let band_h = lip_t + band_depth
cylinder Band diameter band_d height band_h base (band_x, 0, 0) new -> body Band
cylinder Flute diameter (2*flute_r) height (band_h + 2 mm) base (band_x + band_d/2, 0, -1 mm) cut targets Band
pattern circular Flutes features Flute origin (band_x, 0, 0) dir (0, 0, 1) count flutes
cylinder BandBore diameter (minor_int + 2*thread_play) height (band_depth + 1 mm) base (band_x, 0, lip_t) cut targets Band
cylinder LipOpening diameter lip_d height (lip_t + 2 mm) base (band_x, 0, -1 mm) cut targets Band
chamfer LipEdge edges (band_x + lip_d/2, 0, lip_t) distance 0.6 mm body Band

// Disc: flat, a little under the band's bore, with a ring underneath that centres it in the mouth
param disc_t = 2 mm
param ring_h = 1.2 mm
let disc_d = minor_int - 0.6 mm
let disc_x = band_x + band_d/2 + 10 mm + disc_d/2
cylinder Disc diameter disc_d height disc_t base (disc_x, 0, 0) new -> body Disc
cylinder DiscRing diameter (bore_d - 0.8 mm) height ring_h base (disc_x, 0, disc_t) join targets Disc
cylinder DiscRingHollow diameter (bore_d - 4 mm) height (ring_h + 1 mm) base (disc_x, 0, disc_t) cut targets Disc

color Jar "#9cc9c1"
color Band "#c9a227"
color Disc "#cfd2d4"
material Jar, Band, Disc PLA

// Threads last: they are the slow features, and every later boolean would have to cut through them
thread NeckThread face (rn, 0, z_top - neck_h/2) designation M48x3 modeled
thread BandThread face (band_x + minor_int/2 + thread_play, 0, lip_t + band_depth/2) designation M48x3 modeled clearance thread_clear internal axis Z origin (band_x, 0, 0) body Band
