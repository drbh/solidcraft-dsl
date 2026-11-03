// 138. Rugged storage box, inspired by the popular parametric rugged boxes: a pinned hinge, two snap
// latches, a gasket groove in the lid, stacking feet and ribbed walls. The inside size drives it all.
//
// X runs along the hinge, Y from the front (y = 0, where the latches are) to the back, Z up; the
// origin is the base's front-left-bottom corner (the feet stand below z = 0). in_x, in_y and in_d
// are the base's inside, lid_d the lid's inside depth; every other position is worked out from them.
//
// The base and lid each carry a flange round their mating rim: a band rim_out proud of the wall,
// over (or under) a 45 deg skirt, so both print without supports. The lid is modelled closed,
// floating `seat` above the base so the two never touch. A groove in the lid's rim takes a 2 mm
// silicone cord (or a printed TPU ring), which seals on the base's flat rim when the latches pull
// the lid down.
//
// Hinge: five knuckles on the axis behind the back flange, base, lid, base, lid, base, `gap` apart
// along X, each on a 45 deg web like 47_print_in_place_hinge_box. A 3 mm rod through all five is
// the pin (Pin is the bought rod, placed). Latches: a cantilever arm hangs from the lid's top edge
// outside the front flange, its lip snapping under the base flange; the lip's lead-in chamfer rides
// over the band as the lid closes and a pull tab frees it. Under each lip the base's skirt is
// pocketed so the lip has somewhere to go.
//
// Stacking: four corner feet under the base nest in four pockets in the lid's top, stack_clear
// round each side, so stacked boxes can't slide. Vertical ribs stiffen the base walls and carry
// the flange.
//
// Bill of materials: a 3 mm steel rod, (in_x + 2*wall - 2*corner_r - 2 mm) long (126.8 mm at the default
// size), with a drop of glue at each end; 2 mm silicone cord, about the rim's length (2*(in_x + in_y)
// plus a little, 470 mm at the default size).
//
// Print the Base upright on its feet and the Lid top down; neither needs supports. PETG is best:
// the latch arms flex across their layers, so print them with four or more perimeters.
design RuggedBox units mm

// Inside
param in_x = 140 mm
param in_y = 90 mm
param in_d = 55 mm             // the base's inside depth
param lid_d = 18 mm            // the lid's inside depth

// Walls and rim
param wall = 2.4 mm
param floor_t = 3 mm
param lid_top = 4 mm           // thick enough to take the stacking pockets
param corner_r = 8 mm
param rim_out = 3 mm           // how far the flange stands proud of the wall
param rim_h = 6 mm
param seat = 0.3 mm            // lid above the base, modelled; the gasket closes it

// Gasket groove, centred on the rim
param groove_w = 2.2 mm
param groove_d = 1.6 mm

// Hinge
param knuckle_r = 5 mm
param gap = 0.4 mm
param pin_d = 3 mm
param pin_clear = 0.2 mm

// Latches
param latch_w = 18 mm
param arm_t = 2.4 mm
param catch_d = 1.2 mm         // how far the lip reaches under the flange
param lip_t = 2.4 mm
param lip_gap = 0.3 mm

// Feet and ribs
param foot = 14 mm
param foot_in = 4 mm
param foot_h = 2 mm
param stack_clear = 0.4 mm
param rib_w = 4 mm

let OX = in_x + 2*wall
let OY = in_y + 2*wall
let base_h = floor_t + in_d
let lid_h = lid_top + lid_d
let z0L = base_h + seat
let zT = z0L + lid_h

// Hinge axis and knuckles
let y_a = OY + rim_out + gap + knuckle_r
let z_a = base_h + seat/2
let r45 = knuckle_r*sqrt(2)/2
let y0 = OY + rim_out - 1 mm
let hinge_l = OX - 2*corner_r - 2 mm       // ends 1 mm in from the corners' tangent seams
let k = (hinge_l - 4*gap)/5
let pitch = k + gap
let x_h0 = corner_r + 1 mm
let drop = y_a + 0.85*r45 - OY + 0.5 mm

