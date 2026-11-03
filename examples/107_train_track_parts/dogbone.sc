// The shared connector of 107_train_track: a dogbone peg (neck plus round head), drawn once.
//
// Local frame: the piece's end face is x = 0, the peg reaches out along +X, Z up from z0.
// grow = 0 gives the peg itself; grow = clearance gives the socket cutter, the same outline
// grown on every side, which a piece cuts out of its start so a neighbour's peg drops in.
design Dogbone units mm

param neck_w = 6 mm
param head_d = 11.5 mm
param reach = 17 mm          // face to the tip of the head
param overlap = 0.8 mm       // how far the neck reaches back into its own piece
param grow = 0 mm
param z0 = 0 mm
param h = 8.5 mm

let nh = neck_w/2 + grow
let hr = head_d/2 + grow
let cx = reach - head_d/2
let xi = cx - sqrt(hr^2 - nh^2)

sketch Outline on XY offset z0 {
  line a = (-overlap - grow, -nh) (xi, -nh)
  arc head = start a.end through (cx + hr, 0) end (xi, nh)
  line b = head.end (-overlap - grow, nh)
  line back = b.end a.start
  fix a head b back
}
extrude Bone from Outline distance h new -> body Dogbone
color Dogbone "#d9b382"
