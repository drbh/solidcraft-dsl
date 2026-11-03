// 123. Motorised camera slider: a carriage on four V-wheels riding 2020 V-slot, a NEMA17 end and an
// idler end, with a GT2 belt loop under the rail.
//
// X runs along the rail (its ends at x = 0 and x = rail_l), Y across it, Z up. The rail's top is at
// z = 0 and the table at z = -60. The four wheels stand on vertical axles in the rail's two side
// slots, 2 x wheel_y apart: the +Y pair on fixed 6 mm spacers, the -Y pair on eccentric spacers that
// take up play. The belt loops under the rail round a 20T pulley on the motor and a 625 bearing
// idler; its top run is cut and both ends are clamped to the carriage's belt arm, which reaches
// down beside the wheels and under the rail. `carriage_x` places the carriage (live).
//
// Camera: a 1/4-20 nut sits in a hex pocket under the carriage; a 1/4-20 stud through it carries
// a ball head. The end plates bolt to the rail's tapped ends and stand on the table on their own
// feet.
//
// BOM: 2020 V-slot extrusion rail_l long (ends tapped M5), 4 x V-slot solid wheels (24 mm OD,
// 625 bearings), 2 x 6 mm aluminium spacers, 2 x 6 mm eccentric spacers, 4 x M5x30 + nylocs;
// NEMA17 stepper, GT2 20T pulley (5 mm bore), 6 mm GT2 belt (2 x rail_l + 200 mm), 4 x M3x8;
// 2 x 625ZZ bearings, M5x30 + nyloc + 2 washers (idler); 2 x M5x10 (end plates); 4 x M3x10
// (belt clamp), 2 x M3x12 (belt arm); 1/4-20 nut and 1/4-20 x 3/8 stud.
//
// Print (PETG or PLA, 0.2 mm, 4 walls, no supports):
//   Carriage   flat, nut pocket down        BeltArm   on its end (its profile flat)
//   Clamp      flat                         MotorEnd  on its rail-side plate face (the 23 mm boss
//                                                     hole then bridges; it prints clean)
//   IdlerEnd   on its rail-side face
design CameraSlider units mm

param rail_l = 500 mm
param carriage_x = 200 mm      // the carriage's centre along the rail
param wheel_y = 19.8 mm        // wheel axle offset from the rail's centre line (2020 V-slot)
param wheel_x = 30 mm          // half the wheelbase
param plate_t = 8 mm
param spacer = 6 mm
param belt_z = -34 mm          // pulley and idler axis height
param pulley_r = 6.37 mm       // 20T GT2 pitch radius
param belt_t = 1.4 mm
param fit = 0.2 mm

let plate_z = -10 mm + 5.1 mm + spacer          // wheel top (rail centre + half wheel) + spacer
let top_run = belt_z + pulley_r                  // the belt's pitch line on the top run
let jaw_lo = top_run + belt_t/2 + 0.1 mm         // the arm's jaw sits on the belt's back
let jaw_hi = -21 mm                              // 1 mm under the rail
let clamp_hi = top_run - belt_t/2 - 0.1 mm
let xc = carriage_x
let motor_x = -30 mm
let idler_x = rail_l + 30 mm

// ==== Carriage plate
box CarriagePlate size 90 mm 66 mm plate_t corner (xc - 45 mm, -32 mm, plate_z) new -> body Carriage
cylinder CameraBoss radius 18 mm height 3 mm base (xc, 0, plate_z + plate_t - 0.2 mm) join targets Carriage
for k from 0 to 1 {
  cylinder FixedAxle{k} radius 2.6 mm height (plate_t + 5 mm) base (xc + (2*k - 1)*wheel_x, wheel_y, plate_z - 1 mm) cut targets Carriage
  cylinder EccentricAxle{k} radius 3.6 mm height (plate_t + 5 mm) base (xc + (2*k - 1)*wheel_x, -wheel_y, plate_z - 1 mm) cut targets Carriage
  cylinder ArmPilot{k} radius 1.25 mm height 12 mm base (xc + (2*k - 1)*12 mm, 34.5 mm, plate_z + plate_t/2) axis (0,-1,0) cut targets Carriage
}
cylinder CameraHole radius 3.4 mm height (plate_t + 5 mm) base (xc, 0, plate_z - 1 mm) cut targets Carriage
sketch NutSk on XY offset (plate_z - 0.1 mm) {
  polygon nut = (xc, 0) radius 6.55 mm sides 6
  fix nut
}
extrude NutPocket from NutSk distance 5.9 mm cut targets Carriage

// ==== Belt arm: a web down the +Y side of the carriage and a jaw under the rail (profile on YZ)
sketch ArmSk on YZ offset (xc - 20 mm) {
  polyline closed a = (-10 mm, jaw_lo) (40 mm, jaw_lo) (40 mm, plate_z + plate_t) (34.2 mm, plate_z + plate_t) (34.2 mm, jaw_hi) (-10 mm, jaw_hi)
  fix a
}
extrude ArmEx from ArmSk distance 40 mm new -> body BeltArm
for k from 0 to 1 {
  cylinder ArmHole{k} radius 1.7 mm height 8 mm base (xc + (2*k - 1)*12 mm, 41 mm, plate_z + plate_t/2) axis (0,-1,0) cut targets BeltArm
  cylinder ClampPilotA{k} radius 1.25 mm height 5 mm base (xc + (2*k - 1)*14 mm, 7 mm, jaw_lo - 0.5 mm) cut targets BeltArm
  cylinder ClampPilotB{k} radius 1.25 mm height 5 mm base (xc + (2*k - 1)*14 mm, -7 mm, jaw_lo - 0.5 mm) cut targets BeltArm
}
// GT2 teeth across the jaw's underside, to key the belt ends
sketch TeethSk on XY offset (jaw_lo - 0.2 mm) {
  rect t = (xc - 19 mm, -4 mm) (xc - 18 mm, 4 mm)
  fix t
  pattern rectangular t count 19 spacing 2 mm
}
extrude Teeth from TeethSk distance 0.95 mm cut targets BeltArm

