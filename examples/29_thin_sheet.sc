// 29. Thin perfboard, 6 in x 2 in on a 0.1 in (2.54 mm) grid, for pushing header pins through.
// The sheet is one 0.2 mm layer (the default first layer for a 0.4 mm nozzle). Holes sit half a
// pitch in from each edge, so the grid is 60 x 20 and boards tile edge to edge on the same pitch.
// Header pins are 0.64 mm square (0.9 mm across the corners); hole_d leaves room for the first
// layer squishing into the hole. Lower it for a tighter grip.
design ThinPerfboard units mm
param L = 6 in
param W = 2 in
param T = 0.2 mm
param pitch = 2.54 mm
param hole_d = 1.0 mm
let cols = 60
let rows = 20
box Sheet size L W T corner (0,0,0) -> body Sheet
// Two seeds, each patterned over half the rows: one 60 x 20 pattern is over the engine's
// instance limit (somewhere between 1000 and 1200).
let half = rows/2
sketch Locations on XY offset T {
  point a = (pitch/2, pitch/2)
  dim horizontal origin -> a = pitch/2
  dim vertical origin -> a = pitch/2
  point b = (pitch/2, pitch/2 + half*pitch)
  dim horizontal origin -> b = pitch/2
  dim vertical origin -> b = pitch/2 + half*pitch
}
hole SeedHoles at Locations diameter hole_d depth T
pattern Grid features SeedHoles dir (1,0,0) count cols spacing pitch dir2 (0,1,0) count2 half spacing2 pitch
