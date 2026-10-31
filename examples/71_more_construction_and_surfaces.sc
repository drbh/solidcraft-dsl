// 71. More construction geometry, surface and solid commands through `engine`, each checked
// (a test bench, as 66, 68, 69 and 70). Needs engine 0.27 (patterns about named axes).
design MoreConstructionAndSurfaces units mm

// The reference: a 40 x 40 plate, top face at z = 5
box Base size 40 40 5 corner (0,0,0) -> body Base

// Construction points from edges and planes, and a plane through three of them: two top edges
// meeting at (40, 0, 5), three faces meeting at (0, 40, 5), a vertical edge crossing the top
// at (0, 0, 5). The plane through them is the plate's top; a disc on it sits on the plate.
engine construct.point.two_edges {"name": "Q1", "a": {"edge": [20, 0, 5]}, "b": {"edge": [40, 20, 5]}}
engine construct.point.three_planes {"name": "Q2", "a": {"face": [20, 20, 5]}, "b": {"face": [0, 20, 2]}, "c": {"face": [20, 40, 2]}}
engine construct.point.edge_plane {"name": "Q3", "edge": {"edge": [0, 0, 2]}, "plane": {"face": [20, 20, 5]}}
engine construct.plane.three_points {"name": "TopByPoints", "a": {"point": [40, 0, 5]}, "b": {"point": [0, 40, 5]}, "c": {"point": [0, 0, 5]}}
sketch OnTopSk on TopByPoints {
  circle c = (20, 20) radius 5 mm
  fix c
}
extrude OnTopX from OnTopSk distance 2 mm -> body OnTop
check OnTop clearance Base <= 0.001 mm
check OnTop size.z == 2 mm

// construct.plane.two_edges: the plane of two top edges, the top again
engine construct.plane.two_edges {"name": "TopByEdges", "a": {"edge": [20, 0, 5]}, "b": {"edge": [0, 20, 5]}}
sketch OnTop2Sk on TopByEdges {
  circle c = (10, 30) radius 3 mm
  fix c
}
extrude OnTop2X from OnTop2Sk distance 1 mm -> body OnTop2
check OnTop2 clearance Base <= 0.001 mm

// construct.plane.along_path (square to the front top edge halfway, so x = 20) and
// construct.plane.perpendicular (holding that edge, square to the top, so y = 0): a 1 mm thick
// disc on each is thin across x and across y respectively.
engine construct.point.along_path {"name": "EdgeMid", "path": {"edge": [20, 0, 5]}, "t": 0.5}
engine construct.plane.along_path {"name": "Across", "path": {"edge": [20, 0, 5]}, "t": 0.5}
sketch AcrossSk on Across {
  circle c = (0, 0) radius 4 mm
  fix c
}
extrude AcrossX from AcrossSk distance 1 mm symmetric -> body AcrossDisc
check AcrossDisc size.x == 1 mm
engine construct.plane.perpendicular {"name": "AlongEdge", "plane": {"face": [20, 20, 5]}, "line": {"edge": [20, 0, 5]}}
sketch FrontSk on AlongEdge {
  circle c = (0, 0) radius 4 mm
  fix c
}
extrude FrontX from FrontSk distance 1 mm symmetric -> body FrontDisc
check FrontDisc size.y == 1 mm

// construct.axis.through_cylinder and construct.axis.two_points, each driving a pattern of two:
// a pin 10 from a post's axis, on a bar through it, gets its copy 20 away (the pair spans 22);
// a peg 1.5 from the line through two of the plate's top corners (x = 0, z = 5) gets its copy
// turned under that edge, poking 3.5 out of the plate's left side (40 + 3.5 wide).
cylinder Post radius 3 mm height 6 mm base (100, 20, 0) -> body Post
engine construct.axis.through_cylinder {"name": "PostAxis", "face": {"face": [103, 20, 3]}}
cylinder PostBar radius 1 mm height 20 mm base (90, 20, 3 mm) axis X -> body PostPins
cylinder PostPin radius 1 mm height 6 mm base (110, 20, 0) join targets PostPins
pattern circular PostPair features PostPin axis PostAxis count 2 angle 360 deg
check PostPins size.x == 22 mm
engine construct.axis.two_points {"name": "LeftEdge", "a": {"vertex": [0, 0, 5]}, "b": {"vertex": [0, 40, 5]}}
cylinder SidePeg radius 2 mm height 4 mm base (1.5 mm, 20, 5 mm) join targets Base
pattern circular SidePegs features SidePeg axis LeftEdge count 2 angle 360 deg
check Base size.x == 43.5 mm

// solid.cone (the engine's own primitive): r 6, h 9: pi 36 9 / 3 = 339.29
engine solid.cone {"name": "Spike", "radius": "6 mm", "height": "9 mm", "base": [150, 0, 0], "body_name": "Spike"}
check Spike volume >= 339.25 mm3
check Spike volume <= 339.33 mm3

// solid.offset_face: a 10 mm cube's top face moved out 2 mm
box Cube size 10 10 10 corner (150, 30, 0) -> body Cube
engine solid.offset_face {"name": "Grow", "faces": [[155, 35, 10]], "distance": "2 mm", "body": "Cube"}
check Cube size.z == 12 mm
check Cube volume == 1200 mm3

// solid.bounding_solid: the box round the cube, 1 mm clear
engine solid.bounding_solid {"name": "Envelope", "bodies": ["Cube"], "margin": "1 mm", "body_name": "CubeBox"}
check CubeBox volume == 2016 mm3

// surface.boundary: a flat patch of a four-line loop, 20 x 10, thickened 1 mm
sketch Frame on XY offset 50 mm {
  line a = (200, 0) (220, 0)
  line b = (220, 0) (220, 10)
  line c = (220, 10) (200, 10)
  line d = (200, 10) (200, 0)
  fix a b c d
}
engine surface.boundary {"name": "Patch", "sketch": "Frame", "curves": ["l1", "l2", "l3", "l4"], "body_name": "PatchSkin"}
thicken PatchPlate bodies PatchSkin thickness 1 mm new -> body PatchPlate
remove DropPatchSkin bodies PatchSkin
check PatchPlate volume == 200 mm3

// surface.untrim: a plate's top face with a hole, copied out without the hole, thickened 1 mm
box Holed size 20 20 3 corner (250, 0, 0) -> body Holed
cylinder HoleCut radius 4 mm height 5 mm base (260, 10, -1 mm) cut targets Holed
engine surface.untrim {"name": "Whole", "face": {"body": "Holed", "point": [252, 2, 3]}, "holes": "all", "body_name": "WholeSkin"}
thicken WholePlate bodies WholeSkin thickness 1 mm new -> body WholePlate
remove DropWholeSkin bodies WholeSkin
check WholePlate volume == 400 mm3

// surface.unstitch: a box's top face taken out as its own surface; the rest stays a surface body
box Opened size 10 10 10 corner (300, 0, 0) -> body Opened
engine surface.unstitch {"name": "TakeTop", "body": "Opened", "faces": [[305, 5, 10]]}
check Opened size.z == 10 mm
