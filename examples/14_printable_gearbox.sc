// 14. Printable planetary gearbox, 4:1, assembled with M3 screws.
//
// Parts (each its own body, every one prints flat without supports):
//   Housing     ring gear cut into its wall; floor bore for the input; 4 ears with M3 pilot holes
//   Lid         centring spigot, output bore, 4 x M3 clearance holes   -> 4 x M3x20 into Housing
//   Sun         involute sun gear with a D-flat input shaft (prints gear-down)
//   Planet x3   involute planets that run on the carrier pins
//   CarrierBase plate with three pins; each pin has an M3 pilot hole
//   CarrierTop  plate with counterbored M3 holes and the D-flat output shaft -> 3 x M3x12 into the pins
//   InputCrank  crank pressed onto the D-flat input shaft under the housing
//   OutputCrank crank pressed onto the D-flat output shaft above the lid
// Screws self-tap into the 2.5 mm pilot holes. Fit is set by backlash, gap and the *_clear values.
// Planet tooth phase assumes even tooth counts and (z_sun + z_ring) divisible by 3.
design PrintableGearbox units mm

// Gearing
param gm = 1.5 mm
param z_sun = 18
param z_planet = 18
param b = 10 mm
param flank = 20 deg
param backlash = 0.5 mm
let z_ring = z_sun + 2*z_planet
let orbit = gm*(z_sun+z_planet)/2

// Stack and fits
param gap = 0.5 mm
param floor_t = 3 mm
param base_t = 4 mm
param top_t = 5 mm
param lid_t = 4 mm
param wall = 4 mm
param pin_clear = 0.2 mm
param shaft_clear = 0.25 mm
param in_r = 4 mm
param out_r = 5 mm
param planet_bore_r = 4 mm
param m3_pilot = 2.5 mm
param m3_clear = 3.4 mm
param m3_head = 6.5 mm
param m3_head_h = 3.3 mm
let base_z = floor_t + gap
let gear_z = base_z + base_t + gap
let top_z = gear_z + b + gap
let wall_h = top_z + top_t + gap
let lid_top = wall_h + lid_t

