// 75. Sketch editing commands through `engine` (a test bench, as 66 and 68-74): constraints that
// move geometry, edits of finished sketches, copy and paste, and the editor's queries. Sketches
// are extruded 1 mm, so volume is area; queries (solve, inspect, chain, glyphs, snap, loose ends,
// drawing bounds, DXF export) are checked by the design building.
design SketchEditing units mm

// sketch.constraint.collinear: a 20 x 10 outline whose bottom right corner was drawn 1 high; made
// collinear with the fixed bottom left edge, and the right side held vertical, it is a
// rectangle: 200 (195 as drawn)
sketch CollSk on XY {
  engine sketch.line {"points": [[0, 0], [10, 0]]}
  engine sketch.line {"points": [[10, 0], [20, 1], [20, 10], [0, 10], [0, 0]]}
  engine sketch.constraint.fix {"entities": ["l1", "l4", "l5"], "fixed": true}
  engine sketch.constraint.horizontal_vertical {"line": "l3", "mode": "vertical"}
  engine sketch.constraint.collinear {"a": "l1", "b": "l2"}
}
extrude CollX from CollSk distance 1 mm profiles (10, 5) -> body Coll
check Coll volume >= 199.9 mm3
check Coll volume <= 200.1 mm3

// sketch.constraint.curvature, sketch.spline.insert_point and sketch.spline.reverse: two fit
// splines joined G2 (outer ends fixed, so the joint moves), the first given one more fit point
// and reversed; closed by a line, the outline still spans 20
sketch CurveSk on XY {
  engine sketch.spline.fit_point {"points": [[40, 0], [45, 6], [52, 8]]}
  engine sketch.spline.fit_point {"points": ["s1.end", [57, 6], [60, 0]]}
  engine sketch.constraint.fix {"entities": ["s1.start", "s2.end"], "fixed": true}
  engine sketch.constraint.curvature {"a": "s1", "b": "s2"}
  engine sketch.spline.insert_point {"curve": "s1", "at": [42, 3.5]}
  engine sketch.spline.reverse {"curve": "s1"}
  engine sketch.line {"points": [[60, 0], [40, 0]]}
}
extrude CurveX from CurveSk distance 1 mm profiles (50, 2) -> body Curved
check Curved size.x >= 20 mm

// sketch.offset.break: a 20 x 10 rectangle offset 2 out, the copy made editable; the ring
// between them is 24 x 14 - 200 = 136
sketch OffsetSk on XY {
  rect r = (80, 0) (100, 10)
  fix r
  offset r distance 2 mm side (79, -1)
  engine sketch.offset.break {"curve": "l5"}
}
extrude OffsetX from OffsetSk distance 1 mm profiles (79, 5) -> body OffsetRing
check OffsetRing volume == 136 mm3

// sketch.copy and sketch.paste: an r 3 disc copied 10 to the right: two discs 4 apart
sketch CopySk on XY {
  engine sketch.circle.two_point {"p0": [117, 0], "p1": [123, 0]}
  engine sketch.copy {"entities": ["c1"]}
  engine sketch.paste {"translate": [10, 0]}
}
extrude CopyAX from CopySk distance 1 mm profiles (120, 0) -> body CopyA
extrude CopyBX from CopySk distance 1 mm profiles (130, 0) -> body CopyB
check CopyA clearance CopyB >= 3.999 mm
check CopyA clearance CopyB <= 4.001 mm

// sketch.cut and sketch.paste: the same disc cut and pasted 20 to the right; only one remains
// there, 17 from a reference bar at x = 148
sketch CutSk on XY {
  engine sketch.circle.two_point {"p0": [147, 0], "p1": [153, 0]}
  engine sketch.cut {"entities": ["c1"]}
  engine sketch.paste {"translate": [20, 0]}
}
extrude CutX from CutSk distance 1 mm profiles (170, 0) -> body Moved
box CutRef size 1 10 1 corner (147, -5, 0) -> body CutRef
check Moved clearance CutRef >= 18.999 mm
check Moved clearance CutRef <= 19.001 mm

