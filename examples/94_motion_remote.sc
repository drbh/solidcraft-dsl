// 94. Motion remote and nunchuk, the 2006 one-hand TV-remote game controller, printable and working.
//
// The remote lies face up: X across its width (0 to W), Y along its length (y = 0 is the bottom end
// with the ports and strap, y = L the front end with the IR window), Z up (z = 0 its underside,
// z = T its face). The nunchuk lies beside it, its axis along Y at x = nun_x, tail toward -Y.
//
// Bill of materials
//   1  Seeed XIAO nRF52840 Sense: BLE, 6-axis IMU and LiPo charging on one 21 x 17.5 mm board;
//      its USB-C is the charge and program port in the bottom end
//   1  602040 LiPo, 400 mAh, 6 x 20 x 40 mm
//   12 6 x 6 x 5 mm tactile switches: D-pad (4), A, -, Home, +, 1, 2, Power on the face board,
//      B on the board's underside; 2 more in the nunchuk for C and Z
//   1  perfboard 23 x 138 mm (the face board), 1 perfboard 24 x 20 mm (nunchuk buttons)
//   4  3 mm blue LEDs (player lights), 1 5 mm 940 nm IR LED + NPN transistor, 1 12 mm buzzer
//   1  JST-XH 6-pin right-angle socket on the face board's end, and a 6-pin JST-XH plug on the
//      nunchuk's 6-core cable (3V3, GND, X, Y, C, Z)
//   1  PSP-style 2-axis thumbstick on a 20 x 20 mm breakout (nunchuk)
//   6  M2 x 16 self-tapping screws (remote), 4 M2 x 6 (face board), 1 M2 x 12 + 1 M2 x 16 (nunchuk)
//
// How it goes together
//   Bottom     the lower shell: the XIAO in a cradle at the bottom end with its USB-C in the end
//              wall, the LiPo in a rib pocket, the strap's anchor post, the B plunger's guide
//   Top        the upper shell: the face board hangs from four standoffs under the buttons, the
//              LEDs show through four holes, the IR LED looks out of the front window, the JST
//              socket's mouth is in the bottom end. Six M2 screws from underneath close it.
//   DPad, A, Minus, Home, Plus, One, Two, Power   caps with flanges inside, on their switches;
//              the D-pad's cross rocks on a centre post over four switches
//   B          a trigger under the front: a pad, a flange inside and a plunger up to its switch
//   IrWindow   a translucent plate pressed into the front window (print it in natural PLA)
//   Nunchuk, NunchukL   the nunchuk's two halves, split down its axis and joined by two M2
//              screws; the thumbstick sits under the flat, C and Z on a small board behind the front
//   C, Z       the nunchuk's caps
//   Xiao, Battery, FaceBoard, Stick, Cable   the bought parts, for checking fit (not printed)
//
// Printing (PLA, 0.2 mm layers, no supports): Top face down, Bottom underside down, the nunchuk
// halves on their split faces, the caps flange down, B on its pad. The bed-side edges are chamfered.
design MotionRemote units mm

// Remote
param W = 36 mm
param L = 148 mm
param T = 30 mm
param corner_r = 9 mm
param edge_c = 4 mm            // chamfer round the face and the underside
param wall = 2 mm
param split_z = 14 mm
param fit = 0.25 mm
param key_out = 1.5 mm         // how far the caps stand proud of the face
param pilot_d = 1.6 mm         // M2 self-tapping
param clear_d = 2.4 mm
param head_d = 4.2 mm
param boss_r = 2.6 mm

// Nunchuk
param nun_x = 85 mm            // its axis, beside the remote
param nun_y = 25 mm            // its tail
param nun_l = 112 mm
param nun_r = 20 mm            // the head's radius
param nun_wall = 2 mm
param cable_d = 4.5 mm

