// 76. Engine 0.28's infinite construction lines and print face, through `engine` (a test bench, as
// 66 and 68-75). A line is checked by mirroring an r 2 disc across it (a symmetry constraint)
// and measuring the gap between the disc and its mirror image; the lines built from other curves
// (parallel, perpendicular, bisector, tangents) are checked by building. Needs engine 0.28.
design ConstructionLines units mm

// sketch.construction_line.vertical at x = 10: a disc at x = 0 mirrors to x = 20, 16 apart
sketch VertSk on XY {
  engine sketch.circle.two_point {"p0": [-2, 0], "p1": [2, 0]}
  engine sketch.constraint.fix {"entities": ["c1"], "fixed": true}
  engine sketch.construction_line.vertical {"at": [10, 0]}
  engine sketch.constraint.fix {"entities": ["l1"], "fixed": true}
  engine sketch.circle.two_point {"p0": [23, 3], "p1": [27, 3]}
  engine sketch.constraint.symmetry {"a": "c1", "b": "c2", "line": "l1"}
}
extrude VertAX from VertSk distance 1 mm profiles (0, 0) -> body VertA
extrude VertBX from VertSk distance 1 mm profiles (20, 0) -> body VertB
check VertA clearance VertB >= 15.999 mm
check VertA clearance VertB <= 16.001 mm

// sketch.construction_line.horizontal at y = 50: a disc at y = 40 mirrors to y = 60, 16 apart
sketch HorSk on XY {
  engine sketch.circle.two_point {"p0": [-2, 40], "p1": [2, 40]}
  engine sketch.constraint.fix {"entities": ["c1"], "fixed": true}
  engine sketch.construction_line.horizontal {"at": [0, 50]}
  engine sketch.constraint.fix {"entities": ["l1"], "fixed": true}
  engine sketch.circle.two_point {"p0": [1, 62], "p1": [5, 62]}
  engine sketch.constraint.symmetry {"a": "c1", "b": "c2", "line": "l1"}
}
extrude HorAX from HorSk distance 1 mm profiles (0, 40) -> body HorA
extrude HorBX from HorSk distance 1 mm profiles (0, 60) -> body HorB
check HorA clearance HorB >= 15.999 mm
check HorA clearance HorB <= 16.001 mm

// sketch.construction_line.angle at 45 deg through (100, 0): a disc at (110, 0) mirrors to
// (100, 10); centres 10 sqrt(2) apart, so 14.142 - 4 = 10.142 between the discs
sketch AngSk on XY {
  engine sketch.circle.two_point {"p0": [108, 0], "p1": [112, 0]}
  engine sketch.constraint.fix {"entities": ["c1"], "fixed": true}
  engine sketch.construction_line.angle {"at": [100, 0], "angle": 45}
  engine sketch.constraint.fix {"entities": ["l1"], "fixed": true}
  engine sketch.circle.two_point {"p0": [96, 12], "p1": [100, 12]}
  engine sketch.constraint.symmetry {"a": "c1", "b": "c2", "line": "l1"}
}
extrude AngAX from AngSk distance 1 mm profiles (110, 0) -> body AngA
extrude AngBX from AngSk distance 1 mm profiles (100, 10) -> body AngB
check AngA clearance AngB >= 10.141 mm
check AngA clearance AngB <= 10.143 mm

// sketch.construction_line (free, through two points): through (150, 0) and (150, 10), a vertical
// line at x = 150 again: a disc at x = 140 mirrors to x = 160, 16 apart
sketch FreeSk on XY {
  engine sketch.circle.two_point {"p0": [138, 0], "p1": [142, 0]}
  engine sketch.constraint.fix {"entities": ["c1"], "fixed": true}
  engine sketch.construction_line {"at": [150, 0], "through": [150, 10]}
  engine sketch.constraint.fix {"entities": ["l1"], "fixed": true}
  engine sketch.circle.two_point {"p0": [163, 2], "p1": [167, 2]}
  engine sketch.constraint.symmetry {"a": "c1", "b": "c2", "line": "l1"}
}
extrude FreeAX from FreeSk distance 1 mm profiles (140, 0) -> body FreeA
extrude FreeBX from FreeSk distance 1 mm profiles (160, 0) -> body FreeB
check FreeA clearance FreeB >= 15.999 mm
check FreeA clearance FreeB <= 16.001 mm

// Lines built from other curves: parallel and perpendicular to a line, the bisector of two
// lines, tangent at a circle's point, and tangent to two circles (outside and across). The
// outline they sit beside is a 20 x 10 rectangle: 200.
sketch RefSk on XY {
  engine sketch.line {"points": [[200, 0], [220, 0], [220, 10], [200, 10]], "closed": true}
  engine sketch.circle.two_point {"p0": [230, 5], "p1": [234, 5]}
  engine sketch.circle.two_point {"p0": [240, 5], "p1": [246, 5]}
  engine sketch.construction_line.parallel {"at": [210, 20], "curve": "l1"}
  engine sketch.construction_line.perpendicular {"at": [210, 20], "curve": "l1"}
  engine sketch.construction_line.bisector {"at": [220, 0], "curve": "l1", "curve2": "l2"}
  engine sketch.construction_line.tangent_at {"curve": "c1", "at": [234, 5]}
  engine sketch.construction_line.tangent_external {"at": [238, 9], "curve": "c1", "curve2": "c2", "near": [238, 9]}
  engine sketch.construction_line.tangent_two {"at": [238, 4], "curve": "c1", "curve2": "c2", "near": [238, 4]}
}
extrude RefX from RefSk distance 1 mm profiles (210, 5) -> body RefRect
check RefRect volume == 200 mm3

// body.print_face: the face a slicer export lays on the bed, here a block's x = 0 end
box Block size 30 10 10 corner (300, 0, 0) -> body Block
engine body.print_face {"body": "Block", "point": [300, 5, 5]}
check Block volume == 3000 mm3
