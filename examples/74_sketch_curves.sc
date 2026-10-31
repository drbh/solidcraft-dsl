// 74. The remaining sketch commands through `engine`, and the DXF import and chamfer statements
// (a test bench, as 66 and 68-73). Planar sketches are extruded 1 mm, so volume is area (to
// the engine's 0.01 % integration of curves); 3D sketch curves are checked as pipe paths, by the
// pipe's volume. Commands that only make reference curves are checked by building.
design SketchCurves units mm

// sketch.spline.control_point: a cubic on (0,0) (5,10) (15,10) (20,0), closed by a line; it
// peaks at 3/4 of the control height, 7.5
sketch BumpSk on XY {
  engine sketch.spline.control_point {"points": [[0, 0], [5, 10], [15, 10], [20, 0]]}
  engine sketch.line {"points": [[20, 0], [0, 0]]}
}
extrude BumpX from BumpSk distance 1 mm profiles (10, 2) -> body Bump
check Bump size.x == 20 mm
check Bump size.y >= 7.499 mm
check Bump size.y <= 7.501 mm

// sketch.spline.control_point_5: a degree-5 curve on six control points, closed by a line
sketch Bump5Sk on XY {
  engine sketch.spline.control_point_5 {"points": [[30, 0], [32, 6], [36, 10], [44, 10], [48, 6], [50, 0]]}
  engine sketch.line {"points": [[50, 0], [30, 0]]}
}
extrude Bump5X from Bump5Sk distance 1 mm profiles (40, 2) -> body Bump5
check Bump5 size.x == 20 mm

// sketch.equation_curve: x = 10 cos t, y = 10 sin t for t from 0 to pi, closed by a line: a
// half disc, pi 100 / 2 = 157.08
sketch HalfSk on XY {
  engine sketch.equation_curve {"x": "70 + 10 * cos(t)", "y": "10 * sin(t)", "start": 0, "end": "pi"}
  engine sketch.line {"points": [[60, 0], [80, 0]]}
}
extrude HalfX from HalfSk distance 1 mm profiles (70, 3) -> body HalfDisc
check HalfDisc volume >= 157.05 mm3
check HalfDisc volume <= 157.11 mm3

// sketch.blend_curve: a smooth curve from the end of one line to the start of another, closed
// into an outline
sketch BlendSk on XY {
  engine sketch.line {"points": [[100, 0], [110, 0]]}
  engine sketch.line {"points": [[120, 10], [120, 20]]}
  engine sketch.blend_curve {"a": "l1.end", "b": "l2.start"}
  engine sketch.line {"points": [[120, 20], [100, 20], [100, 0]]}
}
extrude BlendX from BlendSk distance 1 mm profiles (105, 15) -> body Blended
check Blended size.x == 20 mm
check Blended size.y == 20 mm

// sketch.centerline: the axis line of a revolved ring; as a centerline it is not part of any
// profile. Ring 5 to 10 from the axis, 10 tall: pi (100 - 25) 10 = 2356.19
sketch RingSk on XZ offset 50 mm {
  line ax = (150, 0) (150, -20)
  rect r = (155, 0) (160, -10)
  fix ax r
  engine sketch.centerline {"curves": ["l1"], "value": true}
}
revolve RingX from RingSk axis ax profiles (157, -5) new -> body Ring
check Ring volume >= 2355.95 mm3
check Ring volume <= 2356.43 mm3

// sketch.construction: a line across a rectangle made construction, so the rectangle is one
// profile again: 20 x 10 = 200, not 100
sketch SplitSk on XY {
  engine sketch.rectangle.two_point {"p0": [200, 0], "p1": [220, 10]}
  engine sketch.line {"points": [[210, 0], [210, 10]]}
  engine sketch.construction {"curves": ["l5"], "value": true}
}
extrude SplitX from SplitSk distance 1 mm profiles (205, 5) -> body Whole
check Whole volume == 200 mm3