let cx = W/2
let in_face = T - wall
let board_top = in_face - 1 mm - 5 mm     // switch face of the face board (flange 1 mm, 5 mm switch)
let board_bot = board_top - 1.6 mm
let nun_z = nun_r + 1 mm                  // the nunchuk's axis height, its belly just off the floor

// ==== Remote shells
sketch OutlineSk on XY {
  rect rounded o = (0,0) (W,L) radius corner_r
  fix o
}
extrude BottomBlank from OutlineSk distance split_z new -> body Bottom
chamfer BottomEdge edges (cx, 0, 0) distance edge_c body Bottom
shell BottomCavity faces (cx, L/2, split_z) thickness wall inside body Bottom
extrude TopBlank from OutlineSk distance (T - split_z) offset split_z new -> body Top
chamfer TopEdge edges (cx, 0, T) distance edge_c body Top
shell TopCavity faces (cx, L/2, split_z) thickness wall inside body Top

// Six screws up through the bottom into the top
sketch BossSk on XY {
  circle a = (3.8 mm, 32 mm) radius boss_r
  circle b = (W - 3.8 mm, 32 mm) radius boss_r
  circle c = (3.8 mm, 88 mm) radius boss_r
  circle d = (W - 3.8 mm, 88 mm) radius boss_r
  circle e = (3.8 mm, 140 mm) radius boss_r
  circle f = (W - 3.8 mm, 140 mm) radius boss_r
  fix a b c d e f
}
extrude BottomBosses from BossSk distance (split_z - wall + 0.1 mm) offset (wall - 0.2 mm) join targets Bottom
extrude TopBosses from BossSk distance (in_face - split_z + 0.1 mm) offset (split_z + 0.1 mm) join targets Top
sketch ClearSk on XY offset (-1 mm) {
  circle a = (3.8 mm, 32 mm) diameter clear_d
  circle b = (W - 3.8 mm, 32 mm) diameter clear_d
  circle c = (3.8 mm, 88 mm) diameter clear_d
  circle d = (W - 3.8 mm, 88 mm) diameter clear_d
  circle e = (3.8 mm, 140 mm) diameter clear_d
  circle f = (W - 3.8 mm, 140 mm) diameter clear_d
  fix a b c d e f
}
extrude Clear from ClearSk distance (split_z + 2 mm) cut targets Bottom
sketch HeadSk on XY offset (-1 mm) {
  circle a = (3.8 mm, 32 mm) diameter head_d
  circle b = (W - 3.8 mm, 32 mm) diameter head_d
  circle c = (3.8 mm, 88 mm) diameter head_d
  circle d = (W - 3.8 mm, 88 mm) diameter head_d
  circle e = (3.8 mm, 140 mm) diameter head_d
  circle f = (W - 3.8 mm, 140 mm) diameter head_d
  fix a b c d e f
}
extrude Heads from HeadSk distance 3.6 mm cut targets Bottom
sketch PilotSk on XY offset (split_z - 0.1 mm) {
  circle a = (3.8 mm, 32 mm) diameter pilot_d
  circle b = (W - 3.8 mm, 32 mm) diameter pilot_d
  circle c = (3.8 mm, 88 mm) diameter pilot_d
  circle d = (W - 3.8 mm, 88 mm) diameter pilot_d
  circle e = (3.8 mm, 140 mm) diameter pilot_d
  circle f = (W - 3.8 mm, 140 mm) diameter pilot_d
  fix a b c d e f
}
extrude Pilots from PilotSk distance 7 mm cut targets Top

