// 53. Desk name plate: a triangular bar with a name debossed into its sloping front face.
//
// X runs along the bar. The cross-section (sketched on YZ) is a right triangle whose front face
// rises from the front edge at `slope`. The lettering is sketched on a construction plane tilted
// to the same slope about the front edge, so it lies on that face and follows any change to the
// slope or the size. It is debossed, so it can be painted in, or printed in a second colour.
//
// Print lying on its base. The front face leans back at the slope and the back is vertical; the
// tops of the letter pockets lean out 0.8 mm at most, which the extrusion lines span.
design DeskNamePlate units mm
param length = 160 mm
param depth = 36 mm
param slope = 55 deg
param name_h = 13 mm
param title_h = 6 mm
param letter_depth = 0.8 mm
let height = depth*tan(slope)
// Distance up the front face from the front edge
let face_len = depth/cos(slope)

sketch Section on YZ {
  polyline closed t = (0,0) (depth,0) (depth,height)
  fix t
}
extrude Bar from Section distance length -> body Plate

plane Face on XY angle slope axis X
sketch Lettering on Face {
  text "ADA LOVELACE" at (length/2,face_len*0.52) height name_h align center
  text "ANALYTICAL ENGINE DEPT." at (length/2,face_len*0.24) height title_h align center
}
emboss Letters from Lettering depth letter_depth deboss targets Plate

check Plate overhang <= 45 deg
check Plate size.z == height