// Sun gear
let sun_rp = gm*z_sun/2
let sun_ra = sun_rp + gm
let sun_rf = sun_rp - 1.25*gm
let su_rb = sun_rp*cos(flank)
let su_phi = (90 deg/z_sun - backlash/(4*sun_rp)*1 rad) + (tan(flank)*1 rad - flank)
let su_tmax = sqrt((sun_ra/su_rb)^2 - 1)
let su_cb = cos(0 deg)
let su_sb = sin(0 deg)
let su_cp = cos(su_phi)
let su_sp = sin(su_phi)
let su_t0 = su_tmax*0/6
let su_a0 = cos(su_t0*1 rad) + su_t0*sin(su_t0*1 rad)
let su_c0 = su_t0*cos(su_t0*1 rad) - sin(su_t0*1 rad)
let su_x0 = su_rb*(su_a0*su_cp - su_c0*su_sp)
let su_y0 = su_rb*(su_a0*su_sp + su_c0*su_cp)
let su_t1 = su_tmax*1/6
let su_a1 = cos(su_t1*1 rad) + su_t1*sin(su_t1*1 rad)
let su_c1 = su_t1*cos(su_t1*1 rad) - sin(su_t1*1 rad)
let su_x1 = su_rb*(su_a1*su_cp - su_c1*su_sp)
let su_y1 = su_rb*(su_a1*su_sp + su_c1*su_cp)
let su_t2 = su_tmax*2/6
let su_a2 = cos(su_t2*1 rad) + su_t2*sin(su_t2*1 rad)
let su_c2 = su_t2*cos(su_t2*1 rad) - sin(su_t2*1 rad)
let su_x2 = su_rb*(su_a2*su_cp - su_c2*su_sp)
let su_y2 = su_rb*(su_a2*su_sp + su_c2*su_cp)
let su_t3 = su_tmax*3/6
let su_a3 = cos(su_t3*1 rad) + su_t3*sin(su_t3*1 rad)
let su_c3 = su_t3*cos(su_t3*1 rad) - sin(su_t3*1 rad)
let su_x3 = su_rb*(su_a3*su_cp - su_c3*su_sp)
let su_y3 = su_rb*(su_a3*su_sp + su_c3*su_cp)
let su_t4 = su_tmax*4/6
let su_a4 = cos(su_t4*1 rad) + su_t4*sin(su_t4*1 rad)
let su_c4 = su_t4*cos(su_t4*1 rad) - sin(su_t4*1 rad)
let su_x4 = su_rb*(su_a4*su_cp - su_c4*su_sp)
let su_y4 = su_rb*(su_a4*su_sp + su_c4*su_cp)
let su_t5 = su_tmax*5/6
let su_a5 = cos(su_t5*1 rad) + su_t5*sin(su_t5*1 rad)
let su_c5 = su_t5*cos(su_t5*1 rad) - sin(su_t5*1 rad)
let su_x5 = su_rb*(su_a5*su_cp - su_c5*su_sp)
let su_y5 = su_rb*(su_a5*su_sp + su_c5*su_cp)
let su_t6 = su_tmax*6/6
let su_a6 = cos(su_t6*1 rad) + su_t6*sin(su_t6*1 rad)
let su_c6 = su_t6*cos(su_t6*1 rad) - sin(su_t6*1 rad)
let su_x6 = su_rb*(su_a6*su_cp - su_c6*su_sp)
let su_y6 = su_rb*(su_a6*su_sp + su_c6*su_cp)
cylinder SunCore radius sun_rf height b base (0,0,gear_z) new -> body Sun
sketch SunToothSketch on XY offset gear_z {
  polyline closed tooth = (0+(sun_rf-0.5 mm)*su_cp*su_cb-(-(sun_rf-0.5 mm)*su_sp)*su_sb,0+(sun_rf-0.5 mm)*su_cp*su_sb+(-(sun_rf-0.5 mm)*su_sp)*su_cb) (0+su_x0*su_cb-(-su_y0)*su_sb,0+su_x0*su_sb+(-su_y0)*su_cb) (0+su_x1*su_cb-(-su_y1)*su_sb,0+su_x1*su_sb+(-su_y1)*su_cb) (0+su_x2*su_cb-(-su_y2)*su_sb,0+su_x2*su_sb+(-su_y2)*su_cb) (0+su_x3*su_cb-(-su_y3)*su_sb,0+su_x3*su_sb+(-su_y3)*su_cb) (0+su_x4*su_cb-(-su_y4)*su_sb,0+su_x4*su_sb+(-su_y4)*su_cb) (0+su_x5*su_cb-(-su_y5)*su_sb,0+su_x5*su_sb+(-su_y5)*su_cb) (0+su_x6*su_cb-(-su_y6)*su_sb,0+su_x6*su_sb+(-su_y6)*su_cb) (0+su_x6*su_cb-su_y6*su_sb,0+su_x6*su_sb+su_y6*su_cb) (0+su_x5*su_cb-su_y5*su_sb,0+su_x5*su_sb+su_y5*su_cb) (0+su_x4*su_cb-su_y4*su_sb,0+su_x4*su_sb+su_y4*su_cb) (0+su_x3*su_cb-su_y3*su_sb,0+su_x3*su_sb+su_y3*su_cb) (0+su_x2*su_cb-su_y2*su_sb,0+su_x2*su_sb+su_y2*su_cb) (0+su_x1*su_cb-su_y1*su_sb,0+su_x1*su_sb+su_y1*su_cb) (0+su_x0*su_cb-su_y0*su_sb,0+su_x0*su_sb+su_y0*su_cb) (0+(sun_rf-0.5 mm)*su_cp*su_cb-(sun_rf-0.5 mm)*su_sp*su_sb,0+(sun_rf-0.5 mm)*su_cp*su_sb+(sun_rf-0.5 mm)*su_sp*su_cb)
  fix tooth.1 tooth.2 tooth.3 tooth.4 tooth.5 tooth.6 tooth.7 tooth.8 tooth.9 tooth.10 tooth.11 tooth.12 tooth.13 tooth.14 tooth.15 tooth.16
}
extrude SunTooth from SunToothSketch distance b join targets Sun
pattern circular SunTeeth features SunTooth axis Z count z_sun angle 360 deg
cylinder InputShaft radius in_r height (gear_z+12 mm+0.5 mm) base (0,0,-12 mm) join targets Sun
box InputFlat size 2 mm (2*in_r+2 mm) 9 mm corner (in_r-0.8 mm,-in_r-1 mm,-13 mm) cut targets Sun

