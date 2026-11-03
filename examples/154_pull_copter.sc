// 154. Pull-string flying propeller: a rack strip spins a pinion, a one-way dog launches the rotor.
//
// Z is up, the spindle axis is the Z axis. A grip carries a gearbox housing on top. Inside it a
// 12-tooth pinion turns on a pivot pin standing up from the housing floor. A toothed strip (the
// "string") runs through a slot in the housing past the pinion. A slide-in cap plate holds the
// pinion down and the strip in, with two M3 screws. Above the cap the spindle flares into a
// 45 deg head, and two ramped dogs on it drive the rotor through two slots in its hub.
//
// To fly: feed the strip's free end (away from the ring) into the slot from the +X side, teeth
// toward the pinion, until it is in to the ring; set the rotor on the head, then pull the ring hard along +X. The pinion turns CCW (seen
// from above), the dogs' vertical faces push the rotor round, and when the strip runs out the
// rotor overruns the head: the slots ride up the dogs' 45 deg ramps and the rotor climbs away.
// Seen from above it spins CCW, leading edges high, so it pushes air down.
//
// Gearing: module `gm` straight-flank teeth (a 20 deg trapezoid approximation of an involute, fine
// for a hand-pulled toy), thinned by `backlash`. A 12-tooth pinion would undercut against a full
// rack, so the rack's teeth are shortened to rack_add*gm and its pitch line moved out by
// rack_shift (a profile shift). Checked in 2D over a full tooth pitch of rotation: the flanks
// never come closer than 0.2 mm. The rack pitch is pi*gm; `teeth_n` teeth give
// teeth_n/12 turns of the pinion per pull (3 turns with the defaults). The rotor's `blades`,
// `pitch`, `chord` and `blade_l` drive its shape; the hub and dog slots are fixed.
//
// Hardware: 2 x M3 x 10 self-tapping screws for the cap plate.
//
// Printing (PLA or PETG, 0.2 mm, 3 perimeters):
// - Handle: upright on the grip's end, as modelled. The housing sits on a 45 deg flare; the strip
//   slot is open at the top (the cap closes it), so nothing bridges.
// - Spindle: upright on the pinion's face. The pivot socket ends in a 45 deg cone and the head is
//   a 45 deg flare, so it needs no support.
// - Cap plate and strip: flat. The strip's teeth lie in the bed plane.
// - Rotor: hub face down. The pitched blades overhang the bed by `pitch`; print them on
//   supports from the build plate only (tree supports peel off the flat blade undersides).
design PullCopter units mm

// Rotor
param blades = 3
param pitch = 18 deg           // blade angle; leading edge high
param blade_l = 38 mm          // from the hub's edge to the tip
param chord = 16 mm
param blade_t = 1.2 mm
// Gearing
param gm = 1.5 mm              // module
param backlash = 0.4 mm
param rack_shift = 0.5 mm      // the strip's pitch line sits this far outside the pinion's pitch circle
param rack_add = 0.7           // rack tooth addendum, x gm (shortened so the 12-tooth pinion doesn't undercut)
param face = 7 mm              // pinion width
param teeth_n = 36             // teeth on the strip
// Fits
param fit = 0.3 mm             // general running clearance
param slot_clear = 0.4 mm      // strip in its slot, dogs in the hub

let zp = 12                    // pinion teeth (the strip's tooth phase assumes 12)
let rp = gm*zp/2
let ra = rp + gm
let rf = rp - 1.25*gm
let rack_p = pi*gm
let s_half = (rack_p/2 - backlash/2)/2      // half tooth thickness at the pitch line
let tn = tan(20 deg)

// Housing and spindle levels
let floor_t = 3 mm
let pin_z0 = floor_t + fit                  // pinion's lower face
let pin_z1 = pin_z0 + face
let hz = pin_z1 + 0.5 mm                    // housing top = cap underside
let cap_t = 3 mm
let head_z0 = hz + cap_t + 0.4 mm           // head flare starts
let head_top = head_z0 + 8 mm               // the 45 deg flare, 8 mm tall
let dog_h = 3 mm
let hub_z0 = head_top + fit
let hub_h = 7 mm
let hub_r = 12 mm

// Strip: its pitch line runs along y = -(rp + rack_shift)
let strip_z0 = pin_z0 + face/2 - 2 mm
let strip_t = 4 mm
let y_pitch = -rp - rack_shift
let y_tip = y_pitch + rack_add*gm
let y_root = y_pitch - 1.25*gm
let y_back = y_root - 5 mm
let x0 = rack_p/2 - 5*rack_p - rack_p/2 - 0.8 mm   // strip's plain end, just past the housing
let strip_end = rack_p/2 + (teeth_n - 6)*rack_p + 12 mm
let ring_c = strip_end + 14 mm

