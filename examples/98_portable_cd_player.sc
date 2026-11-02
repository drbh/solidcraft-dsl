// 98. Portable "CD" player: a printable round case with a hinged lid around a DFPlayer Mini MP3
// player. The 100 mm disc under the window is printed; it turns freely on the spindle, and the
// music comes off a microSD card.
//
// Bought parts: DFPlayer Mini (with a microSD card), ESP32-C3 SuperMini, 503450 LiPo
// (50 x 34 x 5 mm, 1000 mAh), TP4056 USB-C charger board (28 x 17 mm), PJ-320A 3.5 mm jack, four
// 6 x 6 x 5 mm tactile switches (prev, play, next, vol by long press), 3 x M2 x 6 self-tapping
// screws, about 60 mm of 1.75 mm filament for the hinge pin, and hot glue for the boards. Wiring:
// LiPo to the TP4056 B+/B-, its OUT+/OUT- to the C3's 5V/GND and the DFPlayer's VCC/GND, the C3's
// UART to the DFPlayer RX/TX, the switches from C3 GPIOs to GND, DAC_L/DAC_R/GND to the jack.
//
// Z is up, the case lies flat with its centre on the origin, the front (buttons) faces -Y and the
// hinge is at the back (+Y). The base is an open round tub with a flat front:
//  - inside: a fence that locates the LiPo, a rail the switches are glued to, three M2 bosses, and
//    two hinge ears at the back. The other boards are glued to the floor.
//  - in the walls: four button holes and the latch window in front, the USB-C opening on the
//    left, the jack hole on the right.
//  - the deck: screwed to the bosses, it closes the electronics bay and forms the disc well; the
//    spindle peg is on it.
// The lid carries a clear pane in a rabbet under its window, a knuckle that goes between the base
// knuckles on the filament pin, and a cantilever tab whose barb snaps into the front window. Press
// the barb through the window to open. Each button cap has a flange inside the wall, so it can't
// fall out, and presses a switch glued to the rail.
//
// The lid is built closed and swung open by lid_angle about the pin: move the pin to the origin,
// turn about X, move back. The moves are constants, so the angle stays live. Clearances are
// `clear` (0.3 mm) all round; the pin holes are 0.05 mm tight in the base and 0.25 mm loose in the
// lid. Print plates (98_portable_cd_player.print.json, 256 mm bed): the base open side up, the lid
// face down, and the deck, disc, pane (clear PETG) and caps (flange down). No supports.
design PortableCdPlayer units mm

param body_r = 68 mm
param front_y = -56 mm          // the flat front
param wall = 2 mm
param floor_t = 1.6 mm
param base_h = 22 mm
param lid_t = 4 mm
param clear = 0.3 mm
param lid_angle = 70 deg

// Deck and disc
param deck_t = 2 mm
param well_d = 3.5 mm           // deck top below the base top
param disc_r = 50 mm             // a printed 100 mm disc: a 120 mm CD would not clear the flat front
param disc_t = 1.2 mm
param disc_hole_r = 7.5 mm
param window_r = 33 mm

// Hinge, on a 1.75 mm filament pin
param knuckle_r = 3.5 mm
param pin_r = 0.875 mm
param lid_knuckle_w = 31.8 mm
param base_knuckle_w = 9.7 mm

// Latch tab
param tab_w = 14 mm
param tab_t = 1.2 mm
param tab_l = 8 mm
param barb = 0.8 mm

// Buttons on 6 x 6 switches, at x = -26, -14, 14, 26 mm
param cap_w = 6 mm
param cap_h = 4 mm
param button_z = 9 mm

