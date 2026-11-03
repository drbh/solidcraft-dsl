// 149. Collapsing toy sword: telescoping blade segments, printed nested inside the hilt, that flick out and lock.
//
// Z is up from the bed. The set on the left (x = 0) is the print: the hilt and `segments` round
// blade segments, all standing on the bed nested one inside the next, printed in place in one go.
// The set on the right is the same parts drawn extended, for the picture and for checking how they
// lock; it isn't printed. A pommel cap snaps over the hilt's foot after printing and keeps the
// segments from dropping out of the bottom.
//
// How a segment locks: each one has a collar at its foot, a cone `lock_taper` off the vertical,
// and each tube has a matching conical seat in its mouth. Flick the sword and every segment slides
// out until its collar wedges into the seat of the one below: a taper lock, firm by friction.
// Just below each seat there is a ridge `detent` deep, which the collar has to squeeze past,
// `detent - clr` (0.1 mm) of interference. Coming out, it clicks past and then holds the segment
// out. Going back, it needs a firm push on the tip to unlock. Every segment is the next one's
// tube, `ds + clr + wall` larger in radius, so the blade steps down to a blunt 8 mm dome at the tip.
//
// The seat narrows the mouth by ds = seat_l*tan(lock_taper). The next segment's tube passes it
// with `clr` all round and clears the ridge by ds + clr - detent. In the extended set each
// collar is drawn 1 mm short of the lock (0.14 mm off the seat) so the check can measure it. Fully
// home, the cones touch, clr/tan(lock_taper) = 2.1 mm further on.
//
// The defaults (5 segments, 80 mm each) give a 33 mm hilt with a 57 mm round guard. The sword is
// 460 mm long out and 84 mm folded, plus its cap.
//
// Printing: the nested set upright as modelled, one go, PLA, 0.2 mm layers, 3+ perimeters, no
// supports, no brim. Turn on elephant-foot compensation: the segments' feet stand 0.3 mm apart
// on the bed. The seats are 8 deg overhangs, and the ridges, guard and bead are 45 deg. Print the
// cap floor down. After printing, pull each segment out and push it back a few times to free it.
// It is a toy: the tip is blunt, the segments are thin PLA, don't swing it at anyone.
design CollapsingSword units mm

param segments = 5             // blade segments (1 to 7)
param seg_l = 80 mm            // every segment and the hilt
param tip_r = 4 mm             // the innermost segment: a solid rod, domed
param wall = 1.4 mm
param clr = 0.3 mm             // radial play between nested parts
param lock_taper = 8 deg       // the collar and seat cone, from the vertical
param seat_l = 6 mm            // collar and seat height
param detent = 0.4 mm          // ridge depth below each seat; detent - clr is the snap
param guard = 12 mm            // how far the guard stands out from the hilt
param guard_t = 5 mm
param bead = 0.45 mm           // the hilt's snap bead for the cap
param cap_clr = 0.2 mm
param cap_h = 12 mm

let ds = seat_l*tan(lock_taper)
let step = ds + clr + wall
let R0 = tip_r + segments*step          // hilt outer radius
let Rg = R0 + guard
let zr = seg_l - seat_l - 1 mm - 2*detent  // foot of each ridge
let stroke = seg_l - seat_l + clr/tan(lock_taper) - 1 mm  // demo: 1 mm short of the lock
let bz = 5 mm                           // the bead's height on the hilt
let xd = 2*Rg + 30 mm                   // the extended set's axis

