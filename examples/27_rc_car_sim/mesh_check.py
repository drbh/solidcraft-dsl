"""Load the assembled car OBJ (one object per body), check every printed part is a closed solid,
compare volumes with the engine, and write per-group STL files for the physics model."""
import sys
import json
import trimesh
import numpy as np

def load_obj_objects(path):
    """Minimal OBJ reader keeping the `o` groups (trimesh merges them)."""
    verts, objs, cur = [], {}, None
    with open(path) as fh:
        for line in fh:
            if line.startswith("v "):
                verts.append([float(x) for x in line.split()[1:4]])
            elif line.startswith("o "):
                cur = line[2:].strip()
                objs[cur] = []
            elif line.startswith("f ") and cur is not None:
                idx = [int(tok.split("/")[0]) - 1 for tok in line.split()[1:]]
                for k in range(1, len(idx) - 1):
                    objs[cur].append((idx[0], idx[k], idx[k + 1]))
    verts = np.array(verts)
    out = {}
    for name, faces in objs.items():
        faces = np.array(faces)
        used, inv = np.unique(faces.ravel(), return_inverse=True)
        out[name] = trimesh.Trimesh(vertices=verts[used], faces=inv.reshape(-1, 3), process=False)
    return out

if __name__ == '__main__':
    obj_path, out_dir = sys.argv[1], sys.argv[2]
    geoms = load_obj_objects(obj_path)
    print(f"{len(geoms)} bodies in {obj_path}")

    ref = {"ServoF", "ServoR", "Motor", "Esc", "Battery", "Esp32"}
    fit = {"FitTestWall", "FitTestHub"}
    printed = [n for n in geoms if n not in ref and n not in fit]

    bad = []
    rows = []
    for n in sorted(printed):
        m = geoms[n].copy()
        m.merge_vertices()
        wt = m.is_watertight
        vol = m.volume if wt else float("nan")
        rows.append((n, wt, m.is_winding_consistent, vol / 1000, len(m.faces)))
        if not wt or vol <= 0:
            bad.append(n)
    print(f"{'body':12s} watertight winding  volume_cm3  faces")
    for r in rows:
        print(f"{r[0]:12s} {str(r[1]):9s} {str(r[2]):8s} {r[3]:10.2f} {r[4]:7d}")
    print("NOT CLOSED:", bad if bad else "none")

    # groups for the simulation (world mm -> saved in mm; the MJCF scales by 0.001)
    groups = {
        "chassis": ["Chassis", "YokeCapFR", "YokeCapFL", "YokeCapRR", "YokeCapRL", "TieRodF", "TieRodR", "HornF", "HornR",
                    "Axle", "Gear", "Idler", "Pinion", "AxleCupR", "AxleCupL", "BoneR", "BoneL", "ServoF", "ServoR", "Motor",
                    "Esc", "Battery", "Esp32"],
        "chassis_col": ["Chassis"],
        "knuckle_FR": ["KnuckleFR", "StubFR"], "knuckle_FL": ["KnuckleFL", "StubFL"],
        "knuckle_RR": ["KnuckleRR", "StubRR"], "knuckle_RL": ["KnuckleRL", "StubRL"],
        "wheel_FR": ["WheelFR"], "wheel_FL": ["WheelFL"], "wheel_RR": ["WheelRR"], "wheel_RL": ["WheelRL"],
    }
    import os
    os.makedirs(out_dir, exist_ok=True)
    for gname, names in groups.items():
        m = trimesh.util.concatenate([geoms[n] for n in names])
        # decimate heavy visuals so MuJoCo loads quickly
        if len(m.faces) > 60000:
            try:
                m = m.simplify_quadric_decimation(face_count=60000)
            except BaseException as e:  # optional dependency missing
                pass
        m.export(os.path.join(out_dir, gname + ".stl"))
        print(f"wrote {gname}.stl faces={len(m.faces)} bbox={np.round(m.bounds, 1).tolist()}")
    json.dump({"bad": bad, "printed": printed}, open(os.path.join(out_dir, "mesh_check.json"), "w"))
