#!/usr/bin/env python3
"""Build every example and compile a single-page HTML gallery: source on the left, model on the right.

    ci/examples-site.py [-o build/examples.html] [-j 4] [--force] [--only 3 4 ...]

GLBs are built (and cached) exactly as ci/examples-glb.py does, in build/all-examples/<name>/.
For the page each one is cut down to positions only (the viewer rebuilds creased normals),
meshopt-compressed with gltfpack, gzipped and embedded, so the result is one self-contained HTML
file; that is about 20x smaller than the raw GLBs. A thumbnail of each is rendered with the page's
own viewer in headless Chrome. Both are cached in build/all-examples/_gallery/.
The page itself is a Svelte app in site/; this script writes its manifest and runs the Vite build.
Examples that fail to build still appear, with the error in place of the model.
"""
import argparse
import base64
import gzip
import hashlib
import urllib.error
import urllib.request
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

sys.dont_write_bytecode = True  # importing examples-glb.py would leave ci/__pycache__/
_spec = importlib.util.spec_from_file_location("examples_glb", ROOT / "ci/examples-glb.py")
glb_tools = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(glb_tools)


GALLERY = "_gallery"  # compressed GLBs and thumbnails, next to the per-example build folders


def view_glb(glb, work, name):
    """The page's copy of a GLB: normals dropped, then meshopt-compressed by gltfpack."""
    out = work / GALLERY / f"{name}.glb"
    if out.exists() and out.stat().st_mtime > glb.stat().st_mtime:
        return out
    out.parent.mkdir(parents=True, exist_ok=True)
    gltf, binary = glb_tools.read_glb(glb)
    for mesh in gltf.get("meshes", []):
        for prim in mesh["primitives"]:
            prim["attributes"].pop("NORMAL", None)
    bare = out.with_name(f"{name}.bare.glb")  # gltfpack goes by the extension
    glb_tools.write_glb(bare, gltf, binary)
    packed = subprocess.run([str(SITE / "node_modules/.bin/gltfpack"), "-i", str(bare), "-o",
                             str(out), "-cc"], capture_output=True, text=True)
    if packed.returncode != 0:
        print(f"  gltfpack failed for {name}, embedding it uncompressed: "
              f"{(packed.stderr or packed.stdout).strip()[:200]}", file=sys.stderr)
        shutil.copyfile(glb, out)
    bare.unlink(missing_ok=True)
    return out


# Playground builds slower than this are listed apart and not rebuilt on every edit.
SLOW_MS = 5000
TIMING_LIMIT_S = 20  # past this a build only counts as slow


def example_hash(source):
    """What a build's time depends on: the example and its <name>_parts / <name>_art folders."""
    digest = hashlib.sha256(source.read_bytes())
    for folder in sorted(source.parent.glob(f"{source.stem}_*")):
        for f in sorted(folder.rglob("*")) if folder.is_dir() else []:
            if f.is_file():
                digest.update(f.relative_to(source.parent).as_posix().encode() + b"\0" + f.read_bytes())
    return digest.hexdigest()[:16]


def load_timings(path):
    try:
        return json.loads(path.read_text())
    except (OSError, ValueError):
        return {}


def playground_timings(wasm, sources):
    """Each example's build time in the playground engine, re-measured when it changes."""
    path = wasm.with_name("timings.json")
    timings = load_timings(path)
    hashes = {s.stem: example_hash(s) for s in sources}
    stale = [n for n, h in hashes.items() if timings.get(n, {}).get("hash") != h]
    if stale:
        print(f"timing {len(stale)} examples in the playground engine", file=sys.stderr)
        subprocess.run(["node", "scripts/timings.mjs", str(wasm), str(ROOT / "examples"), str(path),
                        "--limit", str(TIMING_LIMIT_S), *stale], cwd=SITE, check=True)
        timings = load_timings(path)
        for name in stale:
            if name in timings:
                timings[name]["hash"] = hashes[name]
        path.write_text(json.dumps(timings, indent=1))
    return timings


