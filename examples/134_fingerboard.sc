// 134. Fingerboard in the style of Tech Deck, printable at true size: a concave deck with kicked
// nose and tail, two trucks on M2 hardware, printed wheels and bushings.
//
// Bill of materials
//   8 x M2 x 6 pan head screws and 8 M2 nuts     trucks to the deck
//   2 x M2 x 8 button head screws                kingpins, self-tapping into the baseplates
//   2 x M2 x 30 button head screws, 2 M2 nyloc nuts   axles
//
// X runs along the board (nose at +X), Y across it, Z up; z = 0 is the underside of the deck on
// its centre line, where the baseplates sit.
//
// The deck is one concave cross-section carried along its side profile, so the concave runs into
// the kicks: flat for flat_l each side of the middle, round a bend_r bend and up kick_ang for
// kick_l. Its sides are the rails; a cut then rounds the nose and tail.
//
// Each truck: the baseplate is screwed under the deck. Its kingpin points down, toward the middle
// of the board as on a real truck: an M2 screw from below, through the lower bushing, the hanger's
// arm and the upper bushing, threads into the baseplate's boss. Tightening it squeezes the
// bushings, which sets how easily the board turns; print them in TPU (the engine has no TPU
// material, so they are listed as PLA). The hanger's beam carries the axle, an M2 x 30 screw that
// clamps in its bore; the wheels run free on it with wheel_play radial clearance.
//
// Print the deck on its side, a rail on the bed, so the layers run the length of the board and the
// kicks need no supports. Print the baseplates top down, the hangers beam down, the wheels and
// bushings flat.
design Fingerboard units mm

// Deck
param deck_l = 96 mm
param deck_w = 26 mm
param deck_t = 1.8 mm
param concave_r = 90 mm         // radius of the deck's top across its width
param flat_l = 34 mm            // from the middle to the start of each bend
param bend_r = 8 mm
param kick_ang = 20 deg
param kick_l = 15 mm

// Trucks
param kingpin_x = 24 mm         // from the middle of the board
param axle_off = 4.5 mm         // the axle outboard of the kingpin
param axle_z = -6.6 mm
param base_l = 15 mm
param base_w = 14 mm
param base_t = 1.6 mm
param hole_dx = 5 mm
param hole_dy = 5 mm
param hanger_w = 18 mm          // the beam, across the board

// Wheels
param wheel_d = 7.5 mm
param wheel_w = 4 mm
param wheel_play = 0.25 mm
param wheel_gap = 0.3 mm        // between wheel and hanger

// Hardware
param m2 = 2 mm
param m2_clear = 1.1 mm         // clearance hole radius
param m2_pilot = 0.8 mm         // self-tapping pilot radius

let hw = deck_w/2
let rb = concave_r + deck_t
let zb = rb - sqrt(rb^2 - hw^2)
let zt = rb - sqrt(concave_r^2 - hw^2)
let mid = deck_t/2
let ax = kingpin_x + axle_off
let pin_top = -base_t - 1.6 mm              // bottom of the kingpin boss
let bush_t = 1.15 mm
let arm_top = pin_top - bush_t - 0.1 mm
let arm_bot = arm_top - 2.95 mm
let head_z = arm_bot - 1.15 mm - 1.05 mm    // under the lower bushing
let wy = hanger_w/2 + wheel_gap