// Latches: arm faces, lip height
let ya1 = -rim_out - gap
let ya0 = ya1 - arm_t
let catch_z = base_h - rim_h + 0.5 mm       // the catch face: the pocket's roof, cut up into the band
let z_lip1 = catch_z - lip_gap
let z_lip0 = z_lip1 - lip_t
let xl0 = OX/4
let xl1 = 3*OX/4

// Groove edges, about the rim's middle
let g_mid = (wall - rim_out)/2
let g_out = g_mid - groove_w/2
let g_in = g_mid + groove_w/2

// ==== Base
sketch BaseSk on XY {
  rect rounded o = (0, 0) (OX, OY) radius corner_r
  fix o
}
extrude BaseBlock from BaseSk distance base_h new -> body Base

// Flange: a 45 deg skirt from just inside the wall out to the band, then the band
sketch BaseSkirtSk on XY offset (base_h - rim_h + 0.2 mm - (rim_out + 0.2 mm)) {
  rect rounded o = (0.5 mm, 0.5 mm) (OX - 0.5 mm, OY - 0.5 mm) radius (corner_r - 0.5 mm)
  fix o
}
extrude BaseSkirt from BaseSkirtSk distance (rim_out + 0.2 mm) taper 45 deg join targets Base
sketch BaseBandSk on XY offset (base_h - rim_h) {
  rect rounded o = (-rim_out, -rim_out) (OX + rim_out, OY + rim_out) radius (corner_r + rim_out)
  fix o
}
extrude BaseBand from BaseBandSk distance rim_h join targets Base

// Ribs: front and back at an eighth, the middle and seven eighths of the length (clear of the
// latches at the quarters), and one on each end
for i from 0 to 2 {
  box RibF{i} size rib_w (rim_out - 0.3 mm + 0.5 mm) (base_h - rim_h + 0.1 mm) corner (OX*(1 + 3*i)/8 - rib_w/2, -(rim_out - 0.3 mm), 0) join targets Base
  box RibB{i} size rib_w (rim_out - 0.3 mm + 0.5 mm) (base_h - rim_h + 0.1 mm) corner (OX*(1 + 3*i)/8 - rib_w/2, OY - 0.5 mm, 0) join targets Base
}
box RibL size (rim_out - 0.3 mm + 0.5 mm) rib_w (base_h - rim_h + 0.1 mm) corner (-(rim_out - 0.3 mm), OY/2 - rib_w/2, 0) join targets Base
box RibR size (rim_out - 0.3 mm + 0.5 mm) rib_w (base_h - rim_h + 0.1 mm) corner (OX - 0.5 mm, OY/2 - rib_w/2, 0) join targets Base

// Feet at the corners
box Foot0 size foot foot (foot_h + 0.5 mm) corner (foot_in, foot_in, -foot_h) join targets Base
box Foot1 size foot foot (foot_h + 0.5 mm) corner (OX - foot_in - foot, foot_in, -foot_h) join targets Base
box Foot2 size foot foot (foot_h + 0.5 mm) corner (foot_in, OY - foot_in - foot, -foot_h) join targets Base
box Foot3 size foot foot (foot_h + 0.5 mm) corner (OX - foot_in - foot, OY - foot_in - foot, -foot_h) join targets Base

// Base knuckles 0, 2, 4 on 45 deg webs from the back wall (YZ sketch: u = y, v = z)
for i from 0 to 2 {
  sketch BaseWebSk{i} on YZ offset (x_h0 + 2*i*pitch) {
    polyline closed w = (y0, base_h - 0.2 mm) (y_a, base_h - 0.2 mm) (y_a + 0.85*r45, z_a - 0.85*r45) (OY - 0.5 mm, z_a - 0.85*r45 - drop) (OY - 0.5 mm, base_h - rim_h)
    fix w
  }
  extrude BaseWeb{i} from BaseWebSk{i} distance k new -> body BaseHinge{i}
  cylinder BaseKnuckle{i} radius knuckle_r height k base (x_h0 + 2*i*pitch, y_a, z_a) axis X join targets BaseHinge{i}
  cylinder PinBoreB{i} radius (pin_d/2 + pin_clear) height (k + 2 mm) base (x_h0 + 2*i*pitch - 1 mm, y_a, z_a) axis X cut targets BaseHinge{i}
  combine JoinBaseHinge{i} target Base tools BaseHinge{i} join
}