// ==== Bottom: USB-C port, XIAO cradle, LiPo pocket, strap hole and anchor, B opening and guide
box UsbC size 10 mm (wall + 2 mm) 4.2 mm corner (cx - 5 mm, -1 mm, wall + 1 mm + 1.2 mm + 1.6 mm - 2.1 mm) cut targets Bottom
sketch CradleSk on XY offset (wall - 0.2 mm) {
  rect l = (cx - 8.75 mm - fit - 1.2 mm, 5 mm) (cx - 8.75 mm - fit, 22 mm)
  rect r = (cx + 8.75 mm + fit, 5 mm) (cx + 8.75 mm + fit + 1.2 mm, 22 mm)
  rect e = (cx - 6 mm, wall + 0.4 mm + 21 mm + fit) (cx + 6 mm, wall + 0.4 mm + 21 mm + fit + 1.2 mm)
  fix l r e
}
extrude Cradle from CradleSk distance 5.2 mm join targets Bottom
sketch PadSk on XY offset (wall - 0.2 mm) {
  rect p1 = (cx - 7 mm, 6 mm) (cx - 4 mm, 20 mm)
  rect p2 = (cx + 4 mm, 6 mm) (cx + 7 mm, 20 mm)
  fix p1 p2
}
extrude Pads from PadSk distance 1.2 mm join targets Bottom
sketch BatSk on XY offset (wall - 0.2 mm) {
  rect l = (cx - 10 mm - fit - 1.2 mm, 32 mm) (cx - 10 mm - fit, 68 mm)
  rect r = (cx + 10 mm + fit, 32 mm) (cx + 10 mm + fit + 1.2 mm, 68 mm)
  rect t = (cx - 7 mm, 70 mm + fit) (cx + 7 mm, 70 mm + fit + 1.2 mm)
  rect b = (cx - 7 mm, 30 mm - fit - 1.2 mm) (cx + 7 mm, 30 mm - fit)
  fix l r t b
}
extrude BatRibs from BatSk distance 4 mm join targets Bottom
cylinder StrapHole diameter 3.6 mm height (wall + 3 mm) base (5.5 mm, -1 mm, 8 mm) axis Y cut targets Bottom
cylinder StrapPost diameter 3.2 mm height (split_z - wall - 0.1 mm) base (5.5 mm, 8 mm, wall - 0.2 mm) axis Z join targets Bottom
sketch BOpenSk on XY offset (-1 mm) {
  rect rounded b = (cx - 8 mm - fit, 101 mm - fit) (cx + 8 mm + fit, 125 mm + fit) radius (5 mm + fit)
  fix b
}
extrude BOpen from BOpenSk distance (wall + 2 mm) cut targets Bottom