// sketch.edit and sketch.cancel: a finished 20 x 10 rectangle opened, a hole drawn, then the edit
// cancelled: no hole, 200
sketch KeptSk on XY {
  rect r = (200, 0) (220, 10)
  fix r
}
engine sketch.edit {"sketch": "KeptSk"}
engine sketch.circle.two_point {"p0": [207, 5], "p1": [213, 5]}
engine sketch.cancel {}
extrude KeptX from KeptSk distance 1 mm profiles (202, 2) -> body Kept
check Kept volume == 200 mm3

// sketch.equation_curve.edit: a half disc of radius 10 edited into a half ellipse 5 high:
// pi 10 5 / 2 = 78.54
sketch EqSk on XY {
  engine sketch.equation_curve {"x": "250 + 10 * cos(t)", "y": "10 * sin(t)", "start": 0, "end": "pi"}
  engine sketch.equation_curve.edit {"curve": "s1", "y": "5 * sin(t)"}
  engine sketch.line {"points": [[240, 0], [260, 0]]}
}
extrude EqX from EqSk distance 1 mm profiles (250, 2) -> body HalfEllipse
check HalfEllipse volume >= 78.50 mm3
check HalfEllipse volume <= 78.58 mm3

// sketch.move_point and sketch.drag (live frames, then drag_end): a free 20 x 10 rectangle's
// corner moved to x = 310, then dragged up to y = 20, keeping it a rectangle: 30 x 20 = 600
sketch DragSk on XY {
  engine sketch.rectangle.two_point {"p0": [280, 0], "p1": [300, 10]}
  engine sketch.constraint.fix {"entities": ["p1"], "fixed": true}
  engine sketch.move_point {"point": "p3", "to": [310, 10]}
  engine sketch.drag {"entity": "p3", "from": [310, 10], "to": [310, 15], "live": true}
  engine sketch.drag {"entity": "p3", "from": [310, 15], "to": [310, 20], "live": true}
  engine sketch.drag_end {}
}
extrude DragX from DragSk distance 1 mm profiles (285, 5) -> body Dragged
check Dragged volume >= 599.9 mm3
check Dragged volume <= 600.1 mm3

// sketch.constrainer: a nearly level top edge (rising 0.2) made horizontal, so a quadrilateral on
// a fixed bottom and left side becomes a 20 x 10 rectangle: 200 (202 as drawn)
sketch LevelSk on XY {
  engine sketch.line {"points": [[340, 0], [360, 0], [360, 10.2], [340, 10], [340, 0]]}
  engine sketch.constraint.fix {"entities": ["l1", "l4"], "fixed": true}
  engine sketch.constrainer {"entities": ["l3"]}
}
extrude LevelX from LevelSk distance 1 mm profiles (350, 5) -> body Level
check Level volume >= 199.9 mm3
check Level volume <= 200.1 mm3

// sketch.loose_ends and sketch.loose_ends.tie: four lines drawn without joining their ends
// (connect off) are tied into a closed 20 x 10 outline: 200
sketch TieSk on XY {
  engine sketch.line {"points": [[380, 0], [400, 0]], "connect": false}
  engine sketch.line {"points": [[400, 0], [400, 10]], "connect": false}
  engine sketch.line {"points": [[400, 10], [380, 10]], "connect": false}
  engine sketch.line {"points": [[380, 10], [380, 0]], "connect": false}
  engine sketch.loose_ends {"sketch": "TieSk"}
  engine sketch.loose_ends.tie {"sketch": "TieSk"}
}
extrude TieX from TieSk distance 1 mm profiles (390, 5) -> body Tied
check Tied volume == 200 mm3

