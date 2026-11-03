// The buffer stop of 107_train_track: a platform at track level `rise` up, with a socket
// for the ramp's peg and a bumper beam with two sprung-looking buffers at the far end.
design TrackBuffer units mm

// Where the piece goes: it places itself (a used part can't be moved from outside once its
// own peg and socket helpers are used up), turning a0 about Z, then moving to (x0, y0)
param x0 = 0 mm
param y0 = 0 mm
param a0 = 0 deg

param bl = 60 mm             // platform length
param rise = 36 mm
param w = 40 mm
param t = 12 mm
param gw = 6 mm
param gd = 3 mm
param gc = 26 mm
param gap = 0.5 mm
param clearance = 0.3 mm

let top = rise + t
box Body size (bl - gap/2) w top corner (gap/2, -w/2, 0) new -> body Buffer
box GrooveA size (bl - 10 mm) gw (gd + 1 mm) corner (-1 mm, gc/2 - gw/2, top - gd) cut targets Buffer
box GrooveB size (bl - 10 mm) gw (gd + 1 mm) corner (-1 mm, -gc/2 - gw/2, top - gd) cut targets Buffer
box Beam size 10 mm w 20 mm corner (bl - 10 mm, -w/2, top - 0.5 mm) join targets Buffer
cylinder BufferA radius 4 mm height 7 mm base (bl - 9.5 mm, gc/2, top + 9.5 mm) axis (-1,0,0) join targets Buffer
cylinder BufferB radius 4 mm height 7 mm base (bl - 9.5 mm, -gc/2, top + 9.5 mm) axis (-1,0,0) join targets Buffer
use Sock from "dogbone.sc" with (grow = clearance, z0 = rise - 0.5 mm, h = t + 1.5 mm) at (-gap/2, 0, 0) -> body SockTool
combine SockCut target Buffer tools SockTool cut
color Buffer "#b8402e"
move Turn bodies Buffer axis (0,0,1) angle a0
move Place bodies Buffer to (x0, y0, 0)
