// 49. Screw-top bottle and cap with real modelled M28x2 threads.
//
// Both parts stand on Z = 0: the bottle upright, the cap upside down beside it (top on the bed,
// open end up), which is how they print. The bottle is one revolved wall section (bottom,
// side wall, a 45 deg shoulder and a thick neck), so the neck is solid enough to take a thread
// while the rest stays thin. The external thread is cut into a neck turned `thread_play` under
// size, and the cap bore is drilled `thread_play` over the M28x2 minor diameter, so the crests
// clear on both sides. The engine cuts the basic ISO profile, so the flanks would touch: the cap
// thread's `clearance` scales the cap out about its axis, which opens its thread by
// `thread_clear` at the thread's radius without changing the pitch. Checked on engine 0.19 by
// screwing the cap onto the neck (1 mm short of the seal) and turning it in 30 deg steps: at the
// angle where the threads mesh, the cap clears the neck by 0.09 mm; at the others the flanks
// overlap. Around 300 deg (295, 300 and 305) the engine's clearance query and intersection don't
// finish, so those angles were measured on finest meshes with ci/mesh-fit.py: the flanks overlap
// there too, 0.49 mm deep (the same check finds 0.088 mm clear at 180 deg). Raise
// `thread_clear` for a looser fit. A ring in the cap seals on the neck's top.
// No supports: the shoulder is 45 deg, and the 60 deg ISO flanks print as a 30 deg overhang.
design BottleAndCap units mm
param body_d = 60 mm
param body_h = 90 mm
param wall = 1.6 mm
param neck_h = 16 mm
param bore_d = 20 mm
param thread_play = 0.2 mm
// Radial play between the flanks, made by scaling the cap (see above)
param thread_clear = 0.2 mm
// M28x2: major 28 mm, minor (internal) 25.835 mm
let major = 28 mm
let minor_int = 25.835 mm
let R = body_d/2
let rn = major/2 - thread_play
let rb = bore_d/2
let z_sh = body_h + (R - rn)
let z_top = z_sh + neck_h
let h_in = body_h - 0.7 mm
let z_sh_in = h_in + (R - wall - rb)

// Bottle: the wall section revolved about Z. (XZ sketch coordinates are (x, -z).)
sketch Section on XZ {
  polyline closed w = (0,0) (R,0) (R,-body_h) (rn,-z_sh) (rn,-z_top) (rb,-z_top) (rb,-z_sh_in) (R - wall,-h_in) (R - wall,-wall) (0,-wall)
  fix w.1 w.2 w.3 w.4 w.5 w.6 w.7 w.8 w.9 w.10
}
revolve Bottle from Section axis Z -> body Bottle

// Cap, upside down at x = cap_x: closed top on the bed, grip flutes around the outside
param cap_d = 34 mm
param cap_top = 2.4 mm
param cap_depth = 14 mm
param flute_r = 1.2 mm
let cap_x = R + cap_d/2 + 10 mm
let cap_h = cap_top + cap_depth
cylinder Cap diameter cap_d height cap_h base (cap_x, 0, 0) new -> body Cap
cylinder Flute diameter (2*flute_r) height (cap_h + 2 mm) base (cap_x + cap_d/2, 0, -1 mm) cut targets Cap
pattern circular Flutes features Flute origin (cap_x, 0, 0) dir (0, 0, 1) count 24
cylinder CapBore diameter (minor_int + 2*thread_play) height (cap_depth + 1 mm) base (cap_x, 0, cap_top) cut targets Cap
// Seal ring: presses on the neck's top face when the cap is tight
let seal_r = (rn + rb)/2
torus Seal major seal_r minor 0.8 mm center (cap_x, 0, cap_top) join targets Cap
// Threads last: they are the slow features, and every later boolean would have to cut through them
thread NeckThread face (rn, 0, z_top - neck_h/2) designation M28x2 modeled
// The cap's thread is opened by thread_clear (an internal thread: the cap is scaled out about its axis)
thread CapThread face (cap_x + minor_int/2 + thread_play, 0, cap_top + cap_depth/2) designation M28x2 modeled clearance thread_clear internal axis Z origin (cap_x, 0, 0) body Cap
