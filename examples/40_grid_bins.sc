// 40. Gridfinity-compatible storage bin (2 x 1 x 3u) and a matching 3 x 2 baseplate.
//
// Origin is the corner of the grid, cells are 42 mm on X and Y, Z is up from the bed. The bin and
// the baseplate are modelled side by side (the plate starts 20 mm past the bin along X).
// Bin: each grid cell gets the stepped base profile of the open Gridfinity spec, from the bed up:
// 35.6 mm (r 0.8) widening 0.8 at 45 deg, 1.8 straight, then 2.15 at 45 deg to 41.5 mm (r 3.75)
// at 4.75 mm. The body above is grid*42 - 0.5 mm square (0.25 mm clearance a side) with r 3.75
// corners and walls wall_t thick, up to units*7 mm. The stacking lip on top repeats the baseplate
// profile from inside (0.7 at 45 deg, 1.8 straight, 1.9 at 45 deg) so another bin's base locates
// in it, carried by a 45 deg chamfer that grows out of the wall. Each cell has four 6.5 x 2.4 mm
// pockets for 6 x 2 mm magnets, 13 mm from the cell centre (26 mm apart).
// Baseplate: the same profile cut down through a plate 4.65 mm high (0.7 at 45 deg, 1.8 straight,
// 2.15 at 45 deg up to 42 mm, r 4), open at the bottom, as the lightweight spec plate.
//
// Deviations from the spec: the base profile's top chamfer stops 0.25 mm short (41.0 mm at
// 4.5 mm, then the 41.5 mm body starts with a 0.25 mm step), and there are ~0.1 mm steps where the
// bottom chamfer meets the straight part (bin z = 0.7 mm, plate z = 0.55 mm); these are modelling
// workarounds (see the comments at the cuts). The lip and the plate's ridges would come to a knife
// edge; here the lip is lip_flat lower and the plate ridge_flat lower, leaving flats that print
// cleanly (the lip's top opening is 41.5 - 2*lip_flat, the plate's cells 42 - 2*ridge_flat).
// The bin's floor is solid above the cells (no screw holes, no scoop or label shelf). Magnet pockets
// cannot be switched off from a parameter (the DSL has no conditionals): delete the MagnetPocket
// lines and its name from the pattern for a bin without them.
//
// Print both open side up, no supports. Every slope is 45 deg, the bin's base chamfers lean out
// from the bed, the lip's underside is a 45 deg chamfer, and the only bridges are the 6.5 mm
// magnet pocket ceilings and the 0.5 mm gaps between cells under the body.
design GridBins units mm

// Bin size in grid units
param grid_x = 2
param grid_y = 1
param units = 3

// Gridfinity standard
param pitch = 42 mm
param unit_h = 7 mm
param bin_gap = 0.5 mm
param wall_t = 1.2 mm
param floor_t = 1.2 mm
param lip_flat = 0.4 mm
param magnet_d = 6.5 mm
param magnet_h = 2.4 mm
param magnet_off = 13 mm

// Baseplate size in grid units
param plate_x = 3
param plate_y = 2
param ridge_flat = 0.25 mm

let base_h = 4.75 mm
let top_z = units*unit_h
let lip_h = 4.4 mm - lip_flat
let bin_r = 3.75 mm
let bx = grid_x*pitch
let by = grid_y*pitch
let h = bin_gap/2
let c = pitch/2

// Body from the top of the cells (see below) to the top of the lip
sketch BodySk on XY offset (base_h-0.25 mm) {
  rect rounded a = (h,h) (bx-h,by-h) radius bin_r
  fix a.bottom a.right a.top a.left a.arc1 a.arc2 a.arc3 a.arc4
}
extrude Body from BodySk distance (top_z+lip_h-base_h+0.25 mm) -> body Bin

// One cell's base profile at the first cell, top step first so each piece joins onto the body,
// then patterned over the grid. The straight step starts at 0.7 mm and the bottom chamfer stops
// inside it at 0.75 mm, so the two never share an edge (a 0.1 mm step at z = 0.7 mm). Likewise
// the body starts at 4.5 mm, 0.25 mm below the spec's 4.75 mm, and the top chamfer runs on into it
// to 4.6 mm, so it crosses the body's underside instead of meeting its edge: a cell whose top face
// sat flush on the body's underside joined into a wrong, oversized solid.
sketch Cell2Sk on XY offset 2.6 mm {
  rect rounded a = (c-18.6 mm,c-18.6 mm) (c+18.6 mm,c+18.6 mm) radius 1.6 mm
  fix a.bottom a.right a.top a.left a.arc1 a.arc2 a.arc3 a.arc4
}
extrude Cell2 from Cell2Sk distance 2 mm taper 45 deg join targets Bin
sketch Cell1Sk on XY offset 0.7 mm {
  rect rounded a = (c-18.6 mm,c-18.6 mm) (c+18.6 mm,c+18.6 mm) radius 1.6 mm
  fix a.bottom a.right a.top a.left a.arc1 a.arc2 a.arc3 a.arc4
}
extrude Cell1 from Cell1Sk distance 1.9 mm join targets Bin
sketch Cell0Sk on XY {
  rect rounded a = (c-17.8 mm,c-17.8 mm) (c+17.8 mm,c+17.8 mm) radius 0.8 mm
  fix a.bottom a.right a.top a.left a.arc1 a.arc2 a.arc3 a.arc4
}
extrude Cell0 from Cell0Sk distance 0.75 mm taper 45 deg join targets Bin

