// 84. Printable toy pull wagon: a tub bed on two bolted bolsters, wheels that roll on steel rod
// axles with TPU tyres, a front bolster that steers on an M3 kingpin and a folding pull handle.
//
// X runs along the wagon (front +X), Y across it, Z up; the floor is z = 0 and the bed is centred
// on the origin in plan. Every printed part fits a 256 mm bed (see 84_wagon.print.json).
//
// Bill of materials
//   2  3 mm steel rod, 120 mm         axles: a press fit in the hangers, the wheels spin on them
//   1  M3 x 25 bolt, nyloc nut         kingpin: bed floor, turntable washer, front bolster
//   1  M3 x 20 bolt, nyloc nut         handle pivot through the front bolster's clevis
//   2  M3 x 10 screws                  rear bolster to the bed floor (self-tapping into 2.5 holes)
//   4  printed TPU tyres               stretched over the wheel cores between their flanges
//
// Assembly: screw the rear bolster under the bed. Bolt the front bolster under the bed's front
// through the turntable washer, nyloc nut snug enough to steer without wobble. Press the rods
// through each bolster's hangers, slide a wheel onto each end, and press a cap on to keep it.
// Pin the handle between the clevis lugs; it folds up by `handle_angle` and down to level (0).
//
// Printing: the bed prints open side up (its floor edges are chamfered, not rounded), the
// bolsters on their sides, the wheels, tyres, caps, washer and handle flat. No supports.
//
// The pose is live: the handle swings about its pin first, then everything steered turns about
// the kingpin by `steer` (rotations about fixed points: move the pivot to the origin, rotate,
// move back). The print layouts turn the steered parts back before laying them flat.
design Wagon units mm

// Pose
param steer = 15 deg
param handle_angle = 40 deg

// Bed
param bed_l = 220 mm
param bed_w = 110 mm
param bed_d = 40 mm
param wall = 2.4 mm
param corner_r = 12 mm
param floor_chamfer = 3 mm

// Running gear
param wheel_r = 24 mm         // over the tyre
param tyre_t = 4 mm
param wheel_w = 10 mm
param flange = 1 mm
param wheelbase = 150 mm
param hanger_y = 45 mm        // hanger centre, either side
param hanger_t = 6 mm
param bar_t = 10 mm           // bolster bar along X
param bar_h = 8 mm
param rod_d = 3 mm
param spin_gap = 0.15 mm      // radial clearance of the wheel bore on the rod
param washer_t = 2 mm

// Handle
param handle_l = 150 mm
param handle_w = 10 mm
param handle_t = 6 mm
param grip_r = 14 mm

let axle_z = wheel_r
let bed_z = 2*wheel_r + 4 mm
let kingpin_x = wheelbase/2
let front_top = bed_z - washer_t - 0.2 mm
let pin_x = kingpin_x + 30 mm
let pin_z = front_top - bar_h - 6 mm
let core_r = wheel_r - tyre_t
let wheel_y = hanger_y + hanger_t/2 + 0.5 mm      // wheel inner face

// ==== Bed: a rounded tub, floor edges chamfered so it prints open side up
box BedBlank size bed_l bed_w bed_d corner (-bed_l/2, -bed_w/2, bed_z) new -> body Bed
fillet Corners edges (bed_l/2, bed_w/2, bed_z + bed_d/2) (-bed_l/2, bed_w/2, bed_z + bed_d/2) (-bed_l/2, -bed_w/2, bed_z + bed_d/2) (bed_l/2, -bed_w/2, bed_z + bed_d/2) radius corner_r body Bed
chamfer Floor edges (0, -bed_w/2, bed_z) distance floor_chamfer body Bed
shell Tub faces (0, 0, bed_z + bed_d) thickness wall inside body Bed
cylinder KingpinHole radius 1.7 mm height (wall + 2 mm) base (kingpin_x, 0, bed_z - 1 mm) cut targets Bed
cylinder RearScrewA radius 1.7 mm height (wall + 2 mm) base (-kingpin_x, 25 mm, bed_z - 1 mm) cut targets Bed
cylinder RearScrewB radius 1.7 mm height (wall + 2 mm) base (-kingpin_x, -25 mm, bed_z - 1 mm) cut targets Bed

