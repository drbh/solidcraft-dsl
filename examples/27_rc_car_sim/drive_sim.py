"""MuJoCo simulation of examples/27_rc_car.sc: the measured masses, the FS90R drive through the
24:14 gears (speed and torque limits), SG90 steering limits, PLA wheels on a floor.
Tests: straight-line speed, front-steer turning circle, 4WS turning circle, crab mode, a slope,
and that the chassis never touches the ground."""
import sys, os, math, json
import numpy as np
import mujoco

mesh_dir, out_dir = sys.argv[1], sys.argv[2]
os.makedirs(out_dir, exist_ok=True)

# ---- numbers from the design / datasheets (SI)
WB, HALF_TRACK, R_W = 0.190, 0.1075, 0.055
Y_KP = 0.075
M_CHASSIS = 0.75           # printed chassis + 2 x MG995, motor, ESC, 2S 1100 mAh, board
M_WHEEL, M_KNUCKLE = 0.06, 0.012
MOTOR_KV, VOLTS = 1000.0, 7.4
MOTOR_T = 0.25             # N m, a 28 mm outrunner's short-time torque
RATIO = 46.0 / 12.0 * 36.0 / 12.0     # 11.5:1 reduction
EFF = 0.85
T_AXLE = MOTOR_T * RATIO * EFF        # total at the axle
W_MAX = MOTOR_KV * VOLTS / 60 * 2 * math.pi / RATIO   # rad/s at the wheels, no load
THROTTLE = 0.4             # the firmware's THROTTLE_LIMIT
STEER_MAX = math.radians(20)
MU = 0.7                   # PLA / O-ring on a wooden floor
T_STEER = 0.98 * 22.0 / 28.0   # MG995 (10 kg cm) through the 22 mm horn / 28 mm arm, per axle

def corner(tag, x, y):
    sgn = 1 if y > 0 else -1
    return f"""
    <body name="knuckle_{tag}" pos="{x} {y} 0">
      <joint name="steer_{tag}" type="hinge" axis="0 0 1" range="-0.5 0.5" damping="0.002"/>
      <inertial pos="0 {sgn*0.012} 0" mass="{M_KNUCKLE}" diaginertia="1e-6 1e-6 1e-6"/>
      <geom type="mesh" mesh="knuckle_{tag}" pos="{-x} {-y} 0" contype="0" conaffinity="0" rgba="0.6 0.6 0.65 1"/>
      <body name="wheel_{tag}" pos="0 {sgn*(HALF_TRACK - Y_KP)} 0">
        <joint name="spin_{tag}" type="hinge" axis="0 1 0" damping="0.0002"/>
        <inertial pos="0 0 0" mass="{M_WHEEL}" diaginertia="5e-5 9e-5 5e-5"/>
        <geom type="cylinder" size="{R_W} 0.018" quat="0.7071 0.7071 0 0" friction="{MU} 0.005 0.0001" rgba="0.2 0.2 0.2 1" condim="4"/>
        <geom type="mesh" mesh="wheel_{tag}" pos="{-x} {-HALF_TRACK*sgn} 0" contype="0" conaffinity="0" rgba="0.3 0.3 0.3 1"/>
      </body>
    </body>"""

