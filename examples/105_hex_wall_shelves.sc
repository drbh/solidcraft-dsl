// 105. Modular hex wall shelves: open hexagon cells that hang on keyholes, joined by slide-on clips.
//
// The wall is the plane y = 0, the cells stand out from it toward -Y, Z is up. A cell is a
// hexagonal tube with flat top and bottom (`af` across the flats, `depth` deep) and a back flange
// round its rim. Two keyholes in the top of the flange hang it on screws; each keyhole has a head
// pocket on the wall side so the flange sits flat. Cells tile like a honeycomb: any two that share
// a face are held at the front by a Clip, a U that slides over the doubled wall from the front.
// The model shows three cells in a cluster with the three clips that join them.
//
// Print (PLA or PETG, 0.2 mm layers, 3 perimeters, no supports): each cell back down, flange on
// the bed; the head pockets bridge 9 mm. Clips stand on end, the U flat on the bed (print one per
// shared face). A cell is about 208 x 180 mm.
// BOM per cell: 2 x 4 x 30 mm pan or round-head wood screws (heads under 8.5 mm) and plugs.
design HexWallShelves units mm

param af = 180 mm            // across the flats
param depth = 100 mm
param wall = 3 mm
param lip = 24 mm            // back flange width, from the outside
param back_t = 6 mm
param key_pitch = 60 mm      // keyhole spacing
param clip_len = 30 mm
param clip_t = 1.6 mm
param clip_reach = 10 mm     // how far the clip's legs run into the cells
param fit = 0.15 mm

let ro = af/sqrt(3)
let key_z = af/2 - wall - (lip - wall)/2 - 3 mm   // keyhole entry centre, in the top of the flange

// ==== One cell at the origin, then copies placed on the honeycomb (hexagons drawn flat-top)
for k from 0 to 2 {
  sketch Outer{k} on XZ offset (-depth) {
    polyline closed h = ((ro), 0) ((ro)/2, (ro)*sqrt(3)/2) (-(ro)/2, (ro)*sqrt(3)/2) (-(ro), 0) (-(ro)/2, -(ro)*sqrt(3)/2) ((ro)/2, -(ro)*sqrt(3)/2)
    fix h
  }
  extrude Cell{k}Body from Outer{k} distance depth new -> body Cell{k}
  sketch Inner{k} on XZ offset (-depth - 1 mm) {
    polyline closed h = (((af - 2*wall)/sqrt(3)), 0) (((af - 2*wall)/sqrt(3))/2, ((af - 2*wall)/sqrt(3))*sqrt(3)/2) (-((af - 2*wall)/sqrt(3))/2, ((af - 2*wall)/sqrt(3))*sqrt(3)/2) (-((af - 2*wall)/sqrt(3)), 0) (-((af - 2*wall)/sqrt(3))/2, -((af - 2*wall)/sqrt(3))*sqrt(3)/2) (((af - 2*wall)/sqrt(3))/2, -((af - 2*wall)/sqrt(3))*sqrt(3)/2)
    fix h
  }
  extrude Cell{k}Hollow from Inner{k} distance (depth - back_t + 1 mm) cut targets Cell{k}
  sketch Lip{k} on XZ offset (-depth - 1 mm) {
    polyline closed h = (((af - 2*lip)/sqrt(3)), 0) (((af - 2*lip)/sqrt(3))/2, ((af - 2*lip)/sqrt(3))*sqrt(3)/2) (-((af - 2*lip)/sqrt(3))/2, ((af - 2*lip)/sqrt(3))*sqrt(3)/2) (-((af - 2*lip)/sqrt(3)), 0) (-((af - 2*lip)/sqrt(3))/2, -((af - 2*lip)/sqrt(3))*sqrt(3)/2) (((af - 2*lip)/sqrt(3))/2, -((af - 2*lip)/sqrt(3))*sqrt(3)/2)
    fix h
  }
  extrude Cell{k}Lip from Lip{k} distance (depth + 2 mm) cut targets Cell{k}
  // keyholes: an entry hole and a slot up through the flange, a head pocket behind
  sketch Key{k} on XZ offset (-back_t - 1 mm) {
    circle a = (-key_pitch/2, -key_z) radius 4.5 mm
    circle b = (key_pitch/2, -key_z) radius 4.5 mm
    slot sa = centers (-key_pitch/2, -key_z) (-key_pitch/2, -(key_z + 8 mm)) width 4.6 mm
    slot sb = centers (key_pitch/2, -key_z) (key_pitch/2, -(key_z + 8 mm)) width 4.6 mm
    fix a b sa sb
  }
  extrude Cell{k}Keys from Key{k} distance (back_t + 2 mm) cut targets Cell{k}
  sketch Pocket{k} on XZ offset (-3.5 mm) {
    slot pa = centers (-key_pitch/2, -key_z) (-key_pitch/2, -(key_z + 8 mm)) width 9.2 mm
    slot pb = centers (key_pitch/2, -key_z) (key_pitch/2, -(key_z + 8 mm)) width 9.2 mm
    fix pa pb
  }
  extrude Cell{k}Pockets from Pocket{k} distance 4.5 mm cut targets Cell{k}
}
move PlaceCell1 bodies Cell1 to (0, 0, af)
move PlaceCell2 bodies Cell2 to (af*cos(30 deg), 0, af*sin(30 deg))

// ==== Clips: a U over the doubled wall at the front edge, built over cell 0's top face and
// turned onto each shared face
for k from 0 to 2 {
  box Clip{k}Block size clip_len (clip_reach + clip_t) (4*wall + 2*fit) corner (-clip_len/2, -depth - clip_t - fit, af/2 - 2*wall - fit) new -> body Clip{k}
  box Clip{k}Gap size (clip_len + 2 mm) (clip_reach + 1 mm) (2*wall + 2*fit) corner (-clip_len/2 - 1 mm, -depth - fit, af/2 - wall - fit) cut targets Clip{k}
}
// clip 0 joins cells 0 and 1 (top face of 0); clip 1 joins 0 and 2 (the 30 deg face of 0);
// clip 2 joins 1 and 2 (the -30 deg face of 1, which is cell 2's 150 deg face)
move TurnClip1 bodies Clip1 axis (0,1,0) angle 60 deg
move TurnClip2 bodies Clip2 axis (0,1,0) angle 120 deg
move PlaceClip2 bodies Clip2 to (0, 0, af)

check Clip0 clearance Cell0 >= 0.1 mm
check Clip0 clearance Cell1 >= 0.1 mm
check Clip1 clearance Cell0 >= 0.1 mm
check Clip1 clearance Cell2 >= 0.1 mm
check Clip2 clearance Cell1 >= 0.1 mm
check Clip2 clearance Cell2 >= 0.1 mm

color Cell0, Cell1, Cell2 "#c9a77c"
color Clip0, Clip1, Clip2 "#3a3a3a"
material Cell0, Cell1, Cell2, Clip0, Clip1, Clip2 PLA
