// 72. More sketch commands through `engine` inside sketches (a test bench, as 66 and 68-71).
// Sketches are extruded 1 mm, so volume is area (to the engine's 0.01 % integration where the
// outline is curved); placement is checked by clearances. Needs engine 0.27 (the arc slot's
// `sweep`).
design MoreSketchCommands units mm

// sketch.ellipse: semi-axes 10 and 5: pi 50 = 157.08
sketch EllipseSk on XY {
  engine sketch.ellipse {"center": [0, 0], "major": [10, 0], "minor_radius": 5}
}
extrude EllipseX from EllipseSk distance 1 mm profiles (0, 0) -> body Ellipse
check Ellipse volume >= 157.06 mm3
check Ellipse volume <= 157.10 mm3

// sketch.conic (rho 0.5, a parabola) closed by a line: 2/3 of the triangle under its apex,
// 2/3 x 20 x 10 / 2 = 66.67
sketch ConicSk on XY {
  engine sketch.conic {"start": [20, 0], "end": [40, 0], "apex": [30, 10], "rho": 0.5}
  engine sketch.line {"points": [[40, 0], [20, 0]]}
}
extrude ConicX from ConicSk distance 1 mm profiles (30, 2) -> body Conic
check Conic volume >= 66.65 mm3
check Conic volume <= 66.68 mm3

// sketch.slot.arc_center: a 4 mm slot on a quarter arc of radius 10: (pi/2 10) 4 + pi 2^2 = 75.40
sketch ArcSlotSk on XY {
  engine sketch.slot.arc_center {"center": [60, 0], "start": [70, 0], "sweep": 90, "width": 4}
}
extrude ArcSlotX from ArcSlotSk distance 1 mm profiles (67.07, 7.07) -> body ArcSlot
check ArcSlot volume >= 75.38 mm3
check ArcSlot volume <= 75.42 mm3

// sketch.slot.arc_three_point: the same slot through three points on its centre arc
sketch ArcSlot3Sk on XY {
  engine sketch.slot.arc_three_point {"start": [100, 0], "through": [97.071, 7.071], "end": [90, 10], "width": 4}
}
extrude ArcSlot3X from ArcSlot3Sk distance 1 mm profiles (97.071, 7.071) -> body ArcSlot3
check ArcSlot3 volume >= 75.38 mm3
check ArcSlot3 volume <= 75.42 mm3

// sketch.circle.three_tangent: the circle inside a 30-40-50 right triangle, r = 10: pi 100
sketch InSk on XY {
  engine sketch.line {"points": [[150, 0], [190, 0], [150, 30]], "closed": true}
  engine sketch.circle.three_tangent {"curves": ["l1", "l2", "l3"], "near": [160, 10]}
}
extrude InX from InSk distance 1 mm profiles (160, 10) -> body Incircle
check Incircle volume >= 314.12 mm3
check Incircle volume <= 314.19 mm3

// sketch.circle.two_tangent: an r 5 circle in the corner of two lines: pi 25
sketch CornerSk on XY {
  engine sketch.line {"points": [[200, 30], [200, 0], [230, 0]]}
  engine sketch.circle.two_tangent {"curves": ["l1", "l2"], "radius": 5, "near": [206, 6]}
}
extrude CornerX from CornerSk distance 1 mm profiles (205, 5) -> body CornerDisc
check CornerDisc volume >= 78.53 mm3
check CornerDisc volume <= 78.55 mm3

// sketch.line.midpoint and sketch.line: a 20 x 10 rectangle whose bottom edge is drawn from its
// middle: 200
sketch MidSk on XY {
  engine sketch.line.midpoint {"mid": [250, 0], "end": [260, 0]}
  engine sketch.line {"points": [[260, 0], [260, 10], [240, 10], [240, 0]]}
}
extrude MidX from MidSk distance 1 mm profiles (250, 5) -> body MidRect
check MidRect volume == 200 mm3

// sketch.constraint.polygon: a skewed quadrilateral on one fixed 10 mm side becomes a square: 100
sketch SquareSk on XY {
  engine sketch.line {"points": [[280, 0], [290, 0], [291, 9], [279, 11]], "closed": true}
  engine sketch.constraint.fix {"entities": ["l1"], "fixed": true}
  engine sketch.constraint.polygon {"lines": ["l1", "l2", "l3", "l4"]}
}
extrude SquareX from SquareSk distance 1 mm profiles (285, 5) -> body Square
check Square volume >= 99.99 mm3
check Square volume <= 100.01 mm3

