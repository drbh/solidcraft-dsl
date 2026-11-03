// 136. Compact cassette at true size, as a working shell you can load with tape: two halves on five
// screws, two hubs on posts, guide rollers, a pressure-pad slot and a window.
//
// Bill of materials
//   3.81 mm audio cassette tape (from a donor cassette, or new pancake tape), about 90 m for a C60
//   1  pressure pad: a 0.15 mm phosphor-bronze leaf about 15 x 3.5 mm with a 4 x 3 x 2 mm felt pad,
//      from a donor cassette
//   2  slip sheets, 0.1 mm PTFE or PET film cut to the reel bay outline (optional; they quieten the packs)
//   5  M2 x 8 self-tapping screws
//
// The cassette lies A side up: X along its length (0 at the middle), Y front to back (y = 0 is the
// front edge with the head openings), Z through it; the B half sits on z = 0 and the halves meet at
// z = split. The tape runs in a 4.6 mm high cavity centred on the split: off the left pack, round
// the left roller, along the front behind the openings (over two guide pins and past the pressure
// pad at the head), round the right roller and onto the right pack.
//
// How it goes together. Hook each end of the tape leader into its hub's slot and press a Clip in
// over it. Drop the Hubs over the B half's posts (each has a counterbore that rides on the post),
// the Rollers onto their pins, and slide the pressure pad's leaf into the two slotted posts behind
// the head opening, its felt facing the front. Lead the tape round the rollers and in front of the
// guide pins and the pad. Put the A half on (its posts go into the hubs' other counterbores) and
// drive the five screws from the B side. The Windows press into recesses in each face.
//
// The deck drives the hubs through the 12.6 mm spindle holes: six teeth inside each hub, sized for
// a common spindle (check yours, they are params). The capstans come up through the two capstan
// holes behind the tape, the pinch rollers and heads in through the front openings. There are no
// erase-protect cavities on the back edge, so decks see the tabs as present: it records.
//
// Printing, without supports: both halves outside face down, the hubs, rollers and clips flat. PLA
// or PETG at 0.12 mm layers for the pins and teeth; the windows in clear PETG (or cut from 0.75 mm
// clear sheet). The engine has no PETG material, so they are given as PLA.
design CompactCassette units mm

// Shell
param L = 100.4 mm
param D = 63.8 mm
param T = 12 mm
param split = 6 mm
param shell_r = 1.5 mm
param cav_h = 4.6 mm           // tape cavity height, centred on the split
param front_t = 1.4 mm         // front wall

// Reels
param hub_dx = 42.5 mm         // between the hub centres
param hub_y = 34.5 mm
param pack_r = 25 mm           // the reel bay round each hub (a full C90 pack is about 24)
param hub_r = 10.75 mm         // tape core
param spindle_r = 6.3 mm       // the shell's spindle holes
param post_r = 7.3 mm          // the annular post round each spindle hole
param post_h = 1.2 mm
param cb_r = 7.6 mm            // the hubs' counterbores over the posts
param cb_d = 1.4 mm
param bore_r = 4 mm            // the hub's drive bore
param tooth_d = 0.9 mm         // six teeth into the bore
param tooth_w = 1.2 mm
param hub_gap = 0.15 mm        // between a hub's face and the cavity floor

// Tape path
param tape_y = 3.5 mm          // the line the tape runs along behind the front openings
param tape_w = 3.81 mm
param roller_x = 44 mm
param roller_r = 2 mm
param roller_pin_r = 1 mm
param roller_fit = 0.25 mm
param guide_x = 11 mm
param guide_r = 1 mm
param pad_x = 8.1 mm           // the pressure-pad leaf's two slotted posts
param pad_slot_y = 6.9 mm
param capstan_x = 22.5 mm
param capstan_r = 1.8 mm

// Front openings (centre for the head, the erase head's pair, the pinch rollers)
param head_w = 14 mm
param erase_x = 14 mm
param erase_w = 5 mm
param pinch_w = 9 mm

// Window
param win_w = 25 mm
param win_h = 12 mm
param win_t = 0.75 mm
param win_lip = 1 mm

