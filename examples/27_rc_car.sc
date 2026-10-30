// 27. A printable four-wheel-steering RC buggy around the parts in hand: two TowerPro MG995
// steering servos (front and rear axle), one 28 mm 1000KV brushless outrunner on the rear axle
// through an ESC, an ESP32-C3 SuperMini, a 2S 1100 mAh LiPo. Firmware: examples/27_rc_car_firmware
// (Wi-Fi access point, phone browser as the remote; front-steer, four-wheel counter-steer and crab
// modes; the ESC is driven like a servo).
// Layout: X forward, Y left, Z up; wheel centres at z = 0, the floor at z = -55. 190 mm
// wheelbase, 215 mm track, 110 mm wheels, 25 mm ground
// clearance, about 252 x 158 mm chassis. Both axles are the same module: a knuckle (steering arm) on
// each side, pivoting on two M4 screws (one up from under the plate, one down through the YokeCap
// screwed to the bumper), a 12 mm stub axle through the knuckle with the wheel on its D-end (M3
// screw), and a tie rod across the car (28 mm arms at the front, 32 at the rear to clear the gear)
// driven by a 4 mm pin on the servo's horn, riding in a slot
// (the rod slides along the pin as it moves). The servo lies on its side on the plate, ears
// screwed (M3 self-tapping) to a wall.
// Rear drive: the motor sits on the left, above and ahead of the axle, on a wall (4 x M3 on a
// 16 mm square, 11 mm hole for its rear boss); its 12T pinion (printed, M3 grub screw with a nut
// trap, plus CA glue) drives a 36T/12T compound idler (on an M4 x 30 screw through two cheeks on
// the gearbox block) which drives the 46T gear on a printed 8 mm D-section axle running in the
// chassis' two bores: module 1, 11.5:1, the 12T pinions profile-shifted +0.5 (their mates -0.5). A drive cup is glued on each axle end, a dog bone (3 mm
// steel pins) runs to the cup on each stub axle, so the rear wheels steer while driven. No
// differential. 1000KV on 2S is about 640 rpm at the wheels, 3.7 m/s flat out, with more pull
// than the tyres can use: cap the throttle in the firmware (THROTTLE_LIMIT) until you know the car.
// Printed (no supports; PLA, PETG for the gears, bones and cups if you have it):
// - Chassis: plate down. Gearbox block, motor wall and rib, servo walls, bumpers, lower yoke arms,
//   battery tray and ESC pad are part of it.
// - YokeCap x4 (flat), Knuckle x4 (as placed), Stub x4 (cup mouth down), Wheel x4 (outer face
//   down), TieRod x2 (flat), Horn x2 (hub face down), Bone x2 (standing), AxleCup x2 (mouth down),
//   Gear, Idler and Pinion (flat, big face down), Axle (flat side down).
// - Fit tests first (fit_test layout): FitTestWall (one servo wall window and its four pilots),
//   FitTestHub (the MG995 spline socket), and one of each moving part: KnuckleFR with StubFR,
//   AxleCupR, BoneR, Gear, Idler, Pinion and Axle.
// Hardware: 2 x MG995, 1 x 28 mm 1000KV outrunner (3.17 mm shaft) with a bidirectional (car) ESC,
// ESP32-C3 SuperMini, 2S 1100 mAh LiPo (72 x 36 x 18 mm envelope, stood on its long edge) with a
// hook-and-loop strap,
// 8 x M4 x 16 (pivots), 8 x M3 x 10 (yoke caps), 4 x M3 x 12 (wheels, with washers), 4 x M3 x 10
// (tie rods), 8 x M3 x 8 (servo ears), 2 x M3 x 8 (horns), 4 x M3 x 8 (motor), 1 x M4 x 30 + nylock
// (idler), 1 x M3 x 6 grub + nut (pinion), 4 x 3 x 19 mm steel pins (dog bones; M3 screw shanks will do), 4 x 3.5 mm O-rings
// about 95 mm ID (tyres, 2 per wheel; optional), zip ties (ESC), CA glue (cups on the axle, pins,
// pinion).
// Steering sign: steer_front / steer_rear turn the wheels left (yaw about +Z) when positive; the
// checks recipe sweeps them to +-20 deg and runs the interference check. gear_turn turns the rear
// train (and the idler and pinion with it) so the checks also roll both meshes through a tooth pitch.
// Precedents (nothing published does all of this in one car, each piece is proven elsewhere):
// printed dog bones / drive cups on steered driven wheels (Tarmo, OpenRC, MakerWorld 4WD B1);
// knuckle-on-two-screws C-hubs and a pin-in-slot servo link as on small RC chassis (FP UC1); the
// SG90 + N20 crab-steer rover (thingiverse 7249736) for all-wheel steering with hobby servos;
// MG995 mounts and spline sockets as printed and used in example 17.
// Reference bodies (not printed): ServoF, ServoR, Motor, Esc, Battery, Esp32.
design RcCar units mm
param steer_front = 0 deg
param steer_rear = 0 deg
param gear_turn = 0 deg
// ==== Chassis: plate, corner pads with the lower yoke arms, bumpers, servo walls and rails,
// gearbox block, motor wall, battery tray, ESC pad and board slot
box ChassisPlate size 252 130 4 corner ((-126),(-65),(-30)) new -> body Chassis
box Bx1 size 20 15 15.5 corner (90,64,(-30)) join targets Chassis
box Bx2 size 16 6 15.5 corner (110,64,(-30)) join targets Chassis
cylinder Cy3 radius 2.2 height 17.5 base (95,75,(-31)) axis Z cut targets Chassis
box Bx4 size 20 15 15.5 corner (90,(-79),(-30)) join targets Chassis
box Bx5 size 16 6 15.5 corner (110,(-70),(-30)) join targets Chassis
cylinder Cy6 radius 2.2 height 17.5 base (95,(-75),(-31)) axis Z cut targets Chassis
box Bx7 size 5 154 44.5 corner (121,(-77),(-30)) join targets Chassis
cylinder Cy8 radius 1.25 height 11 base (123.5,63,4.5) axis Z cut targets Chassis
cylinder Cy9 radius 1.25 height 11 base (123.5,72,4.5) axis Z cut targets Chassis
cylinder Cy10 radius 1.25 height 11 base (123.5,(-63),4.5) axis Z cut targets Chassis
cylinder Cy11 radius 1.25 height 11 base (123.5,(-72),4.5) axis Z cut targets Chassis
box Bx12 size 20 15 15.5 corner ((-110),64,(-30)) join targets Chassis
box Bx13 size 16 6 15.5 corner ((-126),64,(-30)) join targets Chassis
cylinder Cy14 radius 2.2 height 17.5 base ((-95),75,(-31)) axis Z cut targets Chassis
box Bx15 size 20 15 15.5 corner ((-110),(-79),(-30)) join targets Chassis
box Bx16 size 16 6 15.5 corner ((-126),(-70),(-30)) join targets Chassis
cylinder Cy17 radius 2.2 height 17.5 base ((-95),(-75),(-31)) axis Z cut targets Chassis
box Bx18 size 5 154 44.5 corner ((-126),(-77),(-30)) join targets Chassis
cylinder Cy19 radius 1.25 height 11 base ((-123.5),63,4.5) axis Z cut targets Chassis
cylinder Cy20 radius 1.25 height 11 base ((-123.5),72,4.5) axis Z cut targets Chassis
cylinder Cy21 radius 1.25 height 11 base ((-123.5),(-63),4.5) axis Z cut targets Chassis
cylinder Cy22 radius 1.25 height 11 base ((-123.5),(-72),4.5) axis Z cut targets Chassis
// servo walls (ears screwed to the inner face with M3 into 2.5 mm pilots) and rails beside the body
box Bx23 size 4 60.6 23.9 corner (47.412,(-20.15),(-26)) join targets Chassis
box Bx24 size 6 41.1 20.1 corner (46.412,(-10.4),(-26)) cut targets Chassis
cylinder Cy25 radius 1.25 height 6 base (46.412,(-13.2),(-20.95)) axis X cut targets Chassis
cylinder Cy26 radius 1.25 height 6 base (46.412,(-13.2),(-10.95)) axis X cut targets Chassis
cylinder Cy27 radius 1.25 height 6 base (46.412,33.5,(-20.95)) axis X cut targets Chassis
cylinder Cy28 radius 1.25 height 6 base (46.412,33.5,(-10.95)) axis X cut targets Chassis
box Bx29 size 26.1 2.2 8 corner (18.512,(-12.7),(-26)) join targets Chassis
box Bx30 size 26.1 2.2 8 corner (18.512,30.8,(-26)) join targets Chassis
box Bx31 size 4 60.6 23.9 corner ((-47.499),(-20.15),(-26)) join targets Chassis
box Bx32 size 6 41.1 20.1 corner ((-48.499),(-10.4),(-26)) cut targets Chassis
cylinder Cy33 radius 1.25 height 6 base ((-48.499),(-13.2),(-20.95)) axis X cut targets Chassis
cylinder Cy34 radius 1.25 height 6 base ((-48.499),(-13.2),(-10.95)) axis X cut targets Chassis
cylinder Cy35 radius 1.25 height 6 base ((-48.499),33.5,(-20.95)) axis X cut targets Chassis
cylinder Cy36 radius 1.25 height 6 base ((-48.499),33.5,(-10.95)) axis X cut targets Chassis
box Bx37 size 26.1 2.2 8 corner ((-40.699),(-12.7),(-26)) join targets Chassis
box Bx38 size 26.1 2.2 8 corner ((-40.699),30.8,(-26)) join targets Chassis
// gearbox block: two 8.4 mm axle bores either side of the gear slot; two cheeks above it carry
// the idler's M4 screw
box Bx39 size 28 32 33.5 corner ((-107),(-16),(-26)) join targets Chassis
box Bx40 size 30 9 33.5 corner ((-108),(-2.5),(-25)) cut targets Chassis
cylinder Cy41 radius 4.2 height 34 base ((-95),(-17),0) axis Y cut targets Chassis
box Bx42 size 14 4.5 30.091 corner ((-93.039),6.5,7.49) join targets Chassis
cylinder Cy43 radius 7 height 4.5 base ((-86.039),6.5,30.581) axis Y join targets Chassis
box Bx44 size 14 4.5 30.091 corner ((-93.039),(-15),7.49) join targets Chassis
cylinder Cy45 radius 7 height 4.5 base ((-86.039),(-15),30.581) axis Y join targets Chassis
cylinder Cy46 radius 2.1 height 28 base ((-86.039),(-16),27.581) axis Y cut targets Chassis
// motor wall on the left: full height from x = -80 (clear of the left dog bone's swing), the
// part behind it from z = 12 up; a window for the rear tie rod; 11 mm hole for the rear boss,
// 4 x M3 clearance on 16 mm; a rib behind the wall
box Bx47 size 34.514 3.7 79.789 corner ((-80),(-42),(-26)) join targets Chassis
box Bx48 size 36 3.7 41.789 corner ((-81.486),(-42),12) join targets Chassis
box Bx49 size 11 5.7 12.5 corner ((-69.699),(-43),0.5) cut targets Chassis
cylinder Cy50 radius 5.5 height 5.7 base ((-63.486),(-43),35.789) axis Y cut targets Chassis
cylinder Cy51 radius 1.75 height 5.7 base ((-71.486),(-43),27.789) axis Y cut targets Chassis
cylinder Cy52 radius 1.75 height 5.7 base ((-71.486),(-43),43.789) axis Y cut targets Chassis
cylinder Cy53 radius 1.75 height 5.7 base ((-55.486),(-43),27.789) axis Y cut targets Chassis
cylinder Cy54 radius 1.75 height 5.7 base ((-55.486),(-43),43.789) axis Y cut targets Chassis
sketch Sk55 on YZ offset (-48.486) {
  polyline closed p = ((-41.99),(-26)) ((-66),(-26)) ((-41.99),51.789)
  fix p.1 p.2 p.3
}
extrude Ex56 from Sk55 distance 3 join targets Chassis
// battery tray (72 x 36 x 18 pack standing on its long edge across the middle, end walls and four
// posts, strap
// through the plate), ESC pad with
// zip-tie slots, standing slot for the ESP32-C3 SuperMini
box Bx57 size 24 2.5 8 corner ((-12),(-39),(-26)) join targets Chassis
box Bx58 size 24 2.5 8 corner ((-12),36.5,(-26)) join targets Chassis
box Bx59 size 2.5 15 20 corner ((-12),(-39),(-26)) join targets Chassis
box Bx60 size 2.5 15 20 corner ((-12),24,(-26)) join targets Chassis
box Bx61 size 2.5 15 20 corner (9.5,(-39),(-26)) join targets Chassis
box Bx62 size 2.5 15 20 corner (9.5,24,(-26)) join targets Chassis
box Bx63 size 22 3 6 corner ((-11),(-44),(-31)) cut targets Chassis
box Bx64 size 22 3 6 corner ((-11),41,(-31)) cut targets Chassis
box Bx65 size 3.2 3 6 corner (34.4,(-66),(-31)) cut targets Chassis
box Bx66 size 3.2 3 6 corner (34.4,(-36),(-31)) cut targets Chassis
box Bx67 size 3.2 3 6 corner (63.4,(-66),(-31)) cut targets Chassis
box Bx68 size 3.2 3 6 corner (63.4,(-36),(-31)) cut targets Chassis
box Bx69 size 6 20 6 corner ((-3),42,(-26)) join targets Chassis
box Bx70 size 1.3 18.6 6 corner ((-0.65),42.7,(-25)) cut targets Chassis
box Bx71 size 34 24 6 corner ((-60),36,(-31)) cut targets Chassis
box Bx72 size 34 24 6 corner (26,36,(-31)) cut targets Chassis
box Bx73 size 34 24 6 corner ((-60),(-60),(-31)) cut targets Chassis
// ==== YokeCap x4: the upper yoke arm, screwed to the bumper top, carrying the upper M4 pivot
box Bx74 size 20 19 4.5 corner (90,60,14.5) new -> body YokeCapFR
box Bx75 size 16 10 4.5 corner (110,60,14.5) join targets YokeCapFR
cylinder Cy76 radius 2.2 height 6.5 base (95,75,13.5) axis Z cut targets YokeCapFR
cylinder Cy77 radius 1.7 height 6.5 base (123.5,63,13.5) axis Z cut targets YokeCapFR
cylinder Cy78 radius 1.7 height 6.5 base (123.5,72,13.5) axis Z cut targets YokeCapFR
box Bx79 size 20 19 4.5 corner (90,(-79),14.5) new -> body YokeCapFL
box Bx80 size 16 10 4.5 corner (110,(-70),14.5) join targets YokeCapFL
cylinder Cy81 radius 2.2 height 6.5 base (95,(-75),13.5) axis Z cut targets YokeCapFL
cylinder Cy82 radius 1.7 height 6.5 base (123.5,(-63),13.5) axis Z cut targets YokeCapFL
cylinder Cy83 radius 1.7 height 6.5 base (123.5,(-72),13.5) axis Z cut targets YokeCapFL
box Bx84 size 20 19 4.5 corner ((-110),60,14.5) new -> body YokeCapRR
box Bx85 size 16 10 4.5 corner ((-126),60,14.5) join targets YokeCapRR
cylinder Cy86 radius 2.2 height 6.5 base ((-95),75,13.5) axis Z cut targets YokeCapRR
cylinder Cy87 radius 1.7 height 6.5 base ((-123.5),63,13.5) axis Z cut targets YokeCapRR
cylinder Cy88 radius 1.7 height 6.5 base ((-123.5),72,13.5) axis Z cut targets YokeCapRR
box Bx89 size 20 19 4.5 corner ((-110),(-79),14.5) new -> body YokeCapRL
box Bx90 size 16 10 4.5 corner ((-126),(-70),14.5) join targets YokeCapRL
cylinder Cy91 radius 2.2 height 6.5 base ((-95),(-75),13.5) axis Z cut targets YokeCapRL
cylinder Cy92 radius 1.7 height 6.5 base ((-123.5),(-63),13.5) axis Z cut targets YokeCapRL
cylinder Cy93 radius 1.7 height 6.5 base ((-123.5),(-72),13.5) axis Z cut targets YokeCapRL
// ==== Knuckle x4: block with the 12.4 mm stub bore, 3.3 mm M4 pivot pilots top and bottom, and
// the steering arm (28/32 mm from the pivot, 12 deg inward) with a 2.5 mm pilot for the tie rod screw
box Bx94 size 25 12 28 corner (80,69,(-14)) new -> body KnuckleFR
sketch Sk95 on XY offset (-3) {
  polyline closed p = (81,67.935) (68.444,65.266) (66.78,73.091) (81,76.114)
  fix p.1 p.2 p.3 p.4
}
extrude Ex96 from Sk95 distance 6 join targets KnuckleFR
cylinder Cy97 radius 5 height 6 base (67.612,69.178,(-3)) axis Z join targets KnuckleFR
cylinder Cy98 radius 1.25 height 6 base (67.612,69.178,(-2)) axis Z cut targets KnuckleFR
cylinder Cy99 radius 6.2 height 14 base (95,68,0) axis Y cut targets KnuckleFR
cylinder Cy100 radius 1.65 height 7 base (95,75,8) axis Z cut targets KnuckleFR
cylinder Cy101 radius 1.65 height 7 base (95,75,(-15)) axis Z cut targets KnuckleFR
box Bx102 size 25 12 28 corner (80,(-81),(-14)) new -> body KnuckleFL
sketch Sk103 on XY offset (-3) {
  polyline closed p = (81,(-76.114)) (66.78,(-73.091)) (68.444,(-65.266)) (81,(-67.935))
  fix p.1 p.2 p.3 p.4
}
extrude Ex104 from Sk103 distance 6 join targets KnuckleFL
cylinder Cy105 radius 5 height 6 base (67.612,(-69.178),(-3)) axis Z join targets KnuckleFL
cylinder Cy106 radius 1.25 height 6 base (67.612,(-69.178),(-2)) axis Z cut targets KnuckleFL
cylinder Cy107 radius 6.2 height 14 base (95,(-82),0) axis Y cut targets KnuckleFL
cylinder Cy108 radius 1.65 height 7 base (95,(-75),8) axis Z cut targets KnuckleFL
cylinder Cy109 radius 1.65 height 7 base (95,(-75),(-15)) axis Z cut targets KnuckleFL
box Bx110 size 25 12 28 corner ((-105),69,(-14)) new -> body KnuckleRR
sketch Sk111 on XY offset (-3) {
  polyline closed p = ((-81),76.114) ((-62.868),72.259) ((-64.531),64.434) ((-81),67.935)
  fix p.1 p.2 p.3 p.4
}
extrude Ex112 from Sk111 distance 6 join targets KnuckleRR
cylinder Cy113 radius 5 height 6 base ((-63.699),68.347,(-3)) axis Z join targets KnuckleRR
cylinder Cy114 radius 1.25 height 6 base ((-63.699),68.347,(-2)) axis Z cut targets KnuckleRR
cylinder Cy115 radius 6.2 height 14 base ((-95),68,0) axis Y cut targets KnuckleRR
cylinder Cy116 radius 1.65 height 7 base ((-95),75,8) axis Z cut targets KnuckleRR
cylinder Cy117 radius 1.65 height 7 base ((-95),75,(-15)) axis Z cut targets KnuckleRR
box Bx118 size 25 12 28 corner ((-105),(-81),(-14)) new -> body KnuckleRL
sketch Sk119 on XY offset (-3) {
  polyline closed p = ((-81),(-67.935)) ((-64.531),(-64.434)) ((-62.868),(-72.259)) ((-81),(-76.114))
  fix p.1 p.2 p.3 p.4
}
extrude Ex120 from Sk119 distance 6 join targets KnuckleRL
cylinder Cy121 radius 5 height 6 base ((-63.699),(-68.347),(-3)) axis Z join targets KnuckleRL
cylinder Cy122 radius 1.25 height 6 base ((-63.699),(-68.347),(-2)) axis Z cut targets KnuckleRL
cylinder Cy123 radius 6.2 height 14 base ((-95),(-82),0) axis Y cut targets KnuckleRL
cylinder Cy124 radius 1.65 height 7 base ((-95),(-75),8) axis Z cut targets KnuckleRL
cylinder Cy125 radius 1.65 height 7 base ((-95),(-75),(-15)) axis Z cut targets KnuckleRL
// ==== Stub x4: drive cup (mouth inboard, two 3.4 mm slots), 12 mm shaft through the knuckle,
// 15 mm shoulder, D-section for the wheel, M3 pilot in the end. Wheel x4: rim with two O-ring
// grooves, spoked outer disc, hub with the D bore and the counterbored M3 hole
cylinder Cy126 radius 10.5 height 16 base (95,52,0) axis Y new -> body StubFR
cylinder Cy127 radius 8 height 13 base (95,51,0) axis Y cut targets StubFR
box Bx128 size 23 13 3.4 corner (83.5,51,(-1.7)) cut targets StubFR
cylinder Cy129 radius 6 height 13 base (95,68,0) axis Y join targets StubFR
cylinder Cy130 radius 7.5 height 8 base (95,81,0) axis Y join targets StubFR
cylinder Cy131 radius 6 height 22 base (95,89,0) axis Y join targets StubFR
box Bx132 size 14 23.01 2.5 corner (88,88.99,(-7)) cut targets StubFR
cylinder Cy133 radius 1.25 height 13 base (95,99,0) axis Y cut targets StubFR
cylinder Cy134 radius 53.5 height 36 base (95,89.5,0) axis Y new -> body WheelFR
cylinder Cy135 radius 55 height 8.5 base (95,89.5,0) axis Y join targets WheelFR
cylinder Cy136 radius 55 height 13 base (95,101,0) axis Y join targets WheelFR
cylinder Cy137 radius 55 height 8.5 base (95,117,0) axis Y join targets WheelFR
cylinder Cy138 radius 50 height 33 base (95,88.5,0) axis Y cut targets WheelFR
cylinder Cy139 radius 14 height 36 base (95,89.5,0) axis Y join targets WheelFR
cylinder Cy140 radius 8 height 6 base (121.847,120.5,15.5) axis Y cut targets WheelFR
cylinder Cy141 radius 8 height 6 base (95,120.5,31) axis Y cut targets WheelFR
cylinder Cy142 radius 8 height 6 base (68.153,120.5,15.5) axis Y cut targets WheelFR
cylinder Cy143 radius 8 height 6 base (68.153,120.5,(-15.5)) axis Y cut targets WheelFR
cylinder Cy144 radius 8 height 6 base (95,120.5,(-31)) axis Y cut targets WheelFR
cylinder Cy145 radius 8 height 6 base (121.847,120.5,(-15.5)) axis Y cut targets WheelFR
cylinder Cy146 radius 6.15 height 23 base (95,88.5,0) axis Y cut targets WheelFR
box Bx147 size 14 21.99 2.35 corner (88,89.51,(-7)) join targets WheelFR
cylinder Cy148 radius 1.7 height 15.01 base (95,111.49,0) axis Y cut targets WheelFR
cylinder Cy149 radius 4 height 5 base (95,121.5,0) axis Y cut targets WheelFR
cylinder Cy150 radius 10.5 height 16 base (95,(-68),0) axis Y new -> body StubFL
cylinder Cy151 radius 8 height 13 base (95,(-64),0) axis Y cut targets StubFL
box Bx152 size 23 13 3.4 corner (83.5,(-64),(-1.7)) cut targets StubFL
cylinder Cy153 radius 6 height 13 base (95,(-81),0) axis Y join targets StubFL
cylinder Cy154 radius 7.5 height 8 base (95,(-89),0) axis Y join targets StubFL
cylinder Cy155 radius 6 height 22 base (95,(-111),0) axis Y join targets StubFL
box Bx156 size 14 23.01 2.5 corner (88,(-112),(-7)) cut targets StubFL
cylinder Cy157 radius 1.25 height 13 base (95,(-112),0) axis Y cut targets StubFL
cylinder Cy158 radius 53.5 height 36 base (95,(-125.5),0) axis Y new -> body WheelFL
cylinder Cy159 radius 55 height 27.5 base (95,(-117),0) axis Y join targets WheelFL
cylinder Cy160 radius 55 height 13 base (95,(-114),0) axis Y join targets WheelFL
cylinder Cy161 radius 55 height 27.5 base (95,(-125.5),0) axis Y join targets WheelFL
cylinder Cy162 radius 50 height 33 base (95,(-121.5),0) axis Y cut targets WheelFL
cylinder Cy163 radius 14 height 36 base (95,(-125.5),0) axis Y join targets WheelFL
cylinder Cy164 radius 8 height 6 base (121.847,(-126.5),15.5) axis Y cut targets WheelFL
cylinder Cy165 radius 8 height 6 base (95,(-126.5),31) axis Y cut targets WheelFL
cylinder Cy166 radius 8 height 6 base (68.153,(-126.5),15.5) axis Y cut targets WheelFL
cylinder Cy167 radius 8 height 6 base (68.153,(-126.5),(-15.5)) axis Y cut targets WheelFL
cylinder Cy168 radius 8 height 6 base (95,(-126.5),(-31)) axis Y cut targets WheelFL
cylinder Cy169 radius 8 height 6 base (121.847,(-126.5),(-15.5)) axis Y cut targets WheelFL
cylinder Cy170 radius 6.15 height 23 base (95,(-111.5),0) axis Y cut targets WheelFL
box Bx171 size 14 21.99 2.35 corner (88,(-111.5),(-7)) join targets WheelFL
cylinder Cy172 radius 1.7 height 15.01 base (95,(-126.5),0) axis Y cut targets WheelFL
cylinder Cy173 radius 4 height 5 base (95,(-126.5),0) axis Y cut targets WheelFL
cylinder Cy174 radius 10.5 height 16 base ((-95),52,0) axis Y new -> body StubRR
cylinder Cy175 radius 8 height 13 base ((-95),51,0) axis Y cut targets StubRR
box Bx176 size 23 13 3.4 corner ((-106.5),51,(-1.7)) cut targets StubRR
cylinder Cy177 radius 6 height 13 base ((-95),68,0) axis Y join targets StubRR
cylinder Cy178 radius 7.5 height 8 base ((-95),81,0) axis Y join targets StubRR
cylinder Cy179 radius 6 height 22 base ((-95),89,0) axis Y join targets StubRR
box Bx180 size 14 23.01 2.5 corner ((-102),88.99,(-7)) cut targets StubRR
cylinder Cy181 radius 1.25 height 13 base ((-95),99,0) axis Y cut targets StubRR
cylinder Cy182 radius 53.5 height 36 base ((-95),89.5,0) axis Y new -> body WheelRR
cylinder Cy183 radius 55 height 8.5 base ((-95),89.5,0) axis Y join targets WheelRR
cylinder Cy184 radius 55 height 13 base ((-95),101,0) axis Y join targets WheelRR
cylinder Cy185 radius 55 height 8.5 base ((-95),117,0) axis Y join targets WheelRR
cylinder Cy186 radius 50 height 33 base ((-95),88.5,0) axis Y cut targets WheelRR
cylinder Cy187 radius 14 height 36 base ((-95),89.5,0) axis Y join targets WheelRR
cylinder Cy188 radius 8 height 6 base ((-68.153),120.5,15.5) axis Y cut targets WheelRR
cylinder Cy189 radius 8 height 6 base ((-95),120.5,31) axis Y cut targets WheelRR
cylinder Cy190 radius 8 height 6 base ((-121.847),120.5,15.5) axis Y cut targets WheelRR
cylinder Cy191 radius 8 height 6 base ((-121.847),120.5,(-15.5)) axis Y cut targets WheelRR
cylinder Cy192 radius 8 height 6 base ((-95),120.5,(-31)) axis Y cut targets WheelRR
cylinder Cy193 radius 8 height 6 base ((-68.153),120.5,(-15.5)) axis Y cut targets WheelRR
cylinder Cy194 radius 6.15 height 23 base ((-95),88.5,0) axis Y cut targets WheelRR
box Bx195 size 14 21.99 2.35 corner ((-102),89.51,(-7)) join targets WheelRR
cylinder Cy196 radius 1.7 height 15.01 base ((-95),111.49,0) axis Y cut targets WheelRR
cylinder Cy197 radius 4 height 5 base ((-95),121.5,0) axis Y cut targets WheelRR
cylinder Cy198 radius 10.5 height 16 base ((-95),(-68),0) axis Y new -> body StubRL
cylinder Cy199 radius 8 height 13 base ((-95),(-64),0) axis Y cut targets StubRL
box Bx200 size 23 13 3.4 corner ((-106.5),(-64),(-1.7)) cut targets StubRL
cylinder Cy201 radius 6 height 13 base ((-95),(-81),0) axis Y join targets StubRL
cylinder Cy202 radius 7.5 height 8 base ((-95),(-89),0) axis Y join targets StubRL
cylinder Cy203 radius 6 height 22 base ((-95),(-111),0) axis Y join targets StubRL
box Bx204 size 14 23.01 2.5 corner ((-102),(-112),(-7)) cut targets StubRL
cylinder Cy205 radius 1.25 height 13 base ((-95),(-112),0) axis Y cut targets StubRL
cylinder Cy206 radius 53.5 height 36 base ((-95),(-125.5),0) axis Y new -> body WheelRL
cylinder Cy207 radius 55 height 27.5 base ((-95),(-117),0) axis Y join targets WheelRL
cylinder Cy208 radius 55 height 13 base ((-95),(-114),0) axis Y join targets WheelRL
cylinder Cy209 radius 55 height 27.5 base ((-95),(-125.5),0) axis Y join targets WheelRL
cylinder Cy210 radius 50 height 33 base ((-95),(-121.5),0) axis Y cut targets WheelRL
cylinder Cy211 radius 14 height 36 base ((-95),(-125.5),0) axis Y join targets WheelRL
cylinder Cy212 radius 8 height 6 base ((-68.153),(-126.5),15.5) axis Y cut targets WheelRL
cylinder Cy213 radius 8 height 6 base ((-95),(-126.5),31) axis Y cut targets WheelRL
cylinder Cy214 radius 8 height 6 base ((-121.847),(-126.5),15.5) axis Y cut targets WheelRL
cylinder Cy215 radius 8 height 6 base ((-121.847),(-126.5),(-15.5)) axis Y cut targets WheelRL
cylinder Cy216 radius 8 height 6 base ((-95),(-126.5),(-31)) axis Y cut targets WheelRL
cylinder Cy217 radius 8 height 6 base ((-68.153),(-126.5),(-15.5)) axis Y cut targets WheelRL
cylinder Cy218 radius 6.15 height 23 base ((-95),(-111.5),0) axis Y cut targets WheelRL
box Bx219 size 14 21.99 2.35 corner ((-102),(-111.5),(-7)) join targets WheelRL
cylinder Cy220 radius 1.7 height 15.01 base ((-95),(-126.5),0) axis Y cut targets WheelRL
cylinder Cy221 radius 4 height 5 base ((-95),(-126.5),0) axis Y cut targets WheelRL
// ==== TieRod x2: bar on the knuckle arms (M3 x 10 into the arm pilots), slot for the horn pin.
// Horn x2: hub on the MG995 spline (M3 horn screw), arm up to the 4 mm pin at the tie rod's height
box Bx222 size 4 138.357 6 corner (65.612,(-69.178),3) new -> body TieRodF
cylinder Cy223 radius 5 height 6 base (67.612,69.178,3) axis Z join targets TieRodF
cylinder Cy224 radius 1.7 height 8 base (67.612,69.178,2) axis Z cut targets TieRodF
cylinder Cy225 radius 5 height 6 base (67.612,(-69.178),3) axis Z join targets TieRodF
cylinder Cy226 radius 1.7 height 8 base (67.612,(-69.178),2) axis Z cut targets TieRodF
box Bx227 size 4 28 12.5 corner (65.612,(-14),(-1.5)) join targets TieRodF
box Bx228 size 6 4.4 8.4 corner (64.612,(-2.2),0.2) cut targets TieRodF
cylinder Cy229 radius 7 height 7 base (57.112,0,(-15.95)) axis X new -> body HornF
sketch Sk230 on YZ offset 60.112 {
  polyline closed p = ((-6.5),(-15.95)) (6.5,(-15.95)) (4,6) ((-4),6)
  fix p.1 p.2 p.3 p.4
}
extrude Ex231 from Sk230 distance 4 join targets HornF
cylinder Cy232 radius 4 height 4 base (60.112,0,6) axis X join targets HornF
cylinder Cy233 radius 2 height 8.51 base (64.102,0,6) axis X join targets HornF
cylinder Cy234 radius 3 height 7.6 base (56.112,0,(-15.95)) axis X cut targets HornF
cylinder Cy235 radius 1.6 height 9 base (56.112,0,(-15.95)) axis X cut targets HornF
box Bx236 size 4 136.694 6 corner ((-65.699),(-68.347),3) new -> body TieRodR
cylinder Cy237 radius 5 height 6 base ((-63.699),68.347,3) axis Z join targets TieRodR
cylinder Cy238 radius 1.7 height 8 base ((-63.699),68.347,2) axis Z cut targets TieRodR
cylinder Cy239 radius 5 height 6 base ((-63.699),(-68.347),3) axis Z join targets TieRodR
cylinder Cy240 radius 1.7 height 8 base ((-63.699),(-68.347),2) axis Z cut targets TieRodR
box Bx241 size 4 28 12.5 corner ((-65.699),(-14),(-1.5)) join targets TieRodR
box Bx242 size 6 4.4 8.4 corner ((-66.699),(-2.2),0.2) cut targets TieRodR
cylinder Cy243 radius 7 height 7 base ((-60.199),0,(-15.95)) axis X new -> body HornR
sketch Sk244 on YZ offset (-60.199) {
  polyline closed p = ((-6.5),(-15.95)) (6.5,(-15.95)) (4,6) ((-4),6)
  fix p.1 p.2 p.3 p.4
}
extrude Ex245 from Sk244 distance 4 join targets HornR
cylinder Cy246 radius 4 height 4 base ((-60.199),0,6) axis X join targets HornR
cylinder Cy247 radius 2 height 8.49 base ((-68.699),0,6) axis X join targets HornR
cylinder Cy248 radius 3 height 7.6 base ((-59.799),0,(-15.95)) axis X cut targets HornR
cylinder Cy249 radius 1.6 height 9 base ((-61.199),0,(-15.95)) axis X cut targets HornR
// ==== Rear drive: Axle (8 mm D rod, flat down), Gear 46T on it, Idler 36T/12T (4.3 mm bore for an
// M4 screw), Pinion 12T on the motor shaft (3.4 mm bore, M3 grub screw with a nut trap in the hub),
// AxleCup x2 (D socket glued on the
// axle, slotted cup), Bone x2 (dog bone, 3 mm steel pins)
cylinder Cy250 radius 4 height 48 base ((-95),(-24),0) axis Y new -> body Axle
box Bx251 size 10 50 2 corner ((-100),(-25),(-5)) cut targets Axle
sketch Sk253 on XZ offset (-2) {
  polyline closed p = ((-87.655),(-19.94)) ((-87.528),(-20.28)) ((-87.427),(-20.746)) ((-87.379),(-21.23)) ((-87.369),(-21.727)) ((-87.391),(-22.234)) ((-88.087),(-22.46)) ((-88.403),(-22.063)) ((-88.687),(-21.655)) ((-88.933),(-21.235)) ((-89.124),(-20.799)) ((-89.222),(-20.449)) ((-90.438),(-20.755)) ((-90.359),(-21.109)) ((-90.322),(-21.584)) ((-90.341),(-22.07)) ((-90.398),(-22.564)) ((-90.489),(-23.063)) ((-91.21),(-23.192)) ((-91.469),(-22.756)) ((-91.694),(-22.313)) ((-91.881),(-21.863)) ((-92.011),(-21.405)) ((-92.06),(-21.046)) ((-93.307),(-21.182)) ((-93.277),(-21.544)) ((-93.305),(-22.02)) ((-93.389),(-22.499)) ((-93.514),(-22.98)) ((-93.671),(-23.462)) ((-94.403),(-23.492)) ((-94.6),(-23.025)) ((-94.763),(-22.555)) ((-94.887),(-22.084)) ((-94.954),(-21.613)) ((-94.953),(-21.25)) ((-96.207),(-21.216)) ((-96.227),(-21.578)) ((-96.319),(-22.045)) ((-96.468),(-22.509)) ((-96.657),(-22.969)) ((-96.879),(-23.425)) ((-97.608),(-23.355)) ((-97.739),(-22.865)) ((-97.837),(-22.377)) ((-97.895),(-21.894)) ((-97.897),(-21.418)) ((-97.847),(-21.058)) ((-99.085),(-20.854)) ((-99.153),(-21.21)) ((-99.308),(-21.66)) ((-99.519),(-22.099)) ((-99.769),(-22.529)) ((-100.051),(-22.951)) ((-100.764),(-22.782)) ((-100.827),(-22.279)) ((-100.857),(-21.783)) ((-100.85),(-21.296)) ((-100.786),(-20.824)) ((-100.688),(-20.475)) ((-101.886),(-20.103)) ((-102.003),(-20.447)) ((-102.218),(-20.872)) ((-102.486),(-21.278)) ((-102.792),(-21.67)) ((-103.129),(-22.049)) ((-103.812),(-21.785)) ((-103.806),(-21.278)) ((-103.769),(-20.782)) ((-103.695),(-20.301)) ((-103.568),(-19.842)) ((-103.423),(-19.509)) ((-104.559),(-18.978)) ((-104.722),(-19.303)) ((-104.992),(-19.695)) ((-105.314),(-20.06)) ((-105.67),(-20.407)) ((-106.055),(-20.737)) ((-106.697),(-20.382)) ((-106.622),(-19.881)) ((-106.517),(-19.395)) ((-106.378),(-18.928)) ((-106.19),(-18.491)) ((-106.001),(-18.181)) ((-107.055),(-17.5)) ((-107.26),(-17.799)) ((-107.581),(-18.151)) ((-107.949),(-18.469)) ((-108.35),(-18.764)) ((-108.776),(-19.039)) ((-109.363),(-18.6)) ((-109.221),(-18.113)) ((-109.051),(-17.646)) ((-108.85),(-17.202)) ((-108.604),(-16.795)) ((-108.374),(-16.513)) ((-109.325),(-15.696)) ((-109.569),(-15.964)) ((-109.935),(-16.269)) ((-110.344),(-16.534)) ((-110.78),(-16.772)) ((-111.24),(-16.985)) ((-111.762),(-16.471)) ((-111.554),(-16.008)) ((-111.323),(-15.568)) ((-111.063),(-15.156)) ((-110.764),(-14.786)) ((-110.498),(-14.538)) ((-111.329),(-13.599)) ((-111.607),(-13.832)) ((-112.012),(-14.083)) ((-112.452),(-14.29)) ((-112.917),(-14.467)) ((-113.402),(-14.616)) ((-113.849),(-14.035)) ((-113.58),(-13.605)) ((-113.29),(-13.2)) ((-112.977),(-12.828)) ((-112.63),(-12.502)) ((-112.334),(-12.292)) ((-113.029),(-11.249)) ((-113.336),(-11.441)) ((-113.771),(-11.636)) ((-114.235),(-11.781)) ((-114.72),(-11.892)) ((-115.221),(-11.974)) ((-115.584),(-11.338)) ((-115.259),(-10.948)) ((-114.918),(-10.587)) ((-114.556),(-10.261)) ((-114.168),(-9.985)) ((-113.846),(-9.818)) ((-114.392),(-8.689)) ((-114.723),(-8.838)) ((-115.18),(-8.971)) ((-115.66),(-9.052)) ((-116.156),(-9.096)) ((-116.663),(-9.109)) ((-116.936),(-8.429)) ((-116.561),(-8.087)) ((-116.174),(-7.776)) ((-115.771),(-7.502)) ((-115.349),(-7.282)) ((-115.007),(-7.16)) ((-115.395),(-5.967)) ((-115.743),(-6.07)) ((-116.214),(-6.14)) ((-116.701),(-6.154)) ((-117.197),(-6.131)) ((-117.701),(-6.074)) ((-117.88),(-5.364)) ((-117.462),(-5.076)) ((-117.035),(-4.821)) ((-116.6),(-4.604)) ((-116.151),(-4.443)) ((-115.796),(-4.369)) ((-116.018),(-3.135)) ((-116.376),(-3.189)) ((-116.853),(-3.194)) ((-117.337),(-3.142)) ((-117.825),(-3.051)) ((-118.317),(-2.927)) ((-118.397),(-2.198)) ((-117.944),(-1.97)) ((-117.486),(-1.775)) ((-117.025),(-1.62)) ((-116.559),(-1.521)) ((-116.197),(-1.497)) ((-116.249),(-0.244)) ((-116.611),(-0.249)) ((-117.084),(-0.189)) ((-117.556),(-0.071)) ((-118.028),0.085) ((-118.498),0.276) ((-118.478),1.008) ((-117.998),1.172) ((-117.519),1.303) ((-117.041),1.394) ((-116.566),1.429) ((-116.204),1.404) ((-116.084),2.652) ((-116.444),2.696) ((-116.904),2.82) ((-117.356),3.001) ((-117.802),3.22) ((-118.242),3.473) ((-118.122),4.196) ((-117.625),4.293) ((-117.132),4.357) ((-116.645),4.383) ((-116.17),4.352) ((-115.815),4.278) ((-115.526),5.498) ((-115.877),5.591) ((-116.316),5.776) ((-116.739),6.017) ((-117.151),6.295) ((-117.553),6.605) ((-117.336),7.305) ((-116.829),7.334) ((-116.332),7.33) ((-115.847),7.289) ((-115.381),7.194) ((-115.039),7.072) ((-114.587),8.242) ((-114.921),8.382) ((-115.331),8.625) ((-115.717),8.921) ((-116.088),9.253) ((-116.443),9.615) ((-116.133),10.278) ((-115.627),10.238) ((-115.135),10.167) ((-114.66),10.06) ((-114.211),9.902) ((-113.889),9.735) ((-113.282),10.832) ((-113.595),11.016) ((-113.967),11.313) ((-114.31),11.659) ((-114.631),12.038) ((-114.934),12.445) ((-114.537),13.06) ((-114.041),12.951) ((-113.563),12.814) ((-113.107),12.643) ((-112.684),12.426) ((-112.387),12.216) ((-111.637),13.221) ((-111.921),13.446) ((-112.25),13.79) ((-112.542),14.18) ((-112.809),14.599) ((-113.054),15.043) ((-112.576),15.599) ((-112.1),15.423) ((-111.646),15.222) ((-111.217),14.991) ((-110.827),14.718) ((-110.562),14.47) ((-109.681),15.363) ((-109.933),15.625) ((-110.211),16.011) ((-110.448),16.436) ((-110.656),16.888) ((-110.837),17.362) ((-110.289),17.847) ((-109.841),17.608) ((-109.418),17.347) ((-109.025),17.06) ((-108.676),16.736) ((-108.447),16.454) ((-107.453),17.219) ((-107.666),17.513) ((-107.89),17.933) ((-108.066),18.387) ((-108.21),18.862) ((-108.326),19.356) ((-107.716),19.762) ((-107.305),19.465) ((-106.921),19.149) ((-106.571),18.811) ((-106.269),18.442) ((-106.081),18.132) ((-104.992),18.754) ((-105.164),19.074) ((-105.328),19.521) ((-105.441),19.995) ((-105.519),20.486) ((-105.566),20.991) ((-104.907),21.31) ((-104.54),20.959) ((-104.203),20.594) ((-103.902),20.211) ((-103.653),19.805) ((-103.509),19.472) ((-102.345),19.94) ((-102.472),20.28) ((-102.573),20.746) ((-102.621),21.23) ((-102.631),21.727) ((-102.609),22.234) ((-101.913),22.46) ((-101.597),22.063) ((-101.313),21.655) ((-101.067),21.235) ((-100.876),20.799) ((-100.778),20.449) ((-99.562),20.755) ((-99.641),21.109) ((-99.678),21.584) ((-99.659),22.07) ((-99.602),22.564) ((-99.511),23.063) ((-98.79),23.192) ((-98.531),22.756) ((-98.306),22.313) ((-98.119),21.863) ((-97.989),21.405) ((-97.94),21.046) ((-96.693),21.182) ((-96.723),21.544) ((-96.695),22.02) ((-96.611),22.499) ((-96.486),22.98) ((-96.329),23.462) ((-95.597),23.492) ((-95.4),23.025) ((-95.237),22.555) ((-95.113),22.084) ((-95.046),21.613) ((-95.047),21.25) ((-93.793),21.216) ((-93.773),21.578) ((-93.681),22.045) ((-93.532),22.509) ((-93.343),22.969) ((-93.121),23.425) ((-92.392),23.355) ((-92.261),22.865) ((-92.163),22.377) ((-92.105),21.894) ((-92.103),21.418) ((-92.153),21.058) ((-90.915),20.854) ((-90.847),21.21) ((-90.692),21.66) ((-90.481),22.099) ((-90.231),22.529) ((-89.949),22.951) ((-89.236),22.782) ((-89.173),22.279) ((-89.143),21.783) ((-89.15),21.296) ((-89.214),20.824) ((-89.312),20.475) ((-88.114),20.103) ((-87.997),20.447) ((-87.782),20.872) ((-87.514),21.278) ((-87.208),21.67) ((-86.871),22.049) ((-86.188),21.785) ((-86.194),21.278) ((-86.231),20.782) ((-86.305),20.301) ((-86.432),19.842) ((-86.577),19.509) ((-85.441),18.978) ((-85.278),19.303) ((-85.008),19.695) ((-84.686),20.06) ((-84.33),20.407) ((-83.945),20.737) ((-83.303),20.382) ((-83.378),19.881) ((-83.483),19.395) ((-83.622),18.928) ((-83.81),18.491) ((-83.999),18.181) ((-82.945),17.5) ((-82.74),17.799) ((-82.419),18.151) ((-82.051),18.469) ((-81.65),18.764) ((-81.224),19.039) ((-80.637),18.6) ((-80.779),18.113) ((-80.949),17.646) ((-81.15),17.202) ((-81.396),16.795) ((-81.626),16.513) ((-80.675),15.696) ((-80.431),15.964) ((-80.065),16.269) ((-79.656),16.534) ((-79.22),16.772) ((-78.76),16.985) ((-78.238),16.471) ((-78.446),16.008) ((-78.677),15.568) ((-78.937),15.156) ((-79.236),14.786) ((-79.502),14.538) ((-78.671),13.599) ((-78.393),13.832) ((-77.988),14.083) ((-77.548),14.29) ((-77.083),14.467) ((-76.598),14.616) ((-76.151),14.035) ((-76.42),13.605) ((-76.71),13.2) ((-77.023),12.828) ((-77.37),12.502) ((-77.666),12.292) ((-76.971),11.249) ((-76.664),11.441) ((-76.229),11.636) ((-75.765),11.781) ((-75.28),11.892) ((-74.779),11.974) ((-74.416),11.338) ((-74.741),10.948) ((-75.082),10.587) ((-75.444),10.261) ((-75.832),9.985) ((-76.154),9.818) ((-75.608),8.689) ((-75.277),8.838) ((-74.82),8.971) ((-74.34),9.052) ((-73.844),9.096) ((-73.337),9.109) ((-73.064),8.429) ((-73.439),8.087) ((-73.826),7.776) ((-74.229),7.502) ((-74.651),7.282) ((-74.993),7.16) ((-74.605),5.967) ((-74.257),6.07) ((-73.786),6.14) ((-73.299),6.154) ((-72.803),6.131) ((-72.299),6.074) ((-72.12),5.364) ((-72.538),5.076) ((-72.965),4.821) ((-73.4),4.604) ((-73.849),4.443) ((-74.204),4.369) ((-73.982),3.135) ((-73.624),3.189) ((-73.147),3.194) ((-72.663),3.142) ((-72.175),3.051) ((-71.683),2.927) ((-71.603),2.198) ((-72.056),1.97) ((-72.514),1.775) ((-72.975),1.62) ((-73.441),1.521) ((-73.803),1.497) ((-73.751),0.244) ((-73.389),0.249) ((-72.916),0.189) ((-72.444),0.071) ((-71.972),(-0.085)) ((-71.502),(-0.276)) ((-71.522),(-1.008)) ((-72.002),(-1.172)) ((-72.481),(-1.303)) ((-72.959),(-1.394)) ((-73.434),(-1.429)) ((-73.796),(-1.404)) ((-73.916),(-2.652)) ((-73.556),(-2.696)) ((-73.096),(-2.82)) ((-72.644),(-3.001)) ((-72.198),(-3.22)) ((-71.758),(-3.473)) ((-71.878),(-4.196)) ((-72.375),(-4.293)) ((-72.868),(-4.357)) ((-73.355),(-4.383)) ((-73.83),(-4.352)) ((-74.185),(-4.278)) ((-74.474),(-5.498)) ((-74.123),(-5.591)) ((-73.684),(-5.776)) ((-73.261),(-6.017)) ((-72.849),(-6.295)) ((-72.447),(-6.605)) ((-72.664),(-7.305)) ((-73.171),(-7.334)) ((-73.668),(-7.33)) ((-74.153),(-7.289)) ((-74.619),(-7.194)) ((-74.961),(-7.072)) ((-75.413),(-8.242)) ((-75.079),(-8.382)) ((-74.669),(-8.625)) ((-74.283),(-8.921)) ((-73.912),(-9.253)) ((-73.557),(-9.615)) ((-73.867),(-10.278)) ((-74.373),(-10.238)) ((-74.865),(-10.167)) ((-75.34),(-10.06)) ((-75.789),(-9.902)) ((-76.111),(-9.735)) ((-76.718),(-10.832)) ((-76.405),(-11.016)) ((-76.033),(-11.313)) ((-75.69),(-11.659)) ((-75.369),(-12.038)) ((-75.066),(-12.445)) ((-75.463),(-13.06)) ((-75.959),(-12.951)) ((-76.437),(-12.814)) ((-76.893),(-12.643)) ((-77.316),(-12.426)) ((-77.613),(-12.216)) ((-78.363),(-13.221)) ((-78.079),(-13.446)) ((-77.75),(-13.79)) ((-77.458),(-14.18)) ((-77.191),(-14.599)) ((-76.946),(-15.043)) ((-77.424),(-15.599)) ((-77.9),(-15.423)) ((-78.354),(-15.222)) ((-78.783),(-14.991)) ((-79.173),(-14.718)) ((-79.438),(-14.47)) ((-80.319),(-15.363)) ((-80.067),(-15.625)) ((-79.789),(-16.011)) ((-79.552),(-16.436)) ((-79.344),(-16.888)) ((-79.163),(-17.362)) ((-79.711),(-17.847)) ((-80.159),(-17.608)) ((-80.582),(-17.347)) ((-80.975),(-17.06)) ((-81.324),(-16.736)) ((-81.553),(-16.454)) ((-82.547),(-17.219)) ((-82.334),(-17.513)) ((-82.11),(-17.933)) ((-81.934),(-18.387)) ((-81.79),(-18.862)) ((-81.674),(-19.356)) ((-82.284),(-19.762)) ((-82.695),(-19.465)) ((-83.079),(-19.149)) ((-83.429),(-18.811)) ((-83.731),(-18.442)) ((-83.919),(-18.132)) ((-85.008),(-18.754)) ((-84.836),(-19.074)) ((-84.672),(-19.521)) ((-84.559),(-19.995)) ((-84.481),(-20.486)) ((-84.434),(-20.991)) ((-85.093),(-21.31)) ((-85.46),(-20.959)) ((-85.797),(-20.594)) ((-86.098),(-20.211)) ((-86.347),(-19.805)) ((-86.491),(-19.472))
  fix p.1 p.2 p.3 p.4 p.5 p.6 p.7 p.8 p.9 p.10 p.11 p.12 p.13 p.14 p.15 p.16 p.17 p.18 p.19 p.20 p.21 p.22 p.23 p.24 p.25 p.26 p.27 p.28 p.29 p.30 p.31 p.32 p.33 p.34 p.35 p.36 p.37 p.38 p.39 p.40 p.41 p.42 p.43 p.44 p.45 p.46 p.47 p.48 p.49 p.50 p.51 p.52 p.53 p.54 p.55 p.56 p.57 p.58 p.59 p.60 p.61 p.62 p.63 p.64 p.65 p.66 p.67 p.68 p.69 p.70 p.71 p.72 p.73 p.74 p.75 p.76 p.77 p.78 p.79 p.80 p.81 p.82 p.83 p.84 p.85 p.86 p.87 p.88 p.89 p.90 p.91 p.92 p.93 p.94 p.95 p.96 p.97 p.98 p.99 p.100 p.101 p.102 p.103 p.104 p.105 p.106 p.107 p.108 p.109 p.110 p.111 p.112 p.113 p.114 p.115 p.116 p.117 p.118 p.119 p.120 p.121 p.122 p.123 p.124 p.125 p.126 p.127 p.128 p.129 p.130 p.131 p.132 p.133 p.134 p.135 p.136 p.137 p.138 p.139 p.140 p.141 p.142 p.143 p.144 p.145 p.146 p.147 p.148 p.149 p.150 p.151 p.152 p.153 p.154 p.155 p.156 p.157 p.158 p.159 p.160 p.161 p.162 p.163 p.164 p.165 p.166 p.167 p.168 p.169 p.170 p.171 p.172 p.173 p.174 p.175 p.176 p.177 p.178 p.179 p.180 p.181 p.182 p.183 p.184 p.185 p.186 p.187 p.188 p.189 p.190 p.191 p.192 p.193 p.194 p.195 p.196 p.197 p.198 p.199 p.200 p.201 p.202 p.203 p.204 p.205 p.206 p.207 p.208 p.209 p.210 p.211 p.212 p.213 p.214 p.215 p.216 p.217 p.218 p.219 p.220 p.221 p.222 p.223 p.224 p.225 p.226 p.227 p.228 p.229 p.230 p.231 p.232 p.233 p.234 p.235 p.236 p.237 p.238 p.239 p.240 p.241 p.242 p.243 p.244 p.245 p.246 p.247 p.248 p.249 p.250 p.251 p.252 p.253 p.254 p.255 p.256 p.257 p.258 p.259 p.260 p.261 p.262 p.263 p.264 p.265 p.266 p.267 p.268 p.269 p.270 p.271 p.272 p.273 p.274 p.275 p.276 p.277 p.278 p.279 p.280 p.281 p.282 p.283 p.284 p.285 p.286 p.287 p.288 p.289 p.290 p.291 p.292 p.293 p.294 p.295 p.296 p.297 p.298 p.299 p.300 p.301 p.302 p.303 p.304 p.305 p.306 p.307 p.308 p.309 p.310 p.311 p.312 p.313 p.314 p.315 p.316 p.317 p.318 p.319 p.320 p.321 p.322 p.323 p.324 p.325 p.326 p.327 p.328 p.329 p.330 p.331 p.332 p.333 p.334 p.335 p.336 p.337 p.338 p.339 p.340 p.341 p.342 p.343 p.344 p.345 p.346 p.347 p.348 p.349 p.350 p.351 p.352 p.353 p.354 p.355 p.356 p.357 p.358 p.359 p.360 p.361 p.362 p.363 p.364 p.365 p.366 p.367 p.368 p.369 p.370 p.371 p.372 p.373 p.374 p.375 p.376 p.377 p.378 p.379 p.380 p.381 p.382 p.383 p.384 p.385 p.386 p.387 p.388 p.389 p.390 p.391 p.392 p.393 p.394 p.395 p.396 p.397 p.398 p.399 p.400 p.401 p.402 p.403 p.404 p.405 p.406 p.407 p.408 p.409 p.410 p.411 p.412 p.413 p.414 p.415 p.416 p.417 p.418 p.419 p.420 p.421 p.422 p.423 p.424 p.425 p.426 p.427 p.428 p.429 p.430 p.431 p.432 p.433 p.434 p.435 p.436 p.437 p.438 p.439 p.440 p.441 p.442 p.443 p.444 p.445 p.446 p.447 p.448 p.449 p.450 p.451 p.452 p.453 p.454 p.455 p.456 p.457 p.458 p.459 p.460 p.461 p.462 p.463 p.464 p.465 p.466 p.467 p.468 p.469 p.470 p.471 p.472 p.473 p.474 p.475 p.476 p.477 p.478 p.479 p.480 p.481 p.482 p.483 p.484 p.485 p.486 p.487 p.488 p.489 p.490 p.491 p.492 p.493 p.494 p.495 p.496 p.497 p.498 p.499 p.500 p.501 p.502 p.503 p.504 p.505 p.506 p.507 p.508 p.509 p.510 p.511 p.512 p.513 p.514 p.515 p.516 p.517 p.518 p.519 p.520 p.521 p.522 p.523 p.524 p.525 p.526 p.527 p.528 p.529 p.530 p.531 p.532 p.533 p.534 p.535 p.536 p.537 p.538 p.539 p.540 p.541 p.542 p.543 p.544 p.545 p.546 p.547 p.548 p.549 p.550 p.551 p.552
}
extrude GearEx252 from Sk253 distance 8 new -> body Gear
cylinder Cy254 radius 4.15 height 10 base ((-95),(-3),0) axis Y cut targets Gear
box Bx255 size 10 7.98 1.85 corner ((-100),(-1.99),(-5)) join targets Gear
sketch Sk257 on XZ offset (-10) {
  polyline closed p = ((-70.532),(-32.44)) ((-69.898),(-32.637)) ((-69.536),(-32.81)) ((-69.191),(-33.03)) ((-68.856),(-33.283)) ((-68.533),(-33.564)) ((-68.782),(-34.25)) ((-69.211),(-34.257)) ((-69.629),(-34.236)) ((-70.036),(-34.182)) ((-70.424),(-34.082)) ((-71.036),(-33.825)) ((-71.612),(-35.059)) ((-71.021),(-35.363)) ((-70.695),(-35.596)) ((-70.393),(-35.873)) ((-70.108),(-36.18)) ((-69.838),(-36.513)) ((-70.203),(-37.145)) ((-70.626),(-37.077)) ((-71.034),(-36.984)) ((-71.425),(-36.861)) ((-71.79),(-36.695)) ((-72.348),(-36.335)) ((-73.13),(-37.451)) ((-72.6),(-37.853)) ((-72.32),(-38.139)) ((-72.071),(-38.464)) ((-71.843),(-38.816)) ((-71.635),(-39.19)) ((-72.104),(-39.75)) ((-72.509),(-39.61)) ((-72.895),(-39.447)) ((-73.259),(-39.257)) ((-73.589),(-39.031)) ((-74.077),(-38.58)) ((-75.04),(-39.543)) ((-74.588),(-40.03)) ((-74.362),(-40.36)) ((-74.173),(-40.724)) ((-74.01),(-41.11)) ((-73.87),(-41.515)) ((-74.429),(-41.984)) ((-74.803),(-41.776)) ((-75.155),(-41.549)) ((-75.481),(-41.299)) ((-75.766),(-41.019)) ((-76.168),(-40.49)) ((-77.284),(-41.271)) ((-76.924),(-41.829)) ((-76.758),(-42.194)) ((-76.635),(-42.585)) ((-76.542),(-42.993)) ((-76.474),(-43.416)) ((-77.107),(-43.782)) ((-77.439),(-43.511)) ((-77.746),(-43.226)) ((-78.023),(-42.924)) ((-78.256),(-42.598)) ((-78.56),(-42.007)) ((-79.794),(-42.583)) ((-79.537),(-43.196)) ((-79.437),(-43.583)) ((-79.383),(-43.99)) ((-79.362),(-44.408)) ((-79.369),(-44.837)) ((-80.056),(-45.086)) ((-80.336),(-44.763)) ((-80.589),(-44.428)) ((-80.809),(-44.083)) ((-80.982),(-43.722)) ((-81.179),(-43.087)) ((-82.494),(-43.439)) ((-82.347),(-44.087)) ((-82.316),(-44.487)) ((-82.334),(-44.896)) ((-82.386),(-45.312)) ((-82.467),(-45.733)) ((-83.186),(-45.859)) ((-83.406),(-45.492)) ((-83.597),(-45.119)) ((-83.754),(-44.74)) ((-83.862),(-44.354)) ((-83.945),(-43.695)) ((-85.302),(-43.814)) ((-85.27),(-44.478)) ((-85.308),(-44.876)) ((-85.397),(-45.276)) ((-85.52),(-45.677)) ((-85.673),(-46.077)) ((-86.404),(-46.077)) ((-86.557),(-45.677)) ((-86.68),(-45.276)) ((-86.769),(-44.876)) ((-86.807),(-44.478)) ((-86.775),(-43.814)) ((-88.132),(-43.695)) ((-88.215),(-44.354)) ((-88.323),(-44.74)) ((-88.48),(-45.119)) ((-88.671),(-45.492)) ((-88.891),(-45.859)) ((-89.61),(-45.733)) ((-89.691),(-45.312)) ((-89.743),(-44.896)) ((-89.761),(-44.487)) ((-89.73),(-44.087)) ((-89.583),(-43.439)) ((-90.898),(-43.087)) ((-91.095),(-43.722)) ((-91.268),(-44.083)) ((-91.488),(-44.428)) ((-91.741),(-44.763)) ((-92.022),(-45.086)) ((-92.708),(-44.837)) ((-92.715),(-44.408)) ((-92.694),(-43.99)) ((-92.64),(-43.583)) ((-92.54),(-43.196)) ((-92.283),(-42.583)) ((-93.517),(-42.007)) ((-93.821),(-42.598)) ((-94.054),(-42.924)) ((-94.331),(-43.226)) ((-94.638),(-43.511)) ((-94.97),(-43.782)) ((-95.603),(-43.416)) ((-95.535),(-42.993)) ((-95.442),(-42.585)) ((-95.319),(-42.194)) ((-95.153),(-41.829)) ((-94.793),(-41.271)) ((-95.909),(-40.49)) ((-96.311),(-41.019)) ((-96.596),(-41.299)) ((-96.922),(-41.549)) ((-97.274),(-41.776)) ((-97.648),(-41.984)) ((-98.207),(-41.515)) ((-98.067),(-41.11)) ((-97.904),(-40.724)) ((-97.715),(-40.36)) ((-97.489),(-40.03)) ((-97.037),(-39.543)) ((-98),(-38.58)) ((-98.488),(-39.031)) ((-98.818),(-39.257)) ((-99.182),(-39.447)) ((-99.568),(-39.61)) ((-99.973),(-39.75)) ((-100.442),(-39.19)) ((-100.234),(-38.816)) ((-100.006),(-38.464)) ((-99.757),(-38.139)) ((-99.477),(-37.853)) ((-98.947),(-37.451)) ((-99.729),(-36.335)) ((-100.287),(-36.695)) ((-100.652),(-36.861)) ((-101.043),(-36.984)) ((-101.451),(-37.077)) ((-101.874),(-37.145)) ((-102.239),(-36.513)) ((-101.969),(-36.18)) ((-101.684),(-35.873)) ((-101.382),(-35.596)) ((-101.056),(-35.363)) ((-100.465),(-35.059)) ((-101.041),(-33.825)) ((-101.654),(-34.082)) ((-102.041),(-34.182)) ((-102.448),(-34.236)) ((-102.866),(-34.257)) ((-103.295),(-34.25)) ((-103.544),(-33.564)) ((-103.221),(-33.283)) ((-102.886),(-33.03)) ((-102.541),(-32.81)) ((-102.179),(-32.637)) ((-101.545),(-32.44)) ((-101.897),(-31.125)) ((-102.545),(-31.272)) ((-102.944),(-31.303)) ((-103.354),(-31.285)) ((-103.77),(-31.233)) ((-104.19),(-31.152)) ((-104.317),(-30.433)) ((-103.95),(-30.213)) ((-103.577),(-30.022)) ((-103.198),(-29.865)) ((-102.812),(-29.758)) ((-102.153),(-29.674)) ((-102.272),(-28.317)) ((-102.935),(-28.349)) ((-103.334),(-28.311)) ((-103.734),(-28.222)) ((-104.135),(-28.099)) ((-104.535),(-27.946)) ((-104.535),(-27.215)) ((-104.135),(-27.063)) ((-103.734),(-26.939)) ((-103.334),(-26.85)) ((-102.935),(-26.812)) ((-102.272),(-26.844)) ((-102.153),(-25.487)) ((-102.812),(-25.404)) ((-103.198),(-25.296)) ((-103.577),(-25.14)) ((-103.95),(-24.948)) ((-104.317),(-24.728)) ((-104.19),(-24.009)) ((-103.77),(-23.928)) ((-103.354),(-23.876)) ((-102.944),(-23.858)) ((-102.545),(-23.889)) ((-101.897),(-24.036)) ((-101.545),(-22.721)) ((-102.179),(-22.524)) ((-102.541),(-22.351)) ((-102.886),(-22.131)) ((-103.221),(-21.878)) ((-103.544),(-21.598)) ((-103.295),(-20.911)) ((-102.866),(-20.905)) ((-102.448),(-20.926)) ((-102.041),(-20.979)) ((-101.654),(-21.079)) ((-101.041),(-21.336)) ((-100.465),(-20.102)) ((-101.056),(-19.798)) ((-101.382),(-19.565)) ((-101.684),(-19.288)) ((-101.969),(-18.981)) ((-102.239),(-18.649)) ((-101.874),(-18.016)) ((-101.451),(-18.084)) ((-101.043),(-18.177)) ((-100.652),(-18.301)) ((-100.287),(-18.466)) ((-99.729),(-18.826)) ((-98.947),(-17.71)) ((-99.477),(-17.308)) ((-99.757),(-17.023)) ((-100.006),(-16.697)) ((-100.234),(-16.345)) ((-100.442),(-15.971)) ((-99.973),(-15.412)) ((-99.568),(-15.552)) ((-99.182),(-15.715)) ((-98.818),(-15.904)) ((-98.488),(-16.13)) ((-98),(-16.582)) ((-97.037),(-15.619)) ((-97.489),(-15.131)) ((-97.715),(-14.801)) ((-97.904),(-14.437)) ((-98.067),(-14.051)) ((-98.207),(-13.646)) ((-97.648),(-13.177)) ((-97.274),(-13.385)) ((-96.922),(-13.613)) ((-96.596),(-13.862)) ((-96.311),(-14.143)) ((-95.909),(-14.672)) ((-94.793),(-13.891)) ((-95.153),(-13.332)) ((-95.319),(-12.967)) ((-95.442),(-12.577)) ((-95.535),(-12.168)) ((-95.603),(-11.745)) ((-94.97),(-11.38)) ((-94.638),(-11.65)) ((-94.331),(-11.935)) ((-94.054),(-12.237)) ((-93.821),(-12.563)) ((-93.517),(-13.154)) ((-92.283),(-12.578)) ((-92.54),(-11.966)) ((-92.64),(-11.578)) ((-92.694),(-11.172)) ((-92.715),(-10.753)) ((-92.708),(-10.325)) ((-92.022),(-10.075)) ((-91.741),(-10.399)) ((-91.488),(-10.733)) ((-91.268),(-11.079)) ((-91.095),(-11.44)) ((-90.898),(-12.074)) ((-89.583),(-11.722)) ((-89.73),(-11.074)) ((-89.761),(-10.675)) ((-89.743),(-10.265)) ((-89.691),(-9.849)) ((-89.61),(-9.429)) ((-88.891),(-9.302)) ((-88.671),(-9.669)) ((-88.48),(-10.042)) ((-88.323),(-10.421)) ((-88.215),(-10.807)) ((-88.132),(-11.466)) ((-86.775),(-11.347)) ((-86.807),(-10.684)) ((-86.769),(-10.285)) ((-86.68),(-9.885)) ((-86.557),(-9.484)) ((-86.404),(-9.084)) ((-85.673),(-9.084)) ((-85.52),(-9.484)) ((-85.397),(-9.885)) ((-85.308),(-10.285)) ((-85.27),(-10.684)) ((-85.302),(-11.347)) ((-83.945),(-11.466)) ((-83.862),(-10.807)) ((-83.754),(-10.421)) ((-83.597),(-10.042)) ((-83.406),(-9.669)) ((-83.186),(-9.302)) ((-82.467),(-9.429)) ((-82.386),(-9.849)) ((-82.334),(-10.265)) ((-82.316),(-10.675)) ((-82.347),(-11.074)) ((-82.494),(-11.722)) ((-81.179),(-12.074)) ((-80.982),(-11.44)) ((-80.809),(-11.079)) ((-80.589),(-10.733)) ((-80.336),(-10.399)) ((-80.056),(-10.075)) ((-79.369),(-10.325)) ((-79.362),(-10.753)) ((-79.383),(-11.172)) ((-79.437),(-11.578)) ((-79.537),(-11.966)) ((-79.794),(-12.578)) ((-78.56),(-13.154)) ((-78.256),(-12.563)) ((-78.023),(-12.237)) ((-77.746),(-11.935)) ((-77.439),(-11.65)) ((-77.107),(-11.38)) ((-76.474),(-11.745)) ((-76.542),(-12.168)) ((-76.635),(-12.577)) ((-76.758),(-12.967)) ((-76.924),(-13.332)) ((-77.284),(-13.891)) ((-76.168),(-14.672)) ((-75.766),(-14.143)) ((-75.481),(-13.862)) ((-75.155),(-13.613)) ((-74.803),(-13.385)) ((-74.429),(-13.177)) ((-73.87),(-13.646)) ((-74.01),(-14.051)) ((-74.173),(-14.437)) ((-74.362),(-14.801)) ((-74.588),(-15.131)) ((-75.04),(-15.619)) ((-74.077),(-16.582)) ((-73.589),(-16.13)) ((-73.259),(-15.904)) ((-72.895),(-15.715)) ((-72.509),(-15.552)) ((-72.104),(-15.412)) ((-71.635),(-15.971)) ((-71.843),(-16.345)) ((-72.071),(-16.697)) ((-72.32),(-17.023)) ((-72.6),(-17.308)) ((-73.13),(-17.71)) ((-72.348),(-18.826)) ((-71.79),(-18.466)) ((-71.425),(-18.301)) ((-71.034),(-18.177)) ((-70.626),(-18.084)) ((-70.203),(-18.016)) ((-69.838),(-18.649)) ((-70.108),(-18.981)) ((-70.393),(-19.288)) ((-70.695),(-19.565)) ((-71.021),(-19.798)) ((-71.612),(-20.102)) ((-71.036),(-21.336)) ((-70.424),(-21.079)) ((-70.036),(-20.979)) ((-69.629),(-20.926)) ((-69.211),(-20.905)) ((-68.782),(-20.911)) ((-68.533),(-21.598)) ((-68.856),(-21.878)) ((-69.191),(-22.131)) ((-69.536),(-22.351)) ((-69.898),(-22.524)) ((-70.532),(-22.721)) ((-70.18),(-24.036)) ((-69.532),(-23.889)) ((-69.133),(-23.858)) ((-68.723),(-23.876)) ((-68.307),(-23.928)) ((-67.887),(-24.009)) ((-67.76),(-24.728)) ((-68.127),(-24.948)) ((-68.5),(-25.14)) ((-68.879),(-25.296)) ((-69.265),(-25.404)) ((-69.924),(-25.487)) ((-69.805),(-26.844)) ((-69.142),(-26.812)) ((-68.743),(-26.85)) ((-68.343),(-26.939)) ((-67.942),(-27.063)) ((-67.542),(-27.215)) ((-67.542),(-27.946)) ((-67.942),(-28.099)) ((-68.343),(-28.222)) ((-68.743),(-28.311)) ((-69.142),(-28.349)) ((-69.805),(-28.317)) ((-69.924),(-29.674)) ((-69.265),(-29.758)) ((-68.879),(-29.865)) ((-68.5),(-30.022)) ((-68.127),(-30.213)) ((-67.76),(-30.433)) ((-67.887),(-31.152)) ((-68.307),(-31.233)) ((-68.723),(-31.285)) ((-69.133),(-31.303)) ((-69.532),(-31.272)) ((-70.18),(-31.125))
  fix p.1 p.2 p.3 p.4 p.5 p.6 p.7 p.8 p.9 p.10 p.11 p.12 p.13 p.14 p.15 p.16 p.17 p.18 p.19 p.20 p.21 p.22 p.23 p.24 p.25 p.26 p.27 p.28 p.29 p.30 p.31 p.32 p.33 p.34 p.35 p.36 p.37 p.38 p.39 p.40 p.41 p.42 p.43 p.44 p.45 p.46 p.47 p.48 p.49 p.50 p.51 p.52 p.53 p.54 p.55 p.56 p.57 p.58 p.59 p.60 p.61 p.62 p.63 p.64 p.65 p.66 p.67 p.68 p.69 p.70 p.71 p.72 p.73 p.74 p.75 p.76 p.77 p.78 p.79 p.80 p.81 p.82 p.83 p.84 p.85 p.86 p.87 p.88 p.89 p.90 p.91 p.92 p.93 p.94 p.95 p.96 p.97 p.98 p.99 p.100 p.101 p.102 p.103 p.104 p.105 p.106 p.107 p.108 p.109 p.110 p.111 p.112 p.113 p.114 p.115 p.116 p.117 p.118 p.119 p.120 p.121 p.122 p.123 p.124 p.125 p.126 p.127 p.128 p.129 p.130 p.131 p.132 p.133 p.134 p.135 p.136 p.137 p.138 p.139 p.140 p.141 p.142 p.143 p.144 p.145 p.146 p.147 p.148 p.149 p.150 p.151 p.152 p.153 p.154 p.155 p.156 p.157 p.158 p.159 p.160 p.161 p.162 p.163 p.164 p.165 p.166 p.167 p.168 p.169 p.170 p.171 p.172 p.173 p.174 p.175 p.176 p.177 p.178 p.179 p.180 p.181 p.182 p.183 p.184 p.185 p.186 p.187 p.188 p.189 p.190 p.191 p.192 p.193 p.194 p.195 p.196 p.197 p.198 p.199 p.200 p.201 p.202 p.203 p.204 p.205 p.206 p.207 p.208 p.209 p.210 p.211 p.212 p.213 p.214 p.215 p.216 p.217 p.218 p.219 p.220 p.221 p.222 p.223 p.224 p.225 p.226 p.227 p.228 p.229 p.230 p.231 p.232 p.233 p.234 p.235 p.236 p.237 p.238 p.239 p.240 p.241 p.242 p.243 p.244 p.245 p.246 p.247 p.248 p.249 p.250 p.251 p.252 p.253 p.254 p.255 p.256 p.257 p.258 p.259 p.260 p.261 p.262 p.263 p.264 p.265 p.266 p.267 p.268 p.269 p.270 p.271 p.272 p.273 p.274 p.275 p.276 p.277 p.278 p.279 p.280 p.281 p.282 p.283 p.284 p.285 p.286 p.287 p.288 p.289 p.290 p.291 p.292 p.293 p.294 p.295 p.296 p.297 p.298 p.299 p.300 p.301 p.302 p.303 p.304 p.305 p.306 p.307 p.308 p.309 p.310 p.311 p.312 p.313 p.314 p.315 p.316 p.317 p.318 p.319 p.320 p.321 p.322 p.323 p.324 p.325 p.326 p.327 p.328 p.329 p.330 p.331 p.332 p.333 p.334 p.335 p.336 p.337 p.338 p.339 p.340 p.341 p.342 p.343 p.344 p.345 p.346 p.347 p.348 p.349 p.350 p.351 p.352 p.353 p.354 p.355 p.356 p.357 p.358 p.359 p.360 p.361 p.362 p.363 p.364 p.365 p.366 p.367 p.368 p.369 p.370 p.371 p.372 p.373 p.374 p.375 p.376 p.377 p.378 p.379 p.380 p.381 p.382 p.383 p.384 p.385 p.386 p.387 p.388 p.389 p.390 p.391 p.392 p.393 p.394 p.395 p.396 p.397 p.398 p.399 p.400 p.401 p.402 p.403 p.404 p.405 p.406 p.407 p.408 p.409 p.410 p.411 p.412 p.413 p.414 p.415 p.416 p.417 p.418 p.419 p.420 p.421 p.422 p.423 p.424 p.425 p.426 p.427 p.428 p.429 p.430 p.431 p.432
}
extrude GearEx256 from Sk257 distance 6 new -> body Idler
sketch Sk259 on XZ offset (-2) {
  polyline closed p = ((-87.179),(-22.456)) ((-87.267),(-22.078)) ((-87.243),(-21.597)) ((-87.097),(-21.097)) ((-86.855),(-20.594)) ((-86.523),(-20.096)) ((-86.339),(-20.087)) ((-85.957),(-20.547)) ((-85.663),(-21.022)) ((-85.466),(-21.504)) ((-85.392),(-21.98)) ((-85.44),(-22.365)) ((-84.464),(-22.572)) ((-84.351),(-22.201)) ((-84.09),(-21.796)) ((-83.714),(-21.437)) ((-83.252),(-21.122)) ((-82.716),(-20.857)) ((-82.551),(-20.941)) ((-82.451),(-21.53)) ((-82.434),(-22.089)) ((-82.504),(-22.605)) ((-82.678),(-23.053)) ((-82.912),(-23.363)) ((-82.171),(-24.031)) ((-81.887),(-23.765)) ((-81.459),(-23.545)) ((-80.953),(-23.422)) ((-80.396),(-23.38)) ((-79.799),(-23.419)) ((-79.699),(-23.574)) ((-79.906),(-24.134)) ((-80.171),(-24.626)) ((-80.489),(-25.038)) ((-80.865),(-25.34)) ((-81.222),(-25.491)) ((-80.914),(-26.44)) ((-80.536),(-26.352)) ((-80.055),(-26.376)) ((-79.555),(-26.522)) ((-79.051),(-26.764)) ((-78.554),(-27.096)) ((-78.545),(-27.281)) ((-79.004),(-27.662)) ((-79.48),(-27.956)) ((-79.962),(-28.154)) ((-80.438),(-28.227)) ((-80.823),(-28.179)) ((-81.03),(-29.155)) ((-80.659),(-29.268)) ((-80.254),(-29.529)) ((-79.895),(-29.905)) ((-79.579),(-30.367)) ((-79.314),(-30.903)) ((-79.398),(-31.068)) ((-79.988),(-31.168)) ((-80.546),(-31.185)) ((-81.062),(-31.115)) ((-81.511),(-30.941)) ((-81.821),(-30.707)) ((-82.489),(-31.448)) ((-82.223),(-31.732)) ((-82.003),(-32.16)) ((-81.88),(-32.666)) ((-81.838),(-33.223)) ((-81.876),(-33.82)) ((-82.032),(-33.921)) ((-82.592),(-33.713)) ((-83.084),(-33.448)) ((-83.496),(-33.13)) ((-83.798),(-32.754)) ((-83.949),(-32.397)) ((-84.898),(-32.705)) ((-84.81),(-33.083)) ((-84.834),(-33.564)) ((-84.98),(-34.064)) ((-85.222),(-34.568)) ((-85.554),(-35.065)) ((-85.738),(-35.075)) ((-86.12),(-34.615)) ((-86.414),(-34.139)) ((-86.611),(-33.657)) ((-86.685),(-33.182)) ((-86.637),(-32.796)) ((-87.613),(-32.589)) ((-87.726),(-32.96)) ((-87.987),(-33.365)) ((-88.363),(-33.725)) ((-88.825),(-34.04)) ((-89.361),(-34.305)) ((-89.526),(-34.221)) ((-89.626),(-33.632)) ((-89.643),(-33.073)) ((-89.573),(-32.557)) ((-89.399),(-32.108)) ((-89.165),(-31.798)) ((-89.906),(-31.131)) ((-90.19),(-31.396)) ((-90.618),(-31.616)) ((-91.124),(-31.739)) ((-91.681),(-31.781)) ((-92.278),(-31.743)) ((-92.378),(-31.588)) ((-92.171),(-31.027)) ((-91.906),(-30.535)) ((-91.588),(-30.123)) ((-91.212),(-29.821)) ((-90.855),(-29.67)) ((-91.163),(-28.721)) ((-91.541),(-28.809)) ((-92.022),(-28.786)) ((-92.522),(-28.639)) ((-93.026),(-28.397)) ((-93.523),(-28.066)) ((-93.533),(-27.881)) ((-93.073),(-27.499)) ((-92.597),(-27.206)) ((-92.115),(-27.008)) ((-91.639),(-26.934)) ((-91.254),(-26.982)) ((-91.047),(-26.006)) ((-91.418),(-25.893)) ((-91.823),(-25.632)) ((-92.182),(-25.256)) ((-92.498),(-24.794)) ((-92.763),(-24.258)) ((-92.679),(-24.094)) ((-92.09),(-23.993)) ((-91.531),(-23.977)) ((-91.015),(-24.046)) ((-90.566),(-24.22)) ((-90.256),(-24.454)) ((-89.588),(-23.713)) ((-89.854),(-23.429)) ((-90.074),(-23.001)) ((-90.197),(-22.495)) ((-90.239),(-21.938)) ((-90.201),(-21.341)) ((-90.045),(-21.241)) ((-89.485),(-21.448)) ((-88.993),(-21.713)) ((-88.581),(-22.032)) ((-88.279),(-22.407)) ((-88.128),(-22.764))
  fix p.1 p.2 p.3 p.4 p.5 p.6 p.7 p.8 p.9 p.10 p.11 p.12 p.13 p.14 p.15 p.16 p.17 p.18 p.19 p.20 p.21 p.22 p.23 p.24 p.25 p.26 p.27 p.28 p.29 p.30 p.31 p.32 p.33 p.34 p.35 p.36 p.37 p.38 p.39 p.40 p.41 p.42 p.43 p.44 p.45 p.46 p.47 p.48 p.49 p.50 p.51 p.52 p.53 p.54 p.55 p.56 p.57 p.58 p.59 p.60 p.61 p.62 p.63 p.64 p.65 p.66 p.67 p.68 p.69 p.70 p.71 p.72 p.73 p.74 p.75 p.76 p.77 p.78 p.79 p.80 p.81 p.82 p.83 p.84 p.85 p.86 p.87 p.88 p.89 p.90 p.91 p.92 p.93 p.94 p.95 p.96 p.97 p.98 p.99 p.100 p.101 p.102 p.103 p.104 p.105 p.106 p.107 p.108 p.109 p.110 p.111 p.112 p.113 p.114 p.115 p.116 p.117 p.118 p.119 p.120 p.121 p.122 p.123 p.124 p.125 p.126 p.127 p.128 p.129 p.130 p.131 p.132 p.133 p.134 p.135 p.136 p.137 p.138 p.139 p.140 p.141 p.142 p.143 p.144
}
extrude GearEx258 from Sk259 distance 8 join targets Idler
cylinder Cy260 radius 5 height 2.02 base ((-86.039),(-4.01),27.581) axis Y join targets Idler
cylinder Cy261 radius 2.15 height 18 base ((-86.039),(-11),27.581) axis Y cut targets Idler
sketch Sk263 on XZ offset (-10) {
  polyline closed p = ((-68.226),(-33.533)) ((-68.578),(-33.369)) ((-68.943),(-33.055)) ((-69.246),(-32.632)) ((-69.494),(-32.131)) ((-69.682),(-31.563)) ((-69.576),(-31.412)) ((-68.979),(-31.394)) ((-68.423),(-31.456)) ((-67.922),(-31.596)) ((-67.502),(-31.831)) ((-67.227),(-32.106)) ((-66.463),(-31.465)) ((-66.686),(-31.147)) ((-66.845),(-30.693)) ((-66.896),(-30.175)) ((-66.86),(-29.617)) ((-66.739),(-29.031)) ((-66.571),(-28.953)) ((-66.045),(-29.237)) ((-65.595),(-29.568)) ((-65.231),(-29.94)) ((-64.985),(-30.354)) ((-64.885),(-30.729)) ((-63.902),(-30.556)) ((-63.936),(-30.169)) ((-63.846),(-29.696)) ((-63.632),(-29.222)) ((-63.322),(-28.756)) ((-62.924),(-28.31)) ((-62.74),(-28.326)) ((-62.426),(-28.835)) ((-62.202),(-29.347)) ((-62.073),(-29.851)) ((-62.066),(-30.333)) ((-62.167),(-30.707)) ((-61.23),(-31.049)) ((-61.066),(-30.697)) ((-60.752),(-30.332)) ((-60.329),(-30.029)) ((-59.827),(-29.781)) ((-59.26),(-29.593)) ((-59.109),(-29.699)) ((-59.091),(-30.296)) ((-59.153),(-30.852)) ((-59.293),(-31.353)) ((-59.528),(-31.773)) ((-59.803),(-32.048)) ((-59.162),(-32.812)) ((-58.844),(-32.589)) ((-58.389),(-32.43)) ((-57.871),(-32.379)) ((-57.313),(-32.415)) ((-56.728),(-32.536)) ((-56.65),(-32.704)) ((-56.934),(-33.23)) ((-57.265),(-33.68)) ((-57.637),(-34.044)) ((-58.051),(-34.29)) ((-58.426),(-34.39)) ((-58.252),(-35.373)) ((-57.866),(-35.339)) ((-57.393),(-35.429)) ((-56.918),(-35.643)) ((-56.453),(-35.953)) ((-56.007),(-36.351)) ((-56.023),(-36.535)) ((-56.532),(-36.849)) ((-57.044),(-37.073)) ((-57.548),(-37.202)) ((-58.029),(-37.209)) ((-58.404),(-37.108)) ((-58.745),(-38.045)) ((-58.393),(-38.209)) ((-58.029),(-38.523)) ((-57.725),(-38.946)) ((-57.478),(-39.448)) ((-57.29),(-40.015)) ((-57.396),(-40.166)) ((-57.993),(-40.184)) ((-58.549),(-40.122)) ((-59.05),(-39.982)) ((-59.47),(-39.747)) ((-59.744),(-39.472)) ((-60.509),(-40.113)) ((-60.286),(-40.431)) ((-60.127),(-40.886)) ((-60.076),(-41.404)) ((-60.112),(-41.962)) ((-60.233),(-42.547)) ((-60.4),(-42.625)) ((-60.926),(-42.341)) ((-61.377),(-42.01)) ((-61.741),(-41.638)) ((-61.987),(-41.224)) ((-62.087),(-40.849)) ((-63.07),(-41.023)) ((-63.035),(-41.409)) ((-63.125),(-41.882)) ((-63.34),(-42.357)) ((-63.65),(-42.822)) ((-64.047),(-43.268)) ((-64.232),(-43.252)) ((-64.545),(-42.743)) ((-64.77),(-42.231)) ((-64.899),(-41.727)) ((-64.906),(-41.246)) ((-64.804),(-40.871)) ((-65.742),(-40.53)) ((-65.906),(-40.882)) ((-66.22),(-41.246)) ((-66.643),(-41.55)) ((-67.144),(-41.797)) ((-67.712),(-41.985)) ((-67.863),(-41.879)) ((-67.881),(-41.282)) ((-67.819),(-40.726)) ((-67.679),(-40.225)) ((-67.444),(-39.805)) ((-67.169),(-39.531)) ((-67.81),(-38.766)) ((-68.128),(-38.989)) ((-68.582),(-39.148)) ((-69.1),(-39.199)) ((-69.658),(-39.163)) ((-70.244),(-39.042)) ((-70.322),(-38.875)) ((-70.038),(-38.349)) ((-69.707),(-37.898)) ((-69.335),(-37.534)) ((-68.921),(-37.288)) ((-68.546),(-37.188)) ((-68.719),(-36.205)) ((-69.106),(-36.24)) ((-69.579),(-36.15)) ((-70.053),(-35.935)) ((-70.519),(-35.625)) ((-70.965),(-35.228)) ((-70.949),(-35.043)) ((-70.44),(-34.73)) ((-69.928),(-34.505)) ((-69.424),(-34.376)) ((-68.942),(-34.369)) ((-68.568),(-34.471))
  fix p.1 p.2 p.3 p.4 p.5 p.6 p.7 p.8 p.9 p.10 p.11 p.12 p.13 p.14 p.15 p.16 p.17 p.18 p.19 p.20 p.21 p.22 p.23 p.24 p.25 p.26 p.27 p.28 p.29 p.30 p.31 p.32 p.33 p.34 p.35 p.36 p.37 p.38 p.39 p.40 p.41 p.42 p.43 p.44 p.45 p.46 p.47 p.48 p.49 p.50 p.51 p.52 p.53 p.54 p.55 p.56 p.57 p.58 p.59 p.60 p.61 p.62 p.63 p.64 p.65 p.66 p.67 p.68 p.69 p.70 p.71 p.72 p.73 p.74 p.75 p.76 p.77 p.78 p.79 p.80 p.81 p.82 p.83 p.84 p.85 p.86 p.87 p.88 p.89 p.90 p.91 p.92 p.93 p.94 p.95 p.96 p.97 p.98 p.99 p.100 p.101 p.102 p.103 p.104 p.105 p.106 p.107 p.108 p.109 p.110 p.111 p.112 p.113 p.114 p.115 p.116 p.117 p.118 p.119 p.120 p.121 p.122 p.123 p.124 p.125 p.126 p.127 p.128 p.129 p.130 p.131 p.132 p.133 p.134 p.135 p.136 p.137 p.138 p.139 p.140 p.141 p.142 p.143 p.144
}
extrude GearEx262 from Sk263 distance 6 new -> body Pinion
cylinder Cy264 radius 7 height 3.51 base ((-63.486),(-3.51),35.789) axis Y join targets Pinion
cylinder Cy265 radius 1.705 height 12 base ((-63.486),(-11),35.789) axis Y cut targets Pinion
cylinder Cy266 radius 1.6 height 8 base ((-63.486),(-1.75),35.789) axis (0.94,0,0.342) cut targets Pinion
sketch Sk267 on XZ offset (-3.05) {
  polyline closed p = ((-59.581),(-34.124)) ((-61.565),(-39.574)) ((-59.121),(-40.464)) ((-57.138),(-35.014))
  fix p.1 p.2 p.3 p.4
}
extrude Ex268 from Sk267 distance 2.6 cut targets Pinion
cylinder Cy269 radius 10.5 height 21.6 base ((-95),16.4,0) axis Y new -> body AxleCupR
cylinder Cy270 radius 4.15 height 8.9 base ((-95),15.4,0) axis Y cut targets AxleCupR
box Bx271 size 10 7.89 1.85 corner ((-100),16.41,(-5)) join targets AxleCupR
cylinder Cy272 radius 8 height 13 base ((-95),26,0) axis Y cut targets AxleCupR
box Bx273 size 23 13 3.4 corner ((-106.5),26,(-1.7)) cut targets AxleCupR
cylinder Cy274 radius 4 height 30 base ((-95),30,0) axis Y new -> body BoneR
cylinder Cy275 radius 5.5 height 8 base ((-95),28,0) axis Y join targets BoneR
cylinder Cy276 radius 1.5 height 19 base ((-104.5),32,0) axis X join targets BoneR
cylinder Cy277 radius 5.5 height 8 base ((-95),54,0) axis Y join targets BoneR
cylinder Cy278 radius 1.5 height 19 base ((-104.5),58,0) axis X join targets BoneR
cylinder Cy279 radius 10.5 height 21.6 base ((-95),(-38),0) axis Y new -> body AxleCupL
cylinder Cy280 radius 4.15 height 8.9 base ((-95),(-24.3),0) axis Y cut targets AxleCupL
box Bx281 size 10 7.89 1.85 corner ((-100),(-24.3),(-5)) join targets AxleCupL
cylinder Cy282 radius 8 height 13 base ((-95),(-39),0) axis Y cut targets AxleCupL
box Bx283 size 23 13 3.4 corner ((-106.5),(-39),(-1.7)) cut targets AxleCupL
cylinder Cy284 radius 4 height 30 base ((-95),(-60),0) axis Y new -> body BoneL
cylinder Cy285 radius 5.5 height 8 base ((-95),(-36),0) axis Y join targets BoneL
cylinder Cy286 radius 1.5 height 19 base ((-104.5),(-32),0) axis X join targets BoneL
cylinder Cy287 radius 5.5 height 8 base ((-95),(-62),0) axis Y join targets BoneL
cylinder Cy288 radius 1.5 height 19 base ((-104.5),(-58),0) axis X join targets BoneL
// ==== Reference bodies (not printed): servos, motor, ESC, battery, board
box Bx289 size 36.6 40.7 19.7 corner (18.312,(-10.2),(-25.8)) new -> body ServoF
box Bx290 size 2.5 54.6 19.7 corner (44.912,(-17.15),(-25.8)) join targets ServoF
cylinder Cy291 radius 6.5 height 2.2 base (54.912,0,(-15.95)) axis X join targets ServoF
cylinder Cy292 radius 2.9 height 6.3 base (57.112,0,(-15.95)) axis X join targets ServoF
box Bx293 size 36.6 40.7 19.7 corner ((-50.999),(-10.2),(-25.8)) new -> body ServoR
box Bx294 size 2.5 54.6 19.7 corner ((-43.499),(-17.15),(-25.8)) join targets ServoR
cylinder Cy295 radius 6.5 height 2.2 base ((-53.199),0,(-15.95)) axis X join targets ServoR
cylinder Cy296 radius 2.9 height 6.3 base ((-59.499),0,(-15.95)) axis X join targets ServoR
cylinder Cy297 radius 14 height 27 base ((-63.486),(-38),35.789) axis Y new -> body Motor
cylinder Cy298 radius 1.585 height 11 base ((-63.486),(-11),35.789) axis Y join targets Motor
cylinder Cy299 radius 4.5 height 2.5 base ((-63.486),(-40.5),35.789) axis Y join targets Motor
box Bx300 size 45 25 10 corner (28,(-62),(-26)) new -> body Esc
box Bx301 size 18 72 36 corner ((-9),(-36),(-26)) new -> body Battery
box Bx302 size 1 18 22.5 corner ((-0.5),43,(-24)) new -> body Esp32
// ==== Fit tests, placed away from the car: a servo wall window with its pilots, and a spline hub
box Bx303 size 56 4 30 corner (0,0,300) new -> body FitTestWall
box Bx304 size 41.1 6 20.1 corner (7.45,(-1),304.95) cut targets FitTestWall
cylinder Cy305 radius 1.25 height 6 base (4.65,(-1),310) axis Y cut targets FitTestWall
cylinder Cy306 radius 1.25 height 6 base (4.65,(-1),320) axis Y cut targets FitTestWall
cylinder Cy307 radius 1.25 height 6 base (51.35,(-1),310) axis Y cut targets FitTestWall
cylinder Cy308 radius 1.25 height 6 base (51.35,(-1),320) axis Y cut targets FitTestWall
cylinder Cy309 radius 7 height 8 base (70,0,315) axis Y new -> body FitTestHub
cylinder Cy310 radius 3 height 7.6 base (70,1.4,315) axis Y cut targets FitTestHub
cylinder Cy311 radius 1.6 height 10 base (70,(-1),315) axis Y cut targets FitTestHub
// ==== Steering pose: steer_front / steer_rear rotate each corner about its kingpin, move the tie
// rods, swing the horns about the servo shafts and the dog bones about their inner pins
move Mv312 bodies KnuckleFR, StubFR, WheelFR to ((-95),(-75),0)
move Mv313 bodies KnuckleFR, StubFR, WheelFR axis (0,0,1) angle steer_front
move Mv314 bodies KnuckleFR, StubFR, WheelFR to (95,75,0)
move Mv315 bodies KnuckleFL, StubFL, WheelFL to ((-95),75,0)
move Mv316 bodies KnuckleFL, StubFL, WheelFL axis (0,0,1) angle steer_front
move Mv317 bodies KnuckleFL, StubFL, WheelFL to (95,(-75),0)
move Mv318 bodies TieRodF to ((-67.612),(-69.178),0)
move Mv319 bodies TieRodF axis (0,0,1) angle (atan2((-11.643)*sin(steer_front), 150 + (-11.643)*cos(steer_front)))
move Mv320 bodies TieRodF to (67.612,69.178,0)
move Mv321 bodies TieRodF to ((-95),(-75),0)
move Mv322 bodies TieRodF axis (0,0,1) angle (0 deg - steer_front)
move Mv323 bodies TieRodF to (95,75,0)
move Mv324 bodies TieRodF to ((-122.388),(-80.822),0)
move Mv325 bodies TieRodF axis (0,0,1) angle steer_front
move Mv326 bodies TieRodF to (122.388,80.822,0)
move Mv327 bodies HornF to (0,0,15.95)
move Mv328 bodies HornF axis (1,0,0) angle (0 deg - atan2((((-27.388))*sin(steer_front) + ((-5.822))*(cos(steer_front)-1)), sqrt(21.95^2 - (((-27.388))*sin(steer_front) + ((-5.822))*(cos(steer_front)-1))^2)))
move Mv329 bodies HornF to (0,0,(-15.95))
move Mv330 bodies KnuckleRR, StubRR, WheelRR to (95,(-75),0)
move Mv331 bodies KnuckleRR, StubRR, WheelRR axis (0,0,1) angle steer_rear
move Mv332 bodies KnuckleRR, StubRR, WheelRR to ((-95),75,0)
move Mv333 bodies KnuckleRL, StubRL, WheelRL to (95,75,0)
move Mv334 bodies KnuckleRL, StubRL, WheelRL axis (0,0,1) angle steer_rear
move Mv335 bodies KnuckleRL, StubRL, WheelRL to ((-95),(-75),0)
move Mv336 bodies TieRodR to (63.699,(-68.347),0)
move Mv337 bodies TieRodR axis (0,0,1) angle (atan2((-13.306)*sin(steer_rear), 150 + (-13.306)*cos(steer_rear)))
move Mv338 bodies TieRodR to ((-63.699),68.347,0)
move Mv339 bodies TieRodR to (95,(-75),0)
move Mv340 bodies TieRodR axis (0,0,1) angle (0 deg - steer_rear)
move Mv341 bodies TieRodR to ((-95),75,0)
move Mv342 bodies TieRodR to (126.301,(-81.653),0)
move Mv343 bodies TieRodR axis (0,0,1) angle steer_rear
move Mv344 bodies TieRodR to ((-126.301),81.653,0)
move Mv345 bodies HornR to (0,0,15.95)
move Mv346 bodies HornR axis (1,0,0) angle (0 deg - atan2(((31.301)*sin(steer_rear) + ((-6.653))*(cos(steer_rear)-1)), sqrt(21.95^2 - ((31.301)*sin(steer_rear) + ((-6.653))*(cos(steer_rear)-1))^2)))
move Mv347 bodies HornR to (0,0,(-15.95))
move Mv348 bodies BoneR to (95,(-32),0)
move Mv349 bodies BoneR axis (0,0,1) angle (0 deg - atan2(17*sin(steer_rear), 43 - 17*cos(steer_rear)))
move Mv350 bodies BoneR to ((-95),32,0)
move Mv351 bodies BoneL to (95,32,0)
move Mv352 bodies BoneL axis (0,0,1) angle (0 deg - atan2(17*sin(steer_rear), 43 - 17*cos(steer_rear)))
move Mv353 bodies BoneL to ((-95),(-32),0)
move Mv354 bodies Gear, Axle, AxleCupR, AxleCupL, BoneR, BoneL, StubRR, StubRL, WheelRR, WheelRL to (95,0,0)
move Mv355 bodies Gear, Axle, AxleCupR, AxleCupL, BoneR, BoneL, StubRR, StubRL, WheelRR, WheelRL axis (0,1,0) angle gear_turn
move Mv356 bodies Gear, Axle, AxleCupR, AxleCupL, BoneR, BoneL, StubRR, StubRL, WheelRR, WheelRL to ((-95),0,0)
move Mv357 bodies Idler to (86.039,0,(-27.581))
move Mv358 bodies Idler axis (0,1,0) angle (0 deg - gear_turn*46/12)
move Mv359 bodies Idler to ((-86.039),0,27.581)
move Mv360 bodies Pinion to (63.486,0,(-35.789))
move Mv361 bodies Pinion axis (0,1,0) angle (gear_turn*46/12*36/12)
move Mv362 bodies Pinion to ((-63.486),0,35.789)
