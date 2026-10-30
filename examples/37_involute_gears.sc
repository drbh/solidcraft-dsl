// 37. Meshing spur gear pair with true involute flanks: 15 and 30 teeth, module 2, 20 deg.
//
// Z is up; both gears lie flat on z = 0 with their axes along Z, the 15-tooth pinion at the
// origin and the 30-tooth gear at the standard centre distance a = m(z1 + z2)/2 = 45 mm on +X.
// Unlike examples 13 and 14, whose teeth are straight-flank trapezoids or polylines, each flank
// here is a sketch spline through six points of the involute of the base circle, computed with
// `let` expressions (x = rb(cos t + t sin t), y = rb(sin t - t cos t), rotated into place), so
// changing the module, tooth counts or pressure angle regenerates the true profile. Each tooth
// space is cut from a tip-diameter blank: the two involute flanks, radial flanks below the base
// circle, a root arc at the dedendum (1.25 m, giving 0.25 m tip clearance) and root fillets
// where the radial flank meets the root, then the space is patterned around the gear. Backlash
// thins each tooth by backlash/2 at the pitch circle. The second gear is turned half a pitch so
// a space faces the pinion's tooth on the line of centres; 15 teeth against 30 is above the
// 20 deg interference limit for this pair, so no profile shift is needed.
// The pinion has a D-flat bore for a 5 mm motor shaft, the gear an 8 mm bore with a keyway;
// both have a hub on top with the tooth count debossed in it (the label text is literal, so
// update it if you change z1 or z2), and the gear has lightening holes.
//
// Print both flat on the bed as modelled (hub up), no supports: every feature is a vertical
// extrusion from the face, the hub sits on the full gear face.
design InvoluteGears units mm
// Gearing
param gm = 2 mm
param z1 = 15
param z2 = 30
param flank = 20 deg
param face = 8 mm
param backlash = 0.2 mm
param root_r = 0.4 mm
let center_d = gm*(z1 + z2)/2

// Pinion bore: D-flat 5 mm motor shaft
param shaft1_d = 5 mm
param flat_depth = 0.5 mm
param shaft_fit = 0.15 mm
param hub1_r = 7 mm
param hub1_h = 6 mm
// Gear bore: 8 mm shaft with a 3 mm keyway
param shaft2_d = 8 mm
param key_w = 3 mm
param key_depth = 1.4 mm
param hub2_r = 10 mm
param hub2_h = 5 mm
// Lightening holes in the gear web
param n_holes = 6
param hole_r = 5.5 mm
let hole_pitch_r = (hub2_r + gm*z2/2 - 1.25*gm - 3 mm)/2