// Screws
param screw_r = 1.15 mm
param head_r = 2 mm
param pilot_r = 0.8 mm
param boss_r = 2.4 mm

let hx = hub_dx/2
let cav_z = split - cav_h/2
let cav_top = split + cav_h/2
let hub_z = cav_z + hub_gap
let hub_top = cav_top - hub_gap
let tape_z = split - tape_w/2

// ==== Both halves start as slabs; each is hollowed by its own copy of the cavity tool
sketch ShellSk on XY {
  rect rounded o = (-L/2, 0) (L/2, D) radius shell_r
  fix o
}
extrude BBlock from ShellSk distance split new -> body ShellB
extrude ABlock from ShellSk distance (T - split) offset split new -> body ShellA

// The cavity: a bay round each pack, the space between them, and the front tape path
sketch CavitySk on XY offset cav_z {
  circle l = (-hx, hub_y) radius pack_r
  circle r = (hx, hub_y) radius pack_r
  rect mid = (-hx, hub_y) (hx, hub_y + 14 mm)
  rect front = (-roller_x - 2.5 mm, front_t) (roller_x + 2.5 mm, hub_y)
  fix l r mid front
}
for t from 0 to 1 {
  extrude Cavity{t} from CavitySk distance cav_h new -> body Tool{t}
  // What stands in the cavity is taken out of the tool: the spindle posts, screw bosses, and the
  // pressure pad's two slotted posts
  cylinder PostB{t}0 radius post_r height (post_h + 0.5 mm) base (-hx, hub_y, cav_z - 0.5 mm) axis Z cut targets Tool{t}
  cylinder PostB{t}1 radius post_r height (post_h + 0.5 mm) base (hx, hub_y, cav_z - 0.5 mm) axis Z cut targets Tool{t}
  cylinder PostA{t}0 radius post_r height (post_h + 0.5 mm) base (-hx, hub_y, cav_top - post_h) axis Z cut targets Tool{t}
  cylinder PostA{t}1 radius post_r height (post_h + 0.5 mm) base (hx, hub_y, cav_top - post_h) axis Z cut targets Tool{t}
  cylinder BossL{t} radius boss_r height (cav_h + 1 mm) base (-34 mm, 9.5 mm, cav_z - 0.5 mm) axis Z cut targets Tool{t}
  cylinder BossR{t} radius boss_r height (cav_h + 1 mm) base (34 mm, 9.5 mm, cav_z - 0.5 mm) axis Z cut targets Tool{t}
  cylinder BossC{t} radius boss_r height (cav_h + 1 mm) base (0, 12 mm, cav_z - 0.5 mm) axis Z cut targets Tool{t}
  box PadPostL{t} size 2.2 mm 2.8 mm (cav_h + 1 mm) corner (-pad_x - 1.1 mm, 5.8 mm, cav_z - 0.5 mm) cut targets Tool{t}
  box PadPostR{t} size 2.2 mm 2.8 mm (cav_h + 1 mm) corner (pad_x - 1.1 mm, 5.8 mm, cav_z - 0.5 mm) cut targets Tool{t}
}
combine HollowB target ShellB tools Tool0 cut
combine HollowA target ShellA tools Tool1 cut

// Slots in the pad posts for the leaf, open towards the middle
box PadSlotLB size 2.2 mm 0.4 mm (cav_h + 1 mm) corner (-pad_x - 0.9 mm, pad_slot_y - 0.2 mm, cav_z - 0.5 mm) cut targets ShellB
box PadSlotRB size 2.2 mm 0.4 mm (cav_h + 1 mm) corner (pad_x - 1.3 mm, pad_slot_y - 0.2 mm, cav_z - 0.5 mm) cut targets ShellB
box PadSlotLA size 2.2 mm 0.4 mm (cav_h + 1 mm) corner (-pad_x - 0.9 mm, pad_slot_y - 0.2 mm, cav_z - 0.5 mm) cut targets ShellA
box PadSlotRA size 2.2 mm 0.4 mm (cav_h + 1 mm) corner (pad_x - 1.3 mm, pad_slot_y - 0.2 mm, cav_z - 0.5 mm) cut targets ShellA