// ==== Top: cap holes, LED holes, IR window, JST port, face board standoffs
sketch CapHoleSk on XY offset (in_face - 1 mm) {
  polyline closed c = (cx - 3.5 mm - fit, 109 mm - fit) (cx + 3.5 mm + fit, 109 mm - fit) (cx + 3.5 mm + fit, 116.5 mm - fit) (cx + 11 mm + fit, 116.5 mm - fit) (cx + 11 mm + fit, 123.5 mm + fit) (cx + 3.5 mm + fit, 123.5 mm + fit) (cx + 3.5 mm + fit, 131 mm + fit) (cx - 3.5 mm - fit, 131 mm + fit) (cx - 3.5 mm - fit, 123.5 mm + fit) (cx - 11 mm - fit, 123.5 mm + fit) (cx - 11 mm - fit, 116.5 mm - fit) (cx - 3.5 mm - fit, 116.5 mm - fit)
  circle a = (cx, 95 mm) radius (7 mm + fit)
  circle minus = (cx - 8.5 mm, 73 mm) radius (3 mm + fit)
  circle home = (cx, 73 mm) radius (3.6 mm + fit)
  circle plus = (cx + 8.5 mm, 73 mm) radius (3 mm + fit)
  circle one = (cx, 42 mm) radius (4.5 mm + fit)
  circle two = (cx, 28 mm) radius (4.5 mm + fit)
  circle power = (10 mm, 138 mm) radius (2.6 mm + fit)
  circle led1 = (cx - 11.25 mm, 9 mm) diameter 3.2 mm
  circle led2 = (cx - 3.75 mm, 9 mm) diameter 3.2 mm
  circle led3 = (cx + 3.75 mm, 9 mm) diameter 3.2 mm
  circle led4 = (cx + 11.25 mm, 9 mm) diameter 3.2 mm
  fix c a minus home plus one two power led1 led2 led3 led4
}
extrude CapHoles from CapHoleSk distance (wall + 2 mm) cut targets Top
cylinder DPadWell radius 13 mm height 1 mm base (cx, 120 mm, T - 0.6 mm) axis Z cut targets Top
sketch SpeakerSk on XY offset (in_face - 1 mm) {
  circle h = (cx - 8 mm, 56.4 mm) diameter 1.6 mm
  fix h
  pattern rectangular h count 6 spacing 3.2 mm count2 2 spacing2 3.2 mm dir (1,0) dir2 (0,1)
}
extrude Speaker from SpeakerSk distance (wall + 2 mm) cut targets Top
box IrOpening size 24 mm (wall + 2 mm) 9.4 mm corner (cx - 12 mm, L - wall - 1 mm, 14.6 mm) cut targets Top
box JstPort size 18.4 mm (wall + 2 mm) 6.4 mm corner (cx - 9.2 mm, -1 mm, split_z + 0.2 mm) cut targets Top
sketch StandoffSk on XY offset board_top {
  circle a = (8.5 mm, 50 mm) radius 2.6 mm
  circle b = (W - 8.5 mm, 50 mm) radius 2.6 mm
  circle c = (8.5 mm, 106 mm) radius 2.6 mm
  circle d = (W - 8.5 mm, 106 mm) radius 2.6 mm
  fix a b c d
}
extrude Standoffs from StandoffSk distance (in_face - board_top + 0.2 mm) join targets Top
sketch StandoffPilotSk on XY offset (board_top - 0.1 mm) {
  circle a = (8.5 mm, 50 mm) diameter pilot_d
  circle b = (W - 8.5 mm, 50 mm) diameter pilot_d
  circle c = (8.5 mm, 106 mm) diameter pilot_d
  circle d = (W - 8.5 mm, 106 mm) diameter pilot_d
  fix a b c d
}
extrude StandoffPilots from StandoffPilotSk distance 5 mm cut targets Top

// ==== Caps: stem through the face, flange inside on the switch
sketch DPadSk on XY offset (in_face - 0.2 mm) {
  polyline closed c = (cx - 3.5 mm, 109 mm) (cx + 3.5 mm, 109 mm) (cx + 3.5 mm, 116.5 mm) (cx + 11 mm, 116.5 mm) (cx + 11 mm, 123.5 mm) (cx + 3.5 mm, 123.5 mm) (cx + 3.5 mm, 131 mm) (cx - 3.5 mm, 131 mm) (cx - 3.5 mm, 123.5 mm) (cx - 11 mm, 123.5 mm) (cx - 11 mm, 116.5 mm) (cx - 3.5 mm, 116.5 mm)
  fix c
}
extrude DPadKey from DPadSk distance (wall - 0.4 mm + key_out + 0.2 mm) new -> body DPad
cylinder DPadFlange radius 12.6 mm height 1 mm base (cx, 120 mm, in_face - 1 mm) axis Z join targets DPad
cylinder DPadPivot radius 1.5 mm height (in_face - 1 mm - board_top + 0.2 mm) base (cx, 120 mm, board_top) axis Z join targets DPad