// Pockets under the flange for the latch lips
box LipPocket0 size (latch_w + 2 mm) (catch_d + gap + 1 mm) (lip_t + lip_gap + 3 mm) corner (xl0 - latch_w/2 - 1 mm, -rim_out - 1 mm, z_lip0 - 3 mm) cut targets Base
box LipPocket1 size (latch_w + 2 mm) (catch_d + gap + 1 mm) (lip_t + lip_gap + 3 mm) corner (xl1 - latch_w/2 - 1 mm, -rim_out - 1 mm, z_lip0 - 3 mm) cut targets Base

// Hollow it last
sketch BaseCavSk on XY offset floor_t {
  rect rounded o = (wall, wall) (OX - wall, OY - wall) radius (corner_r - wall)
  fix o
}
extrude BaseCavity from BaseCavSk distance (in_d + 1 mm) cut targets Base

// ==== Lid, closed, seat above the base
sketch LidSk on XY offset z0L {
  rect rounded o = (0, 0) (OX, OY) radius corner_r
  fix o
}
extrude LidBlock from LidSk distance lid_h new -> body Lid
sketch LidBandSk on XY offset z0L {
  rect rounded o = (-rim_out, -rim_out) (OX + rim_out, OY + rim_out) radius (corner_r + rim_out)
  fix o
}
extrude LidBand from LidBandSk distance rim_h join targets Lid
sketch LidSkirtSk on XY offset (z0L + rim_h - 0.2 mm) {
  rect rounded o = (-rim_out + 0.3 mm, -rim_out + 0.3 mm) (OX + rim_out - 0.3 mm, OY + rim_out - 0.3 mm) radius (corner_r + rim_out - 0.3 mm)
  fix o
}
extrude LidSkirt from LidSkirtSk distance (rim_out + 0.2 mm) taper (-45 deg) join targets Lid

// Lid knuckles 1, 3, their webs mirrored above the parting line
for i from 0 to 1 {
  sketch LidWebSk{i} on YZ offset (x_h0 + (1 + 2*i)*pitch) {
    polyline closed w = (y0, z0L + 0.2 mm) (OY - 0.5 mm, z0L + rim_h) (OY - 0.5 mm, z_a + 0.85*r45 + drop) (y_a + 0.85*r45, z_a + 0.85*r45) (y_a, z0L + 0.2 mm)
    fix w
  }
  extrude LidWeb{i} from LidWebSk{i} distance k new -> body LidHinge{i}
  cylinder LidKnuckle{i} radius knuckle_r height k base (x_h0 + (1 + 2*i)*pitch, y_a, z_a) axis X join targets LidHinge{i}
  cylinder PinBoreL{i} radius (pin_d/2 + pin_clear) height (k + 2 mm) base (x_h0 + (1 + 2*i)*pitch - 1 mm, y_a, z_a) axis X cut targets LidHinge{i}
  combine JoinLidHinge{i} target Lid tools LidHinge{i} join
}