// Spindle holes through both halves, and the two capstan holes
sketch HolesSk on XY offset (-0.5 mm) {
  circle l = (-hx, hub_y) radius spindle_r
  circle r = (hx, hub_y) radius spindle_r
  circle cl = (-capstan_x, tape_y + 1.2 mm) radius capstan_r
  circle cr = (capstan_x, tape_y + 1.2 mm) radius capstan_r
  fix l r cl cr
}
extrude HolesB from HolesSk distance (split + 1 mm) cut targets ShellB
extrude HolesA from HolesSk distance (T + 1 mm) cut targets ShellA

// Front openings: notches through the front wall, a little deeper than the cavity
box HeadOpenB size head_w (front_t + 1.5 mm) (split - cav_z + 2.2 mm) corner (-head_w/2, -1 mm, cav_z - 1.7 mm) cut targets ShellB
box HeadOpenA size head_w (front_t + 1.5 mm) (cav_top - split + 2.2 mm) corner (-head_w/2, -1 mm, split - 0.5 mm) cut targets ShellA
for s from 0 to 1 {
  box EraseB{s} size erase_w (front_t + 1.5 mm) (split - cav_z + 1 mm) corner ((2*s - 1)*erase_x - erase_w/2, -1 mm, cav_z - 0.5 mm) cut targets ShellB
  box EraseA{s} size erase_w (front_t + 1.5 mm) (cav_top - split + 1 mm) corner ((2*s - 1)*erase_x - erase_w/2, -1 mm, split - 0.5 mm) cut targets ShellA
  box PinchB{s} size pinch_w (front_t + 1.5 mm) (split - cav_z + 2.2 mm) corner ((2*s - 1)*capstan_x - pinch_w/2, -1 mm, cav_z - 1.7 mm) cut targets ShellB
  box PinchA{s} size pinch_w (front_t + 1.5 mm) (cav_top - split + 2.2 mm) corner ((2*s - 1)*capstan_x - pinch_w/2, -1 mm, split - 0.5 mm) cut targets ShellA
}

// Pins in the B half: two for the rollers, two fixed guides the tape slides over
for s from 0 to 1 {
  cylinder RollerPin{s} radius roller_pin_r height (cav_h - 0.3 mm + 0.5 mm) base ((2*s - 1)*roller_x, tape_y + roller_r, cav_z - 0.5 mm) axis Z join targets ShellB
  cylinder GuidePin{s} radius guide_r height (cav_h - 0.3 mm + 0.5 mm) base ((2*s - 1)*guide_x, tape_y + guide_r, cav_z - 0.5 mm) axis Z join targets ShellB
}

// Windows: an opening through each face over the packs, with a recess outside for the insert
sketch WinSk on XY offset (-0.5 mm) {
  rect rounded w = (-win_w/2, hub_y - win_h/2) (win_w/2, hub_y + win_h/2) radius 3 mm
  fix w
}
sketch WinRecessSk on XY offset (-0.5 mm) {
  rect rounded w = (-win_w/2 - win_lip, hub_y - win_h/2 - win_lip) (win_w/2 + win_lip, hub_y + win_h/2 + win_lip) radius 4 mm
  fix w
}
extrude WinB from WinSk distance (cav_z + 1 mm) cut targets ShellB
extrude WinRecessB from WinRecessSk distance (win_t + 0.55 mm) cut targets ShellB
sketch WinASk on XY offset (T + 0.5 mm) {
  rect rounded w = (-win_w/2, hub_y - win_h/2) (win_w/2, hub_y + win_h/2) radius 3 mm
  fix w
}
sketch WinRecessASk on XY offset (T + 0.5 mm) {
  rect rounded w = (-win_w/2 - win_lip, hub_y - win_h/2 - win_lip) (win_w/2 + win_lip, hub_y + win_h/2 + win_lip) radius 4 mm
  fix w
}
extrude WinA from WinASk distance (T - cav_top + 1 mm) negative cut targets ShellA
extrude WinRecessA from WinRecessASk distance (win_t + 0.55 mm) negative cut targets ShellA