// Planet gears: one at (orbit, 0), teeth half a pitch out of phase so a space faces the sun
let pl_rp = gm*z_planet/2
let pl_ra = pl_rp + gm
let pl_rf = pl_rp - 1.25*gm
let pl_rb = pl_rp*cos(flank)
let pl_phi = (90 deg/z_planet - backlash/(4*pl_rp)*1 rad) + (tan(flank)*1 rad - flank)
let pl_tmax = sqrt((pl_ra/pl_rb)^2 - 1)
let pl_cb = cos((180 deg/z_planet))
let pl_sb = sin((180 deg/z_planet))
let pl_cp = cos(pl_phi)
let pl_sp = sin(pl_phi)
let pl_t0 = pl_tmax*0/6
let pl_a0 = cos(pl_t0*1 rad) + pl_t0*sin(pl_t0*1 rad)
let pl_c0 = pl_t0*cos(pl_t0*1 rad) - sin(pl_t0*1 rad)
let pl_x0 = pl_rb*(pl_a0*pl_cp - pl_c0*pl_sp)
let pl_y0 = pl_rb*(pl_a0*pl_sp + pl_c0*pl_cp)
let pl_t1 = pl_tmax*1/6
let pl_a1 = cos(pl_t1*1 rad) + pl_t1*sin(pl_t1*1 rad)
let pl_c1 = pl_t1*cos(pl_t1*1 rad) - sin(pl_t1*1 rad)
let pl_x1 = pl_rb*(pl_a1*pl_cp - pl_c1*pl_sp)
let pl_y1 = pl_rb*(pl_a1*pl_sp + pl_c1*pl_cp)
let pl_t2 = pl_tmax*2/6
let pl_a2 = cos(pl_t2*1 rad) + pl_t2*sin(pl_t2*1 rad)
let pl_c2 = pl_t2*cos(pl_t2*1 rad) - sin(pl_t2*1 rad)
let pl_x2 = pl_rb*(pl_a2*pl_cp - pl_c2*pl_sp)
let pl_y2 = pl_rb*(pl_a2*pl_sp + pl_c2*pl_cp)
let pl_t3 = pl_tmax*3/6
let pl_a3 = cos(pl_t3*1 rad) + pl_t3*sin(pl_t3*1 rad)
let pl_c3 = pl_t3*cos(pl_t3*1 rad) - sin(pl_t3*1 rad)
let pl_x3 = pl_rb*(pl_a3*pl_cp - pl_c3*pl_sp)
let pl_y3 = pl_rb*(pl_a3*pl_sp + pl_c3*pl_cp)
let pl_t4 = pl_tmax*4/6
let pl_a4 = cos(pl_t4*1 rad) + pl_t4*sin(pl_t4*1 rad)
let pl_c4 = pl_t4*cos(pl_t4*1 rad) - sin(pl_t4*1 rad)
let pl_x4 = pl_rb*(pl_a4*pl_cp - pl_c4*pl_sp)
let pl_y4 = pl_rb*(pl_a4*pl_sp + pl_c4*pl_cp)
let pl_t5 = pl_tmax*5/6
let pl_a5 = cos(pl_t5*1 rad) + pl_t5*sin(pl_t5*1 rad)
let pl_c5 = pl_t5*cos(pl_t5*1 rad) - sin(pl_t5*1 rad)
let pl_x5 = pl_rb*(pl_a5*pl_cp - pl_c5*pl_sp)
let pl_y5 = pl_rb*(pl_a5*pl_sp + pl_c5*pl_cp)
let pl_t6 = pl_tmax*6/6
let pl_a6 = cos(pl_t6*1 rad) + pl_t6*sin(pl_t6*1 rad)
let pl_c6 = pl_t6*cos(pl_t6*1 rad) - sin(pl_t6*1 rad)
let pl_x6 = pl_rb*(pl_a6*pl_cp - pl_c6*pl_sp)
let pl_y6 = pl_rb*(pl_a6*pl_sp + pl_c6*pl_cp)
cylinder PlanetCore radius pl_rf height b base (orbit,0,gear_z) new -> body Planet
sketch PlanetToothSketch on XY offset gear_z {
  polyline closed tooth = (orbit+(pl_rf-0.5 mm)*pl_cp*pl_cb-(-(pl_rf-0.5 mm)*pl_sp)*pl_sb,0+(pl_rf-0.5 mm)*pl_cp*pl_sb+(-(pl_rf-0.5 mm)*pl_sp)*pl_cb) (orbit+pl_x0*pl_cb-(-pl_y0)*pl_sb,0+pl_x0*pl_sb+(-pl_y0)*pl_cb) (orbit+pl_x1*pl_cb-(-pl_y1)*pl_sb,0+pl_x1*pl_sb+(-pl_y1)*pl_cb) (orbit+pl_x2*pl_cb-(-pl_y2)*pl_sb,0+pl_x2*pl_sb+(-pl_y2)*pl_cb) (orbit+pl_x3*pl_cb-(-pl_y3)*pl_sb,0+pl_x3*pl_sb+(-pl_y3)*pl_cb) (orbit+pl_x4*pl_cb-(-pl_y4)*pl_sb,0+pl_x4*pl_sb+(-pl_y4)*pl_cb) (orbit+pl_x5*pl_cb-(-pl_y5)*pl_sb,0+pl_x5*pl_sb+(-pl_y5)*pl_cb) (orbit+pl_x6*pl_cb-(-pl_y6)*pl_sb,0+pl_x6*pl_sb+(-pl_y6)*pl_cb) (orbit+pl_x6*pl_cb-pl_y6*pl_sb,0+pl_x6*pl_sb+pl_y6*pl_cb) (orbit+pl_x5*pl_cb-pl_y5*pl_sb,0+pl_x5*pl_sb+pl_y5*pl_cb) (orbit+pl_x4*pl_cb-pl_y4*pl_sb,0+pl_x4*pl_sb+pl_y4*pl_cb) (orbit+pl_x3*pl_cb-pl_y3*pl_sb,0+pl_x3*pl_sb+pl_y3*pl_cb) (orbit+pl_x2*pl_cb-pl_y2*pl_sb,0+pl_x2*pl_sb+pl_y2*pl_cb) (orbit+pl_x1*pl_cb-pl_y1*pl_sb,0+pl_x1*pl_sb+pl_y1*pl_cb) (orbit+pl_x0*pl_cb-pl_y0*pl_sb,0+pl_x0*pl_sb+pl_y0*pl_cb) (orbit+(pl_rf-0.5 mm)*pl_cp*pl_cb-(pl_rf-0.5 mm)*pl_sp*pl_sb,0+(pl_rf-0.5 mm)*pl_cp*pl_sb+(pl_rf-0.5 mm)*pl_sp*pl_cb)
  fix tooth.1 tooth.2 tooth.3 tooth.4 tooth.5 tooth.6 tooth.7 tooth.8 tooth.9 tooth.10 tooth.11 tooth.12 tooth.13 tooth.14 tooth.15 tooth.16
}
extrude PlanetTooth from PlanetToothSketch distance b join targets Planet
pattern circular PlanetTeeth features PlanetTooth origin (orbit,0,0) dir (0,0,1) count z_planet angle 360 deg
cylinder PlanetBore radius planet_bore_r height (b+2 mm) base (orbit,0,gear_z-1 mm) cut targets Planet
move Planet2 bodies Planet axis (0,0,1) angle 120 deg copy
move Planet3 bodies Planet axis (0,0,1) angle 240 deg copy