// Latch arms: root on the lid's top edge, arm outside the flange, lip and pull tab at the foot
box Root0 size latch_w (arm_t + rim_out + gap + 1 mm) 3 mm corner (xl0 - latch_w/2, ya0, zT - 3 mm) join targets Lid
box Arm0 size latch_w arm_t (zT - z_lip0) corner (xl0 - latch_w/2, ya0, z_lip0) join targets Lid
box Lip0 size latch_w (-rim_out + catch_d - ya0) lip_t corner (xl0 - latch_w/2, ya0 + 0.1 mm, z_lip0) join targets Lid
box Tab0 size latch_w 3.5 mm lip_t corner (xl0 - latch_w/2, ya0 - 3 mm, z_lip0) join targets Lid
box Root1 size latch_w (arm_t + rim_out + gap + 1 mm) 3 mm corner (xl1 - latch_w/2, ya0, zT - 3 mm) join targets Lid
box Arm1 size latch_w arm_t (zT - z_lip0) corner (xl1 - latch_w/2, ya0, z_lip0) join targets Lid
box Lip1 size latch_w (-rim_out + catch_d - ya0) lip_t corner (xl1 - latch_w/2, ya0 + 0.1 mm, z_lip0) join targets Lid
box Tab1 size latch_w 3.5 mm lip_t corner (xl1 - latch_w/2, ya0 - 3 mm, z_lip0) join targets Lid
// Lead-ins: the lip's inner bottom edge chamfered back past the band's face
chamfer LeadIn edges (xl0, -rim_out + catch_d, z_lip0) (xl1, -rim_out + catch_d, z_lip0) distance (catch_d + gap) body Lid

// Stacking pockets for the next box's feet
box Pocket0 size (foot + 2*stack_clear) (foot + 2*stack_clear) (foot_h + stack_clear + 1 mm) corner (foot_in - stack_clear, foot_in - stack_clear, zT - foot_h - stack_clear) cut targets Lid
box Pocket1 size (foot + 2*stack_clear) (foot + 2*stack_clear) (foot_h + stack_clear + 1 mm) corner (OX - foot_in - foot - stack_clear, foot_in - stack_clear, zT - foot_h - stack_clear) cut targets Lid
box Pocket2 size (foot + 2*stack_clear) (foot + 2*stack_clear) (foot_h + stack_clear + 1 mm) corner (foot_in - stack_clear, OY - foot_in - foot - stack_clear, zT - foot_h - stack_clear) cut targets Lid
box Pocket3 size (foot + 2*stack_clear) (foot + 2*stack_clear) (foot_h + stack_clear + 1 mm) corner (OX - foot_in - foot - stack_clear, OY - foot_in - foot - stack_clear, zT - foot_h - stack_clear) cut targets Lid

// Hollow the lid
sketch LidCavSk on XY offset (z0L - 1 mm) {
  rect rounded o = (wall, wall) (OX - wall, OY - wall) radius (corner_r - wall)
  fix o
}
extrude LidCavity from LidCavSk distance (lid_d + 1 mm) cut targets Lid

// Gasket groove in the lid's rim: a ring tool cut from the lid only
sketch GrooveOutSk on XY offset (z0L - 0.5 mm) {
  rect rounded o = (g_out, g_out) (OX - g_out, OY - g_out) radius (corner_r - g_out)
  fix o
}
sketch GrooveInSk on XY offset (z0L - 1 mm) {
  rect rounded o = (g_in, g_in) (OX - g_in, OY - g_in) radius (corner_r - g_in)
  fix o
}
extrude GrooveTool from GrooveOutSk distance (groove_d + 0.5 mm) new -> body GrooveRing
extrude GrooveHole from GrooveInSk distance (groove_d + 2 mm) cut targets GrooveRing
combine Groove target Lid tools GrooveRing cut

// ==== The hinge pin (the knuckles were bored before they joined)
cylinder PinRod radius (pin_d/2) height hinge_l base (x_h0, y_a, z_a) axis X new -> body Pin

// ==== Checks
check Lid clearance Base >= (seat - 0.05 mm)
check Pin clearance Base >= (pin_clear - 0.05 mm)
check Pin clearance Lid >= (pin_clear - 0.05 mm)
check Base shells == 1
check Lid shells == 1
check Base size.x <= 250 mm
check Base size.y <= 250 mm

color Base "#3a3f44"
color Lid "#f2a516"
color Pin "#9aa0a6"
material Base, Lid PLA
material Pin Steel