// ==== Deck: one concave section, extruded through the flat, revolved round each bend and
// extruded again for each kick
sketch FlatSec on YZ offset (-flat_l) {
  line l = (-hw, zb) (-hw, zt)
  arc top = start l.end through (0, deck_t) end (hw, zt)
  line r = top.end (hw, zb)
  arc bot = start r.end through (0, 0 mm) end l.start
  fix l top r bot
}
extrude DeckFlat from FlatSec distance (2*flat_l) new -> body Deck
sketch NoseSec on YZ offset (flat_l) {
  line l = (-hw, zb) (-hw, zt)
  arc top = start l.end through (0, deck_t) end (hw, zt)
  line r = top.end (hw, zb)
  arc bot = start r.end through (0, 0 mm) end l.start
  line hinge = (-5 mm, mid + bend_r) (5 mm, mid + bend_r)
  fix l top r bot hinge
}
revolve NoseBend from NoseSec axis NoseSec.hinge angle kick_ang join targets Deck
sketch TailSec on YZ offset (-flat_l) {
  line l = (-hw, zb) (-hw, zt)
  arc top = start l.end through (0, deck_t) end (hw, zt)
  line r = top.end (hw, zb)
  arc bot = start r.end through (0, 0 mm) end l.start
  line hinge = (-5 mm, mid + bend_r) (5 mm, mid + bend_r)
  fix l top r bot hinge
}
revolve TailBend from TailSec axis TailSec.hinge angle (-kick_ang) join targets Deck
// Each kick is drawn with its bend's hinge on the Y axis, tilted about it, then moved into place
sketch KickSec on YZ offset (0) {
  line l = (-hw, zb - (mid + bend_r)) (-hw, zt - (mid + bend_r))
  arc top = start l.end through (0, deck_t - (mid + bend_r)) end (hw, zt - (mid + bend_r))
  line r = top.end (hw, zb - (mid + bend_r))
  arc bot = start r.end through (0, 0 mm - (mid + bend_r)) end l.start
  fix l top r bot
}
extrude NoseKickEx from KickSec distance kick_l new -> body NoseKick
extrude TailKickEx from KickSec distance kick_l negative new -> body TailKick
move NoseKickTilt bodies NoseKick axis (0,1,0) angle (-kick_ang)
move NoseKickPlace bodies NoseKick to (flat_l, 0, mid + bend_r)
move TailKickTilt bodies TailKick axis (0,1,0) angle kick_ang
move TailKickPlace bodies TailKick to (-flat_l, 0, mid + bend_r)
combine Kicks target Deck tools NoseKick, TailKick join
// Round the nose and tail. Each arc starts just outside the rail, so it crosses the rail at an
// angle instead of meeting it tangentially (which the engine can't cut cleanly).
let end_x = deck_l/2 - deck_w/2 - 2 mm
sketch EndCutSk on XY offset (-5 mm) {
  polyline n = (end_x, deck_w/2 + 0.6 mm) (end_x, deck_w/2 + 5 mm) (deck_l/2 + 10 mm, deck_w/2 + 5 mm) (deck_l/2 + 10 mm, -deck_w/2 - 5 mm) (end_x, -deck_w/2 - 5 mm) (end_x, -deck_w/2 - 0.6 mm)
  arc na = start n.end through (deck_l/2, 0) end n.start
  polyline t = (-end_x, -deck_w/2 - 0.6 mm) (-end_x, -deck_w/2 - 5 mm) (-deck_l/2 - 10 mm, -deck_w/2 - 5 mm) (-deck_l/2 - 10 mm, deck_w/2 + 5 mm) (-end_x, deck_w/2 + 5 mm) (-end_x, deck_w/2 + 0.6 mm)
  arc ta = start t.end through (-deck_l/2, 0) end t.start
  fix n na t ta
}
extrude EndCut from EndCutSk distance 20 mm cut targets Deck
sketch DeckHoleSk on XY offset (-1 mm) {
  circle a = (kingpin_x - hole_dx, -hole_dy) radius m2_clear
  circle b = (kingpin_x - hole_dx, hole_dy) radius m2_clear
  circle c = (kingpin_x + hole_dx, -hole_dy) radius m2_clear
  circle d = (kingpin_x + hole_dx, hole_dy) radius m2_clear
  circle e = (-kingpin_x - hole_dx, -hole_dy) radius m2_clear
  circle f = (-kingpin_x - hole_dx, hole_dy) radius m2_clear
  circle g = (-kingpin_x + hole_dx, -hole_dy) radius m2_clear
  circle h = (-kingpin_x + hole_dx, hole_dy) radius m2_clear
  fix a b c d e f g h
}
extrude DeckHoles from DeckHoleSk distance 6 mm cut targets Deck

// ==== Front truck (the rear one is its mirror)
// Baseplate: a plate under the deck with four screw holes and a boss for the kingpin
sketch BaseSk on XY offset (-base_t) {
  rect rounded p = (kingpin_x - base_l/2, -base_w/2) (kingpin_x + base_l/2, base_w/2) radius 2 mm
  fix p
}
extrude BasePlate from BaseSk distance base_t new -> body Baseplate
cylinder PinBoss radius 3 mm height 1.7 mm base (kingpin_x, 0, pin_top) axis Z join targets Baseplate
sketch BaseHoleSk on XY offset (-base_t - 0.5 mm) {
  circle a = (kingpin_x - hole_dx, -hole_dy) radius m2_clear
  circle b = (kingpin_x - hole_dx, hole_dy) radius m2_clear
  circle c = (kingpin_x + hole_dx, -hole_dy) radius m2_clear
  circle d = (kingpin_x + hole_dx, hole_dy) radius m2_clear
  fix a b c d
}
extrude BaseHoles from BaseHoleSk distance (base_t + 1 mm) cut targets Baseplate
cylinder PinPilot radius m2_pilot height (-pin_top - 0.4 mm + 0.1 mm) base (kingpin_x, 0, pin_top - 0.1 mm) axis Z cut targets Baseplate

