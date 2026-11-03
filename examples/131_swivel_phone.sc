// 131. Swivel-screen messenger in the style of the 2002 Sidekick, printable: an ESP32-S3 pocket
// terminal whose screen half turns 180 deg on a hollow pivot to uncover a 40-key thumb keyboard.
//
// The phone lies face up, closed: X across (0 at the left end, W at the right), Y away from the
// typist (0 at the near edge, H at the far edge), Z up (0 under the base). The screen half (the lid)
// sits on the base with a `gap` and turns about a vertical pin at (W/2, py), 9 mm in from the far
// edge. Turned 180 deg it lies past the far edge with only the 18 mm strip around the pin still over
// the base, and the keyboard below that strip is open. `swivel` drives it (0 closed, 180 open).
//
// Bill of materials (module sizes are params; check them against yours):
//   1  Seeed XIAO ESP32-S3 (21 x 17.8 mm), its USB-C out of the left end; it charges the LiPo
//   1  2.8 in ILI9341 320 x 240 SPI display module (86 x 50 mm board, 57.6 x 43.2 mm view), pin
//      header removed, wired with about 9 strands of 30 AWG silicone wire down through the pin
//   1  Adafruit TCA8418 keypad-matrix breakout (25.4 x 17.8 mm, I2C) scanning the 4 x 10 matrix
//   40 6 x 6 x 4.3 mm through-hole tactile switches on a 100 x 44 mm piece of 2.54 mm perfboard,
//      on a 10.16 mm (four-hole) pitch
//   1  402050 LiPo (4 x 20 x 50 mm, about 500 mAh), under the keyboard board
//   1  SS12D00 mini slide switch (power), in the right end
//   4  M2 x 6 countersunk screws hold the deck to the tray; 6 M2 x 4 hold the keyboard board;
//   4  M2 x 8 hold the screen bezel to the lid back; 2 M2 x 3 hold the pivot fork
//
// How the pivot works. The pin is a tube printed upright on the base's deck, 8 mm across with a
// 4 mm bore for the display wires. It rises through a bore in the lid back, and inside the lid a
// flat fork slides sideways into a groove near its top and is screwed to two low bosses on the lid
// back's floor, so the lid can turn but not lift. The lid rides on a shoulder ring round the pin
// that stands 0.35 mm proud of the deck. The detent is a 1 mm cone on a cantilever tongue cut in
// the deck, 24 mm from the pin. The lid back has a spherical dimple at each of the two places the
// bump meets it, closed and open, so the lid clicks into both, and the tongue flexes 0.6 mm as it
// rides between them (about 0.4 % strain).
//
// The keys are 40 caps with flanges under the deck, standing on the switches. Their tops sit 0.1 mm
// above the deck and 0.3 mm under the lid, so the lid sweeps over them as it turns. Legends: a
// waterslide decal or paint pen. The matrix is hand-wired to the TCA8418: 4 row and 10 column lines.
//
// Printing, no supports, PLA: the tray open side up; the deck face up (its underside is flat, the
// pin and the bump on top); the lid back floor down; the bezel face down; the fork flat; the caps
// flange down. 0.2 mm layers; the groove and the fork want 0.1 mm of play per side, so test the fork
// on the pin before printing the rest.
design SwivelPhone units mm

// Body
param W = 120 mm
param H = 64 mm
param corner_r = 8 mm
param wall = 2 mm
param fit = 0.2 mm

// Base: tray and deck
param Tb = 15.6 mm             // base height, to the deck's top face
param floor_t = 1.5 mm
param deck_t = 1.6 mm

// Keyboard
param pitch = 10.16 mm
param cap_w = 8.4 mm
param ky0 = 4 mm               // near edge of the first key row's cell
param pcb_z = 7 mm             // keyboard board's underside
param pcb_t = 1.6 mm
param sw_h = 4.3 mm            // switch height to the plunger top
param flange_t = 0.8 mm

// Screen half (the lid)
param gap = 0.4 mm
param Tl = 11.8 mm
param lid_floor = 1.6 mm
param face_t = 1.4 mm
param split_h = 5.4 mm         // lid back / bezel joint, also the display board's underside
param tft_w = 86 mm
param tft_h = 50 mm
param tft_t = 5 mm
param view_w = 58 mm
param view_h = 43.5 mm