// Pinion: z1 teeth. Tooth spaces are cut from a tip-diameter blank, then patterned.
let g1_rp = gm*z1/2
let g1_ra = g1_rp + gm
let g1_rf = g1_rp - 1.25*gm
let g1_rb = g1_rp*cos(flank)
// Base-circle start of the flank, measured from the tooth centre line
let g1_phi = (90 deg/z1 - backlash/(4*g1_rp)*1 rad) + (tan(flank)*1 rad - flank)
// Run the involute a little past the tip so the cut clears the blank
let g1_tmax = sqrt((((g1_ra + 0.6 mm)/g1_rb)^2) - 1)
let g1_cp = cos(g1_phi)
let g1_sp = sin(g1_phi)
let g1_cr = cos(0 deg)
let g1_sr = sin(0 deg)
let g1_cq = cos(0 deg + 360 deg/z1)
let g1_sq = sin(0 deg + 360 deg/z1)
let g1_t0 = g1_tmax*0/5
let g1_a0 = cos(g1_t0*1 rad) + g1_t0*sin(g1_t0*1 rad)
let g1_c0 = g1_t0*cos(g1_t0*1 rad) - sin(g1_t0*1 rad)
let g1_x0 = g1_rb*(g1_a0*g1_cp - g1_c0*g1_sp)
let g1_y0 = g1_rb*(g1_a0*g1_sp + g1_c0*g1_cp)
let g1_t1 = g1_tmax*1/5
let g1_a1 = cos(g1_t1*1 rad) + g1_t1*sin(g1_t1*1 rad)
let g1_c1 = g1_t1*cos(g1_t1*1 rad) - sin(g1_t1*1 rad)
let g1_x1 = g1_rb*(g1_a1*g1_cp - g1_c1*g1_sp)
let g1_y1 = g1_rb*(g1_a1*g1_sp + g1_c1*g1_cp)
let g1_t2 = g1_tmax*2/5
let g1_a2 = cos(g1_t2*1 rad) + g1_t2*sin(g1_t2*1 rad)
let g1_c2 = g1_t2*cos(g1_t2*1 rad) - sin(g1_t2*1 rad)
let g1_x2 = g1_rb*(g1_a2*g1_cp - g1_c2*g1_sp)
let g1_y2 = g1_rb*(g1_a2*g1_sp + g1_c2*g1_cp)
let g1_t3 = g1_tmax*3/5
let g1_a3 = cos(g1_t3*1 rad) + g1_t3*sin(g1_t3*1 rad)
let g1_c3 = g1_t3*cos(g1_t3*1 rad) - sin(g1_t3*1 rad)
let g1_x3 = g1_rb*(g1_a3*g1_cp - g1_c3*g1_sp)
let g1_y3 = g1_rb*(g1_a3*g1_sp + g1_c3*g1_cp)
let g1_t4 = g1_tmax*4/5
let g1_a4 = cos(g1_t4*1 rad) + g1_t4*sin(g1_t4*1 rad)
let g1_c4 = g1_t4*cos(g1_t4*1 rad) - sin(g1_t4*1 rad)
let g1_x4 = g1_rb*(g1_a4*g1_cp - g1_c4*g1_sp)
let g1_y4 = g1_rb*(g1_a4*g1_sp + g1_c4*g1_cp)
let g1_t5 = g1_tmax*5/5
let g1_a5 = cos(g1_t5*1 rad) + g1_t5*sin(g1_t5*1 rad)
let g1_c5 = g1_t5*cos(g1_t5*1 rad) - sin(g1_t5*1 rad)
let g1_x5 = g1_rb*(g1_a5*g1_cp - g1_c5*g1_sp)
let g1_y5 = g1_rb*(g1_a5*g1_sp + g1_c5*g1_cp)
cylinder PinionBlank radius g1_ra height face base (0,0,0) new -> body Pinion
sketch PinionSpaceSk on XY offset (-1 mm) {
  point rlo = (0+(g1_rf*g1_cp)*g1_cr-(g1_rf*g1_sp)*g1_sr,(g1_rf*g1_cp)*g1_sr+(g1_rf*g1_sp)*g1_cr)
  point rhi = (0+(g1_rf*g1_cp)*g1_cq+(g1_rf*g1_sp)*g1_sq,(g1_rf*g1_cp)*g1_sq-(g1_rf*g1_sp)*g1_cq)
  point l0 = (0+(g1_x0)*g1_cr-(g1_y0)*g1_sr,(g1_x0)*g1_sr+(g1_y0)*g1_cr)
  point l1 = (0+(g1_x1)*g1_cr-(g1_y1)*g1_sr,(g1_x1)*g1_sr+(g1_y1)*g1_cr)
  point l2 = (0+(g1_x2)*g1_cr-(g1_y2)*g1_sr,(g1_x2)*g1_sr+(g1_y2)*g1_cr)
  point l3 = (0+(g1_x3)*g1_cr-(g1_y3)*g1_sr,(g1_x3)*g1_sr+(g1_y3)*g1_cr)
  point l4 = (0+(g1_x4)*g1_cr-(g1_y4)*g1_sr,(g1_x4)*g1_sr+(g1_y4)*g1_cr)
  point l5 = (0+(g1_x5)*g1_cr-(g1_y5)*g1_sr,(g1_x5)*g1_sr+(g1_y5)*g1_cr)
  point h0 = (0+(g1_x0)*g1_cq+(g1_y0)*g1_sq,(g1_x0)*g1_sq-(g1_y0)*g1_cq)
  point h1 = (0+(g1_x1)*g1_cq+(g1_y1)*g1_sq,(g1_x1)*g1_sq-(g1_y1)*g1_cq)
  point h2 = (0+(g1_x2)*g1_cq+(g1_y2)*g1_sq,(g1_x2)*g1_sq-(g1_y2)*g1_cq)
  point h3 = (0+(g1_x3)*g1_cq+(g1_y3)*g1_sq,(g1_x3)*g1_sq-(g1_y3)*g1_cq)
  point h4 = (0+(g1_x4)*g1_cq+(g1_y4)*g1_sq,(g1_x4)*g1_sq-(g1_y4)*g1_cq)
  point h5 = (0+(g1_x5)*g1_cq+(g1_y5)*g1_sq,(g1_x5)*g1_sq-(g1_y5)*g1_cq)
  line rl = rlo l0
  spline lo = l0 l1 l2 l3 l4 l5
  line top = l5 h5
  spline hi = h5 h4 h3 h2 h1 h0
  line rh = h0 rhi
  arc root = center (0,0) start rlo end rhi
  fix rlo rhi l0 l1 l2 l3 l4 l5 h0 h1 h2 h3 h4 h5 root.center
  fillet rlo radius root_r
  fillet rhi radius root_r
}
extrude PinionSpace from PinionSpaceSk distance (face + 2 mm) cut targets Pinion
pattern circular PinionSpaces features PinionSpace origin (0,0,0) dir (0,0,1) count z1 angle 360 deg
// Pinion hub and D-flat bore (the flat is a key left standing in the round bore)
cylinder PinionHub radius hub1_r height (hub1_h + 0.5 mm) base (0,0,face - 0.5 mm) join targets Pinion
cylinder PinionBore radius (shaft1_d/2 + shaft_fit) height (face + hub1_h + 2 mm) base (0,0,-1 mm) cut targets Pinion
box PinionFlat size (flat_depth + 1 mm) (shaft1_d + 2 mm) (face + hub1_h) corner (shaft1_d/2 - flat_depth + shaft_fit,-shaft1_d/2 - 1 mm,0) join targets Pinion

