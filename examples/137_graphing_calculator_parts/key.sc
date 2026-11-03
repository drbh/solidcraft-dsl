// One keycap of 137_graphing_calculator: a cap through the face, a tapered flange that the face
// traps, and a plunger under it that presses a 6x6 tactile switch.
//
// Centred on the Z axis; z values are absolute (the calculator's frame), so the main design places
// it with `at (x, y, 0)`. The flange is tapered at 40 deg so the key prints top down with no
// supports: cap on the bed, flange and plunger above it.
design CalcKey units mm

param cw = 11.5 mm          // cap size
param ch = 6.8 mm
param corner = 1.2 mm
param lip = 1.2 mm          // how far the flange's foot stands out round the cap
param z0 = 21.3 mm          // underside of the flange
param flange_t = 1.2 mm
param top = 27 mm           // top of the cap
param stem_l = 1.5 mm       // plunger below the flange
param stem_d = 3.5 mm

sketch FlangeSk on XY offset z0 {
  rect rounded f = (-cw/2 - lip, -ch/2 - lip) (cw/2 + lip, ch/2 + lip) radius (corner + lip)
  fix f
}
extrude Flange from FlangeSk distance flange_t taper (-40 deg) new -> body Key
sketch CapSk on XY offset (z0 + flange_t - 0.3 mm) {
  rect rounded c = (-cw/2, -ch/2) (cw/2, ch/2) radius corner
  fix c
}
extrude Cap from CapSk distance (top - z0 - flange_t + 0.3 mm) join targets Key
cylinder Stem diameter stem_d height (stem_l + 0.3 mm) base (0, 0, z0 - stem_l) axis Z join targets Key

color Key "#2c2c30"
material Key PLA
