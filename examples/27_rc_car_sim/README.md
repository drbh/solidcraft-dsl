# 27_rc_car: virtual build check

Checks the car without a printer: every printed part's exported mesh is a closed solid whose
volume matches the engine's B-rep, and a MuJoCo rigid-body model of the car (estimated masses,
the 1000KV motor on 2S through the 11.5:1 gears, MG995 torque on the knuckles, a solid rear axle,
PLA wheels on a floor with friction 0.7) drives, steers in the three modes and climbs.

```sh
python3 -m venv .venv && .venv/bin/pip install mujoco trimesh numpy matplotlib pillow
# the assembled design, one OBJ object per body
solidcraft-cli run --in build/rc-car/<run>/design.solidcraft --out sim/car.obj
.venv/bin/python examples/27_rc_car_sim/mesh_check.py sim/car.obj sim/meshes        # per-group STL for the model
.venv/bin/python examples/27_rc_car_sim/mesh_check2.py sim/car.obj <results.json with inspect.measure of the whole car>
.venv/bin/python examples/27_rc_car_sim/drive_sim.py $PWD/sim/meshes $PWD/sim/out  # prints the summary, writes paths.png, render.png
```

Results for the design as committed (MG995 / 28 mm outrunner version; engine 0.19.0, MuJoCo 3.17):
- mesh check: 28 printed parts, 779 cm3, worst mesh/B-rep volume difference 0.12 %, at most 8
  unshared edges per part (tessellation T-junctions, not holes); about 0.8 kg with electronics
- straight, 100 % throttle: 3.7 m/s after 4 s (the no-load limit of 1000KV on 2S through 11.5:1)
- straight, 40 % throttle (the firmware's THROTTLE_LIMIT): 1.5 m/s steady, 63 % of it in 0.28 s,
  the chassis never touches the floor
- slow turns (0.4 m/s): front steer 20 deg circles at 0.68 m (kinematic 0.52 m), 4WS at 0.35 m
  (kinematic 0.26 m); the solid rear axle understeers
- fast turns at 40 % throttle: 1.28 m front steer, 0.85 m 4WS; the knuckles hold 20 deg against
  the tyre loads with the MG995s
- crab 20/20 deg: 5.4 m forward and 0.85 m sideways with a 15 deg heading change
- slopes: climbs 10 deg at 1.4 m/s and 20 deg at 1.2 m/s (traction-limited, not torque-limited)

What the model does not cover: print tolerances, slop in the horn pin-and-slot link, the dog
bones' plunge (the drive is applied at the wheels), gear tooth strength under a stalled motor.
Print the fit_test plate for the fits; keep the ESC's throttle cap on until the car has been seen
to run.