// s = 0: the nested print set at x = 0; s = 1: the extended demo at x = xd, segment k raised k*stroke.
// Segment k's outer radius is tip_r + (segments - k)*step and its bore that less wall; k = 0 is the hilt.
for s from 0 to 1 {
  for k from 0 to 7 {
    // The hilt: bore with ridge and seat, round guard at the top, snap bead at the foot
    if k == 0 {
      sketch S{s}Seg{k}Sk on XZ {
        polyline closed o = (s*xd + R0 - wall, 0) (s*xd + R0 - wall, -zr) (s*xd + R0 - wall - detent, -(zr + detent)) (s*xd + R0 - wall, -(zr + 2*detent)) (s*xd + R0 - wall, -(seg_l - seat_l)) (s*xd + R0 - wall - ds, -seg_l) (s*xd + Rg, -seg_l) (s*xd + Rg, -(seg_l - guard_t)) (s*xd + R0, -(seg_l - guard_t - guard)) (s*xd + R0, -(bz + bead)) (s*xd + R0 + bead, -bz) (s*xd + R0, -(bz - bead)) (s*xd + R0, 0)
        construction line ax = (s*xd, 10 mm) (s*xd, -(seg_l + 10 mm))
        fix o ax
      }
      revolve S{s}Seg{k}Rev from S{s}Seg{k}Sk axis S{s}Seg{k}Sk.ax new -> body S{s}Seg{k}
    }
    // A middle segment: collar at its foot, ridge and seat in its mouth
    if k >= 1 {
      if k < segments {
        sketch S{s}Seg{k}Sk on XZ {
          polyline closed o = (s*xd + tip_r + (segments - k)*step - wall, -(s*k*stroke)) (s*xd + tip_r + (segments - k)*step - wall, -(s*k*stroke + zr)) (s*xd + tip_r + (segments - k)*step - wall - detent, -(s*k*stroke + zr + detent)) (s*xd + tip_r + (segments - k)*step - wall, -(s*k*stroke + zr + 2*detent)) (s*xd + tip_r + (segments - k)*step - wall, -(s*k*stroke + seg_l - seat_l)) (s*xd + tip_r + (segments - k)*step - wall - ds, -(s*k*stroke + seg_l)) (s*xd + tip_r + (segments - k)*step, -(s*k*stroke + seg_l)) (s*xd + tip_r + (segments - k)*step, -(s*k*stroke + seat_l)) (s*xd + tip_r + (segments - k)*step + ds, -(s*k*stroke))
          construction line ax = (s*xd, 10 mm) (s*xd, -(s*k*stroke + seg_l + 10 mm))
          fix o ax
        }
        revolve S{s}Seg{k}Rev from S{s}Seg{k}Sk axis S{s}Seg{k}Sk.ax new -> body S{s}Seg{k}
      }
    }
    // The tip: a solid rod with a collar and a blunt dome
    if k == segments {
      sketch S{s}Seg{k}Sk on XZ {
        line foot = (s*xd, -(s*k*stroke)) (s*xd + tip_r + ds, -(s*k*stroke))
        line collar = foot.end (s*xd + tip_r, -(s*k*stroke + seat_l))
        line side = collar.end (s*xd + tip_r, -(s*k*stroke + seg_l))
        arc dome = start side.end through (s*xd + tip_r*0.7071, -(s*k*stroke + seg_l + tip_r*0.7071)) end (s*xd, -(s*k*stroke + seg_l + tip_r))
        line axis_l = dome.end foot.start
        construction line ax = (s*xd, 10 mm) (s*xd, -(s*k*stroke + seg_l + tip_r + 10 mm))
        fix foot collar side dome axis_l ax
      }
      revolve S{s}Seg{k}Rev from S{s}Seg{k}Sk axis S{s}Seg{k}Sk.ax new -> body S{s}Seg{k}
    }
    if k <= segments {
      material S{s}Seg{k} PLA
      color S{s}Seg{k} "#c9ccd1"
    }
  }

  // Pommel cap: a cup over the hilt's foot, a groove for the bead and four slits so it can flex on
  sketch S{s}CapSk on XZ {
    polyline closed o = (s*xd, 0.3 mm) (s*xd + R0 + cap_clr, 0.3 mm) (s*xd + R0 + cap_clr, -(bz - bead)) (s*xd + R0 + cap_clr + bead, -bz) (s*xd + R0 + cap_clr, -(bz + bead)) (s*xd + R0 + cap_clr, -cap_h) (s*xd + R0 + cap_clr + 2 mm, -cap_h) (s*xd + R0 + cap_clr + 2 mm, 2.8 mm) (s*xd, 2.8 mm)
    construction line ax = (s*xd, 10 mm) (s*xd, -(cap_h + 10 mm))
    fix o ax
  }
  revolve S{s}CapRev from S{s}CapSk axis S{s}CapSk.ax new -> body S{s}Cap
  box S{s}SlitA size 1.5 mm (2*R0 + 10 mm) (cap_h - 2 mm) corner (s*xd - 0.75 mm, -(R0 + 5 mm), 2.5 mm) cut targets S{s}Cap
  box S{s}SlitB size (2*R0 + 10 mm) 1.5 mm (cap_h - 2 mm) corner (s*xd - (R0 + 5 mm), -0.75 mm, 2.5 mm) cut targets S{s}Cap
  material S{s}Cap PLA
  color S{s}Cap "#2b2d31"
}
color S0Seg0, S1Seg0 "#2b2d31"

// ==== Checks. Nested (as printed): each part clears the one round it by clr. Extended: each collar
// stands just off its seat, and the cap's groove clears the bead.
for a from 0 to 6 {
  for b from 1 to 7 {
    if b == a + 1 {
      if b <= segments {
        check S0Seg{b} clearance S0Seg{a} >= (clr - 0.02 mm)
        check S1Seg{b} clearance S1Seg{a} >= 0.1 mm
      }
    }
  }
}
check S0Cap clearance S0Seg0 >= 0.12 mm
// Prints upright with no supports
check S0Seg0 overhang <= 45 deg
check S0Seg1 overhang <= 45 deg
check S0Seg5 overhang <= 45 deg
check S0Cap overhang <= 45 deg
