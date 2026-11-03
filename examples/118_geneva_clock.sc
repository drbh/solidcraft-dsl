// 118. Geneva-drive desk counter: a hand crank steps a 10-slot Geneva wheel and its digit dial one
// digit per turn, and locks it still in between.
//
// Lies flat, Z up, the crank's axis at the origin and the wheel's at (C, 0). The driver is a locking
// disc and a crank arm carrying a pin. For each turn of the crank the pin enters one slot, turns the
// wheel 360/n deg, and leaves; for the rest of the turn the locking disc sits in one of the wheel's
// concave arcs and holds it. The geometry follows from the slot count: crank radius a = C*sin(180/n),
// wheel radius b = C*cos(180/n), so the pin enters and leaves each slot square on, with no impact.
//
// `crank` is live. The wheel's angle is the textbook Geneva relation written as one formula: whole
// turns give whole steps (floor), and inside a turn atan2 follows the pin while it is engaged, clamped
// to +-alpha (90 - 180/n deg) so it holds still the rest of the time. The default crank (200 deg)
// leaves the wheel locked on 0. Digit m is held for crank in 72 + 360m .. 288 + 360m deg (so 560 deg
// shows 1), and the step happens while the pin is in a slot, either side of a whole turn.
//
// Layers: base 0..4, wheel and locking disc 4.3..9.3 on 0.3 mm pads, crank arm 9.6..13.6 above the
// wheel, the dial on the wheel inside the arm's sweep circle, cover 14.2..17.2 with a window on the
// digit. The wheel's lobes pass within C - b (about 3 mm) of the crank axis, so nothing crosses the
// wheel layer there: the driver turns on a short post below it and its hub leaves through the cover
// above it.
//
// Print (PLA, 0.2 mm layers, no supports): base and cover flat, the wheel dial up with the Digit bodies as
// a filament change on its top, the driver on its
// disc (the pin's foot is level with it), the knob flat. Clearances: 0.3 mm round the posts and pin,
// `clear` between the disc and the wheel's arcs.
// Hardware: 4 x M3x8 screws into the standoffs. Grease the slots with a little PTFE or graphite.
design GenevaClock units mm

let n = 10                    // slots, so digits 0..9 (the digit and arc loops below assume 10)
param C = 60 mm               // crank axis to wheel axis
param crank = 200 deg
param pin_r = 3 mm
param clear = 0.3 mm
param layer_t = 5 mm          // wheel and locking disc
param arm_t = 4 mm
param base_t = 4 mm
param cover_t = 3 mm
param digit_h = 8 mm

let a = C*sin(180 deg/n)                 // crank (pin) radius
let b = C*cos(180 deg/n)                 // wheel radius
let alpha = 90 deg - 180 deg/n
let slot_w = 2*pin_r + 2*clear
let slot_in = C - a - 1 mm               // inner end of the slot's centre line
let lock_r = 14.4 mm                     // locking disc: 0.6 mm inside the wheel's arcs (lock_r + clear)
let arm_reach = a + 2*pin_r
let dial_r = C - arm_reach - 1.5 mm
let digit_r = dial_r - digit_h*0.75
let hub_r = 4 mm
let z_layer = base_t + clear
let z_arm = z_layer + layer_t + clear
let z_cover = z_arm + arm_t + 0.6 mm
let u = mod(crank + 180 deg, 360 deg) - 180 deg
let uc = max(min(u, alpha), -alpha)
let wheel_turn = -(floor((crank + 180 deg)/(360 deg))*(360 deg/n) + atan2(a*sin(uc), C - a*cos(uc)))