// Hanger: an arm round the kingpin and a beam across the board for the axle
box HangerArm size (ax - 2 mm - kingpin_x + 2.4 mm + 0.1 mm) 6 mm (arm_top - arm_bot) corner (kingpin_x - 2.4 mm, -3 mm, arm_bot) new -> body Hanger
box HangerBeam size 4 mm hanger_w 4 mm corner (ax - 2 mm, -hanger_w/2, axle_z - 2 mm) join targets Hanger
fillet HangerEnds edges (ax, hanger_w/2, axle_z + 2 mm) (ax, -hanger_w/2, axle_z + 2 mm) radius 1 mm body Hanger
cylinder PinBore radius (m2/2 + 0.4 mm) height 8 mm base (kingpin_x, 0, arm_bot - 1 mm) axis Z cut targets Hanger
cylinder AxleBore radius (m2/2 + 0.1 mm) height (hanger_w + 2 mm) base (ax, -hanger_w/2 - 1 mm, axle_z) axis (0,1,0) cut targets Hanger

// Bushings, above and below the arm
cylinder BushTopRing radius 2.3 mm height bush_t base (kingpin_x, 0, arm_top + 0.05 mm) axis Z new -> body BushTop
cylinder BushTopBore radius (m2/2 + 0.15 mm) height (bush_t + 1 mm) base (kingpin_x, 0, arm_top - 0.5 mm) axis Z cut targets BushTop
cylinder BushBotRing radius 2.3 mm height 1.05 mm base (kingpin_x, 0, arm_bot - 1.05 mm - 0.05 mm) axis Z new -> body BushBot
cylinder BushBotBore radius (m2/2 + 0.15 mm) height 2 mm base (kingpin_x, 0, arm_bot - 1.6 mm) axis Z cut targets BushBot

// Wheels
cylinder WheelLBody radius (wheel_d/2) height wheel_w base (ax, -wy - wheel_w, axle_z) axis (0,1,0) new -> body WheelL
cylinder WheelRBody radius (wheel_d/2) height wheel_w base (ax, wy, axle_z) axis (0,1,0) new -> body WheelR
fillet WheelLEdges edges (ax, -wy - wheel_w, axle_z + wheel_d/2) (ax, -wy, axle_z + wheel_d/2) radius 1 mm body WheelL
fillet WheelREdges edges (ax, wy, axle_z + wheel_d/2) (ax, wy + wheel_w, axle_z + wheel_d/2) radius 1 mm body WheelR
cylinder WheelLBore radius (m2/2 + wheel_play) height (wheel_w + 1 mm) base (ax, -wy - wheel_w - 0.5 mm, axle_z) axis (0,1,0) cut targets WheelL
cylinder WheelRBore radius (m2/2 + wheel_play) height (wheel_w + 1 mm) base (ax, wy - 0.5 mm, axle_z) axis (0,1,0) cut targets WheelR

// The bought parts, placed (not printed): kingpin and axle screws
cylinder KingpinShank radius (m2/2) height 8 mm base (kingpin_x, 0, head_z + 1.05 mm) axis Z new -> body Kingpin
cylinder KingpinHead radius 1.75 mm height 1.05 mm base (kingpin_x, 0, head_z) axis Z join targets Kingpin
cylinder AxleShank radius (m2/2) height 30 mm base (ax, -wy - wheel_w - 0.3 mm, axle_z) axis (0,1,0) new -> body Axle
cylinder AxleHead radius 1.75 mm height 1.05 mm base (ax, -wy - wheel_w - 0.3 mm - 1.05 mm, axle_z) axis (0,1,0) join targets Axle

mirror RearTruck bodies Baseplate, Hanger, BushTop, BushBot, WheelL, WheelR, Kingpin, Axle plane YZ

// ==== Fits
check Baseplate clearance Deck >= 0 mm
check Hanger clearance Baseplate >= 0.5 mm
check Hanger clearance Kingpin >= 0.3 mm
check Hanger clearance BushTop >= 0.03 mm
check Hanger clearance BushBot >= 0.03 mm
check BushTop clearance Kingpin >= 0.1 mm
// The lower bushing rests on the kingpin's head, 0.05 mm above it; its bore has 0.15 mm
check BushBot clearance Kingpin >= 0.04 mm
check WheelL clearance Hanger >= (wheel_gap - 0.01 mm)
check WheelR clearance Hanger >= (wheel_gap - 0.01 mm)
check WheelL clearance Axle >= (wheel_play - 0.01 mm)
check WheelR clearance Axle >= (wheel_play - 0.01 mm)
check WheelL clearance Deck >= 1 mm
check WheelR clearance Deck >= 1 mm
check Kingpin clearance Hanger >= 0.3 mm
check Deck size.x >= (deck_l - 0.1 mm)
check Deck size.x <= (deck_l + 0.1 mm)

// ==== Finish
color Deck "#d9b77e"
color Baseplate, Hanger "#c4ccd6"
color BushTop, BushBot "#e84a3c"
color WheelL, WheelR "#f4f1e8"
color Kingpin, Axle "#606870"
material Deck, Baseplate, Hanger, BushTop, BushBot, WheelL, WheelR PLA
