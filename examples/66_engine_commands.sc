// 66. Engine commands without a statement of their own, used through `engine`, each checked.
//
// Not a part to print: a test bench for the `engine` statement across command families. Each
// section makes a body with an engine command the DSL has no syntax for, and a check measures the
// result against what the command should give.
design EngineCommands units mm
param t = 1.5 mm

// solid.split_face and solid.press_pull: split a block's top in two along x = 20, then raise
// only the right half 3 mm. Only half the top rises: 40 x 20 x 10 + 20 x 20 x 3.
box Block size 40 20 10 corner (0,0,0) -> body Block
engine solid.split_face {"name": "SplitTop", "faces": [[20, 10, 10]], "body": "Block", "plane": {"origin": [20, 0, 0], "normal": [1, 0, 0]}}
engine solid.press_pull {"name": "Raise", "face": [30, 10, 10], "distance": "3 mm"}
check Block volume == 9200 mm3

// surface.offset: copy a cylinder's side 2 mm out as a surface, then thicken it 1 mm into a tube
// between r = 12 and 13 (pi (13^2 - 12^2) 20 = 1570.8, to about 0.01 %). The side is two
// half-faces: pick both.
cylinder Core radius 10 mm height 20 mm base (60, 0, 0) -> body Core
engine surface.offset {"name": "Skin", "faces": [[70, 0, 10], [50, 0, 10]], "body": "Core", "distance": "2 mm", "body_name": "Skin"}
thicken Tube bodies Skin thickness 1 mm new -> body Tube
remove DropSkin bodies Skin
check Tube volume >= 1570.5 mm3
check Tube volume <= 1570.9 mm3

// solid.boundary_fill: the lens where two r = 10 discs 12 apart overlap, 10 thick, as its own
// body; the discs are removed. Lens area 200 acos(0.6) - 6 sqrt(256), so 894.59 mm3; the engine
// integrates the result's volume to about 0.02 %.
cylinder DiscA radius 10 mm height 10 mm base (100, 0, 0) -> body DiscA
cylinder DiscB radius 10 mm height 10 mm base (112, 0, 0) -> body DiscB
engine solid.boundary_fill {"name": "Lens", "tools": ["DiscA", "DiscB"], "cells": [[106, 0, 5]], "operation": "new", "remove_tools": true, "body_name": "Lens"}
check Lens volume >= 894.3 mm3
check Lens volume <= 894.7 mm3

// construct.plane.tangent: a plane touching a rod's side at x = 10; a boss sketched on it,
// extruded 4 mm each way, stands 4 mm out from the rod and sinks 4 mm into it.
cylinder Rod radius 10 mm height 30 mm base (0, 60, 0) -> body Rod
engine construct.plane.tangent {"name": "Flat", "face": {"face": [10, 60, 15]}}
sketch BossSk on Flat {
  circle c = (0,0) radius 3 mm
  fix c
}
extrude Boss from BossSk distance 8 mm symmetric join targets Rod
check Rod size.x == 24 mm

// sketch.edit with sketch.circle.three_point: a circle through three points drawn into a finished
// sketch, then extruded: centre (110, 60), radius 10 (to the engine's 0.01 % here).
sketch Disc on XY {
  point anchor = (0, 0)
  fix anchor
}
engine sketch.edit {"sketch": "Disc"}
engine sketch.circle.three_point {"p0": [100, 60], "p1": [120, 60], "p2": [110, 70]}
engine sketch.constraint.fix {"entities": ["c1"], "fixed": true}
engine sketch.finish {}
extrude Puck from Disc distance 5 mm profiles (110, 60) -> body Puck
check Puck volume >= 1570.6 mm3
check Puck volume <= 1570.9 mm3

// sheet.unfold: a bent tray (as in 64) unfolded flat; its height becomes the thickness.
sheet_rule Steel thickness t bend_radius t k_factor 0.44
box TrayBlank size 60 40 15 corner (0, 120, 0) -> body Tray
shell Open faces (30, 140, 15) thickness t body Tray
convert Sheet body Tray face (30, 140, 0) rule Steel bends (30, 120, 0) (30, 160, 0) (0, 140, 0) (60, 140, 0)
engine sheet.unfold {"body": "Tray"}
check Tray size.z == t