let front_x = sqrt(body_r^2 - front_y^2)
let in_r = body_r - wall
let in_y = front_y + wall
let in_x = sqrt(in_r^2 - in_y^2)
let deck_r = in_r - clear
let deck_y = in_y + clear
let deck_x = sqrt(deck_r^2 - deck_y^2)
let deck_top = base_h - well_d
let deck_z = deck_top - deck_t
let hinge_y = body_r + 0.5 mm
let hinge_z = base_h
let lid_knuckle_x = lid_knuckle_w/2
let base_knuckle_x = lid_knuckle_x + 0.4 mm
let ear_in = base_knuckle_x + 0.3 mm
let ear_out = base_knuckle_x + base_knuckle_w - 0.3 mm
let ear_y = hinge_y + 3 mm
let tab_y = in_y + clear
let boss_at = in_r - 11 mm
let switch_y = in_y + 1.4 mm    // the switch plunger just behind the cap flange

// ==== Base: an open tub with a flat front and two hinge ears at the back, in one outline
sketch BaseSk on XY {
  line f = (-front_x, front_y) (front_x, front_y)
  arc a1 = start f.end through (body_r, 0) end (ear_out, sqrt(body_r^2 - ear_out^2))
  line e1 = a1.end (ear_out, ear_y)
  line e2 = e1.end (ear_in, ear_y)
  line e3 = e2.end (ear_in, sqrt(body_r^2 - ear_in^2))
  arc a2 = start e3.end through (0, body_r) end (-ear_in, sqrt(body_r^2 - ear_in^2))
  line e4 = a2.end (-ear_in, ear_y)
  line e5 = e4.end (-ear_out, ear_y)
  line e6 = e5.end (-ear_out, sqrt(body_r^2 - ear_out^2))
  arc a3 = start e6.end through (-body_r, 0) end f.start
  fix f a1 e1 e2 e3 a2 e4 e5 e6 a3
}
sketch LidSk on XY offset base_h {
  line f = (-front_x, front_y) (front_x, front_y)
  arc a = start f.end through (0, body_r) end f.start
  fix f a
}
sketch CavitySk on XY offset floor_t {
  line f = (-in_x, in_y) (in_x, in_y)
  arc a = start f.end through (0, in_r) end f.start
  fix f a
}
extrude BaseBlock from BaseSk distance base_h new -> body Base
extrude Cavity from CavitySk distance base_h cut targets Base

// Deck bosses, M2 self-tapping
for k from 0 to 2 {
  cylinder Boss{k} radius 3 mm height (deck_z - floor_t + 0.2 mm) base (boss_at*cos((90 + 120*k)*1 deg), boss_at*sin((90 + 120*k)*1 deg), floor_t - 0.2 mm) axis Z join targets Base
  cylinder Pilot{k} radius 0.8 mm height 8 mm base (boss_at*cos((90 + 120*k)*1 deg), boss_at*sin((90 + 120*k)*1 deg), deck_z - 7.9 mm) axis Z cut targets Base
}

// LiPo fence (50 x 34 x 5 cell, clear all round) and the switch rail
box FenceL size 1.2 mm 37.8 mm 3.6 mm corner (-25 mm - clear - 1.2 mm, 1 mm - clear - 1.2 mm, floor_t - 0.2 mm) join targets Base
box FenceR size 1.2 mm 37.8 mm 3.6 mm corner (25 mm + clear, 1 mm - clear - 1.2 mm, floor_t - 0.2 mm) join targets Base
box FenceF size 53 mm 1.2 mm 3.6 mm corner (-26.5 mm, 1 mm - clear - 1.2 mm, floor_t - 0.2 mm) join targets Base
box FenceB size 53 mm 1.2 mm 3.6 mm corner (-26.5 mm, 35 mm + clear, floor_t - 0.2 mm) join targets Base
box Rail size 64 mm 2 mm (deck_z - floor_t - 1.5 mm) corner (-32 mm, switch_y + 5 mm, floor_t - 0.2 mm) join targets Base

