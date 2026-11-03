// 122. Four-hole English pendant ocarina, sized from the Helmholtz resonator formula.
//
// Z is up, the vessel is centred on the origin and split at z = 0, which is also the windway's
// floor. The mouthpiece points along +X. Blow into the duct at the tip: the air sheet crosses the
// window and splits on the labium edge, and the vessel rings at its Helmholtz pitch
//     f = c / (2 pi) * sqrt(A / (V * L))
// with A the open area (window plus open holes), V the air volume and L the effective neck length
// of each opening (wall thickness + 1.6 x its equivalent radius). With every hole shut only the
// window is open, so the chamber volume V comes from the lowest note `f0` and the window. Each
// finger hole's area comes from the scale step it adds, opened in sequence: 1 hole for the 2nd
// (9/8), 2 for the 3rd (5/4), 3 for the 5th (3/2), all 4 for the octave (2). Each hole has its own
// neck length, so its area is scaled by L_hole / L_window (one fixed-point step in the lets).
//
// These sizes land close, not exact: tune by test-drilling. Print the holes `undersize` small,
// play each fingering against a tuner, and open each hole with a drill or reamer until it is in
// tune, lowest hole first. A note too flat everywhere means the vessel is too big for its window:
// widen the window a little (raises every note) or sand the labium edge sharper.
//
// The vessel is two 45 deg frustum shells (45 deg walls print without supports). The fipple head
// sits outside the rim: the duct is a groove in the top half's underside over the bottom half's
// flat; the labium is a blade in the top half with a 17 deg ramp on top; under the window a
// chamber in the bottom half opens into the vessel through the rim.
//
// Print (PLA, 0.12 mm layers for the duct and labium, no supports): both halves split face down.
// Glue the halves with CA or 2-part epoxy, keeping glue out of the duct, then clean the duct with
// a 1 mm card. BOM: glue, a cord for the lanyard hole.
design Ocarina units mm

param f0 = 523.25            // lowest note in Hz (C5)
param c_air = 343000         // speed of sound in mm/s
param duct_h = 1 mm
param ww = 8 mm              // windway and window width
param win_l = 4 mm           // window length along the jet
param wall = 2 mm
param top_k = 0.4            // inner top radius as a fraction of the rim radius
param undersize = 0.4 mm     // finger holes printed this much under their computed diameter
param duct_len = 22 mm
param hole_r_at = 20 mm      // finger holes' radius from the axis

// ---- Acoustics: chamber volume from the window, then hole areas from the scale. The quantities the
// cones are sized from are params (formulas), so the vessel stays live in SolidCraft.
param a_win = win_l*ww/(1 mm)^2                                  // window area, in mm^2
param l_win = wall + 1.6*sqrt(a_win/pi)*1 mm
param vol = a_win*c_air^2/(4*pi^2*f0^2*(l_win/(1 mm)))             // chamber volume, in mm^3
// Hole area fractions of the window, from (step^2 - 1) less the holes already open
let h1 = (9/8)^2 - 1
let h2 = (5/4)^2 - 1 - h1
let h3 = (3/2)^2 - 1 - h1 - h2
let h4 = 2^2 - 1 - h1 - h2 - h3
let d1 = 2*sqrt(h1*a_win*(wall/(1 mm) + 1.6*sqrt(h1*a_win/pi))/(l_win/(1 mm))/pi)*1 mm
let d2 = 2*sqrt(h2*a_win*(wall/(1 mm) + 1.6*sqrt(h2*a_win/pi))/(l_win/(1 mm))/pi)*1 mm
let d3 = 2*sqrt(h3*a_win*(wall/(1 mm) + 1.6*sqrt(h3*a_win/pi))/(l_win/(1 mm))/pi)*1 mm
let d4 = 2*sqrt(h4*a_win*(wall/(1 mm) + 1.6*sqrt(h4*a_win/pi))/(l_win/(1 mm))/pi)*1 mm

// ---- Vessel: two frustums, inner rim radius r_in, 45 deg walls. V = 2 * pi*h/3*(R^2 + R r + r^2)
// with r = top_k*R and h = R - r, so V = k_v * R^3 and R = (V / k_v)^(1/3)
param k_v = 2*pi*(1 - top_k)/3*(1 + top_k + top_k^2)
param r_in = (vol/k_v)^(1/3)*1 mm
param h_in = r_in*(1 - top_k)
param t45 = wall*sqrt(2)                // a 45 deg wall's horizontal thickness
param r_out = r_in + t45
param h_out = h_in + wall
param r_top = r_out - h_out             // outer top radius
let edge = r_out + 9 mm               // labium edge x
let tip = edge + win_l + duct_len
let hw = ww/2 + 3 mm                  // half width of the head

