// 87. Printed tweezers: one piece, two arms sprung from a solid back end, tips tapering to points.
//
// Modelled in its print pose, flat on the bed. X runs from the back end (x < 0) to the tips
// (x = L), Y across the two arms (one each side of y = 0), Z up through the print. The arms are
// strips of thickness `arm_t` in Y and `arm_h` tall in Z; they flex where they leave the back
// block, so arm_t sets the spring. At rest the inner edges diverge from a slot `2*gap` wide at the
// root to `tip_gap` apart at the tips, so squeezing the arms brings the tips together first.
// Over the last `taper_l` each arm narrows to a fine point in plan, and its top slopes down to
// `tip_h` so the points are fine in Z too. Raised ridges on the outside of each arm give grip.
//
// Print flat, no supports: the outline is one extrusion, the tip slope faces up and the ridges
// are full height. PLA, 100% infill or 4+ walls (the arms are only a few lines wide). Print with
// the arms along the bed's X or Y so their outer faces come out smooth.
design Tweezers units mm

param L = 110 mm              // root of the arms to the tips
param back_l = 14 mm          // solid back end
param arm_t = 2.6 mm          // arm thickness in plan: the spring
param arm_h = 7 mm            // arm height (print height)
param gap = 1.2 mm            // half the slot between the arms at their root
param tip_gap = 9 mm          // tips apart at rest
param taper_l = 28 mm         // the tip taper
param tip_w = 0.6 mm          // width of each point
param tip_h = 2.4 mm          // height of the points
param slope_l = 32 mm         // the top slopes down to tip_h over this length
param ridge_x = 40 mm         // first grip ridge
param ridge_n = 7
param ridge_pitch = 3.5 mm
param ridge_w = 1.2 mm
param ridge_h = 0.7 mm        // how far the ridges stand proud

let s = (tip_gap/2 - gap)/L                // slope of each inner edge
let yt = tip_gap/2                         // inner edge at the tip
let yb = gap + s*(L - taper_l) + arm_t     // outer edge where the taper starts
let w = arm_t + gap                        // half width of the back end

// The whole outline as one closed loop: back end, upper arm out and back along its inner edge,
// round the end of the slot, lower arm out and back
sketch Outline on XY {
  polyline closed o = (-back_l, -w) (-back_l, w) (0, w) (L - taper_l, yb) (L, yt + tip_w) (L, yt) (0, gap) (0, -gap) (L, -yt) (L, -yt - tip_w) (L - taper_l, -yb) (0, -w)
  fillet o.1.end radius 3 mm          // back corners
  fillet o.12.end radius 3 mm
  fillet o.6.end radius 1 mm          // the end of the slot, rounded against cracking
  fillet o.7.end radius 1 mm
  fix o
}
extrude Body from Outline distance arm_h new -> body Tweezers

// Fine points in Z: slope the top of both tips down to tip_h
sketch TipSlope on XZ offset (-tip_gap) {
  polyline closed o = (L - slope_l, -arm_h - 1 mm) (L + 1 mm, -arm_h - 1 mm) (L + 1 mm, -tip_h) (L - slope_l, -arm_h)
  fix o
}
extrude TipCut from TipSlope distance (2*tip_gap) cut targets Tweezers

// Grip ridges standing proud of each arm's outer face, square to the arm
sketch Ridges on XY {
  rect a = (ridge_x, gap + s*ridge_x + arm_t - 0.5 mm) (ridge_x + ridge_w, gap + s*ridge_x + arm_t + ridge_h)
  rect b = (ridge_x, -(gap + s*ridge_x + arm_t + ridge_h)) (ridge_x + ridge_w, -(gap + s*ridge_x + arm_t - 0.5 mm))
  fix a b
  pattern rectangular a count ridge_n spacing ridge_pitch dir (1, s)
  pattern rectangular b count ridge_n spacing ridge_pitch dir (1, -s)
}
extrude Grip from Ridges distance arm_h join targets Tweezers

color Tweezers "#2e86c1"
material Tweezers PLA
