// 62. Hat-channel mounting strip: a steel plate that screws to a wall through two slots, with a
// hat-section rail bent along one long edge (up, out, down) to hang things from, and a Z-shaped
// drip lip bent down along the other.
//
// Modelled with the plate lying on XY. Each bent edge is one path flange: an open chain of lines
// drawn across the edge, on a plane at the strip's middle, from the edge out. On engines before
// 0.27 the compiler writes each as edge flanges, one per line (see docs/shape-features.md); the
// result is the same part. Change `rail_h`, `rail_w` or `lip` and the lines, so the flanges,
// follow.
design HatChannelMount units mm
param L = 120 mm
param W = 40 mm
param t = 1.5 mm
param rail_h = 10 mm
param rail_w = 15 mm
param lip = 8 mm
param drip = 6 mm
param screw_d = 4.5 mm
param slot_len = 8 mm
sheet_rule Steel thickness t bend_radius t k_factor 0.44

sketch Blank on XY {
  rect r = (0,0) (L, W)
  slot s1 = centers (20 mm, W/2) (20 mm + slot_len, W/2) width screw_d
  slot s2 = centers (L - 20 mm - slot_len, W/2) (L - 20 mm, W/2) width screw_d
  fix r.bottom r.right r.top r.left s1.line1 s1.line2 s2.line1 s2.line2
}
flange Plate from Blank profiles (2 mm, 2 mm) rule Steel -> body Mount

// Hat rail along y = W, drawn on the YZ plane through the middle (sketch coordinates are (y, z))
sketch Hat on YZ offset (L/2) {
  line up = (W, 0) (W, rail_h)
  line out = (W, rail_h) (W + rail_w, rail_h)
  line down = (W + rail_w, rail_h) (W + rail_w, 0)
  fix up out down
}
path_flange Rail edge (L/2, W, 0) path Hat.up, Hat.out, Hat.down body Mount

// Drip lip along y = 0: down, then out away from the plate
sketch Drip on YZ offset (L/2) {
  line down = (0, 0) (0, -lip)
  line out = (0, -lip) (-drip, -lip)
  fix down out
}
path_flange DripLip edge (L/2, 0, 0) path Drip.down, Drip.out body Mount

// Each line runs to its flange's outer face from the bend's virtual sharp, so the far wall of
// the hat and the end of the drip lip each sit one thickness inside their line's end.
check Mount size.x == L
check Mount size.y == (W + rail_w + drip - 2*t)
check Mount size.z == (rail_h + lip - t)