// ==== Top half
// The vessel's rims stop 0.01 mm off the split, so the head and tab are the only faces on it
cone TopOuter radius (r_out - 0.01 mm) top_radius r_top height (h_out - 0.01 mm) base (0, 0, 0.01 mm) axis Z new -> body Top
cone TopInner radius (r_in + 1 mm) top_radius (r_in*top_k) height (h_in + 1 mm) base (0, 0, -1 mm) axis Z cut targets Top
// Head top: the duct's roof, the labium blade, and its ramp
box HeadTop size (tip - r_in + 4 mm) (2*hw) 3 mm corner (r_in - 4 mm, -hw, 0) join targets Top
box Duct size (tip - edge - win_l + 1 mm) ww (duct_h + 0.1 mm) corner (edge + win_l, -ww/2, -0.1 mm) cut targets Top
box Window size win_l ww 4 mm corner (edge, -ww/2, -0.5 mm) cut targets Top
box BladeRelief size (edge - r_in + 5 mm) (ww + 2 mm) (duct_h/2 + 0.1 mm) corner (r_in - 4.5 mm, -ww/2 - 1 mm, -0.1 mm) cut targets Top
sketch RampSk on XZ offset (-ww/2 - 0.5 mm) {
  polyline closed r = (edge + 1 mm, -(duct_h/2 - (3.3 mm - duct_h/2)/8.5)) (edge + 1 mm, -3.3 mm) (edge - 8.5 mm, -3.3 mm)
  fix r
}
extrude Ramp from RampSk distance (ww + 1 mm) cut targets Top
// Finger holes, printed undersize for tuning
cylinder Hole1 radius ((d1 - undersize)/2) height (h_out + 2 mm) base (hole_r_at*cos(55 deg), hole_r_at*sin(55 deg), -0.5 mm) cut targets Top
cylinder Hole2 radius ((d2 - undersize)/2) height (h_out + 2 mm) base (hole_r_at*cos(305 deg), hole_r_at*sin(305 deg), -0.5 mm) cut targets Top
cylinder Hole3 radius ((d3 - undersize)/2) height (h_out + 2 mm) base (hole_r_at*cos(235 deg), hole_r_at*sin(235 deg), -0.5 mm) cut targets Top
cylinder Hole4 radius ((d4 - undersize)/2) height (h_out + 2 mm) base (hole_r_at*cos(125 deg), hole_r_at*sin(125 deg), -0.5 mm) cut targets Top
// Lanyard tab
box TopTab size 9 mm 8 mm 2 mm corner (-r_out - 6 mm, -4 mm, 0) join targets Top
cylinder TopTabHole radius 1.6 mm height 3 mm base (-r_out - 2.5 mm, 0, -0.5 mm) cut targets Top

// ==== Bottom half: the duct floor, the chamber under the window, and the vessel
cone BottomOuter radius r_top top_radius (r_out - 0.01 mm) height (h_out - 0.01 mm) base (0, 0, -h_out) axis Z new -> body Bottom
cone BottomInner radius (r_in*top_k) top_radius (r_in + 1 mm) height (h_in + 1 mm) base (0, 0, -h_in) axis Z cut targets Bottom
box HeadBottom size (tip - r_out + 3 mm) (2*hw) 7 mm corner (r_out - 3 mm, -hw, -7 mm) join targets Bottom
box Chamber size (edge + win_l - r_in + 3 mm) (ww + 2 mm) 6.1 mm corner (r_in - 3 mm, -ww/2 - 1 mm, -6 mm) cut targets Bottom
box BottomTab size 9 mm 8 mm 2 mm corner (-r_out - 6 mm, -4 mm, -2 mm) join targets Bottom
cylinder BottomTabHole radius 1.6 mm height 3 mm base (-r_out - 2.5 mm, 0, -2.5 mm) cut targets Bottom

color Top "#7fb7a4"
color Bottom "#5f9886"
material Top, Bottom PLA