def model_xml(slope_deg=0.0):
    s = math.radians(slope_deg)
    # floor tilted about Y so +X is uphill
    floor_quat = f"{math.cos(s/2)} 0 {math.sin(-s/2)} 0"
    corners = corner("FR", WB/2, Y_KP) + corner("FL", WB/2, -Y_KP) + corner("RR", -WB/2, Y_KP) + corner("RL", -WB/2, -Y_KP)
    return f"""
<mujoco model="rc_car">
  <compiler angle="radian" meshdir="{mesh_dir}"/>
  <option timestep="0.0005" gravity="0 0 -9.81" integrator="implicitfast"/>
  <asset>
    <mesh name="chassis" file="chassis.stl" scale="0.001 0.001 0.001"/>
    <mesh name="chassis_col" file="chassis_col.stl" scale="0.001 0.001 0.001"/>
    <mesh name="knuckle_FR" file="knuckle_FR.stl" scale="0.001 0.001 0.001"/>
    <mesh name="knuckle_FL" file="knuckle_FL.stl" scale="0.001 0.001 0.001"/>
    <mesh name="knuckle_RR" file="knuckle_RR.stl" scale="0.001 0.001 0.001"/>
    <mesh name="knuckle_RL" file="knuckle_RL.stl" scale="0.001 0.001 0.001"/>
    <mesh name="wheel_FR" file="wheel_FR.stl" scale="0.001 0.001 0.001"/>
    <mesh name="wheel_FL" file="wheel_FL.stl" scale="0.001 0.001 0.001"/>
    <mesh name="wheel_RR" file="wheel_RR.stl" scale="0.001 0.001 0.001"/>
    <mesh name="wheel_RL" file="wheel_RL.stl" scale="0.001 0.001 0.001"/>
    <texture name="grid" type="2d" builtin="checker" rgb1="0.8 0.8 0.8" rgb2="0.6 0.6 0.6" width="256" height="256"/>
    <material name="grid" texture="grid" texrepeat="40 40"/>
  </asset>
  <worldbody>
    <light pos="0 0 3" dir="0 0 -1"/>
    <geom name="floor" type="plane" size="5 5 0.1" quat="{floor_quat}" material="grid" friction="{MU} 0.005 0.0001"/>
    <body name="car" pos="0 0 {R_W + 0.0005}">
      <freejoint/>
      <inertial pos="-0.015 -0.004 -0.004" mass="{M_CHASSIS}" diaginertia="0.004 0.007 0.009"/>
      <geom type="mesh" mesh="chassis" contype="0" conaffinity="0" rgba="0.75 0.75 0.8 1"/>
      <geom name="chassis_col" type="box" size="0.126 0.065 0.002" pos="0 0 -0.028" rgba="0 0 0 0"/>
      <geom name="bumper_f" type="box" size="0.0025 0.077 0.0225" pos="0.1235 0 -0.00775" rgba="0 0 0 0"/>
      <geom name="bumper_r" type="box" size="0.0025 0.077 0.0225" pos="-0.1235 0 -0.00775" rgba="0 0 0 0"/>
      <geom name="gearbox" type="box" size="0.014 0.016 0.0165" pos="-0.093 0 -0.0095" rgba="0 0 0 0"/>
      <geom name="motor" type="box" size="0.018 0.016 0.016" pos="-0.0635 -0.0245 0.036" rgba="0 0 0 0"/>
      {corners}
    </body>
  </worldbody>
  <equality>
    <joint joint1="steer_FR" joint2="steer_FL"/>
    <joint joint1="steer_RR" joint2="steer_RL"/>
    <joint joint1="spin_RR" joint2="spin_RL"/>
  </equality>
  <visual><global offwidth="1280" offheight="960"/></visual>
  <actuator>
    <position name="servo_front" joint="steer_FR" kp="30" ctrlrange="-0.5 0.5" forcerange="-{T_STEER} {T_STEER}"/>
    <position name="servo_rear" joint="steer_RR" kp="30" ctrlrange="-0.5 0.5" forcerange="-{T_STEER} {T_STEER}"/>
    <velocity name="drive_R" joint="spin_RR" kv="0.05" ctrlrange="-{W_MAX} {W_MAX}" forcerange="-{T_AXLE/2} {T_AXLE/2}"/>
    <velocity name="drive_L" joint="spin_RL" kv="0.05" ctrlrange="-{W_MAX} {W_MAX}" forcerange="-{T_AXLE/2} {T_AXLE/2}"/>
  </actuator>
  <sensor>
    <framepos name="pos" objtype="body" objname="car"/>
    <framequat name="quat" objtype="body" objname="car"/>
  </sensor>
</mujoco>"""

