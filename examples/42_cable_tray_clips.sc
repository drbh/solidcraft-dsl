// 42. Under-desk cable tray (one 200 mm section) and a snap-on cable clip for its front rim.
//
// X runs along the tray, Y from the wall side (y = 0, the back wall) out toward the user, Z is up
// from the tray floor; z = height is the underside of the desk. The tray is an open-topped U: a
// tall back wall carries a flange that screws up into the desk (screw_count counterbored holes for
// 4 mm wood screws), the front wall is lower so cables drop in over it. A 45 deg bead runs along
// the outside of the front rim, rim_drop below its top. The clip is an inverted U that pushes down
// over the rim; its outer leg flexes out over the bead (about 2% strain) and its tooth catches
// under it. An open ring on the clip's outer face takes a cable (cable_d) that snaps in through a
// gap narrower than the cable, so a lamp or charger lead can run along the outside of the tray.
// Ends of the tray are open so sections can be butted together.
//
// Print without supports. The tray stands on its floor as modelled: walls are vertical, the
// mounting flange is carried by a 45 deg web under it, the rim bead has 45 deg flanks, and the
// only bridge is the ceiling of each counterbore (screw_head across). The clip prints on its side
// (an X face on the bed): it is one straight extrusion of its side profile, ring and tooth included.
design CableTrayClips units mm

// Tray
param tray_len = 200 mm
param inner_w = 80 mm
param height = 70 mm
param front_h = 50 mm
param wall = 2.5 mm
param floor_t = 2.5 mm
param inner_r = 3 mm
param flange_d = 16 mm
param flange_t = 4 mm

// Screws (4 mm wood screws up into the desk)
param screw_count = 3
param screw_end = 25 mm
param screw_clear = 4.5 mm
param screw_head = 9 mm
param under_head = 3.5 mm

// Rim bead and clip
param bead = 0.8 mm
param rim_drop = 12 mm
param clip_w = 10 mm
param clip_t = 2 mm
param clip_gap = 0.25 mm
param inner_leg = 8 mm
param clip_x = 95 mm
param cable_d = 8 mm
param ring_t = 2 mm
param ring_gap = 6 mm

let out_w = inner_w + 2*wall
let zb = front_h - rim_drop

// Tray side profile, extruded along X
sketch TraySk on YZ {
  polyline closed p = (-flange_d,height) (-flange_d,height-flange_t) (0,height-flange_t-flange_d) (0,0) (out_w,0) (out_w,front_h) (out_w-wall,front_h) (out_w-wall,floor_t) (wall,floor_t) (wall,height)
  fillet p.7.end radius inner_r
  fillet p.8.end radius inner_r
  fix p.1 p.2 p.3 p.4 p.5 p.6 p.7 p.8 p.9 p.10
}
extrude TrayBody from TraySk distance tray_len -> body Tray

// Bead along the outside of the front rim, 45 deg flanks
sketch BeadSk on YZ {
  polyline closed b = (out_w-0.01 mm,zb-bead) (out_w+bead,zb) (out_w-0.01 mm,zb+bead)
  fix b.1 b.2 b.3
}
extrude Bead from BeadSk distance tray_len join targets Tray

// Screw holes in the flange: clearance through, counterbore from below
let sy = -flange_d/2
let pitch_x = (tray_len - 2*screw_end)/(screw_count - 1)
cylinder Screw radius (screw_clear/2) height (flange_t+flange_d+2 mm) base (screw_end,sy,height-flange_t-flange_d-1 mm) cut targets Tray
cylinder ScrewHead radius (screw_head/2) height (flange_t+flange_d+1 mm-under_head) base (screw_end,sy,height-flange_t-flange_d-1 mm) cut targets Tray
pattern Screws features Screw, ScrewHead dir (1,0,0) count screw_count spacing pitch_x

// Clip: inverted U over the rim, tooth under the bead, cable ring on the outside
let yi = out_w - wall - clip_gap
let yo = out_w + bead + clip_gap
let top = front_h + clip_gap
let tooth_z = zb - clip_gap*sqrt(2)
let leg_bot = tooth_z - 2*bead - 1.5 mm
sketch ClipSk on YZ offset clip_x {
  polyline closed c = (yi-clip_t,top-inner_leg) (yi,top-inner_leg) (yi,top) (yo,top) (yo,tooth_z) (out_w+clip_gap,tooth_z-bead) (yo,tooth_z-2*bead) (yo,leg_bot) (yo+clip_t,leg_bot) (yo+clip_t,top+clip_t) (yi-clip_t,top+clip_t)
  fix c.1 c.2 c.3 c.4 c.5 c.6 c.7 c.8 c.9 c.10 c.11
}
extrude ClipBody from ClipSk distance clip_w new -> body Clip

// Cable ring: open toward +Y through a gap narrower than the cable
let ring_ri = cable_d/2 + 0.2 mm
let ring_ro = ring_ri + ring_t
let ry = yo + clip_t + ring_ri
let rz = (top + leg_bot)/2
cylinder Ring radius ring_ro height clip_w base (clip_x,ry,rz) axis X join targets Clip
cylinder RingBore radius ring_ri height (clip_w+2 mm) base (clip_x-1 mm,ry,rz) axis X cut targets Clip
box RingGap size (clip_w+2 mm) (ring_ro+1 mm) ring_gap corner (clip_x-1 mm,ry,rz-ring_gap/2) cut targets Clip

// Print orientations (also used by the overhang checks); prints without supports.
check Tray overhang <= 45 deg
print Clip up X
check Clip overhang <= 45 deg
