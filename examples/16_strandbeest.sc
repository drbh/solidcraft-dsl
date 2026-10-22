// 16. Strandbeest walking module: four Theo Jansen legs on one crank, M3 screws at every joint.
//
// Generated from Jansen's holy numbers (a=38 b=41.5 c=39.3 d=40.1 e=55.8 f=39.4 g=36.7 h=65.7
// i=49 j=50 k=61.9 l=7.8 m=15 mm) solved at a 45 deg crank angle; the back legs run 180 deg out
// of phase. The linkage lies in XY; parts stack in Z on 3 mm layers with 0.4 mm running gaps.
// Per leg: link c (pivot-lower), triangle T1 (pivot, upper joint, outer joint), triangle T2
// (lower joint, knee, foot), link k (crank-lower), link f (outer-knee), link j (crank-upper).
// Holes: 3.4 mm where a part turns on the screw, 2.5 mm where the screw self-taps (frame, crank).
// Joints that move relative to everything use an M3 screw with a nylock nut.
//
// Hardware (26 screws, 16 nuts):
//   moving joints P1-P4    8 x M3x12 + 8 x M3x16, each with a nylock nut
//   left fixed pivots      2 x M3x12 into the frame (front and back share the pilot hole)
//   right fixed pivots     2 x M3x16 into the frame bosses
//   crank pins             2 x M3x20 into the crank pin bosses
//   axle ends              2 x M3x8 into the D-shaft, clamping the crank arms
//   frame halves           2 x M3x6 (front half clearance, back half pilot)
// Print: two plates (front parts, back parts), every part flat with bosses up; axle on its flat.
design Strandbeest units mm