def run(name, steer_f, steer_r, throttle, seconds, slope=0.0):
    m = mujoco.MjModel.from_xml_string(model_xml(slope))
    d = mujoco.MjData(m)
    if slope:
        # start the car on the tilted plane: rotate its pose by the slope about Y
        s = math.radians(slope)
        d.qpos[3:7] = [math.cos(s/2), 0, math.sin(-s/2), 0]
        d.qpos[0:3] = [0, 0, R_W / math.cos(s) + 0.001]
    mujoco.mj_forward(m, d)
    floor = mujoco.mj_name2id(m, mujoco.mjtObj.mjOBJ_GEOM, "floor")
    car_body = mujoco.mj_name2id(m, mujoco.mjtObj.mjOBJ_BODY, "car")
    traj, chassis_touch, min_clear = [], 0, 1e9
    n = int(seconds / m.opt.timestep)
    for i in range(n):
        t = d.time
        ramp = min(1.0, t / 0.3)
        d.ctrl[0] = steer_f * STEER_MAX * ramp
        d.ctrl[1] = steer_r * STEER_MAX * ramp
        d.ctrl[2] = d.ctrl[3] = throttle * W_MAX * ramp
        mujoco.mj_step(m, d)
        for c in d.contact[:d.ncon]:
            if floor in (c.geom1, c.geom2) and m.geom_bodyid[c.geom1 if c.geom2 == floor else c.geom2] == car_body:
                chassis_touch += 1
        if i % 40 == 0:
            q = d.qpos[3:7]
            yaw = math.atan2(2*(q[0]*q[3] + q[1]*q[2]), 1 - 2*(q[2]**2 + q[3]**2))
            traj.append((t, d.qpos[0], d.qpos[1], d.qpos[2], yaw, float(np.linalg.norm(d.qvel[0:2]))))
    traj = np.array(traj)
    traj[:, 4] = np.unwrap(traj[:, 4])
    jf = m.jnt_qposadr[mujoco.mj_name2id(m, mujoco.mjtObj.mjOBJ_JOINT, "steer_FR")]
    jr = m.jnt_qposadr[mujoco.mj_name2id(m, mujoco.mjtObj.mjOBJ_JOINT, "steer_RR")]
    run.steer = (math.degrees(d.qpos[jf]), math.degrees(d.qpos[jr]))
    return m, d, traj, chassis_touch

results = {}
summary = []

# 1. straight line at the firmware's throttle cap, then flat out
m, d, tr, touch = run("straight_full", 0, 0, 1.0, 4.0)
results["straight_full"] = dict(speed_mps=tr[-1, 5], heading_drift_deg=math.degrees(tr[-1, 4]), chassis_floor_contacts=int(touch))
summary.append(f"straight, 100% throttle: {tr[-1,5]:.2f} m/s after 4 s (no-load {W_MAX*R_W:.2f}), chassis-floor contacts {touch}")
m, d, tr, touch = run("straight", 0, 0, THROTTLE, 4.0)
v_end = tr[-1, 5]; dist = math.hypot(tr[-1, 1] - tr[0, 1], tr[-1, 2] - tr[0, 2])
t63 = tr[np.argmax(tr[:, 5] > 0.63 * v_end), 0]
results["straight"] = dict(speed_mps=v_end, distance_m=dist, time_to_63pct_s=t63, chassis_floor_contacts=int(touch),
                           heading_drift_deg=math.degrees(tr[-1, 4]))
summary.append(f"straight, {int(THROTTLE*100)}% throttle (firmware cap): {v_end:.2f} m/s steady, {dist:.2f} m in 4 s, reaches 63% in {t63:.2f} s, heading drift {math.degrees(tr[-1,4]):.1f} deg, chassis-floor contacts {touch}")

def turning(tr):
    # fit a circle to the xy path after the ramp
    pts = tr[tr[:, 0] > 1.0][:, 1:3]
    A = np.c_[2*pts, np.ones(len(pts))]
    b = (pts**2).sum(1)
    cx, cy, c = np.linalg.lstsq(A, b, rcond=None)[0]
    return math.sqrt(c + cx*cx + cy*cy)

# 2. front steer only
m, d, tr, touch = run("front", 1.0, 0.0, THROTTLE, 6.0)
r_front = turning(tr)
results["front_steer"] = dict(radius_m=r_front, yaw_total_deg=math.degrees(tr[-1, 4]), speed_mps=tr[-1, 5], chassis_floor_contacts=int(touch))
summary.append(f"front steer 20 deg: turning radius {r_front:.2f} m (geometry {WB/math.tan(STEER_MAX):.2f} m to the rear axle centre), yawed {math.degrees(tr[-1,4]):.0f} deg in 6 s")

