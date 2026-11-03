#!/usr/bin/env python3
"""Build every example and merge their GLB exports into one scene laid out on a grid.

    ci/examples-glb.py [-o build/all-examples.glb] [-j 4] [--force] [--only 3 4 ...]

Each example is evaluated with a GLB-only request into build/all-examples/<name>/ and reused
on later runs until its .sc file (or the dsl binary) changes. Examples that fail are reported
and left out. Every example becomes a named node, so viewers list them by name. Models stand on
the ground (y = 0) in rows ordered by file name. No dependencies beyond the standard library.
"""
import argparse
import functools
import hashlib
import json
import math
import os
import struct
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
REQUEST = json.dumps({"version": 1, "defaults": False,
                      "requests": [{"id": "mesh", "kind": "export", "format": "glb"}]})


@functools.cache
def toolchain_hash():
    """The compiler's sources and the engine version: a change to either rebuilds every example."""
    digest = hashlib.sha256()
    for path in sorted([*ROOT.glob("src/**/*.rs"), *ROOT.glob("crates/**/*.rs"), ROOT / "Cargo.lock"]):
        digest.update(path.relative_to(ROOT).as_posix().encode() + b"\0" + path.read_bytes())
    cli = os.environ.get("SOLIDCRAFT_CLI", "solidcraft-cli")
    try:
        version = subprocess.run([cli, "--version"], capture_output=True, text=True, timeout=30).stdout
    except OSError:
        version = ""
    digest.update(version.encode())
    return digest.hexdigest()


def inputs_hash(source):
    """What an example's GLB depends on: the toolchain, the example, and its <name>_* folders
    (parts it `use`s, art it imports). Content, not mtimes, so a fresh checkout (CI) reuses a
    restored cache."""
    digest = hashlib.sha256(toolchain_hash().encode() + source.read_bytes())
    for folder in sorted(source.parent.glob(f"{source.stem}_*")):
        for f in sorted(folder.rglob("*")) if folder.is_dir() else []:
            if f.is_file():
                digest.update(f.relative_to(source.parent).as_posix().encode() + b"\0" + f.read_bytes())
    return digest.hexdigest()


def build(source, out_dir, binary, force, timeout):
    """Evaluate one example; return the path of its GLB or raise with the reason."""
    glb = next(out_dir.rglob("*.glb"), None) if out_dir.exists() else None
    stamp = out_dir / "inputs.sha256"
    key = inputs_hash(source)
    if glb and not force:
        if stamp.exists() and stamp.read_text().strip() == key:
            return glb, "cached"
        if not stamp.exists():
            # A cache from before the stamp: adopt it if it is newer than its sources.
            parts = source.with_name(f"{source.stem}_parts")
            inputs = [source, binary] + (sorted(parts.rglob("*.sc")) if parts.is_dir() else [])
            if glb.stat().st_mtime > max(p.stat().st_mtime for p in inputs):
                stamp.write_text(key)
                return glb, "cached"
    for old in out_dir.rglob("*.glb") if out_dir.exists() else []:
        old.unlink()
    proc = subprocess.run(
        [str(binary), "evaluate", str(source), "--output-dir", str(out_dir), "--requests", "-",
         "--no-previews", "--json"],
        input=REQUEST, capture_output=True, text=True, timeout=timeout, cwd=ROOT)
    glb = next(out_dir.rglob("*.glb"), None)
    if proc.returncode != 0 or glb is None:
        try:
            diags = json.loads(proc.stdout).get("diagnostics", [])
            reason = next((d["message"] for d in diags if d.get("severity") == "error"), None)
        except (json.JSONDecodeError, AttributeError):
            reason = None
        raise RuntimeError(reason or (proc.stderr.strip().splitlines() or ["no GLB produced"])[-1])
    stamp.write_text(key)
    return glb, "built"


def read_glb(path):
    data = path.read_bytes()
    magic, _, _ = struct.unpack_from("<4sII", data)
    if magic != b"glTF":
        raise ValueError(f"{path} is not a GLB")
    gltf, binary, offset = None, b"", 12
    while offset < len(data):
        length, kind = struct.unpack_from("<I4s", data, offset)
        chunk = data[offset + 8: offset + 8 + length]
        if kind == b"JSON":
            gltf = json.loads(chunk)
        elif kind == b"BIN\0":
            binary = chunk
        offset += 8 + length
    return gltf, binary


