// 19. Mini open-frame planetary gearbox: nothing over the gears, one M3 screw holds it together.
//
// A demonstration piece: the frame is as thin as printing allows, not sized for load. Housing
// 40.9 mm across and 11 mm tall. Module 1 with 12/12/36 teeth gives 4:1; the pressure angle is
// 25 deg because 12-tooth gears at 20 deg interfere at the root.
//
// Parts (each its own body; every one prints without supports):
//   Housing     spoked floor and the ring gear in a 1.2 mm band; stands on its floor
//   Sun         sun gear with a D-flat input shaft pointing up (prints gear-down)
//   Planet x3   planets with three marker holes, running on the carrier pins
//   CarrierBase spoked plate with three pins; the sun stands on its hub
//   CarrierTop  spoked plate on the pin tips, the sun shaft passing through its centre; the
//               tube around pin 0 is the output, turning once for four turns of the input
//   InputWheel  spoked thumbwheel on the sun shaft (prints cap-down)
//
// Assembly: carrier base into the housing, planets onto the pins, sun down into the middle,
// carrier top onto the pin tips, thumbwheel onto the shaft, then the one screw: M3x8 down
// through the thumbwheel's cap into the shaft end. That captures everything on the axis: the
// carrier top is held between the sun gear and the thumbwheel, the carrier base sits under the
// sun on the floor. Spur gears make no axial force, so the stack rests in the housing under
// its own weight; keep it upright.
// The cut-outs are computed by the generator for the default parameters below; regenerate the
// file after changing module, tooth counts, wall or stack heights.
// Planet tooth phase assumes even tooth counts and (z_sun + z_ring) divisible by 3.
design MiniOpenGearbox units mm

// Gearing
param gm = 1 mm
param z_sun = 12
param z_planet = 12
param b = 6 mm
param flank = 25 deg
param backlash = 0.3 mm
let z_ring = z_sun + 2*z_planet
let orbit = gm*(z_sun+z_planet)/2

// Stack and fits
param gap = 0.5 mm
param floor_t = 1.2 mm
param base_t = 1.8 mm
param top_t = 2 mm
param wall = 1.2 mm
param pin_clear = 0.2 mm
param pin_fit = 0.15 mm
param shaft_clear = 0.25 mm
param in_r = 3 mm
param planet_bore_r = 2 mm
param m3_pilot = 2.5 mm
param m3_clear = 3.4 mm
let base_z = floor_t + gap
let gear_z = base_z + base_t + gap
let top_z = gear_z + b + gap
let ring_h = gear_z + b + gap

// Thumbwheel: hub on the shaft, cap on top for the screw, rim and spokes level with the cap
param wheel_r = 9 mm
param wheel_t = 2 mm
param wheel_hub_r = 4.5 mm
param wheel_h = 6 mm
param cap_t = 1.5 mm
param wheel_fit = 0.1 mm
let wheel_z = top_z + top_t + gap
let shaft_top = wheel_z + wheel_h - cap_t

// Sun gear, with its input shaft up through the carrier top into the thumbwheel
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
cylinder InputShaft radius in_r height (shaft_top-gear_z-b+0.5 mm) base (0,0,gear_z+b-0.5 mm) join targets Sun
box InputFlat size 2 mm (2*in_r+2 mm) (shaft_top-wheel_z+1.5 mm) corner (in_r-0.8 mm,-in_r-1 mm,wheel_z-0.5 mm) cut targets Sun
cylinder ShaftPilot radius (m3_pilot/2) height 8 mm base (0,0,shaft_top-7 mm) cut targets Sun

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
cylinder PlanetMark0 radius 0.6 mm height (b+2 mm) base (orbit+3.4 mm*cos(0 deg),3.4 mm*sin(0 deg),gear_z-1 mm) cut targets Planet
cylinder PlanetMark1 radius 0.6 mm height (b+2 mm) base (orbit+3.4 mm*cos(120 deg),3.4 mm*sin(120 deg),gear_z-1 mm) cut targets Planet
cylinder PlanetMark2 radius 0.6 mm height (b+2 mm) base (orbit+3.4 mm*cos(240 deg),3.4 mm*sin(240 deg),gear_z-1 mm) cut targets Planet
move Planet2 bodies Planet axis (0,0,1) angle 120 deg copy
move Planet3 bodies Planet axis (0,0,1) angle 240 deg copy

// Housing: spoked floor and the ring gear band; the tooth spaces are external-involute shapes
let ring_rp = gm*z_ring/2
let ring_tip = ring_rp - gm
let ring_root = ring_rp + 1.25*gm
let housing_r = ring_root + wall
let carrier_r = ring_tip - 0.6 mm
cylinder HousingBlank radius housing_r height ring_h new -> body Housing
cylinder HousingBore radius ring_tip height (ring_h-floor_t+1 mm) base (0,0,floor_t) cut targets Housing
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
extrude RingSpace from RingSpaceSketch distance (ring_h-floor_t+1 mm) cut targets Housing
pattern circular RingSpaces features RingSpace axis Z count z_ring angle 360 deg

