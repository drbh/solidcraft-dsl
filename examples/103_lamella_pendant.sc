// 103. Lamella pendant lamp: radial fins round an E27 socket, a ceiling canopy and a cord grip.
//
// Z is up, the lamp hangs on the Z axis. The Shade is one print: a top Hub plate with the bore an
// E27 lamp holder's shade ring clamps through, `fins` thin radial fins whose outer edge bows out
// on an arc, and a ring tying their lower ends. Fin count, bow and depth are parameters, so the
// shade can go from a few blades to a near-solid lantern. The cord runs up from the holder to
// the Canopy on the ceiling, where the CordGrip (a slotted collar pinched by an M3 screw) holds
// the cable's weight off the terminals.
//
// Print (PLA or PETG; use LED bulbs only, about 8 W or less):
//   Shade upside down, hub plate on the bed. Each fin is a thin wall whose edges lean at most
//   about 45 deg; the ring at the top of the print bridges `fin gap` between fins.
//   Canopy open side down; CordGrip flange down.
// BOM: E27 lamp holder with a threaded shell and shade ring (40 mm thread), 2-core 0.75 mm2 round
// cable, LED A60 bulb, 2 x M4 ceiling screws and plugs, 1 x M3x10 screw.
design LamellaPendant units mm

param shade_h = 180 mm        // hub plate top to the lower ring
param r_rim = 84 mm           // outer radius at the lower ring
param r_bow = 110 mm          // outer radius at mid height
param r_hub = 52 mm           // hub plate radius
param fins = 24
param fin_t = 1.6 mm
param fin_depth = 26 mm       // fin depth at mid height
param hub_t = 4 mm
param holder_bore = 40.6 mm   // E27 threaded shell + 0.6 mm
param ring_w = 8 mm
param ring_h = 3 mm
param drop = 220 mm           // cord length from the hub to the ceiling
param canopy_r = 50 mm
param canopy_h = 28 mm
param cord_d = 7 mm

let top_z = shade_h
let ceiling = shade_h + drop

// ==== Shade: the hub plate, then the fins, then the lower ring that ties their ends
cylinder HubPlate radius r_hub height hub_t base (0, 0, top_z - hub_t) axis (0,0,1) new -> body Shade
cylinder HubBore radius (holder_bore/2) height (hub_t + 2 mm) base (0, 0, top_z - hub_t - 1 mm) axis (0,0,1) cut targets Shade
// Each fin bows out on an arc, its ends sunk well inside the hub plate and the ring (an arc that
// leaves a plate through its round edge makes every join retry). Each fin is drawn on its own plane turned k/fins of a turn about Z (cheaper and cleaner than a
// feature pattern of 24 joins). `fins` is read when compiling: up to 48, rebuild to change it.
for k from 0 to 47 {
  if k < fins {
    plane FinPlane{k} on XZ angle (k*360 deg/fins) axis Z
    sketch FinSk{k} on FinPlane{k} {
      arc outer{k} = start (r_rim - 3 mm, -1 mm) through (r_bow, -shade_h/2) end (r_hub - 10 mm, -(top_z - 1.5 mm))
      line top{k} = outer{k}.end (holder_bore/2 + 3 mm, -(top_z - 1.5 mm))
      arc inner{k} = start top{k}.end through (r_bow - fin_depth, -shade_h/2) end (r_rim - ring_w + 2 mm, -1 mm)
      line bottom{k} = inner{k}.end outer{k}.start
      fix outer{k} top{k} inner{k} bottom{k}
    }
    extrude Fin{k} from FinSk{k} distance fin_t symmetric join targets Shade
  }
}
sketch RingSk on XY {
  circle o = (0, 0) radius r_rim
  circle i = (0, 0) radius (r_rim - ring_w)
  fix o i
}
extrude Ring from RingSk distance ring_h join targets Shade


