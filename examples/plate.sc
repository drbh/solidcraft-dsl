// The parametric plate from the solidcraft-modeling skill: 4 counterbored holes in a pattern.
// Expected volume 23335.2 mm³ with 22 faces; 35335.2 mm³ after L = 120.
design Plate units mm

param L = 80 mm
param W = 50 mm
param T = 6 mm
param inset = 8 mm

let half_L = L/2
let half_W = W/2
let hole_x = half_L - inset
let hole_y = half_W - inset

sketch PlateSketch on XY {
  rect center outline = (0,0) (half_L, half_W)
  dim outline.bottom = L
  dim outline.left = W
}

extrude Plate from PlateSketch distance T
fillet Corners edges (half_L, half_W, T/2) (-half_L, half_W, T/2) (half_L, -half_W, T/2) (-half_L, -half_W, T/2) radius 5

sketch HolePts on XY offset T {
  point h = (hole_x, hole_y)
  dim horizontal origin -> h = hole_x
  dim vertical   origin -> h = hole_y
}

hole Hole at HolePts diameter 3.4 counterbore 6.5 3.3
pattern Holes features Hole dir (-1,0,0) count 2 spacing (L - 2*inset) dir2 (0,-1,0) count2 2 spacing2 (W - 2*inset)