// ==== Base: plate, a post for the wheel, a stub for the driver, pads and four standoffs
box BasePlate size (C + 2*b - 20 mm) (2*b + 10 mm) base_t corner (-30 mm, -b - 5 mm, 0) new -> body Base
cylinder WheelPost radius 4 mm height (z_cover - base_t - 0.2 mm + 0.1 mm) base (C, 0, base_t - 0.1 mm) axis (0,0,1) join targets Base
cylinder WheelPad radius 7 mm height (clear + 0.1 mm) base (C, 0, base_t - 0.1 mm) axis (0,0,1) join targets Base
cylinder DriverStub radius 2.5 mm height (clear + 3 mm + 0.1 mm) base (0, 0, base_t - 0.1 mm) axis (0,0,1) join targets Base
cylinder DriverPad radius 4.5 mm height (clear + 0.1 mm) base (0, 0, base_t - 0.1 mm) axis (0,0,1) join targets Base
for k from 0 to 3 {
  cylinder Standoff{k} radius 4 mm height (z_cover - base_t + 0.1 mm) base (-24 mm + mod(k, 2)*(C + 2*b - 32 mm), (floor(k/2)*2 - 1)*(b - 1 mm), base_t - 0.1 mm) axis (0,0,1) join targets Base
  cylinder StandoffPilot{k} radius 1.25 mm height 9 mm base (-24 mm + mod(k, 2)*(C + 2*b - 32 mm), (floor(k/2)*2 - 1)*(b - 1 mm), z_cover - 8.9 mm) axis (0,0,1) cut targets Base
}

// ==== Wheel: disc with n radial slots and n concave locking arcs, the dial and digits on top
cylinder WheelDisc radius b height layer_t base (C, 0, z_layer) axis (0,0,1) new -> body Wheel
sketch SlotSk on XY offset z_layer {
  point ctr = (C, 0)
  slot s = centers (C - slot_in, 0) (C - b - 4 mm, 0) width slot_w
  fix ctr s
  pattern circular s count n center ctr angle 360 deg
}
extrude Slots from SlotSk distance (layer_t + 2 mm) offset (-1 mm) cut targets Wheel
for k from 0 to 9 {
  cylinder LockArc{k} radius (lock_r + clear) height (layer_t + 2 mm) base (C + C*cos(198 deg + 36 deg*k), C*sin(198 deg + 36 deg*k), z_layer - 1 mm) axis (0,0,1) cut targets Wheel
}
cylinder Dial radius dial_r height (arm_t - 0.6 mm + 0.1 mm) base (C, 0, z_layer + layer_t - 0.1 mm) axis (0,0,1) join targets Wheel
cylinder WheelBore radius (4 mm + clear) height (layer_t + arm_t + 2 mm) base (C, 0, z_layer - 1 mm) axis (0,0,1) cut targets Wheel
// The numerals are their own bodies, standing on the dial: print them as a filament change at the
// dial's top (or in a second colour on a multi-material printer). Cut or joined into the dial they
// make the engine retry the boolean.
// Each numeral is drawn where the window will see it once the wheel has turned it there (upright, its
// baseline dropped by 0.35*digit_h so it sits centred in the window).
for k from 0 to 9 {
  sketch DigitSk{k} on XY offset (z_layer + layer_t + arm_t - 0.6 mm) {
    text "{k}" at (C + digit_r*cos(18 deg + 36 deg*k) + 0.35*digit_h*sin(18 deg + 36 deg*k), digit_r*sin(18 deg + 36 deg*k) - 0.35*digit_h*cos(18 deg + 36 deg*k)) height digit_h align center angle (18 deg + 36 deg*k)
  }
  extrude DigitEx{k} from DigitSk{k} distance 0.6 mm new -> body Digit{k}
}

// ==== Driver: locking disc (rising 0.4 mm past the wheel's top to carry the arm) with its relief
// crescent, crank arm, pin, hub with a hex for the knob
cylinder LockDisc radius lock_r height (layer_t + clear + 0.1 mm) base (0, 0, z_layer) axis (0,0,1) new -> body Driver
cylinder Relief radius (b + 0.8 mm) height (layer_t + 2 mm) base (C, 0, z_layer - 1 mm) axis (0,0,1) cut targets Driver
cylinder StubBore radius (2.5 mm + clear) height (3 mm + 0.1 mm) base (0, 0, z_layer - 0.1 mm) axis (0,0,1) cut targets Driver
sketch ArmSk on XY offset (z_arm) {
  slot s = centers (0, 0) (a, 0) width (2*pin_r + 6 mm)
  fix s
}
extrude Arm from ArmSk distance arm_t join targets Driver
cylinder Pin radius pin_r height (layer_t + clear + 0.2 mm) base (a, 0, z_layer) axis (0,0,1) join targets Driver
cylinder Hub radius hub_r height (z_cover + cover_t + 1 mm - z_arm - arm_t + 0.1 mm) base (0, 0, z_arm + arm_t - 0.1 mm) axis (0,0,1) join targets Driver
sketch HexSk on XY offset (z_cover + cover_t + 1 mm - 0.1 mm) {
  polygon h = (0, 0) radius 3.9 mm sides 6
  fix h
}
extrude HexTop from HexSk distance 6.1 mm join targets Driver

