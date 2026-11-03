// 135. Rugged square smartwatch in the style of the 1983 square shock-resistant digital: a hard case
// around a 1.3" IPS panel, an nRF52840 board and a LiPo, a stretch-on bezel, four side buttons and a
// strap with a pin buckle.
//
// Bill of materials (all sizes are params; measure yours)
//   1  1.3 in 240 x 240 ST7789 IPS panel, bare, with its 12-pin 0.8 mm FPC (26.2 x 29.2 x 1.5 mm)
//      and a 12-pin FPC breakout, which tucks in beside the board
//   1  Seeed XIAO nRF52840 (21 x 17.8 mm); it charges the LiPo through its USB-C
//   1  402025 LiPo, 3.7 V about 150 mAh (4 x 20 x 25 mm)
//   4  6 x 6 x 5 mm tactile switches, legs clipped short and wired
//   1  lens: 1 mm clear acrylic or polycarbonate, cut to the Lens outline (or printed in clear PETG)
//   4  M1.6 x 5 self-tapping screws for the back
//   3  16 mm spring bars, 1.78 mm: two hold the straps, one is the buckle's hinge
//
// The watch lies face up: X across (3 o'clock at +X), Y up the face (12 o'clock at +Y), Z out of
// the face; the back plate sits on z = 0. Case, bezel and back are one octagon, a square with
// its corners cut: each outline is that octagon grown or shrunk by a distance d (`let e_*` is
// its straight edges, `let f_*` where they end).
//
// How it goes together. The Case is the hard core: a tub, open at the back, with the lens recess
// and window in its face, the strap lugs, a bead round its waist and the button holes. Inside,
// four cradles hold the switches on edge against the side walls; their cheeks also locate the
// XIAO, which lies components down with its USB-C in the slot at 9 o'clock. The panel sits face up
// against the face's underside between four ribs; the LiPo lies on the back plate. Each Plunger
// goes in from inside before its switch: its flange is wider than the hole, so it can't fall
// out, and its tip stands proud of the bezel's guard. The Lens drops into the recess. The Bezel
// is stretched over the case: its groove snaps over the bead and its lip holds the lens down.
// The BackPlate's lip locates it inside the case and four screws close it.
// Straps: each strap's end tube goes between the lugs on a spring bar. The short strap's other
// end carries the buckle Frame on the third spring bar, which also passes through the Prong's
// ring in a notch in the tube. The Keeper slides over the short strap.
//
// Printing, all without supports: the Case and Bezel face down, the BackPlate outside down, the
// Plungers standing on their flanges, the straps, Frame and Keeper flat, the Prong on its side.
// Case, back, plungers, Frame and Prong in PETG (or PLA); Bezel, straps and Keeper in TPU 95A.
// The engine has no TPU or PETG material, so they are given as PLA here.
// The stack is 16.1 mm thick, about 2.5 mm more than the original, for the bought parts.
design RuggedWatch units mm

// Case: a square of half-width A with its corners cut by C
param A = 23 mm
param C = 8 mm
param wall = 2 mm
param face_t = 2 mm
param back_t = 1.6 mm
param case_top = 14.3 mm       // top of the case's face
param bead = 0.8 mm            // the waist bead the bezel snaps over
param bead_z = 4 mm
param bead_h = 1.5 mm

// Bezel (TPU)
param bez_t = 2.5 mm
param bez_bot = 2 mm
param bez_mid = 12 mm          // where the bezel's sides start to slope in
param bez_top = 16.1 mm
param bez_slope = 20 deg
param lip = 4 mm               // how far the lip reaches in over the face

// Lens
param lens_t = 1 mm
param recess = 2.8 mm          // the recess's inset from the case edge
param lens_fit = 0.2 mm

// Display panel
param panel_w = 26.2 mm
param panel_h = 29.2 mm
param panel_t = 1.5 mm
param active = 23.4 mm
param active_top = 1.4 mm      // from the panel's top edge to its active area
param win_y = 1 mm             // window centre

// Board and battery
param esp_w = 17.8 mm
param esp_l = 21 mm
param esp_t = 1.2 mm
param usb_w = 8.9 mm
param usb_l = 7.4 mm
param usb_t = 3.2 mm
param bat_w = 25 mm
param bat_h = 20 mm
param bat_t = 4 mm

