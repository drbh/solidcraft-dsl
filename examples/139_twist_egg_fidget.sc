// 139. Twist egg fidget, inspired by the trending print-in-place twist eggs: three nested layers that
// screw out of one another along steep two-start helices, printed in place as one egg.
//
// Z is up, the egg's axis; z = 0 at its equator. The outline is two half-ellipses, a fat bottom
// (b_lo) and a taller top (b_hi), round a 1 mm band at the equator, cut flat at flat_k of the
// widest radius so every layer stands on the bed. The layers are concentric: the Core inside r1,
// the Mid from r1 + gap to r2, the Shell from r2 + gap to the outline. Each inner layer carries two
// square ridges (section `ridge`) on helices of lead `lead`, centred on its outer face; the next
// layer out has the matching grooves, `clear` bigger all round. Twist the core and it climbs out of
// the top in about one turn, then screw it back in; the mid does the same in the shell. The grooves
// run on through the top surface, so a layer can come right out and go back.
//
// The grooves follow exactly the ridges' helices, started below the bed (where they cut nothing):
// one whole lead lower with the same start angle, or a little lower with the start angle wound
// back by the same fraction of a turn, so they are in phase; they run on past the top. The
// starts are off the revolves' seam in the XZ plane: 30 and 210 deg for the core, 75 and 255 deg
// for the mid.
//
// Print it as modelled, all three layers at once, no supports: the outline is never steeper than
// about 32 deg from upright above the flat, and the ridges' and grooves' flat faces are 0.75 to
// 1.1 mm overhangs. Twist each layer free after printing. PLA works; a 0.2 mm layer gives the
// clearances room.
design TwistEgg units mm

param a = 22 mm               // widest radius
param b_lo = 26 mm            // lower half-height
param b_hi = 34 mm            // upper half-height
param eq = 1 mm               // the band at the equator
param flat_k = 0.8            // the flat base, as a fraction of the widest radius

param r1 = 8.5 mm             // Core's radius
param r2 = 15 mm              // Mid's outer radius
param gap = 0.3 mm            // radial gap between layers

param lead = 40 mm            // helix lead (rise per turn)
param ridge = 1.5 mm          // ridge section (square)
param clear = 0.35 mm         // groove over ridge, all round
param margin = 2 mm           // ridges stop this far above the bed

let s_f = sqrt(1 - flat_k^2)
let z_f = -eq/2 - b_lo*s_f                // the bed
let r_f = flat_k*a

// Where the upper outline crosses a radius r: z_hi(r) = eq/2 + b_hi*sqrt(1 - (r/a)^2); angles for the curves
let t_r1 = atan2(r1, sqrt(a^2 - r1^2))
let t_r1g = atan2(r1 + gap, sqrt(a^2 - (r1 + gap)^2))
let t_r2 = atan2(r2, sqrt(a^2 - r2^2))
let t_r2g = atan2(r2 + gap, sqrt(a^2 - (r2 + gap)^2))
let t_f = atan2(flat_k, s_f)

// ==== The three layers, revolved (XZ sketch: u = r, v = -z)
sketch CoreSk on XZ {
  curve top = (a*sin(t*1 deg), -(eq/2 + b_hi*cos(t*1 deg))) t from 0 to (t_r1/(1 deg)) points 16
  line base = (0, -z_f) (r1, -z_f)
  line side = base.end top.end
  line axis = top.start base.start
  fix top base side axis
}
revolve CoreRev from CoreSk axis Z -> body Core

sketch MidSk on XZ {
  curve top = (a*sin(t*1 deg), -(eq/2 + b_hi*cos(t*1 deg))) t from (t_r1g/(1 deg)) to (t_r2/(1 deg)) points 16
  line base = (r1 + gap, -z_f) (r2, -z_f)
  line outer = base.end top.end
  line inner = top.start base.start
  fix top base outer inner
}
revolve MidRev from MidSk axis Z -> body Mid