// sketch.delete: a circle drawn inside a rectangle is deleted, so the rectangle has no hole: 200
sketch HoleSk on XY {
  engine sketch.rectangle.two_point {"p0": [240, 0], "p1": [260, 10]}
  engine sketch.circle.two_point {"p0": [248, 5], "p1": [252, 5]}
  engine sketch.delete {"entities": ["c1"]}
}
extrude HoleX from HoleSk distance 1 mm profiles (245, 5) -> body NoHole
check NoHole volume == 200 mm3

// sketch.trim.path: a path across a square and a circle on its right edge removes the circle's
// inside half and the square's edge across it: 400 + pi 25 / 2 = 439.27
sketch TrimSk on XY {
  engine sketch.rectangle.two_point {"p0": [280, 0], "p1": [300, 20]}
  engine sketch.circle.two_point {"p0": [295, 10], "p1": [305, 10]}
  engine sketch.trim.path {"points": [[294, 10.5], [300.5, 10.5]]}
}
extrude TrimX from TrimSk distance 1 mm profiles (290, 10) -> body Trimmed
check Trimmed volume >= 439.2 mm3
check Trimmed volume <= 439.33 mm3

// sketch.break: a rectangle's bottom edge broken in two; the outline is unchanged: 200
sketch BrokenSk on XY {
  engine sketch.rectangle.two_point {"p0": [320, 0], "p1": [340, 10]}
  engine sketch.break {"curve": "l1", "points": [[330, 0]]}
}
extrude BrokenX from BrokenSk distance 1 mm profiles (330, 5) -> body Broken
check Broken volume == 200 mm3

// Sketch chamfers with two distances and with an angle (statements): 400 - 3 x 5 / 2 - 4 x 4 / 2
sketch ChamfersSk on XY offset 20 mm {
  polyline closed o = (0, 40) (20, 40) (20, 60) (0, 60)
  fix o.1 o.2 o.3 o.4
  chamfer o.1.end distance 3 mm distance2 5 mm
  chamfer o.3.end distance 4 mm angle 45 deg
}
extrude ChamfersX from ChamfersSk distance 1 mm -> body Chamfers
check Chamfers volume == 384.5 mm3

// sketch.constraint.midpoint: a circle's centre pulled to the middle of a fixed line, so the
// disc sits 10 from a reference block at the line's start
sketch MidSk on XY offset 20 mm {
  engine sketch.line {"points": [[50, 40], [70, 40]], "construction": true}
  engine sketch.constraint.fix {"entities": ["l1"], "fixed": true}
  engine sketch.circle.two_point {"p0": [52, 45], "p1": [56, 45]}
  engine sketch.constraint.midpoint {"point": "c1.center", "line": "l1"}
}
extrude MidX from MidSk distance 1 mm profiles (60, 40) -> body MidDisc
box MidRef size 1 10 1 corner (49, 35, 20) -> body MidRef
check MidDisc clearance MidRef >= 7.999 mm
check MidDisc clearance MidRef <= 8.001 mm

// Dimension commands: a circle's 10 mm diameter dimension (constraint k1, parameter d1) switched to
// a radius, made driven and back, its text moved; the circle keeps its size: pi 25
sketch DimSk on XY offset 20 mm {
  circle c = (100, 50) radius 5 mm
  dim c = 10 mm
  engine sketch.toggle_diameter {"constraint": "k1"}
  engine sketch.toggle_driven {"constraint": "k1", "driven": true}
  engine sketch.toggle_driven {"constraint": "k1", "driven": false}
  engine sketch.dimension_text {"dimension": "k1", "at": [110, 60]}
}
extrude DimX from DimSk distance 1 mm -> body DimDisc
check DimDisc volume >= 78.53 mm3
check DimDisc volume <= 78.55 mm3

// DXF import (statement): a 20 x 20 square drawn in another tool
sketch DxfSk on XY offset 20 mm {
  dxf "74_sketch_curves_art/square.dxf" at (130, 40)
}
extrude DxfX from DxfSk distance 1 mm -> body DxfSquare
check DxfSquare volume == 400 mm3