// Buttons
param btn_y = 13 mm            // the four buttons at +-btn_y on both sides
param btn_z = 7.5 mm
param shaft_r = 1.5 mm
param flange_r = 2.7 mm
param fit = 0.2 mm
param proud = 0.7 mm           // how far a plunger stands out of its guard
param sw = 6 mm
param sw_body = 3.5 mm
param sw_plunger = 1.5 mm

// Lugs and straps
param strap_w = 15.8 mm
param lug_gap = 16.4 mm        // between the lugs' inner faces
param lug_t = 3 mm
param bar_y = 27 mm            // the spring bars, from the centre
param bar_z = 6 mm
param bar_r = 0.89 mm          // 1.78 mm spring bar
param tip_r = 0.45 mm
param strap_t = 3 mm
param short_l = 72 mm          // short strap, bar to buckle hinge
param long_l = 118 mm          // long strap, bar to tip
param hole_r = 1.1 mm
param hole_first = 30 mm       // first hole from the bar
param hole_pitch = 6.25 mm

// Screws
param screw_r = 0.9 mm         // M1.6 clearance
param pilot_r = 0.65 mm
param head_r = 1.6 mm

let q = sqrt(2) - 1
let e0 = A
let f0 = A - C
let e_in = A - wall
let f_in = A - C - wall*q
let e_bd = A + bead
let f_bd = A - C + bead*q
let e_bz = A + bez_t
let f_bz = A - C + bez_t*q
let e_rc = A - recess
let f_rc = A - C - recess*q
let e_ln = A - recess - lens_fit
let f_ln = A - C - (recess + lens_fit)*q
let e_lp = A - lip
let f_lp = A - C - lip*q
let e_lo = A - wall - 0.25 mm
let f_lo = A - C - (wall + 0.25 mm)*q
let e_li = A - wall - 1.45 mm
let f_li = A - C - (wall + 1.45 mm)*q

let face_in = case_top - face_t
let lens_z = case_top - lens_t - 0.05 mm
let panel_top = win_y + active/2 + active_top
let panel_z = face_in - panel_t - 0.05 mm
let esp_z = panel_z - 0.25 mm - esp_t          // the XIAO's board, components down
let usb_z = esp_z - usb_t                      // the bottom of its USB-C
let esp_x0 = -e_in + 0.6 mm                    // the board's USB end
let bat_z = back_t + 0.2 mm
let seat = e_in - 0.3 mm                      // where each pocket ends, short of the wall
let sw_in = seat - 1.2 mm - sw_plunger - sw_body   // the switch's back face: flange, 0.1 mm gap, switch
let boss_x = 15.5 mm
let boss_y = 18.5 mm
let lug_in = lug_gap/2
let short_end = bar_y + short_l

// ==== Case: an octagonal tub with a 2 mm face
sketch CaseSk on XY offset back_t {
  polyline closed o = (e0, -f0) (e0, f0) (f0, e0) (-f0, e0) (-e0, f0) (-e0, -f0) (-f0, -e0) (f0, -e0)
  fix o
}
extrude CaseBlock from CaseSk distance (case_top - back_t) new -> body Case
sketch CavitySk on XY offset (back_t - 0.5 mm) {
  polyline closed o = (e_in, -f_in) (e_in, f_in) (f_in, e_in) (-f_in, e_in) (-e_in, f_in) (-e_in, -f_in) (-f_in, -e_in) (f_in, -e_in)
  fix o
}
// The hollow is cut as one tool body, with the solid left standing in it taken out of the tool
// first: four switch cradles out from the side walls and four screw bosses in the corners
extrude Cavity from CavitySk distance (face_in - back_t + 0.5 mm) new -> body CavityTool
for i from 0 to 1 {
  for j from 0 to 1 {
    box Cradle{i}{j} size (e_in + 0.1 mm - 13.5 mm) (sw + 1.8 mm) (face_in + 0.1 mm - 4.1 mm) corner ((2*i - 1)*(e_in + 13.5 mm + 0.1 mm)/2 - (e_in + 0.1 mm - 13.5 mm)/2, (2*j - 1)*btn_y - sw/2 - 0.9 mm, 4.1 mm) cut targets CavityTool
    cylinder Boss{i}{j} radius 2.2 mm height (face_in - back_t + 2 mm) base ((2*i - 1)*boss_x, (2*j - 1)*boss_y, back_t - 1 mm) axis Z cut targets CavityTool
  }
}
combine Hollow target Case tools CavityTool cut

