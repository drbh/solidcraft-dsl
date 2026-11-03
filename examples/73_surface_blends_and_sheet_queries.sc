// 73. Surface blend, fair and match, and the sheet queries, through `engine` (a test bench, as
// 66 and 68-72). Two flat 20 x 20 patches, one 10 higher and 10 along, are joined by a smooth
// blend; the blend is faired and its far edge matched to the upper patch; the three are
// stitched into one sheet that spans both. Then a bent tray's rules and bend table are queried.
design SurfaceBlendsAndSheetQueries units mm

sketch LowSk on XY {
  rect r = (0, 0) (20, 20)
  fix r
}
patch Low from LowSk -> body Sheet
sketch HighSk on XY offset 10 mm {
  rect r = (30, 0) (50, 20)
  fix r
}
patch High from HighSk -> body High
engine surface.blend {"name": "Blend", "from": {"body": "Sheet", "edges": [[20, 10, 0]]}, "to": {"body": "High", "edges": [[30, 10, 10]]}, "body_name": "Ramp"}
engine surface.fair {"name": "Fair", "body": "Ramp", "keep_edges": false}
engine surface.match {"name": "Match", "body": "Ramp", "target": "High", "edges": [[30, 10, 10]], "order": "g1"}
// One sheet from x = 0 to 50 and z = 0 to 10: the ramp spans the gap between the patches
stitch Join bodies Sheet, Ramp, High
check Sheet size.x == 50 mm
check Sheet size.z == 10 mm

// sheet.rules and sheet.export_bend_table: queries on a bent tray (as 64); the design builds
sheet_rule Steel thickness 1.5 mm bend_radius 1.5 mm k_factor 0.44
box TrayBlank size 60 40 15 corner (0, 60, 0) -> body Tray
shell Open faces (30, 80, 15) thickness 1.5 mm body Tray
convert Sheet2 body Tray face (30, 80, 0) rule Steel bends (30, 60, 0) (30, 100, 0) (0, 80, 0) (60, 80, 0)
engine sheet.rules {}
engine sheet.export_bend_table {"body": "Tray", "format": "json"}
check Tray size.z == 15 mm