// sketch.auto_constrain (and from a datum), then sketch.finish_auto_constrain: a free rectangle
// fully constrained as drawn; its area is unchanged: 200
sketch AutoSk on XY {
  engine sketch.rectangle.two_point {"p0": [420, 0], "p1": [440, 10]}
  engine sketch.auto_constrain_from_datum {}
  engine sketch.auto_constrain {}
  engine sketch.solve {}
  engine sketch.chain {"curve": "l1"}
  engine sketch.glyphs {}
  engine sketch.snap {"at": [440, 10]}
  engine sketch.inspect {}
  engine sketch.line {"points": [[420, 20], [440, 20]]}
  engine sketch.infinite {"curves": ["l5"], "value": true}
}
extrude AutoX from AutoSk distance 1 mm profiles (430, 5) -> body Auto
check Auto volume == 200 mm3
sketch FinishSk on XY {
  engine sketch.rectangle.two_point {"p0": [460, 0], "p1": [470, 10]}
  engine sketch.finish_auto_constrain {}
}
extrude FinishX from FinishSk distance 1 mm profiles (465, 5) -> body Finished
check Finished volume == 100 mm3

// sketch.redefine: a finished sketch moved onto a plane 10 up; its disc stands 10 above a plate
box Plate size 20 20 1 corner (490, -10, -1 mm) -> body Plate
sketch Moving on XY {
  circle c = (500, 0) radius 4 mm
  fix c
}
engine sketch.redefine {"sketch": "Moving", "plane": {"origin": [0, 0, 10], "normal": [0, 0, 1], "x_dir": [1, 0, 0], "y_dir": [0, 1, 0]}}
extrude MovingX from Moving distance 1 mm -> body Lifted
check Lifted clearance Plate >= 9.999 mm
check Lifted clearance Plate <= 10.001 mm

// sketch.break_link: a box's top face projected into a sketch, then unlinked into plain geometry;
// the outline is unchanged: 10 x 10 = 100
box Source size 10 10 10 corner (530, 0, 0) -> body Source
sketch UnlinkedSk on XY offset 20 mm {
  project face (535, 5, 10) body Source
  engine sketch.break_link {"entities": ["l1", "l2", "l3", "l4"]}
}
extrude UnlinkedX from UnlinkedSk distance 1 mm -> body Unlinked
check Unlinked volume == 100 mm3

// sketch.drawing_bounds (of SVG text) and sketch.export_dxf (returned as text): queries
sketch Queries on XY {
  line q = (560, 0) (570, 0)
  fix q
  engine sketch.drawing_bounds {"text": "<svg xmlns='http://www.w3.org/2000/svg' width='20' height='10'><rect width='20' height='10'/></svg>", "format": "svg"}
  engine sketch.export_dxf {"sketch": "Queries"}
}

// sketch.draw.dimensioned: a rectangle drawn 15 x 8 and dimensioned 20 x 10 in one step: 200
sketch DimDrawSk on XY offset 20 mm {
  engine sketch.draw.dimensioned {"command": "sketch.rectangle.two_point", "params": {"p0": [0, 0], "p1": [15, 8]}, "dimensions": {"width": 20, "height": 10}}
}
extrude DimDrawX from DimDrawSk distance 1 mm profiles (5, 5) -> body DimDrawn
check DimDrawn volume == 200 mm3

// sketch.edit_text: 5 mm text "II" edited to 10 mm "IIII" (picked by one of its curves): the
// lettering, extruded 1 mm, grows from under 6 to over 8 tall
sketch LabelSk on XY offset 20 mm {
  point o = (40, 0)
  fix o
  engine sketch.text {"text": "II", "at": [40, 0], "height": 5}
  engine sketch.edit_text {"entity": "l1", "text": "IIII", "height": 10}
}
extrude LabelX from LabelSk distance 1 mm -> body Label
check Label size.y >= 8 mm

// sketch.auto_project: with it on, a sketch made on a face starts with that face's edges, so the
// outline of a 10 x 10 block's top can be extruded straight away: 100. (It is a setting of the
// running design, not a saved preference.) Turned off again after.
box AutoBlock size 10 10 10 corner (80, 40, 0) -> body AutoBlock
engine sketch.auto_project {"value": true}
sketch OnFaceSk on face (85, 45, 10) {
}
engine sketch.auto_project {"value": false}
extrude OnFaceX from OnFaceSk distance 1 mm new -> body FaceCopy
check FaceCopy volume == 100 mm3
