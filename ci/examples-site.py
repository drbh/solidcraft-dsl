#!/usr/bin/env python3
"""Build every example and compile a single-page HTML gallery: source on the left, model on the right.

    ci/examples-site.py [-o build/examples.html] [-j 4] [--force] [--only 3 4 ...]

GLBs are built (and cached) exactly as ci/examples-glb.py does, in build/all-examples/<name>/.
Each one is gzipped and embedded in the page, so the result is one self-contained HTML file.
The page itself is a Svelte app in site/; this script writes its manifest and runs the Vite build.
Examples that fail to build still appear, with the error in place of the model.
"""
import argparse
import base64
import gzip
import importlib.util
import json
import os
import re
import shutil
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SITE = ROOT / "site"

_spec = importlib.util.spec_from_file_location("examples_glb", ROOT / "ci/examples-glb.py")
glb_tools = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(glb_tools)


ACRONYMS = {"rc", "pcb", "esp32", "led", "usb"}


def describe(source):
    """Title and one-line summary from the file name and the leading comment block."""
    stem = source.stem
    match = re.match(r"(\d+)_(.*)", stem)
    number, rest = (match.group(1), match.group(2)) if match else (None, stem)
    words = rest.split("_")
    title = " ".join(w.upper() if w in ACRONYMS or re.fullmatch(r"[a-z]{2,}\d+", w) else w for w in words)
    title = title[:1].upper() + title[1:]
    text = source.read_text()
    summary = ""
    for line in text.splitlines():
        if not line.startswith("//"):
            break
        line = line[2:].strip()
        if line:
            summary = re.sub(r"^\d+\.?\s*(\([^)]*\)\.?)?\s*", "", line) if number else line
            break
    return number, title, summary, text


def stats(glb):
    """Triangle count and bounding-box size in mm (the engine writes metres, Y up)."""
    gltf, _ = glb_tools.read_glb(glb)
    scene = gltf["scenes"][gltf.get("scene", 0)]
    lo, hi = glb_tools.bounds(gltf, scene["nodes"])
    triangles = 0
    for mesh in gltf.get("meshes", []):
        for prim in mesh["primitives"]:
            index = prim.get("indices", prim["attributes"]["POSITION"])
            triangles += gltf["accessors"][index]["count"] // 3
    # Report design axes: X, Y (glTF -Z), Z (glTF Y).
    size = [hi[0] - lo[0], hi[2] - lo[2], hi[1] - lo[1]]
    return triangles, [round(s * 1000, 2) for s in size]


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("-o", "--output", type=Path, default=ROOT / "build/examples.html")
    parser.add_argument("-j", "--jobs", type=int, default=4)
    parser.add_argument("--force", action="store_true", help="rebuild cached examples")
    parser.add_argument("--only", nargs="+", default=[],
                        help="example name prefixes, e.g. --only 30 31 bracket")
    parser.add_argument("--timeout", type=float, default=1200, help="seconds per example")
    parser.add_argument("--bin", type=Path, default=ROOT / "target/debug/solidcraft-dsl")
    parser.add_argument("--work", type=Path, default=ROOT / "build/all-examples",
                        help="GLB cache directory (shared with ci/examples-glb.py)")
    parser.add_argument("--manifest-only", action="store_true",
                        help="write the manifest for `npm run dev` and stop")
    args = parser.parse_args()

    if not args.bin.exists():
        subprocess.run(["cargo", "build", "--quiet"], cwd=ROOT, check=True)
    sources = sorted(p for p in (ROOT / "examples").glob("*.sc")
                     if not args.only or any(p.stem.startswith(o) for o in args.only))

    def job(source):
        try:
            glb, how = glb_tools.build(source, args.work / source.stem, args.bin, args.force,
                                       args.timeout)
            print(f"  {how:6} {source.stem}", file=sys.stderr)
            return source, glb, None
        except Exception as error:  # noqa: BLE001 - report and keep going
            print(f"  FAILED {source.stem}: {error}", file=sys.stderr)
            return source, None, str(error)

    print(f"building {len(sources)} examples with {args.jobs} jobs", file=sys.stderr)
    with ThreadPoolExecutor(args.jobs) as pool:
        results = list(pool.map(job, sources))

    examples = []
    for source, glb, error in results:
        number, title, summary, text = describe(source)
        entry = {"name": source.stem, "file": f"examples/{source.name}", "number": number,
                 "title": title, "summary": summary, "source": text, "error": error}
        if glb:
            raw = glb.read_bytes()
            entry["triangles"], entry["size"] = stats(glb)
            entry["glbBytes"] = len(raw)
            entry["glb"] = base64.b64encode(gzip.compress(raw, 9, mtime=0)).decode()
        examples.append(entry)

    commit = subprocess.run(["git", "rev-parse", "--short", "HEAD"], cwd=ROOT,
                            capture_output=True, text=True).stdout.strip()
    manifest = SITE / ".data/manifest.json"
    manifest.parent.mkdir(parents=True, exist_ok=True)
    manifest.write_text(json.dumps({"commit": commit, "examples": examples}))
    print(f"wrote {manifest}", file=sys.stderr)
    if args.manifest_only:
        return 0

    npm = shutil.which("npm") or sys.exit("npm is required to build the page")
    if not (SITE / "node_modules").exists():
        subprocess.run([npm, "ci"], cwd=SITE, check=True)
    out_dir = SITE / ".data/dist"
    subprocess.run([npm, "run", "build", "--", "--outDir", str(out_dir), "--emptyOutDir"],
                   cwd=SITE, check=True, env={**os.environ, "EXAMPLES_MANIFEST": str(manifest)})
    args.output.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(out_dir / "index.html", args.output)
    failed = [(s.stem, e) for s, _, e in results if e]
    print(f"wrote {args.output} ({len(examples)} examples, "
          f"{args.output.stat().st_size / 1e6:.1f} MB)", file=sys.stderr)
    for name, err in failed:
        print(f"  no model for {name}: {err}", file=sys.stderr)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