// Pivot and detent
param pivot_in = 9 mm          // pin centre from the far edge
param pin_d = 8 mm
param bore_d = 4 mm
param groove_d = 6.4 mm
param fork_t = 1.2 mm
param fork_boss = 1.2 mm
param detent_r = 24 mm
param swivel = 180 deg         // 0 closed, 180 open

let kx0 = (W - 10*pitch)/2
let sw_top = pcb_z + pcb_t + sw_h
let deck_z = Tb - deck_t
let zl0 = Tb + gap
let split_z = zl0 + split_h
let face_z = zl0 + Tl - face_t
let px = W/2
let py = H - pivot_in
let fork_z = zl0 + lid_floor + fork_boss
let tft_x = (W - tft_w)/2
let tft_y = (H - tft_h)/2
let tg_x0 = px + detent_r - 2.8 mm     // the tongue's free end
let tg_x1 = px + detent_r + 16 mm      // its root
let usb_z = floor_t + 3 mm

// ==== Tray: a rounded tub, open at the top, walls to the deck's top face
sketch Outline on XY {
  rect rounded o = (0,0) (W,H) radius corner_r
  fix o
}
extrude TrayBlock from Outline distance Tb new -> body Tray
fillet TrayRound edges (W/2, 0, 0) radius 2.5 mm body Tray
sketch CavitySk on XY offset floor_t {
  rect rounded c = (wall, wall) (W - wall, H - wall) radius (corner_r - wall)
  fix c
}
extrude Cavity from CavitySk distance Tb cut targets Tray
// USB-C in the left end, power switch in the right end
sketch UsbSk on YZ offset (-1 mm) {
  slot u = centers (27 mm - 3.2 mm, usb_z) (27 mm + 3.2 mm, usb_z) width 3.6 mm
  fix u
}
extrude UsbPort from UsbSk distance (wall + 2 mm) cut targets Tray
box PowerSlot size (wall + 2 mm) 5 mm 2.2 mm corner (W - wall - 1 mm, 46.25 mm, floor_t + 0.7 mm) cut targets Tray

// Deck bosses in the four corners (M2 pilot), keyboard posts at the switch-gap crossings
for k from 0 to 3 {
  cylinder DeckBoss{k} radius 2.8 mm height (deck_z - floor_t + 0.5 mm) base (6 mm + mod(k, 2)*(W - 12 mm), 6 mm + floor(k/2)*(H - 12 mm), floor_t - 0.5 mm) axis Z join targets Tray
  cylinder DeckPilot{k} radius 0.8 mm height 7 mm base (6 mm + mod(k, 2)*(W - 12 mm), 6 mm + floor(k/2)*(H - 12 mm), deck_z - 6.9 mm) axis Z cut targets Tray
}
for k from 0 to 5 {
  cylinder KbPost{k} radius 1.8 mm height (pcb_z - floor_t + 0.5 mm) base (kx0 + (2 + 3*mod(k, 3))*pitch, ky0 + 1*pitch + floor(k/3)*(30.24 mm), floor_t - 0.5 mm) axis Z join targets Tray
  cylinder KbPilot{k} radius 0.8 mm height 4 mm base (kx0 + (2 + 3*mod(k, 3))*pitch, ky0 + 1*pitch + floor(k/3)*(30.24 mm), pcb_z - 3.9 mm) axis Z cut targets Tray
}
// Locating ribs: the XIAO at the left end, the LiPo, the TCA8418, the power switch
box XiaoRibN size 21 mm 1.2 mm 2 mm corner (wall - 0.2 mm, 18 mm - fit - 1.2 mm, floor_t - 0.2 mm) join targets Tray
box XiaoRibF size 21 mm 1.2 mm 2 mm corner (wall - 0.2 mm, 35.8 mm + fit, floor_t - 0.2 mm) join targets Tray
box BatRibL size 1.2 mm 14 mm 2 mm corner (33 mm - fit - 1.2 mm, 20 mm, floor_t - 0.2 mm) join targets Tray
box BatRibR size 1.2 mm 14 mm 2 mm corner (83 mm + fit, 20 mm, floor_t - 0.2 mm) join targets Tray
box TcaRibL size 1.2 mm 12 mm 2 mm corner (97 mm - fit - 1.2 mm, 22 mm, floor_t - 0.2 mm) join targets Tray
box SwRibN size 5 mm 1.2 mm 3 mm corner (W - wall - 4.6 mm, 44.5 mm - fit - 1.2 mm, floor_t - 0.2 mm) join targets Tray
box SwRibF size 5 mm 1.2 mm 3 mm corner (W - wall - 4.6 mm, 53 mm + fit, floor_t - 0.2 mm) join targets Tray

