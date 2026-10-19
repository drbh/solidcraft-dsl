// An L-bracket: a sketched profile extruded symmetrically, two holes from a point sketch.
design Bracket units mm

param L = 60 mm          // base length along X
param W = 40 mm          // width along Y
param T = 6 mm           // thickness
param H = 40 mm          // upright height
param hole_d = 6.4 mm    // M6 clearance
param hole_inset = 15 mm // holes from the free end
param hole_pitch = 20 mm // between the holes

sketch Profile on XZ {
  // XZ: sketch x is +X, sketch y is -Z, so heights are negative.
  polyline closed infer outline = (0,0) (60,0) (60,-6) (6,-6) (6,-40) (0,-40)
  dim outline.1 = L
  dim outline.2 = T
  dim outline.6 = H
  dim horizontal origin -> outline.3.end = T
}

extrude Body from Profile distance W symmetric -> body Bracket

sketch HolePts on XY offset T {
  point a = (45, 10)
  point b = (45, -10)
  dim horizontal origin -> a = L - hole_inset
  dim vertical   origin -> a = hole_pitch / 2
  dim horizontal origin -> b = L - hole_inset
  dim vertical   origin -> b = hole_pitch / 2
}

hole Holes at HolePts diameter hole_d
