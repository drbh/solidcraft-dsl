// 82. Printable balanced-arm desk lamp: bolted printed arms, extension springs, a weighted base
// and a 5 V USB LED in a bell shade that prints without supports.
//
// Z is up and the lamp reaches toward +X. Everything except the hardware prints in PLA or PETG,
// and every part fits a 256 mm bed (see 82_desk_lamp.print.json for the plates).
//
// Bill of materials
//   1  M10x1 hollow lamp nipple, 30 mm, 2 nuts, 2 washers   base to turret pivot, cable runs inside
//   2  M5 x 40 bolt, nyloc nut, 4 washers                  shoulder and elbow (washers set friction)
//   1  M4 x 30 bolt, nyloc nut, 2 washers                  head
//   3  M3 x 30 bolt, nyloc nut                             spring crossbolts on the arms
//   1  M3 x 10 bolt, nut                                   spring anchor on the turret
//   1  extension spring 6 mm OD x 0.6 wire x 30 mm free     lower arm: tail to turret ear
//   1  extension spring 8 mm OD x 0.8 wire x 60 mm free     upper arm: tail to lower crossbolt
//   1  5 V USB COB LED disc, 32 mm, about 3 W               glued to the spreader
//   1  aluminium disc 44 mm x 1.5 mm                        heat spreader between LED and carrier
//   1  KCD11 mini rocker switch (13 x 8.8 mm panel cut)     in the base wall, on the cable's 5 V line
//   1  USB-A cable, about 2 m                               in through the base, up the nipple
//   4  M3 x 8 screws                                        bottom cover into the base bosses
//   2  M3 x 6 screws                                        carrier into the shade neck
//      zip ties                                             cable to the lower arm's tie holes
//      ballast: steel washers or a bag of BBs              in the base cavity, about 600 g
//
// Assembly: thread the cable in through the base's strain relief (zip tie round the post), wire
// the rocker, and feed the cable up through the nipple, which clamps the turret onto the base's top
// wall (a washer each side lets the turret swivel). The turret's tongue sits between the two lower
// arm bars on the shoulder bolt; the upper arm sits between their tops on the elbow bolt; the
// shade's ears straddle the upper arm's tip on the head bolt. Tighten the nyloc nuts until each
// joint holds its pose, then hook the springs: lower spring from the lower arms' tail crossbolt to
// the turret's front ear, upper spring from the upper arm's tail to the lower arms' crossbolt.
// Fill the base with ballast and screw the cover on.
//
// Printing: the base prints upside down (its top on the bed), the bars on their sides, the shade
// on its rim with the neck and ears up (every wall leans 45 deg or less), turret and cover as
// modelled. A brim helps the shade.
//
// The pose is live. Everything is modelled in the zero pose (lower arm straight up, upper arm
// level, shade pointing down) and posed tip first, each part turning about a pivot that has not
// moved yet, so only the angles are formulas:
//   base_yaw     turret turn about Z
//   lower_angle  lower arm forward from vertical
//   upper_angle  upper arm up from square to the lower arm
//   head_angle   shade forward from square to the upper arm
// The print layouts turn each part back by the same angles before laying it flat.
design DeskLamp units mm

// Pose
param base_yaw = 20 deg
param lower_angle = 25 deg
param upper_angle = 45 deg
param head_angle = -10 deg

// Base and turret
param base_r = 80 mm
param base_h = 32 mm
param base_wall = 3.4 mm
param top_wall = 3 mm
param cover_t = 3 mm
param turret_r = 34 mm
param turret_t = 10 mm
param tongue_t = 12 mm

// Arms
param lower_l = 190 mm       // shoulder to elbow
param lower_tail = 28 mm     // below the shoulder, for the lower spring
param upper_l = 185 mm       // elbow to head
param upper_tail = 45 mm     // behind the elbow, for the upper spring
param bar_w = 18 mm
param bar_t = 6 mm
param gap = 0.3 mm           // side clearance at every joint

// Shade and light
param shade_r = 70 mm
param shade_wall = 2 mm
param neck_r = 28 mm
param ear_l = 22 mm
param led_r = 16 mm
param spreader_r = 22 mm

let shoulder_x = -22 mm
let shoulder_z = base_h + turret_t + 40 mm
let elbow_z = shoulder_z + lower_l
let head_x = shoulder_x + upper_l
let bar_y = tongue_t/2 + gap              // inner face of each lower bar
let shade_top = 128 mm                    // shade's boss top, in its own frame (rim at 0)
let pivot_h = shade_top + ear_l           // head pivot above the rim
let shade_z = elbow_z - pivot_h           // where the rim sits in the zero pose