// ==== Handle: grip, 45 deg flare, gearbox housing
cylinder Grip radius 12 mm height 90 mm base (0, -3 mm, -102 mm) axis Z new -> body Handle
// (a revolve about the grip's axis, x = 0, y = -3; YZ sketch: u = y, v = z)
sketch FlareSk on YZ offset 0 {
  construction line ax = (-3 mm, -20 mm) (-3 mm, 5 mm)
  polyline closed f = (-3 mm, -13.5 mm) (8.5 mm, -13.5 mm) (22 mm, 0) (-3 mm, 0)
  fix ax f
}
revolve Flare from FlareSk axis ax new -> body FlareTool
combine FlareJoin target Handle tools FlareTool join
box Housing size 36 mm 34 mm hz corner (-18 mm, -20 mm, -0.01 mm) join targets Handle
cylinder Cavity radius (ra + 1 mm) height hz base (0, 0, floor_t) axis Z cut targets Handle
box StripSlot size 40 mm (y_tip + 0.6 mm - (y_back - slot_clear)) (hz) corner (-20 mm, y_back - slot_clear, strip_z0 - fit) cut targets Handle
cylinder Pivot radius 2.5 mm height (pin_z0 + 4.7 mm - floor_t + 0.5 mm) base (0, 0, floor_t - 0.5 mm) axis Z join targets Handle
cylinder Pilot1 radius 1.25 mm height 9 mm base (14.5 mm, 10 mm, hz - 8.99 mm) axis Z cut targets Handle
cylinder Pilot2 radius 1.25 mm height 9 mm base (-14.5 mm, -18.1 mm, hz - 8.99 mm) axis Z cut targets Handle

// ==== Cap plate: slides in round the shaft (slot open to +Y), two screws
box CapPlate size 36 mm 34 mm cap_t corner (-18 mm, -20 mm, hz) new -> body Cap
box CapSlot size (6 mm + 2*0.4 mm) 20 mm (cap_t + 2 mm) corner (-3.4 mm, 0, hz - 1 mm) cut targets Cap
cylinder CapCentre radius 3.4 mm height (cap_t + 2 mm) base (0, 0, hz - 1 mm) axis Z cut targets Cap
cylinder CapHole1 radius 1.7 mm height (cap_t + 2 mm) base (14.5 mm, 10 mm, hz - 1 mm) axis Z cut targets Cap
cylinder CapHole2 radius 1.7 mm height (cap_t + 2 mm) base (-14.5 mm, -18.1 mm, hz - 1 mm) axis Z cut targets Cap

// ==== Spindle: pinion, shaft, flared head with two ramped dogs and a pilot boss
cylinder PinionRoot radius rf height face base (0, 0, pin_z0) axis Z new -> body Spindle
sketch ToothSk on XY offset pin_z0 {
  polyline closed p = (rf - 0.5 mm, -(s_half + (rp - rf + 0.5 mm)*tn)) (ra, -(s_half - gm*tn)) (ra, s_half - gm*tn) (rf - 0.5 mm, s_half + (rp - rf + 0.5 mm)*tn)
  fix p
}
extrude Tooth from ToothSk distance face join targets Spindle
pattern circular Teeth features Tooth origin (0,0,0) dir (0,0,1) count zp angle 360 deg
cylinder Shaft radius 3 mm height (head_z0 - pin_z1 + 0.5 mm) base (0, 0, pin_z1 - 0.5 mm) axis Z join targets Spindle
// (the flare starts 0.1 mm inside the shaft's radius, so its base circle doesn't lie on the shaft)
cone HeadFlare radius 2.9 mm top_radius 10.9 mm height 8 mm base (0, 0, head_z0) axis Z join targets Spindle
cylinder PilotBoss radius 2 mm height (dog_h + 0.5 mm + 0.1 mm) base (0, 0, head_top - 0.1 mm) axis Z join targets Spindle
// Dog at +X: vertical drive face at y = 0 facing +Y (the CCW direction there), 45 deg ramp toward -Y
sketch DogSk on YZ offset 3.5 mm {
  polyline closed d = (0, head_top - 0.1 mm) (0, head_top + dog_h) (-dog_h - 0.1 mm, head_top - 0.1 mm)
  fix d
}
extrude Dog from DogSk distance 4.5 mm join targets Spindle
pattern circular Dogs features Dog origin (0,0,0) dir (0,0,1) count 2 angle 360 deg
// Pivot socket in the pinion's face, ending in a 45 deg cone
cylinder Socket radius (2.5 mm + fit) height 5.5 mm base (0, 0, pin_z0 - 0.5 mm) axis Z cut targets Spindle
cone SocketTip radius 2.8 mm top_radius 0.3 mm height 2.5 mm base (0, 0, pin_z0 + 4.99 mm) axis Z cut targets Spindle