// ==== Rear bolster (fixed): a bar under the floor and two hangers holding the axle rod. The
// hangers share the bar's back face, which is the face it prints on
box RearBar size bar_t (2*hanger_y + hanger_t) bar_h corner (-kingpin_x - bar_t/2, -hanger_y - hanger_t/2, bed_z - bar_h) new -> body RearBolster
box RearHangerA size (bar_t - 1 mm) hanger_t (bed_z - bar_h - axle_z + 6.5 mm) corner (-kingpin_x - bar_t/2, hanger_y - hanger_t/2, axle_z - 6 mm) join targets RearBolster
box RearHangerB size (bar_t - 1 mm) hanger_t (bed_z - bar_h - axle_z + 6.5 mm) corner (-kingpin_x - bar_t/2, -hanger_y - hanger_t/2, axle_z - 6 mm) join targets RearBolster
cylinder RearRodHole radius (rod_d/2) height (2*hanger_y + 2*hanger_t) base (-kingpin_x, -hanger_y - hanger_t, axle_z) axis (0,1,0) cut targets RearBolster
cylinder RearPilotA radius 1.25 mm height (bar_h - 1 mm) base (-kingpin_x, 25 mm, bed_z - bar_h + 1.5 mm) cut targets RearBolster
cylinder RearPilotB radius 1.25 mm height (bar_h - 1 mm) base (-kingpin_x, -25 mm, bed_z - bar_h + 1.5 mm) cut targets RearBolster

// ==== Front bolster (steers): the same, a kingpin hole and a clevis for the handle
cylinder TurntableDisc radius 14 mm height washer_t base (kingpin_x, 0, front_top + 0.1 mm) new -> body Washer
cylinder TurntableHole radius 1.7 mm height (washer_t + 2 mm) base (kingpin_x, 0, front_top - 0.9 mm) cut targets Washer
box FrontBar size bar_t (2*hanger_y + hanger_t) bar_h corner (kingpin_x - bar_t/2, -hanger_y - hanger_t/2, front_top - bar_h) new -> body FrontBolster
box FrontHangerA size (bar_t - 1 mm) hanger_t (front_top - bar_h - axle_z + 6.5 mm) corner (kingpin_x - bar_t/2, hanger_y - hanger_t/2, axle_z - 6 mm) join targets FrontBolster
box FrontHangerB size (bar_t - 1 mm) hanger_t (front_top - bar_h - axle_z + 6.5 mm) corner (kingpin_x - bar_t/2, -hanger_y - hanger_t/2, axle_z - 6 mm) join targets FrontBolster
box ClevisA size (pin_x + 6 mm - kingpin_x - bar_t/2 + 0.5 mm) 4 mm (front_top - bar_h - pin_z + 6.5 mm) corner (kingpin_x + bar_t/2 - 0.5 mm, handle_t/2 + 0.3 mm, pin_z - 6 mm) join targets FrontBolster
box ClevisB size (pin_x + 6 mm - kingpin_x - bar_t/2 + 0.5 mm) 4 mm (front_top - bar_h - pin_z + 6.5 mm) corner (kingpin_x + bar_t/2 - 0.5 mm, -handle_t/2 - 4.3 mm, pin_z - 6 mm) join targets FrontBolster
cylinder FrontRodHole radius (rod_d/2) height (2*hanger_y + 2*hanger_t) base (kingpin_x, -hanger_y - hanger_t, axle_z) axis (0,1,0) cut targets FrontBolster
cylinder FrontKingpin radius 1.7 mm height (bar_h + 2 mm) base (kingpin_x, 0, front_top - bar_h - 1 mm) cut targets FrontBolster
cylinder ClevisHole radius 1.7 mm height (handle_t + 12 mm) base (pin_x, -handle_t/2 - 6 mm, pin_z) axis (0,1,0) cut targets FrontBolster

