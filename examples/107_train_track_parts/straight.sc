// A straight of 107_train_track: socket at the start (x = 0), dogbone peg at the end (x = L).
design TrackStraight units mm

// Where the piece goes: it places itself (a used part can't be moved from outside once its
// own peg and socket helpers are used up), turning a0 about Z, then moving to (x0, y0)
param x0 = 0 mm
param y0 = 0 mm
param a0 = 0 deg

param L = 144 mm
param w = 40 mm
param t = 12 mm
param gw = 6 mm              // groove width
param gd = 3 mm              // groove depth
param gc = 26 mm             // groove centre to centre
param gap = 0.5 mm           // end gap left between neighbouring pieces
param clearance = 0.3 mm
param ph = 8.5 mm            // peg height, kept under the groove floor

box Body size (L - gap) w t corner (gap/2, -w/2, 0) new -> body Straight
box GrooveA size (L + 2 mm) gw (gd + 1 mm) corner (-1 mm, gc/2 - gw/2, t - gd) cut targets Straight
box GrooveB size (L + 2 mm) gw (gd + 1 mm) corner (-1 mm, -gc/2 - gw/2, t - gd) cut targets Straight
use Sock from "dogbone.sc" with (grow = clearance, z0 = -1 mm, h = t + 2 mm) at (-gap/2, 0, 0) -> body SockTool
combine SockCut target Straight tools SockTool cut
use Peg from "dogbone.sc" with (h = ph) at (L - gap/2, 0, 0) join targets Straight
color Straight "#d9b382"
move Turn bodies Straight axis (0,0,1) angle a0
move Place bodies Straight to (x0, y0, 0)
