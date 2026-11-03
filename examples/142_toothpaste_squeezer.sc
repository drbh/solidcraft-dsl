// 142. Ratchet toothpaste squeezer: a slotted roller you wind with a key, held by a printed pawl.
//
// X runs along the roller, Y across the frame, Z up from the bed; the origin is under the middle
// of the roller. A U frame (a base and two cheeks) carries the roller in two plain bores. Feed
// the crimped end of the tube up through the slot in the base and into the slot through the
// roller, then turn the key: the tube winds onto the roller and is pulled flat through the base
// slot, which pushes the paste forward. Sizes follow `tube_w`, the tube's flattened width.
//
// The ratchet: a 12-tooth wheel on the roller, outside the +X cheek, and a pawl beside it, a
// thin upright cantilever standing on a foot off the frame. It carries a wedge that sits in the
// wheel's tooth gap. Turning the key counterclockwise, seen from the key end, rides the teeth'
// slopes over the wedge (the beam bends about 2 mm, about 0.7% strain in PLA); turning it back
// is stopped by a tooth's radial face. Press the pawl outward with a thumb to unwind.
//
// Hardware: one M3 x 10 screw (self-tapping into the 2.5 mm pilot) holds the end cap on the
// roller's -X end; the cap is wider than the bore, so the roller can't slide out.
//
// Printing, PLA or PETG, 0.2 mm layers, no supports: the Frame on its base (the pawl beam then
// prints upright; the bores are 14.5 mm and bridge cleanly, ream them if they sag); the Roller
// standing on its key; the Cap flat.
design ToothpasteSqueezer units mm

// Tube
param tube_w = 50 mm           // flattened width of the tube
param slot_t = 3 mm            // slot through the roller, for the tube's crimped end
param feed_gap = 5 mm          // slot in the base the tube is pulled through

// Frame
param ct = 5 mm                // cheek thickness
param cd = 36 mm               // frame depth, along Y
param base_t = 4 mm
param az = 26 mm               // roller axis height
param ch = 46 mm               // cheek height

// Roller and ratchet
param rr = 7 mm                // roller radius
param fit = 0.25 mm            // radial clearance in the bores
param gap = 0.3 mm             // axial clearance at the cheeks, wheel to pawl
param rw = 14 mm               // ratchet tip radius
param tooth_h = 2 mm           // tooth depth
param wheel_t = 4.5 mm
param beam_t = 1 mm            // pawl beam thickness (its stiffness)

let L = tube_w + 6 mm          // span between the cheeks
let xo = L/2 + ct              // outer face of a cheek
let rroot = rw - tooth_h
let xw0 = xo + gap             // the wheel's inner face
let xw1 = xw0 + wheel_t
let yb = rw + 0.5 mm           // the pawl beam's inner face
let foot_h = 6 mm

// ==== Frame: base plate, two cheeks, the feed slot and the two bores
box Base size (2*xo) cd base_t corner (-xo, -cd/2, 0) new -> body Frame
box CheekL size ct cd (ch - base_t + 0.5 mm) corner (-xo, -cd/2, base_t - 0.5 mm) join targets Frame
box CheekR size ct cd (ch - base_t + 0.5 mm) corner (L/2, -cd/2, base_t - 0.5 mm) join targets Frame
box FeedSlot size (tube_w + 2 mm) feed_gap (base_t + 2 mm) corner (-(tube_w/2 + 1 mm), -feed_gap/2, -1 mm) cut targets Frame
cylinder Bores radius (rr + fit) height (2*xo + 2 mm) base (-xo - 1 mm, 0, az) axis X cut targets Frame

