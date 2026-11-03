// 109. Modular marble run: pieces that stack on one standard socket, shown built up on its base.
//
// Z is up. Every piece is turned about its own vertical axis and shares one interface:
//   spigot  at the bottom, a 24 mm tube 4 mm long, then a 45 deg cone out to the 32 mm shoulder;
//   socket  at the top, the same shape grown by `fit`, so a spigot drops in and its cone seats.
// The bore is marble_d + 3 mm all the way through, so a 16 mm glass marble runs free. Anything with a
// socket takes anything with a spigot, and the base has sockets on the same `pitch` as the splitter's
// outlets, so towers can be rearranged.
//
// Pieces:
//   Base      a block with three sockets on `pitch` over a sloping gutter that rolls marbles out of
//             the +X end into a tray (where 130's screw lift picks them up)
//   Drop      a straight tube with two windows to watch the marble fall
//   Spiral    a solid tower with a two-turn round tunnel cut through it: the marble enters from the
//             top socket, spirals down and leaves through the spigot
//   Splitter  one socket on top forking into two spigots `pitch` apart: each marble picks a side
//   Funnel    a 45 deg catch cone on a spigot
// The model shows them stacked: Drop on the left socket, Spiral on the right, the Splitter on both,
// the Funnel on top, and a marble in the funnel and one in the tray.
//
// Print (PLA, 0.2 mm, no supports): Base flat; Drop, Spiral and Funnel upright as shown (every
// overhang is 45 deg; the Spiral's tunnel ceiling bridges like a round hole); the Splitter on its
// side so its channels print as horizontal tunnels. If a spigot is tight, sand its cone or raise
// `fit`. No hardware.
design MarbleRun units mm

param marble_d = 16 mm
param fit = 0.3 mm
param pitch = 45 mm          // socket spacing on the base, and the splitter's outlets
param tube_h = 106 mm        // Drop and Spiral, spigot tip to top, so they stand level
param splitter_h = 60 mm

let r_b = marble_d/2 + 1.5 mm        // bore radius
let r_s = r_b + 2.5 mm               // spigot
let r_k = r_s + 4 mm                 // shoulder, after the 45 deg cone
let r_c = r_k + 3 mm                 // collar round a socket
let r_t = 31 mm                      // spiral tower
param r_mid = 18.5 mm                // spiral tunnel centre line radius (a param: `coil` takes no lets)
param lead = 27 mm                   // spiral tunnel drop per turn
let z_low = 36 mm                    // tunnel's lower end, in the spiral's own frame
let z_high = z_low + 2*lead          // its upper end, straight above (two whole turns)
let base_t = 32 mm
let tray_l = 60 mm
let r_f = 40 mm                      // funnel mouth

// ==== Drop: spigot, 45 deg collar, tube, socket at the top; two windows
sketch DropSk on XZ {
  polyline closed o = (r_b, 0) (r_s, 0) (r_s, -4 mm) (r_k, -8 mm) (r_c, -11 mm) (r_c, -tube_h) (r_k + fit, -tube_h) (r_s + fit, -(tube_h - 4 mm)) (r_s + fit, -(tube_h - 8 mm)) (r_b, -(tube_h - 8 mm))
  fix o
}
revolve DropRev from DropSk axis Z new -> body Drop
box DropWindows size 6 mm (2*r_c + 2 mm) (tube_h - 40 mm) corner (-3 mm, -r_c - 1 mm, 20 mm) cut targets Drop

// ==== Spiral: a solid tower; the bores stop at the tunnel's two ends, a coil cuts the tunnel
sketch SpiralSk on XZ {
  polyline closed o = (r_b, 0) (r_s, 0) (r_s, -4 mm) (r_k, -8 mm) (r_t, -(8 mm + r_t - r_k)) (r_t, -tube_h) (r_k + fit, -tube_h) (r_s + fit, -(tube_h - 4 mm)) (r_s + fit, -(tube_h - 8 mm)) (r_b, -(tube_h - 8 mm)) (r_b, -z_high) (0, -z_high) (0, -z_low) (r_b, -z_low)
  fix o
}
revolve SpiralRev from SpiralSk axis Z new -> body Spiral
// turn the revolve's seam away from the tunnel's end faces, which lie in the XZ plane
move SpiralSeam bodies Spiral axis (0,0,1) angle 20 deg
coil Tunnel diameter (2*r_mid) turns 2 pitch lead section_size (marble_d + 3 mm) base (0, 0, 36 mm) cut targets Spiral
// short ramps joining the centre bores to the tunnel's ends, both sloping down the marble's way
cylinder InRamp radius r_b height (hypot(r_mid, 7 mm)) base (0, 0, z_high + 3 mm) axis (r_mid/(1 mm), 0, -7) cut targets Spiral
cylinder OutRamp radius r_b height (hypot(r_mid, 7 mm) + 2 mm) base (r_mid, 0, z_low + 2 mm) axis (-r_mid/(1 mm), 0, -7) cut targets Spiral

