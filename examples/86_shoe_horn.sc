// 86. Shoe horn: a curved trough that narrows from the heel end into a flat handle with a hanging slot.
//
// Modelled in its print pose. Z runs along the horn: the heel end (the end that goes into the
// shoe) stands on the bed at z = 0 and the handle points up. The trough is an arc band (inner
// radius R, wall t, opening toward +Y) extruded up Z, then trimmed to its outline by intersecting
// with the plan shape seen along Y: wide and rounded at the heel, narrowing toward the handle. The
// handle is a flat plate on the trough's lowest line (y = -R - t), so the back of the horn is one
// straight line from heel to handle end; a short loft (the neck) flattens the arc into it.
//
// Print standing on the heel end, no supports: every layer is a closed arc band or the flat
// handle, and the hanging slot runs along Z so its top is a small arch. The footprint is only the
// arc band, so use a brim (8 mm or so). PLA or PETG, 3 or 4 walls so the wall is solid.
design ShoeHorn units mm

param R = 30 mm               // inner radius of the trough
param t = 2.4 mm              // wall thickness
param half_span = 65 deg      // the arc band runs this far each side of the bottom
param trough_l = 125 mm       // heel end to where the handle takes over
param total_l = 190 mm
param heel_w = 54 mm          // width across the heel end, seen from the front
param neck_w = 26 mm          // width where the trough ends
param blend_l = 25 mm         // length of the neck that flattens the trough into the handle
param handle_w = 30 mm        // width at the handle end
param handle_t = 3.6 mm
param slot_w = 7 mm
param slot_l = 14 mm

let Ro = R + t
let s = sin(half_span)
let c = cos(half_span)

// The trough: an arc band round the bottom (-Y), extruded up Z
sketch Section on XY {
  arc o = center (0,0) start (-Ro*s, -Ro*c) end (Ro*s, -Ro*c)
  arc i = center (0,0) start (-R*s, -R*c) end (R*s, -R*c)
  line right = o.end i.end
  line left = i.start o.start
  fix o i right left
}
extrude Trough from Section distance trough_l new -> body ShoeHorn

// Its outline seen from the front (along Y): rounded heel, narrowing to the neck. It runs 1 mm past
// both ends of the trough so no face of the cut lies on one of the trough's.
sketch Outline on XZ offset (-Ro - 5 mm) {
  polyline closed o = (-heel_w/2, 1 mm) (heel_w/2, 1 mm) (neck_w/2, -(trough_l + 1 mm)) (-neck_w/2, -(trough_l + 1 mm))
  fillet o.1.end radius 18 mm
  fillet o.4.end radius 18 mm
  fix o
}
extrude Trim from Outline distance (2*Ro + 10 mm) intersect targets ShoeHorn

// Neck: a short loft from the trough's narrow arc section to the flat handle's section, so the
// curve flattens out into the handle instead of stopping at a step
// The loft starts 2 mm down inside the trough, 0.3 mm inside its wall, so the two overlap rather
// than meet face to face (a flush union of the arc faces fails).
let nx = neck_w/2 - 0.5 mm
let ro = Ro - 0.3 mm
let ri = R + 0.3 mm
sketch NeckSk on XY offset (trough_l - 2 mm) {
  arc o = center (0,0) start (-nx, -sqrt(ro^2 - nx^2)) end (nx, -sqrt(ro^2 - nx^2))
  arc i = center (0,0) start (-nx, -sqrt(ri^2 - nx^2)) end (nx, -sqrt(ri^2 - nx^2))
  line right = o.end i.end
  line left = i.start o.start
  fix o i right left
}
sketch FlatSk on XY offset (trough_l + blend_l) {
  rect r = (-neck_w/2, -Ro) (neck_w/2, -Ro + handle_t)
  fix r
}
loft Neck sections NeckSk, FlatSk join targets ShoeHorn

// The flat handle, on the trough's back line (y = -Ro), widening a little to a rounded end
sketch HandleSk on XZ offset (-Ro) {
  polyline closed o = (-neck_w/2, -(trough_l + blend_l)) (neck_w/2, -(trough_l + blend_l)) (handle_w/2, -total_l) (-handle_w/2, -total_l)
  fillet o.2.end radius 12 mm
  fillet o.3.end radius 12 mm
  fix o
}
extrude Handle from HandleSk distance handle_t join targets ShoeHorn

// Hanging slot through the handle, long along Z so it prints as an arch
sketch SlotSk on XZ offset (-Ro - 1 mm) {
  slot h = centers (0, -(total_l - 12 mm)) (0, -(total_l - 12 mm - slot_l + slot_w)) width slot_w
  fix h
}
extrude HangSlot from SlotSk distance (handle_t + 2 mm) cut targets ShoeHorn

color ShoeHorn "#c0392b"
material ShoeHorn PLA