// ==== Lamp holder and bulb, for fit only (not printed)
cylinder RefHolder radius 20 mm height 58 mm base (0, 0, top_z - 48 mm) axis (0,0,1) new -> body RefHolder
sketch BulbSk on XZ {
  line ax = (0, -(top_z - 48 mm)) (0, -(top_z - 48 mm - 108 mm))
  arc glass = start ax.end through (30 mm, -(top_z - 48 mm - 70 mm)) end (13 mm, -(top_z - 48 mm))
  line neck = glass.end ax.start
  fix ax glass neck
}
revolve RefBulb from BulbSk axis Z -> body RefBulb

// ==== Canopy: a cup screwed to the ceiling, the cord through its floor
sketch CanopySk on XZ {
  polyline closed o = (cord_d/2 + 4 mm, -(ceiling - canopy_h)) (canopy_r - 8 mm, -(ceiling - canopy_h)) (canopy_r, -(ceiling - 8 mm)) (canopy_r, -ceiling) (canopy_r - 2 mm, -ceiling) (canopy_r - 2 mm, -(ceiling - 8 mm + 0.8 mm)) (canopy_r - 8 mm - 0.8 mm, -(ceiling - canopy_h + 2 mm)) (cord_d/2 + 4 mm, -(ceiling - canopy_h + 2 mm))
  fix o
}
revolve Canopy from CanopySk axis Z -> body Canopy
// a ring of ceiling-screw bosses inside the rim (M4 clearance)
cylinder BossA radius 5 mm height (canopy_h - 2.5 mm) base (canopy_r - 14 mm, 0, ceiling - canopy_h + 1.5 mm) axis (0,0,1) join targets Canopy
cylinder BossB radius 5 mm height (canopy_h - 2.5 mm) base (-(canopy_r - 14 mm), 0, ceiling - canopy_h + 1.5 mm) axis (0,0,1) join targets Canopy
cylinder ScrewA radius 2.25 mm height (canopy_h + 2 mm) base (canopy_r - 14 mm, 0, ceiling - canopy_h - 1 mm) axis (0,0,1) cut targets Canopy
cylinder ScrewB radius 2.25 mm height (canopy_h + 2 mm) base (-(canopy_r - 14 mm), 0, ceiling - canopy_h - 1 mm) axis (0,0,1) cut targets Canopy

// ==== Cord grip: a flanged, slotted collar in the canopy floor, pinched by an M3 screw
sketch GripSk on XZ {
  polyline closed o = (cord_d/2, -(ceiling - canopy_h - 6 mm)) (cord_d/2 + 3.6 mm, -(ceiling - canopy_h - 6 mm)) (cord_d/2 + 3.6 mm, -(ceiling - canopy_h + 2.3 mm)) (cord_d/2 + 7 mm, -(ceiling - canopy_h + 2.3 mm)) (cord_d/2 + 7 mm, -(ceiling - canopy_h + 4.3 mm)) (cord_d/2, -(ceiling - canopy_h + 4.3 mm))
  fix o
}
revolve CordGrip from GripSk axis Z -> body CordGrip
box GripSlot size 1.6 mm (cord_d + 10 mm) 7 mm corner (-0.8 mm, 0, ceiling - canopy_h - 6.5 mm) cut targets CordGrip
cylinder GripScrew radius 1.25 mm height (cord_d + 12 mm) base (-(cord_d/2 + 6 mm), cord_d/2 + 1.8 mm, ceiling - canopy_h - 2.5 mm) axis (1,0,0) cut targets CordGrip
// the canopy floor's hole is the collar plus clearance
cylinder GripHole radius (cord_d/2 + 3.6 mm + 0.3 mm) height 4 mm base (0, 0, ceiling - canopy_h - 1 mm) axis (0,0,1) cut targets Canopy

check CordGrip clearance Canopy >= 0.2 mm
check RefHolder clearance Shade >= 0.2 mm

color Shade "#e8e2d6"
color Canopy, CordGrip "#e8e2d6"
color RefHolder "#2b2b2b"
color RefBulb "#fff6d8"
material Shade, Canopy, CordGrip PLA