// Frame, split at z=0 so each half prints flat with its pivot bosses up; the halves are
// clamped by the pivot screws and two M3 screws (clearance in the front half, pilot in the back)
sketch FrameFrontSk on XY offset (0) {
  slot bar = centers (-38,-7.8) (38,-7.8) width 18
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FrameFrontBar from FrameFrontSk distance 3 new -> body FrameFront
cylinder FrameFrontHub radius 12 height 3 base (0,0,(0)) join targets FrameFront
cylinder FrameFrontBossL radius 5 height 3.4 base (-38,-7.8,(3)) join targets FrameFront
cylinder FrameFrontBossR radius 5 height 10.2 base (38,-7.8,(3)) join targets FrameFront
cylinder FrameFrontPivotL radius 1.25 height 15.2 base (-38,-7.8,(-1)) cut targets FrameFront
cylinder FrameFrontPivotR radius 1.25 height 15.2 base (38,-7.8,(-1)) cut targets FrameFront
cylinder FrameFrontAxle radius 4.25 height 5 base (0,0,(-1)) cut targets FrameFront
cylinder FrameFrontMountL radius 1.7 height 5 base (-27,-7.8,(-1)) cut targets FrameFront
cylinder FrameFrontMountR radius 1.7 height 5 base (27,-7.8,(-1)) cut targets FrameFront
sketch FrameBackSk on XY offset (-3) {
  slot bar = centers (-38,-7.8) (38,-7.8) width 18
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FrameBackBar from FrameBackSk distance 3 new -> body FrameBack
cylinder FrameBackHub radius 12 height 3 base (0,0,(-3)) join targets FrameBack
cylinder FrameBackBossL radius 5 height 3.4 base (-38,-7.8,(-6.4)) join targets FrameBack
cylinder FrameBackBossR radius 5 height 10.2 base (38,-7.8,(-13.2)) join targets FrameBack
cylinder FrameBackPivotL radius 1.25 height 15.2 base (-38,-7.8,(-14.2)) cut targets FrameBack
cylinder FrameBackPivotR radius 1.25 height 15.2 base (38,-7.8,(-14.2)) cut targets FrameBack
cylinder FrameBackAxle radius 4.25 height 5 base (0,0,(-4)) cut targets FrameBack
cylinder FrameBackMountL radius 1.25 height 5 base (-27,-7.8,(-4)) cut targets FrameBack
cylinder FrameBackMountR radius 1.25 height 5 base (27,-7.8,(-4)) cut targets FrameBack

// CrankFront: arm on the crank layer, boss carrying the pin up to the legs' k links
sketch CrankFrontSk on XY offset (3.4) {
  slot bar = centers (0,0) (10.6066,10.6066) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude CrankFrontEx from CrankFrontSk distance 3 new -> body CrankFront
cylinder CrankFrontHub radius 6 height 3 base (0,0,(3.4)) join targets CrankFront
cylinder CrankFrontPinBoss radius 4 height 6.8 base (10.6066,10.6066,(6.4)) join targets CrankFront
cylinder CrankFrontPin radius 1.25 height 11.8 base (10.6066,10.6066,(2.4)) cut targets CrankFront
cylinder CrankFrontBore radius 4.1 height 5 base (0,0,(2.4)) cut targets CrankFront
box CrankFrontKey size 1.5 6 3 corner (3.3,-3,3.4) join targets CrankFront

// CrankBack: arm on the crank layer, boss carrying the pin up to the legs' k links
sketch CrankBackSk on XY offset (-6.4) {
  slot bar = centers (0,0) (-10.6066,-10.6066) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude CrankBackEx from CrankBackSk distance 3 new -> body CrankBack
cylinder CrankBackHub radius 6 height 3 base (0,0,(-6.4)) join targets CrankBack
cylinder CrankBackPinBoss radius 4 height 6.8 base (-10.6066,-10.6066,(-13.2)) join targets CrankBack
cylinder CrankBackPin radius 1.25 height 11.8 base (-10.6066,-10.6066,(-14.2)) cut targets CrankBack
cylinder CrankBackBore radius 4.1 height 5 base (0,0,(-7.4)) cut targets CrankBack
box CrankBackKey size 1.5 6 3 corner (3.3,-3,-6.4) join targets CrankBack

// Crank axle: prints lying on its D flat
cylinder AxleBody radius 4 height 12.8 base (0,0,(-6.4)) new -> body CrankAxle
box AxleFlat size 2 10 14.8 corner (3.2,-5,-7.4) cut targets CrankAxle
cylinder AxlePilotF radius 1.25 height 9 base (0,0,(-1.6)) cut targets CrankAxle
cylinder AxlePilotB radius 1.25 height 9 base (0,0,(-7.4)) cut targets CrankAxle

// Front left leg
sketch FLcSk on XY offset (6.8) {
  slot bar = centers (-38,-7.8) (-20.4268,-42.9521) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FLcEx from FLcSk distance 3 new -> body FLc
cylinder FLcH0 radius 1.7 height 5 base (-38,-7.8,(5.8)) cut targets FLc
cylinder FLcH1 radius 1.7 height 5 base (-20.4268,-42.9521,(5.8)) cut targets FLc
sketch FLT1Sk on XY offset (10.2) {
  polyline closed web = (-38,-7.8) (-33.8486,33.4918) (-77.5532,-1.2004)
  fix web.1 web.2 web.3
}
extrude FLT1Ex from FLT1Sk distance 3 new -> body FLT1
sketch FLT1Edge0Sk on XY offset (10.2) {
  slot bar = centers (-38,-7.8) (-33.8486,33.4918) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FLT1Edge0 from FLT1Edge0Sk distance 3 join targets FLT1
sketch FLT1Edge1Sk on XY offset (10.2) {
  slot bar = centers (-33.8486,33.4918) (-77.5532,-1.2004) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FLT1Edge1 from FLT1Edge1Sk distance 3 join targets FLT1
sketch FLT1Edge2Sk on XY offset (10.2) {
  slot bar = centers (-77.5532,-1.2004) (-38,-7.8) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FLT1Edge2 from FLT1Edge2Sk distance 3 join targets FLT1
cylinder FLT1Boss radius 4 height 3.4 base (-33.8486,33.4918,(13.2)) join targets FLT1
cylinder FLT1H0 radius 1.7 height 8.4 base (-38,-7.8,(9.2)) cut targets FLT1
cylinder FLT1H1 radius 1.7 height 8.4 base (-33.8486,33.4918,(9.2)) cut targets FLT1
cylinder FLT1H2 radius 1.7 height 8.4 base (-77.5532,-1.2004,(9.2)) cut targets FLT1
sketch FLT2Sk on XY offset (10.2) {
  polyline closed web = (-20.4268,-42.9521) (-56.0757,-34.2319) (-24.3985,-91.7909)
  fix web.1 web.2 web.3
}
extrude FLT2Ex from FLT2Sk distance 3 new -> body FLT2
sketch FLT2Edge0Sk on XY offset (10.2) {
  slot bar = centers (-20.4268,-42.9521) (-56.0757,-34.2319) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FLT2Edge0 from FLT2Edge0Sk distance 3 join targets FLT2
sketch FLT2Edge1Sk on XY offset (10.2) {
  slot bar = centers (-56.0757,-34.2319) (-24.3985,-91.7909) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FLT2Edge1 from FLT2Edge1Sk distance 3 join targets FLT2
sketch FLT2Edge2Sk on XY offset (10.2) {
  slot bar = centers (-24.3985,-91.7909) (-20.4268,-42.9521) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FLT2Edge2 from FLT2Edge2Sk distance 3 join targets FLT2
cylinder FLT2H0 radius 1.7 height 5 base (-20.4268,-42.9521,(9.2)) cut targets FLT2
cylinder FLT2H1 radius 1.7 height 5 base (-56.0757,-34.2319,(9.2)) cut targets FLT2
sketch FLkSk on XY offset (13.6) {
  slot bar = centers (10.6066,10.6066) (-20.4268,-42.9521) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FLkEx from FLkSk distance 3 new -> body FLk
cylinder FLkH0 radius 1.7 height 5 base (10.6066,10.6066,(12.6)) cut targets FLk
cylinder FLkH1 radius 1.7 height 5 base (-20.4268,-42.9521,(12.6)) cut targets FLk
sketch FLfSk on XY offset (13.6) {
  slot bar = centers (-77.5532,-1.2004) (-56.0757,-34.2319) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FLfEx from FLfSk distance 3 new -> body FLf
cylinder FLfH0 radius 1.7 height 5 base (-77.5532,-1.2004,(12.6)) cut targets FLf
cylinder FLfH1 radius 1.7 height 5 base (-56.0757,-34.2319,(12.6)) cut targets FLf
sketch FLjSk on XY offset (17) {
  slot bar = centers (10.6066,10.6066) (-33.8486,33.4918) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FLjEx from FLjSk distance 3 new -> body FLj
cylinder FLjH0 radius 1.7 height 5 base (10.6066,10.6066,(16)) cut targets FLj
cylinder FLjH1 radius 1.7 height 5 base (-33.8486,33.4918,(16)) cut targets FLj

// Front right leg
sketch FRcSk on XY offset (13.6) {
  slot bar = centers (38,-7.8) (33.6224,-46.8554) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FRcEx from FRcSk distance 3 new -> body FRc
cylinder FRcH0 radius 1.7 height 5 base (38,-7.8,(12.6)) cut targets FRc
cylinder FRcH1 radius 1.7 height 5 base (33.6224,-46.8554,(12.6)) cut targets FRc
sketch FRT1Sk on XY offset (17) {
  polyline closed web = (38,-7.8) (57.079,29.0543) (74.7354,-23.8785)
  fix web.1 web.2 web.3
}
extrude FRT1Ex from FRT1Sk distance 3 new -> body FRT1
sketch FRT1Edge0Sk on XY offset (17) {
  slot bar = centers (38,-7.8) (57.079,29.0543) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FRT1Edge0 from FRT1Edge0Sk distance 3 join targets FRT1
sketch FRT1Edge1Sk on XY offset (17) {
  slot bar = centers (57.079,29.0543) (74.7354,-23.8785) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FRT1Edge1 from FRT1Edge1Sk distance 3 join targets FRT1
sketch FRT1Edge2Sk on XY offset (17) {
  slot bar = centers (74.7354,-23.8785) (38,-7.8) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FRT1Edge2 from FRT1Edge2Sk distance 3 join targets FRT1
cylinder FRT1Boss radius 4 height 3.4 base (57.079,29.0543,(20)) join targets FRT1
cylinder FRT1H0 radius 1.7 height 8.4 base (38,-7.8,(16)) cut targets FRT1
cylinder FRT1H1 radius 1.7 height 8.4 base (57.079,29.0543,(16)) cut targets FRT1
cylinder FRT1H2 radius 1.7 height 8.4 base (74.7354,-23.8785,(16)) cut targets FRT1
sketch FRT2Sk on XY offset (17) {
  polyline closed web = (33.6224,-46.8554) (66.8314,-62.4776) (6.017,-87.3393)
  fix web.1 web.2 web.3
}
extrude FRT2Ex from FRT2Sk distance 3 new -> body FRT2
sketch FRT2Edge0Sk on XY offset (17) {
  slot bar = centers (33.6224,-46.8554) (66.8314,-62.4776) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FRT2Edge0 from FRT2Edge0Sk distance 3 join targets FRT2
sketch FRT2Edge1Sk on XY offset (17) {
  slot bar = centers (66.8314,-62.4776) (6.017,-87.3393) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FRT2Edge1 from FRT2Edge1Sk distance 3 join targets FRT2
sketch FRT2Edge2Sk on XY offset (17) {
  slot bar = centers (6.017,-87.3393) (33.6224,-46.8554) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FRT2Edge2 from FRT2Edge2Sk distance 3 join targets FRT2
cylinder FRT2H0 radius 1.7 height 5 base (33.6224,-46.8554,(16)) cut targets FRT2
cylinder FRT2H1 radius 1.7 height 5 base (66.8314,-62.4776,(16)) cut targets FRT2
sketch FRkSk on XY offset (20.4) {
  slot bar = centers (10.6066,10.6066) (33.6224,-46.8554) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FRkEx from FRkSk distance 3 new -> body FRk
cylinder FRkH0 radius 1.7 height 5 base (10.6066,10.6066,(19.4)) cut targets FRk
cylinder FRkH1 radius 1.7 height 5 base (33.6224,-46.8554,(19.4)) cut targets FRk
sketch FRfSk on XY offset (20.4) {
  slot bar = centers (74.7354,-23.8785) (66.8314,-62.4776) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FRfEx from FRfSk distance 3 new -> body FRf
cylinder FRfH0 radius 1.7 height 5 base (74.7354,-23.8785,(19.4)) cut targets FRf
cylinder FRfH1 radius 1.7 height 5 base (66.8314,-62.4776,(19.4)) cut targets FRf
sketch FRjSk on XY offset (23.8) {
  slot bar = centers (10.6066,10.6066) (57.079,29.0543) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FRjEx from FRjSk distance 3 new -> body FRj
cylinder FRjH0 radius 1.7 height 5 base (10.6066,10.6066,(22.8)) cut targets FRj
cylinder FRjH1 radius 1.7 height 5 base (57.079,29.0543,(22.8)) cut targets FRj

// Back left leg
sketch BLcSk on XY offset (-9.8) {
  slot bar = centers (-38,-7.8) (-68.4508,-32.6443) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BLcEx from BLcSk distance 3 new -> body BLc
cylinder BLcH0 radius 1.7 height 5 base (-38,-7.8,(-10.8)) cut targets BLc
cylinder BLcH1 radius 1.7 height 5 base (-68.4508,-32.6443,(-10.8)) cut targets BLc
sketch BLT1Sk on XY offset (-13.2) {
  polyline closed web = (-38,-7.8) (-34.1222,33.5184) (-77.596,-1.4625)
  fix web.1 web.2 web.3
}
extrude BLT1Ex from BLT1Sk distance 3 new -> body BLT1
sketch BLT1Edge0Sk on XY offset (-13.2) {
  slot bar = centers (-38,-7.8) (-34.1222,33.5184) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BLT1Edge0 from BLT1Edge0Sk distance 3 join targets BLT1
sketch BLT1Edge1Sk on XY offset (-13.2) {
  slot bar = centers (-34.1222,33.5184) (-77.596,-1.4625) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BLT1Edge1 from BLT1Edge1Sk distance 3 join targets BLT1
sketch BLT1Edge2Sk on XY offset (-13.2) {
  slot bar = centers (-77.596,-1.4625) (-38,-7.8) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BLT1Edge2 from BLT1Edge2Sk distance 3 join targets BLT1
cylinder BLT1Boss radius 4 height 3.4 base (-34.1222,33.5184,(-16.6)) join targets BLT1
cylinder BLT1H0 radius 1.7 height 8.4 base (-38,-7.8,(-17.6)) cut targets BLT1
cylinder BLT1H1 radius 1.7 height 8.4 base (-34.1222,33.5184,(-17.6)) cut targets BLT1
cylinder BLT1H2 radius 1.7 height 8.4 base (-77.596,-1.4625,(-17.6)) cut targets BLT1
sketch BLT2Sk on XY offset (-13.2) {
  polyline closed web = (-68.4508,-32.6443) (-105.0354,-29.7368) (-64.5616,-81.4897)
  fix web.1 web.2 web.3
}
extrude BLT2Ex from BLT2Sk distance 3 new -> body BLT2
sketch BLT2Edge0Sk on XY offset (-13.2) {
  slot bar = centers (-68.4508,-32.6443) (-105.0354,-29.7368) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BLT2Edge0 from BLT2Edge0Sk distance 3 join targets BLT2
sketch BLT2Edge1Sk on XY offset (-13.2) {
  slot bar = centers (-105.0354,-29.7368) (-64.5616,-81.4897) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BLT2Edge1 from BLT2Edge1Sk distance 3 join targets BLT2
sketch BLT2Edge2Sk on XY offset (-13.2) {
  slot bar = centers (-64.5616,-81.4897) (-68.4508,-32.6443) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BLT2Edge2 from BLT2Edge2Sk distance 3 join targets BLT2
cylinder BLT2H0 radius 1.7 height 5 base (-68.4508,-32.6443,(-14.2)) cut targets BLT2
cylinder BLT2H1 radius 1.7 height 5 base (-105.0354,-29.7368,(-14.2)) cut targets BLT2
sketch BLkSk on XY offset (-16.6) {
  slot bar = centers (-10.6066,-10.6066) (-68.4508,-32.6443) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BLkEx from BLkSk distance 3 new -> body BLk
cylinder BLkH0 radius 1.7 height 5 base (-10.6066,-10.6066,(-17.6)) cut targets BLk
cylinder BLkH1 radius 1.7 height 5 base (-68.4508,-32.6443,(-17.6)) cut targets BLk
sketch BLfSk on XY offset (-16.6) {
  slot bar = centers (-77.596,-1.4625) (-105.0354,-29.7368) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BLfEx from BLfSk distance 3 new -> body BLf
cylinder BLfH0 radius 1.7 height 5 base (-77.596,-1.4625,(-17.6)) cut targets BLf
cylinder BLfH1 radius 1.7 height 5 base (-105.0354,-29.7368,(-17.6)) cut targets BLf
sketch BLjSk on XY offset (-20) {
  slot bar = centers (-10.6066,-10.6066) (-34.1222,33.5184) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BLjEx from BLjSk distance 3 new -> body BLj
cylinder BLjH0 radius 1.7 height 5 base (-10.6066,-10.6066,(-21)) cut targets BLj
cylinder BLjH1 radius 1.7 height 5 base (-34.1222,33.5184,(-21)) cut targets BLj

// Back right leg
sketch BRcSk on XY offset (-16.6) {
  slot bar = centers (38,-7.8) (39.4103,-47.0747) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BRcEx from BRcSk distance 3 new -> body BRc
cylinder BRcH0 radius 1.7 height 5 base (38,-7.8,(-17.6)) cut targets BRc
cylinder BRcH1 radius 1.7 height 5 base (39.4103,-47.0747,(-17.6)) cut targets BRc
sketch BRT1Sk on XY offset (-20) {
  polyline closed web = (38,-7.8) (19.4719,29.3344) (72.6404,12.4004)
  fix web.1 web.2 web.3
}
extrude BRT1Ex from BRT1Sk distance 3 new -> body BRT1
sketch BRT1Edge0Sk on XY offset (-20) {
  slot bar = centers (38,-7.8) (19.4719,29.3344) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BRT1Edge0 from BRT1Edge0Sk distance 3 join targets BRT1
sketch BRT1Edge1Sk on XY offset (-20) {
  slot bar = centers (19.4719,29.3344) (72.6404,12.4004) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BRT1Edge1 from BRT1Edge1Sk distance 3 join targets BRT1
sketch BRT1Edge2Sk on XY offset (-20) {
  slot bar = centers (72.6404,12.4004) (38,-7.8) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BRT1Edge2 from BRT1Edge2Sk distance 3 join targets BRT1
cylinder BRT1Boss radius 4 height 3.4 base (19.4719,29.3344,(-23.4)) join targets BRT1
cylinder BRT1H0 radius 1.7 height 8.4 base (38,-7.8,(-24.4)) cut targets BRT1
cylinder BRT1H1 radius 1.7 height 8.4 base (19.4719,29.3344,(-24.4)) cut targets BRT1
cylinder BRT1H2 radius 1.7 height 8.4 base (72.6404,12.4004,(-24.4)) cut targets BRT1
sketch BRT2Sk on XY offset (-20) {
  polyline closed web = (39.4103,-47.0747) (70.0783,-26.9162) (59.513,-91.7612)
  fix web.1 web.2 web.3
}
extrude BRT2Ex from BRT2Sk distance 3 new -> body BRT2
sketch BRT2Edge0Sk on XY offset (-20) {
  slot bar = centers (39.4103,-47.0747) (70.0783,-26.9162) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BRT2Edge0 from BRT2Edge0Sk distance 3 join targets BRT2
sketch BRT2Edge1Sk on XY offset (-20) {
  slot bar = centers (70.0783,-26.9162) (59.513,-91.7612) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BRT2Edge1 from BRT2Edge1Sk distance 3 join targets BRT2
sketch BRT2Edge2Sk on XY offset (-20) {
  slot bar = centers (59.513,-91.7612) (39.4103,-47.0747) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BRT2Edge2 from BRT2Edge2Sk distance 3 join targets BRT2
cylinder BRT2H0 radius 1.7 height 5 base (39.4103,-47.0747,(-21)) cut targets BRT2
cylinder BRT2H1 radius 1.7 height 5 base (70.0783,-26.9162,(-21)) cut targets BRT2
sketch BRkSk on XY offset (-23.4) {
  slot bar = centers (-10.6066,-10.6066) (39.4103,-47.0747) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BRkEx from BRkSk distance 3 new -> body BRk
cylinder BRkH0 radius 1.7 height 5 base (-10.6066,-10.6066,(-24.4)) cut targets BRk
cylinder BRkH1 radius 1.7 height 5 base (39.4103,-47.0747,(-24.4)) cut targets BRk
sketch BRfSk on XY offset (-23.4) {
  slot bar = centers (72.6404,12.4004) (70.0783,-26.9162) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BRfEx from BRfSk distance 3 new -> body BRf
cylinder BRfH0 radius 1.7 height 5 base (72.6404,12.4004,(-24.4)) cut targets BRf
cylinder BRfH1 radius 1.7 height 5 base (70.0783,-26.9162,(-24.4)) cut targets BRf
sketch BRjSk on XY offset (-26.8) {
  slot bar = centers (-10.6066,-10.6066) (19.4719,29.3344) width 8
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude BRjEx from BRjSk distance 3 new -> body BRj
cylinder BRjH0 radius 1.7 height 5 base (-10.6066,-10.6066,(-27.8)) cut targets BRj
cylinder BRjH1 radius 1.7 height 5 base (19.4719,29.3344,(-27.8)) cut targets BRj