def hashed(site, folder, name, data):
    """Write `data` as site/folder/<stem>.<hash>.<ext> and return its URL relative to the site."""
    stem, _, ext = name.partition(".")
    digest = hashlib.sha256(data).hexdigest()[:10]
    path = site / folder / f"{stem}.{digest}.{ext}"
    path.parent.mkdir(parents=True, exist_ok=True)
    if not path.exists():
        path.write_bytes(data)
    return f"{folder}/{path.name}"


HEADERS = """\
# Content-hashed files never change under the same name.
/m/*
  Cache-Control: public, max-age=31536000, immutable
/t/*
  Cache-Control: public, max-age=31536000, immutable
/e/*
  Cache-Control: public, max-age=31536000, immutable
"""


def write_site(site, write_manifest, npm, out_dir, manifest, engine, build_index):
    """The hosted copy: the same two pages, with their large data as separate files, and
    build-index.json, which lets the next build (`--reuse`) download what didn't change."""
    if site.exists():
        shutil.rmtree(site)
    site.mkdir(parents=True)
    write_manifest(site)
    subprocess.run([npm, "run", "build", "--", "--outDir", str(out_dir), "--emptyOutDir"],
                   cwd=SITE, check=True, stdout=subprocess.DEVNULL,
                   env={**os.environ, "EXAMPLES_MANIFEST": str(manifest)})
    shutil.copyfile(out_dir / "index.html", site / "index.html")
    engine_url = None
    if engine:
        engine_url = hashed(site, "e", "engine.wasm.gz", engine.read_bytes())
        pg_dir = SITE / ".data/playground"
        subprocess.run([npm, "run", "build", "--", "--outDir", str(pg_dir), "--emptyOutDir"],
                       cwd=SITE, check=True, stdout=subprocess.DEVNULL,
                       env={**os.environ, "PAGE": "playground", "PLAYGROUND_ENGINE_URL": engine_url,
                            "PLAYGROUND_GALLERY": "./",
                            "PLAYGROUND_TIMINGS": str(engine.with_name("timings.json"))})
        shutil.copyfile(pg_dir / "playground.html", site / "playground.html")
    (site / "_headers").write_text(HEADERS)
    (site / "build-index.json").write_text(json.dumps(build_index(engine_url), indent=1))
    files = [p for p in site.rglob("*") if p.is_file()]
    largest = max(files, key=lambda p: p.stat().st_size)
    print(f"wrote {site} ({len(files)} files, {sum(p.stat().st_size for p in files) / 1e6:.1f} MB; "
          f"largest {largest.relative_to(site)} at {largest.stat().st_size / 1e6:.1f} MB)",
          file=sys.stderr)
    if largest.stat().st_size > 25 * 1024 * 1024:
        sys.exit(f"{largest} is over Cloudflare Pages' 25 MiB file limit")


def fetch(url, timeout=120):
    with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": "examples-site"}),
                                timeout=timeout) as response:
        return response.read()


def fetch_index(base):
    """A published site's build-index.json, or {} (and a warning) when there is none."""
    try:
        index = json.loads(fetch(f"{base.rstrip('/')}/build-index.json", timeout=30))
        print(f"reusing outputs from {base} ({len(index.get('examples', {}))} examples)", file=sys.stderr)
        return index
    except (urllib.error.URLError, OSError, ValueError) as error:
        print(f"warning: no build index at {base} ({error}); building everything", file=sys.stderr)
        return {}


def ungzip(data):
    return gzip.decompress(data) if data[:2] == b"\x1f\x8b" else data


def download_example(base, entry, gallery, name):
    """Fetch a published model (and thumbnail) into the gallery cache; return its stats."""
    gallery.mkdir(parents=True, exist_ok=True)
    root = base.rstrip("/")
    (gallery / f"{name}.glb").write_bytes(ungzip(fetch(f"{root}/{entry['glb']}")))
    if entry.get("thumb"):  # after the model, so the thumbnail isn't older than it
        (gallery / f"{name}.webp").write_bytes(fetch(f"{root}/{entry['thumb']}"))
    return {"triangles": entry["triangles"], "size": entry["size"]}


