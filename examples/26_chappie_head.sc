// Chappie's head (Chappie, 2015) as a working, printable robot head: ears, brow loop and chin
// loops on four micro servos, a lit visor, and an ESP32-C3 inside, on a stand that a USB-C cable
// runs up through. Firmware: examples/26_chappie_head_firmware.
// Geometry: from "chappie the robot" by jgdnetwork on Sketchfab (CC BY 4.0,
// https://sketchfab.com/3d-models/a1b989801e8b47e8a7c21e3f26f55d31): each part's outline and
// position measured off that mesh, rebuilt as solids and scaled 1.5x (131 mm across the side pods,
// 233 mm chin to crown without the ears), so the servos fit inside.
// Printed (no supports):
// - The head: 2.5 mm shells split down the middle; two pairs of bar magnets in the cut faces hold
//   the halves together, so the head opens for access. Each half is three pieces glued together:
//   HeadRingLeft (the walls, printed on the cut face; inside: the board pocket), LedShelfLeft (the
//   LED strip's shelf, glued to the cap's shelf stub) and HeadCapLeft (the side wall with the pods,
//   bars and the servo pockets, printed on its flat inner face). The right half's are their
//   mirrors, "HeadRingLeft (2)", "LedShelfLeft (2)" and "HeadCapLeft (2)".
// - BrowLoop*/ChinLoop* halves: the moving loops, each side a flat arm with its pivot hub, printed
//   on its outer face. The halves of each loop meet at the middle on 1.75 mm filament pins
//   (2.1 mm holes); glue each pin into one half only, so the loops part when the head opens.
// - EarRight/EarLeft: printed on their outer faces.
// - Screen: the visor's diffuser, in white or natural PLA, glued into the visor's recess.
// - PivotPlug x2: the free pivot of the brow and chin loops (glued into the wall hole from inside).
// - Stand: base and hollow post. BoardCradle: the ESP32-C3's holder, dropped into the pocket
//   in the neck before the head is closed.
// Fixing, with four screws in all: each servo's tabs are riveted to its cap with 1.75 mm
// filament (push it through the tab into the 2.0 mm hole, trim, melt the head flat); the pivot
// plugs are glued in; each ear and each loop's driven hub takes one M2 x 5-6 screw into the
// servo shaft; the head halves close on magnets; ring to cap, screen and loop pins are glued.
// Hardware: 4 x SG90 micro servo, 4 x M2 x 5-6 screw, 12 x WS2812B on 144/m strip, ESP32-C3
// SuperMini, 4 x 59 x 9.8 x 2.7 mm bar magnets, slim USB-C cable, 5 V 2-3 A USB supply.
// Reference bodies (not printed): Servo*, LedStrip, Esp32.
// Frame: X to the head's right, Y front (-) to back, Z up.
design ChappieHead units mm
// ==== HeadRingLeft: the shell's walls up to the side wall's inner face (printed on the cut face)
sketch Sk1 on YZ offset (-38.30 mm) {
  polyline closed p = ((-117.15 mm),(-87.90 mm)) ((-106.20 mm),(-96.60 mm)) ((-100.65 mm),(-97.95 mm)) ((-81.00 mm),(-97.80 mm)) ((-24.00 mm),(-89.70 mm)) ((-1.80 mm),(-84.90 mm)) (24.90 mm,(-71.70 mm)) (90.45 mm,(-33.60 mm)) (63.90 mm,38.40 mm) (11.25 mm,52.35 mm) ((-8.25 mm),52.50 mm) ((-53.70 mm),50.55 mm) ((-68.55 mm),44.10 mm) ((-82.80 mm),34.35 mm) ((-116.55 mm),3.90 mm)
  fix p.1 p.2 p.3 p.4 p.5 p.6 p.7 p.8 p.9 p.10 p.11 p.12 p.13 p.14 p.15
}
extrude Ex2 from Sk1 distance 38.30 mm -> body HeadRingLeft
box Bx3 size 38.30 mm 48.60 mm 28.65 mm corner ((-38.30 mm),(-124.50 mm),(-33.15 mm)) join targets HeadRingLeft
box Bx4 size 38.30 mm 4.50 mm 31.95 mm corner ((-38.30 mm),(-127.50 mm),(-36.45 mm)) join targets HeadRingLeft
box Bx5 size 25.05 mm 26.40 mm 38.55 mm corner ((-25.05 mm),(-120.90 mm),(-79.80 mm)) join targets HeadRingLeft
sketch Sk6 on YZ offset (-29.55 mm) {
  polyline closed p = ((-107.10 mm),(-135.00 mm)) ((-80.25 mm),(-134.85 mm)) ((-79.05 mm),(-93.00 mm)) ((-100.20 mm),(-93.00 mm)) ((-107.25 mm),(-104.70 mm))
  fix p.1 p.2 p.3 p.4 p.5
}
extrude Ex7 from Sk6 distance 29.55 mm join targets HeadRingLeft
cylinder Cy8 radius 12.00 mm height 33.15 mm base ((-33.15 mm),(-94.05 mm),(-118.65 mm)) axis X join targets HeadRingLeft
box Bx9 size 5.85 mm 5.25 mm 28.05 mm corner ((-5.85 mm),(-111.00 mm),(-134.10 mm)) join targets HeadRingLeft
sketch Sk10 on YZ offset (-31.50 mm) {
  polyline closed p = (49.95 mm,(-47.85 mm)) (50.25 mm,(-94.80 mm)) (78.45 mm,(-82.65 mm)) (72.45 mm,(-46.65 mm))
  fix p.1 p.2 p.3 p.4
}
extrude Ex11 from Sk10 distance 31.50 mm join targets HeadRingLeft
sketch Sk12 on YZ offset (-34.05 mm) {
  polyline closed p = (25.65 mm,(-97.50 mm)) (50.25 mm,(-97.50 mm)) (49.95 mm,(-44.70 mm)) (25.80 mm,(-54.45 mm))
  fix p.1 p.2 p.3 p.4
}
extrude Ex13 from Sk12 distance 34.05 mm join targets HeadRingLeft
sketch Sk14 on YZ offset (-35.25 mm) {
  polyline closed p = (0.45 mm,(-93.90 mm)) (7.05 mm,(-100.80 mm)) (25.05 mm,(-100.80 mm)) (24.75 mm,(-48.00 mm)) (7.05 mm,(-54.45 mm)) (0.60 mm,(-57.75 mm))
  fix p.1 p.2 p.3 p.4 p.5 p.6
}
extrude Ex15 from Sk14 distance 35.25 mm join targets HeadRingLeft
sketch Sk16 on YZ offset (-2.25 mm) {
  polyline closed p = (63.00 mm,40.20 mm) (77.10 mm,2.70 mm) (87.00 mm,6.45 mm) (73.05 mm,43.95 mm)
  fix p.1 p.2 p.3 p.4
}
extrude Ex17 from Sk16 distance 2.25 mm join targets HeadRingLeft
box Bx18 size 2.25 mm 14.25 mm 37.80 mm corner ((-2.25 mm),73.35 mm,6.45 mm) join targets HeadRingLeft
sketch Sk19 on YZ offset (-31.20 mm) {
  polyline closed p = (82.20 mm,(-16.80 mm)) (91.35 mm,(-13.50 mm)) (72.45 mm,35.40 mm) (63.60 mm,32.10 mm)
  fix p.1 p.2 p.3 p.4
}
extrude Ex20 from Sk19 distance 31.20 mm join targets HeadRingLeft
// side pods with their bolt ring; cleared round the ear's hub
// side bars along the top, on the skull's sides, stopping clear of the brow loop's hub
cylinder Cy21 radius 4.80 mm height 38.30 mm base ((-38.30 mm),24.45 mm,37.80 mm) axis X join targets HeadRingLeft
// hollowing: The skull's inside, the visor box's inside (open to it), the visor
// window and the diffuser's recess
sketch Sk22 on YZ offset (-38.30 mm) {
  polyline closed p = ((-114.64 mm),(-86.70 mm)) ((-105.08 mm),(-94.30 mm)) ((-100.36 mm),(-95.45 mm)) ((-81.19 mm),(-95.30 mm)) ((-24.44 mm),(-87.24 mm)) ((-2.63 mm),(-82.52 mm)) (23.72 mm,(-69.50 mm)) (87.38 mm,(-32.49 mm)) (62.00 mm,36.32 mm) (10.91 mm,49.85 mm) ((-8.21 mm),50.00 mm) ((-53.13 mm),48.07 mm) ((-67.34 mm),41.90 mm) ((-81.25 mm),32.38 mm) ((-114.06 mm),2.78 mm)
  fix p.1 p.2 p.3 p.4 p.5 p.6 p.7 p.8 p.9 p.10 p.11 p.12 p.13 p.14 p.15
}
extrude Ex23 from Sk22 distance 38.30 mm cut targets HeadRingLeft
box Bx24 size 38.30 mm 46.10 mm 23.65 mm corner ((-38.30 mm),(-122.00 mm),(-30.65 mm)) cut targets HeadRingLeft
box Bx25 size 38.30 mm 3.00 mm 25.20 mm corner ((-38.30 mm),(-128.50 mm),(-31.35 mm)) cut targets HeadRingLeft
box Bx26 size 38.30 mm 5.50 mm 22.20 mm corner ((-38.30 mm),(-126.50 mm),(-29.85 mm)) cut targets HeadRingLeft
// LED shelf: a plate 12 mm behind the diffuser for the strip, across the visor. Nothing in the ring
// holds it (the visor pocket opens into the skull), so it is a part of its own, glued to the cap's
// shelf stub (Bx74) at x = -38.30.
box Bx27 size 38.30 mm 2.00 mm 16.00 mm corner ((-38.30 mm),(-113.50 mm),(-26.83 mm)) new -> body LedShelfLeft
// servo mounts: each servo's top sits in a pocket in the side wall with its tabs flat on the
// inner face (riveted with 1.75 mm filament through the tab holes into 2.0 mm holes), under a housing bump outside; its boss
// comes out through the bump. Ear, brow and chin positions (the mirror gives the right half
// the same; the brow servo goes on the right, the chin servo on the left, a PivotPlug in each
// other pocket)
box Bx28 size 1.00 mm 23.20 mm 12.40 mm corner ((-38.30 mm),15.63 mm,4.07 mm) cut targets HeadRingLeft
cylinder Cy29 radius 1.00 mm height 1.00 mm base ((-38.30 mm),13.33 mm,10.27 mm) axis X cut targets HeadRingLeft
cylinder Cy30 radius 1.00 mm height 1.00 mm base ((-38.30 mm),41.12 mm,10.27 mm) axis X cut targets HeadRingLeft
box Bx31 size 1.00 mm 23.20 mm 12.40 mm corner ((-38.30 mm),(-92.75 mm),(-2.30 mm)) cut targets HeadRingLeft
cylinder Cy32 radius 1.00 mm height 1.00 mm base ((-38.30 mm),(-95.05 mm),3.90 mm) axis X cut targets HeadRingLeft
cylinder Cy33 radius 1.00 mm height 1.00 mm base ((-38.30 mm),(-67.25 mm),3.90 mm) axis X cut targets HeadRingLeft
box Bx34 size 1.00 mm 23.20 mm 12.40 mm corner ((-38.30 mm),(-87.35 mm),(-86.90 mm)) cut targets HeadRingLeft
cylinder Cy35 radius 1.00 mm height 1.00 mm base ((-38.30 mm),(-89.65 mm),(-80.70 mm)) axis X cut targets HeadRingLeft
cylinder Cy36 radius 1.00 mm height 1.00 mm base ((-38.30 mm),(-61.85 mm),(-80.70 mm)) axis X cut targets HeadRingLeft
// magnets: two 59 x 9.8 x 2.7 mm bar magnets per half, one along the crown and one along
// the sloped back, each in a slot in a rib on the cut face (the other half's magnet faces it)
sketch Sk37 on YZ offset (-4.10 mm) {
  polyline closed p = (9.64 mm,51.50 mm) ((-51.74 mm),49.78 mm) ((-51.38 mm),37.09 mm) (9.99 mm,38.80 mm)
  fix p.1 p.2 p.3 p.4
}
extrude Ex38 from Sk37 distance 4.10 mm join targets HeadRingLeft
sketch Sk39 on YZ offset (-2.90 mm) {
  polyline closed p = (8.68 mm,49.97 mm) ((-50.70 mm),48.31 mm) ((-50.43 mm),38.61 mm) (8.95 mm,40.27 mm)
  fix p.1 p.2 p.3 p.4
}
extrude Ex40 from Sk39 distance 2.90 mm cut targets HeadRingLeft
sketch Sk41 on YZ offset (-4.10 mm) {
  polyline closed p = (85.95 mm,(-25.74 mm)) (64.71 mm,31.87 mm) (52.80 mm,27.47 mm) (74.04 mm,(-30.14 mm))
  fix p.1 p.2 p.3 p.4
}
extrude Ex42 from Sk41 distance 4.10 mm join targets HeadRingLeft
sketch Sk43 on YZ offset (-2.90 mm) {
  polyline closed p = (84.20 mm,(-25.32 mm)) (63.65 mm,30.41 mm) (54.55 mm,27.05 mm) (75.10 mm,(-28.68 mm))
  fix p.1 p.2 p.3 p.4
}
extrude Ex44 from Sk43 distance 2.90 mm cut targets HeadRingLeft
// board pocket: a plain pocket across the cut for the BoardCradle, which the halves hold when
// the head closes
box Bx45 size 11.85 mm 33.80 mm 43.65 mm corner ((-11.85 mm),31.05 mm,(-73.65 mm)) cut targets HeadRingLeft
// neck socket for the stand's post (and the USB-C plug inside it)
cylinder Cy46 radius 9.30 mm height 27.35 mm base (0.00 mm,37.50 mm,(-100.00 mm)) cut targets HeadRingLeft
// ==== HeadCapLeft: the side wall and all on it (printed on its inner face)
sketch Sk47 on YZ offset (-40.80 mm) {
  polyline closed p = ((-117.15 mm),(-87.90 mm)) ((-106.20 mm),(-96.60 mm)) ((-100.65 mm),(-97.95 mm)) ((-81.00 mm),(-97.80 mm)) ((-24.00 mm),(-89.70 mm)) ((-1.80 mm),(-84.90 mm)) (24.90 mm,(-71.70 mm)) (90.45 mm,(-33.60 mm)) (63.90 mm,38.40 mm) (11.25 mm,52.35 mm) ((-8.25 mm),52.50 mm) ((-53.70 mm),50.55 mm) ((-68.55 mm),44.10 mm) ((-82.80 mm),34.35 mm) ((-116.55 mm),3.90 mm)
  fix p.1 p.2 p.3 p.4 p.5 p.6 p.7 p.8 p.9 p.10 p.11 p.12 p.13 p.14 p.15
}
extrude Ex48 from Sk47 distance 2.50 mm -> body HeadCapLeft
box Bx49 size 5.80 mm 48.60 mm 28.65 mm corner ((-44.10 mm),(-124.50 mm),(-33.15 mm)) join targets HeadCapLeft
box Bx50 size 5.80 mm 4.50 mm 31.95 mm corner ((-44.10 mm),(-127.50 mm),(-36.45 mm)) join targets HeadCapLeft
// side pods with their bolt ring; cleared round the ear's hub
cylinder Cy51 radius 60.30 mm height 11.85 mm base ((-51.15 mm),(-21.23 mm),(-24.15 mm)) axis X join targets HeadCapLeft
cylinder Cy52 radius 46.50 mm height 15.60 mm base ((-65.25 mm),(-21.23 mm),(-24.15 mm)) axis X join targets HeadCapLeft
cylinder Cy53 radius 2.70 mm height 3.45 mm base ((-53.85 mm),30.52 mm,(-24.15 mm)) axis X join targets HeadCapLeft
cylinder Cy54 radius 2.70 mm height 3.45 mm base ((-53.85 mm),23.59 mm,1.72 mm) axis X join targets HeadCapLeft
cylinder Cy55 radius 2.70 mm height 3.45 mm base ((-53.85 mm),4.65 mm,20.67 mm) axis X join targets HeadCapLeft
cylinder Cy56 radius 2.70 mm height 3.45 mm base ((-53.85 mm),(-21.22 mm),27.60 mm) axis X join targets HeadCapLeft
cylinder Cy57 radius 2.70 mm height 3.45 mm base ((-53.85 mm),(-47.10 mm),20.67 mm) axis X join targets HeadCapLeft
cylinder Cy58 radius 2.70 mm height 3.45 mm base ((-53.85 mm),(-66.04 mm),1.72 mm) axis X join targets HeadCapLeft
cylinder Cy59 radius 2.70 mm height 3.45 mm base ((-53.85 mm),(-72.97 mm),(-24.15 mm)) axis X join targets HeadCapLeft
cylinder Cy60 radius 2.70 mm height 3.45 mm base ((-53.85 mm),(-66.04 mm),(-50.02 mm)) axis X join targets HeadCapLeft
cylinder Cy61 radius 2.70 mm height 3.45 mm base ((-53.85 mm),(-47.10 mm),(-68.97 mm)) axis X join targets HeadCapLeft
cylinder Cy62 radius 2.70 mm height 3.45 mm base ((-53.85 mm),(-21.23 mm),(-75.90 mm)) axis X join targets HeadCapLeft
cylinder Cy63 radius 2.70 mm height 3.45 mm base ((-53.85 mm),4.65 mm,(-68.97 mm)) axis X join targets HeadCapLeft
cylinder Cy64 radius 2.70 mm height 3.45 mm base ((-53.85 mm),23.59 mm,(-50.03 mm)) axis X join targets HeadCapLeft
cylinder Cy65 radius 15.83 mm height 19.10 mm base ((-66.00 mm),32.62 mm,10.27 mm) axis X cut targets HeadCapLeft
cylinder Cy66 radius 14.40 mm height 19.10 mm base ((-66.00 mm),(-86.55 mm),3.90 mm) axis X cut targets HeadCapLeft
cylinder Cy67 radius 14.40 mm height 19.10 mm base ((-66.00 mm),(-81.15 mm),(-80.70 mm)) axis X cut targets HeadCapLeft
// side bars along the top, on the skull's sides, stopping clear of the brow loop's hub
sketch Sk68 on YZ offset (-49.40 mm) {
  slot c = centers ((-71.09 mm),16.45 mm) (24.45 mm,37.80 mm) width 9.60 mm
  fix c.line1 c.line2 c.arc1 c.arc2
}
extrude Ex69 from Sk68 distance 9.60 mm join targets HeadCapLeft
cylinder Cy70 radius 4.80 mm height 11.10 mm base ((-49.40 mm),24.45 mm,37.80 mm) axis X join targets HeadCapLeft
// hollowing: The skull's inside, the visor box's inside (open to it), the visor
// window and the diffuser's recess
box Bx71 size 3.30 mm 46.10 mm 23.65 mm corner ((-41.60 mm),(-122.00 mm),(-30.65 mm)) cut targets HeadCapLeft
box Bx72 size 3.40 mm 3.00 mm 25.20 mm corner ((-41.70 mm),(-128.50 mm),(-31.35 mm)) cut targets HeadCapLeft
box Bx73 size 1.90 mm 5.50 mm 22.20 mm corner ((-40.20 mm),(-126.50 mm),(-29.85 mm)) cut targets HeadCapLeft
// LED shelf: a plate 12 mm behind the diffuser for the strip, across the visor
box Bx74 size 4.30 mm 2.00 mm 16.00 mm corner ((-42.60 mm),(-113.50 mm),(-26.83 mm)) join targets HeadCapLeft
// servo mounts: each servo's top sits in a pocket in the side wall with its tabs flat on the
// inner face (riveted with 1.75 mm filament through the tab holes into 2.0 mm holes), under a housing bump outside; its boss
// comes out through the bump. Ear, brow and chin positions (the mirror gives the right half
// the same; the brow servo goes on the right, the chin servo on the left, a PivotPlug in each
// other pocket)
box Bx75 size 4.10 mm 33.80 mm 16.40 mm corner ((-43.90 mm),10.33 mm,2.07 mm) join targets HeadCapLeft
box Bx76 size 4.40 mm 23.20 mm 12.40 mm corner ((-42.70 mm),15.63 mm,4.07 mm) cut targets HeadCapLeft
sketch Sk77 on YZ offset (-47.90 mm) {
  slot c = centers (32.62 mm,10.27 mm) (26.62 mm,10.27 mm) width 11.60 mm
  fix c.line1 c.line2 c.arc1 c.arc2
}
extrude Ex78 from Sk77 distance 5.70 mm cut targets HeadCapLeft
cylinder Cy79 radius 1.00 mm height 5.10 mm base ((-43.40 mm),13.33 mm,10.27 mm) axis X cut targets HeadCapLeft
cylinder Cy80 radius 1.00 mm height 5.10 mm base ((-43.40 mm),41.12 mm,10.27 mm) axis X cut targets HeadCapLeft
box Bx81 size 4.10 mm 33.80 mm 16.40 mm corner ((-43.90 mm),(-98.05 mm),(-4.30 mm)) join targets HeadCapLeft
box Bx82 size 4.40 mm 23.20 mm 12.40 mm corner ((-42.70 mm),(-92.75 mm),(-2.30 mm)) cut targets HeadCapLeft
sketch Sk83 on YZ offset (-47.90 mm) {
  slot c = centers ((-86.55 mm),3.90 mm) ((-80.55 mm),3.90 mm) width 11.60 mm
  fix c.line1 c.line2 c.arc1 c.arc2
}
extrude Ex84 from Sk83 distance 5.70 mm cut targets HeadCapLeft
cylinder Cy85 radius 1.00 mm height 5.10 mm base ((-43.40 mm),(-95.05 mm),3.90 mm) axis X cut targets HeadCapLeft
cylinder Cy86 radius 1.00 mm height 5.10 mm base ((-43.40 mm),(-67.25 mm),3.90 mm) axis X cut targets HeadCapLeft
box Bx87 size 4.10 mm 33.80 mm 16.40 mm corner ((-43.90 mm),(-92.65 mm),(-88.90 mm)) join targets HeadCapLeft
box Bx88 size 4.40 mm 23.20 mm 12.40 mm corner ((-42.70 mm),(-87.35 mm),(-86.90 mm)) cut targets HeadCapLeft
sketch Sk89 on YZ offset (-47.90 mm) {
  slot c = centers ((-81.15 mm),(-80.70 mm)) ((-75.15 mm),(-80.70 mm)) width 11.60 mm
  fix c.line1 c.line2 c.arc1 c.arc2
}
extrude Ex90 from Sk89 distance 5.70 mm cut targets HeadCapLeft
cylinder Cy91 radius 1.00 mm height 5.10 mm base ((-43.40 mm),(-89.65 mm),(-80.70 mm)) axis X cut targets HeadCapLeft
cylinder Cy92 radius 1.00 mm height 5.10 mm base ((-43.40 mm),(-61.85 mm),(-80.70 mm)) axis X cut targets HeadCapLeft
// magnets: two 59 x 9.8 x 2.7 mm bar magnets per half, one along the crown and one along
// the sloped back, each in a slot in a rib on the cut face (the other half's magnet faces it)
// board pocket: a plain pocket across the cut for the BoardCradle, which the halves hold when
// the head closes
// neck socket for the stand's post (and the USB-C plug inside it)
// ==== the right half: both pieces mirrored (named "HeadRingLeft (2)" and "HeadCapLeft (2)")
mirror RingMirror bodies HeadRingLeft plane YZ
mirror ShelfMirror bodies LedShelfLeft plane YZ
mirror CapMirror bodies HeadCapLeft plane YZ
// ---- BrowLoopRight
cylinder Cy93 radius 12.90 mm height 11.10 mm base (47.40 mm,(-86.55 mm),3.90 mm) axis X new -> body BrowLoopRight
sketch Sk94 on YZ offset 47.40 mm {
  slot c = centers ((-86.55 mm),3.90 mm) ((-123.68 mm),5.40 mm) width 11.10 mm
  fix c.line1 c.line2 c.arc1 c.arc2
}
extrude Ex95 from Sk94 distance 11.10 mm join targets BrowLoopRight
cylinder Cy96 radius 5.55 mm height 48.40 mm base (0.00 mm,(-123.68 mm),5.40 mm) axis X join targets BrowLoopRight
cylinder Cy97 radius 1.05 mm height 9.00 mm base ((-1.00 mm),(-123.68 mm),5.40 mm) axis X cut targets BrowLoopRight
cylinder Cy98 radius 2.38 mm height 4.00 mm base (46.40 mm,(-86.55 mm),3.90 mm) axis X cut targets BrowLoopRight
cylinder Cy99 radius 1.10 mm height 13.10 mm base (46.40 mm,(-86.55 mm),3.90 mm) axis X cut targets BrowLoopRight
cylinder Cy100 radius 2.40 mm height 7.60 mm base (51.90 mm,(-86.55 mm),3.90 mm) axis X cut targets BrowLoopRight
// ---- ChinLoopRight
cylinder Cy101 radius 12.90 mm height 9.60 mm base (47.40 mm,(-81.15 mm),(-80.70 mm)) axis X new -> body ChinLoopRight
sketch Sk102 on YZ offset 47.40 mm {
  slot c = centers ((-81.15 mm),(-80.70 mm)) ((-124.05 mm),(-102.30 mm)) width 9.60 mm
  fix c.line1 c.line2 c.arc1 c.arc2
}
extrude Ex103 from Sk102 distance 9.60 mm join targets ChinLoopRight
cylinder Cy104 radius 4.80 mm height 48.40 mm base (0.00 mm,(-124.05 mm),(-102.30 mm)) axis X join targets ChinLoopRight
cylinder Cy105 radius 1.05 mm height 9.00 mm base ((-1.00 mm),(-124.05 mm),(-102.30 mm)) axis X cut targets ChinLoopRight
sketch Sk106 on YZ offset 47.40 mm {
  slot c = centers ((-81.15 mm),(-80.70 mm)) ((-36.00 mm),(-99.45 mm)) width 9.60 mm
  fix c.line1 c.line2 c.arc1 c.arc2
}
extrude Ex107 from Sk106 distance 9.60 mm join targets ChinLoopRight
cylinder Cy108 radius 4.80 mm height 48.40 mm base (0.00 mm,(-36.00 mm),(-99.45 mm)) axis X join targets ChinLoopRight
cylinder Cy109 radius 1.05 mm height 9.00 mm base ((-1.00 mm),(-36.00 mm),(-99.45 mm)) axis X cut targets ChinLoopRight
cylinder Cy110 radius 2.38 mm height 4.00 mm base (46.40 mm,(-81.15 mm),(-80.70 mm)) axis X cut targets ChinLoopRight
cylinder Cy111 radius 1.10 mm height 11.60 mm base (46.40 mm,(-81.15 mm),(-80.70 mm)) axis X cut targets ChinLoopRight
cylinder Cy112 radius 2.40 mm height 6.10 mm base (51.90 mm,(-81.15 mm),(-80.70 mm)) axis X cut targets ChinLoopRight
// ---- BrowLoopLeft
cylinder Cy113 radius 12.90 mm height 11.10 mm base ((-58.50 mm),(-86.55 mm),3.90 mm) axis X new -> body BrowLoopLeft
sketch Sk114 on YZ offset (-58.50 mm) {
  slot c = centers ((-86.55 mm),3.90 mm) ((-123.68 mm),5.40 mm) width 11.10 mm
  fix c.line1 c.line2 c.arc1 c.arc2
}
extrude Ex115 from Sk114 distance 11.10 mm join targets BrowLoopLeft
cylinder Cy116 radius 5.55 mm height 48.40 mm base ((-48.40 mm),(-123.68 mm),5.40 mm) axis X join targets BrowLoopLeft
cylinder Cy117 radius 1.05 mm height 9.00 mm base ((-8.00 mm),(-123.68 mm),5.40 mm) axis X cut targets BrowLoopLeft
cylinder Cy118 radius 2.38 mm height 4.00 mm base ((-50.40 mm),(-86.55 mm),3.90 mm) axis X cut targets BrowLoopLeft
cylinder Cy119 radius 1.10 mm height 13.10 mm base ((-59.50 mm),(-86.55 mm),3.90 mm) axis X cut targets BrowLoopLeft
cylinder Cy120 radius 2.40 mm height 7.60 mm base ((-59.50 mm),(-86.55 mm),3.90 mm) axis X cut targets BrowLoopLeft
// ---- ChinLoopLeft
cylinder Cy121 radius 12.90 mm height 9.60 mm base ((-57.00 mm),(-81.15 mm),(-80.70 mm)) axis X new -> body ChinLoopLeft
sketch Sk122 on YZ offset (-57.00 mm) {
  slot c = centers ((-81.15 mm),(-80.70 mm)) ((-124.05 mm),(-102.30 mm)) width 9.60 mm
  fix c.line1 c.line2 c.arc1 c.arc2
}
extrude Ex123 from Sk122 distance 9.60 mm join targets ChinLoopLeft
cylinder Cy124 radius 4.80 mm height 48.40 mm base ((-48.40 mm),(-124.05 mm),(-102.30 mm)) axis X join targets ChinLoopLeft
cylinder Cy125 radius 1.05 mm height 9.00 mm base ((-8.00 mm),(-124.05 mm),(-102.30 mm)) axis X cut targets ChinLoopLeft
sketch Sk126 on YZ offset (-57.00 mm) {
  slot c = centers ((-81.15 mm),(-80.70 mm)) ((-36.00 mm),(-99.45 mm)) width 9.60 mm
  fix c.line1 c.line2 c.arc1 c.arc2
}
extrude Ex127 from Sk126 distance 9.60 mm join targets ChinLoopLeft
cylinder Cy128 radius 4.80 mm height 48.40 mm base ((-48.40 mm),(-36.00 mm),(-99.45 mm)) axis X join targets ChinLoopLeft
cylinder Cy129 radius 1.05 mm height 9.00 mm base ((-8.00 mm),(-36.00 mm),(-99.45 mm)) axis X cut targets ChinLoopLeft
cylinder Cy130 radius 2.38 mm height 4.00 mm base ((-50.40 mm),(-81.15 mm),(-80.70 mm)) axis X cut targets ChinLoopLeft
cylinder Cy131 radius 1.10 mm height 11.60 mm base ((-58.00 mm),(-81.15 mm),(-80.70 mm)) axis X cut targets ChinLoopLeft
cylinder Cy132 radius 2.40 mm height 6.10 mm base ((-58.00 mm),(-81.15 mm),(-80.70 mm)) axis X cut targets ChinLoopLeft
// ---- EarRight: blade and hub, on its servo's spline
sketch Sk133 on YZ offset 45.00 mm {
  polyline closed p = (33.15 mm,23.40 mm) (40.05 mm,16.20 mm) (55.65 mm,19.05 mm) (86.25 mm,39.75 mm) (147.75 mm,109.50 mm) (145.50 mm,114.15 mm) (132.00 mm,129.00 mm) (57.15 mm,68.70 mm) (38.55 mm,36.75 mm)
  fix p.1 p.2 p.3 p.4 p.5 p.6 p.7 p.8 p.9
}
extrude Ex134 from Sk133 distance 11.46 mm new -> body EarRight
cylinder Cy135 radius 14.33 mm height 9.06 mm base (47.40 mm,32.62 mm,10.27 mm) axis X join targets EarRight
cylinder Cy136 radius 2.38 mm height 4.00 mm base (46.40 mm,32.62 mm,10.27 mm) axis X cut targets EarRight
cylinder Cy137 radius 1.10 mm height 11.06 mm base (46.40 mm,32.62 mm,10.27 mm) axis X cut targets EarRight
cylinder Cy138 radius 2.40 mm height 5.56 mm base (51.90 mm,32.62 mm,10.27 mm) axis X cut targets EarRight
// ---- EarLeft: blade and hub, on its servo's spline
sketch Sk139 on YZ offset (-56.46 mm) {
  polyline closed p = (33.15 mm,23.40 mm) (40.05 mm,16.20 mm) (55.65 mm,19.05 mm) (86.25 mm,39.75 mm) (147.75 mm,109.50 mm) (145.50 mm,114.15 mm) (132.00 mm,129.00 mm) (57.15 mm,68.70 mm) (38.55 mm,36.75 mm)
  fix p.1 p.2 p.3 p.4 p.5 p.6 p.7 p.8 p.9
}
extrude Ex140 from Sk139 distance 11.46 mm new -> body EarLeft
cylinder Cy141 radius 14.33 mm height 9.06 mm base ((-56.46 mm),32.62 mm,10.27 mm) axis X join targets EarLeft
cylinder Cy142 radius 2.38 mm height 4.00 mm base ((-50.40 mm),32.62 mm,10.27 mm) axis X cut targets EarLeft
cylinder Cy143 radius 1.10 mm height 11.06 mm base ((-57.46 mm),32.62 mm,10.27 mm) axis X cut targets EarLeft
cylinder Cy144 radius 2.40 mm height 5.56 mm base ((-57.46 mm),32.62 mm,10.27 mm) axis X cut targets EarLeft
// ---- Screen: the visor's diffuser
box Bx145 size 82.80 mm 1.80 mm 24.60 mm corner ((-41.40 mm),(-127.50 mm),(-31.05 mm)) new -> body Screen
// ---- PivotPlugBrow: the free pivot, shaped like the servo's top (tabs, top block, boss) with a pin
// for the loop's hub; glued into the unused servo pocket
box Bx146 size 2.50 mm 32.50 mm 12.00 mm corner ((-38.30 mm),(-97.40 mm),(-2.10 mm)) new -> body PivotPlugBrow
box Bx147 size 4.90 mm 22.80 mm 12.00 mm corner ((-42.70 mm),(-92.55 mm),(-2.10 mm)) join targets PivotPlugBrow
cylinder Cy148 radius 5.50 mm height 4.90 mm base ((-47.10 mm),(-86.55 mm),3.90 mm) axis X join targets PivotPlugBrow
cylinder Cy149 radius 2.23 mm height 3.80 mm base ((-50.40 mm),(-86.55 mm),3.90 mm) axis X join targets PivotPlugBrow
cylinder Cy150 radius 1.10 mm height 4.50 mm base ((-39.30 mm),(-95.05 mm),3.90 mm) axis X cut targets PivotPlugBrow
cylinder Cy151 radius 1.10 mm height 4.50 mm base ((-39.30 mm),(-67.25 mm),3.90 mm) axis X cut targets PivotPlugBrow
// ---- PivotPlugChin: the free pivot, shaped like the servo's top (tabs, top block, boss) with a pin
// for the loop's hub; glued into the unused servo pocket
box Bx152 size 2.50 mm 32.50 mm 12.00 mm corner (35.80 mm,(-92.00 mm),(-86.70 mm)) new -> body PivotPlugChin
box Bx153 size 4.90 mm 22.80 mm 12.00 mm corner (37.80 mm,(-87.15 mm),(-86.70 mm)) join targets PivotPlugChin
cylinder Cy154 radius 5.50 mm height 4.90 mm base (42.20 mm,(-81.15 mm),(-80.70 mm)) axis X join targets PivotPlugChin
cylinder Cy155 radius 2.23 mm height 3.80 mm base (46.60 mm,(-81.15 mm),(-80.70 mm)) axis X join targets PivotPlugChin
cylinder Cy156 radius 1.10 mm height 4.50 mm base (34.80 mm,(-89.65 mm),(-80.70 mm)) axis X cut targets PivotPlugChin
cylinder Cy157 radius 1.10 mm height 4.50 mm base (34.80 mm,(-61.85 mm),(-80.70 mm)) axis X cut targets PivotPlugChin
// ---- Stand: base and a hollow post up into the neck socket; the USB-C cable runs up the post
// and out of the base through a channel underneath
cylinder Cy158 radius 82.50 mm height 9.00 mm base (0.00 mm,(-15.00 mm),(-174.00 mm)) new -> body Stand
cylinder Cy159 radius 9.25 mm height 91.50 mm base (0.00 mm,37.50 mm,(-166.00 mm)) join targets Stand
cylinder Cy160 radius 6.75 mm height 101.50 mm base (0.00 mm,37.50 mm,(-175.00 mm)) cut targets Stand
box Bx161 size 9.00 mm 45.00 mm 6.00 mm corner ((-4.50 mm),37.50 mm,(-175.00 mm)) cut targets Stand
// ---- BoardCradle: holds the ESP32-C3 on edge, USB-C down: its parts against the front wall,
// the PCB's back on the rib between the header rows, Dupont plugs and their wires behind. The
// USB-C plug comes up through the floor. Printed floor down; swap it to change the board mount
box Bx162 size 23.40 mm 33.50 mm 11.00 mm corner ((-11.70 mm),31.20 mm,(-73.50 mm)) new -> body BoardCradle
box Bx163 size 19.00 mm 29.10 mm 9.00 mm corner ((-9.50 mm),33.40 mm,(-70.50 mm)) cut targets BoardCradle
box Bx164 size 8.00 mm 25.30 mm 13.00 mm corner ((-4.00 mm),38.20 mm,(-71.50 mm)) join targets BoardCradle
box Bx165 size 14.00 mm 7.60 mm 4.50 mm corner ((-7.00 mm),31.50 mm,(-74.50 mm)) cut targets BoardCradle
// ---- Fit test (print first, a few minutes): a piece of side wall with one servo pocket, its
// figure-8 boss slot and tab-screw pilots, exactly as in the caps (inner face down), and a hub
// with the spline socket, as on the ears and loops. Placed away from the head.
box Bx166 size 5.60 mm 33.80 mm 20.00 mm corner (0.00 mm,(-16.90 mm),250.00 mm) new -> body FitTestPocket
box Bx167 size 5.40 mm 23.20 mm 12.40 mm corner ((-1.00 mm),(-11.60 mm),253.80 mm) cut targets FitTestPocket
sketch Sk168 on YZ offset 3.90 mm {
  slot c = centers ((-5.40 mm),260.00 mm) (0.60 mm,260.00 mm) width 11.60 mm
  fix c.line1 c.line2 c.arc1 c.arc2
}
extrude Ex169 from Sk168 distance 2.70 mm cut targets FitTestPocket
cylinder Cy170 radius 1.00 mm height 6.10 mm base ((-1.00 mm),(-13.90 mm),260.00 mm) axis X cut targets FitTestPocket
cylinder Cy171 radius 1.00 mm height 6.10 mm base ((-1.00 mm),13.90 mm,260.00 mm) axis X cut targets FitTestPocket
cylinder Cy172 radius 10.00 mm height 8.00 mm base (0.00 mm,40.00 mm,260.00 mm) axis X new -> body FitTestHub
cylinder Cy173 radius 2.38 mm height 4.00 mm base (5.00 mm,40.00 mm,260.00 mm) axis X cut targets FitTestHub
cylinder Cy174 radius 1.10 mm height 10.00 mm base ((-1.00 mm),40.00 mm,260.00 mm) axis X cut targets FitTestHub
cylinder Cy175 radius 2.40 mm height 4.50 mm base ((-1.00 mm),40.00 mm,260.00 mm) axis X cut targets FitTestHub
// ---- More fit tests, each with the head's own dimensions and printed the same way up:
// FitTestPlugPin (the pivot plug's pin: turns freely in FitTestHub's socket), FitTestMagnet (one
// bar magnet slot, open on the bed side as in the head), FitTestLoopPin (the loop halves' 2.1 mm
// hole for 1.75 mm filament), FitTestSocket and FitTestPost (the stand's post in the neck socket, and the USB-C plug through the post).
box Bx176 size 4.10 mm 61.40 mm 12.70 mm corner (0.00 mm,(-30.70 mm),293.65 mm) new -> body FitTestMagnet
box Bx177 size 3.90 mm 59.40 mm 9.70 mm corner ((-1.00 mm),(-29.70 mm),295.15 mm) cut targets FitTestMagnet
cylinder Cy178 radius 4.80 mm height 8.00 mm base (0.00 mm,110.00 mm,300.00 mm) axis X new -> body FitTestLoopPin
cylinder Cy179 radius 1.05 mm height 7.00 mm base (2.00 mm,110.00 mm,300.00 mm) axis X cut targets FitTestLoopPin
cylinder Cy180 radius 6.00 mm height 2.00 mm base (0.00 mm,130.00 mm,300.00 mm) axis X new -> body FitTestPlugPin
cylinder Cy181 radius 2.23 mm height 4.50 mm base (1.50 mm,130.00 mm,300.00 mm) axis X join targets FitTestPlugPin
cylinder Cy182 radius 13.00 mm height 10.00 mm base (0.00 mm,165.00 mm,300.00 mm) axis X new -> body FitTestSocket
cylinder Cy183 radius 9.30 mm height 12.00 mm base ((-1.00 mm),165.00 mm,300.00 mm) axis X cut targets FitTestSocket
cylinder Cy184 radius 9.25 mm height 20.00 mm base (0.00 mm,200.00 mm,300.00 mm) axis X new -> body FitTestPost
cylinder Cy185 radius 6.75 mm height 22.00 mm base ((-1.00 mm),200.00 mm,300.00 mm) axis X cut targets FitTestPost
// ---- Reference bodies: the servos, the LED strip and the board (not printed)
box Bx186 size 22.70 mm 22.80 mm 12.00 mm corner (20.00 mm,15.83 mm,4.27 mm) new -> body ServoEarRight
box Bx187 size 2.50 mm 32.50 mm 12.00 mm corner (35.80 mm,10.98 mm,4.27 mm) join targets ServoEarRight
cylinder Cy188 radius 5.50 mm height 4.90 mm base (42.20 mm,32.62 mm,10.27 mm) axis X join targets ServoEarRight
cylinder Cy189 radius 2.75 mm height 4.90 mm base (42.20 mm,27.12 mm,10.27 mm) axis X join targets ServoEarRight
cylinder Cy190 radius 2.40 mm height 3.50 mm base (46.60 mm,32.62 mm,10.27 mm) axis X join targets ServoEarRight
box Bx191 size 22.70 mm 22.80 mm 12.00 mm corner ((-42.70 mm),15.83 mm,4.27 mm) new -> body ServoEarLeft
box Bx192 size 2.50 mm 32.50 mm 12.00 mm corner ((-38.30 mm),10.98 mm,4.27 mm) join targets ServoEarLeft
cylinder Cy193 radius 5.50 mm height 4.90 mm base ((-47.10 mm),32.62 mm,10.27 mm) axis X join targets ServoEarLeft
cylinder Cy194 radius 2.75 mm height 4.90 mm base ((-47.10 mm),27.12 mm,10.27 mm) axis X join targets ServoEarLeft
cylinder Cy195 radius 2.40 mm height 3.50 mm base ((-50.10 mm),32.62 mm,10.27 mm) axis X join targets ServoEarLeft
box Bx196 size 22.70 mm 22.80 mm 12.00 mm corner (20.00 mm,(-92.55 mm),(-2.10 mm)) new -> body ServoBrow
box Bx197 size 2.50 mm 32.50 mm 12.00 mm corner (35.80 mm,(-97.40 mm),(-2.10 mm)) join targets ServoBrow
cylinder Cy198 radius 5.50 mm height 4.90 mm base (42.20 mm,(-86.55 mm),3.90 mm) axis X join targets ServoBrow
cylinder Cy199 radius 2.75 mm height 4.90 mm base (42.20 mm,(-81.05 mm),3.90 mm) axis X join targets ServoBrow
cylinder Cy200 radius 2.40 mm height 3.50 mm base (46.60 mm,(-86.55 mm),3.90 mm) axis X join targets ServoBrow
box Bx201 size 22.70 mm 22.80 mm 12.00 mm corner ((-42.70 mm),(-87.15 mm),(-86.70 mm)) new -> body ServoChin
box Bx202 size 2.50 mm 32.50 mm 12.00 mm corner ((-38.30 mm),(-92.00 mm),(-86.70 mm)) join targets ServoChin
cylinder Cy203 radius 5.50 mm height 4.90 mm base ((-47.10 mm),(-81.15 mm),(-80.70 mm)) axis X join targets ServoChin
cylinder Cy204 radius 2.75 mm height 4.90 mm base ((-47.10 mm),(-75.65 mm),(-80.70 mm)) axis X join targets ServoChin
cylinder Cy205 radius 2.40 mm height 3.50 mm base ((-50.10 mm),(-81.15 mm),(-80.70 mm)) axis X join targets ServoChin
box Bx206 size 82.00 mm 2.50 mm 10.00 mm corner ((-41.00 mm),(-116.00 mm),(-23.83 mm)) new -> body LedStrip
box Bx207 size 18.00 mm 1.00 mm 22.50 mm corner ((-9.00 mm),37.00 mm,(-70.00 mm)) new -> body Esp32
box Bx208 size 9.00 mm 3.30 mm 7.30 mm corner ((-4.50 mm),33.80 mm,(-70.00 mm)) join targets Esp32
box Bx209 size 2.54 mm 16.60 mm 20.30 mm corner ((-8.89 mm),37.90 mm,(-68.90 mm)) join targets Esp32
box Bx210 size 2.54 mm 16.60 mm 20.30 mm corner (6.35 mm,37.90 mm,(-68.90 mm)) join targets Esp32