cylinder AStem radius 7 mm height (wall + key_out + 0.2 mm) base (cx, 95 mm, in_face - 0.2 mm) axis Z new -> body A
cylinder AFlange radius 8.6 mm height 1 mm base (cx, 95 mm, in_face - 1 mm) axis Z join targets A
fillet ATop edges (cx + 7 mm, 95 mm, T + key_out) radius 1.2 mm body A
cylinder MinusStem radius 3 mm height (wall + key_out) base (cx - 8.5 mm, 73 mm, in_face - 0.2 mm) axis Z new -> body Minus
cylinder MinusFlange radius 4.4 mm height 1 mm base (cx - 8.5 mm, 73 mm, in_face - 1 mm) axis Z join targets Minus
cylinder HomeStem radius 3.6 mm height (wall + key_out) base (cx, 73 mm, in_face - 0.2 mm) axis Z new -> body Home
cylinder HomeFlange radius 5 mm height 1 mm base (cx, 73 mm, in_face - 1 mm) axis Z join targets Home
cylinder PlusStem radius 3 mm height (wall + key_out) base (cx + 8.5 mm, 73 mm, in_face - 0.2 mm) axis Z new -> body Plus
cylinder PlusFlange radius 4.4 mm height 1 mm base (cx + 8.5 mm, 73 mm, in_face - 1 mm) axis Z join targets Plus
cylinder OneStem radius 4.5 mm height (wall + key_out) base (cx, 42 mm, in_face - 0.2 mm) axis Z new -> body One
cylinder OneFlange radius 6 mm height 1 mm base (cx, 42 mm, in_face - 1 mm) axis Z join targets One
cylinder TwoStem radius 4.5 mm height (wall + key_out) base (cx, 28 mm, in_face - 0.2 mm) axis Z new -> body Two
cylinder TwoFlange radius 6 mm height 1 mm base (cx, 28 mm, in_face - 1 mm) axis Z join targets Two
cylinder PowerStem radius 2.6 mm height (wall + key_out) base (10 mm, 138 mm, in_face - 0.2 mm) axis Z new -> body Power
cylinder PowerFlange radius 3.9 mm height 1 mm base (10 mm, 138 mm, in_face - 1 mm) axis Z join targets Power

// B: the pad stands 2 mm under the underside, its flange inside, a plunger up to the switch
sketch BPadSk on XY offset (-2 mm) {
  rect rounded b = (cx - 8 mm, 101 mm) (cx + 8 mm, 125 mm) radius 5 mm
  fix b
}
extrude BPad from BPadSk distance (wall + 2 mm) new -> body B
sketch BFlangeSk on XY offset wall {
  rect rounded b = (cx - 9.5 mm, 99.5 mm) (cx + 9.5 mm, 126.5 mm) radius 6.5 mm
  fix b
}
extrude BFlange from BFlangeSk distance 1 mm join targets B
cylinder BPlunger radius 2 mm height (board_bot - 5 mm - wall - 0.8 mm) base (cx, 113 mm, wall + 0.8 mm) axis Z join targets B

// IR window insert: a plug for the opening and a flange inside
box IrPlug size (24 mm - 2*fit) wall (9.4 mm - 2*fit) corner (cx - 12 mm + fit, L - wall, 14.6 mm + fit) new -> body IrWindow
box IrFlange size 27 mm 1 mm 12 mm corner (cx - 13.5 mm, L - wall - 1 mm, 13.3 mm) join targets IrWindow

