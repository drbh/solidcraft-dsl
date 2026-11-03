// 129. Kaleidoscope: three mirror strips in a printed tube, a peep-hole eyepiece and a turning bead cell.
//
// Z runs along the tube, from the eyepiece end (z = 0) to the object end (z = tube_l); X and Y
// across it. The tube's bore is an equilateral triangle that holds three mirror strips face in,
// so the three images repeat into the classic six-fold pattern. The eyepiece is a cup pressed on
// the near end with a 6 mm peep hole. At the far end the object cell, a cup with slit skirt,
// snaps over a V groove round the tube on an internal bead, so it turns freely but can't come
// off. Inside the cell, from the tube end out: a glued retainer ring, an inner clear disc, a
// spacer ring that makes the bead chamber, an outer clear disc, and the cell's window lip.
//
// BOM:
//   3 x mirror strips, 25 x 150 mm, 1 mm acrylic mirror (or 1 mm craft glass mirror), face in
//   2 x clear discs, 1.5 mm acrylic, diameter 2*disc_r (36.2 mm) (or print them in clear PETG); frost the
//     outer one with fine sandpaper so the cell is lit evenly
//   beads, sequins, short glass rods to fill about a third of the 6 mm chamber
//   a drop of CA glue for the retainer ring
//
// Print (PLA, 0.2 mm, no supports): tube standing on its eyepiece end; eyepiece on its plate;
// cell on its window lip (open end up, so the bead's lower face is a 45 deg chamfer); spacer and
// retainer flat. Assembly: slide the strips into the bore and press on the eyepiece. In the cell,
// lay the outer disc on the window lip, then the spacer, the beads, the inner disc, and glue the
// retainer ring in behind them. Snap the cell over the tube's far end; it turns on the groove.
design Kaleidoscope units mm

param mirror_w = 25 mm
param mirror_l = 150 mm
param tri_clear = 1.5 mm       // added to the triangle side for the strips' thickness and fit
param tube_r = 18 mm
param press = 0.15 mm          // eyepiece fit, per side
param turn = 0.3 mm            // cell running clearance, per side
param wall = 2 mm
param peep_d = 6 mm
param bead_h = 0.7 mm          // how far the cell's snap bead stands in from its bore
param chamber = 6 mm           // depth of the bead chamber
param disc_t = 1.5 mm
param window_r = 14 mm

let tube_l = mirror_l + 2 mm
let tri_side = mirror_w + tri_clear
let groove_z = tube_l - 6 mm
let cell_r = tube_r + turn                   // cell bore
let disc_r = cell_r - 0.2 mm
let stack0 = tube_l + 0.5 mm                 // the stack starts just past the tube's end
let lip_z = stack0 + 2 mm + 2*disc_t + chamber
let cell_bot = groove_z - 4 mm

// ==== Tube with its triangular mirror bore and the V groove the cell snaps into
cylinder TubeBody radius tube_r height tube_l base (0,0,0) axis (0,0,1) new -> body Tube
sketch Bore on XY offset (-1 mm) {
  polygon tri = (0,0) radius (tri_side/sqrt(3)) sides 3
  fix tri
}
extrude BoreCut from Bore distance (tube_l + 2 mm) cut targets Tube
sketch GrooveSk on XZ {
  polyline closed g = (tube_r - 1.2 mm, -groove_z) (tube_r + 0.5 mm, -(groove_z - 1.7 mm)) (tube_r + 0.5 mm, -(groove_z + 1.7 mm))
  fix g
}
revolve Groove from GrooveSk axis Z cut targets Tube

// ==== Eyepiece: a cup pressed on the near end, a 6 mm peep hole in its plate
cylinder EyeBody radius (tube_r + press + wall) height (10 mm + wall) base (0,0,-wall) axis (0,0,1) new -> body Eyepiece
cylinder EyeBore radius (tube_r + press) height 11 mm base (0,0,0) axis (0,0,1) cut targets Eyepiece
cylinder Peep radius (peep_d/2) height (wall + 2 mm) base (0,0,-wall - 1 mm) axis (0,0,1) cut targets Eyepiece

// ==== Object cell: a cup over the far end, with an inner snap bead and a window lip
cylinder CellBody radius (cell_r + wall) height (lip_z + wall - cell_bot) base (0,0,cell_bot) axis (0,0,1) new -> body Cell
cylinder CellBore radius cell_r height (lip_z - cell_bot + 1 mm) base (0,0,cell_bot - 1 mm) axis (0,0,1) cut targets Cell
cylinder Window radius window_r height (wall + 2 mm) base (0,0,lip_z - 1 mm) axis (0,0,1) cut targets Cell
sketch BeadSk on XZ {
  polyline closed b = (cell_r + 0.3 mm, -(groove_z - bead_h - 0.3 mm)) (cell_r - bead_h, -groove_z) (cell_r + 0.3 mm, -(groove_z + bead_h + 0.3 mm))
  fix b
}
revolve Bead from BeadSk axis Z join targets Cell
// four slits in the skirt let it spring over the tube's lip as the bead snaps into the groove
box SlitY size 1.5 mm (2*wall + 2 mm) 8 mm corner (-0.75 mm, cell_r - 1 mm, cell_bot - 1 mm) cut targets Cell
box SlitX size (2*wall + 2 mm) 1.5 mm 8 mm corner (cell_r - 1 mm, -0.75 mm, cell_bot - 1 mm) cut targets Cell
box SlitNY size 1.5 mm (2*wall + 2 mm) 8 mm corner (-0.75 mm, -cell_r - wall - 1 mm, cell_bot - 1 mm) cut targets Cell
box SlitNX size (2*wall + 2 mm) 1.5 mm 8 mm corner (-cell_r - wall - 1 mm, -0.75 mm, cell_bot - 1 mm) cut targets Cell

// ==== The cell's stack, tube end outward
cylinder RetainerBody radius disc_r height 2 mm base (0,0,stack0) axis (0,0,1) new -> body Retainer
cylinder RetainerBore radius (window_r) height 4 mm base (0,0,stack0 - 1 mm) axis (0,0,1) cut targets Retainer
cylinder InnerDiscBody radius disc_r height disc_t base (0,0,stack0 + 2 mm) axis (0,0,1) new -> body InnerDisc
cylinder SpacerBody radius disc_r height chamber base (0,0,stack0 + 2 mm + disc_t) axis (0,0,1) new -> body Spacer
cylinder SpacerBore radius window_r height (chamber + 2 mm) base (0,0,stack0 + 2 mm + disc_t - 1 mm) axis (0,0,1) cut targets Spacer
cylinder OuterDiscBody radius disc_r height disc_t base (0,0,stack0 + 2 mm + disc_t + chamber) axis (0,0,1) new -> body OuterDisc

// The cell turns: it must clear the tube everywhere, bead in groove included
check Cell clearance Tube >= 0.25 mm

color Tube "#2f6f8f"
color Eyepiece, Cell "#e0a84a"
color Retainer, Spacer "#d9d4c7"
color InnerDisc, OuterDisc "#cfe8f2"
material Tube, Eyepiece, Cell, Retainer, Spacer PLA
// InnerDisc and OuterDisc are 1.5 mm clear acrylic (no acrylic in the engine's material library)