// sketch.constraint.equal: an r 3 circle made equal to a fixed r 5 one: each pi 25
sketch EqualSk on XY {
  engine sketch.circle.two_point {"p0": [305, 0], "p1": [315, 0]}
  engine sketch.constraint.fix {"entities": ["c1"], "fixed": true}
  engine sketch.circle.two_point {"p0": [327, 0], "p1": [333, 0]}
  engine sketch.constraint.equal {"a": "c1", "b": "c2"}
}
extrude EqualX from EqualSk distance 1 mm profiles (330, 0) -> body EqualDisc
check EqualDisc volume >= 78.53 mm3
check EqualDisc volume <= 78.55 mm3

// sketch.constraint.tangent: a circle with a fixed centre grows to touch a fixed line 10 below it
sketch TangentSk on XY {
  engine sketch.line {"points": [[350, 0], [380, 0]]}
  engine sketch.constraint.fix {"entities": ["l1"], "fixed": true}
  engine sketch.circle.two_point {"p0": [362, 10], "p1": [368, 10]}
  engine sketch.constraint.fix {"entities": ["c1.center"], "fixed": true}
  engine sketch.constraint.tangent {"a": "l1", "b": "c1"}
}
extrude TangentX from TangentSk distance 1 mm profiles (365, 10) -> body TangentDisc
check TangentDisc volume >= 314.12 mm3
check TangentDisc volume <= 314.19 mm3

// sketch.constraint.symmetry: a circle mirrored about a fixed vertical line 10 to its right; the
// two discs, extruded separately, are 20 apart centre to centre, so 16 apart edge to edge
sketch SymSk on XY {
  engine sketch.circle.two_point {"p0": [398, 0], "p1": [402, 0]}
  engine sketch.constraint.fix {"entities": ["c1"], "fixed": true}
  engine sketch.line {"points": [[410, -10], [410, 10]], "construction": true}
  engine sketch.constraint.fix {"entities": ["l1"], "fixed": true}
  engine sketch.circle.two_point {"p0": [413, 3], "p1": [417, 3]}
  engine sketch.constraint.symmetry {"a": "c1", "b": "c2", "line": "l1"}
}
extrude SymAX from SymSk distance 1 mm profiles (400, 0) -> body SymA
extrude SymBX from SymSk distance 1 mm profiles (420, 0) -> body SymB
check SymA clearance SymB >= 15.999 mm
check SymA clearance SymB <= 16.001 mm

// sketch.move (a copy) and sketch.transform (scaled 2 about its centre): r 3 moved 10 right
// leaves the discs 4 apart; an r 2 circle scaled to r 4 is pi 16
sketch MoveSk on XY {
  engine sketch.circle.two_point {"p0": [447, 0], "p1": [453, 0]}
  engine sketch.move {"entities": ["c1"], "translate": [10, 0], "copy": true}
}
extrude MoveAX from MoveSk distance 1 mm profiles (450, 0) -> body MoveA
extrude MoveBX from MoveSk distance 1 mm profiles (460, 0) -> body MoveB
check MoveA clearance MoveB >= 3.999 mm
check MoveA clearance MoveB <= 4.001 mm
sketch ScaleSk on XY {
  engine sketch.circle.two_point {"p0": [478, 0], "p1": [482, 0]}
  engine sketch.transform {"entities": ["c1"], "factor": 2, "center": [480, 0]}
}
extrude ScaleX from ScaleSk distance 1 mm profiles (480, 0) -> body Scaled
check Scaled volume >= 50.26 mm3
check Scaled volume <= 50.28 mm3

// sketch.extend: a bottom line that stops short of the right side is extended to meet it,
// closing a 25 x 20 rectangle: 500
sketch ExtendSk on XY {
  engine sketch.line {"points": [[525, 0], [525, 20], [500, 20], [500, 0], [515, 0]]}
  engine sketch.extend {"curve": "l4", "at": [515, 0]}
}
extrude ExtendX from ExtendSk distance 1 mm profiles (510, 10) -> body Extended
check Extended volume == 500 mm3

// sketch.intersect: where a cylinder crosses the sketch plane, an r 5 circle: pi 25
cylinder Rod radius 5 mm height 10 mm base (600, 0, -5 mm) -> body Rod
sketch SectionSk on XY {
  engine sketch.intersect {"refs": [{"body": "Rod"}], "projection_link": false}
}
extrude SectionX from SectionSk distance 1 mm profiles (600, 0) new -> body SectionDisc
check SectionDisc volume >= 78.53 mm3
check SectionDisc volume <= 78.55 mm3
