// 3. OpenSCAD port of 03_bolt_plate.sc, kept for comparison.
// Same plate, derived spacing and 2x2 hole pattern. Units are implicitly mm.
L = 80;
W = 50;
T = 6;
inset = 10;
hole_d = 5;
hole_x = L - inset;
hole_y = W - inset;

// Small overshoot so the cut faces don't coincide with the plate faces.
eps = 0.01;
$fn = 64;

module first_hole() {
  // `hole ... depth T` drills from the top face (z = T) down to z = 0.
  translate([hole_x, hole_y, -eps]) cylinder(d = hole_d, h = T + 2 * eps);
}

difference() {
  cube([L, W, T]);
  // pattern: dir (-1,0,0) count 2 spacing L-2*inset, dir2 (0,-1,0) count2 2 spacing2 W-2*inset
  for (i = [0 : 1], j = [0 : 1])
    translate([-i * (L - 2 * inset), -j * (W - 2 * inset), 0]) first_hole();
}