def playground_engine_key():
    """What the playground engine depends on: its crate, the compiler and the engine version."""
    digest = hashlib.sha256(glb_tools.engine_version().encode())
    paths = [*ROOT.glob("site/playground/engine/**/*"), *ROOT.glob("src/**/*.rs"),
             *ROOT.glob("crates/**/*.rs"), ROOT / "Cargo.lock", ROOT / "flake.nix",
             ROOT / "ci/playground-engine.py"]
    for path in sorted(p for p in paths if p.is_file()):
        digest.update(path.relative_to(ROOT).as_posix().encode() + b"\0" + path.read_bytes())
    return digest.hexdigest()


def prepare_engine(engine, key, published, base):
    """build/playground/engine.wasm(.gz) for `key`: kept, downloaded, or built (slowest)."""
    stamp = engine.with_name("engine.inputs")
    if engine.exists() and stamp.exists() and stamp.read_text().strip() == key:
        return
    if published and published.get("inputs") == key:
        print("reusing the published playground engine", file=sys.stderr)
        packed = fetch(f"{base.rstrip('/')}/{published['url']}", timeout=600)
        engine.parent.mkdir(parents=True, exist_ok=True)
        engine.write_bytes(packed)
        engine.with_suffix("").write_bytes(ungzip(packed))  # the timing pass runs the raw module
    else:
        subprocess.run([sys.executable, str(ROOT / "ci/playground-engine.py"),
                        "-o", str(engine.with_suffix(""))], check=True)
    stamp.write_text(key)


def adopt_timings(path, published, sources):
    """Take published timings whose example hash matches, so only changed examples are timed."""
    if not published:
        return
    timings = load_timings(path)
    hashes = {s.stem: example_hash(s) for s in sources}
    taken = 0
    for name, entry in published.items():
        if hashes.get(name) and entry.get("hash") == hashes[name] and timings.get(name, {}).get("hash") != hashes[name]:
            timings[name] = entry
            taken += 1
    if taken:
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(timings, indent=1))


ACRONYMS = {"rc", "pcb", "esp32", "led", "usb", "cd", "tft"}
SPELLINGS = {"ipod": "iPod", "macbook": "MacBook"}


