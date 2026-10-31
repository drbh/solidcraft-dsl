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
// Every hole is a circle in one sketch, cut with one extrude: much faster than patterning a hole
// feature, which cuts each copy into the sheet in turn (158 s on engine 0.28, against 6 s here;
// see docs/engine-performance.md). A sketch pattern makes at most 500 copies, so the 60 columns
// are three patterns of 20 x 20. The engine takes sketch pattern counts and spacings as numbers:
// they follow `pitch` when compiled, not when it is changed in SolidCraft.
sketch HoleGrid on XY offset (-1 mm) {
  circle h1 = (pitch/2, pitch/2) diameter hole_d
  circle h2 = (pitch/2 + cols/3*pitch, pitch/2) diameter hole_d
  circle h3 = (pitch/2 + 2*cols/3*pitch, pitch/2) diameter hole_d
  fix h1 h2 h3
  pattern rectangular h1 count (cols/3) spacing pitch count2 rows spacing2 pitch
  pattern rectangular h2 count (cols/3) spacing pitch count2 rows spacing2 pitch
  pattern rectangular h3 count (cols/3) spacing pitch count2 rows spacing2 pitch
}
extrude Holes from HoleGrid distance (T + 2 mm) cut targets Sheet