// ==== Screws: clearance and counterbores in the B half, pilots in the A half
sketch ScrewSk on XY offset (-0.5 mm) {
  circle a = (-47 mm, 59 mm) radius screw_r
  circle b = (47 mm, 59 mm) radius screw_r
  circle c = (-34 mm, 9.5 mm) radius screw_r
  circle d = (34 mm, 9.5 mm) radius screw_r
  circle e = (0, 12 mm) radius screw_r
  fix a b c d e
}
extrude ScrewHoles from ScrewSk distance (split + 1 mm) cut targets ShellB
sketch HeadSk on XY offset (-0.5 mm) {
  circle a = (-47 mm, 59 mm) radius head_r
  circle b = (47 mm, 59 mm) radius head_r
  circle c = (-34 mm, 9.5 mm) radius head_r
  circle d = (34 mm, 9.5 mm) radius head_r
  circle e = (0, 12 mm) radius head_r
  fix a b c d e
}
extrude Heads from HeadSk distance 2.3 mm cut targets ShellB
sketch PilotSk on XY offset (split - 0.5 mm) {
  circle a = (-47 mm, 59 mm) radius pilot_r
  circle b = (47 mm, 59 mm) radius pilot_r
  circle c = (-34 mm, 9.5 mm) radius pilot_r
  circle d = (34 mm, 9.5 mm) radius pilot_r
  circle e = (0, 12 mm) radius pilot_r
  fix a b c d e
}
extrude Pilots from PilotSk distance 5.5 mm cut targets ShellA

// ==== Hubs: a tape core with a counterbore each side over the posts, six teeth in the middle,
// and a slot in the rim for the leader and its clip
for s from 0 to 1 {
  cylinder HubCore{s} radius hub_r height (hub_top - hub_z) base ((2*s - 1)*hx, hub_y, hub_z) axis Z new -> body Hub{s}
  cylinder HubCbB{s} radius cb_r height (cb_d + 0.5 mm) base ((2*s - 1)*hx, hub_y, hub_z - 0.5 mm) axis Z cut targets Hub{s}
  cylinder HubCbA{s} radius cb_r height (cb_d + 0.5 mm) base ((2*s - 1)*hx, hub_y, hub_top - cb_d) axis Z cut targets Hub{s}
  cylinder HubBore{s} radius bore_r height (hub_top - hub_z + 1 mm) base ((2*s - 1)*hx, hub_y, hub_z - 0.5 mm) axis Z cut targets Hub{s}
  box HubSlot{s} size 2 mm 2.9 mm (hub_top - hub_z + 1 mm) corner ((2*s - 1)*hx - 1 mm, hub_y + hub_r - 2.4 mm, hub_z - 0.5 mm) cut targets Hub{s}
  box Clip{s}Body size 1.9 mm 2.6 mm (hub_top - hub_z - 0.4 mm) corner ((2*s - 1)*hx - 0.95 mm, hub_y + hub_r - 2.4 mm, hub_z + 0.2 mm) new -> body Clip{s}
}
for s from 0 to 1 {
  for k from 0 to 5 {
    sketch ToothSk{s}{k} on XY offset (hub_z + cb_d - 0.15 mm) {
      polyline closed p = ((2*s - 1)*hx + (bore_r - tooth_d)*cos(60*k deg + 30 deg) - tooth_w/2*sin(60*k deg + 30 deg), hub_y + (bore_r - tooth_d)*sin(60*k deg + 30 deg) + tooth_w/2*cos(60*k deg + 30 deg)) ((2*s - 1)*hx + (bore_r - tooth_d)*cos(60*k deg + 30 deg) + tooth_w/2*sin(60*k deg + 30 deg), hub_y + (bore_r - tooth_d)*sin(60*k deg + 30 deg) - tooth_w/2*cos(60*k deg + 30 deg)) ((2*s - 1)*hx + (bore_r + 0.3 mm)*cos(60*k deg + 30 deg) + tooth_w/2*sin(60*k deg + 30 deg), hub_y + (bore_r + 0.3 mm)*sin(60*k deg + 30 deg) - tooth_w/2*cos(60*k deg + 30 deg)) ((2*s - 1)*hx + (bore_r + 0.3 mm)*cos(60*k deg + 30 deg) - tooth_w/2*sin(60*k deg + 30 deg), hub_y + (bore_r + 0.3 mm)*sin(60*k deg + 30 deg) + tooth_w/2*cos(60*k deg + 30 deg))
      fix p
    }
    extrude Tooth{s}{k} from ToothSk{s}{k} distance (hub_top - hub_z - 2*cb_d + 0.3 mm) join targets Hub{s}
  }
}