// ==== Nunchuk: a profile revolved about Y at the origin, flattened, moved beside the remote,
// split down its axis and shelled; then its holes and screws
sketch NunSk on XY {
  line a = (0, 0) (8 mm, 0)
  arc b = start a.end through (15 mm, 0.35*nun_l) end (nun_r - 1 mm, 0.7*nun_l)
  arc c = start b.end through (0.75*nun_r, 0.92*nun_l) end (0, nun_l)
  line d = c.end a.start
  fix a b c d
}
revolve NunBody from NunSk axis Y -> body Nunchuk
box StickFlat size (3*nun_r) (0.45*nun_l) nun_r corner (-1.5*nun_r, 0.58*nun_l, 0.6*nun_r) cut targets Nunchuk
box FrontFlat size (3*nun_r) 10 mm (3*nun_r) corner (-1.5*nun_r, nun_l - 6 mm, -1.5*nun_r) cut targets Nunchuk
// Hollow it with a smaller revolve (the engine can't offset the nose's spindle-torus face), kept
// nun_wall under the flat and behind the front
sketch NunInSk on XY {
  line a = (0, nun_wall) (8 mm - nun_wall, nun_wall)
  arc b = start a.end through (15 mm - nun_wall, 0.35*nun_l) end (nun_r - 1 mm - nun_wall, 0.7*nun_l)
  arc c = start b.end through (0.75*nun_r - nun_wall, 0.92*nun_l - nun_wall) end (0, nun_l - nun_wall)
  line d = c.end a.start
  fix a b c d
}
revolve NunInside from NunInSk axis Y -> body NunCavity
box CavityUnderFlat size (3*nun_r) (0.45*nun_l + nun_wall) nun_r corner (-1.5*nun_r, 0.58*nun_l - nun_wall, 0.6*nun_r - nun_wall) cut targets NunCavity
box CavityBehindFront size (3*nun_r) 12 mm (3*nun_r) corner (-1.5*nun_r, nun_l - 6 mm - nun_wall, -1.5*nun_r) cut targets NunCavity
combine Hollow target Nunchuk tools NunCavity cut
move PlaceNunchuk bodies Nunchuk to (nun_x, nun_y, nun_z)
split NunSplit target Nunchuk origin (nun_x, 0, 0) normal (1, 0, 0) -> body NunchukL

// Thumbstick hole in the flat, C and Z in the front, the cable at the tail (each cuts both halves)
cylinder StickHoleR radius 9.5 mm height 8 mm base (nun_x, nun_y + 0.8*nun_l, nun_z + 0.6*nun_r - 4 mm) axis Z cut targets Nunchuk
cylinder StickHoleL radius 9.5 mm height 8 mm base (nun_x, nun_y + 0.8*nun_l, nun_z + 0.6*nun_r - 4 mm) axis Z cut targets NunchukL
cylinder CHoleR radius (3.5 mm + fit) height 8 mm base (nun_x, nun_y + nun_l - 10 mm, nun_z + 5 mm) axis Y cut targets Nunchuk
cylinder CHoleL radius (3.5 mm + fit) height 8 mm base (nun_x, nun_y + nun_l - 10 mm, nun_z + 5 mm) axis Y cut targets NunchukL
box ZHoleR size (14 mm + 2*fit) 8 mm (6 mm + 2*fit) corner (nun_x - 7 mm - fit, nun_y + nun_l - 10 mm, nun_z - 7 mm - fit) cut targets Nunchuk
box ZHoleL size (14 mm + 2*fit) 8 mm (6 mm + 2*fit) corner (nun_x - 7 mm - fit, nun_y + nun_l - 10 mm, nun_z - 7 mm - fit) cut targets NunchukL
cylinder CableHoleR diameter (cable_d + 0.5 mm) height 10 mm base (nun_x, nun_y - 1 mm, nun_z) axis Y cut targets Nunchuk
cylinder CableHoleL diameter (cable_d + 0.5 mm) height 10 mm base (nun_x, nun_y - 1 mm, nun_z) axis Y cut targets NunchukL