// Clamp plate under the belt, on four M3 screws into the jaw
box ClampPlate size 40 mm 20 mm 2.4 mm corner (xc - 20 mm, -10 mm, clamp_hi - 2.4 mm) new -> body Clamp
for k from 0 to 1 {
  cylinder ClampHoleA{k} radius 1.7 mm height 4 mm base (xc + (2*k - 1)*14 mm, 7 mm, clamp_hi - 3 mm) cut targets Clamp
  cylinder ClampHoleB{k} radius 1.7 mm height 4 mm base (xc + (2*k - 1)*14 mm, -7 mm, clamp_hi - 3 mm) cut targets Clamp
}

// ==== Motor end: a plate on the rail's end, standing on the table, with a flange for the NEMA17
box MotorPlate size 8 mm 48 mm 60 mm corner (-8 mm, -30 mm, -60 mm) new -> body MotorEnd
box MotorFlange size 45.5 mm 6 mm 46 mm corner (motor_x - 23.2 mm, 11.7 mm, belt_z - 23 mm) join targets MotorEnd
cylinder MotorBolt radius 2.75 mm height 10 mm base (-9 mm, 0, -10 mm) axis (1,0,0) cut targets MotorEnd
cylinder MotorBoltHead radius 5 mm height 4.1 mm base (-9 mm, 0, -10 mm) axis (1,0,0) cut targets MotorEnd
box MotorBeltWindow size 10 mm 14 mm 22 mm corner (-9 mm, -7 mm, belt_z - 11 mm) cut targets MotorEnd
cylinder MotorBossHole radius 11.6 mm height 8 mm base (motor_x, 11 mm, belt_z) axis (0,1,0) cut targets MotorEnd
for k from 0 to 1 {
  cylinder MotorScrewA{k} radius 1.7 mm height 8 mm base (motor_x + (2*k - 1)*15.5 mm, 11 mm, belt_z + 15.5 mm) axis (0,1,0) cut targets MotorEnd
  cylinder MotorScrewB{k} radius 1.7 mm height 8 mm base (motor_x + (2*k - 1)*15.5 mm, 11 mm, belt_z - 15.5 mm) axis (0,1,0) cut targets MotorEnd
}

// ==== Idler end: the same plate, with two cheeks carrying the idler on a slotted M5
box IdlerPlate size 8 mm 48 mm 60 mm corner (rail_l, -24 mm, -60 mm) new -> body IdlerEnd
box IdlerCheekA size 36.2 mm 4 mm 22 mm corner (rail_l + 7.8 mm, 5.4 mm, belt_z - 11 mm) join targets IdlerEnd
box IdlerCheekB size 36.2 mm 4 mm 22 mm corner (rail_l + 7.8 mm, -9.4 mm, belt_z - 11 mm) join targets IdlerEnd
cylinder IdlerBolt radius 2.75 mm height 10 mm base (rail_l - 1 mm, 0, -10 mm) axis (1,0,0) cut targets IdlerEnd
cylinder IdlerBoltHead radius 5 mm height 4.1 mm base (rail_l + 4 mm, 0, -10 mm) axis (1,0,0) cut targets IdlerEnd
box IdlerBeltWindow size 10 mm 14 mm 22 mm corner (rail_l - 1 mm, -7 mm, belt_z - 11 mm) cut targets IdlerEnd
sketch IdlerSlotSk on XZ offset (-10 mm) {
  slot s = centers (idler_x - 4 mm, -belt_z) (idler_x + 4 mm, -belt_z) width 5.4 mm
  fix s
}
extrude IdlerSlot from IdlerSlotSk distance 20 mm cut targets IdlerEnd

// ==== Bought parts, for fit (not printed)
box RefRail size rail_l 20 mm 20 mm corner (0, -10 mm, -20 mm) new -> body RefRail
for k from 0 to 1 {
  cylinder RefWheelFixed{k} radius 11.9 mm height 10.2 mm base (xc + (2*k - 1)*wheel_x, wheel_y, -15.1 mm) new -> body RefWheelFixed{k}
  cylinder RefWheelEcc{k} radius 11.9 mm height 10.2 mm base (xc + (2*k - 1)*wheel_x, -wheel_y, -15.1 mm) new -> body RefWheelEcc{k}
}
box RefMotor size 42.3 mm 40 mm 42.3 mm corner (motor_x - 21.15 mm, 17.9 mm, belt_z - 21.15 mm) new -> body RefMotor
cylinder RefPulley radius (pulley_r - 0.4 mm) height 7 mm base (motor_x, -3.5 mm, belt_z) axis (0,1,0) new -> body RefPulley
cylinder RefIdler radius 8 mm height 10 mm base (idler_x, -5 mm, belt_z) axis (0,1,0) new -> body RefIdler
box RefBeltBottom size (idler_x - motor_x) 6 mm belt_t corner (motor_x, -3 mm, belt_z - pulley_r - belt_t/2) new -> body RefBeltBottom

color Carriage, BeltArm, Clamp "#e2563b"
color MotorEnd, IdlerEnd "#2f3237"
color RefRail, RefWheelFixed0, RefWheelFixed1, RefWheelEcc0, RefWheelEcc1, RefIdler "#c7cbd1"
color RefMotor, RefPulley "#55585e"
color RefBeltBottom "#1b1b1b"
material Carriage, BeltArm, Clamp, MotorEnd, IdlerEnd PLA      // print in PETG; the engine's library has no PETG
material RefRail Aluminum