// The bead round the waist
sketch BeadSk on XY offset bead_z {
  polyline closed o = (e_bd, -f_bd) (e_bd, f_bd) (f_bd, e_bd) (-f_bd, e_bd) (-e_bd, f_bd) (-e_bd, -f_bd) (-f_bd, -e_bd) (f_bd, -e_bd)
  polyline closed i = (e0 - 0.3 mm, -f0) (e0 - 0.3 mm, f0) (f0, e0 - 0.3 mm) (-f0, e0 - 0.3 mm) (-(e0 - 0.3 mm), f0) (-(e0 - 0.3 mm), -f0) (-f0, -(e0 - 0.3 mm)) (f0, -(e0 - 0.3 mm))
  fix o i
}
extrude Bead from BeadSk distance bead_h join targets Case

// Lens recess and the display window
sketch RecessSk on XY offset (case_top + 0.5 mm) {
  polyline closed o = (e_rc, -f_rc) (e_rc, f_rc) (f_rc, e_rc) (-f_rc, e_rc) (-e_rc, f_rc) (-e_rc, -f_rc) (-f_rc, -e_rc) (f_rc, -e_rc)
  fix o
}
extrude Recess from RecessSk distance (lens_t + 0.6 mm) negative cut targets Case
sketch WindowSk on XY offset (case_top) {
  rect rounded w = (-active/2 - 0.1 mm, win_y - active/2 - 0.1 mm) (active/2 + 0.1 mm, win_y + active/2 + 0.1 mm) radius 1 mm
  fix w
}
extrude Window from WindowSk distance (face_t + 0.5 mm) negative cut targets Case

// Ribs round the panel on the face's underside (a gap in the bottom one for the FPC)
box RibTop size (panel_w + 2.4 mm) 1 mm 1.4 mm corner (-panel_w/2 - 1.2 mm, panel_top + 0.2 mm, face_in - 1.4 mm) join targets Case
box RibL size 1 mm (panel_h + 0.4 mm) 1.4 mm corner (-panel_w/2 - 1.2 mm, panel_top - panel_h - 0.2 mm, face_in - 1.4 mm) join targets Case
box RibR size 1 mm (panel_h + 0.4 mm) 1.4 mm corner (panel_w/2 + 0.2 mm, panel_top - panel_h - 0.2 mm, face_in - 1.4 mm) join targets Case
box RibBL size (panel_w/2 - 5 mm) 1 mm 1.4 mm corner (-panel_w/2 - 1.2 mm, panel_top - panel_h - 1.2 mm, face_in - 1.4 mm) join targets Case
box RibBR size (panel_w/2 - 5 mm) 1 mm 1.4 mm corner (6.2 mm, panel_top - panel_h - 1.2 mm, face_in - 1.4 mm) join targets Case

// Switch pockets in the cradles (a backstop and two cheeks each), open at the back for the switch
for i from 0 to 1 {
  for j from 0 to 1 {
    box Pocket{i}{j} size (seat - sw_in) (sw + 0.2 mm) (btn_z + sw/2 + 0.6 mm - 3.9 mm) corner ((2*i - 1)*(seat + sw_in)/2 - (seat - sw_in)/2, (2*j - 1)*btn_y - sw/2 - 0.1 mm, 3.9 mm) cut targets Case
    cylinder BtnHole{i}{j} radius (shaft_r + fit) height (wall + 1 mm) base ((2*i - 1)*(e_in - 0.5 mm), (2*j - 1)*btn_y, btn_z) axis ((2*i - 1), 0, 0) cut targets Case
  }
}

sketch PilotSk on XY offset (back_t - 0.5 mm) {
  circle a = (boss_x, boss_y) radius pilot_r
  circle b = (-boss_x, boss_y) radius pilot_r
  circle c = (-boss_x, -boss_y) radius pilot_r
  circle d = (boss_x, -boss_y) radius pilot_r
  fix a b c d
}
extrude Pilots from PilotSk distance 6 mm cut targets Case

