// 157. Heat-set insert press: a soldering iron on a sprung carriage riding two 8 mm rods, with a depth stop.
//
// Z is up from the bench, the base plate's top face is at z = base_t. Two 8 mm steel rods stand
// in sockets at the back of the base, `rod_gap` apart, and a yoke ties their tops together. A
// carriage slides on the rods in two printed plain bearings, `rod_clear` round each rod. A
// compression spring on each rod, between its base boss and the carriage, holds the carriage up
// against the yoke. On the carriage's front a split collar clamps the iron's barrel
// (`iron_d`) with an M3 pinch screw, its tip pointing down at the work area in front of the rods.
//
// Push down on the thumb wing (left) to press the insert in; let go and the springs lift the iron.
// The depth stop is an M4 screw through the tab on the right. Set it so its tip lands on the stop
// pad on the base when the insert is flush, lock it with a nut, and every insert goes in to the
// same depth. The work area under the iron is flat for parts or a fixture plate.
//
// Hardware:
// - 2 x 8 mm smooth rod, rod_l long (200 mm). They press into the base and yoke sockets (8.1 mm
//   printed, so they grip; glue them if loose).
// - 2 x compression spring, 8.5-9 mm ID, about 0.8 mm wire, about 130 mm free length.
// - 1 x M3 x 20 pinch screw and nut for the collar.
// - 1 x M4 x 30 screw and nut for the depth stop.
// - 4 x stick-on rubber feet.
//
// Printing (PETG preferred near the iron, PLA works; the collar only touches the iron's grip,
// not its heater):
// - Base: flat, as modelled.
// - Carriage: flat on its underside, as modelled. The bores are vertical, and the pinch-screw
//   hole is a 45 deg diamond.
// - Yoke: upside down, so its blind sockets open upward.
// No supports anywhere.
design HeatSetInsertPress units mm

// Iron
param iron_d = 12 mm           // the barrel or grip the collar clamps
param iron_off = 50 mm         // iron axis in front of the rods
param collar_wall = 5 mm
param collar_h = 35 mm
// Frame
param rod_d = 8 mm
param rod_l = 200 mm
param rod_gap = 50 mm          // between the rod centres
param rod_clear = 0.25 mm      // carriage bearings, radial
param rod_grip = 0.05 mm       // base and yoke sockets, radial (a light press)
param base_t = 8 mm
param boss_h = 20 mm
// Carriage
param car_h = 35 mm
param car_d = 26 mm

let rod_y = 25 mm
let iron_y = rod_y - iron_off
let rod_z0 = 3.3 mm                          // 0.3 mm above the socket floor (3 mm)
let rod_z1 = rod_z0 + rod_l
let yoke_z0 = rod_z1 - 20 mm
let car_z1 = yoke_z0 - 0.5 mm
let car_z0 = car_z1 - car_h
let car_w = rod_gap + 20 mm
let collar_r = iron_d/2 + collar_wall
let stop_x = car_w/2 + 7.5 mm

// ==== Base: plate, two rod bosses, the stop pad
box Plate size 130 mm (rod_y + 20 mm - (iron_y - 50 mm)) base_t corner (-65 mm, iron_y - 50 mm, 0) new -> body Base
fillet PlateCorners edges (-65 mm, iron_y - 50 mm, base_t/2) (65 mm, iron_y - 50 mm, base_t/2) (-65 mm, rod_y + 20 mm, base_t/2) (65 mm, rod_y + 20 mm, base_t/2) radius 8 mm body Base
for k from 0 to 1 {
  cylinder Boss{k} radius 9 mm height (boss_h + 0.1 mm) base ((2*k - 1)*rod_gap/2, rod_y, base_t - 0.1 mm) axis Z join targets Base
  cylinder Socket{k} radius (rod_d/2 + rod_grip) height (base_t + boss_h) base ((2*k - 1)*rod_gap/2, rod_y, 3 mm) axis Z cut targets Base
}
cylinder StopPad radius 6 mm height 5.1 mm base (stop_x, rod_y, base_t - 0.1 mm) axis Z join targets Base

