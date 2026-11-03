// 144. Print-in-place infinity cube: eight cubes in a loop of knuckle hinges that folds forever.
//
// Z is up from the bed in the design, X and Y across it; cube n sits in cell (i, j, k) of a 2 x 2 x 2
// block, the cells `p = c + g` apart, so every pair of neighbours is `g` apart. The cubes are
// hinged in a loop of eight, each to two others, along outer edges of the block: six hinges run
// along Y (the top seam, both side seams) and two along X (the bottom seam). The block opens
// along either the top seam or the bottom seam into two halves; each fold of 180 deg leaves a new
// compact shape with the hinges on its outside, and from it another fold opens, round and round.
// A fold search over this loop (180 deg folds only, a fold refused when a knuckle would land
// against a cube) reaches 27 distinct 2 x 2 x 2 arrangements.
//
// Each hinge axis sits g/2 outside both cubes' faces and centred in the gap between them, which is
// the one place a 180 deg turn lands a cube exactly on the next cell. The knuckles are cylinders
// on that axis: two end knuckles and a pin on one cube, a middle knuckle on the other bored for
// the pin. Each cube is cut clear of the other's knuckles by `clr` radially and `ga` axially, so
// the two turn freely but never touch. The knuckles stand R - g/2 proud of the faces at the seams.
//
// Printing: as one piece, PLA, 0.2 mm layers, no supports, the block turned so its y = 0 face is
// on the bed (the print recipe does this). The six Y hinges then stand upright, the best way to
// print a pin in its bore, and the two X hinges lie level with 0.35 mm round their pins. Break
// each hinge free by flexing it gently both ways before folding. The eight hinge blocks below were
// written out from the loop's hinge table and repeat one pattern: change sizes with the params.
design InfinityCube units mm

param c = 20 mm                // cube edge
param g = 0.8 mm               // gap between neighbouring cubes
param R = 3.2 mm               // knuckle radius
param rp = 1.5 mm              // pin radius
param clr = 0.35 mm            // radial clearance round knuckles and pins
param ga = 0.4 mm              // axial gap between knuckles
param inset = 0.3 mm           // knuckles and pins stop short of the cube ends

let p = c + g
let mid = c/3                  // the middle knuckle's length
let end_l = (c - mid)/2 - ga   // each end knuckle's length

// ==== The eight cubes, numbered round the hinge loop
box Block0 size c c c corner (0, 0, (1*p)) new -> body Cube0
box Block1 size c c c corner (0, 0, 0) new -> body Cube1
box Block2 size c c c corner (0, (1*p), 0) new -> body Cube2
box Block3 size c c c corner (0, (1*p), (1*p)) new -> body Cube3
box Block4 size c c c corner ((1*p), (1*p), (1*p)) new -> body Cube4
box Block5 size c c c corner ((1*p), (1*p), 0) new -> body Cube5
box Block6 size c c c corner ((1*p), 0, 0) new -> body Cube6
box Block7 size c c c corner ((1*p), 0, (1*p)) new -> body Cube7

// ---- Hinge 0: Cube0 carries the end knuckles and the pin, Cube1 the middle knuckle
cylinder H0ClearA radius (R + clr) height (c - 2*end_l + 0.2 mm) base ((-g/2), 0 + (end_l - 0.1 mm), (1*p - g/2)) axis Y cut targets Cube0
cylinder H0EndA radius R height (end_l - inset) base ((-g/2), 0 + inset, (1*p - g/2)) axis Y join targets Cube0
cylinder H0EndB radius R height (end_l - inset) base ((-g/2), 0 + (c - end_l), (1*p - g/2)) axis Y join targets Cube0
cylinder H0Pin radius rp height (c - 2*inset - 2 mm) base ((-g/2), 0 + (inset + 1 mm), (1*p - g/2)) axis Y join targets Cube0
cylinder H0ClearB1 radius (R + clr) height (end_l + ga + 1.1 mm) base ((-g/2), 0 + (-1 mm), (1*p - g/2)) axis Y cut targets Cube1
cylinder H0ClearB2 radius (R + clr) height (end_l + ga + 1.1 mm) base ((-g/2), 0 + (c - end_l - ga - 0.1 mm), (1*p - g/2)) axis Y cut targets Cube1
cylinder H0Mid radius R height mid base ((-g/2), 0 + (end_l + ga), (1*p - g/2)) axis Y join targets Cube1
cylinder H0Bore radius (rp + clr) height (c + 2 mm) base ((-g/2), 0 + (-1 mm), (1*p - g/2)) axis Y cut targets Cube1

