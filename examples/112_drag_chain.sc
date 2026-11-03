// 112. Drag chain (cable carrier): snap-together printed links, shown as a chain running out,
// round a half-turn bend and back over itself, the way it rides on a moving axis.
//
// Z is up, the lower run lies along +X with its pivots at z = 0, Y runs across the chain. One
// link lives in 112_drag_chain_parts/link.sc: its front plates sit inside the previous link's
// rear plates on snap pins, so a chain is any number of the same printed link. Each link's
// teeth stop it at `theta` against its neighbour, so the chain can only curl one way and never
// tighter than the bend. Six links make the half turn (theta = 30 deg), and the pitch follows
// from the bend radius: p = 2 * bend_r * sin(theta/2). Change bend_r and the links, the pitch
// and the bend all follow.
//
// Print the links upright as modelled (bottom bar on the bed), no supports: the top bar bridges
// between the walls. Nylon or PETG flexes best for the snap; PLA works with 0.4 mm clearance.
// 0.2 mm layers, 3 walls, 30 % infill. Snap the chain together by squeezing each link's front
// plates in and pushing them inside the next link's rear plates until the pins click into the
// holes. Bill of materials: the printed links (14 here), nothing else; cables up to 20 x 14 mm.
design DragChain units mm

param bend_r = 58 mm         // radius of the bend, at the pivots
param Wi = 20 mm             // cable channel width
param H = 20 mm
param clearance = 0.3 mm

let theta = 30 deg
let p = 2*bend_r*sin(theta/2)
let cx = 4*p - bend_r*sin(theta/2)          // the bend's centre
let cz = bend_r*cos(theta/2)

// The lower run, straight along +X
for k from 0 to 3 {
  use L{k} from "112_drag_chain_parts/link.sc" with (p = p, theta = theta, Wi = Wi, H = H, clearance = clearance, x0 = (k*p))
}
// Round the bend: each link turned theta further, its front pivot on the bend's circle
for k from 0 to 4 {
  use B{k} from "112_drag_chain_parts/link.sc" with (p = p, theta = theta, Wi = Wi, H = H, clearance = clearance, x0 = (cx + bend_r*sin(theta/2 + k*theta)), z0 = (cz - bend_r*cos(theta/2 + k*theta)), a0 = ((k + 1)*theta))
}
// The upper run, back along -X
for k from 0 to 4 {
  use U{k} from "112_drag_chain_parts/link.sc" with (p = p, theta = theta, Wi = Wi, H = H, clearance = clearance, x0 = (4*p - k*p), z0 = (2*cz), a0 = 180 deg)
}

// A straight joint keeps its clearance; a joint in the bend sits on its stop
check L1_Link clearance L0_Link >= (clearance - 0.01 mm)
check B2_Link clearance B1_Link <= 0.01 mm
// The runs clear each other
check U4_Link clearance L0_Link >= 50 mm