// ==== Funnel: spigot, then a 45 deg cone out to the mouth with a short lip
sketch FunnelSk on XZ {
  polyline closed o = (r_b, 0) (r_s, 0) (r_s, -4 mm) (r_k, -8 mm) (r_f, -(8 mm + r_f - r_k)) (r_f, -(11 mm + r_f - r_k)) (r_f - 2.5 mm, -(11 mm + r_f - r_k)) (r_b, -(13.5 mm - r_k + r_b))
  fix o
}
revolve FunnelRev from FunnelSk axis Z new -> body Funnel

// ==== Splitter: a block with a socket on top forking to two spigots pitch apart
box SplitBlock size (2*pitch + 2*r_c) (2*r_c) splitter_h corner (-pitch - r_c, -r_c, 0) new -> body Splitter
sketch SplitSocketSk on XZ {
  polyline closed o = (0, -(splitter_h + 1 mm)) (r_k + fit + 1 mm, -(splitter_h + 1 mm)) (r_s + fit, -(splitter_h - 4 mm)) (r_s + fit, -(splitter_h - 8 mm)) (0, -(splitter_h - 8 mm))
  fix o
}
revolve SplitSocket from SplitSocketSk axis Z cut targets Splitter
for k from 0 to 1 {
  sketch SplitSpigotSk{k} on XZ {
    polyline closed o = ((2*k - 1)*pitch, -0.3 mm) ((2*k - 1)*pitch + r_k + 0.3 mm, -0.3 mm) ((2*k - 1)*pitch + r_s, 4 mm) ((2*k - 1)*pitch + r_s, 8 mm) ((2*k - 1)*pitch, 8 mm)
    construction line ax = ((2*k - 1)*pitch, -10 mm) ((2*k - 1)*pitch, 20 mm)
    fix o ax
  }
  revolve SplitSpigot{k} from SplitSpigotSk{k} axis SplitSpigotSk{k}.ax join targets Splitter
  cylinder SplitOutlet{k} radius r_b height 22 mm base ((2*k - 1)*pitch, 0, -9 mm) axis (0,0,1) cut targets Splitter
  cylinder SplitChannel{k} radius (r_b + 0.4 mm) height (hypot(pitch, splitter_h - 22 mm) + 2 mm) base ((2*k - 1)*1 mm, 0, splitter_h - 9 mm) axis ((2*k - 1)*pitch/(1 mm), 0, -(splitter_h - 22 mm)/(1 mm)) cut targets Splitter
}

// ==== Base: three sockets over a gutter that falls toward the tray at the +X end
box BaseBlock size (2*pitch + 2*r_c + tray_l) (2*r_c + 6 mm) base_t corner (-pitch - r_c, -r_c - 3 mm, 0) new -> body Base
for k from 0 to 2 {
  sketch BaseSocketSk{k} on XZ {
    polyline closed o = ((k - 1)*pitch, -(base_t + 1 mm)) ((k - 1)*pitch + r_k + fit + 1 mm, -(base_t + 1 mm)) ((k - 1)*pitch + r_s + fit, -(base_t - 4 mm)) ((k - 1)*pitch + r_s + fit, -(base_t - 8 mm)) ((k - 1)*pitch, -(base_t - 8 mm))
    construction line ax = ((k - 1)*pitch, 0) ((k - 1)*pitch, -10 mm)
    fix o ax
  }
  revolve BaseSocket{k} from BaseSocketSk{k} axis BaseSocketSk{k}.ax cut targets Base
}
// the gutter runs under all three sockets and falls 4 % to the tray
cylinder Gutter radius (r_b + 0.5 mm) height (2*pitch + r_c + 20 mm) base (-pitch - 15 mm, 0, base_t - 12 mm) axis (1, 0, -0.04) cut targets Base
box Tray size (tray_l - 4 mm) (2*r_c + 1 mm) (base_t - 2 mm) corner (pitch + r_c - 2 mm, -r_c - 0.5 mm, 3 mm) cut targets Base

// ==== Stack it up
move PlaceDrop bodies Drop to (-pitch, 0, base_t - 8 mm)
move PlaceSpiral bodies Spiral to (pitch, 0, base_t - 8 mm)
move PlaceSplitter bodies Splitter to (0, 0, base_t - 8 mm + tube_h)
move PlaceFunnel bodies Funnel to (0, 0, base_t - 16 mm + tube_h + splitter_h)
sphere MarbleA radius (marble_d/2) center (0, 0, base_t - 16 mm + tube_h + splitter_h + 25 mm) new -> body MarbleA
sphere MarbleB radius (marble_d/2) center (pitch + r_c + tray_l/2, 0, 3 mm + marble_d/2 + 0.2 mm) new -> body MarbleB

color Base "#2f4858"
color Drop "#f6ae2d"
color Spiral "#86bbd8"
color Splitter "#f26419"
color Funnel "#33658a"
color MarbleA, MarbleB "#a8e6cf"
material Base, Drop, Spiral, Splitter, Funnel PLA
material MarbleA, MarbleB Glass
