// 65. Domed control knob: a tapered grip closed by a smooth domed top, on a D-shaft.
//
// Built from surfaces: the side is a loft surface from the base circle to the rim, the top is a
// patch over the rim's edge loop (g1: it meets the side without a crease) that passes through
// the apex `dome_h` above the rim, the base is a flat patch of the base sketch, and `stitch` sews
// the three into one closed solid. That solid then takes ordinary cuts: the D-shaped bore for a
// 6 mm shaft.
//
// Print standing on its base, no supports.
design DomedKnob units mm
param base_r = 20 mm
param rim_r = 16 mm
param rim_z = 18 mm
param dome_h = 6 mm
param shaft_d = 6.2 mm
param flat = 4.7 mm
param bore_h = 14 mm

sketch Base on XY {
  circle c = (0,0) radius base_r
  fix c
}
sketch Rim on XY offset rim_z {
  circle c = (0,0) radius rim_r
  fix c
}
loft Side sections Base, Rim surface -> body Knob
// The rim's edge loop, picked all the way round
patch Dome edges (rim_r*cos(45 deg), rim_r*sin(45 deg), rim_z) (-rim_r*cos(45 deg), rim_r*sin(45 deg), rim_z) (-rim_r*cos(45 deg), -rim_r*sin(45 deg), rim_z) (rim_r*cos(45 deg), -rim_r*sin(45 deg), rim_z) of Knob continuity g1 through (0, 0, rim_z + dome_h) -> body DomeTop
patch Bottom from Base -> body BaseFace
stitch Close bodies Knob, DomeTop, BaseFace

// D-shaped bore: a round hole with a flat, from the base up
sketch Bore on XY offset (-1 mm) {
  circle c = (0,0) diameter shaft_d
  fix c
}
extrude BoreCut from Bore distance (bore_h + 1 mm) cut targets Knob
box Flat size shaft_d shaft_d bore_h corner (-shaft_d/2, flat - shaft_d/2, 0) join targets Knob

check Knob overhang <= 45 deg