// USB-C at 9 o'clock: a slot through the wall, the outside thinned so the plug reaches
sketch UsbSk on YZ offset (-e0 - 1 mm) {
  rect rounded u = (-usb_w/2 - 0.25 mm, usb_z - 0.2 mm) (usb_w/2 + 0.25 mm, usb_z + usb_t + 0.2 mm) radius 1.2 mm
  fix u
}
extrude UsbSlot from UsbSk distance (wall + 2 mm) cut targets Case
sketch PlugSk on YZ offset (-e0 - 1 mm) {
  rect rounded u = (-6.5 mm, usb_z + usb_t/2 - 3.75 mm) (6.5 mm, usb_z + usb_t/2 + 3.75 mm) radius 2.5 mm
  fix u
}
extrude PlugPocket from PlugSk distance 2.2 mm cut targets Case

// Lugs: two pairs, on the 12 and 6 o'clock walls
for i from 0 to 1 {
  for j from 0 to 1 {
    sketch LugSk{i}{j} on YZ offset ((2*i - 1)*(lug_in + lug_t/2) - lug_t/2) {
      line a = ((2*j - 1)*(e0 - 0.1 mm), bar_z - 4 mm) ((2*j - 1)*bar_y, bar_z - 4 mm)
      arc b = start a.end through ((2*j - 1)*(bar_y + 4 mm), bar_z) end ((2*j - 1)*bar_y, bar_z + 4 mm)
      line c = b.end ((2*j - 1)*(e0 - 0.1 mm), bar_z + 4 mm)
      line d = c.end a.start
      fix a b c d
    }
    extrude Lug{i}{j} from LugSk{i}{j} distance lug_t join targets Case
  }
}
for j from 0 to 1 {
  cylinder BarHole{j} radius (tip_r + 0.15 mm) height (lug_gap + 2*lug_t + 2 mm) base (-lug_in - lug_t - 1 mm, (2*j - 1)*bar_y, bar_z) axis X cut targets Case
}

// ==== Bezel (TPU): straight sides, a sloped top, a lip over the face
sketch BezelSk on XY offset bez_bot {
  polyline closed o = (e_bz, -f_bz) (e_bz, f_bz) (f_bz, e_bz) (-f_bz, e_bz) (-e_bz, f_bz) (-e_bz, -f_bz) (-f_bz, -e_bz) (f_bz, -e_bz)
  fix o
}
extrude BezelSides from BezelSk distance (bez_mid - bez_bot) new -> body Bezel
sketch BezelTopSk on XY offset bez_mid {
  polyline closed o = (e_bz, -f_bz) (e_bz, f_bz) (f_bz, e_bz) (-f_bz, e_bz) (-e_bz, f_bz) (-e_bz, -f_bz) (-f_bz, -e_bz) (f_bz, -e_bz)
  fix o
}
extrude BezelTop from BezelTopSk distance (bez_top - bez_mid) taper (-bez_slope) join targets Bezel
sketch BezelInSk on XY offset (bez_bot - 0.5 mm) {
  polyline closed o = (e0, -f0) (e0, f0) (f0, e0) (-f0, e0) (-e0, f0) (-e0, -f0) (-f0, -e0) (f0, -e0)
  fix o
}
extrude BezelIn from BezelInSk distance (case_top - bez_bot + 0.5 mm) cut targets Bezel
sketch LipSk on XY offset (case_top - 0.5 mm) {
  polyline closed o = (e_lp, -f_lp) (e_lp, f_lp) (f_lp, e_lp) (-f_lp, e_lp) (-e_lp, f_lp) (-e_lp, -f_lp) (-f_lp, -e_lp) (f_lp, -e_lp)
  fix o
}
extrude LipOpening from LipSk distance (bez_top - case_top + 1 mm) cut targets Bezel
sketch GrooveSk on XY offset bead_z {
  polyline closed o = (e_bd, -f_bd) (e_bd, f_bd) (f_bd, e_bd) (-f_bd, e_bd) (-e_bd, f_bd) (-e_bd, -f_bd) (-f_bd, -e_bd) (f_bd, -e_bd)
  fix o
}
extrude Groove from GrooveSk distance bead_h cut targets Bezel

