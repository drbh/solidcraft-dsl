// The hanging half of a 45 deg French cleat, shared by every holder in 113_french_cleat_system.
//
// X along the wall (the holder's width, from x = 0), Y out from the wall face (y = 0), Z up. The
// wall cleat is a cleat_t x cleat_h bar whose top bevel falls 45 deg toward the wall: its front top
// edge is at (cleat_t, cleat_h), its back top edge at (0, cleat_h - cleat_t). This piece hooks
// over it: a back plate just in front of the wall cleat, a hook whose underside follows the
// bevel `gap` above it, and a spacer at the bottom of the plate that rests on the wall so the
// holder hangs plumb. Holders are built on the plate's front face (y = cleat_t + gap + plate_t)
// with their lowest part at z = zmin, so each prints standing up: the hook's bevel is a 45 deg
// overhang and everything else stands on the bed.
design Hanger units mm

param w = 60 mm
param zmin = -30 mm
param cleat_t = 12 mm
param cleat_h = 30 mm
param gap = 0.4 mm
param plate_t = 5 mm
param top = 42 mm

let hy0 = cleat_t + gap
let hy1 = hy0 + plate_t

box Plate size w plate_t (top - zmin) corner (0, hy0, zmin) new -> body Hanger
sketch HookSk on YZ offset 0 {
  polyline closed p = (gap, cleat_h - cleat_t + gap + 0.6 mm) (hy0 + 0.3 mm, cleat_h + 0.3 mm + gap + 0.6 mm) (hy0 + 0.3 mm, top - 0.3 mm) (gap, top - 0.3 mm)
  fix p
}
extrude Hook from HookSk distance w join targets Hanger
box Spacer size w (hy0 - gap + 0.3 mm) 10 mm corner (0, gap, zmin) join targets Hanger

color Hanger "#c47b2b"
material Hanger PLA