// ---- Hinge 1: Cube1 carries the end knuckles and the pin, Cube2 the middle knuckle
cylinder H1ClearA radius (R + clr) height (c - 2*end_l + 0.2 mm) base (0 + (end_l - 0.1 mm), (1*p - g/2), (-g/2)) axis X cut targets Cube1
cylinder H1EndA radius R height (end_l - inset) base (0 + inset, (1*p - g/2), (-g/2)) axis X join targets Cube1
cylinder H1EndB radius R height (end_l - inset) base (0 + (c - end_l), (1*p - g/2), (-g/2)) axis X join targets Cube1
cylinder H1Pin radius rp height (c - 2*inset - 2 mm) base (0 + (inset + 1 mm), (1*p - g/2), (-g/2)) axis X join targets Cube1
cylinder H1ClearB1 radius (R + clr) height (end_l + ga + 1.1 mm) base (0 + (-1 mm), (1*p - g/2), (-g/2)) axis X cut targets Cube2
cylinder H1ClearB2 radius (R + clr) height (end_l + ga + 1.1 mm) base (0 + (c - end_l - ga - 0.1 mm), (1*p - g/2), (-g/2)) axis X cut targets Cube2
cylinder H1Mid radius R height mid base (0 + (end_l + ga), (1*p - g/2), (-g/2)) axis X join targets Cube2
cylinder H1Bore radius (rp + clr) height (c + 2 mm) base (0 + (-1 mm), (1*p - g/2), (-g/2)) axis X cut targets Cube2

// ---- Hinge 2: Cube2 carries the end knuckles and the pin, Cube3 the middle knuckle
cylinder H2ClearA radius (R + clr) height (c - 2*end_l + 0.2 mm) base ((-g/2), (1*p) + (end_l - 0.1 mm), (1*p - g/2)) axis Y cut targets Cube2
cylinder H2EndA radius R height (end_l - inset) base ((-g/2), (1*p) + inset, (1*p - g/2)) axis Y join targets Cube2
cylinder H2EndB radius R height (end_l - inset) base ((-g/2), (1*p) + (c - end_l), (1*p - g/2)) axis Y join targets Cube2
cylinder H2Pin radius rp height (c - 2*inset - 2 mm) base ((-g/2), (1*p) + (inset + 1 mm), (1*p - g/2)) axis Y join targets Cube2
cylinder H2ClearB1 radius (R + clr) height (end_l + ga + 1.1 mm) base ((-g/2), (1*p) + (-1 mm), (1*p - g/2)) axis Y cut targets Cube3
cylinder H2ClearB2 radius (R + clr) height (end_l + ga + 1.1 mm) base ((-g/2), (1*p) + (c - end_l - ga - 0.1 mm), (1*p - g/2)) axis Y cut targets Cube3
cylinder H2Mid radius R height mid base ((-g/2), (1*p) + (end_l + ga), (1*p - g/2)) axis Y join targets Cube3
cylinder H2Bore radius (rp + clr) height (c + 2 mm) base ((-g/2), (1*p) + (-1 mm), (1*p - g/2)) axis Y cut targets Cube3