// 3D sketch: line3d, arc3d, point3d and spline3d drawn in space (wires w1, w2, w3 and point q1),
// move3d, and the curves as pipe paths.
// A 30 mm straight pipe of 4 mm diameter is pi 4 30 = 376.99; a quarter arc of radius 20 is
// pi 4 (pi 20 / 2) = 394.78.
sketch Wire on XY {
  point o = (0, 0)
  fix o
  engine sketch.options {"sketch": "Wire", "sketch_3d": true}
  engine sketch.line3d {"points": [[0, 100, 0], [0, 100, 20]]}
  engine sketch.move3d {"wire": "w1", "index": 1, "to": [0, 100, 30]}
  engine sketch.arc3d {"start": [40, 100, 0], "through": [54.142, 100, 5.858], "end": [60, 100, 20]}
  engine sketch.point3d {"point": [80, 100, 10]}
  engine sketch.spline3d {"points": [[100, 100, 0], [105, 105, 10], [110, 100, 20]]}
}
engine solid.pipe {"name": "StraightPipe", "path_sketch": "Wire", "path": ["w1"], "diameter": "4 mm", "body_name": "Straight"}
engine solid.pipe {"name": "BentPipe", "path_sketch": "Wire", "path": ["w2"], "diameter": "4 mm", "body_name": "Bent"}
check Straight volume >= 376.95 mm3
check Straight volume <= 377.03 mm3
check Bent volume >= 394.74 mm3
check Bent volume <= 394.82 mm3
engine solid.pipe {"name": "CurvedPipe", "path_sketch": "Wire", "path": ["w3"], "diameter": "4 mm", "body_name": "Curved"}
check Curved size.z >= 20 mm

// Curves from 3D geometry: a cylinder's outline spun into the plane, its section fitted with arcs
// (an r 5 circle, extruded: pi 25), and a box's top edges included as reference curves (they
// don't form profiles)
cylinder Can radius 5 mm height 10 mm base (200, 100, 0) -> body Can
sketch SpunSk on XZ offset 100 mm {
  engine sketch.spun_profile {"body": "Can", "axis": "Z"}
}
sketch FitSk on XY offset 5 mm {
  engine sketch.fit_curves_to_section {"body": "Can"}
}
extrude FitX from FitSk distance 1 mm profiles (200, 100) new -> body FitDisc
check FitDisc volume >= 78.5 mm3
check FitDisc volume <= 78.6 mm3
box Cube size 10 10 10 corner (230, 100, 0) -> body Cube
sketch IncludeSk on XY offset 30 mm {
  engine sketch.include_3d {"refs": [{"edge": [235, 100, 10]}, {"edge": [240, 105, 10]}, {"edge": [235, 110, 10]}, {"edge": [230, 105, 10]}]}
}

// Linked 3D reference curves: where two cylinders meet, a u line on a cylinder, and a sketch line
// projected down onto the horizontal one's top. Like the included edges above, they are references: checked by building.
cylinder CrossA radius 5 mm height 30 mm base (300, 100, 0) -> body CrossA
cylinder CrossB radius 4 mm height 30 mm base (285, 100, 15 mm) axis X -> body CrossB
sketch RefCurves on XY {
  line g = (288, 100) (296, 100)
  fix g
  engine sketch.options {"sketch": "RefCurves", "sketch_3d": true}
  engine sketch.intersection_curve {"a": {"body": "CrossA"}, "b": {"body": "CrossB"}}
  engine sketch.isoparametric_curve {"face": [305, 100, 5], "body": "CrossA", "direction": "u"}
}
sketch Shadow on XY offset 40 mm {
  engine sketch.options {"sketch": "Shadow", "sketch_3d": true}
  engine sketch.project_to_surface {"curves": [{"sketch": "RefCurves", "curve": "l1"}], "face": [292, 100, 19], "body": "CrossB"}
}
