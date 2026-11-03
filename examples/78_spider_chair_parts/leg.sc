// One leg of 78_spider_chair: arm, strut and leg in one vertical plane, on two ringed hubs.
//
// Built in the XZ plane (sketch v is -z there): u = x runs out from the centre node at x = 0, z is
// up. The arm runs from the node to the hub, the strut rises from the hub to the knee and the leg
// falls from the knee to the floor, its foot cut level. The arm and leg share the middle layer
// (y in [-bar_t/2, bar_t/2]) and the strut lies on its +y face, so each hub clamps two bars.
design SpiderLeg units mm

param frame_z = 368 mm
param arm_l = 300 mm
param knee_out = 110 mm
param knee_up = 230 mm
param foot_out = 230 mm

param bar_w = 36 mm
param bar_t = 14 mm
param rib = 1.5 mm
param hub_r = 26 mm
param screw_gap = 20 mm

let knee_u = arm_l + knee_out
let knee_z = frame_z + knee_up
let foot_u = knee_u + foot_out
let core_w = bar_w - 2*rib
let y_in = -bar_t/2
let y_out = 3*bar_t/2 + 1 mm
let hub_len = 2*bar_t + 2 mm

// Bars: a core plus four 2 mm plates rib proud on each edge
sketch ArmCoreSk on XZ offset y_in {
  slot s = centers (0, -frame_z) (arm_l, -frame_z) width core_w
  fix s
}
sketch ArmRibSk on XZ offset y_in {
  slot s = centers (0, -frame_z) (arm_l, -frame_z) width bar_w
  fix s
}
extrude ArmCore from ArmCoreSk distance bar_t new -> body Arm
for k from 0 to 3 {
  extrude ArmRib{k} from ArmRibSk distance 2 mm offset (4*k mm) join targets Arm
}

sketch StrutCoreSk on XZ offset (bar_t/2) {
  slot s = centers (arm_l, -frame_z) (knee_u, -knee_z) width core_w
  fix s
}
sketch StrutRibSk on XZ offset (bar_t/2) {
  slot s = centers (arm_l, -frame_z) (knee_u, -knee_z) width bar_w
  fix s
}
extrude StrutCore from StrutCoreSk distance bar_t new -> body Strut
for k from 0 to 3 {
  extrude StrutRib{k} from StrutRibSk distance 2 mm offset (4*k mm) join targets Strut
}

sketch LegCoreSk on XZ offset y_in {
  slot s = centers (knee_u, -knee_z) (foot_u, 0) width core_w
  fix s
}
sketch LegRibSk on XZ offset y_in {
  slot s = centers (knee_u, -knee_z) (foot_u, 0) width bar_w
  fix s
}
extrude LegCore from LegCoreSk distance bar_t new -> body Leg
for k from 0 to 3 {
  extrude LegRib{k} from LegRibSk distance 2 mm offset (4*k mm) join targets Leg
}
box FootCut size (4*bar_w) (4*bar_t) bar_w corner (foot_u - 2*bar_w, -2*bar_t, -bar_w) cut targets Leg

// Hubs: a core with eight proud rings through both layers, two socket screws on the outer face
cylinder HubCore radius (hub_r - rib) height hub_len base (arm_l, y_in - 1 mm, frame_z) axis (0,1,0) new -> body Hub
cylinder KneeCore radius (hub_r - rib) height hub_len base (knee_u, y_in - 1 mm, knee_z) axis (0,1,0) new -> body Knee
for k from 0 to 7 {
  cylinder HubRing{k} radius hub_r height 2 mm base (arm_l, y_in - 1 mm + 4*k mm, frame_z) axis (0,1,0) join targets Hub
  cylinder KneeRing{k} radius hub_r height 2 mm base (knee_u, y_in - 1 mm + 4*k mm, knee_z) axis (0,1,0) join targets Knee
}
for k from 0 to 1 {
  cylinder HubBore{k} radius 5.5 mm height 1.2 mm base (arm_l + (k - 0.5)*screw_gap, y_out - 1.2 mm, frame_z) axis (0,1,0) cut targets Hub
  cylinder HubHead{k} radius 4.6 mm height 1.2 mm base (arm_l + (k - 0.5)*screw_gap, y_out - 1.2 mm, frame_z) axis (0,1,0) join targets Hub
  cylinder KneeBore{k} radius 5.5 mm height 1.2 mm base (knee_u + (k - 0.5)*screw_gap, y_out - 1.2 mm, knee_z) axis (0,1,0) cut targets Knee
  cylinder KneeHead{k} radius 4.6 mm height 1.2 mm base (knee_u + (k - 0.5)*screw_gap, y_out - 1.2 mm, knee_z) axis (0,1,0) join targets Knee
}
sketch HubSocketSk on XZ offset y_out {
  polygon a = (arm_l - screw_gap/2, -frame_z) radius 2.5 mm sides 6
  polygon b = (arm_l + screw_gap/2, -frame_z) radius 2.5 mm sides 6
  fix a b
}
extrude HubSockets from HubSocketSk distance 3 mm negative cut targets Hub
sketch KneeSocketSk on XZ offset y_out {
  polygon a = (knee_u - screw_gap/2, -knee_z) radius 2.5 mm sides 6
  polygon b = (knee_u + screw_gap/2, -knee_z) radius 2.5 mm sides 6
  fix a b
}
extrude KneeSockets from KneeSocketSk distance 3 mm negative cut targets Knee

color Arm, Strut, Leg, Hub, Knee "#b8bbbe"
material Arm, Strut, Leg, Hub, Knee Steel