// ==== Deck: a flat plate inside the tray's walls, key holes, the pin and the detent tongue on top
sketch DeckSk on XY offset deck_z {
  rect rounded d = (wall + fit, wall + fit) (W - wall - fit, H - wall - fit) radius (corner_r - wall - fit)
  fix d
}
extrude DeckPlate from DeckSk distance deck_t new -> body Deck
sketch KeyHoleSk on XY offset (deck_z - 1 mm) {
  rect rounded k = (kx0 + (pitch - cap_w)/2 - fit, ky0 + (pitch - cap_w)/2 - fit) (kx0 + (pitch + cap_w)/2 + fit, ky0 + (pitch + cap_w)/2 + fit) radius 1.2 mm
  fix k
  pattern rectangular k count 10 spacing pitch count2 4 spacing2 pitch
}
extrude KeyHoles from KeyHoleSk distance (deck_t + 2 mm) cut targets Deck
for k from 0 to 3 {
  cylinder DeckScrew{k} radius 1.15 mm height (deck_t + 2 mm) base (6 mm + mod(k, 2)*(W - 12 mm), 6 mm + floor(k/2)*(H - 12 mm), deck_z - 1 mm) axis Z cut targets Deck
  cone DeckSink{k} radius 0.9 mm top_radius 2.15 mm height 1.25 mm base (6 mm + mod(k, 2)*(W - 12 mm), 6 mm + floor(k/2)*(H - 12 mm), Tb - 1.24 mm) axis Z cut targets Deck
}
// The pin: shoulder ring, tube, groove for the fork, wire bore
cylinder Shoulder radius 7 mm height (gap + 0.25 mm) base (px, py, Tb - 0.3 mm) axis Z join targets Deck
cylinder Pin radius (pin_d/2) height (fork_z - 0.1 mm - Tb + 0.3 mm) base (px, py, Tb - 0.3 mm) axis Z join targets Deck
cylinder GrooveCore radius (groove_d/2) height (fork_t + 0.4 mm) base (px, py, fork_z - 0.2 mm) axis Z join targets Deck
cylinder PinHead radius (pin_d/2) height 0.8 mm base (px, py, fork_z + fork_t + 0.1 mm) axis Z join targets Deck
cylinder PinBore radius (bore_d/2) height (fork_z + 4 mm - deck_z) base (px, py, deck_z - 1 mm) axis Z cut targets Deck
// The detent tongue: a U-slot leaves an 18.8 mm cantilever, its free end carrying the bump
box TongueEnd size 0.8 mm 6.6 mm (deck_t + 2 mm) corner (tg_x0 - 0.8 mm, py - 3.3 mm, deck_z - 1 mm) cut targets Deck
box TongueN size (tg_x1 - tg_x0) 0.8 mm (deck_t + 2 mm) corner (tg_x0 - 0.01 mm, py - 3.3 mm, deck_z - 1 mm) cut targets Deck
box TongueF size (tg_x1 - tg_x0) 0.8 mm (deck_t + 2 mm) corner (tg_x0 - 0.01 mm, py + 2.5 mm, deck_z - 1 mm) cut targets Deck
cone Bump radius 1.6 mm top_radius 0.6 mm height 1.1 mm base (px + detent_r, py, Tb - 0.1 mm) axis Z join targets Deck

// ==== Keys: one cap per switch, its flange under the deck
for r from 0 to 3 {
  for c from 0 to 9 {
    sketch CapSk{r}{c} on XY offset (sw_top + flange_t - 0.1 mm) {
      rect rounded o = (kx0 + c*pitch + (pitch - cap_w)/2, ky0 + r*pitch + (pitch - cap_w)/2) (kx0 + c*pitch + (pitch + cap_w)/2, ky0 + r*pitch + (pitch + cap_w)/2) radius 1 mm
      fix o
    }
    extrude CapTop{r}{c} from CapSk{r}{c} distance (Tb + 0.1 mm - sw_top - flange_t + 0.1 mm) new -> body Key{r}{c}
    box CapFlange{r}{c} size 9.6 mm 9.6 mm flange_t corner (kx0 + c*pitch + 0.28 mm, ky0 + r*pitch + 0.28 mm, sw_top) join targets Key{r}{c}
    color Key{r}{c} "#d8d4cc"
  }
}