def describe(source):
    """Title and one-line summary from the file name and the leading comment block."""
    stem = source.stem
    match = re.match(r"(\d+)_(.*)", stem)
    number, rest = (match.group(1), match.group(2)) if match else (None, stem)
    words = rest.split("_")
    title = " ".join(SPELLINGS[w] if w in SPELLINGS else w.upper() if w in ACRONYMS or re.fullmatch(r"[a-z]{2,}\d+", w) else w for w in words)
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
    parser.add_argument("--playground", action="store_true",
                        help="also build the engine (ci/playground-engine.py) and playground.html")
    parser.add_argument("--site", type=Path,
                        help="also write a hosted copy here: index.html and playground.html that "
                             "fetch content-hashed models, thumbnails and the engine from beside "
                             "them (for Cloudflare Pages, whose files are capped at 25 MiB)")
    parser.add_argument("--reuse", metavar="URL",
                        help="a published site (e.g. https://solidcraft-dsl.dholtz.com): download the "
                             "models, thumbnails, timings and engine its build-index.json already has for "
                             "the same inputs instead of building them")
    parser.add_argument("--no-thumbs", action="store_true",
                        help="don't render missing thumbnails (needs Chrome)")
    args = parser.parse_args()
    args.work = args.work.resolve()  # the thumbnail pass runs from site/

    if not args.bin.exists():
        subprocess.run(["cargo", "build", "--quiet"], cwd=ROOT, check=True)
    def order(p):  # numbered examples by number (so 11 comes before 100), then the rest by name
        match = re.match(r"(\d+)_", p.stem)
        return (0, int(match.group(1)), p.stem) if match else (1, 0, p.stem)

    sources = sorted((p for p in (ROOT / "examples").glob("*.sc")
                      if not args.only or any(p.stem.startswith(o) for o in args.only)), key=order)

    # Each example's key: the hash of the command script it compiles to (see examples-glb.py).
    with ThreadPoolExecutor(args.jobs) as pool:
        keys = dict(zip((s.stem for s in sources),
                        pool.map(lambda s: glb_tools.inputs_hash(s, args.bin), sources)))
    index = fetch_index(args.reuse) if args.reuse else {}
    published = index.get("examples", {})
    reused = {}  # name -> stats of a model downloaded from the published site

    def job(source):
        name = source.stem
        try:
            out = args.work / name
            if not args.force and (glb := glb_tools.cached(source, out, args.bin, keys[name])):
                how = "cached"
            elif not args.force and published.get(name, {}).get("inputs") == keys[name]:
                reused[name] = download_example(args.reuse, published[name], args.work / GALLERY, name)
                print(f"  reused {name}", file=sys.stderr)
                return source, None, None
            else:
                glb, how = glb_tools.build(source, out, args.bin, args.force, args.timeout, keys[name])
            print(f"  {how:6} {name}", file=sys.stderr)
            return source, glb, None
        except Exception as error:  # noqa: BLE001 - report and keep going
            print(f"  FAILED {name}: {error}", file=sys.stderr)
            return source, None, str(error)

    print(f"building {len(sources)} examples with {args.jobs} jobs", file=sys.stderr)
    with ThreadPoolExecutor(args.jobs) as pool:
        results = list(pool.map(job, sources))
    counts = {}
    for source, glb, error in results:
        kind = "failed" if error else "reused" if source.stem in reused else "local"
        counts[kind] = counts.get(kind, 0) + 1
    print(f"examples: {counts}", file=sys.stderr)

    npm = shutil.which("npm") or sys.exit("npm is required to build the page")
    if not (SITE / "node_modules").exists():
        subprocess.run([npm, "ci"], cwd=SITE, check=True)

    views = {}
    for source, glb, error in results:
        if glb:
            views[source.stem] = view_glb(glb, args.work, source.stem)
        elif source.stem in reused:
            views[source.stem] = args.work / GALLERY / f"{source.stem}.glb"
    thumbs = args.work / GALLERY

    def stale_thumbs():
        return [name for name, view in views.items()
                if not (thumbs / f"{name}.webp").exists()
                or (thumbs / f"{name}.webp").stat().st_mtime < view.stat().st_mtime]

    commit = subprocess.run(["git", "rev-parse", "--short", "HEAD"], cwd=ROOT,
                            capture_output=True, text=True).stdout.strip()
    manifest = SITE / ".data/manifest.json"
    out_dir = SITE / ".data/dist"

    site_index = {}  # what the hosted copy publishes in build-index.json, for --reuse

    def write_manifest(site=None):
        """The page's data. With `site`, models and thumbnails go there as files the page
        fetches (named by content hash, so they can be cached forever) instead of inline."""
        examples = []
        for source, glb, error in results:
            number, title, summary, text = describe(source)
            entry = {"name": source.stem, "file": f"examples/{source.name}", "number": number,
                     "title": title, "summary": summary, "source": text, "error": error}
            if glb or source.stem in reused:
                view = views[source.stem].read_bytes()
                known = reused.get(source.stem)
                entry["triangles"], entry["size"] = (known["triangles"], known["size"]) if known else stats(glb)
                entry["glbBytes"] = len(view)
                packed = gzip.compress(view, 9, mtime=0)
                thumb = thumbs / f"{source.stem}.webp"
                if site:
                    entry["glbUrl"] = hashed(site, "m", f"{source.stem}.glb.gz", packed)
                    if thumb.exists():
                        entry["thumbUrl"] = hashed(site, "t", f"{source.stem}.webp", thumb.read_bytes())
                    site_index[source.stem] = {"inputs": keys[source.stem], "glb": entry["glbUrl"],
                                               "thumb": entry.get("thumbUrl"),
                                               "triangles": entry["triangles"], "size": entry["size"]}
                else:
                    entry["glb"] = base64.b64encode(packed).decode()
                    if thumb.exists():
                        entry["thumb"] = base64.b64encode(thumb.read_bytes()).decode()
            if source.stem in timings:
                entry["playground_ms"] = timings[source.stem]["ms"]
                entry["playground_slow"] = timings[source.stem]["ms"] > SLOW_MS
                entry["playground_timed_out"] = bool(timings[source.stem].get("timedOut"))
            examples.append(entry)
        manifest.parent.mkdir(parents=True, exist_ok=True)
        manifest.write_text(json.dumps({"commit": commit, "examples": examples,
                                        "playground": playground}))
        print(f"wrote {manifest}", file=sys.stderr)
        return examples

    def build_page():
        subprocess.run([npm, "run", "build", "--", "--outDir", str(out_dir), "--emptyOutDir"],
                       cwd=SITE, check=True, stdout=subprocess.DEVNULL,
                       env={**os.environ, "EXAMPLES_MANIFEST": str(manifest)})

    engine = ROOT / "build/playground/engine.wasm.gz"
    engine_key = playground_engine_key()
    if args.playground:
        prepare_engine(engine, engine_key, index.get("engine"), args.reuse)
    # The gallery links to the playground when there is an engine to build it with.
    playground = engine.exists()
    if args.playground:
        adopt_timings(engine.with_name("timings.json"), index.get("timings", {}), sources)
    timings = playground_timings(engine.with_suffix(""), sources) if args.playground else \
        load_timings(engine.with_name("timings.json"))

    examples = write_manifest()
    if args.manifest_only:
        return 0
    build_page()
    missing = stale_thumbs()
    if missing and not args.no_thumbs:
        # Render with the page just built, then build it again with the thumbnails in.
        print(f"rendering {len(missing)} thumbnails", file=sys.stderr)
        subprocess.run(["node", "scripts/thumbs.mjs", str(out_dir / "index.html"), str(thumbs),
                        *missing], cwd=SITE)
        examples = write_manifest()
        build_page()

    args.output.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(out_dir / "index.html", args.output)
    if playground:
        # playground.html sits next to the gallery, which links to it by that name.
        pg_dir = SITE / ".data/playground"
        subprocess.run([npm, "run", "build", "--", "--outDir", str(pg_dir), "--emptyOutDir"],
                       cwd=SITE, check=True, stdout=subprocess.DEVNULL,
                       env={**os.environ, "PAGE": "playground", "PLAYGROUND_ENGINE": str(engine),
                            "PLAYGROUND_TIMINGS": str(engine.with_name("timings.json"))})
        page = args.output.with_name("playground.html")
        shutil.copyfile(pg_dir / "playground.html", page)
        print(f"wrote {page} ({page.stat().st_size / 1e6:.1f} MB)", file=sys.stderr)
    if args.site:
        write_site(args.site.resolve(), write_manifest, npm, out_dir, manifest,
                   engine if playground else None,
                   lambda engine_url: {"version": 1, "examples": site_index, "timings": timings,
                                       "engine": {"inputs": engine_key, "url": engine_url}
                                       if engine_url else None})
    failed = [(s.stem, e) for s, _, e in results if e]
    no_thumb = [e["name"] for e in examples if e.get("glb") and not e.get("thumb")]
    print(f"wrote {args.output} ({len(examples)} examples, "
          f"{args.output.stat().st_size / 1e6:.1f} MB)", file=sys.stderr)
    if no_thumb:
        print(f"  no thumbnail for {len(no_thumb)}: {' '.join(no_thumb[:10])}", file=sys.stderr)
    for name, err in failed:
        print(f"  no model for {name}: {err}", file=sys.stderr)
    return 1 if failed else 0

if __name__ == "__main__":
    sys.exit(main())
