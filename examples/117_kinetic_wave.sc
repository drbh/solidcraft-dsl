// 117. Kinetic wave desk sculpture: a hand-cranked camshaft lifts a row of sliders into a travelling wave.
//
// X runs along the row, Z up, the camshaft on the X axis at z = shaft_z. Eight eccentric cams sit on
// a hex shaft, each turned `phase` further than the last; every cam carries a slider that stands on
// it by gravity and runs in a slot in the comb above. The top of an eccentric disc rises and falls
// as e*cos(angle), so the sliders' caps trace a sine wave that walks along the row as the crank turns.
//
// `crank` is live, and so is the wave. The shaft, cams and crank turn about the shaft. Each slider has
// to move straight up and down by e*(cos(crank + phase) - 1) from where it is drawn (on top of its
// cam's peak), and a `move ... to` is fixed when compiling, so the slide is built from four rotations
// about X: -t about the X axis then +t about a line e/2 below it moves a body by (R(t) - I)*(0,0,e/2);
// +t then -t moves it by (R(-t) - I)*(0,0,e/2); together that is exactly (0, 0, e*(cos t - 1)).
//
// Parts: Frame (base with two bearing cheeks), Comb (rests on the cheeks, two M3 screws), Shaft (hex,
// turns in round bearing holes), Cam0..7 (hex bore, each marked by its own lobe position), Slider0..7,
// Crank (hex bore, handle). Print in PLA without supports: the frame as drawn, the comb flat, the shaft
// on a flat of its hex, cams and crank flat, sliders on their sides. Push the cams onto the shaft in
// order with the lobes stepping round by `phase`, and fix each with a drop of glue.
// Hardware: 2 x M3x10 screws.
design KineticWave units mm

param crank = 30 deg
param pitch_x = 14 mm        // cam and slider spacing along the row
param ecc = 6 mm             // cam eccentricity: the wave is 2*ecc high
param cam_r = 12 mm
param cam_t = 9 mm
param shaft_z = 28 mm
param hex_r = 4.6 mm         // hex shaft, centre to corner
param fit = 0.25 mm
param bar_l = 22 mm          // slider stem, foot to cap

let phase = 45 deg           // 360 deg over the 8 cams: one full wavelength along the row
let x0 = 10 mm               // first cam's centre
let x_end = x0 + 7*pitch_x + cam_t/2 + 3.5 mm      // inner face of the right cheek
let comb_z = shaft_z + cam_r + ecc + 3 mm + 3 mm   // above the highest foot
let foot_z = shaft_z + cam_r + ecc + 0.2 mm        // foot underside, slider drawn at its cam's peak

// ==== Frame: base and two cheeks with bearing holes, pilot holes for the comb
box FrameBase size (x_end + 12 mm) 48 mm 4 mm corner (-6 mm, -24 mm, 0) new -> body Frame
box CheekL size 6 mm 32 mm (comb_z - 3.9 mm) corner (-6 mm, -16 mm, 3.9 mm) join targets Frame
box CheekR size 6 mm 32 mm (comb_z - 3.9 mm) corner (x_end, -16 mm, 3.9 mm) join targets Frame
cylinder BearingL radius (hex_r + 0.4 mm) height 8 mm base (-7 mm, 0, shaft_z) axis (1,0,0) cut targets Frame
cylinder BearingR radius (hex_r + 0.4 mm) height 8 mm base (x_end - 1 mm, 0, shaft_z) axis (1,0,0) cut targets Frame
cylinder PilotL radius 1.25 mm height 9 mm base (-3 mm, 0, comb_z - 8.9 mm) axis (0,0,1) cut targets Frame
cylinder PilotR radius 1.25 mm height 9 mm base (x_end + 3 mm, 0, comb_z - 8.9 mm) axis (0,0,1) cut targets Frame

// ==== Comb: a bar across the cheek tops with one slot per slider
box CombBar size (x_end + 12 mm) 22 mm 4 mm corner (-6 mm, -11 mm, comb_z) new -> body Comb
sketch CombSlotSk on XY offset (comb_z - 1 mm) {
  rect s = (x0 - 3 mm - fit, -7 mm - fit) (x0 + 3 mm + fit, 7 mm + fit)
  fix s
  pattern rectangular s count 8 spacing pitch_x
}
extrude CombSlots from CombSlotSk distance 6 mm cut targets Comb
cylinder CombScrewL radius 1.7 mm height 6 mm base (-3 mm, 0, comb_z - 1 mm) axis (0,0,1) cut targets Comb
cylinder CombScrewR radius 1.7 mm height 6 mm base (x_end + 3 mm, 0, comb_z - 1 mm) axis (0,0,1) cut targets Comb

// ==== Shaft: a hex bar through both cheeks and out to the crank
sketch ShaftSk on YZ offset (-10 mm) {
  polygon h = (0, shaft_z) radius hex_r sides 6
  fix h
}
extrude ShaftBar from ShaftSk distance (x_end + 26 mm) new -> body Shaft