// ==== Base: a hollow drum, open below for ballast, closed by a screwed cover
cylinder BaseBlank radius base_r height base_h new -> body Base
fillet BaseEdge edges (base_r, 0, base_h) radius 6 mm body Base
cylinder BaseCavity radius (base_r - base_wall) height (base_h - top_wall + 1 mm) base (0, 0, -1 mm) cut targets Base
cylinder BaseRabbet radius (base_r - 2.2 mm) height (cover_t + 1 mm) base (0, 0, -1 mm) cut targets Base
for k from 0 to 3 {
  cylinder Boss{k} radius 5 mm height (base_h - top_wall - cover_t + 0.3 mm) base (44 mm*cos((45 + 90*k)*1 deg), 44 mm*sin((45 + 90*k)*1 deg), cover_t) join targets Base
  cylinder Pilot{k} radius 1.25 mm height 10 mm base (44 mm*cos((45 + 90*k)*1 deg), 44 mm*sin((45 + 90*k)*1 deg), cover_t - 0.5 mm) cut targets Base
}
cylinder NippleHole radius 5.2 mm height (top_wall + 2 mm) base (0, 0, base_h - top_wall - 1 mm) cut targets Base
// Cable in at the back, past a strain-relief post with a zip-tie slot
cylinder CableExit radius 3.2 mm height (base_wall + 2 mm) base (-base_r - 1 mm, 0, 14 mm) axis X cut targets Base
box ReliefPost size 8 mm 16 mm 14.5 mm corner (-70 mm, -8 mm, base_h - top_wall - 14 mm) join targets Base
box ReliefSlot size 3 mm 18 mm 5 mm corner (-67.5 mm, -9 mm, base_h - top_wall - 11 mm) cut targets Base
// Rocker switch window at the front
box RockerWindow size (base_wall + 4 mm) 13 mm 8.8 mm corner (base_r - base_wall - 2 mm, -6.5 mm, 12 mm) cut targets Base

cylinder CoverDisc radius (base_r - 2.2 mm - gap) height cover_t new -> body Cover
for k from 0 to 3 {
  cylinder CoverHole{k} radius 1.7 mm height (cover_t + 2 mm) base (44 mm*cos((45 + 90*k)*1 deg), 44 mm*sin((45 + 90*k)*1 deg), -1 mm) cut targets Cover
}

// The hollow nipple: base top wall to turret top (hardware)
cylinder NippleTube radius 5 mm height 30 mm base (0, 0, base_h - top_wall - 8 mm) new -> body Nipple
cylinder NippleBore radius 3.5 mm height 32 mm base (0, 0, base_h - top_wall - 9 mm) cut targets Nipple

// ==== Turret: a disc on the base with the shoulder tongue behind the nipple and a spring ear in front
cylinder TurretDisc radius turret_r height turret_t base (0, 0, base_h) new -> body Turret
cylinder TurretBore radius 5.2 mm height (turret_t + 2 mm) base (0, 0, base_h - 1 mm) cut targets Turret
sketch TongueSk on XZ offset (-tongue_t/2) {
  slot s = centers (shoulder_x, -(base_h + 12 mm)) (shoulder_x, -shoulder_z) width 20 mm
  fix s
}
extrude Tongue from TongueSk distance tongue_t join targets Turret
cylinder TongueHole radius 2.65 mm height (tongue_t + 2 mm) base (shoulder_x, -tongue_t/2 - 1 mm, shoulder_z) axis (0,1,0) cut targets Turret
box SpringEar size 8 mm 6 mm 12.5 mm corner (12 mm, -3 mm, base_h + turret_t - 0.5 mm) join targets Turret
cylinder SpringEarHole radius 1.65 mm height 8 mm base (16 mm, -4 mm, base_h + turret_t + 8 mm) axis (0,1,0) cut targets Turret

