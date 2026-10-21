// 2. Calculated sketch seeds plus persistent dimensions. V = L*W*T.
design CenteredPlate units mm
param L = 40 mm
param W = 24 mm
param T = 4 mm
sketch Outline on XY {
  rect center rim = (0,0) (L/2, W/2)
  dim rim.bottom = L
  dim rim.left = W
}
extrude Plate from Outline distance T -> body PlateBody