// Pawl: a foot off the +X cheek and an upright beam with a wedge at the axis height. The wedge's
// top is the stop face, level with the axis; its underside slopes 59 deg from the bed.
box PawlFoot size (xw1 - xo + 0.5 mm) (cd/2 - yb) foot_h corner (xo - 0.5 mm, yb, 0) join targets Frame
box PawlBeam size wheel_t beam_t (az + 1 mm - foot_h + 0.2 mm) corner (xw0, yb, foot_h - 0.2 mm) join targets Frame
sketch WedgeSk on YZ offset xw0 {
  polyline closed w = (yb + 0.2 mm, az - 4 mm) (yb + 0.2 mm, az - gap) (rroot + gap, az - gap)
  fix w
}
extrude Wedge from WedgeSk distance wheel_t join targets Frame

// ==== Roller: journal and core in one, the tube slot through it, the wheel and the key outside
cylinder RollerCore radius rr height (2*xo + 2*gap + 0.5 mm) base (-xo - gap, 0, az) axis X new -> body Roller
box TubeSlot size (tube_w + 2 mm) slot_t (2*rr + 2 mm) corner (-(tube_w/2 + 1 mm), -slot_t/2, az - rr - 1 mm) cut targets Roller
// Ratchet wheel: 12 teeth, each a radial face at k*30 deg then a slope down to the next root, so
// a radial face sits at 0 deg, against the pawl's wedge
sketch WheelSk on YZ offset xw0 {
  polyline closed t = (rroot, az) (rw, az) (rroot*cos(30 deg), az + rroot*sin(30 deg)) (rw*cos(30 deg), az + rw*sin(30 deg)) (rroot*cos(60 deg), az + rroot*sin(60 deg)) (rw*cos(60 deg), az + rw*sin(60 deg)) (0, az + rroot) (0, az + rw) (rroot*cos(120 deg), az + rroot*sin(120 deg)) (rw*cos(120 deg), az + rw*sin(120 deg)) (rroot*cos(150 deg), az + rroot*sin(150 deg)) (rw*cos(150 deg), az + rw*sin(150 deg)) (-rroot, az) (-rw, az) (rroot*cos(210 deg), az + rroot*sin(210 deg)) (rw*cos(210 deg), az + rw*sin(210 deg)) (rroot*cos(240 deg), az + rroot*sin(240 deg)) (rw*cos(240 deg), az + rw*sin(240 deg)) (0, az - rroot) (0, az - rw) (rroot*cos(300 deg), az + rroot*sin(300 deg)) (rw*cos(300 deg), az + rw*sin(300 deg)) (rroot*cos(330 deg), az + rroot*sin(330 deg)) (rw*cos(330 deg), az + rw*sin(330 deg))
  fix t
}
extrude Wheel from WheelSk distance wheel_t join targets Roller
// The key: a hub and a wing to turn it by
cylinder KeyHub radius 8 mm height (6.5 mm + gap) base (xw1 - 0.5 mm, 0, az) axis X join targets Roller
box KeyWing size 6 mm 34 mm 10 mm corner (xw1 + gap, -17 mm, az - 5 mm) join targets Roller
fillet KeyEnds edges (xw1 + gap + 3 mm, 17 mm, az + 5 mm) (xw1 + gap + 3 mm, 17 mm, az - 5 mm) (xw1 + gap + 3 mm, -17 mm, az + 5 mm) (xw1 + gap + 3 mm, -17 mm, az - 5 mm) radius 4 mm body Roller
cylinder Pilot radius 1.25 mm height 10 mm base (-xo - gap - 0.1 mm, 0, az) axis X cut targets Roller

// ==== Cap: a disc screwed to the roller's -X end, wider than the bore
cylinder CapDisc radius 11 mm height 2.5 mm base (-xo - gap - 2.5 mm, 0, az) axis X new -> body Cap
cylinder CapHole radius 1.7 mm height 4 mm base (-xo - gap - 3 mm, 0, az) axis X cut targets Cap

// ==== Fits
check Roller clearance Frame >= 0.2 mm
check Cap clearance Frame >= 0.25 mm

color Frame "#f2f2ee"
color Roller "#3c8dde"
color Cap "#3c8dde"
material Frame, Roller, Cap PLA
