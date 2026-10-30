// 41. Pegboard screwdriver holder for standard 1/4 in holes on a 1 in pitch.
//
// X runs along the board, Y points out of the board (its front face is y = 0, the back face
// y = -board_t), Z is up. A back plate sits flat on the board. One column of pegs goes into the
// holes: at the top a hook bar that passes through the board and bends down behind it, and
// peg_rows holes lower a short straight peg that stops inside the hole and keeps the holder from
// swinging. To hang it, tilt the top toward the board, feed the hook through, then swing it down
// until the peg drops into its hole. The hook and peg are hook_d square with chamfered corners
// (an octagon about 5.9 mm across the corners) so they pass the 6.35 mm hole. An arm along the
// top carries a row of screwdriver holes running out from the board, with a 45 deg gusset.
//
// Print on its side, the x = 0 face on the bed, no supports. The plate, arm, gusset, hook and peg
// are all straight extrusions along X from that face, so their outlines (including the hook's bend)
// lie flat on the bed. The pegs sit at x = 0, on the bed, which is why the column is at the plate's
// edge rather than its centre (a centred hook would hang in the air). The chamfers on the pegs are
// 45 deg. The screwdriver holes run along Z, which is horizontal when printed, so they are
// teardrops pointing +X (up on the bed).
design PegboardHolder units mm

// Measure these on the board
param board_t = 6.35 mm
param hole_pitch = 1 in
param peg_rows = 2
param hook_d = 5.5 mm
param hook_gap = 0.6 mm
param hook_leg = 9 mm

// Holder
param width = 20 mm
param plate_t = 5 mm
param height = 75 mm
param arm_t = 10 mm
param tool_d = 8 mm
param tool_count = 4
param tool_pitch = 15 mm
param gusset = 8 mm
param corner_r = 2 mm

let hook_z = height - arm_t/2
let peg_z = hook_z - peg_rows*hole_pitch
let behind = board_t + hook_gap
let arm_len = gusset + tool_count*tool_pitch
let hc = (hook_d - hook_d/(1 + sqrt(2)))/2
let r = tool_d/2

// Back plate, arm along the top and a 45 deg gusset under the arm, as one side profile
sketch BodySk on YZ {
  polyline closed p = (0,0) (plate_t,0) (plate_t,height-arm_t-gusset) (plate_t+gusset,height-arm_t) (plate_t+arm_len,height-arm_t) (plate_t+arm_len,height) (0,height)
  fix p.1 p.2 p.3 p.4 p.5 p.6 p.7
}
extrude Body from BodySk distance width -> body Holder
fillet BodyCorners edges (width/2,plate_t+arm_len,height) (width/2,plate_t+arm_len,height-arm_t) (width/2,0,height) radius corner_r body Holder

// Top hook: bar through the board, leg down behind it
sketch HookSk on YZ {
  polyline closed k = (1 mm,hook_z+hook_d/2) (-behind-hook_d,hook_z+hook_d/2) (-behind-hook_d,hook_z-hook_d/2-hook_leg) (-behind,hook_z-hook_d/2-hook_leg) (-behind,hook_z-hook_d/2) (1 mm,hook_z-hook_d/2)
  fix k.1 k.2 k.3 k.4 k.5 k.6
}
extrude Hook from HookSk distance hook_d join targets Holder
chamfer HookEdges edges (0,-behind/2,hook_z+hook_d/2) (hook_d,-behind/2,hook_z+hook_d/2) (0,-behind/2,hook_z-hook_d/2) (hook_d,-behind/2,hook_z-hook_d/2) (0,-behind-hook_d,hook_z-hook_d/2-hook_leg/2) (hook_d,-behind-hook_d,hook_z-hook_d/2-hook_leg/2) (0,-behind,hook_z-hook_d/2-hook_leg/2) (hook_d,-behind,hook_z-hook_d/2-hook_leg/2) distance hc body Holder

// Lower stabilising peg, stops 0.5 mm short of the back face
let peg_len = board_t - 0.5 mm
box Peg size hook_d (peg_len+1 mm) hook_d corner (0,-peg_len,peg_z-hook_d/2) join targets Holder
chamfer PegEdges edges (0,-peg_len/2,peg_z-hook_d/2) (0,-peg_len/2,peg_z+hook_d/2) (hook_d,-peg_len/2,peg_z-hook_d/2) (hook_d,-peg_len/2,peg_z+hook_d/2) distance hc body Holder

// Screwdriver holes through the arm: teardrops pointing +X, patterned out along Y
let y0 = plate_t + gusset + tool_pitch/2
sketch ToolSk on XY offset (height-arm_t-1 mm) {
  circle h = (width/2,y0) radius r
  fix h
}
extrude ToolHole from ToolSk distance (arm_t+2 mm) cut targets Holder
sketch ToolTipSk on XY offset (height-arm_t-1 mm) {
  polyline closed t = (width/2+r*0.6,y0-r*0.8) (width/2+r*sqrt(2),y0) (width/2+r*0.6,y0+r*0.8)
  fix t.1 t.2 t.3
}
extrude ToolTip from ToolTipSk distance (arm_t+2 mm) cut targets Holder
pattern ToolRow features ToolHole, ToolTip dir (0,1,0) count tool_count spacing tool_pitch
