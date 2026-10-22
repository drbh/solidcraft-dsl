// 13. Planetary gearbox: sun, three planets, internal ring with flange, carrier with output shaft.
// Teeth are straight-flank trapezoids (20 deg flanks) patterned around each gear; every gear is
// its own body. Tooth counts satisfy ring = sun + 2*planet, so the planets sit on a common orbit.
design PlanetaryGearbox units mm
param gm = 2 mm
param z_sun = 15
param z_planet = 12
param b = 10 mm
param flank = 20 deg
let z_ring = z_sun + 2*z_planet
let orbit = gm*(z_sun+z_planet)/2
param backlash = 0.12
let half_pitch_t = (1-backlash)*pi*gm/4
let w_root = half_pitch_t + 1.25*gm*tan(flank)
let w_tip = half_pitch_t - gm*tan(flank)

// Sun gear
let sun_rp = gm*z_sun/2
let sun_rf = sun_rp - 1.25*gm
let sun_ra = sun_rp + gm
cylinder SunCore radius sun_rf height b new -> body Sun
sketch SunToothSketch on XY {
  polyline closed tooth = (sun_rf-0.5 mm,-w_root) (sun_ra,-w_tip) (sun_ra,w_tip) (sun_rf-0.5 mm,w_root)
  fix tooth.1 tooth.2 tooth.3 tooth.4
}
extrude SunTooth from SunToothSketch distance b join targets Sun
pattern circular SunTeeth features SunTooth axis Z count z_sun angle 360 deg

// Planet gears: one built at (orbit, 0), teeth offset half a pitch so a gap faces the sun
param planet_bore_r = 3.5 mm
let pl_rp = gm*z_planet/2
let pl_rf = pl_rp - 1.25*gm
let pl_ra = pl_rp + gm
let ph = 180 deg / z_planet
let c = cos(ph)
let s = sin(ph)
let r0 = pl_rf - 0.5 mm
cylinder PlanetCore radius pl_rf height b base (orbit,0,0) new -> body Planet
sketch PlanetToothSketch on XY {
  polyline closed tooth = (orbit+r0*c+w_root*s,r0*s-w_root*c) (orbit+pl_ra*c+w_tip*s,pl_ra*s-w_tip*c) (orbit+pl_ra*c-w_tip*s,pl_ra*s+w_tip*c) (orbit+r0*c-w_root*s,r0*s+w_root*c)
  fix tooth.1 tooth.2 tooth.3 tooth.4
}
extrude PlanetTooth from PlanetToothSketch distance b join targets Planet
pattern circular PlanetTeeth features PlanetTooth origin (orbit,0,0) dir (0,0,1) count z_planet angle 360 deg
cylinder PlanetBore radius planet_bore_r height (b+2 mm) base (orbit,0,-1 mm) cut targets Planet
move Planet2 bodies Planet axis (0,0,1) angle 120 deg copy
move Planet3 bodies Planet axis (0,0,1) angle 240 deg copy

// Ring gear: internal teeth, wider at their (outer) root, tips shortened for clearance
param ring_wall = 6 mm
let ring_rp = gm*z_ring/2
let ring_rf = ring_rp + 1.25*gm
let ring_ra = ring_rp - 0.8*gm
let ring_or = ring_rf + ring_wall
cylinder RingBlank radius ring_or height b new -> body Ring
cylinder RingBore radius ring_rf height (b+2 mm) base (0,0,-1 mm) cut targets Ring
sketch RingToothSketch on XY {
  polyline closed tooth = (ring_rf+0.5 mm,-w_root) (ring_ra,-w_tip) (ring_ra,w_tip) (ring_rf+0.5 mm,w_root)
  fix tooth.1 tooth.2 tooth.3 tooth.4
}
extrude RingTooth from RingToothSketch distance b join targets Ring
pattern circular RingTeeth features RingTooth axis Z count z_ring angle 360 deg

// Mounting lugs on the ring, patterned four times
param lug_r = 7 mm
param lug_hole_d = 5.5 mm
param lugs = 4
let lug_x = ring_or + lug_r - 1 mm
cylinder Lug radius lug_r height b base (lug_x,0,0) join targets Ring
box LugWeb size (lug_x-ring_or+2 mm) (2*lug_r) b corner (ring_or-2 mm,-lug_r,0) join targets Ring
cylinder LugHole radius (lug_hole_d/2) height (b+2 mm) base (lug_x,0,-1 mm) cut targets Ring
pattern circular Lugs features Lug, LugWeb, LugHole axis Z count lugs angle 360 deg

// Planet carrier: two plates joined by three pins through the planet bores, output shaft below
param gap = 1.5 mm
param plate_t = 4 mm
param pin_clear = 0.3 mm
param out_r = 7 mm
param out_len = 25 mm
let carrier_r = orbit + planet_bore_r + 6 mm
let pin_r = planet_bore_r - pin_clear
let low_z = -gap - plate_t
let top_z = b + gap
cylinder LowerPlate radius carrier_r height plate_t base (0,0,low_z) new -> body Carrier
cylinder UpperPlate radius carrier_r height plate_t base (0,0,top_z) join targets Carrier
cylinder Pin radius pin_r height (b+2*gap) base (orbit,0,-gap) join targets Carrier
pattern circular Pins features Pin axis Z count 3 angle 360 deg
cylinder SunWindow radius (sun_ra+1 mm) height (plate_t+2 mm) base (0,0,top_z-1 mm) cut targets Carrier
cylinder OutputShaft radius out_r height out_len base (0,0,low_z-out_len) join targets Carrier
box OutputKey size 2 mm 4 mm (out_len-5 mm) corner (out_r-1.5 mm,-2 mm,low_z-out_len-1 mm) cut targets Carrier

// Input shaft on the sun, with a D flat
param in_r = 5 mm
param in_len = 30 mm
cylinder InputShaft radius in_r height in_len base (0,0,b) join targets Sun
box InputFlat size 2 mm (2*in_r+2 mm) (in_len-8 mm) corner (in_r-1 mm,-in_r-1 mm,b+in_len-(in_len-8 mm)+1 mm) cut targets Sun

// Lightening holes between the pins, through both plates
param light_r = 5 mm
let light_at = orbit - 1 mm
cylinder Lightening radius light_r height (b+2*gap+2*plate_t+2 mm) base (light_at*cos(60 deg),light_at*sin(60 deg),low_z-1 mm) cut targets Carrier
pattern circular LightRing features Lightening axis Z count 3 angle 360 deg

// Edge breaks
chamfer CarrierRims edges (0,-carrier_r,low_z) (0,-carrier_r,top_z+plate_t) distance 0.8 mm body Carrier
chamfer RingFaces edges (ring_or*cos(45 deg),ring_or*sin(45 deg),0) (ring_or*cos(45 deg),ring_or*sin(45 deg),b) distance 0.8 mm body Ring
