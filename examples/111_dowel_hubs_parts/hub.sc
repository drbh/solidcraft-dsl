// A dowel hub of 111_dowel_hubs: a core block with a socket on any of its six sides.
//
// Centred on the origin, Z up as printed. Flags px, nx, py, ny, pz, nz (1 or 0) choose the
// sockets: a corner is px py pz, a tee px nx py, a cross px nx py ny. Every socket takes a
// dowel or pipe of diameter `dowel_d` with `fit` radial play and an M3 self-tapping set screw
// through a pilot in the socket's wall. Sideways sockets are octagons with a flat top (the top
// bridges a short span and the other flats lean 45 deg), so the hub prints upright without
// supports; upward sockets are round. Print downward sockets (nz) upside down.
design DowelHub units mm

param dowel_d = 12.7 mm
param fit = 0.15 mm
param wall = 3 mm
param depth = 19 mm
param pilot_d = 2.5 mm
param px = 1
param nx = 0
param py = 1
param ny = 0
param pz = 1
param nz = 0

let ri = dowel_d/2 + fit
let ro = ri/cos(22.5 deg)
let od = 2*ro + 2*wall
let s = od + 1.2 mm
let a = ro*sin(22.5 deg)
let b = ro*cos(22.5 deg)

box Core size s s s corner (-s/2, -s/2, -s/2) new -> body Hub

if px > 0 {
  cylinder ArmPX diameter od height (depth + 0.5 mm) base (s/2 - 0.5 mm, 0, 0) axis (1,0,0) join targets Hub
  sketch BorePXSk on YZ offset (s/2 + depth + 0.5 mm) {
    polyline closed o = (a, b) (b, a) (b, -a) (a, -b) (-a, -b) (-b, -a) (-b, a) (-a, b)
    fix o
  }
  extrude BorePX from BorePXSk distance (depth + 1.5 mm) negative cut targets Hub
  cylinder PilotPX diameter pilot_d height (od/2 + 1 mm) base (s/2 + depth/2, 0, 0) axis (0,0,1) cut targets Hub
}
if nx > 0 {
  cylinder ArmNX diameter od height (depth + 0.5 mm) base (-s/2 + 0.5 mm, 0, 0) axis (-1,0,0) join targets Hub
  sketch BoreNXSk on YZ offset (-s/2 - depth - 0.5 mm) {
    polyline closed o = (a, b) (b, a) (b, -a) (a, -b) (-a, -b) (-b, -a) (-b, a) (-a, b)
    fix o
  }
  extrude BoreNX from BoreNXSk distance (depth + 1.5 mm) cut targets Hub
  cylinder PilotNX diameter pilot_d height (od/2 + 1 mm) base (-s/2 - depth/2, 0, 0) axis (0,0,1) cut targets Hub
}
if py > 0 {
  cylinder ArmPY diameter od height (depth + 0.5 mm) base (0, s/2 - 0.5 mm, 0) axis (0,1,0) join targets Hub
  sketch BorePYSk on XZ offset (s/2 + depth + 0.5 mm) {
    polyline closed o = (a, b) (b, a) (b, -a) (a, -b) (-a, -b) (-b, -a) (-b, a) (-a, b)
    fix o
  }
  extrude BorePY from BorePYSk distance (depth + 1.5 mm) negative cut targets Hub
  cylinder PilotPY diameter pilot_d height (od/2 + 1 mm) base (0, s/2 + depth/2, 0) axis (0,0,1) cut targets Hub
}
if ny > 0 {
  cylinder ArmNY diameter od height (depth + 0.5 mm) base (0, -s/2 + 0.5 mm, 0) axis (0,-1,0) join targets Hub
  sketch BoreNYSk on XZ offset (-s/2 - depth - 0.5 mm) {
    polyline closed o = (a, b) (b, a) (b, -a) (a, -b) (-a, -b) (-b, -a) (-b, a) (-a, b)
    fix o
  }
  extrude BoreNY from BoreNYSk distance (depth + 1.5 mm) cut targets Hub
  cylinder PilotNY diameter pilot_d height (od/2 + 1 mm) base (0, -s/2 - depth/2, 0) axis (0,0,1) cut targets Hub
}
if pz > 0 {
  cylinder ArmPZ diameter od height (depth + 0.5 mm) base (0, 0, s/2 - 0.5 mm) axis (0,0,1) join targets Hub
  cylinder BorePZ diameter (2*ri) height (depth + 1.5 mm) base (0, 0, s/2 - 1 mm) axis (0,0,1) cut targets Hub
  cylinder PilotPZ diameter pilot_d height (od/2 + 1 mm) base (0, 0, s/2 + depth/2) axis (1,0,0) cut targets Hub
}
if nz > 0 {
  cylinder ArmNZ diameter od height (depth + 0.5 mm) base (0, 0, -s/2 + 0.5 mm) axis (0,0,-1) join targets Hub
  cylinder BoreNZ diameter (2*ri) height (depth + 1.5 mm) base (0, 0, -s/2 + 1 mm) axis (0,0,-1) cut targets Hub
  cylinder PilotNZ diameter pilot_d height (od/2 + 1 mm) base (0, 0, -s/2 - depth/2) axis (1,0,0) cut targets Hub
}

color Hub "#e8762c"
material Hub PLA