// Housing: the ring gear's tooth spaces are external-involute shapes cut into the bore
let ring_rp = gm*z_ring/2
let ring_tip = ring_rp - gm
let ring_root = ring_rp + 1.25*gm
let housing_r = ring_root + wall
cylinder HousingBlank radius housing_r height wall_h new -> body Housing
cylinder HousingBore radius ring_tip height (wall_h-floor_t+1 mm) base (0,0,floor_t) cut targets Housing
cylinder InputBore radius (in_r+shaft_clear) height (floor_t+2 mm) base (0,0,-1 mm) cut targets Housing
let rg_rb = ring_rp*cos(flank)
let rg_phi = (90 deg/z_ring + backlash/(4*ring_rp)*1 rad) + (tan(flank)*1 rad - flank)
let rg_tmax = sqrt((ring_root/rg_rb)^2 - 1)
let rg_cb = cos((180 deg/z_ring))
let rg_sb = sin((180 deg/z_ring))
let rg_cp = cos(rg_phi)
let rg_sp = sin(rg_phi)
let rg_t0 = rg_tmax*0/6
let rg_a0 = cos(rg_t0*1 rad) + rg_t0*sin(rg_t0*1 rad)
let rg_c0 = rg_t0*cos(rg_t0*1 rad) - sin(rg_t0*1 rad)
let rg_x0 = rg_rb*(rg_a0*rg_cp - rg_c0*rg_sp)
let rg_y0 = rg_rb*(rg_a0*rg_sp + rg_c0*rg_cp)
let rg_t1 = rg_tmax*1/6
let rg_a1 = cos(rg_t1*1 rad) + rg_t1*sin(rg_t1*1 rad)
let rg_c1 = rg_t1*cos(rg_t1*1 rad) - sin(rg_t1*1 rad)
let rg_x1 = rg_rb*(rg_a1*rg_cp - rg_c1*rg_sp)
let rg_y1 = rg_rb*(rg_a1*rg_sp + rg_c1*rg_cp)
let rg_t2 = rg_tmax*2/6
let rg_a2 = cos(rg_t2*1 rad) + rg_t2*sin(rg_t2*1 rad)
let rg_c2 = rg_t2*cos(rg_t2*1 rad) - sin(rg_t2*1 rad)
let rg_x2 = rg_rb*(rg_a2*rg_cp - rg_c2*rg_sp)
let rg_y2 = rg_rb*(rg_a2*rg_sp + rg_c2*rg_cp)
let rg_t3 = rg_tmax*3/6
let rg_a3 = cos(rg_t3*1 rad) + rg_t3*sin(rg_t3*1 rad)
let rg_c3 = rg_t3*cos(rg_t3*1 rad) - sin(rg_t3*1 rad)
let rg_x3 = rg_rb*(rg_a3*rg_cp - rg_c3*rg_sp)
let rg_y3 = rg_rb*(rg_a3*rg_sp + rg_c3*rg_cp)
let rg_t4 = rg_tmax*4/6
let rg_a4 = cos(rg_t4*1 rad) + rg_t4*sin(rg_t4*1 rad)
let rg_c4 = rg_t4*cos(rg_t4*1 rad) - sin(rg_t4*1 rad)
let rg_x4 = rg_rb*(rg_a4*rg_cp - rg_c4*rg_sp)
let rg_y4 = rg_rb*(rg_a4*rg_sp + rg_c4*rg_cp)
let rg_t5 = rg_tmax*5/6
let rg_a5 = cos(rg_t5*1 rad) + rg_t5*sin(rg_t5*1 rad)
let rg_c5 = rg_t5*cos(rg_t5*1 rad) - sin(rg_t5*1 rad)
let rg_x5 = rg_rb*(rg_a5*rg_cp - rg_c5*rg_sp)
let rg_y5 = rg_rb*(rg_a5*rg_sp + rg_c5*rg_cp)
let rg_t6 = rg_tmax*6/6
let rg_a6 = cos(rg_t6*1 rad) + rg_t6*sin(rg_t6*1 rad)
let rg_c6 = rg_t6*cos(rg_t6*1 rad) - sin(rg_t6*1 rad)
let rg_x6 = rg_rb*(rg_a6*rg_cp - rg_c6*rg_sp)
let rg_y6 = rg_rb*(rg_a6*rg_sp + rg_c6*rg_cp)
sketch RingSpaceSketch on XY offset floor_t {
  polyline closed tooth = (0+(ring_tip-1 mm)*rg_cp*rg_cb-(-(ring_tip-1 mm)*rg_sp)*rg_sb,0+(ring_tip-1 mm)*rg_cp*rg_sb+(-(ring_tip-1 mm)*rg_sp)*rg_cb) (0+rg_x0*rg_cb-(-rg_y0)*rg_sb,0+rg_x0*rg_sb+(-rg_y0)*rg_cb) (0+rg_x1*rg_cb-(-rg_y1)*rg_sb,0+rg_x1*rg_sb+(-rg_y1)*rg_cb) (0+rg_x2*rg_cb-(-rg_y2)*rg_sb,0+rg_x2*rg_sb+(-rg_y2)*rg_cb) (0+rg_x3*rg_cb-(-rg_y3)*rg_sb,0+rg_x3*rg_sb+(-rg_y3)*rg_cb) (0+rg_x4*rg_cb-(-rg_y4)*rg_sb,0+rg_x4*rg_sb+(-rg_y4)*rg_cb) (0+rg_x5*rg_cb-(-rg_y5)*rg_sb,0+rg_x5*rg_sb+(-rg_y5)*rg_cb) (0+rg_x6*rg_cb-(-rg_y6)*rg_sb,0+rg_x6*rg_sb+(-rg_y6)*rg_cb) (0+rg_x6*rg_cb-rg_y6*rg_sb,0+rg_x6*rg_sb+rg_y6*rg_cb) (0+rg_x5*rg_cb-rg_y5*rg_sb,0+rg_x5*rg_sb+rg_y5*rg_cb) (0+rg_x4*rg_cb-rg_y4*rg_sb,0+rg_x4*rg_sb+rg_y4*rg_cb) (0+rg_x3*rg_cb-rg_y3*rg_sb,0+rg_x3*rg_sb+rg_y3*rg_cb) (0+rg_x2*rg_cb-rg_y2*rg_sb,0+rg_x2*rg_sb+rg_y2*rg_cb) (0+rg_x1*rg_cb-rg_y1*rg_sb,0+rg_x1*rg_sb+rg_y1*rg_cb) (0+rg_x0*rg_cb-rg_y0*rg_sb,0+rg_x0*rg_sb+rg_y0*rg_cb) (0+(ring_tip-1 mm)*rg_cp*rg_cb-(ring_tip-1 mm)*rg_sp*rg_sb,0+(ring_tip-1 mm)*rg_cp*rg_sb+(ring_tip-1 mm)*rg_sp*rg_cb)
  fix tooth.1 tooth.2 tooth.3 tooth.4 tooth.5 tooth.6 tooth.7 tooth.8 tooth.9 tooth.10 tooth.11 tooth.12 tooth.13 tooth.14 tooth.15 tooth.16
}
extrude RingSpace from RingSpaceSketch distance (wall_h-floor_t+1 mm) cut targets Housing
pattern circular RingSpaces features RingSpace axis Z count z_ring angle 360 deg