// Carrier base: plate with three pins; the sun stands on its hub
let pin_r = planet_bore_r - pin_clear
cylinder BasePlate radius carrier_r height base_t base (0,0,base_z) new -> body CarrierBase
cylinder Pin0 radius pin_r height (b+2*gap+top_t+post_h-0.3 mm) base (orbit*cos(0 deg),orbit*sin(0 deg),base_z+base_t) join targets CarrierBase
cylinder Pin1 radius pin_r height (b+2*gap+top_t-0.3 mm) base (orbit*cos(120 deg),orbit*sin(120 deg),base_z+base_t) join targets CarrierBase
cylinder Pin2 radius pin_r height (b+2*gap+top_t-0.3 mm) base (orbit*cos(240 deg),orbit*sin(240 deg),base_z+base_t) join targets CarrierBase

// Carrier top: plate on the pin tips; the sun shaft passes through; the output post is a tube
// around pin 0 (open at the top, so it prints without a bridge)
param post_r = 3 mm
param post_h = 4 mm
cylinder TopPlate radius carrier_r height top_t base (0,0,top_z) new -> body CarrierTop
cylinder TopShaftHole radius (in_r+shaft_clear) height (top_t+2 mm) base (0,0,top_z-1 mm) cut targets CarrierTop
cylinder OutputPost radius post_r height post_h base (orbit,0,top_z+top_t) join targets CarrierTop
cylinder TopPinHole0 radius (pin_r+pin_fit) height (top_t+post_h+2 mm) base (orbit*cos(0 deg),orbit*sin(0 deg),top_z-1 mm) cut targets CarrierTop
cylinder TopPinHole1 radius (pin_r+pin_fit) height (top_t+2 mm) base (orbit*cos(120 deg),orbit*sin(120 deg),top_z-1 mm) cut targets CarrierTop
cylinder TopPinHole2 radius (pin_r+pin_fit) height (top_t+2 mm) base (orbit*cos(240 deg),orbit*sin(240 deg),top_z-1 mm) cut targets CarrierTop

// Thumbwheel: modelled in place (cap up); prints cap-down
cylinder WheelHub radius wheel_hub_r height wheel_h base (0,0,wheel_z) new -> body InputWheel
cylinder WheelDisc radius wheel_r height wheel_t base (0,0,wheel_z+wheel_h-wheel_t) join targets InputWheel
cylinder WheelBore radius (in_r+wheel_fit) height (wheel_h-cap_t+1 mm) base (0,0,wheel_z-1 mm) cut targets InputWheel
box WheelKey size (1 mm-wheel_fit) 4.6 mm (wheel_h-cap_t) corner (in_r-0.8 mm+wheel_fit,-2.3 mm,wheel_z) join targets InputWheel
cylinder WheelScrewHole radius (m3_clear/2) height (wheel_h+2 mm) base (0,0,wheel_z-1 mm) cut targets InputWheel
cylinder WheelGrip0 radius 0.8 mm height (wheel_t+2 mm) base (wheel_r*cos(15 deg),wheel_r*sin(15 deg),wheel_z+wheel_h-wheel_t-1 mm) cut targets InputWheel
cylinder WheelGrip1 radius 0.8 mm height (wheel_t+2 mm) base (wheel_r*cos(45 deg),wheel_r*sin(45 deg),wheel_z+wheel_h-wheel_t-1 mm) cut targets InputWheel
cylinder WheelGrip2 radius 0.8 mm height (wheel_t+2 mm) base (wheel_r*cos(75 deg),wheel_r*sin(75 deg),wheel_z+wheel_h-wheel_t-1 mm) cut targets InputWheel
cylinder WheelGrip3 radius 0.8 mm height (wheel_t+2 mm) base (wheel_r*cos(105 deg),wheel_r*sin(105 deg),wheel_z+wheel_h-wheel_t-1 mm) cut targets InputWheel
cylinder WheelGrip4 radius 0.8 mm height (wheel_t+2 mm) base (wheel_r*cos(135 deg),wheel_r*sin(135 deg),wheel_z+wheel_h-wheel_t-1 mm) cut targets InputWheel
cylinder WheelGrip5 radius 0.8 mm height (wheel_t+2 mm) base (wheel_r*cos(165 deg),wheel_r*sin(165 deg),wheel_z+wheel_h-wheel_t-1 mm) cut targets InputWheel
cylinder WheelGrip6 radius 0.8 mm height (wheel_t+2 mm) base (wheel_r*cos(195 deg),wheel_r*sin(195 deg),wheel_z+wheel_h-wheel_t-1 mm) cut targets InputWheel
cylinder WheelGrip7 radius 0.8 mm height (wheel_t+2 mm) base (wheel_r*cos(225 deg),wheel_r*sin(225 deg),wheel_z+wheel_h-wheel_t-1 mm) cut targets InputWheel
cylinder WheelGrip8 radius 0.8 mm height (wheel_t+2 mm) base (wheel_r*cos(255 deg),wheel_r*sin(255 deg),wheel_z+wheel_h-wheel_t-1 mm) cut targets InputWheel
cylinder WheelGrip9 radius 0.8 mm height (wheel_t+2 mm) base (wheel_r*cos(285 deg),wheel_r*sin(285 deg),wheel_z+wheel_h-wheel_t-1 mm) cut targets InputWheel
cylinder WheelGrip10 radius 0.8 mm height (wheel_t+2 mm) base (wheel_r*cos(315 deg),wheel_r*sin(315 deg),wheel_z+wheel_h-wheel_t-1 mm) cut targets InputWheel
cylinder WheelGrip11 radius 0.8 mm height (wheel_t+2 mm) base (wheel_r*cos(345 deg),wheel_r*sin(345 deg),wheel_z+wheel_h-wheel_t-1 mm) cut targets InputWheel