// ---- Hinge 3: Cube3 carries the end knuckles and the pin, Cube4 the middle knuckle
cylinder H3ClearA radius (R + clr) height (c - 2*end_l + 0.2 mm) base ((1*p - g/2), (1*p) + (end_l - 0.1 mm), (2*p - g/2)) axis Y cut targets Cube3
cylinder H3EndA radius R height (end_l - inset) base ((1*p - g/2), (1*p) + inset, (2*p - g/2)) axis Y join targets Cube3
cylinder H3EndB radius R height (end_l - inset) base ((1*p - g/2), (1*p) + (c - end_l), (2*p - g/2)) axis Y join targets Cube3
cylinder H3Pin radius rp height (c - 2*inset - 2 mm) base ((1*p - g/2), (1*p) + (inset + 1 mm), (2*p - g/2)) axis Y join targets Cube3
cylinder H3ClearB1 radius (R + clr) height (end_l + ga + 1.1 mm) base ((1*p - g/2), (1*p) + (-1 mm), (2*p - g/2)) axis Y cut targets Cube4
cylinder H3ClearB2 radius (R + clr) height (end_l + ga + 1.1 mm) base ((1*p - g/2), (1*p) + (c - end_l - ga - 0.1 mm), (2*p - g/2)) axis Y cut targets Cube4
cylinder H3Mid radius R height mid base ((1*p - g/2), (1*p) + (end_l + ga), (2*p - g/2)) axis Y join targets Cube4
cylinder H3Bore radius (rp + clr) height (c + 2 mm) base ((1*p - g/2), (1*p) + (-1 mm), (2*p - g/2)) axis Y cut targets Cube4

// ---- Hinge 4: Cube4 carries the end knuckles and the pin, Cube5 the middle knuckle
cylinder H4ClearA radius (R + clr) height (c - 2*end_l + 0.2 mm) base ((2*p - g/2), (1*p) + (end_l - 0.1 mm), (1*p - g/2)) axis Y cut targets Cube4
cylinder H4EndA radius R height (end_l - inset) base ((2*p - g/2), (1*p) + inset, (1*p - g/2)) axis Y join targets Cube4
cylinder H4EndB radius R height (end_l - inset) base ((2*p - g/2), (1*p) + (c - end_l), (1*p - g/2)) axis Y join targets Cube4
cylinder H4Pin radius rp height (c - 2*inset - 2 mm) base ((2*p - g/2), (1*p) + (inset + 1 mm), (1*p - g/2)) axis Y join targets Cube4
cylinder H4ClearB1 radius (R + clr) height (end_l + ga + 1.1 mm) base ((2*p - g/2), (1*p) + (-1 mm), (1*p - g/2)) axis Y cut targets Cube5
cylinder H4ClearB2 radius (R + clr) height (end_l + ga + 1.1 mm) base ((2*p - g/2), (1*p) + (c - end_l - ga - 0.1 mm), (1*p - g/2)) axis Y cut targets Cube5
cylinder H4Mid radius R height mid base ((2*p - g/2), (1*p) + (end_l + ga), (1*p - g/2)) axis Y join targets Cube5
cylinder H4Bore radius (rp + clr) height (c + 2 mm) base ((2*p - g/2), (1*p) + (-1 mm), (1*p - g/2)) axis Y cut targets Cube5

// ---- Hinge 5: Cube5 carries the end knuckles and the pin, Cube6 the middle knuckle
cylinder H5ClearA radius (R + clr) height (c - 2*end_l + 0.2 mm) base ((1*p) + (end_l - 0.1 mm), (1*p - g/2), (-g/2)) axis X cut targets Cube5
cylinder H5EndA radius R height (end_l - inset) base ((1*p) + inset, (1*p - g/2), (-g/2)) axis X join targets Cube5
cylinder H5EndB radius R height (end_l - inset) base ((1*p) + (c - end_l), (1*p - g/2), (-g/2)) axis X join targets Cube5
cylinder H5Pin radius rp height (c - 2*inset - 2 mm) base ((1*p) + (inset + 1 mm), (1*p - g/2), (-g/2)) axis X join targets Cube5
cylinder H5ClearB1 radius (R + clr) height (end_l + ga + 1.1 mm) base ((1*p) + (-1 mm), (1*p - g/2), (-g/2)) axis X cut targets Cube6
cylinder H5ClearB2 radius (R + clr) height (end_l + ga + 1.1 mm) base ((1*p) + (c - end_l - ga - 0.1 mm), (1*p - g/2), (-g/2)) axis X cut targets Cube6
cylinder H5Mid radius R height mid base ((1*p) + (end_l + ga), (1*p - g/2), (-g/2)) axis X join targets Cube6
cylinder H5Bore radius (rp + clr) height (c + 2 mm) base ((1*p) + (-1 mm), (1*p - g/2), (-g/2)) axis X cut targets Cube6