// Housing ears with M3 pilot holes (screws come down through the lid)
param ear_r = 6 mm
let ear_x = housing_r + 3 mm
cylinder Ear radius ear_r height wall_h base (ear_x,0,0) join targets Housing
box EarWeb size (ear_x-housing_r+2 mm) (2*ear_r) wall_h corner (housing_r-2 mm,-ear_r,0) join targets Housing
cylinder EarPilot radius (m3_pilot/2) height 17 mm base (ear_x,0,wall_h-16 mm) cut targets Housing
pattern circular Ears features Ear, EarWeb, EarPilot axis Z count 4 angle 360 deg

// Lid: plate with matching ears, a centring spigot into the ring bore and the output bore
param spigot_h = 1.5 mm
let carrier_r = orbit + planet_bore_r + 5 mm
cylinder LidPlate radius housing_r height lid_t base (0,0,wall_h) new -> body Lid
cylinder LidEarE radius ear_r height lid_t base (ear_x,0,wall_h) join targets Lid
box LidEarWebE size (ear_x-housing_r+2 mm) (2*ear_r) lid_t corner (housing_r-2 mm,-ear_r,wall_h) join targets Lid
cylinder LidHoleE radius (m3_clear/2) height (lid_t+2 mm) base (ear_x,0,wall_h-1 mm) cut targets Lid
cylinder LidEarN radius ear_r height lid_t base (0,ear_x,wall_h) join targets Lid
box LidEarWebN size (2*ear_r) (ear_x-housing_r+2 mm) lid_t corner (-ear_r,housing_r-2 mm,wall_h) join targets Lid
cylinder LidHoleN radius (m3_clear/2) height (lid_t+2 mm) base (0,ear_x,wall_h-1 mm) cut targets Lid
cylinder LidEarW radius ear_r height lid_t base ((-ear_x),0,wall_h) join targets Lid
box LidEarWebW size (ear_x-housing_r+2 mm) (2*ear_r) lid_t corner (-ear_x,-ear_r,wall_h) join targets Lid
cylinder LidHoleW radius (m3_clear/2) height (lid_t+2 mm) base ((-ear_x),0,wall_h-1 mm) cut targets Lid
cylinder LidEarS radius ear_r height lid_t base (0,(-ear_x),wall_h) join targets Lid
box LidEarWebS size (2*ear_r) (ear_x-housing_r+2 mm) lid_t corner (-ear_r,-ear_x,wall_h) join targets Lid
cylinder LidHoleS radius (m3_clear/2) height (lid_t+2 mm) base (0,(-ear_x),wall_h-1 mm) cut targets Lid
cylinder Spigot radius (ring_tip-0.3 mm) height (spigot_h+0.5 mm) base (0,0,wall_h-spigot_h) join targets Lid
cylinder SpigotRelief radius (carrier_r+1 mm) height spigot_h base (0,0,wall_h-spigot_h) cut targets Lid
cylinder OutputBore radius (out_r+shaft_clear) height (lid_t+spigot_h+2 mm) base (0,0,wall_h-spigot_h-1 mm) cut targets Lid

