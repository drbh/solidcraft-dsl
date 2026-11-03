// One link of 112_drag_chain, in its own frame: travel along +X, the front pivot at the origin,
// the rear pivot at (p, 0, 0), Y across, Z up (z = 0 is the pivot line).
//
// Each side wall is two plates joined by a joggle block at mid-length: a narrow front plate
// (y in [Wi/2, Wi/2 + wt]) carrying a pin, and a wide rear plate one wall plus `clearance`
// further out carrying the hole. The next link's front plate sits inside this one's rear
// plate, its pin snapped into the hole (flex the walls in to fit it). Two teeth on the front
// plate make the bend stops: the top tooth reaches the previous link's joggle when the chain has
// turned `theta` toward +Z, and a stop just below the pivot line keeps it from bending the
// other way past straight.
// Bars across the top and bottom close the cable channel; they end at the joggle, clear of the
// neighbours. The link places itself: it turns a0 toward +Z about Y, then moves to (x0, 0, z0).
design DragLink units mm

param x0 = 0 mm
param z0 = 0 mm
param a0 = 0 deg
param p = 30 mm              // pitch, pivot to pivot
param theta = 30 deg         // the most two links turn against each other
param H = 20 mm              // link height
param Wi = 20 mm             // cable channel width
param wt = 2 mm
param bt = 3 mm              // bar thickness
param rp = 2.5 mm            // pin radius
param clearance = 0.3 mm

let jx = p/2                                    // joggle centre
let a_top = ((jx - 1.5 mm) - H/2*sin(theta))/cos(theta)
let a_bot = jx - 1.5 mm - clearance
let yo = Wi/2 + wt + clearance                  // rear plate's inner face
let yout = yo + wt                              // its outer face

// Front plates (narrow), both sides (sketch v is -z): the top tooth, the reverse stop 3 mm below
// the pivot line, then a corner sloping in to the bottom so no corner swings out under the chain
sketch FrontRSk on XZ offset (Wi/2) {
  polyline closed o = (-a_top, -H/2) (jx + 1 mm, -H/2) (jx + 1 mm, H/2) (0, H/2) (-7 mm, 7.5 mm) (-a_bot, 3 mm)
  fix o
}
extrude FrontR from FrontRSk distance wt new -> body Link
// Joggles: the step from each front plate out to its rear plate (also the bend stops' anvils)
box JoggleR size 3 mm (yout - Wi/2 - 0.6 mm) (H - 0.4 mm) corner (jx - 1.5 mm, Wi/2 + 0.3 mm, -H/2 + 0.2 mm) join targets Link
// Rear plates (wide): flat bottom, rounded top about the rear pivot
sketch RearRSk on XZ offset yo {
  line b = (jx - 1 mm, H/2) (p + H/2, H/2)
  line e = b.end (p + H/2, 0)
  arc r = start e.end through (p + H/2*cos(45 deg), -H/2*sin(45 deg)) end (p, -H/2)
  line t = r.end (jx - 1 mm, -H/2)
  line s = t.end b.start
  fix b e r t s
}
extrude RearR from RearRSk distance wt join targets Link

// Bars across the channel, between the front plates, ending in the joggles
box TopBar size (jx - 1 mm) (Wi + 1 mm) bt corner (2 mm, -Wi/2 - 0.5 mm, H/2 - bt - 0.2 mm) join targets Link
box BottomBar size (jx - 1 mm) (Wi + 1 mm) bt corner (2 mm, -Wi/2 - 0.5 mm, -H/2 + 0.2 mm) join targets Link

// The left side, joined through the bars
sketch FrontLSk on XZ offset (-Wi/2 - wt) {
  polyline closed o = (-a_top, -H/2) (jx + 1 mm, -H/2) (jx + 1 mm, H/2) (0, H/2) (-7 mm, 7.5 mm) (-a_bot, 3 mm)
  fix o
}
extrude FrontL from FrontLSk distance wt join targets Link

box JoggleL size 3 mm (yout - Wi/2 - 0.6 mm) (H - 0.4 mm) corner (jx - 1.5 mm, -yout + 0.3 mm, -H/2 + 0.2 mm) join targets Link

sketch RearLSk on XZ offset (-yout) {
  line b = (jx - 1 mm, H/2) (p + H/2, H/2)
  line e = b.end (p + H/2, 0)
  arc r = start e.end through (p + H/2*cos(45 deg), -H/2*sin(45 deg)) end (p, -H/2)
  line t = r.end (jx - 1 mm, -H/2)
  line s = t.end b.start
  fix b e r t s
}
extrude RearL from RearLSk distance wt join targets Link


// Pins on the front plates' outer faces, holes through the rear plates
cylinder PinR radius rp height (clearance + wt + 0.1 mm) base (0, Wi/2 + wt - 0.3 mm, 0) axis (0,1,0) join targets Link
cylinder PinL radius rp height (clearance + wt + 0.1 mm) base (0, -Wi/2 - wt + 0.3 mm, 0) axis (0,-1,0) join targets Link
cylinder Hole radius (rp + clearance) height (2*yout + 2 mm) base (p, -yout - 1 mm, 0) axis (0,1,0) cut targets Link

color Link "#2e3238"
material Link Nylon
move Turn bodies Link axis (0,-1,0) angle a0
move Place bodies Link to (x0, 0, z0)