// Guards round the buttons, and the holes through them
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder Guard{i}{j} radius 3.8 mm height 1.5 mm base ((2*i - 1)*(e_bz - 0.3 mm), (2*j - 1)*btn_y, btn_z) axis ((2*i - 1), 0, 0) join targets Bezel
    cylinder GuardHole{i}{j} radius (shaft_r + 0.3 mm) height (bez_t + 3 mm) base ((2*i - 1)*(e0 - 0.5 mm), (2*j - 1)*btn_y, btn_z) axis ((2*i - 1), 0, 0) cut targets Bezel
  }
}
// Notches over the lugs, and the opening for the USB plug
for j from 0 to 1 {
  box LugNotch{j} size (2*(lug_in + lug_t) + 1 mm) (bar_y + 5 mm - e0 + 0.1 mm) (10.5 mm - bez_bot + 0.5 mm) corner (-(lug_in + lug_t) - 0.5 mm, (2*j - 1)*(e0 + bar_y + 5 mm - 0.1 mm)/2 - (bar_y + 5 mm - e0 + 0.1 mm)/2, bez_bot - 0.5 mm) cut targets Bezel
}
sketch PlugHoleSk on YZ offset (-e_bz - 1 mm) {
  rect rounded u = (-6.75 mm, usb_z + usb_t/2 - 4 mm) (6.75 mm, usb_z + usb_t/2 + 4 mm) radius 2.5 mm
  fix u
}
extrude PlugHole from PlugHoleSk distance (bez_t + 2 mm) cut targets Bezel

// ==== Lens: the recess's outline, lens_fit smaller
sketch LensSk on XY offset lens_z {
  polyline closed o = (e_ln, -f_ln) (e_ln, f_ln) (f_ln, e_ln) (-f_ln, e_ln) (-e_ln, f_ln) (-e_ln, -f_ln) (-f_ln, -e_ln) (f_ln, -e_ln)
  fix o
}
extrude LensPlate from LensSk distance lens_t new -> body Lens

// ==== Back plate: flush with the case, a lip inside it, four counterbored screws
sketch BackSk on XY {
  polyline closed o = (e0, -f0) (e0, f0) (f0, e0) (-f0, e0) (-e0, f0) (-e0, -f0) (-f0, -e0) (f0, -e0)
  fix o
}
extrude BackBlock from BackSk distance back_t new -> body BackPlate
sketch BackLipSk on XY offset (back_t - 0.1 mm) {
  polyline closed o = (e_lo, -f_lo) (e_lo, f_lo) (f_lo, e_lo) (-f_lo, e_lo) (-e_lo, f_lo) (-e_lo, -f_lo) (-f_lo, -e_lo) (f_lo, -e_lo)
  polyline closed i = (e_li, -f_li) (e_li, f_li) (f_li, e_li) (-f_li, e_li) (-e_li, f_li) (-e_li, -f_li) (-f_li, -e_li) (f_li, -e_li)
  fix o i
}
extrude BackLip from BackLipSk distance 1.1 mm join targets BackPlate
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder LipRelief{i}{j} radius 2.6 mm height 1.5 mm base ((2*i - 1)*boss_x, (2*j - 1)*boss_y, back_t) axis Z cut targets BackPlate
  }
}
sketch ScrewSk on XY offset (-0.5 mm) {
  circle a = (boss_x, boss_y) radius screw_r
  circle b = (-boss_x, boss_y) radius screw_r
  circle c = (-boss_x, -boss_y) radius screw_r
  circle d = (boss_x, -boss_y) radius screw_r
  fix a b c d
}
extrude ScrewHoles from ScrewSk distance (back_t + 1 mm) cut targets BackPlate
sketch HeadSk on XY offset (-0.5 mm) {
  circle a = (boss_x, boss_y) radius head_r
  circle b = (-boss_x, boss_y) radius head_r
  circle c = (-boss_x, -boss_y) radius head_r
  circle d = (boss_x, -boss_y) radius head_r
  fix a b c d
}
extrude Heads from HeadSk distance 1.4 mm cut targets BackPlate

// ==== Plungers: a flange inside the wall, a shaft out through the guard
for i from 0 to 1 {
  for j from 0 to 1 {
    cylinder PlFlange{i}{j} radius flange_r height 1 mm base ((2*i - 1)*(seat - 1.1 mm), (2*j - 1)*btn_y, btn_z) axis ((2*i - 1), 0, 0) new -> body Plunger{i}{j}
    cylinder PlShaft{i}{j} radius shaft_r height (e_bz + 1.2 mm + proud + 0.1 mm - seat + 0.5 mm) base ((2*i - 1)*(seat - 0.5 mm), (2*j - 1)*btn_y, btn_z) axis ((2*i - 1), 0, 0) join targets Plunger{i}{j}
    fillet PlTip{i}{j} edges ((2*i - 1)*(e_bz + 1.2 mm + proud + 0.1 mm), (2*j - 1)*btn_y + shaft_r, btn_z) radius 0.6 mm body Plunger{i}{j}
  }
}