// Carrier base: plate with three pins; the sun shaft passes through its centre
let pin_r = planet_bore_r - pin_clear
cylinder BasePlate radius carrier_r height base_t base (0,0,base_z) new -> body CarrierBase
cylinder BaseHole radius (in_r+1 mm) height (base_t+2 mm) base (0,0,base_z-1 mm) cut targets CarrierBase
cylinder Pin radius pin_r height (b+2*gap) base (orbit,0,base_z+base_t) join targets CarrierBase
cylinder PinPilot radius (m3_pilot/2) height (b+2*gap+2.1 mm) base (orbit,0,base_z+base_t-2 mm) cut targets CarrierBase
pattern circular Pins features Pin, PinPilot axis Z count 3 angle 360 deg

// Carrier top: plate screwed onto the pins, carrying the output shaft
param out_len = 20 mm
cylinder TopPlate radius carrier_r height top_t base (0,0,top_z) new -> body CarrierTop
cylinder TopHole radius (m3_clear/2) height (top_t+2 mm) base (orbit,0,top_z-1 mm) cut targets CarrierTop
cylinder TopCounterbore radius (m3_head/2) height (m3_head_h+1 mm) base (orbit,0,top_z+top_t-m3_head_h) cut targets CarrierTop
pattern circular TopHoles features TopHole, TopCounterbore axis Z count 3 angle 360 deg
cylinder OutputShaft radius out_r height (out_len+0.5 mm) base (0,0,top_z+top_t-0.5 mm) join targets CarrierTop
box OutputFlat size 2 mm (2*out_r+2 mm) 10 mm corner (out_r-1 mm,-out_r-1 mm,top_z+top_t+out_len-9 mm) cut targets CarrierTop

