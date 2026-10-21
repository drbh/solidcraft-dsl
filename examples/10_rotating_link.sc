// 10. Persistent expression-bound points, shared references and counterbored holes.
// Requires an engine with expression-driven sketch points (current development source).
// Changing angle or L in the native design moves the outline AND both holes.
design RotatingLink units mm
param L = 60 mm
param W = 20 mm
param T = 5 mm
param angle = 30 deg
param inset = 10 mm
param hole_d = 6 mm
param cb_d = 8 mm
param cb_depth = 2 mm
let ux = cos(angle)
let uy = sin(angle)
let hx = L/2
let hy = W/2
let pitch = hx-inset
sketch Outline on XY {
  bind point a = (-hx*ux+hy*uy, -hx*uy-hy*ux)
  bind point b = (hx*ux+hy*uy, hx*uy-hy*ux)
  bind point c = (hx*ux-hy*uy, hx*uy+hy*ux)
  bind point d = (-hx*ux-hy*uy, -hx*uy+hy*ux)
  polyline closed rim = a b c d
}
extrude Link from Outline distance T -> body LinkBody
sketch Holes on XY offset T {
  bind point left = (-pitch*ux,-pitch*uy)
  bind point right = (pitch*ux,pitch*uy)
}
hole Bores at Holes diameter hole_d counterbore cb_d cb_depth depth T
