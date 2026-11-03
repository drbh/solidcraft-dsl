// 130. Archimedes-screw marble lift for the 109 marble run: turn the knob and marbles climb the tube.
//
// Z is up, the lift lies in the XZ plane, its axis rising at `incline` from the bottom end P0. Inside
// the tube is a shaftless auger, a wire helix like a stiff spring, hung from a hub at its top end. A
// marble dropped into the intake lies in the bottom of the tube between two turns of the wire; turning
// the knob screws the wire up the tube and pushes the marble ahead of it, because gravity keeps the
// marble in the bottom of the inclined tube instead of letting it turn with the screw. At the top it
// reaches the hub, drops through the outlet and down the chute.
//
// It joins the run on 109's interface: the intake on top of the tube's lower end is a standard socket
// (a 109 piece's spigot plugs in, or drop marbles in by hand), and the chute ends in a standard
// spigot that plugs into any 109 socket below it. marble_d, fit and the socket sizes match 109.
//
// `crank` is live: the auger, shaft and knob turn about the tube's axis.
//
// Parts: LiftA and its mirror "LiftA (2)" (the tube, intake and chute, halved down the XZ plane so each
// half prints flat on its cut face; three ears take M3 bolts to clamp them), Auger (wire helix, hub and boss; print it hub
// down, the helix stands up like a spring), Shaft (an 8 mm rod pinned into the auger's boss and the
// knob; print it lying down), Knob (print it disc down, handle up), Stand (base, front block and rear
// post with cradles for the tube; print as drawn). PLA, no supports.
// Hardware: 3 x M3x25 bolts and nuts, two 20 mm lengths of 1.75 mm filament as cross pins.
design MarbleScrewLift units mm

param marble_d = 16 mm
param fit = 0.3 mm
param crank = 0 deg
param incline = 40 deg
param lift_l = 200 mm        // tube length along its axis
param chute_l = 40 mm        // outlet to the spigot's shoulder
param wire_d = 5 mm
param lead = 24 mm           // the auger's pitch: wire_d plus room for a marble

let r_b = marble_d/2 + 1.5 mm        // 109's bore, socket and spigot sizes
let r_s = r_b + 2.5 mm
let r_k = r_s + 4 mm
let r_c = r_k + 3 mm
let r_in = marble_d/2 + 11.5 mm      // tube bore: the auger's wire runs 1 mm inside it
let r_o = r_in + 2.5 mm
let r_w = r_in - 1 mm - wire_d/2     // wire centre line radius
let ca = cos(incline)
let sa = sin(incline)
let z0 = r_o/ca + 4 mm               // P0 = (0, 0, z0): the tube's lower end on its axis
let s_in = 30 mm                     // intake, along the axis from P0
let s_out = lift_l - 36 mm           // outlet
let s_hub = lift_l - 26 mm           // the auger's hub
let x_in = s_in*ca
let z_in = z0 + s_in*sa
let x_out = s_out*ca
let z_out = z0 + s_out*sa
let z_sh = z_out - chute_l           // chute spigot's shoulder