// ==== Lid: one rounded block, hollowed, its openings cut, split into the lid back and the bezel
extrude LidBlock from Outline distance Tl offset zl0 new -> body Bezel
fillet LidTopRound edges (W/2, 0, zl0 + Tl) radius 2 mm body Bezel
fillet LidBotRound edges (W/2, 0, zl0) radius 1 mm body Bezel
sketch LidCavSk on XY offset (zl0 + lid_floor) {
  rect rounded c = (wall, wall) (W - wall, H - wall) radius (corner_r - wall)
  fix c
}
extrude LidCavity from LidCavSk distance (Tl - lid_floor - face_t) cut targets Bezel
sketch FaceSk on XY offset (face_z - 1 mm) {
  rect rounded v = ((W - view_w)/2, (H - view_h)/2) ((W + view_w)/2, (H + view_h)/2) radius 1 mm
  slot e = centers (W/2 - 8 mm, H - 3.8 mm) (W/2 + 8 mm, H - 3.8 mm) width 1.4 mm
  fix v e
}
extrude FaceHoles from FaceSk distance (face_t + 2 mm) cut targets Bezel
cylinder LidPinBore radius (pin_d/2 + 0.25 mm) height (lid_floor + 2 mm) base (px, py, zl0 - 1 mm) axis Z cut targets Bezel
for k from 0 to 1 {
  sphere Dimple{k} radius 2 mm center (px + (1 - 2*k)*detent_r, py, zl0 + 0.8 mm - 2 mm) cut targets Bezel
}
split LidHalves target Bezel origin (0, 0, split_z) normal (0, 0, 1) -> body LidBack

// Lid back: screw bosses for the bezel, posts under the display board's corners, fork bosses
for k from 0 to 3 {
  cylinder LidBoss{k} radius 2.8 mm height (split_h - lid_floor + 0.5 mm) base (6 mm + mod(k, 2)*(W - 12 mm), 6 mm + floor(k/2)*(H - 12 mm), zl0 + lid_floor - 0.5 mm) axis Z join targets LidBack
  cylinder LidPilot{k} radius 0.8 mm height (split_h - 0.6 mm + 0.1 mm) base (6 mm + mod(k, 2)*(W - 12 mm), 6 mm + floor(k/2)*(H - 12 mm), zl0 + 0.6 mm) axis Z cut targets LidBack
  cylinder TftPost{k} radius 2.5 mm height (split_h - lid_floor + 0.5 mm) base (tft_x + 3.5 mm + mod(k, 2)*(tft_w - 7 mm), tft_y + 3.5 mm + floor(k/2)*(tft_h - 7 mm), zl0 + lid_floor - 0.5 mm) axis Z join targets LidBack
}
for k from 0 to 1 {
  cylinder ForkBoss{k} radius 2.2 mm height (fork_boss + 0.5 mm) base (px + 8.5 mm, py + (2*k - 1)*4 mm, zl0 + lid_floor - 0.5 mm) axis Z join targets LidBack
  cylinder ForkPilot{k} radius 0.8 mm height 2.5 mm base (px + 8.5 mm, py + (2*k - 1)*4 mm, fork_z - 2.4 mm) axis Z cut targets LidBack
}

// Bezel: screw bosses down to the joint (counterbored in the face), ribs round the display board
for k from 0 to 3 {
  cylinder BezelBoss{k} radius 2.8 mm height (face_z - split_z + 0.5 mm) base (6 mm + mod(k, 2)*(W - 12 mm), 6 mm + floor(k/2)*(H - 12 mm), split_z) axis Z join targets Bezel
  cylinder BezelHole{k} radius 1.15 mm height (Tl + 1 mm) base (6 mm + mod(k, 2)*(W - 12 mm), 6 mm + floor(k/2)*(H - 12 mm), split_z - 0.1 mm) axis Z cut targets Bezel
  cylinder BezelHead{k} radius 2.1 mm height (face_t + 1 mm) base (6 mm + mod(k, 2)*(W - 12 mm), 6 mm + floor(k/2)*(H - 12 mm), face_z) axis Z cut targets Bezel
}
box TftRibL size 1.2 mm 20 mm 3 mm corner (tft_x - fit - 1.2 mm, H/2 - 10 mm, face_z - 3 mm) join targets Bezel
box TftRibR size 1.2 mm 20 mm 3 mm corner (tft_x + tft_w + fit, H/2 - 10 mm, face_z - 3 mm) join targets Bezel