// ==== Rollers on the pins
for s from 0 to 1 {
  cylinder RollerBody{s} radius roller_r height (cav_h - 0.4 mm) base ((2*s - 1)*roller_x, tape_y + roller_r, cav_z + 0.2 mm) axis Z new -> body Roller{s}
  cylinder RollerBore{s} radius (roller_pin_r + roller_fit) height (cav_h + 1 mm) base ((2*s - 1)*roller_x, tape_y + roller_r, cav_z - 0.5 mm) axis Z cut targets Roller{s}
}

// ==== Window inserts
sketch WinPlateBSk on XY offset 0.025 mm {
  rect rounded w = (-win_w/2 - win_lip + 0.15 mm, hub_y - win_h/2 - win_lip + 0.15 mm) (win_w/2 + win_lip - 0.15 mm, hub_y + win_h/2 + win_lip - 0.15 mm) radius 3.85 mm
  fix w
}
extrude WindowBPlate from WinPlateBSk distance win_t new -> body WindowB
sketch WinPlateASk on XY offset (T - win_t - 0.025 mm) {
  rect rounded w = (-win_w/2 - win_lip + 0.15 mm, hub_y - win_h/2 - win_lip + 0.15 mm) (win_w/2 + win_lip - 0.15 mm, hub_y + win_h/2 + win_lip - 0.15 mm) radius 3.85 mm
  fix w
}
extrude WindowAPlate from WinPlateASk distance win_t new -> body WindowA

// ==== The tape, placed (not printed): a full pack on the left, a part one on the right, the run
cylinder PackL radius 22 mm height tape_w base (-hx, hub_y, tape_z) axis Z new -> body TapeL
cylinder PackLCore radius (hub_r + 0.05 mm) height (tape_w + 1 mm) base (-hx, hub_y, tape_z - 0.5 mm) axis Z cut targets TapeL
cylinder PackR radius 13 mm height tape_w base (hx, hub_y, tape_z) axis Z new -> body TapeR
cylinder PackRCore radius (hub_r + 0.05 mm) height (tape_w + 1 mm) base (hx, hub_y, tape_z - 0.5 mm) axis Z cut targets TapeR
box TapeRunBox size (2*roller_x) 0.05 mm tape_w corner (-roller_x, tape_y - 0.06 mm, tape_z) new -> body TapeRun

// ==== Fits
check Hub0 clearance ShellB >= 0.14 mm
check Hub0 clearance ShellA >= 0.14 mm
check Hub1 clearance ShellB >= 0.14 mm
check Roller0 clearance ShellB >= 0.19 mm
check Roller1 clearance ShellA >= 0.19 mm
check WindowB clearance ShellB >= 0.02 mm
check WindowA clearance ShellA >= 0.02 mm
check TapeL clearance ShellB >= 0.3 mm
check TapeRun clearance ShellA >= 0.01 mm

// ==== Finish
color ShellA, ShellB "#1e1f22"
color Hub0, Hub1, Clip0, Clip1 "#f2f2ee"
color Roller0, Roller1 "#e8e8e8"
color WindowA, WindowB "#cfe3ea"
color TapeL, TapeR, TapeRun "#4a2e1c"
material ShellA, ShellB, Hub0, Hub1, Clip0, Clip1, Roller0, Roller1, WindowA, WindowB PLA