// Two M2 screws across the halves: pilot bosses in the right half, clearance and heads in the left
cylinder NunBossR1 radius 2.75 mm height 10.5 mm base (nun_x + 0.1 mm, nun_y + 18 mm, nun_z) axis X join targets Nunchuk
cylinder NunBossL1 radius 2.75 mm height 10.5 mm base (nun_x - 10.6 mm, nun_y + 18 mm, nun_z) axis X join targets NunchukL
cylinder NunBossR2 radius 2.75 mm height 15 mm base (nun_x + 0.1 mm, nun_y + 62 mm, nun_z - 6 mm) axis X join targets Nunchuk
cylinder NunBossL2 radius 2.75 mm height 15 mm base (nun_x - 15.1 mm, nun_y + 62 mm, nun_z - 6 mm) axis X join targets NunchukL
cylinder NunPilot1 diameter pilot_d height 7 mm base (nun_x - 0.1 mm, nun_y + 18 mm, nun_z) axis X cut targets Nunchuk
cylinder NunPilot2 diameter pilot_d height 9 mm base (nun_x - 0.1 mm, nun_y + 62 mm, nun_z - 6 mm) axis X cut targets Nunchuk
cylinder NunClear1 diameter clear_d height 20 mm base (nun_x - 19.9 mm, nun_y + 18 mm, nun_z) axis X cut targets NunchukL
cylinder NunClear2 diameter clear_d height 30 mm base (nun_x - 29.9 mm, nun_y + 62 mm, nun_z - 6 mm) axis X cut targets NunchukL
cylinder NunHead1 diameter head_d height 15 mm base (nun_x - 21 mm, nun_y + 18 mm, nun_z) axis X cut targets NunchukL
cylinder NunHead2 diameter head_d height 23 mm base (nun_x - 31 mm, nun_y + 62 mm, nun_z - 6 mm) axis X cut targets NunchukL

// Nunchuk caps: C round, Z wide, each with a flange inside the front
cylinder CStem radius 3.5 mm height (nun_wall + 2.2 mm) base (nun_x, nun_y + nun_l - 6 mm - nun_wall - 0.2 mm, nun_z + 5 mm) axis Y new -> body C
cylinder CFlange radius 4.8 mm height 1 mm base (nun_x, nun_y + nun_l - 6 mm - nun_wall - 1 mm, nun_z + 5 mm) axis Y join targets C
box ZStem size 14 mm (nun_wall + 2.2 mm) 6 mm corner (nun_x - 7 mm, nun_y + nun_l - 6 mm - nun_wall - 0.2 mm, nun_z - 7 mm) new -> body Z
box ZFlange size 15 mm 1 mm 7.5 mm corner (nun_x - 7.5 mm, nun_y + nun_l - 6 mm - nun_wall - 1 mm, nun_z - 7.75 mm) join targets Z

// ==== Bought parts, for fit
box XiaoBoard size 17.5 mm 21 mm 4.4 mm corner (cx - 8.75 mm, wall + 0.4 mm, wall + 1 mm) new -> body Xiao
box LiPo size 20 mm 40 mm 6 mm corner (cx - 10 mm, 30 mm, wall + 0.05 mm) new -> body Battery
box FacePerf size 23.2 mm 138 mm 1.6 mm corner (cx - 11.6 mm, 5 mm, board_bot) new -> body FaceBoard
box StickModule size 20 mm 20 mm 6 mm corner (nun_x - 10 mm, nun_y + 0.8*nun_l - 10 mm, nun_z + 0.6*nun_r - nun_wall - 6.1 mm) new -> body Stick
cylinder StickNub radius 8 mm height 7 mm base (nun_x, nun_y + 0.8*nun_l, nun_z + 0.6*nun_r - nun_wall - 0.2 mm) axis Z join targets Stick
sketch CableSk on XY offset (split_z + 3.4 mm) {
  line a = (cx, 1 mm) (cx, -25 mm)
  arc b = start a.end through ((cx + nun_x)/2, -25 mm - (nun_x - cx)/2) end (nun_x, -25 mm)
  line c = b.end (nun_x, nun_y + 4 mm)
  fix a b c
}
pipe CableRun path CableSk.a, CableSk.b, CableSk.c diameter cable_d new -> body Cable

color Top, Bottom, Nunchuk, NunchukL "#f5f5f3"
color DPad, A, Minus, Home, Plus, One, Two, B, C, Z "#e9e9e6"
color Power "#d23c32"
color IrWindow "#2a2a30"
color Xiao, FaceBoard "#2f7d4a"
color Battery "#3b5ba5"
color Stick "#3a3a3a"
color Cable "#e2e2df"
material Top, Bottom, Nunchuk, NunchukL, DPad, A, Minus, Home, Plus, One, Two, Power, B, C, Z, IrWindow PLA