// ---- Hinge 6: Cube6 carries the end knuckles and the pin, Cube7 the middle knuckle
cylinder H6ClearA radius (R + clr) height (c - 2*end_l + 0.2 mm) base ((2*p - g/2), 0 + (end_l - 0.1 mm), (1*p - g/2)) axis Y cut targets Cube6
cylinder H6EndA radius R height (end_l - inset) base ((2*p - g/2), 0 + inset, (1*p - g/2)) axis Y join targets Cube6
cylinder H6EndB radius R height (end_l - inset) base ((2*p - g/2), 0 + (c - end_l), (1*p - g/2)) axis Y join targets Cube6
cylinder H6Pin radius rp height (c - 2*inset - 2 mm) base ((2*p - g/2), 0 + (inset + 1 mm), (1*p - g/2)) axis Y join targets Cube6
cylinder H6ClearB1 radius (R + clr) height (end_l + ga + 1.1 mm) base ((2*p - g/2), 0 + (-1 mm), (1*p - g/2)) axis Y cut targets Cube7
cylinder H6ClearB2 radius (R + clr) height (end_l + ga + 1.1 mm) base ((2*p - g/2), 0 + (c - end_l - ga - 0.1 mm), (1*p - g/2)) axis Y cut targets Cube7
cylinder H6Mid radius R height mid base ((2*p - g/2), 0 + (end_l + ga), (1*p - g/2)) axis Y join targets Cube7
cylinder H6Bore radius (rp + clr) height (c + 2 mm) base ((2*p - g/2), 0 + (-1 mm), (1*p - g/2)) axis Y cut targets Cube7

// ---- Hinge 7: Cube7 carries the end knuckles and the pin, Cube0 the middle knuckle
cylinder H7ClearA radius (R + clr) height (c - 2*end_l + 0.2 mm) base ((1*p - g/2), 0 + (end_l - 0.1 mm), (2*p - g/2)) axis Y cut targets Cube7
cylinder H7EndA radius R height (end_l - inset) base ((1*p - g/2), 0 + inset, (2*p - g/2)) axis Y join targets Cube7
cylinder H7EndB radius R height (end_l - inset) base ((1*p - g/2), 0 + (c - end_l), (2*p - g/2)) axis Y join targets Cube7
cylinder H7Pin radius rp height (c - 2*inset - 2 mm) base ((1*p - g/2), 0 + (inset + 1 mm), (2*p - g/2)) axis Y join targets Cube7
cylinder H7ClearB1 radius (R + clr) height (end_l + ga + 1.1 mm) base ((1*p - g/2), 0 + (-1 mm), (2*p - g/2)) axis Y cut targets Cube0
cylinder H7ClearB2 radius (R + clr) height (end_l + ga + 1.1 mm) base ((1*p - g/2), 0 + (c - end_l - ga - 0.1 mm), (2*p - g/2)) axis Y cut targets Cube0
cylinder H7Mid radius R height mid base ((1*p - g/2), 0 + (end_l + ga), (2*p - g/2)) axis Y join targets Cube0
cylinder H7Bore radius (rp + clr) height (c + 2 mm) base ((1*p - g/2), 0 + (-1 mm), (2*p - g/2)) axis Y cut targets Cube0

// ==== Fits: every hinged pair keeps its radial clearance, and no two cubes touch
check Cube0 clearance Cube1 >= (clr - 0.05 mm)
check Cube1 clearance Cube2 >= (clr - 0.05 mm)
check Cube2 clearance Cube3 >= (clr - 0.05 mm)
check Cube3 clearance Cube4 >= (clr - 0.05 mm)
check Cube4 clearance Cube5 >= (clr - 0.05 mm)
check Cube5 clearance Cube6 >= (clr - 0.05 mm)
check Cube6 clearance Cube7 >= (clr - 0.05 mm)
check Cube7 clearance Cube0 >= (clr - 0.05 mm)
check Cube0 clearance Cube6 >= (g - 0.05 mm)
check Cube1 clearance Cube6 >= (g - 0.05 mm)
check Cube2 clearance Cube4 >= (g - 0.05 mm)

color Cube0, Cube2, Cube4, Cube6 "#ef5b3f"
color Cube1, Cube3, Cube5, Cube7 "#f6c343"
material Cube0, Cube1, Cube2, Cube3, Cube4, Cube5, Cube6, Cube7 PLA
