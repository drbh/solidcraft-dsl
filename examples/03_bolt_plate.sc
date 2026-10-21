// 3. Forward references, derived spacing and a two-direction hole pattern.
design BoltPlate units mm
let hole_x = L - inset
let hole_y = W - inset
param L = 80 mm
param W = 50 mm
param T = 6 mm
param inset = 10 mm
param hole_d = 5 mm
box Plate size L W T -> body PlateBody
sketch Locations on XY offset T {
  point h = (hole_x, hole_y)
  dim horizontal origin -> h = hole_x
  dim vertical origin -> h = hole_y
}
hole FirstHole at Locations diameter hole_d depth T
pattern Holes features FirstHole dir (-1,0,0) count (1+1) spacing (L-2*inset) dir2 (0,-1,0) count2 2 spacing2 (W-2*inset)