sketch ShellSk on XZ {
  line base = (r2 + gap, -z_f) (r_f, -z_f)
  curve low = (a*sin(t*1 deg), eq/2 + b_lo*cos(t*1 deg)) t from (t_f/(1 deg)) to 90 points 16
  line band = low.end (a, -eq/2)
  curve top = (a*sin(t*1 deg), -(eq/2 + b_hi*cos(t*1 deg))) t from (t_r2g/(1 deg)) to 90 points 24
  line inner = top.start base.start
  fix base low band top inner
}
revolve ShellRev from ShellSk axis Z -> body Shell

// ==== Ridges: two starts on each inner layer, from `margin` above the bed to where the outline
// is 2.5 mm outside the layer (written out: coil sizes take parameters, not lets)
coil CoreRidgeA diameter (2*r1) turns ((eq + b_hi*sqrt(1 - ((r1 + 2.5 mm)/a)^2) + b_lo*sqrt(1 - flat_k^2) - margin)/lead) pitch lead section_size ridge square section_position center start_angle 30 deg base (0, 0, -eq/2 - b_lo*sqrt(1 - flat_k^2) + margin) join targets Core
coil CoreRidgeB diameter (2*r1) turns ((eq + b_hi*sqrt(1 - ((r1 + 2.5 mm)/a)^2) + b_lo*sqrt(1 - flat_k^2) - margin)/lead) pitch lead section_size ridge square section_position center start_angle 210 deg base (0, 0, -eq/2 - b_lo*sqrt(1 - flat_k^2) + margin) join targets Core
coil MidRidgeA diameter (2*r2) turns ((eq + b_hi*sqrt(1 - ((r2 + 2.5 mm)/a)^2) + b_lo*sqrt(1 - flat_k^2) - margin)/lead) pitch lead section_size ridge square section_position center start_angle 75 deg base (0, 0, -eq/2 - b_lo*sqrt(1 - flat_k^2) + margin) join targets Mid
coil MidRidgeB diameter (2*r2) turns ((eq + b_hi*sqrt(1 - ((r2 + 2.5 mm)/a)^2) + b_lo*sqrt(1 - flat_k^2) - margin)/lead) pitch lead section_size ridge square section_position center start_angle 255 deg base (0, 0, -eq/2 - b_lo*sqrt(1 - flat_k^2) + margin) join targets Mid

// ==== Grooves: the same helices running on out of the top. The mid's start one lead lower, the
// shell's just below the bed with the start angle wound back to match (the engine cuts that
// shorter helix more cleanly). The engine still nudges whichever shell groove it cuts second by
// about 1e-4 mm (a "Boolean retry" warning); each cuts cleanly alone, and the result is sound.
coil MidGrooveA diameter (2*r1) turns (2.6) pitch lead section_size (ridge + 2*clear) square section_position center start_angle 30 deg base (0, 0, -eq/2 - b_lo*sqrt(1 - flat_k^2) + margin - lead) cut targets Mid
coil MidGrooveB diameter (2*r1) turns (2.6) pitch lead section_size (ridge + 2*clear) square section_position center start_angle 210 deg base (0, 0, -eq/2 - b_lo*sqrt(1 - flat_k^2) + margin - lead) cut targets Mid
coil ShellGrooveA diameter (2*r2) turns (1.25) pitch lead section_size (ridge + 2*clear) square section_position center start_angle (75 deg - 360 deg*(margin + 1 mm)/lead) base (0, 0, -eq/2 - b_lo*sqrt(1 - flat_k^2) + margin - margin - 1 mm) cut targets Shell
coil ShellGrooveB diameter (2*r2) turns (1.25) pitch lead section_size (ridge + 2*clear) square section_position center start_angle (255 deg - 360 deg*(margin + 1 mm)/lead) base (0, 0, -eq/2 - b_lo*sqrt(1 - flat_k^2) + margin - margin - 1 mm) cut targets Shell

// ==== Checks: the layers clear each other everywhere, as printed
check Core clearance Mid >= (min(gap, clear) - 0.05 mm)
check Mid clearance Shell >= (min(gap, clear) - 0.05 mm)
check Core clearance Shell >= gap
check Core shells == 1
check Mid shells == 1
check Shell shells == 1

color Core "#e8b04a"
color Mid "#c0392b"
color Shell "#2c3e50"
material Core, Mid, Shell PLA
