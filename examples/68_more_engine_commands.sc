// 68. More engine commands without a statement of their own, each checked (a test bench, as 66).
// Needs engine 0.27: the pegs are patterned about a named construction axis.
design MoreEngineCommands units mm
param t = 1.5 mm

// surface.extend: a 10 mm tall wall surface, its top edge pushed up 5 mm more, then thickened.
sketch WallLine on XY {
  line w = (0, 0) (30, 0)
  fix w
}
extrude WallSkin from WallLine distance 10 mm curves w surface -> body WallSkin
engine surface.extend {"name": "Taller", "body": "WallSkin", "edges": [[15, 0, 10]], "distance": "5 mm"}
thicken Wall bodies WallSkin thickness 2 mm new -> body Wall
remove DropWallSkin bodies WallSkin
check Wall size.z == 15 mm
check Wall volume == 900 mm3

// surface.trim: a 40 x 40 square surface cut by the plane x = 25, keeping x < 25, thickened.
sketch Square on XY offset 40 mm {
  rect r = (0, 0) (40, 40)
  fix r
}
patch SquareSkin from Square -> body SquareSkin
engine surface.trim {"name": "Trimmed", "body": "SquareSkin", "plane": {"origin": [25, 0, 40], "normal": [1, 0, 0]}, "keep": [10, 20, 40]}
thicken Plate bodies SquareSkin thickness 1 mm new -> body Plate
remove DropSquareSkin bodies SquareSkin
check Plate volume == 1000 mm3
check Plate size.x == 25 mm

// surface.ruled: a strip 6 mm wide standing on a box's top front edge, along the top face's
// normal (straight up), thickened into a 1 mm lip.
box Base size 50 20 10 corner (0, 60, 0) -> body Base
engine surface.ruled {"name": "Lip", "body": "Base", "edges": [[25, 60, 10]], "distance": "6 mm", "face": [25, 70, 10], "body_name": "LipSkin"}
thicken LipWall bodies LipSkin thickness 1 mm new -> body LipWall
remove DropLipSkin bodies LipSkin
check LipWall size.z == 6 mm
check LipWall volume == 300 mm3

// surface.reverse_normal: a revolved surface's normal points toward its axis (see 63); turned
// over, the same thicken grows outward: a tube from r = 10 to 12.
sketch TubeLine on XZ {
  line l = (10, 0) (10, -20)
  fix l
}
revolve TubeSkin from TubeLine axis Z curves l surface -> body TubeSkin
move PlaceTube bodies TubeSkin to (100, 0, 0)
engine surface.reverse_normal {"name": "Outward", "body": "TubeSkin"}
thicken TubeWall bodies TubeSkin thickness 2 mm new -> body Tube
remove DropTubeSkin bodies TubeSkin
check Tube size.x == 24 mm

// construct.axis.two_planes: the axis where XZ and YZ meet (world Z), driving a circular pattern
// of four pegs round the origin of a disc.
cylinder Disc radius 20 mm height 3 mm base (0, 120, 0) -> body Disc
engine construct.axis.two_planes {"name": "DiscAxis", "a": {"plane": {"origin": [0, 120, 0], "normal": [1, 0, 0]}}, "b": {"plane": {"origin": [0, 120, 0], "normal": [0, 1, 0]}}}
cylinder Peg radius 2 mm height 5 mm base (12, 120, 3 mm) join targets Disc
pattern circular Pegs features Peg axis DiscAxis count 4 angle 360 deg
check Disc size.x == 40 mm
check Disc size.y == 40 mm

// sheet.unfold then sheet.refold: a bent tray flattened and bent back; it ends as it started.
sheet_rule Steel thickness t bend_radius t k_factor 0.44
box TrayBlank size 60 40 15 corner (0, 180, 0) -> body Tray
shell Open faces (30, 200, 15) thickness t body Tray
convert Sheet body Tray face (30, 200, 0) rule Steel bends (30, 180, 0) (30, 220, 0) (0, 200, 0) (60, 200, 0)
engine sheet.unfold {"body": "Tray"}
engine sheet.refold {"body": "Tray"}
check Tray size.z == 15 mm

// sketch.trim inside a sketch: a 20 x 20 square with a circle on its right edge; the circle's
// half inside the square and the square's edge across the circle are trimmed away, leaving one
// outline, a square with a round bump (400 + pi 5^2 / 2 = 439.27 mm2, 2 mm thick).
sketch Bump on XY offset 60 mm {
  rect r = (100, 100) (120, 120)
  circle c = (120, 110) radius 5 mm
  fix r c
  engine sketch.trim {"curve": "c1", "at": [115, 110]}
  engine sketch.trim {"curve": "l2", "at": [120, 110]}
}
extrude BumpPlate from Bump distance 2 mm profiles (110, 110) -> body BumpPlate
check BumpPlate size.x == 25 mm
check BumpPlate volume >= 878.4 mm3
check BumpPlate volume <= 878.7 mm3

// solid.align: a block's bottom face put onto the top face of another, 30 mm away.
box Lower size 10 10 10 corner (150, 0, 0) -> body Lower
box Upper size 10 10 10 corner (150, 0, 40) -> body Upper
engine solid.align {"name": "Stack", "bodies": ["Upper"], "from_face": [155, 5, 40], "to_face": [155, 5, 10]}
check Upper clearance Lower <= 0.001 mm