# 3. four-wheel counter steer
m, d, tr, touch = run("4ws", 1.0, -1.0, THROTTLE, 6.0)
r_4ws = turning(tr)
results["4ws"] = dict(radius_m=r_4ws, yaw_total_deg=math.degrees(tr[-1, 4]), speed_mps=tr[-1, 5], chassis_floor_contacts=int(touch))
summary.append(f"4WS 20/-20 deg: turning radius {r_4ws:.2f} m (geometry {WB/2/math.tan(STEER_MAX):.2f} m), yawed {math.degrees(tr[-1,4]):.0f} deg in 6 s, knuckles held at {run.steer[0]:.1f}/{run.steer[1]:.1f} deg, {tr[-1,5]:.2f} m/s")

# 3b. slow turns: close to the kinematic radii
m, d, tr, touch = run("front_slow", 1.0, 0.0, 0.12, 8.0)
r_fs = turning(tr)
m, d, tr2, touch = run("4ws_slow", 1.0, -1.0, 0.12, 8.0)
r_4s = turning(tr2)
results["slow_turns"] = dict(front_radius_m=r_fs, fws_radius_m=r_4s, speed_mps=tr[-1, 5])
summary.append(f"slow turns at {tr[-1,5]:.2f} m/s: front steer radius {r_fs:.2f} m (kinematic {WB/math.tan(STEER_MAX):.2f}), 4WS radius {r_4s:.2f} m (kinematic {WB/2/math.tan(STEER_MAX):.2f})")

# 4. crab
m, d, tr, touch = run("crab", 1.0, 1.0, THROTTLE, 4.0)
dx, dy = tr[-1, 1] - tr[0, 1], tr[-1, 2] - tr[0, 2]
results["crab"] = dict(dx_m=dx, dy_m=dy, path_angle_deg=math.degrees(math.atan2(dy, dx)), yaw_total_deg=math.degrees(tr[-1, 4]))
summary.append(f"crab 20/20 deg: moved {dx:.2f} m forward and {dy:.2f} m sideways (path {math.degrees(math.atan2(dy,dx)):.0f} deg, wheels at 20), yaw change {math.degrees(tr[-1,4]):.1f} deg")

# 5. slope climb
for slope in (10, 20):
    m, d, tr, touch = run(f"slope{slope}", 0, 0, THROTTLE, 5.0, slope=slope)
    climbed = tr[-1, 1] - tr[0, 1]
    results[f"slope_{slope}"] = dict(advance_m=climbed, speed_mps=tr[-1, 5])
    summary.append(f"{slope} deg slope: {'climbs' if climbed > 0.2 else 'stalls'} ({climbed:.2f} m uphill in 5 s, {tr[-1,5]:.2f} m/s)")

json.dump(results, open(os.path.join(out_dir, "sim_results.json"), "w"), indent=1)
print("\n".join(summary))

# plot the three steering-mode paths
try:
    import matplotlib
    matplotlib.use("Agg")
    import matplotlib.pyplot as plt
    fig, ax = plt.subplots(figsize=(7, 7))
    for name, sf, sr, sec in (("front steer", 1, 0, 6), ("4WS counter", 1, -1, 6), ("crab", 1, 1, 4), ("straight", 0, 0, 4)):
        _, _, tr, _ = run(name, sf, sr, THROTTLE, sec)
        ax.plot(tr[:, 1], tr[:, 2], label=name)
    ax.set_aspect("equal"); ax.grid(True); ax.legend(); ax.set_xlabel("x [m]"); ax.set_ylabel("y [m]")
    ax.set_title(f"27_rc_car in MuJoCo: paths at {int(THROTTLE*100)}% throttle")
    fig.savefig(os.path.join(out_dir, "paths.png"), dpi=120)
    print("wrote paths.png")
except Exception as e:
    print("plot skipped:", e)

# try an offscreen render
try:
    m = mujoco.MjModel.from_xml_string(model_xml(0))
    d = mujoco.MjData(m)
    d.ctrl[:] = [STEER_MAX, -STEER_MAX, 0, 0]
    for _ in range(2000):
        mujoco.mj_step(m, d)
    r = mujoco.Renderer(m, 720, 960)
    cam = mujoco.MjvCamera(); cam.lookat[:] = d.qpos[0:3]; cam.distance = 0.9; cam.azimuth = 150; cam.elevation = -25
    r.update_scene(d, cam)
    import PIL.Image
    PIL.Image.fromarray(r.render()).save(os.path.join(out_dir, "render.png"))
    print("wrote render.png")
except Exception as e:
    print("render skipped:", type(e).__name__, e)