// ==== Rods (bought steel; shown in place)
cylinder RodL0 radius (rod_d/2) height rod_l base (-rod_gap/2, rod_y, rod_z0) axis Z new -> body RodL
cylinder RodR0 radius (rod_d/2) height rod_l base (rod_gap/2, rod_y, rod_z0) axis Z new -> body RodR
material RodL, RodR Steel
color RodL, RodR "#c9ccd1"

// ==== Yoke: ties the rod tops, blind sockets from below
box YokeBlock size car_w 20 mm 26 mm corner (-car_w/2, rod_y - 10 mm, yoke_z0) new -> body Yoke
for k from 0 to 1 {
  cylinder YokeSocket{k} radius (rod_d/2 + rod_grip) height 21.3 mm base ((2*k - 1)*rod_gap/2, rod_y, yoke_z0 - 1 mm) axis Z cut targets Yoke
}

// ==== Carriage: block on the rods, arm forward to the split collar, stop tab and thumb wing
box CarBlock size car_w car_d car_h corner (-car_w/2, rod_y - car_d/2, car_z0) new -> body Carriage
box Arm size 16 mm (rod_y - car_d/2 + 0.5 mm - iron_y) car_h corner (-8 mm, iron_y, car_z0) join targets Carriage
cylinder Collar radius collar_r height car_h base (0, iron_y, car_z0) axis Z join targets Carriage
box Ears size 18 mm 10 mm car_h corner (-9 mm, iron_y - collar_r - 8 mm, car_z0) join targets Carriage
box StopTab size 15.5 mm 20 mm 10 mm corner (car_w/2 - 0.5 mm, rod_y - 10 mm, car_z0) join targets Carriage
box ThumbWing size 30.5 mm 20 mm 10 mm corner (-car_w/2 - 30 mm, rod_y - 10 mm, car_z0) join targets Carriage
for k from 0 to 1 {
  cylinder Bearing{k} radius (rod_d/2 + rod_clear) height (car_h + 2 mm) base ((2*k - 1)*rod_gap/2, rod_y, car_z0 - 1 mm) axis Z cut targets Carriage
}
cylinder IronBore radius (iron_d/2 + 0.2 mm) height (car_h + 2 mm) base (0, iron_y, car_z0 - 1 mm) axis Z cut targets Carriage
box CollarSplit size 2 mm (collar_r + 10 mm) (car_h + 2 mm) corner (-1 mm, iron_y - collar_r - 9 mm, car_z0 - 1 mm) cut targets Carriage
// Pinch screw across the split: an M3 clearance hole as a 45 deg diamond (prints without support)
sketch PinchSk on YZ offset (-10 mm) {
  polygon h = (iron_y - collar_r - 3 mm, car_z0 + car_h/2) radius 2.3 mm sides 4
  fix h
}
extrude PinchHole from PinchSk distance 20 mm cut targets Carriage
cylinder StopHole radius 1.65 mm height 12 mm base (stop_x, rod_y, car_z0 - 1 mm) axis Z cut targets Carriage

// ==== Fits and printing
check Carriage clearance RodL >= (rod_clear - 0.01 mm)
check Carriage clearance RodR >= (rod_clear - 0.01 mm)
check Carriage clearance Yoke >= 0.4 mm
check Carriage clearance Base >= 100 mm
check Base clearance RodL >= (rod_grip - 0.01 mm)
check Yoke clearance RodR >= (rod_grip - 0.01 mm)
check Base overhang <= 45 deg
check Carriage overhang <= 45 deg
check Yoke overhang <= 45 deg up (0, 0, -1)

color Base, Yoke "#2b2d42"
color Carriage "#ef8354"
material Base, Yoke, Carriage PLA