// ==== Tube with its intake collar, chute and clamp ears (one body, split at the end)
cylinder TubeOuter radius r_o height lift_l base (0, 0, z0) axis (ca, 0, sa) new -> body LiftA
cylinder InCollar radius r_c height (r_o/ca + 16 mm) base (x_in, 0, z_in) axis (0,0,1) join targets LiftA
cylinder Chute radius r_c height (chute_l + 0.1 mm) base (x_out, 0, z_sh - 0.1 mm) axis (0,0,1) join targets LiftA
sketch SpigotSk on XZ {
  polyline closed o = (x_out, -(z_sh + 0.3 mm)) (x_out + r_k + 0.3 mm, -(z_sh + 0.3 mm)) (x_out + r_s - 0.1 mm, -(z_sh - 4 mm)) (x_out + r_s, -(z_sh - 4 mm)) (x_out + r_s, -(z_sh - 8 mm)) (x_out, -(z_sh - 8 mm))
  construction line ax = (x_out, -(z_sh + 10 mm)) (x_out, -(z_sh - 20 mm))
  fix o ax
}
revolve Spigot from SpigotSk axis SpigotSk.ax join targets LiftA
for k from 0 to 2 {
  box Ear{k} size 12 mm 12 mm 12 mm corner ((80 mm + 45 mm*k)*ca - (r_o + 3 mm)*sa - 6 mm, -6 mm, z0 + (80 mm + 45 mm*k)*sa + (r_o + 3 mm)*ca - 6 mm) join targets LiftA
}
// the insides: bore with end caps, intake socket and hole, outlet hole and chute bore, bolt holes
cylinder TubeBore radius r_in height (lift_l - 6 mm) base (3 mm*ca, 0, z0 + 3 mm*sa) axis (ca, 0, sa) cut targets LiftA
cylinder TopBearing radius (4 mm + fit) height 8 mm base ((lift_l - 5 mm)*ca, 0, z0 + (lift_l - 5 mm)*sa) axis (ca, 0, sa) cut targets LiftA
sketch InSocketSk on XZ {
  polyline closed o = (x_in, -(z_in + r_o/ca + 17 mm)) (x_in + r_k + fit + 1 mm, -(z_in + r_o/ca + 17 mm)) (x_in + r_s + fit, -(z_in + r_o/ca + 12 mm)) (x_in + r_s + fit, -(z_in + r_o/ca + 8 mm)) (x_in, -(z_in + r_o/ca + 8 mm))
  construction line ax = (x_in, -(z_in + r_o/ca + 30 mm)) (x_in, -z_in)
  fix o ax
}
revolve InSocket from InSocketSk axis InSocketSk.ax cut targets LiftA
cylinder InHole radius r_b height (r_o/ca + 9 mm) base (x_in, 0, z_in) axis (0,0,1) cut targets LiftA
cylinder OutHole radius r_b height (chute_l + 8.2 mm) base (x_out, 0, z_sh - 8.1 mm) axis (0,0,1) cut targets LiftA
for k from 0 to 2 {
  cylinder EarBolt{k} radius 1.7 mm height 14 mm base ((80 mm + 45 mm*k)*ca - (r_o + 3 mm)*sa, -7 mm, z0 + (80 mm + 45 mm*k)*sa + (r_o + 3 mm)*ca) axis (0,1,0) cut targets LiftA
}
// keep the +Y half and mirror it for the other. Each half stops 0.25 mm short of the mid-plane, so the
// bolts clamp them tight (a cut exactly on y = 0, through the cylinders' seams, makes the engine retry)
box HalfCut size (lift_l + 100 mm) 100 mm (lift_l + 100 mm) corner (-100 mm, -99.75 mm, -50 mm) cut targets LiftA
mirror OtherHalf bodies LiftA plane XZ

// ==== Auger: wire helix from just above the bottom cap to the hub; boss with a cross-pin hole
// (`coil` takes parameters, not lets, so its sizes are written out: r_w, s_hub and z0 from above)
coil Wire diameter (marble_d + 21 mm - wire_d) turns ((lift_l - 32 mm)/lead) pitch lead section_size wire_d base (6 mm*cos(incline), 0, (marble_d/2 + 14 mm)/cos(incline) + 4 mm + 6 mm*sin(incline)) axis (cos(incline), 0, sin(incline)) new -> body Auger
cylinder Hub radius (r_w + wire_d/2) height 4 mm base ((s_hub - 1 mm)*ca, 0, z0 + (s_hub - 1 mm)*sa) axis (ca, 0, sa) join targets Auger
cylinder Boss radius 7 mm height 10 mm base ((s_hub + 2.9 mm)*ca, 0, z0 + (s_hub + 2.9 mm)*sa) axis (ca, 0, sa) join targets Auger
cylinder BossBore radius (4 mm + 0.15 mm) height 9 mm base ((s_hub + 4 mm)*ca, 0, z0 + (s_hub + 4 mm)*sa) axis (ca, 0, sa) cut targets Auger
cylinder BossPin radius 1 mm height 16 mm base ((s_hub + 8 mm)*ca, -8 mm, z0 + (s_hub + 8 mm)*sa) axis (0,1,0) cut targets Auger

