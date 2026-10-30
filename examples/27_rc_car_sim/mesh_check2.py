"""Closedness of each printed part's tessellation and its volume against the engine's B-rep volume."""
import sys, json, numpy as np, trimesh
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mesh_check import load_obj_objects  # noqa
obj_path, engine_json = sys.argv[1], sys.argv[2]
geoms = load_obj_objects(obj_path)
eng = {b['name']: b['volume_mm3'] for b in json.load(open(engine_json))[1]['bodies']}
skip = {"ServoF", "ServoR", "Motor", "Esc", "Battery", "Esp32", "FitTestWall", "FitTestHub"}
print(f"{'body':12s} open_edges  mesh_cm3  brep_cm3  diff%")
worst = 0
for n in sorted(geoms):
    if n in skip: continue
    m = geoms[n].copy()
    m.merge_vertices(digits_vertex=4)
    m.update_faces(m.nondegenerate_faces())
    bound = trimesh.grouping.group_rows(m.edges_sorted, require_count=1)
    vol = abs(m.volume) / 1000
    bv = eng[n] / 1000
    diff = 100 * (vol - bv) / bv
    worst = max(worst, abs(diff))
    print(f"{n:12s} {len(bound):10d} {vol:9.2f} {bv:9.2f} {diff:6.2f}")
print(f"worst volume difference {worst:.2f}%")
