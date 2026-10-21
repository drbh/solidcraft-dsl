// 4. A calculated closed profile revolved about a world axis.
// Fixed seed profile is intentionally rebuilt from source when its radii/lengths change.
design SteppedShaft units mm
param shaft_r = 6 mm
param collar_r = 10 mm
param shaft_len = 20 mm
param collar_len = 6 mm
let total_len = shaft_len + collar_len
sketch Section on XZ {
  polyline closed profile = (0,0) (shaft_r,0) (shaft_r,-shaft_len) (collar_r,-shaft_len) (collar_r,-total_len) (0,-total_len)
  fix profile.1 profile.2 profile.3 profile.4 profile.5 profile.6
}
revolve Shaft from Section axis Z -> body ShaftBody
