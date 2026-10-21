// 8. Shell face selection, derived internal dimensions and two separate bodies.
design Enclosure units mm
param L = 60 mm
param W = 40 mm
param H = 25 mm
param wall = 3 mm
let inside_L = L-2*wall
let inside_W = W-2*wall
let cavity_volume = inside_L*inside_W*(H-wall)
box Blank size L W H -> body EnclosureBody
shell Cavity faces (L/2,W/2,H) thickness wall inside body EnclosureBody
box Lid size L W wall corner (0,W+10 mm,0) new -> body LidBody