// ==== Cams and sliders, one per position
for k from 0 to 7 {
  cylinder Cam{k} radius cam_r height cam_t base (x0 + k*pitch_x - cam_t/2, -ecc*sin(k*phase), shaft_z + ecc*cos(k*phase)) axis (1,0,0) new -> body Cam{k}
  sketch CamBoreSk{k} on YZ offset (x0 + k*pitch_x - cam_t/2 - 1 mm) {
    polygon h = (0, shaft_z) radius (hex_r + fit) sides 6
    fix h
  }
  extrude CamBore{k} from CamBoreSk{k} distance (cam_t + 2 mm) cut targets Cam{k}
  sketch SliderSk{k} on XZ offset (-7 mm) {
    polyline closed o = (x0 + k*pitch_x - 5 mm, -foot_z) (x0 + k*pitch_x + 5 mm, -foot_z) (x0 + k*pitch_x + 5 mm, -(foot_z + 3 mm)) (x0 + k*pitch_x + 3 mm, -(foot_z + 3 mm)) (x0 + k*pitch_x + 3 mm, -(foot_z + 3 mm + bar_l)) (x0 + k*pitch_x + 6 mm, -(foot_z + 3 mm + bar_l)) (x0 + k*pitch_x + 6 mm, -(foot_z + 8 mm + bar_l)) (x0 + k*pitch_x - 6 mm, -(foot_z + 8 mm + bar_l)) (x0 + k*pitch_x - 6 mm, -(foot_z + 3 mm + bar_l)) (x0 + k*pitch_x - 3 mm, -(foot_z + 3 mm + bar_l)) (x0 + k*pitch_x - 3 mm, -(foot_z + 3 mm)) (x0 + k*pitch_x - 5 mm, -(foot_z + 3 mm))
    fix o
  }
  extrude Slider{k} from SliderSk{k} distance 14 mm new -> body Slider{k}
}

// ==== Crank: hex bore on the shaft's end, arm, handle
sketch CrankSk on YZ offset (x_end + 8 mm) {
  slot s = centers (0, shaft_z) (0, shaft_z + 28 mm) width 16 mm
  fix s
}
extrude CrankArm from CrankSk distance 6 mm new -> body Crank
sketch CrankBoreSk on YZ offset (x_end + 7 mm) {
  polygon h = (0, shaft_z) radius (hex_r + fit) sides 6
  fix h
}
extrude CrankBore from CrankBoreSk distance 8 mm cut targets Crank
cylinder CrankHandle radius 4.5 mm height 24 mm base (x_end + 13.9 mm, 0, shaft_z + 28 mm) axis (1,0,0) join targets Crank

// ==== Pose: turn the shaft, cams and crank; slide each slider to its cam's new height
move ShaftDown bodies Shaft, Crank, Cam0, Cam1, Cam2, Cam3, Cam4, Cam5, Cam6, Cam7 to (0, 0, -shaft_z)
move ShaftTurn bodies Shaft, Crank, Cam0, Cam1, Cam2, Cam3, Cam4, Cam5, Cam6, Cam7 axis (1,0,0) angle crank
move ShaftUp bodies Shaft, Crank, Cam0, Cam1, Cam2, Cam3, Cam4, Cam5, Cam6, Cam7 to (0, 0, shaft_z)
for k from 0 to 7 {
  move SlideA{k} bodies Slider{k} axis (1,0,0) angle (-(crank + k*phase))
  move SlideB{k} bodies Slider{k} to (0, 0, ecc/2)
  move SlideC{k} bodies Slider{k} axis (1,0,0) angle (crank + k*phase)
  move SlideD{k} bodies Slider{k} to (0, 0, -ecc/2)
  move SlideE{k} bodies Slider{k} axis (1,0,0) angle (crank + k*phase)
  move SlideF{k} bodies Slider{k} to (0, 0, ecc/2)
  move SlideG{k} bodies Slider{k} axis (1,0,0) angle (-(crank + k*phase))
  move SlideH{k} bodies Slider{k} to (0, 0, -ecc/2)
}

check Slider0 clearance Cam0 >= 0.1 mm
check Slider3 clearance Cam3 >= 0.1 mm
check Slider5 clearance Comb >= 0.1 mm

color Frame "#2b2d42"
color Comb "#8d99ae"
color Shaft, Crank "#edf2f4"
color Cam0, Cam1, Cam2, Cam3, Cam4, Cam5, Cam6, Cam7 "#ef233c"
color Slider0, Slider1, Slider2, Slider3, Slider4, Slider5, Slider6, Slider7 "#219ebc"
material Frame, Comb, Shaft, Crank, Cam0, Cam1, Cam2, Cam3, Cam4, Cam5, Cam6, Cam7, Slider0, Slider1, Slider2, Slider3, Slider4, Slider5, Slider6, Slider7 PLA