def write_glb(path, gltf, binary):
    text = json.dumps(gltf, separators=(",", ":")).encode()
    text += b" " * (-len(text) % 4)
    binary += b"\0" * (-len(binary) % 4)
    total = 12 + 8 + len(text) + (8 + len(binary) if binary else 0)
    with open(path, "wb") as f:
        f.write(struct.pack("<4sII", b"glTF", 2, total))
        f.write(struct.pack("<I4s", len(text), b"JSON") + text)
        if binary:
            f.write(struct.pack("<I4s", len(binary), b"BIN\0") + binary)


# --- transforms, enough to find each model's world-space bounding box ---

def matmul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def node_matrix(node):
    if "matrix" in node:
        m = node["matrix"]  # column-major
        return [[m[c * 4 + r] for c in range(4)] for r in range(4)]
    x, y, z, w = node.get("rotation", [0, 0, 0, 1])
    sx, sy, sz = node.get("scale", [1, 1, 1])
    tx, ty, tz = node.get("translation", [0, 0, 0])
    r = [[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
         [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
         [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]]
    return [[r[0][0] * sx, r[0][1] * sy, r[0][2] * sz, tx],
            [r[1][0] * sx, r[1][1] * sy, r[1][2] * sz, ty],
            [r[2][0] * sx, r[2][1] * sy, r[2][2] * sz, tz],
            [0, 0, 0, 1]]


def bounds(gltf, roots):
    lo, hi = [math.inf] * 3, [-math.inf] * 3
    identity = [[float(i == j) for j in range(4)] for i in range(4)]
    stack = [(n, identity) for n in roots]
    while stack:
        index, parent = stack.pop()
        node = gltf["nodes"][index]
        world = matmul(parent, node_matrix(node))
        stack += [(c, world) for c in node.get("children", [])]
        if "mesh" not in node:
            continue
        for prim in gltf["meshes"][node["mesh"]]["primitives"]:
            acc = gltf["accessors"][prim["attributes"]["POSITION"]]
            for corner in range(8):
                p = [acc["max" if corner >> i & 1 else "min"][i] for i in range(3)] + [1]
                q = [sum(world[r][k] * p[k] for k in range(4)) for r in range(3)]
                lo = [min(a, b) for a, b in zip(lo, q)]
                hi = [max(a, b) for a, b in zip(hi, q)]
    return lo, hi


# --- merging ---

# Top-level arrays, and the keys inside other objects that index into each of them.
ARRAYS = ["accessors", "bufferViews", "buffers", "meshes", "materials", "nodes", "images",
          "textures", "samplers", "skins", "cameras", "animations"]


def remap(gltf, base):
    """Shift every index in one glTF by the sizes of the arrays merged before it."""
    def tex(info):
        if isinstance(info, dict) and "index" in info:
            info["index"] += base["textures"]

    for view in gltf.get("bufferViews", []):
        view["buffer"] = 0  # all binary chunks are concatenated into buffer 0 by the caller
    for acc in gltf.get("accessors", []):
        if "bufferView" in acc:
            acc["bufferView"] += base["bufferViews"]
        sparse = acc.get("sparse")
        if sparse:
            sparse["indices"]["bufferView"] += base["bufferViews"]
            sparse["values"]["bufferView"] += base["bufferViews"]
    for mesh in gltf.get("meshes", []):
        for prim in mesh["primitives"]:
            prim["attributes"] = {k: v + base["accessors"] for k, v in prim["attributes"].items()}
            if "indices" in prim:
                prim["indices"] += base["accessors"]
            if "material" in prim:
                prim["material"] += base["materials"]
            for target in prim.get("targets", []):
                for k in target:
                    target[k] += base["accessors"]
    for mat in gltf.get("materials", []):
        pbr = mat.get("pbrMetallicRoughness", {})
        for key in ("baseColorTexture", "metallicRoughnessTexture"):
            tex(pbr.get(key))
        for key in ("normalTexture", "occlusionTexture", "emissiveTexture"):
            tex(mat.get(key))
    for t in gltf.get("textures", []):
        if "source" in t:
            t["source"] += base["images"]
        if "sampler" in t:
            t["sampler"] += base["samplers"]
    for image in gltf.get("images", []):
        if "bufferView" in image:
            image["bufferView"] += base["bufferViews"]
    for node in gltf.get("nodes", []):
        for key, array in (("mesh", "meshes"), ("skin", "skins"), ("camera", "cameras")):
            if key in node:
                node[key] += base[array]
        if "children" in node:
            node["children"] = [c + base["nodes"] for c in node["children"]]
    for skin in gltf.get("skins", []):
        skin["joints"] = [j + base["nodes"] for j in skin["joints"]]
        if "skeleton" in skin:
            skin["skeleton"] += base["nodes"]
        if "inverseBindMatrices" in skin:
            skin["inverseBindMatrices"] += base["accessors"]
    for anim in gltf.get("animations", []):
        for sampler in anim["samplers"]:
            sampler["input"] += base["accessors"]
            sampler["output"] += base["accessors"]
        for channel in anim["channels"]:
            channel["target"]["node"] += base["nodes"]


def merge(models, gap):
    """models: [(name, gltf, binary)] -> merged (gltf, binary) with one grid node per model."""
    placed = []
    for name, gltf, binary in models:
        scene = gltf["scenes"][gltf.get("scene", 0)]
        lo, hi = bounds(gltf, scene["nodes"])
        if math.inf in hi or -math.inf in hi:
            print(f"  skip {name}: empty mesh", file=sys.stderr)
            continue
        placed.append((name, gltf, binary, scene["nodes"], lo, hi))

    # Rows of roughly equal width, in the ground (x, z) plane of the Y-up scene.
    per_row = max(1, round(math.sqrt(len(placed))))
    out = {"asset": {"version": "2.0", "generator": "solidcraft-dsl ci/examples-glb.py"},
           "scene": 0, "scenes": [{"name": "examples", "nodes": []}], "buffers": [{}]}
    blob = bytearray()
    used = set()
    z = 0.0
    for row_start in range(0, len(placed), per_row):
        row = placed[row_start:row_start + per_row]
        x = 0.0
        depth = max(hi[2] - lo[2] for *_, lo, hi in row)
        for name, gltf, binary, roots, lo, hi in row:
            base = {k: len(out.get(k, [])) for k in ARRAYS}
            blob += b"\0" * (-len(blob) % 8)
            for view in gltf.get("bufferViews", []):
                view["byteOffset"] = view.get("byteOffset", 0) + len(blob)
            blob += binary
            remap(gltf, base)
            roots = [r + base["nodes"] for r in roots]
            for key in ARRAYS:
                if key != "buffers" and gltf.get(key):
                    out.setdefault(key, []).extend(gltf[key])
            used.update(gltf.get("extensionsUsed", []))
            # Shift the model so its bounding box starts at (x, 0, z) and is centred in the row.
            offset = [x - lo[0], -lo[1], z + (depth - (hi[2] - lo[2])) / 2 - lo[2]]
            out["nodes"].append({"name": name, "translation": offset, "children": roots})
            out["scenes"][0]["nodes"].append(len(out["nodes"]) - 1)
            x += hi[0] - lo[0] + gap
        z += depth + gap
    out["buffers"][0]["byteLength"] = len(blob)
    if used:
        out["extensionsUsed"] = sorted(used)
    return out, bytes(blob)


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("-o", "--output", type=Path, default=ROOT / "build/all-examples.glb")
    parser.add_argument("-j", "--jobs", type=int, default=4)
    parser.add_argument("--force", action="store_true", help="rebuild cached examples")
    parser.add_argument("--only", nargs="+", default=[],
                        help="example name prefixes, e.g. --only 30 31 bracket")
    parser.add_argument("--gap", type=float, default=30, help="spacing between models in mm")
    parser.add_argument("--timeout", type=float, default=1200, help="seconds per example")
    parser.add_argument("--bin", type=Path, default=ROOT / "target/debug/solidcraft-dsl")
    args = parser.parse_args()

    if not args.bin.exists():
        subprocess.run(["cargo", "build", "--quiet"], cwd=ROOT, check=True)
    sources = sorted(p for p in (ROOT / "examples").glob("*.sc")
                     if not args.only or any(p.stem.startswith(o) for o in args.only))
    work = ROOT / "build/all-examples"

    def job(source):
        try:
            glb, how = build(source, work / source.stem, args.bin, args.force, args.timeout)
            print(f"  {how:6} {source.stem}", file=sys.stderr)
            return source.stem, glb, None
        except Exception as error:  # noqa: BLE001 - report and keep going
            print(f"  FAILED {source.stem}: {error}", file=sys.stderr)
            return source.stem, None, str(error)

    print(f"building {len(sources)} examples with {args.jobs} jobs", file=sys.stderr)
    with ThreadPoolExecutor(args.jobs) as pool:
        results = list(pool.map(job, sources))

    models = [(name, *read_glb(glb)) for name, glb, _ in results if glb]
    gltf, binary = merge(models, args.gap / 1000)  # engine GLBs are in metres
    args.output.parent.mkdir(parents=True, exist_ok=True)
    write_glb(args.output, gltf, binary)
    failed = [(name, err) for name, _, err in results if err]
    print(f"wrote {args.output} ({len(models)} examples, {len(binary) / 1e6:.1f} MB)",
          file=sys.stderr)
    for name, err in failed:
        print(f"  left out {name}: {err}", file=sys.stderr)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
