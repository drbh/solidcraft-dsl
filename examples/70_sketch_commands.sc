// 70. Sketch commands through `engine` inside sketches, and statements no other example used
// (a test bench, as 66, 68 and 69). Each sketch is extruded 1 mm, so its volume is its area, to
// the engine's 0.01 % integration of curved faces where the outline has arcs.
design SketchCommands units mm

// sketch.rectangle.three_point: a 20 x 10 rectangle turned 36.87 deg (first edge to (16, 12))
sketch TiltedSk on XY {
  engine sketch.rectangle.three_point {"p0": [0, 0], "p1": [16, 12], "p2": [10, 20]}
}
extrude TiltedX from TiltedSk distance 1 mm profiles (8, 10) -> body Tilted
check Tilted volume == 200 mm3

// sketch.polygon.edge: a regular hexagon with a 10 mm side, 3 sqrt(3)/2 10^2 = 259.81
sketch HexSk on XY {
  engine sketch.polygon.edge {"p0": [40, 0], "p1": [50, 0], "sides": 6}
}
extrude HexX from HexSk distance 1 mm profiles (45, 8) -> body Hex
check Hex volume >= 259.8 mm3
check Hex volume <= 259.82 mm3

// sketch.slot.center_point: arc centres 20 apart, 6 wide: 20 x 6 + pi 3^2 = 148.27
sketch SlotSk on XY {
  engine sketch.slot.center_point {"center": [80, 10], "end": [90, 10], "width": 6}
}
extrude SlotX from SlotSk distance 1 mm profiles (80, 10) -> body Slot
check Slot volume >= 148.26 mm3
check Slot volume <= 148.28 mm3

// sketch.circle.two_point and sketch.constraint.concentric: an r 8 circle drawn off-centre is
// pulled onto an r 5 one; the ring between them is pi (8^2 - 5^2) = 122.52
sketch RingSk on XY {
  engine sketch.circle.two_point {"p0": [115, 10], "p1": [125, 10]}
  engine sketch.constraint.fix {"entities": ["c1"], "fixed": true}
  engine sketch.circle.two_point {"p0": [113, 11], "p1": [129, 11]}
  engine sketch.constraint.concentric {"a": "c1", "b": "c2"}
}
extrude RingX from RingSk distance 1 mm profiles (126.5, 10) -> body Ring
check Ring volume >= 122.51 mm3
check Ring volume <= 122.53 mm3

// sketch.line and sketch.arc.tangent: a 20 x 20 square whose right side is a half circle
// leaving the bottom edge tangentially: 400 + pi 10^2 / 2 = 557.08
sketch TongueSk on XY {
  engine sketch.line {"points": [[150, 0], [170, 0]]}
  engine sketch.arc.tangent {"start": "l1.end", "end": [170, 20]}
  engine sketch.line {"points": [[170, 20], [150, 20], [150, 0]]}
}
extrude TongueX from TongueSk distance 1 mm profiles (160, 10) -> body Tongue
check Tongue volume >= 556.97 mm3
check Tongue volume <= 557.19 mm3

// sketch.scale: an r 5 circle scaled 2 times about its centre: pi 10^2 = 314.16
sketch GrownSk on XY {
  engine sketch.circle.two_point {"p0": [205, 10], "p1": [215, 10]}
  engine sketch.scale {"entities": ["c1"], "base": [210, 10], "factor": 2}
}
extrude GrownX from GrownSk distance 1 mm profiles (210, 10) -> body Grown
check Grown volume >= 314.10 mm3
check Grown volume <= 314.22 mm3

// sketch.pattern.circular: an r 2 circle copied 4 times round (250, 10), the four discs
// extruded onto a 24 x 24 x 1 plate (disjoint profiles would make four bodies): 576 + 4 pi 2^2
sketch Holes4 on XY {
  engine sketch.circle.two_point {"p0": [256, 10], "p1": [260, 10]}
  engine sketch.pattern.circular {"entities": ["c1"], "center": [250, 10], "count": 4}
}
box StudPlate size 24 24 1 corner (238, -2, -1) -> body Studs
extrude Holes4X from Holes4 distance 1 mm profiles (258, 10) (250, 18) (242, 10) (250, 2) join targets Studs
check Studs volume >= 626.2 mm3
check Studs volume <= 626.33 mm3

// sketch chamfer (statement): a 20 x 20 square with two 3 mm corners off: 400 - 2 x 4.5 = 391
sketch Cut on XY offset 10 mm {
  polyline closed o = (0, 40) (20, 40) (20, 60) (0, 60)
  fix o.1 o.2 o.3 o.4
  chamfer o.1.end distance 3 mm
  chamfer o.3.end distance 3 mm
}
extrude CutX from Cut distance 1 mm -> body Chamfered
check Chamfered volume == 391 mm3

// project (statement): the outline of the hexagon's top face, drawn on a plane above it and
// extruded as a lid of the same area
sketch HexLidSk on XY offset 5 mm {
  project face (45, 8, 1) body Hex
}
extrude HexLidX from HexLidSk distance 1 mm -> body HexLid
check HexLid volume >= 259.8 mm3
check HexLid volume <= 259.82 mm3

// pipe (statement): a 6 mm tube with a 1 mm wall along a 50 mm line: pi (3^2 - 2^2) 50 = 785.4
sketch PipePath on XZ offset 40 mm {
  line p = (0, 0) (0, -50)
  fix p
}
pipe Tube path PipePath.p diameter 6 mm wall 1 mm new -> body Tube
check Tube volume >= 785.3 mm3
check Tube volume <= 785.5 mm3
check Tube size.z == 50 mm

// coil (statement): 5 turns at a 4 mm pitch on a 20 mm diameter, 1.5 mm section
coil Spring diameter 20 mm turns 5 pitch 4 mm section_size 1.5 mm base (300, 0, 0) new -> body Spring
check Spring size.z >= 20 mm
check Spring size.x >= 20 mm
