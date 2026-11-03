// One crate of 147_battery_crate: a block of cell pockets on top, matching pockets underneath for
// the cells of the crate below, a stacking lip round the top and a step round the bottom.
//
// The crate's corner is at the origin, Z up, z0 its base. `cell` picks a row of the cell table:
// 0 AA (14.5 x 50.5), 1 AAA (10.5 x 44.5), 2 18650 (18.6 x 65). Each table column is picked with
// Lagrange weights w0, w1, w2 (one of them 1 and the others 0 for cell = 0, 1 or 2), so the whole
// crate follows the one integer param.
design BatteryCrate units mm

param cell = 0
param cols = 5
param rows = 4
param fit = 0.5 mm             // diametral play of a cell in its pocket
param web = 1.6 mm             // wall between pockets
param wall = 3.5 mm            // outer wall, outside the pocket grid
param seat = 0.6               // share of the cell's length held in the top pocket
param floor_t = 1.2 mm         // between the top and bottom pockets
param lip_h = 2 mm             // stacking lip above the top face
param lip_w = 1.6 mm
param stack_fit = 0.3 mm       // play between the lip and the next crate's step
param z0 = 0 mm

let w0 = (1 - cell)*(2 - cell)/2
let w1 = cell*(2 - cell)
let w2 = cell*(cell - 1)/2
let cell_d = 14.5 mm*w0 + 10.5 mm*w1 + 18.6 mm*w2
let cell_l = 50.5 mm*w0 + 44.5 mm*w1 + 65 mm*w2
let pd = cell_d + fit                 // pocket diameter
let pitch = pd + web
let L = cols*pitch - web + 2*wall
let W = rows*pitch - web + 2*wall
let depth = seat*cell_l               // top pocket depth
let proud = cell_l - depth            // how far a seated cell stands above the top face
let under = proud + 1 mm              // bottom pocket depth: the cells below, plus 1 mm
let H = under + floor_t + depth       // to the top face (the lip stands above it)
let step = lip_w + stack_fit          // the bottom step, inset to fit inside the lip below

// Block and lip
sketch BlockSk on XY offset z0 {
  rect rounded o = (0, 0) (L, W) radius 4 mm
  fix o
}
extrude Block from BlockSk distance H new -> body Crate
sketch LipSk on XY offset (z0 + H - 0.2 mm) {
  rect rounded o = (0, 0) (L, W) radius 4 mm
  rect rounded i = (lip_w, lip_w) (L - lip_w, W - lip_w) radius (4 mm - lip_w)
  fix o i
}
extrude Lip from LipSk distance (lip_h + 0.2 mm) join targets Crate

// Bottom step: a band round the foot, so it drops inside the lip of the crate below
sketch StepSk on XY offset (z0 - 1 mm) {
  rect rounded o = (-1 mm, -1 mm) (L + 1 mm, W + 1 mm) radius 5 mm
  rect rounded i = (step, step) (L - step, W - step) radius (4 mm - step)
  fix o i
}
extrude Step from StepSk distance (lip_h + 1 mm) cut targets Crate

// Pockets: one sketch pattern each for the top and the bottom
sketch TopPocketSk on XY offset (z0 + H - depth) {
  circle k = (wall + pd/2, wall + pd/2) diameter pd
  fix k
  pattern rectangular k count cols spacing pitch count2 rows spacing2 pitch
}
extrude TopPockets from TopPocketSk distance (depth + lip_h + 1 mm) cut targets Crate
sketch UnderPocketSk on XY offset (z0 - 1 mm) {
  circle k = (wall + pd/2, wall + pd/2) diameter pd
  fix k
  pattern rectangular k count cols spacing pitch count2 rows spacing2 pitch
}
extrude UnderPockets from UnderPocketSk distance (under + 1 mm) cut targets Crate

// Finger notches through the lip at both ends, to lift the crate off the one below
cylinder NotchL radius 9 mm height (lip_w + 2 mm) base (-1 mm, W/2, z0 + H + lip_h + 5 mm) axis (1,0,0) cut targets Crate
cylinder NotchR radius 9 mm height (lip_w + 2 mm) base (L - lip_w - 1 mm, W/2, z0 + H + lip_h + 5 mm) axis (1,0,0) cut targets Crate

// Label recess on the front face, 0.6 mm deep, for a printed or paper label
box Label size (L - 24 mm) 0.6 mm 10 mm corner (12 mm, -0.01 mm, z0 + H - 14 mm) cut targets Crate

color Crate "#3a3d42"
material Crate PLA