// Cranks: one on each D-flat shaft, arms in line when assembled, so four turns of the input
// crank show as one turn of the output crank. Each prints flat with its knob pointing up.
param crank_len = 30 mm
param crank_t = 4 mm
param hub_r = 9 mm
param hub_h = 6 mm
param boss_r = 6 mm
param knob_r = 4 mm
param knob_h = 18 mm
param crank_fit = 0.1 mm

// Input crank under the housing: hub flush with the shaft end, arm at the hub's top, knob hanging down
let ic_z = -12 mm
let ia_z = ic_z + hub_h - crank_t
cylinder InHub radius hub_r height hub_h base (0,0,ic_z) new -> body InputCrank
box InArm size (crank_len-hub_r+1 mm) (2*boss_r-2 mm) crank_t corner (hub_r-1 mm,-boss_r+1 mm,ia_z) join targets InputCrank
cylinder InBoss radius boss_r height crank_t base (crank_len,0,ia_z) join targets InputCrank
cylinder InKnob radius knob_r height knob_h base (crank_len,0,ia_z-knob_h) join targets InputCrank
cylinder InBore radius (in_r+crank_fit) height (hub_h+2 mm) base (0,0,ic_z-1 mm) cut targets InputCrank
box InKey size 2 mm (2*in_r+2 mm) hub_h corner (in_r-0.8 mm+crank_fit,-in_r-1 mm,ic_z) join targets InputCrank

// Output crank above the lid: hub flush with the shaft end, arm at the hub's bottom, knob pointing up
let oc_z = top_z + top_t + out_len - hub_h
cylinder OutHub radius hub_r height hub_h base (0,0,oc_z) new -> body OutputCrank
box OutArm size (crank_len-hub_r+1 mm) (2*boss_r-2 mm) crank_t corner (hub_r-1 mm,-boss_r+1 mm,oc_z) join targets OutputCrank
cylinder OutBoss radius boss_r height crank_t base (crank_len,0,oc_z) join targets OutputCrank
cylinder OutKnob radius knob_r height knob_h base (crank_len,0,oc_z+crank_t) join targets OutputCrank
cylinder OutBore radius (out_r+crank_fit) height (hub_h+2 mm) base (0,0,oc_z-1 mm) cut targets OutputCrank
box OutKey size 2 mm (2*out_r+2 mm) hub_h corner (out_r-1 mm+crank_fit,-out_r-1 mm,oc_z) join targets OutputCrank