// ==== Lower arm: two bars either side of the tongue, shoulder to elbow
sketch LowerSk on XZ offset bar_y {
  slot s = centers (shoulder_x, -(shoulder_z - lower_tail)) (shoulder_x, -elbow_z) width bar_w
  fix s
}
extrude LowerBarL from LowerSk distance bar_t new -> body LowerL
extrude LowerBarR from LowerSk distance bar_t offset (-2*bar_y - bar_t) new -> body LowerR
cylinder ShoulderHole radius 2.65 mm height (2*bar_y + 2*bar_t + 2 mm) base (shoulder_x, -bar_y - bar_t - 1 mm, shoulder_z) axis (0,1,0) cut targets LowerL, LowerR
cylinder ElbowHole radius 2.65 mm height (2*bar_y + 2*bar_t + 2 mm) base (shoulder_x, -bar_y - bar_t - 1 mm, elbow_z) axis (0,1,0) cut targets LowerL, LowerR
cylinder TailHole radius 1.65 mm height (2*bar_y + 2*bar_t + 2 mm) base (shoulder_x, -bar_y - bar_t - 1 mm, shoulder_z - lower_tail + 6 mm) axis (0,1,0) cut targets LowerL, LowerR
cylinder CrossHole radius 1.65 mm height (2*bar_y + 2*bar_t + 2 mm) base (shoulder_x, -bar_y - bar_t - 1 mm, elbow_z - 60 mm) axis (0,1,0) cut targets LowerL, LowerR
for k from 1 to 2 {
  cylinder TieHole{k} radius 1.6 mm height (2*bar_y + 2*bar_t + 2 mm) base (shoulder_x + 4.5 mm, -bar_y - bar_t - 1 mm, shoulder_z + 55 mm*k) axis (0,1,0) cut targets LowerL, LowerR
}

// ==== Upper arm: one bar between the lower bars' tops, from its spring tail out to the head
sketch UpperSk on XZ offset (-tongue_t/2) {
  slot s = centers (shoulder_x - upper_tail, -elbow_z) (head_x, -elbow_z) width bar_w
  fix s
}
extrude UpperBar from UpperSk distance tongue_t new -> body UpperArm
cylinder UpperElbowHole radius 2.65 mm height (tongue_t + 2 mm) base (shoulder_x, -tongue_t/2 - 1 mm, elbow_z) axis (0,1,0) cut targets UpperArm
cylinder UpperTipHole radius 2.15 mm height (tongue_t + 2 mm) base (head_x, -tongue_t/2 - 1 mm, elbow_z) axis (0,1,0) cut targets UpperArm
cylinder UpperTailHole radius 1.65 mm height (tongue_t + 2 mm) base (shoulder_x - upper_tail + 2 mm, -tongue_t/2 - 1 mm, elbow_z) axis (0,1,0) cut targets UpperArm

// ==== Shade: revolved in its own frame (rim on z = 0, sketch v is -z), every wall at 45 deg or
// steeper, a flat boss on top carrying two ears; then moved under the head pivot
sketch ShadeSk on XZ {
  polyline closed o = (shade_r, 0) (neck_r, -95 mm) (neck_r, -110 mm) (13 mm, -125 mm) (13 mm, -shade_top) (4 mm, -shade_top) (4 mm, -125.5 mm) (9.67 mm, -125.5 mm) (neck_r - shade_wall, -109.17 mm) (neck_r - shade_wall, -94.6 mm) (shade_r - 2.2 mm, 0)
  fix o
}
revolve ShadeRev from ShadeSk axis Z new -> body Shade
// Each ear: a flat-bottomed plate standing on the boss, rounded over the pivot (one outline, so
// no side faces are shared between pieces)
sketch EarSk on XZ offset (tongue_t/2 + gap) {
  line bottom = (-6 mm, -(shade_top - 0.5 mm)) (6 mm, -(shade_top - 0.5 mm))
  line right = bottom.end (6 mm, -pivot_h)
  arc top = start right.end through (0, -(pivot_h + 6 mm)) end (-6 mm, -pivot_h)
  line left = top.end bottom.start
  fix bottom right top left
}
extrude EarA from EarSk distance 4 mm join targets Shade
extrude EarB from EarSk distance 4 mm offset (-tongue_t - 2*gap - 4 mm) join targets Shade
cylinder EarHole radius 2.15 mm height (tongue_t + 2*gap + 10 mm) base (0, -tongue_t/2 - gap - 5 mm, pivot_h) axis (0,1,0) cut targets Shade
cylinder NeckScrews radius 1.7 mm height (2*neck_r + 4 mm) base (-neck_r - 2 mm, 0, 105 mm) axis X cut targets Shade

