// A cross axle of 110_beam_axle_set: `span` across the arms, `arm` thick, `len` long, along Z
// from z = 0, centred on the Z axis. It turns freely in the round holes and drives anything
// with a cross hole. Prints lying on two arms.
design Axle units mm

param len = 21 mm
param span = 5.6 mm
param arm = 2 mm

sketch CrossSk on XY {
  polyline closed o = (span/2, -arm/2) (span/2, arm/2) (arm/2, arm/2) (arm/2, span/2) (-arm/2, span/2) (-arm/2, arm/2) (-span/2, arm/2) (-span/2, -arm/2) (-arm/2, -arm/2) (-arm/2, -span/2) (arm/2, -span/2) (arm/2, -arm/2)
  fix o
}
extrude Rod from CrossSk distance len new -> body Axle

color Axle "#2a2a2a"
material Axle PLA