// ==== Strip: a flat bar with rack teeth on its +Y edge, plain end in the housing, ring at the far end
box StripBar size (strip_end - x0) (y_root - y_back + 0.5 mm) strip_t corner (x0, y_back, strip_z0) new -> body Strip
sketch RackSk on XY offset strip_z0 {
  polyline closed p = (rack_p/2 - 5*rack_p - (s_half + (1.25*gm + 0.5 mm)*tn), y_root - 0.5 mm) (rack_p/2 - 5*rack_p - (s_half - rack_add*gm*tn), y_tip) (rack_p/2 - 5*rack_p + (s_half - rack_add*gm*tn), y_tip) (rack_p/2 - 5*rack_p + (s_half + (1.25*gm + 0.5 mm)*tn), y_root - 0.5 mm)
  fix p
}
extrude RackTooth from RackSk distance strip_t join targets Strip
pattern RackTeeth features RackTooth dir (1,0,0) count teeth_n spacing rack_p
cylinder RingOuter radius 15 mm height strip_t base (ring_c, y_back + 3 mm, strip_z0) axis Z join targets Strip
cylinder RingInner radius 11 mm height (strip_t + 2 mm) base (ring_c, y_back + 3 mm, strip_z0 - 1 mm) axis Z cut targets Strip

// ==== Rotor: hub with two through-slots for the dogs and a pilot bore, pitched flat blades
cylinder Hub radius hub_r height hub_h base (0, 0, hub_z0) axis Z new -> body Rotor
cylinder HubBore radius (2 mm + slot_clear) height (hub_h + 2 mm) base (0, 0, hub_z0 - 1 mm) axis Z cut targets Rotor
box DogSlot size 5.3 mm (dog_h + 0.1 mm + 2*slot_clear) (hub_h + 2 mm) corner (3.1 mm, -dog_h - 0.1 mm - slot_clear, hub_z0 - 1 mm) cut targets Rotor
pattern circular DogSlots features DogSlot origin (0,0,0) dir (0,0,1) count 2 angle 360 deg
// Blade along +X: a chord x blade_t plate turned `pitch` about its radial centre line, leading edge (+Y) high
let bz = hub_z0 + hub_h/2
sketch BladeSk on YZ offset (hub_r - 0.5 mm) {
  polyline closed b = (-(chord/2)*cos(pitch) + (blade_t/2)*sin(pitch), bz - (chord/2)*sin(pitch) - (blade_t/2)*cos(pitch)) ((chord/2)*cos(pitch) + (blade_t/2)*sin(pitch), bz + (chord/2)*sin(pitch) - (blade_t/2)*cos(pitch)) ((chord/2)*cos(pitch) - (blade_t/2)*sin(pitch), bz + (chord/2)*sin(pitch) + (blade_t/2)*cos(pitch)) (-(chord/2)*cos(pitch) - (blade_t/2)*sin(pitch), bz - (chord/2)*sin(pitch) + (blade_t/2)*cos(pitch))
  fix b
}
extrude Blade from BladeSk distance (blade_l + 0.5 mm) join targets Rotor
pattern circular Blades features Blade origin (0,0,0) dir (0,0,1) count blades angle 360 deg

// ==== Fits, in the assembled pose
check Spindle clearance Handle >= (fit - 0.01 mm)
check Spindle clearance Cap >= (0.4 mm - 0.01 mm)
check Strip clearance Handle >= (fit - 0.01 mm)
check Strip clearance Cap >= 0.5 mm
check Strip clearance Spindle >= 0.2 mm
check Rotor clearance Spindle >= (fit - 0.01 mm)
check Handle overhang <= 45 deg
check Spindle overhang <= 45 deg
check Cap overhang <= 45 deg

color Handle, Cap "#3a7bd5"
color Spindle "#f5f5f5"
color Strip "#ff5a5f"
color Rotor "#ffd23f"
material Handle, Cap, Spindle, Strip, Rotor PLA