// ==== Straps (TPU): a tube on the spring bar, a band out to the buckle or the tip
sketch Strap12Sk on YZ offset (-strap_w/2) {
  circle t = (bar_y, bar_z) radius 3 mm
  rect band = (bar_y, bar_z - 3 mm) (short_end, bar_z - 3 mm + strap_t)
  circle e = (short_end, bar_z - 0.5 mm) radius 2.5 mm
  fix t band e
}
extrude Strap12Band from Strap12Sk distance strap_w new -> body Strap12
cylinder Strap12Bar radius (bar_r + 0.11 mm) height (strap_w + 2 mm) base (-strap_w/2 - 1 mm, bar_y, bar_z) axis X cut targets Strap12
cylinder Strap12Hinge radius (bar_r + 0.11 mm) height (strap_w + 2 mm) base (-strap_w/2 - 1 mm, short_end, bar_z - 0.5 mm) axis X cut targets Strap12
box ProngNotch size 3 mm 7 mm 8 mm corner (-1.5 mm, short_end - 3.5 mm, bar_z - 4.5 mm) cut targets Strap12

sketch Strap6Sk on YZ offset (-strap_w/2) {
  circle t = (-bar_y, bar_z) radius 3 mm
  rect band = (-bar_y - long_l, bar_z - 3 mm) (-bar_y, bar_z - 3 mm + strap_t)
  fix t band
}
extrude Strap6Band from Strap6Sk distance strap_w new -> body Strap6
fillet Strap6TipL edges (-strap_w/2, -bar_y - long_l, bar_z - 1.5 mm) radius 6 mm body Strap6
fillet Strap6TipR edges (strap_w/2, -bar_y - long_l, bar_z - 1.5 mm) radius 6 mm body Strap6
cylinder Strap6Bar radius (bar_r + 0.11 mm) height (strap_w + 2 mm) base (-strap_w/2 - 1 mm, -bar_y, bar_z) axis X cut targets Strap6
for k from 0 to 8 {
  cylinder Hole{k} radius hole_r height (strap_t + 2 mm) base (0, -bar_y - hole_first - k*hole_pitch, bar_z - 4 mm) axis Z cut targets Strap6
}

// ==== Buckle: a U frame on the hinge bar, the prong on the same bar, a keeper loop
sketch FrameSk on XY offset (bar_z - 3 mm) {
  rect rounded f = (-lug_in - 1.9 mm, short_end - 3.5 mm) (lug_in + 1.9 mm, short_end + 17 mm) radius 3 mm
  fix f
}
extrude FrameBlock from FrameSk distance 4.3 mm new -> body Frame
box FrameOpen size (2*lug_in - 0.2 mm) 18 mm 6 mm corner (-lug_in + 0.1 mm, short_end - 4 mm, bar_z - 4 mm) cut targets Frame
cylinder FrameHinge radius (tip_r + 0.15 mm) height (2*lug_in + 6 mm) base (-lug_in - 3 mm, short_end, bar_z - 0.5 mm) axis X cut targets Frame

cylinder ProngRing radius 2.2 mm height 2.4 mm base (-1.2 mm, short_end, bar_z - 0.5 mm) axis X new -> body Prong
box ProngArm size 2.4 mm 16.5 mm 1.6 mm corner (-1.2 mm, short_end, bar_z + 1.5 mm) join targets Prong
cylinder ProngBore radius (bar_r + 0.11 mm) height 4 mm base (-2 mm, short_end, bar_z - 0.5 mm) axis X cut targets Prong

box KeeperBlock size (strap_w + 3 mm) 6 mm (2*strap_t + 3 mm) corner (-strap_w/2 - 1.5 mm, short_end - 15 mm, bar_z - 4.5 mm) new -> body Keeper
box KeeperHole size (strap_w + 0.6 mm) 8 mm (2*strap_t + 0.6 mm) corner (-strap_w/2 - 0.3 mm, short_end - 16 mm, bar_z - 3.3 mm) cut targets Keeper