// Magnet pockets, open to the bed
sketch MagnetSk on XY offset (-1 mm) {
  circle m0 = (c-magnet_off,c-magnet_off) diameter magnet_d
  circle m1 = (c+magnet_off,c-magnet_off) diameter magnet_d
  circle m2 = (c+magnet_off,c+magnet_off) diameter magnet_d
  circle m3 = (c-magnet_off,c+magnet_off) diameter magnet_d
  fix m0 m1 m2 m3
}
extrude MagnetPocket from MagnetSk distance (magnet_h+1 mm) cut targets Bin
pattern Cells features Cell2, Cell1, Cell0, MagnetPocket dir (1,0,0) count grid_x spacing pitch dir2 (0,1,0) count2 grid_y spacing2 pitch

// Cavity and stacking lip, built from straight cuts and chamfers (the chamfers follow the whole
// rounded outline): first the lip's narrowest opening (inset li) and the opening above top_z
// (inset h + 0.7), a concave chamfer fills the step between them into the lip's 45 deg slope and
// a convex one makes the top slope. Then the cavity (inset wi) up to top_z - (li - wi), and a
// chamfer on the overhanging step's inner edge turns it into the 45 deg support under the lip.
let wi = h + wall_t
let li = h + 2.6 mm
let ui = h + 0.7 mm
let sup_z = top_z - (li - wi)
sketch LipNeckSk on XY offset (sup_z-1 mm) {
  rect rounded a = (li,li) (bx-li,by-li) radius (bin_r-2.6 mm)
  fix a.bottom a.right a.top a.left a.arc1 a.arc2 a.arc3 a.arc4
}
extrude LipNeck from LipNeckSk distance (lip_h+(li-wi)+2 mm) cut targets Bin
sketch LipMouthSk on XY offset top_z {
  rect rounded a = (ui,ui) (bx-ui,by-ui) radius (bin_r-0.7 mm)
  fix a.bottom a.right a.top a.left a.arc1 a.arc2 a.arc3 a.arc4
}
extrude LipMouth from LipMouthSk distance (lip_h+1 mm) cut targets Bin
chamfer LipSlope edges (bx/2,ui,top_z) distance 1.9 mm body Bin
chamfer LipTop edges (bx/2,ui,top_z+lip_h) distance (0.7 mm-lip_flat) body Bin
sketch CavitySk on XY offset (base_h+floor_t) {
  rect rounded a = (wi,wi) (bx-wi,by-wi) radius (bin_r-wall_t)
  fix a.bottom a.right a.top a.left a.arc1 a.arc2 a.arc3 a.arc4
}
extrude Cavity from CavitySk distance (sup_z-base_h-floor_t) cut targets Bin
chamfer LipSupport edges (bx/2,li,sup_z) distance (li-wi) body Bin

// Baseplate, placed beside the bin
let px0 = bx + 20 mm
let pw = plate_x*pitch
let pd = plate_y*pitch
let plate_h = 4.65 mm - ridge_flat
sketch PlateSk on XY {
  rect rounded a = (px0,0) (px0+pw,pd) radius 4 mm
  fix a.bottom a.right a.top a.left a.arc1 a.arc2 a.arc3 a.arc4
}
extrude PlateBlock from PlateSk distance plate_h new -> body Baseplate
// One cell's pocket, bottom up: 36.3 (r 1.15), 0.7 at 45 deg, 1.8 straight, 45 deg to the top.
// Each cut overlaps the next (the slopes start or stop inside the straight part) so no two cuts
// share or nearly share an edge; that leaves a 0.11 mm step at z = 0.55 mm.
sketch Pocket0Sk on XY offset (-0.01 mm) {
  rect rounded a = (px0+c-18.15 mm,c-18.15 mm) (px0+c+18.15 mm,c+18.15 mm) radius 1.15 mm
  fix a.bottom a.right a.top a.left a.arc1 a.arc2 a.arc3 a.arc4
}
extrude Pocket0 from Pocket0Sk distance 0.6 mm taper 45 deg cut targets Baseplate
sketch Pocket1Sk on XY offset 0.55 mm {
  rect rounded a = (px0+c-18.85 mm,c-18.85 mm) (px0+c+18.85 mm,c+18.85 mm) radius 1.85 mm
  fix a.bottom a.right a.top a.left a.arc1 a.arc2 a.arc3 a.arc4
}
extrude Pocket1 from Pocket1Sk distance 2 mm cut targets Baseplate
sketch Pocket2Sk on XY offset 2.45 mm {
  rect rounded a = (px0+c-18.8 mm,c-18.8 mm) (px0+c+18.8 mm,c+18.8 mm) radius 1.8 mm
  fix a
}
extrude Pocket2 from Pocket2Sk distance (plate_h-2.45 mm+0.01 mm) taper 45 deg cut targets Baseplate
pattern Pockets features Pocket0, Pocket1, Pocket2 dir (1,0,0) count plate_x spacing pitch dir2 (0,1,0) count2 plate_y spacing2 pitch