// ==== Drive shaft (a printed rod, pinned at both ends) and the knob with its handle
cylinder ShaftRod radius 4 mm height (lift_l + 9.5 mm - s_hub - 4.5 mm) base ((s_hub + 4.5 mm)*ca, 0, z0 + (s_hub + 4.5 mm)*sa) axis (ca, 0, sa) new -> body Shaft
cylinder ShaftPinLow radius 1 mm height 10 mm base ((s_hub + 8 mm)*ca, -5 mm, z0 + (s_hub + 8 mm)*sa) axis (0,1,0) cut targets Shaft
cylinder ShaftPinHigh radius 1 mm height 10 mm base ((lift_l + 6 mm)*ca, -5 mm, z0 + (lift_l + 6 mm)*sa) axis (0,1,0) cut targets Shaft
cylinder KnobDisc radius 20 mm height 8 mm base ((lift_l + 2 mm)*ca, 0, z0 + (lift_l + 2 mm)*sa) axis (ca, 0, sa) new -> body Knob
cylinder KnobBore radius (4 mm + 0.15 mm) height 7.7 mm base ((lift_l + 1.9 mm)*ca, 0, z0 + (lift_l + 1.9 mm)*sa) axis (ca, 0, sa) cut targets Knob
cylinder KnobPin radius 1 mm height 42 mm base ((lift_l + 6 mm)*ca, -21 mm, z0 + (lift_l + 6 mm)*sa) axis (0,1,0) cut targets Knob
cylinder Handle radius 4.5 mm height 22 mm base ((lift_l + 9.9 mm)*ca, 13 mm, z0 + (lift_l + 9.9 mm)*sa) axis (ca, 0, sa) join targets Knob

// ==== Stand: base, a block under the lower end and a post under the upper part, tube cradles
box StandBase size (lift_l*ca + 40 mm) 60 mm 4 mm corner (-20 mm, -30 mm, 0) new -> body Stand
box FrontBlock size 30 mm 36 mm (z0 + 20 mm*sa) corner (0, -18 mm, 3.9 mm) join targets Stand
box RearPost size 20 mm 36 mm (z0 + 120 mm*sa) corner (120 mm*ca - 10 mm, -18 mm, 3.9 mm) join targets Stand
cylinder Cradle radius (r_o + fit) height (lift_l + 40 mm) base (-20 mm*ca, 0, z0 - 20 mm*sa) axis (ca, 0, sa) cut targets Stand

// ==== Pose: the auger and knob turn about the tube's axis
move ToAxis bodies Auger, Shaft, Knob to (0, 0, -z0)
move Turn bodies Auger, Shaft, Knob axis (ca, 0, sa) angle crank
move FromAxis bodies Auger, Shaft, Knob to (0, 0, z0)

sphere Marble radius (marble_d/2) center (60 mm*ca + (r_in - marble_d/2 - 0.5 mm)*sa, 0, z0 + 60 mm*sa - (r_in - marble_d/2 - 0.5 mm)*ca) new -> body Marble

check Auger clearance LiftA >= 0.3 mm
check Shaft clearance LiftA >= 0.2 mm
check Knob clearance LiftA >= 0.2 mm

color LiftA "#86bbd8"
color Auger "#f6ae2d"
color Knob, Shaft "#f26419"
color Stand "#2f4858"
color Marble "#a8e6cf"
material LiftA, Auger, Shaft, Knob, Stand PLA
material Marble Glass
