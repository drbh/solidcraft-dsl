// 145. Cascade card wallet: a sleeve for n cards with a print-in-place stepped pusher that fans
// them out when you slide the thumb button.
//
// X runs along the cards' long edge, Y up out of the wallet's open top, Z through the stack (the
// button's side is z < 0, the front); the origin is the inside front-left-bottom corner. The
// cards stand on a pusher whose top is a staircase across the stack: the front card on the
// lowest step, each card behind it `step` higher. A neck through a closed slot in the front wall
// joins the pusher to the thumb button outside. Slide the button up by `travel` and every card
// rises with it, the back one furthest, so their top edges fan out to read and pick from. The
// slot's ends are the stops; the pusher can't leave the sleeve.
//
// The bottom of the sleeve is open, so the pusher stands on the bed beside the walls, never above
// a gap. A tongue cut in the back wall carries a bump that presses the stack forward, so the cards
// stay put upside down. A thumb notch in the front wall's top edge reaches the front card.
//
// Printing: one piece, PLA or PETG, 0.2 mm layers, no supports, standing on its open bottom (Y up,
// the print recipe turns it). Every gap round the pusher, neck and button is `fit`. Break the
// pusher free by sliding it once before loading cards.
design CardWallet units mm

// Cards
param card_l = 85.6 mm
param card_h = 54 mm
param card_t = 0.8 mm          // per card, embossed cards are about 0.8 mm
param n_cards = 6              // 1 to 10; read when compiling (one step per card)
param step = 3 mm              // how far each card stands above the one in front
param travel = 25 mm           // button stroke

// Sleeve
param wall = 1.6 mm
param fit = 0.3 mm
param side_play = 0.5 mm       // each side of the cards along X
param ph = 8 mm                // pusher height under the front card

let in_x = card_l + 2*side_play
let in_t = n_cards*card_t + 2*fit + 0.8 mm   // inside thickness, with room to slide
let H = ph + (n_cards - 1)*step + card_h + 0.5 mm
let cx = in_x/2
let zs = (in_t - 2*fit)/n_cards              // the steps' pitch across the stack
let neck_w = 10 mm
let neck_y0 = 2 mm + fit                     // neck bottom, `fit` above the slot's lower end
let neck_h = 4 mm
let tongue_y = H - 30 mm                     // root of the back-wall tongue

// ==== Sleeve: a tube open at both ends, rounded along its edges
box Outer size (in_x + 2*wall) H (in_t + 2*wall) corner (-wall, 0, -wall) new -> body Case
fillet Corners edges (-wall, H/2, -wall) (in_x + wall, H/2, -wall) (-wall, H/2, in_t + wall) (in_x + wall, H/2, in_t + wall) radius 2 mm body Case
box Inner size in_x (H + 2 mm) in_t corner (0, -1 mm, 0) cut targets Case
// Slot for the neck in the front wall: closed at both ends, 2 mm above the bottom edge
box Slot size (neck_w + 2*fit) (neck_h + travel + 2*fit) (wall + 2 mm) corner (cx - neck_w/2 - fit, 2 mm, -wall - 1 mm) cut targets Case
// Thumb notch in the front wall's top edge
cylinder Notch radius 12 mm height (wall + 2 mm) base (cx, H, -wall - 1 mm) axis Z cut targets Case
// Tongue in the back wall: a U of 0.8 mm slots round a 12 x 26 mm tongue rooted at its bottom
box TongueSlotL size 0.8 mm 26.4 mm (wall + 2 mm) corner (cx - 6.8 mm, tongue_y, in_t - 1 mm) cut targets Case
box TongueSlotR size 0.8 mm 26.4 mm (wall + 2 mm) corner (cx + 6 mm, tongue_y, in_t - 1 mm) cut targets Case
box TongueSlotT size 14 mm 0.8 mm (wall + 2 mm) corner (cx - 7 mm, tongue_y + 26 mm, in_t - 1 mm) cut targets Case
// The bump near its free end: a rod along X standing 0.6 mm proud of the wall's inside
cylinder Bump radius 1 mm height 10 mm base (cx - 5 mm, tongue_y + 22 mm, in_t + 0.4 mm) axis X join targets Case

// ==== Pusher: a block under the stack with one step per card, the neck and the button
box PusherBase size (in_x - 2*fit) ph (in_t - 2*fit) corner (fit, 0, fit) new -> body Slider
for k from 1 to 9 {
  if k < n_cards {
    box Step{k} size (in_x - 2*fit) (k*step + 0.5 mm) (in_t - fit - (fit + k*zs)) corner (fit, ph - 0.5 mm, fit + k*zs) join targets Slider
  }
}
box Neck size neck_w neck_h (wall + 2*fit + 1 mm) corner (cx - neck_w/2, neck_y0, -wall - fit - 0.5 mm) join targets Slider
box Button size 18 mm 12 mm 2.5 mm corner (cx - 9 mm, 0, -wall - fit - 2.5 mm) join targets Slider
fillet ButtonTop edges (cx, 12 mm, -wall - fit - 2.5 mm) radius 1.2 mm body Slider
// Grip ribs across the button
for k from 0 to 3 {
  box Rib{k} size 16 mm 0.8 mm 0.6 mm corner (cx - 8 mm, 2.5 mm + 2.4*k mm, -wall - fit - 3 mm) join targets Slider
}

// ==== Fits
check Slider clearance Case >= (fit - 0.05 mm)

color Case "#2e3440"
color Slider "#e0a526"
material Case, Slider PLA