// ==== The bought parts, placed (not printed)
box PanelGlass size panel_w panel_h panel_t corner (-panel_w/2, panel_top - panel_h, panel_z) new -> body Panel
box EspPcb size esp_l esp_w esp_t corner (esp_x0, -esp_w/2, esp_z) new -> body Xiao
box EspUsb size usb_l usb_w usb_t corner (esp_x0 - 0.5 mm, -usb_w/2, usb_z) join targets Xiao
box Cell size bat_w bat_h bat_t corner (-bat_w/2, -bat_h/2, bat_z) new -> body Battery
for i from 0 to 1 {
  for j from 0 to 1 {
    box SwBody{i}{j} size sw_body sw sw corner ((2*i - 1)*(sw_in + sw_body/2) - sw_body/2, (2*j - 1)*btn_y - sw/2, btn_z - sw/2) new -> body Switch{i}{j}
    cylinder SwPlunger{i}{j} radius 1.75 mm height (sw_plunger + 0.1 mm) base ((2*i - 1)*(sw_in + sw_body - 0.1 mm), (2*j - 1)*btn_y, btn_z) axis ((2*i - 1), 0, 0) join targets Switch{i}{j}
  }
}
for j from 0 to 1 {
  cylinder BarBody{j} radius bar_r height (lug_gap - 0.4 mm) base (-lug_in + 0.2 mm, (2*j - 1)*bar_y, bar_z) axis X new -> body SpringBar{j}
  cylinder BarTipL{j} radius tip_r height 1.4 mm base (-lug_in - 1.1 mm, (2*j - 1)*bar_y, bar_z) axis X join targets SpringBar{j}
  cylinder BarTipR{j} radius tip_r height 1.4 mm base (lug_in - 0.3 mm, (2*j - 1)*bar_y, bar_z) axis X join targets SpringBar{j}
}
cylinder HingeBody radius bar_r height (2*lug_in - 0.4 mm) base (-lug_in + 0.2 mm, short_end, bar_z - 0.5 mm) axis X new -> body SpringBar2
cylinder HingeTipL radius tip_r height 1.4 mm base (-lug_in - 1.1 mm, short_end, bar_z - 0.5 mm) axis X join targets SpringBar2
cylinder HingeTipR radius tip_r height 1.4 mm base (lug_in - 0.3 mm, short_end, bar_z - 0.5 mm) axis X join targets SpringBar2

// ==== Fits
check Plunger10 clearance Case >= 0.09 mm
check Plunger01 clearance Bezel >= 0.25 mm
check Plunger10 clearance Switch10 >= 0.05 mm
check Xiao clearance Case >= 0.09 mm
check Battery clearance Case >= 0.5 mm
check Battery clearance BackPlate >= 0.15 mm
check Panel clearance Case >= 0.04 mm
check Lens clearance Case >= 0.04 mm
check Lens clearance Bezel >= 0.04 mm
check Strap12 clearance Case >= 0.2 mm
check Strap6 clearance Bezel >= 0.25 mm
check SpringBar0 clearance Case >= 0.1 mm
check SpringBar1 clearance Case >= 0.1 mm
check SpringBar1 clearance Strap12 >= 0.1 mm
check SpringBar2 clearance Frame >= 0.09 mm
check Prong clearance Frame >= 0.15 mm
check Prong clearance Strap12 >= 0.2 mm
check Frame clearance Strap12 >= 0.19 mm
check Keeper clearance Strap12 >= 0.25 mm
check SpringBar2 clearance Prong >= 0.1 mm

// ==== Finish
color Case, BackPlate "#2b2d30"
color Bezel, Strap12, Strap6, Keeper "#17181a"
color Plunger00, Plunger01, Plunger10, Plunger11, Frame, Prong "#9da3a8"
color Lens "#cfe3ea"
color Panel "#20283a"
color Xiao "#2f6b3a"
color Battery "#9aa4b0"
color Switch00, Switch01, Switch10, Switch11 "#303030"
color SpringBar0, SpringBar1, SpringBar2 "#c9ccd1"
material Case, BackPlate, Bezel, Strap12, Strap6, Keeper, Plunger00, Plunger01, Plunger10, Plunger11, Frame, Prong, Lens PLA
material SpringBar0, SpringBar1, SpringBar2 Steel
