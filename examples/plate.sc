// The parametric plate from the solidcraft-modeling skill: 4 counterbored holes in a pattern.
// Expected volume 23335.2 mm³ with 22 faces; 35335.2 mm³ after L = 120.
design Plate units mm

param L = 80 mm
param W = 50 mm
param T = 6 mm
param inset = 8 mm

sketch PlateSketch on XY {
  rect center outline = (0,0) (40,25)
  dim outline.bottom = L
  dim outline.left = W
}

extrude Plate from PlateSketch distance T
fillet Corners edges (40,25,3) (-40,25,3) (40,-25,3) (-40,-25,3) radius 5

sketch HolePts on XY offset T {
  point h = (32, 17)
  dim horizontal origin -> h = L/2 - inset
  dim vertical   origin -> h = W/2 - inset
}

hole Hole at HolePts diameter 3.4 counterbore 6.5 3.3
pattern Holes features Hole dir (-1,0,0) count 2 spacing (L - 2*inset) dir2 (0,-1,0) count2 2 spacing2 (W - 2*inset)
