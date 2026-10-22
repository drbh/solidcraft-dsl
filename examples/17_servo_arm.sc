// 17. Five-servo arm (MG995) with a table-edge clamp flange and a gear-driven gripper.
//
// Generated (gen_arm.py) in the straight-up pose: J1 base yaw, J2 shoulder, J3 elbow, J4 wrist
// pitch, J5 gripper. Links are flat plates; each bolts to a servo horn with the horn's own screws
// and holds the next servo by its ears (M3 self-tapping into 2.5 mm pilots): one screw through the
// shaft-end ear, and at the cable end one screw with a penny washer (M3 x 9 mm) beside the ear,
// clamping it, because every mount has an open channel there for the lead's boot and plug.
// Lower each servo in plug first. Servo bodies and
// horns are included as reference bodies (Servo*, Horn*) for clearance checks; do not print them.
// Servo dimensions are MG995 datasheet values: measure yours, especially the horn screw radius.
design ServoArm units mm

// Base: round plate with a tongue to the table edge (x = -60), pockets beside the yaw servo,
// a windowed drum carrying the turntable, and the yaw servo cradle
sketch BasePlateSk on XY {
  line top = (30.4631,54) (-60,54)
  line back = top.end (-60,(-54))
  line bottom = back.end (30.4631,(-54))
  arc front = start bottom.end through (62,0) end top.start
  fillet top.end radius 10
  fillet back.end radius 10
  fillet bottom.end radius 8
  fillet top.start radius 8
  fix top back bottom front
}
extrude BasePlate from BasePlateSk distance 4 -> body Base
sketch BaseTie0Sk on XY offset (-1) {
  rect rounded r = (51,(-10.1)) (56,(-7.9)) radius 1.05
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude BaseTie0 from BaseTie0Sk distance 6 cut targets Base
sketch BaseTie1Sk on XY offset (-1) {
  rect rounded r = (51,7.9) (56,10.1) radius 1.05
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude BaseTie1 from BaseTie1Sk distance 6 cut targets Base
hole BaseScrew0 at (-42,-42,4) diameter 4.5 countersink 9 mm 90 deg depth 4
hole BaseScrew1 at (-42,42,4) diameter 4.5 countersink 9 mm 90 deg depth 4
hole BaseScrew2 at (40,-40,4) diameter 4.5 countersink 9 mm 90 deg depth 4
hole BaseScrew3 at (40,40,4) diameter 4.5 countersink 9 mm 90 deg depth 4
// EdgeClamp: radiused lip with two windows, tab screwed onto the plate
sketch ClampLipSk on YZ offset (-65) {
  polyline closed edge = ((-50),(-32)) (50,(-32)) (50,9) ((-50),9)
  fillet edge.4.end radius 6
  fillet edge.1.end radius 6
  fillet edge.2.end radius 6
  fillet edge.3.end radius 6
  fix edge.1 edge.2 edge.3 edge.4
}
extrude ClampLip from ClampLipSk distance 5 new -> body EdgeClamp
sketch ClampTabSk on XY offset 4 {
  polyline closed edge = ((-65),(-50)) ((-48),(-50)) ((-48),50) ((-65),50)
  fillet edge.4.end radius 5
  fillet edge.1.end radius 5
  fillet edge.2.end radius 5
  fillet edge.3.end radius 5
  fix edge.1 edge.2 edge.3 edge.4
}
extrude ClampTab from ClampTabSk distance 5 join targets EdgeClamp
sketch ClampWin0Sk on YZ offset (-66) {
  rect rounded r = ((-22),(-27)) (22,(-6)) radius 5
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude ClampWin0 from ClampWin0Sk distance 7 cut targets EdgeClamp
cylinder ClampTableScrew0 radius 2.25 height 8 base ((-67),(-36),(-16)) axis X cut targets EdgeClamp
cylinder ClampTableScrew1 radius 2.25 height 8 base ((-67),36,(-16)) axis X cut targets EdgeClamp
cylinder ClampTabHole0 radius 1.7 height 7 base ((-56),(-32),3) axis Z cut targets EdgeClamp
cylinder BaseClampPilot0 radius 1.25 height 4 base ((-56),(-32),0.8) axis Z cut targets Base
cylinder ClampTabHole1 radius 1.7 height 7 base ((-56),32,3) axis Z cut targets EdgeClamp
cylinder BaseClampPilot1 radius 1.25 height 4 base ((-56),32,0.8) axis Z cut targets Base
// Drum: 2.6 mm wall, four radiused windows (the +X one is the cable exit), solid top ring
cylinder Drum radius 46.6 height 43 base (0,0,4) axis Z join targets Base
cylinder DrumBore radius 44 height 43.4 base (0,0,4) axis Z cut targets Base
sketch DrumWin0Sk on YZ offset 41 {
  rect rounded r = ((-21),8) (21,42) radius 7
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude DrumWin0 from DrumWin0Sk distance 6.6 cut targets Base
sketch DrumWin1Sk on YZ offset (-47.6) {
  rect rounded r = ((-21),8) (21,42) radius 7
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude DrumWin1 from DrumWin1Sk distance 6.6 cut targets Base
sketch DrumWin2Sk on XZ offset 41 {
  rect rounded r = ((-21),(-42)) (21,(-8)) radius 7
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude DrumWin2 from DrumWin2Sk distance 6.6 cut targets Base
sketch DrumWin3Sk on XZ offset (-47.6) {
  rect rounded r = ((-21),(-42)) (21,(-8)) radius 7
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude DrumWin3 from DrumWin3Sk distance 6.6 cut targets Base
sketch PlatePocketS0Sk on XY offset (-1) {
  polyline closed edge = ((-28.6329),(-23.4341)) ((-23.6003),(-28.496)) ((-17.6894),(-32.4974)) ((-11.1202),(-35.2894)) ((-4.1371),(-36.768)) (3,(-36.8782)) (3,(-17.5)) ((-32.5998),(-17.5))
  fillet edge.8.end radius 4
  fillet edge.1.end radius 4
  fillet edge.2.end radius 4
  fillet edge.3.end radius 4
  fillet edge.4.end radius 4
  fillet edge.5.end radius 3.1629
  fillet edge.6.end radius 4
  fillet edge.7.end radius 1.7164
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8
}
extrude PlatePocketS0 from PlatePocketS0Sk distance 6 cut targets Base
sketch PlatePocketS1Sk on XY offset (-1) {
  polyline closed edge = (17,(-32.8634)) (21.7257,(-29.9498)) (25.9624,(-26.362)) (29.6145,(-22.1807)) (32.5998,(-17.5)) (17,(-17.5))
  fillet edge.6.end radius 1.3947
  fillet edge.1.end radius 4
  fillet edge.2.end radius 4
  fillet edge.3.end radius 4
  fillet edge.4.end radius 1.3697
  fillet edge.5.end radius 4
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6
}
extrude PlatePocketS1 from PlatePocketS1Sk distance 6 cut targets Base
sketch PlatePocketN0Sk on XY offset (-1) {
  polyline closed edge = ((-32.5998),17.5) (3,17.5) (3,36.8782) ((-4.1371),36.768) ((-11.1202),35.2894) ((-17.6894),32.4974) ((-23.6003),28.496) ((-28.6329),23.4341)
  fillet edge.8.end radius 1.7164
  fillet edge.1.end radius 4
  fillet edge.2.end radius 3.1629
  fillet edge.3.end radius 4
  fillet edge.4.end radius 4
  fillet edge.5.end radius 4
  fillet edge.6.end radius 4
  fillet edge.7.end radius 4
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8
}
extrude PlatePocketN0 from PlatePocketN0Sk distance 6 cut targets Base
sketch PlatePocketN1Sk on XY offset (-1) {
  polyline closed edge = (17,17.5) (32.5998,17.5) (29.6145,22.1807) (25.9624,26.362) (21.7257,29.9498) (17,32.8634)
  fillet edge.6.end radius 4
  fillet edge.1.end radius 1.3697
  fillet edge.2.end radius 4
  fillet edge.3.end radius 4
  fillet edge.4.end radius 4
  fillet edge.5.end radius 1.3947
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6
}
extrude PlatePocketN1 from PlatePocketN1Sk distance 6 cut targets Base
box YawEnd0 size 7.65 26.3 27.6 corner ((-17.65),(-13.15),3) join targets Base
box YawEnd1 size 7.65 31.7 27.6 corner (30.3,(-15.85),3) join targets Base
box YawSideLip0 size 41.3 3.5 7 corner ((-10.5),(-13.15),3) join targets Base
box YawSideLip1 size 41.3 3.5 7 corner ((-10.5),9.65,3) join targets Base
box YawPocketCut size 41.3 20.3 27.6 corner ((-10.5),(-10.15),4) cut targets Base
box YawBootChannel size 9.65 9.2 27.6 corner (29.8,(-4.6),4) cut targets Base
cylinder YawEarPilot0 radius 1.25 height 13 base ((-14.6),(-5),18.6) axis Z cut targets Base
cylinder YawEarPilot1 radius 1.25 height 13 base (34.9,11.85,18.6) axis Z cut targets Base
box Servo1Case size 40.7 19.7 36.6 corner ((-10.2),(-9.85),4) new -> body Servo1
box Servo1Ears size 54.6 19.7 2.5 corner ((-17.15),(-9.85),30.6) join targets Servo1
box Servo1Boot size 5 8 7 corner (30,(-4),6) join targets Servo1
cylinder Servo1Boss radius 6.5 height 2.2 base (0,0,40.6) axis Z join targets Servo1
cylinder Servo1Spline radius 2.95 height 6.3 base (0,0,40.6) axis Z join targets Servo1
cylinder Horn1Disc radius 12 height 2.5 base (0,0,44.9) axis Z new -> body Horn1

// Turntable: round bearing disc with three radiused sector pockets; shoulder tower with gussets
cylinder TurntableDisc radius 50 height 5 base (0,0,47.4) axis Z new -> body Turntable
sketch TurntableYokePadSk on XY offset 47.4 {
  rect rounded r = ((-27),(-56.4)) (27,(-20)) radius 9
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude TurntableYokePad from TurntableYokePadSk distance 5 join targets Turntable
cylinder TurntableHornC radius 1.7 height 7 base (0,0,46.4) axis Z cut targets Turntable
cylinder TurntableHornH0 radius 1.2 height 7 base (5.3033,5.3033,46.4) axis Z cut targets Turntable
cylinder TurntableHornH1 radius 1.2 height 7 base ((-5.3033),(-5.3033),46.4) axis Z cut targets Turntable
sketch TurntablePocket0Sk on XY offset 46.4 {
  polyline closed edge = (14.1272,7.5115) (15.7882,2.5948) (15.7882,(-2.5948)) (14.1272,(-7.5115)) (35.3179,(-18.7789)) (37.8959,(-12.8025)) (39.4705,(-6.4871)) (40,0) (39.4705,6.4871) (37.8959,12.8025) (35.3179,18.7789)
  fillet edge.11.end radius 2.7505
  fillet edge.1.end radius 4
  fillet edge.2.end radius 4
  fillet edge.3.end radius 2.7505
  fillet edge.4.end radius 2.6996
  fillet edge.5.end radius 4
  fillet edge.6.end radius 4
  fillet edge.7.end radius 4
  fillet edge.8.end radius 4
  fillet edge.9.end radius 4
  fillet edge.10.end radius 2.6996
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11
}
extrude TurntablePocket0 from TurntablePocket0Sk distance 7 cut targets Turntable
sketch TurntablePocket1Sk on XY offset 46.4 {
  polyline closed edge = ((-7.5115),14.1272) ((-2.5948),15.7882) (2.5948,15.7882) (7.5115,14.1272) (18.7789,35.3179) (12.8025,37.8959) (6.4871,39.4705) (0,40) ((-6.4871),39.4705) ((-12.8025),37.8959) ((-18.7789),35.3179)
  fillet edge.11.end radius 2.7505
  fillet edge.1.end radius 4
  fillet edge.2.end radius 4
  fillet edge.3.end radius 2.7505
  fillet edge.4.end radius 2.6996
  fillet edge.5.end radius 4
  fillet edge.6.end radius 4
  fillet edge.7.end radius 4
  fillet edge.8.end radius 4
  fillet edge.9.end radius 4
  fillet edge.10.end radius 2.6996
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11
}
extrude TurntablePocket1 from TurntablePocket1Sk distance 7 cut targets Turntable
sketch TurntablePocket2Sk on XY offset 46.4 {
  polyline closed edge = ((-14.1272),(-7.5115)) ((-15.7882),(-2.5948)) ((-15.7882),2.5948) ((-14.1272),7.5115) ((-35.3179),18.7789) ((-37.8959),12.8025) ((-39.4705),6.4871) ((-40),0) ((-39.4705),(-6.4871)) ((-37.8959),(-12.8025)) ((-35.3179),(-18.7789))
  fillet edge.11.end radius 2.7505
  fillet edge.1.end radius 4
  fillet edge.2.end radius 4
  fillet edge.3.end radius 2.7505
  fillet edge.4.end radius 2.6996
  fillet edge.5.end radius 4
  fillet edge.6.end radius 4
  fillet edge.7.end radius 4
  fillet edge.8.end radius 4
  fillet edge.9.end radius 4
  fillet edge.10.end radius 2.6996
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11
}
extrude TurntablePocket2 from TurntablePocket2Sk distance 7 cut targets Turntable
sketch ShoulderTowerSk on XZ offset (-25.3) {
  polyline closed edge = ((-24),(-52.4)) (24,(-52.4)) (16,(-61.4)) (16,(-117.15)) ((-16),(-117.15)) ((-16),(-61.4))
  fillet edge.2.end radius 6
  fillet edge.3.end radius 6
  fillet edge.4.end radius 6
  fillet edge.5.end radius 6
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6
}
extrude ShoulderTower from ShoulderTowerSk distance 6 join targets Turntable
sketch IdlerTowerSk on XZ offset (-52.4) {
  polyline closed edge = ((-24),(-52.4)) (24,(-52.4)) (16,(-61.4)) (16,(-96)) (14.7821,(-102.1229)) (11.3137,(-107.3137)) (6.1229,(-110.7821)) (0,(-112)) ((-6.1229),(-110.7821)) ((-11.3137),(-107.3137)) ((-14.7821),(-102.1229)) ((-16),(-96)) ((-16),(-61.4))
  fillet edge.2.end radius 6
  fillet edge.3.end radius 6
  fillet edge.4.end radius 6
  fillet edge.5.end radius 6
  fillet edge.6.end radius 6
  fillet edge.7.end radius 6
  fillet edge.8.end radius 6
  fillet edge.9.end radius 6
  fillet edge.10.end radius 6
  fillet edge.11.end radius 6
  fillet edge.12.end radius 6
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11 edge.12 edge.13
}
extrude IdlerTower from IdlerTowerSk distance 5 join targets Turntable
box YokeWall0 size 3.5 27.1 48.1 corner ((-19.5),(-52),51.9) join targets Turntable
box YokeWall1 size 3.5 27.1 48.1 corner (16,(-52),51.9) join targets Turntable
cylinder IdlerBore radius 5.2 height 7 base (0,(-53.4),96) axis Y cut targets Turntable
sketch TurntableCableSk on XY offset 46.4 {
  rect rounded r = ((-8),(-44.4)) (8,(-28.3)) radius 4
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude TurntableCable from TurntableCableSk distance 7 cut targets Turntable
sketch TurntableTie0Sk on XY offset 46.4 {
  rect rounded r = ((-12.6),(-42.4)) ((-10.4),(-37.4)) radius 1.05
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude TurntableTie0 from TurntableTie0Sk distance 7 cut targets Turntable
sketch TurntableTie1Sk on XY offset 46.4 {
  rect rounded r = (10.4,(-42.4)) (12.6,(-37.4)) radius 1.05
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude TurntableTie1 from TurntableTie1Sk distance 7 cut targets Turntable
box TurntableWin2 size 20.3 8 41.3 corner ((-10.15),(-26.3),65.2) cut targets Turntable
sketch TurntableBoot2Sk on XZ offset (-26.3) {
  rect rounded r = ((-4.6),(-67.2)) (4.6,(-59.9)) radius 1.5
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude TurntableBoot2 from TurntableBoot2Sk distance 8 cut targets Turntable
cylinder TurntableEar20 radius 1.25 height 8 base ((-5),(-26.3),110.6) axis Y cut targets Turntable
cylinder TurntableEar21 radius 1.25 height 8 base (11.85,(-26.3),61.1) axis Y cut targets Turntable
box Servo2Case size 19.7 36.6 40.7 corner ((-9.85),(-45.9),65.5) new -> body Servo2
box Servo2Ears size 19.7 2.5 54.6 corner ((-9.85),(-19.3),58.55) join targets Servo2
box Servo2Boot size 8 7 5 corner ((-4),(-43.9),61) join targets Servo2
cylinder Servo2Boss radius 6.5 height 2.2 base (0,(-9.3),96) axis Y join targets Servo2
cylinder Servo2Spline radius 2.95 height 6.3 base (0,(-9.3),96) axis Y join targets Servo2
cylinder Horn2Disc radius 12 height 2.5 base (0,(-5),96) axis Y new -> body Horn2

// Upper arm: tapered plate, radiused web pockets, cable-tie slots; holds the elbow servo
sketch UpperArmBodySk on XZ offset (-2.5) {
  polyline closed edge = ((-8.68),(-82)) (8.68,(-82)) (14,(-89.7)) (16.5,(-152)) (16.5,(-205.4207)) (12.2596,(-210.5156)) (6.527,(-213.8437)) (0,(-215)) ((-6.527),(-213.8437)) ((-12.2596),(-210.5156)) ((-16.5),(-205.4207)) ((-16.5),(-152)) ((-14),(-89.7))
  fillet edge.13.end radius 7
  fillet edge.1.end radius 7
  fillet edge.2.end radius 7
  fillet edge.4.end radius 7
  fillet edge.5.end radius 7
  fillet edge.6.end radius 7
  fillet edge.7.end radius 7
  fillet edge.8.end radius 7
  fillet edge.9.end radius 7
  fillet edge.10.end radius 7
  fillet edge.12.end radius 7
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11 edge.12 edge.13
}
extrude UpperArmBody from UpperArmBodySk distance 5 new -> body UpperArm
cylinder UpperArmClevisHole0 radius 1.7 height 7 base ((-6),(-3.5),130) axis Y cut targets UpperArm
cylinder UpperArmClevisHole1 radius 1.7 height 7 base (6,(-3.5),142) axis Y cut targets UpperArm
sketch UpperArmTie0Sk on XZ offset (-3.5) {
  rect rounded r = ((-14.3),(-156)) ((-12.1),(-151)) radius 1.05
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude UpperArmTie0 from UpperArmTie0Sk distance 7 cut targets UpperArm
sketch UpperArmTie1Sk on XZ offset (-3.5) {
  rect rounded r = (12.1,(-156)) (14.3,(-151)) radius 1.05
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude UpperArmTie1 from UpperArmTie1Sk distance 7 cut targets UpperArm
cylinder UpperArmHornC2 radius 1.7 height 7 base (0,(-3.5),96) axis Y cut targets UpperArm
cylinder UpperArmHornH20 radius 1.2 height 7 base (5.3033,(-3.5),101.3033) axis Y cut targets UpperArm
cylinder UpperArmHornH21 radius 1.2 height 7 base ((-5.3033),(-3.5),90.6967) axis Y cut targets UpperArm
box UpperArmWin3 size 20.3 7 41.3 corner ((-10.15),(-3.5),165.2) cut targets UpperArm
sketch UpperArmBoot3Sk on XZ offset (-3.5) {
  rect rounded r = ((-4.6),(-167.2)) (4.6,(-159.9)) radius 1.5
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude UpperArmBoot3 from UpperArmBoot3Sk distance 7 cut targets UpperArm
cylinder UpperArmEar30 radius 1.25 height 7 base ((-5),(-3.5),210.6) axis Y cut targets UpperArm
cylinder UpperArmEar31 radius 1.25 height 7 base (11.85,(-3.5),161.1) axis Y cut targets UpperArm
box Servo3Case size 19.7 36.6 40.7 corner ((-9.85),(-24.1),165.5) new -> body Servo3
box Servo3Ears size 19.7 2.5 54.6 corner ((-9.85),2.5,158.55) join targets Servo3
box Servo3Boot size 8 7 5 corner ((-4),(-22.1),161) join targets Servo3
cylinder Servo3Boss radius 6.5 height 2.2 base (0,12.5,196) axis Y join targets Servo3
cylinder Servo3Spline radius 2.95 height 6.3 base (0,12.5,196) axis Y join targets Servo3
cylinder Horn3Disc radius 12 height 2.5 base (0,16.8,196) axis Y new -> body Horn3

// Elbow brace: bearing plate past the elbow servo's case, standoffs beside the case to the upper arm
sketch BracePlateSk on XZ offset (-30.6) {
  polyline closed edge = ((-17),(-169)) (17,(-169)) (17,(-196)) (15.706,(-202.5056)) (12.0208,(-208.0208)) (6.5056,(-211.706)) (0,(-213)) ((-6.5056),(-211.706)) ((-12.0208),(-208.0208)) ((-15.706),(-202.5056)) ((-17),(-196))
  fillet edge.11.end radius 6
  fillet edge.1.end radius 6
  fillet edge.2.end radius 6
  fillet edge.3.end radius 6
  fillet edge.4.end radius 6
  fillet edge.5.end radius 6
  fillet edge.6.end radius 6
  fillet edge.7.end radius 6
  fillet edge.8.end radius 6
  fillet edge.9.end radius 6
  fillet edge.10.end radius 6
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11
}
extrude BracePlate from BracePlateSk distance 5 new -> body ElbowBrace
cylinder BraceBore radius 5.2 height 7 base (0,(-31.6),196) axis Y cut targets ElbowBrace
cylinder BraceStandoff0 radius 3 height 23.6 base ((-13.5),(-26.1),176) axis Y join targets ElbowBrace
cylinder BracePilot0 radius 1.25 height 13 base ((-13.5),(-14.5),176) axis Y cut targets ElbowBrace
cylinder UpperArmBraceHole0 radius 1.7 height 7 base ((-13.5),(-3.5),176) axis Y cut targets UpperArm
cylinder BraceStandoff1 radius 3 height 23.6 base (13.5,(-26.1),176) axis Y join targets ElbowBrace
cylinder BracePilot1 radius 1.25 height 13 base (13.5,(-14.5),176) axis Y cut targets ElbowBrace
cylinder UpperArmBraceHole1 radius 1.7 height 7 base (13.5,(-3.5),176) axis Y cut targets UpperArm

// Shoulder clevis: second upper-arm plate riding on the idler boss, joined to the upper arm by
// two standoffs and a rib (2 x M3 through the upper arm); its boss runs in the idler tower's bore.
// Prints plate-down with the standoffs and boss up.
sketch ClevisPlateSk on XZ offset (-58.9) {
  polyline closed edge = ((-8.7),(-82)) (8.7,(-82)) (14,(-89.7)) (14,(-150)) ((-14),(-150)) ((-14),(-89.7))
  fillet edge.6.end radius 6
  fillet edge.1.end radius 6
  fillet edge.2.end radius 6
  fillet edge.3.end radius 6
  fillet edge.4.end radius 6
  fillet edge.5.end radius 6
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6
}
extrude ClevisPlate from ClevisPlateSk distance 5 new -> body ShoulderClevis
cylinder ClevisBoss radius 5 height 7 base (0,(-54.4),96) axis Y join targets ShoulderClevis
cylinder ClevisStandoff0 radius 4.5 height 51.9 base ((-6),(-54.4),130) axis Y join targets ShoulderClevis
cylinder ClevisStandoff1 radius 4.5 height 51.9 base (6,(-54.4),142) axis Y join targets ShoulderClevis
sketch ClevisRibSk on XZ offset (-54.4) {
  slot bar = centers ((-6),(-130)) (6,(-142)) width 4
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude ClevisRib from ClevisRibSk distance 51.9 join targets ShoulderClevis
cylinder ClevisPilot0 radius 1.25 height 15 base ((-6),(-16.5),130) axis Y cut targets ShoulderClevis
cylinder ClevisPilot1 radius 1.25 height 15 base (6,(-16.5),142) axis Y cut targets ShoulderClevis

// Forearm: same construction; holds the wrist servo facing back toward y = 0
sketch ForearmBodySk on XZ offset 19.3 {
  polyline closed edge = ((-8.68),(-182)) (8.68,(-182)) (14,(-189.7)) (16.5,(-242)) (16.5,(-295.4207)) (12.2596,(-300.5156)) (6.527,(-303.8437)) (0,(-305)) ((-6.527),(-303.8437)) ((-12.2596),(-300.5156)) ((-16.5),(-295.4207)) ((-16.5),(-242)) ((-14),(-189.7))
  fillet edge.13.end radius 7
  fillet edge.1.end radius 7
  fillet edge.2.end radius 7
  fillet edge.4.end radius 7
  fillet edge.5.end radius 7
  fillet edge.6.end radius 7
  fillet edge.7.end radius 7
  fillet edge.8.end radius 7
  fillet edge.9.end radius 7
  fillet edge.10.end radius 7
  fillet edge.12.end radius 7
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11 edge.12 edge.13
}
extrude ForearmBody from ForearmBodySk distance 5 new -> body Forearm
cylinder ForearmClevisHole0 radius 1.7 height 7 base ((-6),18.3,228) axis Y cut targets Forearm
cylinder ForearmClevisHole1 radius 1.7 height 7 base (6,18.3,240) axis Y cut targets Forearm
sketch ForearmTie0Sk on XZ offset 18.3 {
  rect rounded r = ((-14.3),(-246)) ((-12.1),(-241)) radius 1.05
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude ForearmTie0 from ForearmTie0Sk distance 7 cut targets Forearm
sketch ForearmTie1Sk on XZ offset 18.3 {
  rect rounded r = (12.1,(-246)) (14.3,(-241)) radius 1.05
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude ForearmTie1 from ForearmTie1Sk distance 7 cut targets Forearm
cylinder ForearmHornC3 radius 1.7 height 7 base (0,18.3,196) axis Y cut targets Forearm
cylinder ForearmHornH30 radius 1.2 height 7 base (5.3033,18.3,201.3033) axis Y cut targets Forearm
cylinder ForearmHornH31 radius 1.2 height 7 base ((-5.3033),18.3,190.6967) axis Y cut targets Forearm
box ForearmWin4 size 20.3 7 41.3 corner ((-10.15),18.3,255.2) cut targets Forearm
sketch ForearmBoot4Sk on XZ offset 18.3 {
  rect rounded r = ((-4.6),(-257.2)) (4.6,(-249.9)) radius 1.5
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude ForearmBoot4 from ForearmBoot4Sk distance 7 cut targets Forearm
cylinder ForearmEar40 radius 1.25 height 7 base ((-5),18.3,300.6) axis Y cut targets Forearm
cylinder ForearmEar41 radius 1.25 height 7 base (11.85,18.3,251.1) axis Y cut targets Forearm
box Servo4Case size 19.7 36.6 40.7 corner ((-9.85),9.3,255.5) new -> body Servo4
box Servo4Ears size 19.7 2.5 54.6 corner ((-9.85),16.8,248.55) join targets Servo4
box Servo4Boot size 8 7 5 corner ((-4),36.9,251) join targets Servo4
cylinder Servo4Boss radius 6.5 height 2.2 base (0,7.1,286) axis Y join targets Servo4
cylinder Servo4Spline radius 2.95 height 6.3 base (0,3,286) axis Y join targets Servo4
cylinder Horn4Disc radius 12 height 2.5 base (0,2.5,286) axis Y new -> body Horn4

// Forearm clevis: second forearm plate outside the elbow brace; its boss runs in the brace's bore
sketch FClevisPlateSk on XZ offset (-37.1) {
  polyline closed edge = ((-8.7),(-182)) (8.7,(-182)) (14,(-189.7)) (14,(-248)) ((-14),(-248)) ((-14),(-189.7))
  fillet edge.6.end radius 6
  fillet edge.1.end radius 6
  fillet edge.2.end radius 6
  fillet edge.3.end radius 6
  fillet edge.4.end radius 6
  fillet edge.5.end radius 6
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6
}
extrude FClevisPlate from FClevisPlateSk distance 5 new -> body ForearmClevis
cylinder FClevisBoss radius 5 height 7 base (0,(-32.6),196) axis Y join targets ForearmClevis
cylinder FClevisStandoff0 radius 4.5 height 51.9 base ((-6),(-32.6),228) axis Y join targets ForearmClevis
cylinder FClevisStandoff1 radius 4.5 height 51.9 base (6,(-32.6),240) axis Y join targets ForearmClevis
sketch FClevisRibSk on XZ offset (-32.6) {
  slot bar = centers ((-6),(-228)) (6,(-240)) width 4
  fix bar.line1 bar.line2 bar.arc1 bar.arc2
}
extrude FClevisRib from FClevisRibSk distance 51.9 join targets ForearmClevis
cylinder FClevisPilot0 radius 1.25 height 15 base ((-6),5.3,228) axis Y cut targets ForearmClevis
cylinder FClevisPilot1 radius 1.25 height 15 base (6,5.3,240) axis Y cut targets ForearmClevis

// Wrist: carrier for the gripper servo and the idler finger's pivot
sketch WristBodySk on XZ offset (-2.5) {
  polyline closed edge = ((-8.7),(-272)) (8.7,(-272)) (14,(-279.7)) (16.5,(-297)) (16.5,(-329)) (30,(-332)) (30,(-350)) (16.5,(-360)) ((-16.5),(-360)) ((-16.5),(-297)) ((-14),(-279.7))
  fillet edge.11.end radius 6
  fillet edge.1.end radius 6
  fillet edge.2.end radius 6
  fillet edge.3.end radius 6
  fillet edge.4.end radius 6
  fillet edge.5.end radius 6
  fillet edge.6.end radius 6
  fillet edge.7.end radius 6
  fillet edge.8.end radius 6
  fillet edge.9.end radius 6
  fillet edge.10.end radius 6
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11
}
extrude WristBody from WristBodySk distance 5 new -> body Wrist
cylinder WristHornC4 radius 1.7 height 7 base (0,(-3.5),286) axis Y cut targets Wrist
cylinder WristHornH40 radius 1.2 height 7 base (5.3033,(-3.5),291.3033) axis Y cut targets Wrist
cylinder WristHornH41 radius 1.2 height 7 base ((-5.3033),(-3.5),280.6967) axis Y cut targets Wrist
sketch WristTie0Sk on XZ offset (-3.5) {
  rect rounded r = ((-14.3),(-301)) ((-12.1),(-296)) radius 1.05
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude WristTie0 from WristTie0Sk distance 7 cut targets Wrist
sketch WristTie1Sk on XZ offset (-3.5) {
  rect rounded r = (12.1,(-301)) (14.3,(-296)) radius 1.05
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude WristTie1 from WristTie1Sk distance 7 cut targets Wrist
box WristWin5 size 20.3 7 41.3 corner ((-10.15),(-3.5),310.2) cut targets Wrist
sketch WristBoot5Sk on XZ offset (-3.5) {
  rect rounded r = ((-4.6),(-312.2)) (4.6,(-304.9)) radius 1.5
  fix r.bottom r.right r.top r.left r.arc1 r.arc2 r.arc3 r.arc4
}
extrude WristBoot5 from WristBoot5Sk distance 7 cut targets Wrist
cylinder WristEar50 radius 1.25 height 7 base ((-5),(-3.5),355.6) axis Y cut targets Wrist
cylinder WristEar51 radius 1.25 height 7 base (11.85,(-3.5),306.1) axis Y cut targets Wrist
box Servo5Case size 19.7 36.6 40.7 corner ((-9.85),(-12.5),310.5) new -> body Servo5
box Servo5Ears size 19.7 2.5 54.6 corner ((-9.85),(-5),303.55) join targets Servo5
box Servo5Boot size 8 7 5 corner ((-4),15.1,306) join targets Servo5
cylinder Servo5Boss radius 6.5 height 2.2 base (0,(-14.7),341) axis Y join targets Servo5
cylinder Servo5Spline radius 2.95 height 6.3 base (0,(-18.8),341) axis Y join targets Servo5
cylinder Horn5Disc radius 12 height 2.5 base (0,(-19.3),341) axis Y new -> body Horn5
cylinder PivotBoss radius 5 height 16.4 base (21,(-18.9),341) axis Y join targets Wrist
cylinder PivotPilot radius 1.25 height 23.8 base (21,(-20.3),341) axis Y cut targets Wrist

// Gripper: meshing involute gears (module 1.5, 14 teeth) with fingers; ribbed jaws
cylinder FingerACore radius 8.625 height 5 base (0,(-24.3),341) axis Y new -> body FingerA
sketch FingerAToothSk on XZ offset (-24.3) {
  polyline closed tooth = (8.0666,(-341.9723)) (9.7959,(-342.1808)) (9.8614,(-342.1836)) (10.058,(-342.1719)) (10.3812,(-342.1157)) (10.8219,(-341.9858)) (11.3656,(-341.7551)) (11.9934,(-341.3987)) (11.9934,(-340.6013)) (11.3656,(-340.2449)) (10.8219,(-340.0142)) (10.3812,(-339.8843)) (10.058,(-339.8281)) (9.8614,(-339.8164)) (9.7959,(-339.8192)) (8.0666,(-340.0277))
  fix tooth.1 tooth.2 tooth.3 tooth.4 tooth.5 tooth.6 tooth.7 tooth.8 tooth.9 tooth.10 tooth.11 tooth.12 tooth.13 tooth.14 tooth.15 tooth.16
}
extrude FingerATooth from FingerAToothSk distance 5 join targets FingerA
pattern circular FingerATeeth features FingerATooth origin (0,(-24.3),341) dir (0,1,0) count 14 angle 360 deg
sketch FingerAFingerSk on XZ offset (-24.3) {
  polyline closed edge = ((-5),(-347)) ((-12),(-367)) ((-14),(-387)) ((-9),(-407)) ((-3),(-411)) (0.5,(-409)) ((-1),(-404)) (0.8,(-401.6)) ((-1),(-399.2)) (0.8,(-396.8)) ((-1),(-394.4)) (0.8,(-392)) ((-1),(-389.6)) (0.8,(-387.2)) ((-1),(-384.8)) (0.8,(-382.4)) ((-1),(-380)) (3.5,(-368)) (6,(-351))
  fillet edge.19.end radius 3
  fillet edge.1.end radius 3
  fillet edge.2.end radius 3
  fillet edge.3.end radius 3
  fillet edge.4.end radius 2.9351
  fillet edge.5.end radius 2.2824
  fillet edge.6.end radius 2.6743
  fillet edge.7.end radius 1.8
  fillet edge.8.end radius 1.8
  fillet edge.9.end radius 1.8
  fillet edge.10.end radius 1.8
  fillet edge.11.end radius 1.8
  fillet edge.12.end radius 1.8
  fillet edge.13.end radius 1.8
  fillet edge.14.end radius 1.8
  fillet edge.15.end radius 1.8
  fillet edge.16.end radius 2.4645
  fillet edge.17.end radius 3
  fillet edge.18.end radius 3
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11 edge.12 edge.13 edge.14 edge.15 edge.16 edge.17 edge.18 edge.19
}
extrude FingerAFinger from FingerAFingerSk distance 5 join targets FingerA
cylinder FingerAHornC5 radius 1.7 height 7 base (0,(-25.3),341) axis Y cut targets FingerA
cylinder FingerAHornH50 radius 1.2 height 7 base (5.3033,(-25.3),346.3033) axis Y cut targets FingerA
cylinder FingerAHornH51 radius 1.2 height 7 base ((-5.3033),(-25.3),335.6967) axis Y cut targets FingerA
cylinder FingerBCore radius 8.625 height 5 base (21,(-24.3),341) axis Y new -> body FingerB
sketch FingerBToothSk on XZ offset (-24.3) {
  polyline closed tooth = (29.0807,(-340.153)) (30.813,(-339.9714)) (30.8776,(-339.9596)) (31.0666,(-339.9044)) (31.3692,(-339.7776)) (31.7699,(-339.553)) (32.2487,(-339.2071)) (32.7814,(-338.7199)) (32.604,(-337.9425)) (31.9126,(-337.7348)) (31.3312,(-337.6308)) (30.8727,(-337.6023)) (30.545,(-337.6194)) (30.3508,(-337.6517)) (30.2875,(-337.669)) (28.648,(-338.257))
  fix tooth.1 tooth.2 tooth.3 tooth.4 tooth.5 tooth.6 tooth.7 tooth.8 tooth.9 tooth.10 tooth.11 tooth.12 tooth.13 tooth.14 tooth.15 tooth.16
}
extrude FingerBTooth from FingerBToothSk distance 5 join targets FingerB
pattern circular FingerBTeeth features FingerBTooth origin (21,(-24.3),341) dir (0,1,0) count 14 angle 360 deg
sketch FingerBFingerSk on XZ offset (-24.3) {
  polyline closed edge = (15,(-351)) (17.5,(-368)) (22,(-380)) (20.2,(-382.4)) (22,(-384.8)) (20.2,(-387.2)) (22,(-389.6)) (20.2,(-392)) (22,(-394.4)) (20.2,(-396.8)) (22,(-399.2)) (20.2,(-401.6)) (22,(-404)) (20.5,(-409)) (24,(-411)) (30,(-407)) (35,(-387)) (33,(-367)) (26,(-347))
  fillet edge.19.end radius 3
  fillet edge.1.end radius 3
  fillet edge.2.end radius 2.4645
  fillet edge.3.end radius 1.8
  fillet edge.4.end radius 1.8
  fillet edge.5.end radius 1.8
  fillet edge.6.end radius 1.8
  fillet edge.7.end radius 1.8
  fillet edge.8.end radius 1.8
  fillet edge.9.end radius 1.8
  fillet edge.10.end radius 1.8
  fillet edge.11.end radius 1.8
  fillet edge.12.end radius 2.6743
  fillet edge.13.end radius 2.2824
  fillet edge.14.end radius 2.9351
  fillet edge.15.end radius 3
  fillet edge.16.end radius 3
  fillet edge.17.end radius 3
  fillet edge.18.end radius 3
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11 edge.12 edge.13 edge.14 edge.15 edge.16 edge.17 edge.18 edge.19
}
extrude FingerBFinger from FingerBFingerSk distance 5 join targets FingerB
cylinder FingerBPivot radius 1.7 height 7 base (21,(-25.3),341) axis Y cut targets FingerB

// Reference: centre-screw heads on the outer face of each horn-mounted part (do not print)
cylinder Head1Cap radius 2.9 height 3.2 base (0,0,52.4) axis Z new -> body Head1
cylinder Head2Cap radius 2.9 height 3.2 base (0,2.5,96) axis Y new -> body Head2
cylinder Head3Cap radius 2.9 height 3.2 base (0,24.3,196) axis Y new -> body Head3
cylinder Head4Cap radius 2.9 height 3.2 base (0,(-5.7),286) axis Y new -> body Head4
cylinder Head5Cap radius 2.9 height 3.2 base (0,(-27.5),341) axis Y new -> body Head5
cylinder Head7Cap radius 2.9 height 3.2 base ((-6),2.5,130) axis Y new -> body Head7
cylinder Head8Cap radius 2.9 height 3.2 base (6,2.5,142) axis Y new -> body Head8
cylinder Head9Cap radius 2.9 height 3.2 base ((-13.5),2.5,176) axis Y new -> body Head9
cylinder Head10Cap radius 2.9 height 3.2 base (13.5,2.5,176) axis Y new -> body Head10
cylinder Head11Cap radius 2.9 height 3.2 base ((-6),24.3,228) axis Y new -> body Head11
cylinder Head12Cap radius 2.9 height 3.2 base (6,24.3,240) axis Y new -> body Head12
