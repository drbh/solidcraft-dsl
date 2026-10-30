// 31. Storage clip that snaps onto a bicycle down tube and holds the front wheel straight.
//
// X runs along the down tube, Y is across the bike, Z points from the tube toward the tire.
// A C-shaped collar snaps over the tube through a slot narrower than the tube, on the side
// away from the wheel. A saddle on top of the collar carries two prongs that reach out about
// 2 in on either side of the tire. Each prong tapers in two directions: it narrows along the
// tube (side profile) and thins toward the tip (outer face slopes in), while the inner faces
// stay parallel so the tire sits in a constant-width slot. A triangular window lightens each prong.
//
// Print standing on the x = 0 end, no supports. Everything except the taper and the windows is a
// straight extrusion along X, so the collar, bore and snap slot print as vertical walls. The
// taper only trims the upper (+X) side of the prongs, so its sloped face points up. The windows
// have a flat floor and 45 deg sides meeting in a point, so they need no bridge. Layers lie across
// the tube axis, so both the snap hoop and the prongs bend along layers, not across them.
design BikeWheelHolder units mm
// Measure these on the bike
param tube_d = 41.15 mm
param tire_w = 28 mm
param tire_play = 4 mm
// Collar
param collar_len = 40 mm
param wall = 4 mm
param snap = 0.15
// Prongs
param reach = 2 in
param saddle_t = 5 mm
param prong_base_t = 8 mm
param prong_tip_t = 4 mm
param tip_len = 14 mm
let tube_r = tube_d/2
let ring_r = tube_r + wall
let gap = tire_w + tire_play
let half_out = gap/2 + prong_base_t
let saddle_top = ring_r + saddle_t
let tip_z = saddle_top + reach
let opening = tube_d*(1 - snap)
// Window in each prong: flat side at x = win_x, apex at x = win_x + win_a
param win_x = 3 mm
let win_a = reach/2 - 6 mm
let win_z = saddle_top + 3 mm

// Collar and saddle
cylinder Collar radius ring_r height collar_len base (0,0,0) axis X -> body Holder
box Saddle size collar_len (2*half_out) saddle_t corner (0,-half_out,ring_r - 2 mm) join targets Holder
box SaddleFill size collar_len (2*half_out) (ring_r) corner (0,-half_out,0) join targets Holder

// Prongs: cross-section in YZ, outer face slopes from base to tip thickness
sketch ProngsSk on YZ offset 0 {
  polyline closed right = (gap/2, ring_r - 2 mm) (half_out, ring_r - 2 mm) (gap/2 + prong_tip_t, tip_z) (gap/2, tip_z)
  polyline closed left = (-gap/2, ring_r - 2 mm) (-gap/2, tip_z) (-gap/2 - prong_tip_t, tip_z) (-half_out, ring_r - 2 mm)
  fix right.1 right.2 right.3 right.4 left.1 left.2 left.3 left.4
}
extrude Prongs from ProngsSk distance collar_len join targets Holder

// Side taper: trim the +X side of the prongs so they narrow from collar_len to tip_len.
// (XZ sketch coordinates are (x, -z).)
sketch TaperSk on XZ offset (-half_out - 1 mm) {
  polyline closed back = (collar_len + 1 mm, -saddle_top) (collar_len, -saddle_top) (tip_len, -tip_z) (tip_len, -tip_z - 1 mm) (collar_len + 1 mm, -tip_z - 1 mm)
  fix back.1 back.2 back.3 back.4 back.5
}
extrude Taper from TaperSk distance (2*half_out + 2 mm) cut targets Holder

// Prong windows, through both prongs
sketch WindowSk on XZ offset (-half_out - 1 mm) {
  polyline closed tri = (win_x, -win_z) (win_x + win_a, -win_z - win_a) (win_x, -win_z - 2*win_a)
  fix tri.1 tri.2 tri.3
}
extrude Windows from WindowSk distance (2*half_out + 2 mm) cut targets Holder

// Tube bore and snap slot (slot faces away from the wheel)
cylinder Bore radius tube_r height (collar_len + 2 mm) base (-1 mm,0,0) axis X cut targets Holder
box Slot size (collar_len + 2 mm) opening (ring_r + 1 mm) corner (-1 mm,-opening/2,-ring_r - 1 mm) cut targets Holder

// Print orientations (also used by the overhang checks); prints without supports.
print Holder up X
check Holder overhang <= 45 deg
