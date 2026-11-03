// 121. Bistable compliant clicker: one printed piece that snaps between two stable states, no hinge.
//
// Z is up from the bed, the clicker is centred on the origin and the shuttle moves along Y. A
// rigid frame holds a shuttle on two pairs of thin arched blades, one pair each side. The blades
// are clamped at both ends and printed bowed toward +Y by `rise`. Push the shuttle's post toward
// -Y and the blades buckle through and snap into the mirror image, 2*rise further on: the second
// stable state. Push it back and it snaps home. Two blades per side, `pair` apart, keep the
// shuttle from rotating.
//
// Bistability: a clamped, pre-curved double blade snaps through when rise/blade_t is above about
// 2.3 (Qiu, Lang and Slocum, 2004). The defaults give 3.1. Below about 2.5 it may only spring
// back; well above 4 the snap is hard and the blades are strained more. The window leaves the
// shuttle win_y - shuttle_w/2 = 8 mm toward -Y, more than its 2*rise = 5 mm of travel.
//
// Printing: flat, one plate, PLA or PETG, 0.2 mm layers, no supports. Print the blades with at
// least two perimeters across their width (blade_t >= 2 x line width) and no infill to speak of.
// The keyring hole is in a lug off the frame's -X end.
design BistableClicker units mm

param frame_l = 64 mm          // along X
param frame_w = 44 mm          // along Y
param wall = 5 mm
param height = 5 mm            // print height; the blades' in-plane stiffness
param shuttle_l = 12 mm
param shuttle_w = 14 mm
param blade_t = 0.8 mm
param rise = 2.5 mm            // how far the blades bow in the printed state
param pair = 8 mm              // spacing of the two blades on each side
param post_h = 8 mm            // finger post on the shuttle, above the frame
param ring_d = 6 mm

let win_x = frame_l/2 - wall
let win_y = frame_w/2 - wall
let sx = shuttle_l/2
let bx = (win_x + sx)/2          // blade mid-span
let travel = 2*rise

// Frame: a rectangle with a window, rounded outside
sketch FrameSk on XY {
  rect rounded o = (-frame_l/2, -frame_w/2) (frame_l/2, frame_w/2) radius 6
  rect w = (-win_x, -win_y) (win_x, win_y)
  fix o w
}
extrude Frame from FrameSk distance height new -> body Clicker
// Keyring lug beyond the -X end, overlapping the frame wall
cylinder RingLug radius (ring_d/2 + 3 mm) height height base (-frame_l/2 - ring_d/2 + 1 mm, 0, 0) axis (0,0,1) join targets Clicker
cylinder RingHole radius (ring_d/2) height (height + 2 mm) base (-frame_l/2 - ring_d/2 + 1 mm, 0, -1 mm) axis (0,0,1) cut targets Clicker

// Blades: each a band between two arcs, ends 0.3 mm into the frame and the shuttle
for side from 0 to 1 {
  for j from 0 to 1 {
    sketch Blade{side}{j} on XY {
      line inner_end = ((2*side - 1)*(sx - 0.3 mm), (j - 0.5)*pair - blade_t/2) ((2*side - 1)*(sx - 0.3 mm), (j - 0.5)*pair + blade_t/2)
      arc top = start inner_end.end through ((2*side - 1)*bx, (j - 0.5)*pair + rise + blade_t/2) end ((2*side - 1)*(win_x + 0.3 mm), (j - 0.5)*pair + blade_t/2)
      line outer_end = top.end ((2*side - 1)*(win_x + 0.3 mm), (j - 0.5)*pair - blade_t/2)
      arc bottom = start outer_end.end through ((2*side - 1)*bx, (j - 0.5)*pair + rise - blade_t/2) end inner_end.start
      fix inner_end top outer_end bottom
    }
    extrude BladeEx{side}{j} from Blade{side}{j} distance height join targets Clicker
  }
}

// Shuttle in the middle of the window, joined after the blades it hangs on, with its finger post
box Shuttle size shuttle_l shuttle_w height corner (-sx, -shuttle_w/2, 0) join targets Clicker
cylinder Post radius 4 mm height post_h base (0, 0, height - 0.3 mm) axis (0,0,1) join targets Clicker

color Clicker "#ff7a3d"
material Clicker PLA
