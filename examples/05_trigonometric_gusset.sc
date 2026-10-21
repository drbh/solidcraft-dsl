// 5. Trigonometry, area-valued helpers and powers. Volume = area*T.
design Gusset units mm
param run = 40 mm
param slope = 30 deg
param T = 5 mm
let rise = run*tan(slope)
let area = run*rise/2
let hypotenuse = sqrt(run^2 + rise^2)
sketch Triangle on XY {
  polyline closed edges = (0,0) (run,0) (run,rise)
  fix edges.1 edges.2 edges.3
}
extrude Web from Triangle distance T -> body GussetBody
