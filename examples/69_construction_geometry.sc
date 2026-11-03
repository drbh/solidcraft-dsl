// 69. Construction planes, axes and points, and a few more commands, through `engine` (a test
// bench, as 66 and 68). Each construction is checked by what is built on it: a disc sketched on a
// plane must stand where the plane is (clearances to a base plate), and a pattern about an axis
// must land where the axis is. Needs engine 0.27 (patterns about named axes).
design ConstructionGeometry units mm

// The reference: a 40 x 40 plate, top face at z = 5, corners at (0,0) and (40,40)
box Base size 40 40 5 corner (0,0,0) -> body Base

// construct.plane.offset: 30 above XY; a 2 mm disc on it clears the plate by 25
engine construct.plane.offset {"name": "Up30", "base": "XY", "offset": "30 mm"}
sketch DiscASk on Up30 {
  circle c = (20, 20) radius 5 mm
  fix c
}
extrude DiscAx from DiscASk distance 2 mm -> body DiscA
check DiscA clearance Base >= 24.999 mm
check DiscA clearance Base <= 25.001 mm

// construct.plane.midplane: halfway between the plate's top (z = 5) and the disc's underside
// (z = 30), so at z = 17.5; a disc 1 mm thick centred on it clears the plate by 12
engine construct.plane.midplane {"name": "Mid", "a": {"face": [20, 20, 5]}, "b": {"face": [20, 20, 30]}}
sketch DiscBSk on Mid {
  circle c = (20, 20) radius 5 mm
  fix c
}
extrude DiscBx from DiscBSk distance 1 mm symmetric -> body DiscB
check DiscB clearance Base >= 11.999 mm
check DiscB clearance Base <= 12.001 mm

// construct.point.vertex x3 and construct.plane.three_points: points at the plate's three bottom
// corners make the plane z = 0 (the engine picks points by where they are, {"point": [x,y,z]});
// a disc extruded 2 mm down from it touches the plate
engine construct.point.vertex {"name": "C1", "point": {"vertex": [0, 0, 0]}}
engine construct.point.vertex {"name": "C2", "point": {"vertex": [40, 0, 0]}}
engine construct.point.vertex {"name": "C3", "point": {"vertex": [0, 40, 0]}}
engine construct.plane.three_points {"name": "Floor", "a": {"point": [0, 0, 0]}, "b": {"point": [40, 0, 0]}, "c": {"point": [0, 40, 0]}}
sketch FootSk on Floor {
  circle c = (20, 20) radius 5 mm
  fix c
}
extrude FootX from FootSk distance 2 mm negative -> body Foot
check Foot clearance Base <= 0.001 mm
check Foot size.z == 2 mm

// construct.axis.edge: the plate's front top edge (y = 0, z = 5), as an axis along X. A peg on
// a short hub lying on that line, patterned 4 times about it, joins the hub: the set spans 3 mm
// either side of the axis in both y and z.
cylinder Hub radius 1 mm height 4 mm base (48, 0, 5 mm) axis X -> body Hub
cylinder Peg radius 0.5 mm height 3 mm base (50, 0, 5 mm) join targets Hub
engine construct.axis.edge {"name": "FrontEdge", "edge": {"edge": [20, 0, 5]}}
pattern circular PegRing features Peg axis FrontEdge count 4 angle 360 deg
check Hub size.y == 6 mm
check Hub size.z == 6 mm

// construct.point.center and construct.axis.normal_to_face, on a second plate: the centre of a
// boss's top edge at (20, 80), and the axis square to the plate's top through it; a pin 12 from
// it, patterned in 2, lands on the far side, so the plate keeps its 40 mm width.
box Deck size 40 40 5 corner (0, 60, 0) -> body Deck
cylinder Boss radius 3 mm height 3 mm base (20, 80, 5 mm) join targets Deck
engine construct.point.center {"name": "BossMid", "of": {"edge": [23, 80, 8]}}
engine construct.axis.normal_to_face {"name": "Up", "face": {"face": [10, 70, 5]}, "point": {"point": [20, 80, 8]}}
cylinder Pin radius 1 mm height 4 mm base (32, 80, 5 mm) join targets Deck
pattern circular PinPair features Pin axis Up count 2 angle 360 deg
check Deck size.x == 40 mm
let deck_v = 40 mm*40 mm*5 mm + pi*(3 mm)^2*3 mm + 2*pi*(1 mm)^2*4 mm
check Deck volume >= (deck_v - 0.05 mm^3)
check Deck volume <= (deck_v + 0.05 mm^3)

// sheet.fold: a flat 60 x 30 aluminium plate folded up 90 deg along the line y = 20
sheet_rule Alu thickness 1 mm bend_radius 1 mm k_factor 0.44
sketch Blank on XY offset 60 mm {
  rect r = (0, 0) (60, 30)
  fix r
}
flange Plate from Blank profiles (2 mm, 2 mm) rule Alu -> body Folded
engine sheet.fold {"name": "Fold", "points": [[0, 20, 60], [60, 20, 60]], "angle": "90 deg", "fixed": [30, 5, 60]}
check Folded size.y <= 22 mm
check Folded size.z >= 10 mm

// surface.sweep: an open quarter arc swept 50 mm along Y, thickened 1 mm into a curved gutter
sketch GutterSection on XZ {
  arc a = center (100, 0) start (110, 0) end (100, -10)
  fix a
}
sketch GutterPath on XY {
  line p = (100, 0) (100, 50)
  fix p
}
engine surface.sweep {"name": "GutterSkin", "sketch": "GutterSection", "curves": ["a1"], "path_sketch": "GutterPath", "path": ["l1"], "body_name": "GutterSkin"}
thicken Gutter bodies GutterSkin thickness 1 mm new -> body Gutter
remove DropGutterSkin bodies GutterSkin
check Gutter size.y == 50 mm

// solid.replace_face: a 10 mm block's top face moved onto the plane z = 25
box Riser size 10 10 10 corner (150, 0, 0) -> body Riser
engine solid.replace_face {"name": "Raise", "faces": [[155, 5, 10]], "target": {"origin": [0, 0, 25], "normal": [0, 0, 1]}, "body": "Riser"}
check Riser size.z == 25 mm
check Riser volume == 2500 mm3
