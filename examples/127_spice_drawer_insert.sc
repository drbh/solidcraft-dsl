// 127. Tiered spice-drawer insert: four stepped rows so every jar's lid label shows from above.
//
// X runs across the drawer (its width), Y from the front of the drawer (y = 0) to the back, Z up.
// The insert is one stair profile, drawn in the YZ plane and extruded across the drawer: four
// treads, each tread_d deep and rise higher than the one in front, with a lip at the front of
// every tread so jars don't slide forward. The profile is shelled to an open-ended tube (walls
// `wall` thick, both ends open against the drawer sides), then split at mid-width so each half
// fits the bed. A coupler, the inside of the profile less `clear`, slides half its length into
// each half and keys them together; it is shelled open-ended like the halves.
//
// Sizing: tread_d is the jar diameter plus room to lift it out; set drawer_w to the drawer's inside
// width less a few mm. Tier count is fixed at four by the profile.
//
// Print: each half standing on its cut end, the profile flat on the bed. Every wall is vertical,
// so nothing overhangs and it prints like a vase with walls; 2-3 perimeters, 10% infill (the walls
// are solid anyway). The coupler prints the same way. Halves are about 199 x 240 x 96 mm.
//
// BOM: 3 printed parts, nothing else. A dab of glue in the coupler is optional.
design SpiceDrawerInsert units mm

param drawer_w = 400 mm       // insert width across the drawer
param jar_d = 45 mm           // jar diameter
param tread_d = 60 mm         // depth of each row, front to back (>= jar_d + 10 mm)
param rise = 22 mm            // height step between rows
param lip_t = 6 mm          // > 2*wall, so the shell runs up inside it
param lip_h = 5 mm
param wall = 2 mm
param clear = 0.25 mm         // coupler fit, per side
param coupler_l = 40 mm

let depth = 4*tread_d
let d = wall + clear           // coupler inset from the outer profile

// ==== The stair profile, treads and lips (YZ sketch: first coordinate y, second z)
sketch Profile on YZ {
  polyline closed s = (0,0) (depth,0) (depth,4*rise) (3*tread_d + lip_t,4*rise) (3*tread_d + lip_t,4*rise + lip_h) (3*tread_d,4*rise + lip_h) (3*tread_d,3*rise) (2*tread_d + lip_t,3*rise) (2*tread_d + lip_t,3*rise + lip_h) (2*tread_d,3*rise + lip_h) (2*tread_d,2*rise) (tread_d + lip_t,2*rise) (tread_d + lip_t,2*rise + lip_h) (tread_d,2*rise + lip_h) (tread_d,rise) (lip_t,rise) (lip_t,rise + lip_h) (0,rise + lip_h)
  fix s
}
extrude Stair from Profile distance drawer_w new -> body Insert
shell Hollow faces (0, 2*tread_d, rise/2) (drawer_w, 2*tread_d, rise/2) thickness wall inside body Insert
split Halves target Insert origin (drawer_w/2,0,0) normal (1,0,0) -> body InsertLeft

// ==== Coupler: the inside of the stair (lips left out), inset by wall + clear
sketch CouplerSk on YZ offset (drawer_w/2 - coupler_l/2) {
  polyline closed c = (d,d) (depth - d,d) (depth - d,4*rise - d) (3*tread_d + d,4*rise - d) (3*tread_d + d,3*rise - d) (2*tread_d + d,3*rise - d) (2*tread_d + d,2*rise - d) (tread_d + d,2*rise - d) (tread_d + d,rise - d) (d,rise - d)
  fix c
}
extrude CouplerEx from CouplerSk distance coupler_l new -> body Coupler
shell CouplerHollow faces (drawer_w/2 - coupler_l/2, 2*tread_d, rise/2) (drawer_w/2 + coupler_l/2, 2*tread_d, rise/2) thickness wall inside body Coupler

color Insert, InsertLeft "#e9e4da"
color Coupler "#c46a3a"
material Insert, InsertLeft, Coupler PLA
