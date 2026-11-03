// A 45-ish degree slope of 106_building_bricks: nx wide, two rows deep, one brick tall.
//
// Same grid and fits as brick.sc. The back row carries studs; the front row falls from the top
// to a one-plate lip. It is solid, with a socket under every cell for the studs below. The part
// starts at its cell corner (0, 0, 0), the slope facing -Y.
design Slope units mm

param nx = 2
param pitch = 10 mm
param plate_h = 4 mm
param stud_d = 6 mm
param stud_h = 2.2 mm
param fit = 0.1 mm
param gap = 0.1 mm

let h = 3*plate_h
let rise = (h - plate_h)/(pitch - gap)

box Block size (nx*pitch - 2*gap) (2*pitch - 2*gap) h corner (gap, gap, 0) new -> body Slope

// The slope: everything above the line from the lip (front, z = plate_h) to the back row's edge
sketch SlopeSk on YZ offset (-1 mm) {
  polyline closed o = (gap - 1 mm, plate_h - rise*1 mm) (pitch + 0.5 mm, h + rise*0.5 mm) (pitch + 0.5 mm, h + 5 mm) (gap - 1 mm, h + 5 mm)
  fix o
}
extrude SlopeCut from SlopeSk distance (nx*pitch + 2 mm) cut targets Slope

for i from 0 to 7 {
  if i < nx {
    cylinder Stud{i} diameter stud_d height (stud_h + 0.2 mm) base (pitch/2 + i*pitch, 1.5*pitch, h - 0.2 mm) join targets Slope
    cylinder SocketF{i} diameter (stud_d + 2*fit) height (stud_h + 0.5 mm) base (pitch/2 + i*pitch, pitch/2, -0.3 mm) cut targets Slope
    cylinder SocketB{i} diameter (stud_d + 2*fit) height (stud_h + 0.5 mm) base (pitch/2 + i*pitch, 1.5*pitch, -0.3 mm) cut targets Slope
  }
}

color Slope "#2f6fd2"
material Slope PLA
