// One brick or plate of 106_building_bricks: nx by ny studs, `units` plates tall.
//
// The cell is `pitch` square and a plate is `plate_h` tall (3 plates make a brick). The part
// starts at its cell corner (0, 0, 0) and stands on z = 0. Studs on top fit the sockets
// underneath: the walls inside and the tubes between studs touch each stud `fit` off, and
// neighbouring bricks leave `gap` per side.
design Brick units mm

param nx = 2
param ny = 4
param units = 3
param pitch = 10 mm
param plate_h = 4 mm
param stud_d = 6 mm
param stud_h = 2.2 mm
param fit = 0.1 mm
param gap = 0.1 mm
param top_t = 1.2 mm

let h = units*plate_h
let wall = pitch/2 - stud_d/2 - gap - fit
let tube_od = 2*(pitch*sqrt(2)/2 - stud_d/2 - fit)

box Block size (nx*pitch - 2*gap) (ny*pitch - 2*gap) h corner (gap, gap, 0) new -> body Brick
box Cavity size (nx*pitch - 2*gap - 2*wall) (ny*pitch - 2*gap - 2*wall) (h - top_t + 0.3 mm) corner (gap + wall, gap + wall, -0.3 mm) cut targets Brick

// Studs on top, and tubes between every four studs underneath (none in a single row). The
// loops run to the largest size (8 x 8) and keep the cells inside nx by ny.
for i from 0 to 7 {
  for j from 0 to 7 {
    if i < nx {
      if j < ny {
        cylinder Stud{i}_{j} diameter stud_d height (stud_h + 0.2 mm) base (pitch/2 + i*pitch, pitch/2 + j*pitch, h - 0.2 mm) join targets Brick
      }
    }
    if i < nx - 1 {
      if j < ny - 1 {
        cylinder Tube{i}_{j} diameter tube_od height (h - top_t + 0.3 mm) base ((i + 1)*pitch, (j + 1)*pitch, 0) join targets Brick
        cylinder Bore{i}_{j} diameter (tube_od - 2.4 mm) height (h - top_t - 0.2 mm) base ((i + 1)*pitch, (j + 1)*pitch, -0.3 mm) cut targets Brick
      }
    }
  }
}

color Brick "#d23b2f"
material Brick PLA