// ==== Wheels: k = 0, 1 front (left, right), 2, 3 rear. A cored hub between two flanges with
// lightening holes, a TPU tyre on the core, a push-on cap outboard, all on axis Y
for k from 0 to 3 {
  cylinder Core{k} radius core_r height (wheel_w - 2*flange + 0.4 mm) base (wheelbase/2*(1 - 2*floor(k/2)), (1 - 2*mod(k, 2))*(wheel_y + flange - 0.2 mm) - mod(k, 2)*(wheel_w - 2*flange + 0.4 mm), axle_z) axis (0,1,0) new -> body Wheel{k}
  cylinder FlangeIn{k} radius (core_r + 1.6 mm) height flange base (wheelbase/2*(1 - 2*floor(k/2)), (1 - 2*mod(k, 2))*wheel_y - mod(k, 2)*flange, axle_z) axis (0,1,0) join targets Wheel{k}
  cylinder FlangeOut{k} radius (core_r + 1.6 mm) height flange base (wheelbase/2*(1 - 2*floor(k/2)), (1 - 2*mod(k, 2))*(wheel_y + wheel_w - flange) - mod(k, 2)*flange, axle_z) axis (0,1,0) join targets Wheel{k}
  cylinder Bore{k} radius (rod_d/2 + spin_gap) height (wheel_w + 2 mm) base (wheelbase/2*(1 - 2*floor(k/2)), (1 - 2*mod(k, 2))*(wheel_y - 1 mm) - mod(k, 2)*(wheel_w + 2 mm), axle_z) axis (0,1,0) cut targets Wheel{k}
  for j from 0 to 4 {
    cylinder Hole{k}x{j} radius 3.5 mm height (wheel_w + 2 mm) base (wheelbase/2*(1 - 2*floor(k/2)) + 11 mm*cos(72 deg*j), (1 - 2*mod(k, 2))*(wheel_y - 1 mm) - mod(k, 2)*(wheel_w + 2 mm), axle_z + 11 mm*sin(72 deg*j)) axis (0,1,0) cut targets Wheel{k}
  }
  cylinder TyreRing{k} radius wheel_r height (wheel_w - 2*flange - 0.2 mm) base (wheelbase/2*(1 - 2*floor(k/2)), (1 - 2*mod(k, 2))*(wheel_y + flange + 0.1 mm) - mod(k, 2)*(wheel_w - 2*flange - 0.2 mm), axle_z) axis (0,1,0) new -> body Tyre{k}
  cylinder TyreBore{k} radius core_r height wheel_w base (wheelbase/2*(1 - 2*floor(k/2)), (1 - 2*mod(k, 2))*wheel_y - mod(k, 2)*wheel_w, axle_z) axis (0,1,0) cut targets Tyre{k}
  cylinder CapBody{k} radius 4 mm height 4 mm base (wheelbase/2*(1 - 2*floor(k/2)), (1 - 2*mod(k, 2))*(wheel_y + wheel_w + 0.3 mm) - mod(k, 2)*4 mm, axle_z) axis (0,1,0) new -> body Cap{k}
  cylinder CapBore{k} radius (rod_d/2 - 0.05 mm) height 3 mm base (wheelbase/2*(1 - 2*floor(k/2)), (1 - 2*mod(k, 2))*(wheel_y + wheel_w + 0.3 mm) - mod(k, 2)*3 mm, axle_z) axis (0,1,0) cut targets Cap{k}
}
// The rods (hardware), hanger to cap on each side
cylinder FrontRod radius (rod_d/2) height (2*wheel_y + 2*wheel_w + 3.6 mm) base (kingpin_x, -wheel_y - wheel_w - 1.8 mm, axle_z) axis (0,1,0) new -> body FrontAxle
cylinder RearRod radius (rod_d/2) height (2*wheel_y + 2*wheel_w + 3.6 mm) base (-kingpin_x, -wheel_y - wheel_w - 1.8 mm, axle_z) axis (0,1,0) new -> body RearAxle

// ==== Handle: a flat tongue on the clevis pin with a ring grip at its end
sketch HandleSk on XZ offset (-handle_t/2) {
  slot s = centers (pin_x, -pin_z) (pin_x + handle_l, -pin_z) width handle_w
  fix s
}
extrude Tongue from HandleSk distance handle_t new -> body Handle
cylinder GripRing radius grip_r height handle_t base (pin_x + handle_l + grip_r - 3 mm, -handle_t/2, pin_z) axis (0,1,0) join targets Handle
cylinder GripHole radius (grip_r - 5 mm) height (handle_t + 2 mm) base (pin_x + handle_l + grip_r - 3 mm, -handle_t/2 - 1 mm, pin_z) axis (0,1,0) cut targets Handle
cylinder PinHole radius 1.7 mm height (handle_t + 2 mm) base (pin_x, -handle_t/2 - 1 mm, pin_z) axis (0,1,0) cut targets Handle

// Swing the handle up about its pin, then steer the front gear about the kingpin
move HandleToPin bodies Handle to (-pin_x, 0, -pin_z)
move HandleSwing bodies Handle axis (0,1,0) angle (-handle_angle)
move HandleBack bodies Handle to (pin_x, 0, pin_z)
move SteerToPin bodies FrontBolster, FrontAxle, Wheel0, Wheel1, Tyre0, Tyre1, Cap0, Cap1, Handle to (-kingpin_x, 0, 0)
move Steer bodies FrontBolster, FrontAxle, Wheel0, Wheel1, Tyre0, Tyre1, Cap0, Cap1, Handle axis (0,0,1) angle steer
move SteerBack bodies FrontBolster, FrontAxle, Wheel0, Wheel1, Tyre0, Tyre1, Cap0, Cap1, Handle to (kingpin_x, 0, 0)

// ==== Finish
color Bed "#c4161c"
color Tyre0, Tyre1, Tyre2, Tyre3 "#1a1a1a"
color Wheel0, Wheel1, Wheel2, Wheel3, Cap0, Cap1, Cap2, Cap3 "#e8e8ea"
color RearBolster, FrontBolster, Washer, Handle "#2b2b2e"
color FrontAxle, RearAxle "#9a9da2"
material Bed, RearBolster, FrontBolster, Washer, Handle, Wheel0, Wheel1, Wheel2, Wheel3, Cap0, Cap1, Cap2, Cap3 PLA
// Tyres print in TPU (95A); the engine library has no TPU, so they keep the default material
material FrontAxle, RearAxle Steel