// Skeleton: spoked floor, carriers and thumbwheel
sketch FloorWindowSk on XY offset (-1) {
  polyline closed edge = (16.1691,1) (15.7253,3.8929) (14.7683,6.6587) (13.3292,9.2072) (11.4551,11.4551) (9.2072,13.3292) (6.6587,14.7683) (3.8929,15.7253) (1,16.1691) (1,4.899) (3.5355,3.5355) (4.899,1)
  fillet edge.12.end radius 1
  fillet edge.8.end radius 1
  fillet edge.9.end radius 0.5
  fillet edge.11.end radius 0.5
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11 edge.12
}
extrude FloorWindow from FloorWindowSk distance 3.2 cut targets Housing
pattern circular FloorWindows features FloorWindow axis Z count 4 angle 360 deg
sketch CarrierBaseWindowSk on XY offset 0.7 {
  polyline closed edge = (14.8805,3.1) (13.9046,6.1402) (12.3151,8.9094) (10.1822,11.2855) (7.6,13.1636) (4.6824,14.4608) (1.5582,15.1199) ((-1.6348),15.1118) ((-4.7556),14.4369) (0.7232,4.9474) (2.5,4.3301) (3.923,3.1)
  fillet edge.12.end radius 1
  fillet edge.8.end radius 1
  fillet edge.9.end radius 0.5
  fillet edge.11.end radius 0.5
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11 edge.12
}
extrude CarrierBaseWindow from CarrierBaseWindowSk distance 3.8 cut targets CarrierBase
pattern circular CarrierBaseWindows features CarrierBaseWindow axis Z count 3 angle 360 deg
sketch CarrierTopWindowSk on XY offset 9.5 {
  polyline closed edge = (14.8805,3.1) (13.9046,6.1402) (12.3151,8.9094) (10.1822,11.2855) (7.6,13.1636) (4.6824,14.4608) (1.5582,15.1199) ((-1.6348),15.1118) ((-4.7556),14.4369) (0.7232,4.9474) (2.5,4.3301) (3.923,3.1)
  fillet edge.12.end radius 1
  fillet edge.8.end radius 1
  fillet edge.9.end radius 0.5
  fillet edge.11.end radius 0.5
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11 edge.12
}
extrude CarrierTopWindow from CarrierTopWindowSk distance 4 cut targets CarrierTop
pattern circular CarrierTopWindows features CarrierTopWindow axis Z count 3 angle 360 deg
sketch WheelWindowSk on XY offset 16 {
  polyline closed edge = (2.8,6.8498) (1.18,7.3053) ((-0.5012),7.383) ((-2.1563),7.0789) ((-3.7),6.4086) ((-5.0523),5.4069) ((-6.1433),4.1255) ((-6.9166),2.6308) ((-7.3321),1) ((-5.1029),1) ((-2.6),4.5033) (1.6854,4.9193)
  fillet edge.12.end radius 0.8
  fillet edge.8.end radius 0.8
  fillet edge.9.end radius 0.4
  fillet edge.11.end radius 0.4
  fix edge.1 edge.2 edge.3 edge.4 edge.5 edge.6 edge.7 edge.8 edge.9 edge.10 edge.11 edge.12
}
extrude WheelWindow from WheelWindowSk distance 4 cut targets InputWheel
pattern circular WheelWindows features WheelWindow axis Z count 3 angle 360 deg