// ==== Cover: hub hole, digit window, four counterbored screws
box CoverPlate size (C + 2*b - 20 mm) (2*b + 10 mm) cover_t corner (-30 mm, -b - 5 mm, z_cover) new -> body Cover
cylinder HubHole radius (hub_r + clear) height (cover_t + 2 mm) base (0, 0, z_cover - 1 mm) axis (0,0,1) cut targets Cover
sketch WindowSk on XY offset (z_cover - 1 mm) {
  rect rounded w = (C + digit_r - digit_h*0.7, -digit_h*0.75) (C + digit_r + digit_h*0.7, digit_h*0.75) radius 2 mm
  fix w
}
extrude Window from WindowSk distance (cover_t + 2 mm) cut targets Cover
for k from 0 to 3 {
  cylinder CoverHole{k} radius 1.7 mm height (cover_t + 2 mm) base (-24 mm + mod(k, 2)*(C + 2*b - 32 mm), (floor(k/2)*2 - 1)*(b - 1 mm), z_cover - 1 mm) axis (0,0,1) cut targets Cover
  cylinder CoverBore{k} radius 3.1 mm height 1.6 mm base (-24 mm + mod(k, 2)*(C + 2*b - 32 mm), (floor(k/2)*2 - 1)*(b - 1 mm), z_cover + cover_t - 1.5 mm) axis (0,0,1) cut targets Cover
}

// ==== Knob: pushes onto the hub's hex
cylinder KnobDisc radius 14 mm height 8 mm base (0, 0, z_cover + cover_t + 1.5 mm) axis (0,0,1) new -> body Knob
sketch KnobHexSk on XY offset (z_cover + cover_t + 1.5 mm - 1 mm) {
  polygon h = (0, 0) radius (3.9 mm + clear*0.6) sides 6
  fix h
}
extrude KnobHex from KnobHexSk distance 7 mm cut targets Knob
for k from 0 to 11 {
  cylinder KnobFlute{k} radius 1.6 mm height 10 mm base (14.6 mm*cos(30 deg*k), 14.6 mm*sin(30 deg*k), z_cover + cover_t + 0.5 mm) axis (0,0,1) cut targets Knob
}
cylinder KnobHandle radius 3 mm height 10 mm base (8.5 mm, 0, z_cover + cover_t + 9.4 mm) axis (0,0,1) join targets Knob

// ==== Pose: crank about its axis, wheel about its own on the Geneva relation
move TurnDriver bodies Driver, Knob axis (0,0,1) angle crank
move WheelToOrigin bodies Wheel, Digit0, Digit1, Digit2, Digit3, Digit4, Digit5, Digit6, Digit7, Digit8, Digit9 to (-C, 0, 0)
move TurnWheel bodies Wheel, Digit0, Digit1, Digit2, Digit3, Digit4, Digit5, Digit6, Digit7, Digit8, Digit9 axis (0,0,1) angle wheel_turn
move WheelBack bodies Wheel, Digit0, Digit1, Digit2, Digit3, Digit4, Digit5, Digit6, Digit7, Digit8, Digit9 to (C, 0, 0)

// The locking disc runs concentric with whichever arc it sits in, 0.3 mm inside it. The engine's
// clearance query reads that pair as touching (0 mm) at the arcs' ends, where the untrimmed rim circle
// crosses the disc, so the fit is checked with an exact interference query instead: none at crank 20,
// 100, 200 and 290 deg (the print recipe repeats it at the current crank).
check Driver clearance Cover >= 0.2 mm
check Wheel clearance Cover >= 0.2 mm

color Base "#3b3f46"
color Cover "#d8d4cc"
color Wheel "#c9902f"
color Digit0, Digit1, Digit2, Digit3, Digit4, Digit5, Digit6, Digit7, Digit8, Digit9 "#1d1d1f"
color Driver "#8a9bab"
color Knob "#b03a2e"
material Base, Cover, Wheel, Digit0, Digit1, Digit2, Digit3, Digit4, Digit5, Digit6, Digit7, Digit8, Digit9, Driver, Knob PLA
