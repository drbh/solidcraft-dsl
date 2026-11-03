// 104. Rocking lounge chair: two flat-cut plywood sides on one rocker arc, slats for seat and back.
//
// Z is up, the sitter faces -Y, the chair is centred on x = 0. Each side is a single flat panel:
// its bottom edge is an arc of radius `rocker_r` touching the floor at y = 0, its top edge is the
// seat line (falling toward the back) and then the back line (raked back), and a rounded window
// lightens the middle. The seat and back slats lie across the two panels' top edges and are
// screwed down into them; two dowel stretchers through the panels near the floor square the frame.
// The low point of the rocker sits under the middle of the seat, so the chair comes to rest
// slightly reclined and rocks about there.
//
// Make it: the sides are 24 mm Baltic birch, cut on a CNC router or with a jigsaw from a full-size
// paper template (both sides together, then flush-trimmed so they match); round every edge with a
// 6 mm round-over bit and sand the rocker edge smooth and true. The slats are 70 x 20 mm hardwood
// (ash or oak), ends rounded, each fixed with two 5 x 50 mm screws per side into the panel edge,
// pre-drilled and counter-bored, plugged or capped. The stretchers are 32 mm hardwood dowel glued
// into 32 mm holes. Finish with oil or lacquer; stick felt strips on the rockers for wooden floors.
//
// Cut list (default sizes): 2 x side panel about 970 x 900 from 24 mm ply (one 1220 x 2440 sheet
// holds both); 8 seat slats and 8 back slats, 70 x 20 x (`width` + 2 x 24) = 608 mm; 2 x 32 mm
// dowel 608 mm. BOM: 64 x 5 x 50 mm screws, 32 plugs or caps, wood glue, finish, felt.
design RockingChair units mm

param width = 560 mm           // between the side panels
param panel_t = 24 mm
param rocker_r = 1100 mm       // radius of the rocker edge
param front_y = -430 mm        // rocker tips, front and back
param back_y = 520 mm
param seat_front_z = 400 mm    // seat line, front end (the slats sit on it)
param seat_back_y = 200 mm
param seat_back_z = 320 mm
param back_top_y = 440 mm      // back line, top end
param back_top_z = 900 mm
param slat_w = 70 mm
param slat_t = 20 mm
param dowel_d = 32 mm

let front_z = rocker_r - sqrt(rocker_r^2 - front_y^2)
let back_z = rocker_r - sqrt(rocker_r^2 - back_y^2)
let seat_l = hypot(seat_back_y - front_y, seat_back_z - seat_front_z)
let back_l = hypot(back_top_y - seat_back_y, back_top_z - seat_back_z)
let seat_ang = atan2(seat_back_z - seat_front_z, seat_back_y - front_y)
let back_ang = atan2(back_top_z - seat_back_z, back_top_y - seat_back_y)
let slat_l = width + 2*panel_t

// ==== Side panels (YZ sketches: first coordinate y, second z)
sketch RightSk on YZ offset (width/2) {
  polyline o = (front_y, front_z) (front_y, seat_front_z) (seat_back_y, seat_back_z) (back_top_y, back_top_z) (back_top_y + 75 mm, back_top_z - 20 mm) (back_y, back_z)
  arc rocker = start o.5.end through (0, 0) end o.1.start
  fillet o.1.end radius 30 mm
  fillet o.2.end radius 70 mm
  fillet o.3.end radius 30 mm
  fillet o.4.end radius 30 mm
  fix o rocker
}
extrude RightPanel from RightSk distance panel_t new -> body RightPanel
sketch LeftSk on YZ offset (-width/2 - panel_t) {
  polyline o = (front_y, front_z) (front_y, seat_front_z) (seat_back_y, seat_back_z) (back_top_y, back_top_z) (back_top_y + 75 mm, back_top_z - 20 mm) (back_y, back_z)
  arc rocker = start o.5.end through (0, 0) end o.1.start
  fillet o.1.end radius 30 mm
  fillet o.2.end radius 70 mm
  fillet o.3.end radius 30 mm
  fillet o.4.end radius 30 mm
  fix o rocker
}
extrude LeftPanel from LeftSk distance panel_t new -> body LeftPanel

// Lightening window through both panels, and the stretcher holes
sketch WindowSk on YZ offset (-width/2 - panel_t - 1 mm) {
  slot w = centers (-150 mm, 170 mm) (250 mm, 200 mm) width 120 mm
  circle f = (-300 mm, 100 mm) diameter (dowel_d + 0.2 mm)
  circle r = (380 mm, 140 mm) diameter (dowel_d + 0.2 mm)
  fix w f r
}
extrude Windows from WindowSk distance (width + 2*panel_t + 2 mm) cut targets RightPanel, LeftPanel

// ==== Stretchers: dowels through both panels
cylinder FrontStretcher radius (dowel_d/2) height slat_l base (-width/2 - panel_t, -300 mm, 100 mm) axis (1,0,0) new -> body FrontStretcher
cylinder RearStretcher radius (dowel_d/2) height slat_l base (-width/2 - panel_t, 380 mm, 140 mm) axis (1,0,0) new -> body RearStretcher

// ==== Slats: built flat at the origin, tilted to their line, then set on it (centres a slat
// thickness above the panel edge, spread evenly from 6 % to 94 % of the line)
for k from 0 to 7 {
  box SeatSlat{k} size slat_l slat_w slat_t corner (-slat_l/2, -slat_w/2, 0.2 mm) new -> body SeatSlat{k}
  move SeatTilt{k} bodies SeatSlat{k} axis (1,0,0) angle seat_ang
  move SeatPlace{k} bodies SeatSlat{k} to (0, front_y + (0.06 + 0.88*k/7)*(seat_back_y - front_y), seat_front_z + (0.06 + 0.88*k/7)*(seat_back_z - seat_front_z))
  color SeatSlat{k} "#b07a48"
}
for k from 0 to 7 {
  box BackSlat{k} size slat_l slat_w slat_t corner (-slat_l/2, -slat_w/2, 0.2 mm) new -> body BackSlat{k}
  move BackTilt{k} bodies BackSlat{k} axis (1,0,0) angle back_ang
  move BackPlace{k} bodies BackSlat{k} to (0, seat_back_y + (0.08 + 0.85*k/7)*(back_top_y - seat_back_y), seat_back_z + (0.08 + 0.85*k/7)*(back_top_z - seat_back_z))
  color BackSlat{k} "#b07a48"
}

color RightPanel, LeftPanel "#dcc49a"
color FrontStretcher, RearStretcher "#b07a48"