// Hinge: a knuckle on each ear (0.3 mm wider than the ear at each end), a notch between them for
// the lid's knuckle
for k from 0 to 1 {
  cylinder Knuckle{k} radius knuckle_r height base_knuckle_w base ((2*k - 1)*(base_knuckle_x + base_knuckle_w/2) - base_knuckle_w/2, hinge_y, hinge_z) axis X join targets Base
}
box KnuckleNotch size (2*lid_knuckle_x + 0.4 mm) (body_r - hinge_y + knuckle_r + 2 mm) (knuckle_r + 1.5 mm) corner (-lid_knuckle_x - 0.2 mm, hinge_y - knuckle_r - 0.5 mm, hinge_z - knuckle_r - 0.5 mm) cut targets Base
cylinder BasePin radius (pin_r - 0.05 mm) height (2*(base_knuckle_x + base_knuckle_w) + 2 mm) base (-base_knuckle_x - base_knuckle_w - 1 mm, hinge_y, hinge_z) axis X cut targets Base

// Openings: button holes and the latch window in front, USB-C on the left, the jack on the right
for k from 0 to 3 {
  box ButtonHole{k} size (cap_w + 2*clear) (wall + 2 mm) (cap_h + 2*clear) corner ((2*floor(k/2) - 1)*(14 mm + 12 mm*(1 - mod(k, 2))) - cap_w/2 - clear, front_y - 1 mm, button_z - cap_h/2 - clear) cut targets Base
}
box LatchWindow size (tab_w + 1 mm) (wall + 2 mm) 2.1 mm corner (-tab_w/2 - 0.5 mm, front_y - 1 mm, base_h - tab_l - 0.3 mm) cut targets Base
box UsbC size (wall + 3 mm) 9.6 mm 3.8 mm corner (-body_r - 1 mm, -4.8 mm, floor_t + 1 mm) cut targets Base
cylinder JackHole radius 3.1 mm height (wall + 4 mm) base (body_r - wall - 2 mm, 0, floor_t + 3.3 mm) axis X cut targets Base

// ==== Deck: closes the bay, forms the disc well, carries the spindle peg
sketch DeckSk on XY offset deck_z {
  line f = (-deck_x, deck_y) (deck_x, deck_y)
  arc a = start f.end through (0, deck_r) end f.start
  fix f a
}
extrude DeckPlate from DeckSk distance deck_t new -> body Deck
cylinder Spindle radius (disc_hole_r - 0.25 mm) height 2.5 mm base (0, 0, deck_top - 0.1 mm) axis Z join targets Deck
box TabRelief size (tab_w + 2 mm) 3 mm (deck_t + 2 mm) corner (-tab_w/2 - 1 mm, deck_y - 1 mm, deck_z - 1 mm) cut targets Deck
for k from 0 to 2 {
  cylinder DeckHole{k} radius 1.2 mm height (deck_t + 2 mm) base (boss_at*cos((90 + 120*k)*1 deg), boss_at*sin((90 + 120*k)*1 deg), deck_z - 1 mm) axis Z cut targets Deck
  cylinder DeckSink{k} radius 2.3 mm height 2 mm base (boss_at*cos((90 + 120*k)*1 deg), boss_at*sin((90 + 120*k)*1 deg), deck_top - 1.2 mm) axis Z cut targets Deck
}

// The printed disc on the spindle
cylinder DiscPlate radius disc_r height disc_t base (0, 0, deck_top) axis Z new -> body Disc
cylinder DiscHole radius disc_hole_r height (disc_t + 2 mm) base (0, 0, deck_top - 1 mm) axis Z cut targets Disc