// ==== Fork: slides into the pin's groove from the left, two screws into the lid back's bosses
sketch ForkSk on XY offset fork_z {
  rect f = (px - 6 mm, py - 6.25 mm) (px + 12 mm, py + 6.25 mm)
  fix f
}
extrude ForkPlate from ForkSk distance fork_t new -> body Fork
box ForkSlot size 8 mm (groove_d + 0.2 mm) (fork_t + 2 mm) corner (px - 8 mm, py - groove_d/2 - 0.1 mm, fork_z - 1 mm) cut targets Fork
cylinder ForkSeat radius (groove_d/2 + 0.1 mm) height (fork_t + 2 mm) base (px, py, fork_z - 1 mm) axis Z cut targets Fork
for k from 0 to 1 {
  cylinder ForkHole{k} radius 1.15 mm height (fork_t + 2 mm) base (px + 8.5 mm, py + (2*k - 1)*4 mm, fork_z - 1 mm) axis Z cut targets Fork
}

// ==== Modules, for checking fit (not printed)
box RefTft size tft_w tft_h tft_t corner (tft_x, tft_y, split_z) new -> body RefTft
box RefKeyboard size 100 mm 43.8 mm pcb_t corner (kx0 + 0.8 mm, 2.6 mm, pcb_z) new -> body RefKeyboard
for k from 0 to 3 {
  box RefSwitch{k} size 6 mm (3*pitch + 6 mm) (sw_h - 0.6 mm) corner (kx0 + (k*3 + 0.5)*pitch - 3 mm, ky0 + 0.5*pitch - 3 mm, pcb_z + pcb_t) new -> body RefSwitch{k}
}
box RefXiao size 21 mm 17.8 mm 4.4 mm corner (wall + 0.3 mm, 18 mm, floor_t) new -> body RefXiao
box RefBattery size 50 mm 20 mm 4 mm corner (33 mm, 17 mm, floor_t) new -> body RefBattery
box RefTca size 17.8 mm 25.4 mm 3 mm corner (97 mm, 16 mm, floor_t) new -> body RefTca
box RefPower size 3.6 mm 8.5 mm 3.6 mm corner (W - wall - 4.1 mm, 44.5 mm, floor_t) new -> body RefPower

// ==== The swivel: the lid's parts turn about the pin (moved to the origin, turned, moved back)
move PivotIn bodies Bezel, LidBack, Fork, RefTft to (-px, -py, 0)
move Swivel bodies Bezel, LidBack, Fork, RefTft axis (0,0,1) angle swivel
move PivotOut bodies Bezel, LidBack, Fork, RefTft to (px, py, 0)

// ==== Fits
check Fork clearance Deck >= 0.08 mm
check LidBack clearance Deck >= 0.04 mm
check Bezel clearance Deck >= 0.2 mm
check LidBack clearance Key30 >= 0.2 mm
check RefTft clearance Fork >= 0.2 mm
check RefTft clearance Deck >= 0.2 mm
check Key00 clearance Deck >= 0.1 mm
check Deck shells == 1
check RefKeyboard clearance RefBattery >= 0.5 mm
check RefKeyboard clearance RefXiao >= 0.5 mm
check RefKeyboard clearance Tray >= 0 mm

color Tray, Deck "#2b2d31"
color Bezel, LidBack "#c9ccd1"
color Fork "#e0b040"
color RefTft "#16181c"
color RefKeyboard, RefSwitch0, RefSwitch1, RefSwitch2, RefSwitch3, RefXiao, RefTca "#2f6f3a"
color RefBattery "#c8a13a"
color RefPower "#8a8f96"
material Tray, Deck, Bezel, LidBack, Fork PLA
