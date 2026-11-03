// 120. Print-in-place ball bearing: two races and a ring of balls that spin straight off the bed.
//
// Z is the bearing's axis, the bed is z = 0, the bearing is centred on the origin. The inner and
// outer races are revolved sections, each with a V groove whose flanks run at 45 deg, so every
// overhang in the print is 45 deg and nothing needs supports. The balls sit on the pitch circle,
// centred at mid-width. Each flank stays `clearance` off the ball (the groove apex is
// (ball_d/2 + clearance) * sqrt 2 from the ball's centre), and the races' faces stand `gap_half`
// either side of the pitch circle, closer than the ball's radius, so the balls are captive.
//
// A ball's lowest point would start in mid air, so each ball stands on a thin post (post_r) from
// the bed, in the gap between the races. The first turn snaps the posts; shake the stubs out.
//
// Printing: flat on the bed, one plate, 0.2 mm layers, no supports, no brim needed at the default
// size. Hold the outer race and turn the inner one to snap the posts and break the balls free; a
// drop of light oil helps. If the
// balls fuse, raise `clearance` to 0.4-0.45 mm; if they rattle, lower it.
//
// Sizes: `balls` (up to 12) is read when compiling, so change it and rebuild. The loop below makes
// at most 12 balls; each needs about ball_d + 1.5 mm of pitch circle.
design PrintedBearing units mm

param bore = 8 mm
param od = 32 mm
param width = 10 mm
param ball_d = 5 mm
param balls = 9
param clearance = 0.35 mm
param chamfer = 0.6 mm
param post_r = 0.6 mm             // breakaway post under each ball

let r_bore = bore/2
let r_out = od/2
let r_pitch = (r_bore + r_out)/2
let gap_half = ball_d/2 - 0.9 mm                    // race faces from the pitch circle
let apex = (ball_d/2 + clearance)*sqrt(2)           // groove apex from the ball centre
let zc = width/2
let r_in_face = r_pitch - gap_half
let r_out_face = r_pitch + gap_half
let open_half = apex - gap_half                     // half the groove's opening on a face

// Inner race: bore to the inner face, V groove in its outer face (on XZ, sketch v is -z)
sketch InnerSk on XZ {
  polyline closed o = (r_bore + chamfer, 0) (r_in_face, 0) (r_in_face, -(zc - open_half)) (r_pitch - apex, -zc) (r_in_face, -(zc + open_half)) (r_in_face, -width) (r_bore + chamfer, -width) (r_bore, -(width - chamfer)) (r_bore, -chamfer)
  fix o
}
revolve InnerRace from InnerSk axis Z -> body Inner

// Outer race: outer face to the OD, V groove in its inner face
sketch OuterSk on XZ {
  polyline closed o = (r_out_face, 0) (r_out - chamfer, 0) (r_out, -chamfer) (r_out, -(width - chamfer)) (r_out - chamfer, -width) (r_out_face, -width) (r_out_face, -(zc + open_half)) (r_pitch + apex, -zc) (r_out_face, -(zc - open_half))
  fix o
}
revolve OuterRace from OuterSk axis Z -> body Outer

// Balls on the pitch circle
for k from 0 to 11 {
  if k < balls {
    sphere Ball{k} radius (ball_d/2) center (r_pitch*cos(k*360 deg/balls), r_pitch*sin(k*360 deg/balls), zc) new -> body Ball{k}
    cylinder Post{k} radius post_r height (zc - ball_d/2 + 0.3 mm) base (r_pitch*cos(k*360 deg/balls), r_pitch*sin(k*360 deg/balls), 0) join targets Ball{k}
  }
}

// Every ball keeps its clearance to both races (checked exactly on the first ball; the rest are copies)
check Inner clearance Ball0 >= (clearance - 0.01 mm)
check Outer clearance Ball0 >= (clearance - 0.01 mm)
check Inner clearance Outer >= (2*gap_half - 0.01 mm)

color Inner, Outer "#c9ccd1"
for k from 0 to 11 {
  if k < balls {
    color Ball{k} "#e8b04a"
  }
}
material Inner, Outer PLA
