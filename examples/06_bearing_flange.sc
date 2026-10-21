// 6. Joined solids, a through bore and circularly patterned bolt holes.
design BearingFlange units mm
param flange_r = 30 mm
param T = 8 mm
param boss_r = 12 mm
param boss_h = 6 mm
param bore_r = 4 mm
param bolt_circle_r = 22 mm
param hole_r = 2.5 mm
param bolts = 6
cylinder Flange radius flange_r height T -> body FlangeBody
cylinder Boss radius boss_r height boss_h base (0,0,T) join targets FlangeBody
hole Bore at (0,0,T+boss_h) diameter (2*bore_r) depth (T+boss_h)
sketch Bolts on XY offset T {
  point h = (bolt_circle_r,0)
  dim horizontal origin -> h = bolt_circle_r
  coincident h x
}
hole BoltHole at Bolts diameter (2*hole_r) depth T
pattern circular BoltRing features BoltHole axis Z count bolts angle 360 deg