// ==== Lid, built closed
extrude LidPlate from LidSk distance lid_t new -> body Lid
cylinder LidWindow radius window_r height (lid_t + 2 mm) base (0, 0, base_h - 1 mm) axis Z cut targets Lid
cylinder LidRabbet radius (window_r + 2 mm) height 2.2 mm base (0, 0, base_h - 1 mm) axis Z cut targets Lid
for k from 0 to 1 {
  box LidRelief{k} size (base_knuckle_w + 1 mm) (body_r - hinge_y + knuckle_r + 2 mm) (lid_t + 2 mm) corner ((2*k - 1)*(base_knuckle_x + base_knuckle_w/2) - base_knuckle_w/2 - 0.5 mm, hinge_y - knuckle_r - 0.7 mm, base_h - 1 mm) cut targets Lid
}
cylinder LidKnuckle radius knuckle_r height lid_knuckle_w base (-lid_knuckle_x, hinge_y, hinge_z) axis X join targets Lid
cylinder LidPin radius (pin_r + 0.25 mm) height (lid_knuckle_w + 2 mm) base (-lid_knuckle_x - 1 mm, hinge_y, hinge_z) axis X cut targets Lid
box Tab size tab_w tab_t (tab_l + 0.5 mm) corner (-tab_w/2, tab_y, base_h - tab_l) join targets Lid
box Barb size tab_w (barb + 0.1 mm) 1.5 mm corner (-tab_w/2, tab_y - barb, base_h - tab_l) join targets Lid
cylinder PaneDisc radius (window_r + 2 mm - clear) height 1 mm base (0, 0, base_h + 0.05 mm) axis Z new -> body Pane

move LidToPin bodies Lid, Pane to (0, -hinge_y, -hinge_z)
move LidOpen bodies Lid, Pane axis (1,0,0) angle (-lid_angle)
move LidBack bodies Lid, Pane to (0, hinge_y, hinge_z)

// ==== Button caps: a stem through the wall and a flange inside it
for k from 0 to 3 {
  box CapStem{k} size cap_w (wall + 1.7 mm) cap_h corner ((2*floor(k/2) - 1)*(14 mm + 12 mm*(1 - mod(k, 2))) - cap_w/2, front_y - 1.5 mm, button_z - cap_h/2) new -> body Cap{k}
  box CapFlange{k} size (cap_w + 3 mm) 1.2 mm (cap_h + 3 mm) corner ((2*floor(k/2) - 1)*(14 mm + 12 mm*(1 - mod(k, 2))) - cap_w/2 - 1.5 mm, in_y + 0.1 mm, button_z - cap_h/2 - 1.5 mm) join targets Cap{k}
}

// ==== Bought parts, for the look and for fit
box LiPoCell size 50 mm 34 mm 5 mm corner (-25 mm, 1 mm, floor_t + 0.1 mm) new -> body LiPo
box ChargerBoard size 28 mm 17 mm 4 mm corner (-in_r + 0.6 mm, -8.5 mm, floor_t + 0.1 mm) new -> body Charger
box McuBoard size 22.5 mm 18 mm 3.5 mm corner (8 mm, -40 mm, floor_t + 0.1 mm) new -> body Mcu
box PlayerBoard size 20.6 mm 20.6 mm 4 mm corner (-30 mm, -42 mm, floor_t + 0.1 mm) new -> body Player
box JackBody size 12 mm 6 mm 5.4 mm corner (in_r - 12.5 mm, -3 mm, floor_t + 0.1 mm) new -> body Jack
for k from 0 to 3 {
  box Switch{k} size 6 mm 5 mm 6 mm corner ((2*floor(k/2) - 1)*(14 mm + 12 mm*(1 - mod(k, 2))) - 3 mm, switch_y, button_z - 3 mm) new -> body Switch{k}
}

color Base, Lid, Deck "#c7cad0"
color Disc "#e4e8ef"
color Pane "#9fb7c9"
color Cap0, Cap1, Cap2, Cap3 "#4f535a"
color LiPo "#d9b44a"
color Charger, Mcu, Player "#1f5f8b"
color Jack, Switch0, Switch1, Switch2, Switch3 "#2b2b2b"
material Base, Lid, Deck, Disc, Pane, Cap0, Cap1, Cap2, Cap3 PLA