// Gear: z2 teeth. Tooth spaces are cut from a tip-diameter blank, then patterned.
let g2_rp = gm*z2/2
let g2_ra = g2_rp + gm
let g2_rf = g2_rp - 1.25*gm
let g2_rb = g2_rp*cos(flank)
// Base-circle start of the flank, measured from the tooth centre line
let g2_phi = (90 deg/z2 - backlash/(4*g2_rp)*1 rad) + (tan(flank)*1 rad - flank)
// Run the involute a little past the tip so the cut clears the blank
let g2_tmax = sqrt((((g2_ra + 0.6 mm)/g2_rb)^2) - 1)
let g2_cp = cos(g2_phi)
let g2_sp = sin(g2_phi)
let g2_cr = cos((180 deg - 180 deg/z2))
let g2_sr = sin((180 deg - 180 deg/z2))
let g2_cq = cos((180 deg - 180 deg/z2) + 360 deg/z2)
let g2_sq = sin((180 deg - 180 deg/z2) + 360 deg/z2)
let g2_t0 = g2_tmax*0/5
let g2_a0 = cos(g2_t0*1 rad) + g2_t0*sin(g2_t0*1 rad)
let g2_c0 = g2_t0*cos(g2_t0*1 rad) - sin(g2_t0*1 rad)
let g2_x0 = g2_rb*(g2_a0*g2_cp - g2_c0*g2_sp)
let g2_y0 = g2_rb*(g2_a0*g2_sp + g2_c0*g2_cp)
let g2_t1 = g2_tmax*1/5
let g2_a1 = cos(g2_t1*1 rad) + g2_t1*sin(g2_t1*1 rad)
let g2_c1 = g2_t1*cos(g2_t1*1 rad) - sin(g2_t1*1 rad)
let g2_x1 = g2_rb*(g2_a1*g2_cp - g2_c1*g2_sp)
let g2_y1 = g2_rb*(g2_a1*g2_sp + g2_c1*g2_cp)
let g2_t2 = g2_tmax*2/5
let g2_a2 = cos(g2_t2*1 rad) + g2_t2*sin(g2_t2*1 rad)
let g2_c2 = g2_t2*cos(g2_t2*1 rad) - sin(g2_t2*1 rad)
let g2_x2 = g2_rb*(g2_a2*g2_cp - g2_c2*g2_sp)
let g2_y2 = g2_rb*(g2_a2*g2_sp + g2_c2*g2_cp)
let g2_t3 = g2_tmax*3/5
let g2_a3 = cos(g2_t3*1 rad) + g2_t3*sin(g2_t3*1 rad)
let g2_c3 = g2_t3*cos(g2_t3*1 rad) - sin(g2_t3*1 rad)
let g2_x3 = g2_rb*(g2_a3*g2_cp - g2_c3*g2_sp)
let g2_y3 = g2_rb*(g2_a3*g2_sp + g2_c3*g2_cp)
let g2_t4 = g2_tmax*4/5
let g2_a4 = cos(g2_t4*1 rad) + g2_t4*sin(g2_t4*1 rad)
let g2_c4 = g2_t4*cos(g2_t4*1 rad) - sin(g2_t4*1 rad)
let g2_x4 = g2_rb*(g2_a4*g2_cp - g2_c4*g2_sp)
let g2_y4 = g2_rb*(g2_a4*g2_sp + g2_c4*g2_cp)
let g2_t5 = g2_tmax*5/5
let g2_a5 = cos(g2_t5*1 rad) + g2_t5*sin(g2_t5*1 rad)
let g2_c5 = g2_t5*cos(g2_t5*1 rad) - sin(g2_t5*1 rad)
let g2_x5 = g2_rb*(g2_a5*g2_cp - g2_c5*g2_sp)
let g2_y5 = g2_rb*(g2_a5*g2_sp + g2_c5*g2_cp)
cylinder GearBlank radius g2_ra height face base (center_d,0,0) new -> body Gear
sketch GearSpaceSk on XY offset (-1 mm) {
  point rlo = (center_d+(g2_rf*g2_cp)*g2_cr-(g2_rf*g2_sp)*g2_sr,(g2_rf*g2_cp)*g2_sr+(g2_rf*g2_sp)*g2_cr)
  point rhi = (center_d+(g2_rf*g2_cp)*g2_cq+(g2_rf*g2_sp)*g2_sq,(g2_rf*g2_cp)*g2_sq-(g2_rf*g2_sp)*g2_cq)
  point l0 = (center_d+(g2_x0)*g2_cr-(g2_y0)*g2_sr,(g2_x0)*g2_sr+(g2_y0)*g2_cr)
  point l1 = (center_d+(g2_x1)*g2_cr-(g2_y1)*g2_sr,(g2_x1)*g2_sr+(g2_y1)*g2_cr)
  point l2 = (center_d+(g2_x2)*g2_cr-(g2_y2)*g2_sr,(g2_x2)*g2_sr+(g2_y2)*g2_cr)
  point l3 = (center_d+(g2_x3)*g2_cr-(g2_y3)*g2_sr,(g2_x3)*g2_sr+(g2_y3)*g2_cr)
  point l4 = (center_d+(g2_x4)*g2_cr-(g2_y4)*g2_sr,(g2_x4)*g2_sr+(g2_y4)*g2_cr)
  point l5 = (center_d+(g2_x5)*g2_cr-(g2_y5)*g2_sr,(g2_x5)*g2_sr+(g2_y5)*g2_cr)
  point h0 = (center_d+(g2_x0)*g2_cq+(g2_y0)*g2_sq,(g2_x0)*g2_sq-(g2_y0)*g2_cq)
  point h1 = (center_d+(g2_x1)*g2_cq+(g2_y1)*g2_sq,(g2_x1)*g2_sq-(g2_y1)*g2_cq)
  point h2 = (center_d+(g2_x2)*g2_cq+(g2_y2)*g2_sq,(g2_x2)*g2_sq-(g2_y2)*g2_cq)
  point h3 = (center_d+(g2_x3)*g2_cq+(g2_y3)*g2_sq,(g2_x3)*g2_sq-(g2_y3)*g2_cq)
  point h4 = (center_d+(g2_x4)*g2_cq+(g2_y4)*g2_sq,(g2_x4)*g2_sq-(g2_y4)*g2_cq)
  point h5 = (center_d+(g2_x5)*g2_cq+(g2_y5)*g2_sq,(g2_x5)*g2_sq-(g2_y5)*g2_cq)
  line rl = rlo l0
  spline lo = l0 l1 l2 l3 l4 l5
  line top = l5 h5
  spline hi = h5 h4 h3 h2 h1 h0
  line rh = h0 rhi
  arc root = center (center_d,0) start rlo end rhi
  fix rlo rhi l0 l1 l2 l3 l4 l5 h0 h1 h2 h3 h4 h5 root.center
  fillet rlo radius root_r
  fillet rhi radius root_r
}
extrude GearSpace from GearSpaceSk distance (face + 2 mm) cut targets Gear
pattern circular GearSpaces features GearSpace origin (center_d,0,0) dir (0,0,1) count z2 angle 360 deg
// Gear hub, keyed bore and lightening holes
cylinder GearHub radius hub2_r height (hub2_h + 0.5 mm) base (center_d,0,face - 0.5 mm) join targets Gear
cylinder GearBore radius (shaft2_d/2 + shaft_fit) height (face + hub2_h + 2 mm) base (center_d,0,-1 mm) cut targets Gear
box Keyway size (shaft2_d/2 + key_depth) key_w (face + hub2_h + 2 mm) corner (center_d,-key_w/2,-1 mm) cut targets Gear
cylinder Lightening radius hole_r height (face + 2 mm) base (center_d - hole_pitch_r,0,-1 mm) cut targets Gear
pattern circular LighteningHoles features Lightening origin (center_d,0,0) dir (0,0,1) count n_holes angle 360 deg

// Tooth counts cut 0.4 mm into the hub tops (sketches on XY planes at the hub top; a face
// sketch on these many-faced bodies exceeds the engine's face-capture limit)
sketch PinionLabelSk on XY offset (face + hub1_h) {
  text "15T" at (0,(shaft1_d/2 + hub1_r)/2 - 0.8 mm) height 2 mm align center
}
extrude PinionLabel from PinionLabelSk distance 0.4 mm negative cut targets Pinion
sketch GearLabelSk on XY offset (face + hub2_h) {
  text "30T" at (center_d,(shaft2_d/2 + hub2_r)/2 - 1.2 mm) height 3 mm align center
}
extrude GearLabel from GearLabelSk distance 0.4 mm negative cut targets Gear

// The teeth mesh with backlash and never touch.
check Pinion clearance Gear > 0 mm