// LED carrier: a vented disc with a ring the neck screws bite into; LED and spreader under it
cylinder CarrierDisc radius (neck_r - shade_wall - gap) height 3 mm base (0, 0, 97 mm) new -> body Carrier
cylinder CarrierRing radius (neck_r - shade_wall - gap) height 9 mm base (0, 0, 99.5 mm) join targets Carrier
cylinder CarrierRingBore radius (neck_r - shade_wall - gap - 2 mm) height 10 mm base (0, 0, 100 mm) cut targets Carrier
cylinder CarrierCable radius 3 mm height 5 mm base (0, 0, 96 mm) cut targets Carrier
for k from 0 to 5 {
  cylinder Vent{k} radius 2.5 mm height 5 mm base (17 mm*cos(60 deg*k + 30 deg), 17 mm*sin(60 deg*k + 30 deg), 96 mm) cut targets Carrier
}
cylinder CarrierTap radius 1.25 mm height (2*neck_r) base (-neck_r, 0, 105 mm) axis X cut targets Carrier
cylinder SpreaderDisc radius spreader_r height 1.5 mm base (0, 0, 95.5 mm) new -> body Spreader
cylinder LedDisc radius led_r height 2.5 mm base (0, 0, 93 mm) new -> body Led

move ShadeIn bodies Shade, Carrier, Spreader, Led to (head_x, 0, shade_z)

// ==== Joint hardware: bolts on the pivot axes, so they follow the pose
cylinder ShoulderShank radius 2.5 mm height (2*bar_y + 2*bar_t + 4 mm) base (shoulder_x, -bar_y - bar_t - 2 mm, shoulder_z) axis (0,1,0) new -> body ShoulderBolt
cylinder ShoulderHead radius 4.25 mm height 5 mm base (shoulder_x, bar_y + bar_t + 1.5 mm, shoulder_z) axis (0,1,0) join targets ShoulderBolt
cylinder ElbowShank radius 2.5 mm height (2*bar_y + 2*bar_t + 4 mm) base (shoulder_x, -bar_y - bar_t - 2 mm, elbow_z) axis (0,1,0) new -> body ElbowBolt
cylinder ElbowHead radius 4.25 mm height 5 mm base (shoulder_x, bar_y + bar_t + 1.5 mm, elbow_z) axis (0,1,0) join targets ElbowBolt
cylinder HeadShank radius 2 mm height (tongue_t + 2*gap + 12 mm) base (head_x, -tongue_t/2 - gap - 6 mm, elbow_z) axis (0,1,0) new -> body HeadBolt
cylinder HeadHead radius 3.5 mm height 4 mm base (head_x, tongue_t/2 + gap + 5.5 mm, elbow_z) axis (0,1,0) join targets HeadBolt

// ==== Pose, tip first (each turn about a pivot still in its zero-pose place)
move HeadToPivot bodies Shade, Carrier, Spreader, Led, HeadBolt to (-head_x, 0, -elbow_z)
move HeadTurn bodies Shade, Carrier, Spreader, Led, HeadBolt axis (0,1,0) angle (-head_angle)
move HeadBack bodies Shade, Carrier, Spreader, Led, HeadBolt to (head_x, 0, elbow_z)

move UpperToElbow bodies UpperArm, ElbowBolt, Shade, Carrier, Spreader, Led, HeadBolt to (-shoulder_x, 0, -elbow_z)
move UpperTurn bodies UpperArm, ElbowBolt, Shade, Carrier, Spreader, Led, HeadBolt axis (0,1,0) angle (-upper_angle)
move UpperBack bodies UpperArm, ElbowBolt, Shade, Carrier, Spreader, Led, HeadBolt to (shoulder_x, 0, elbow_z)

move LowerToShoulder bodies LowerL, LowerR, ShoulderBolt, UpperArm, ElbowBolt, Shade, Carrier, Spreader, Led, HeadBolt to (-shoulder_x, 0, -shoulder_z)
move LowerTurn bodies LowerL, LowerR, ShoulderBolt, UpperArm, ElbowBolt, Shade, Carrier, Spreader, Led, HeadBolt axis (0,1,0) angle lower_angle
move LowerBack bodies LowerL, LowerR, ShoulderBolt, UpperArm, ElbowBolt, Shade, Carrier, Spreader, Led, HeadBolt to (shoulder_x, 0, shoulder_z)

move Swivel bodies Turret, LowerL, LowerR, ShoulderBolt, UpperArm, ElbowBolt, Shade, Carrier, Spreader, Led, HeadBolt axis (0,0,1) angle base_yaw

// ==== Finish
color Base, Cover, Turret "#2a2b2e"
color LowerL, LowerR, UpperArm "#d9dcdf"
color Shade "#d6452f"
color Carrier "#f2f2f2"
color Spreader, Nipple, ShoulderBolt, ElbowBolt, HeadBolt "#9a9da2"
color Led "#fff4d6"
material Base, Cover, Turret, LowerL, LowerR, UpperArm, Shade, Carrier PLA
material Spreader Aluminum
material Nipple, ShoulderBolt, ElbowBolt, HeadBolt Steel
