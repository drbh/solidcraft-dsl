#!/usr/bin/env python3
"""Build the playground's engine: solidcraft-dsl plus the SolidCraft engine, as one WASI module.

    ci/playground-engine.py [-o build/playground/engine.wasm] [--src build/solidcraft-src]

The engine is checked out at the version flake.nix pins, and site/playground/engine is copied
into that checkout as apps/sc-playground, since it only builds as a member of the engine's
workspace. It is compiled for wasm32-wasip1 with the newest installed toolchain the engine
accepts, and its custom sections (debug info, names) are stripped. Writes the module and a
gzipped copy next to it. Cargo's incremental build makes reruns cheap.
"""
import argparse
import gzip
import re
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CRATE = ROOT / "site/playground/engine"
REPO = "https://github.com/tangency-labs/solidcraft.git"
TARGET = "wasm32-wasip1"


def engine_version():
    match = re.search(r'version = "(\d+\.\d+\.\d+)"', (ROOT / "flake.nix").read_text())
    if not match:
        sys.exit("can't find the pinned engine version in flake.nix")
    return match.group(1)


def checkout(src, version):
    """The engine's sources at tag v<version> in `src`. Works in place, so a restored build
    cache (src/target) survives, and switching versions only fetches the new tag."""
    tag = f"v{version}"
    marker = src / ".playground-tag"
    if (src / ".git").exists() and marker.exists() and marker.read_text().strip() == tag:
        return src
    src.mkdir(parents=True, exist_ok=True)
    git = lambda *a: subprocess.run(["git", *a], cwd=src, check=True, capture_output=True)
    if not (src / ".git").exists():
        git("init", "-q")
        git("remote", "add", "origin", REPO)
    git("fetch", "-q", "--depth", "1", "origin", "tag", tag)
    git("checkout", "-q", "-f", tag)
    marker.write_text(tag)
    return src


def version_tuple(text):
    return tuple(int(n) for n in re.match(r"(\d+)\.(\d+)(?:\.(\d+))?", text).groups(default="0"))


def toolchain(src):
    """The newest installed stable toolchain at or above the engine's rust-version."""
    need = re.search(r'rust-version = "([\d.]+)"', (src / "Cargo.toml").read_text())
    need = version_tuple(need.group(1)) if need else (0, 0, 0)
    listed = subprocess.run(["rustup", "toolchain", "list"], capture_output=True, text=True,
                            check=True).stdout.split()
    candidates = []
    for name in listed:
        if re.match(r"\d+\.\d+", name):
            candidates.append((version_tuple(name), name.split("-")[0]))
        elif name.startswith("stable"):
            out = subprocess.run(["rustc", "+stable", "--version"], capture_output=True, text=True)
            if out.returncode == 0:
                candidates.append((version_tuple(out.stdout.split()[1]), "stable"))
    ok = sorted(c for c in candidates if c[0] >= need)
    if not ok:
        sys.exit(f"the engine needs Rust {'.'.join(map(str, need))} or newer "
                 f"(rustup toolchain install {'.'.join(map(str, need))})")
    name = ok[-1][1]
    subprocess.run(["rustup", "target", "add", "--toolchain", name, TARGET], check=True,
                   capture_output=True)
    return name


def install_crate(src):
    dest = src / "apps/sc-playground"
    if dest.exists():
        shutil.rmtree(dest)
    shutil.copytree(CRATE, dest, ignore=shutil.ignore_patterns("Cargo.toml.in", "target"))
    manifest = (CRATE / "Cargo.toml.in").read_text().replace("@DSL_PATH@", str(ROOT))
    (dest / "Cargo.toml").write_text(manifest)


def strip_custom_sections(data):
    """Drop every custom section (id 0): debug info and the name section."""
    def leb(at):
        value = shift = 0
        while True:
            byte = data[at]
            at += 1
            value |= (byte & 0x7F) << shift
            shift += 7
            if byte < 0x80:
                return value, at
    if data[:4] != b"\0asm":
        raise ValueError("not a wasm module")
    out = bytearray(data[:8])
    at = 8
    while at < len(data):
        start = at
        section = data[at]
        size, at = leb(at + 1)
        end = at + size
        if section != 0:
            out += data[start:end]
        at = end
    return bytes(out)


def main():
    parser = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    parser.add_argument("-o", "--output", type=Path, default=ROOT / "build/playground/engine.wasm")
    parser.add_argument("--src", type=Path, default=ROOT / "build/solidcraft-src",
                        help="where to check the engine out")
    args = parser.parse_args()

    version = engine_version()
    src = checkout(args.src.resolve(), version)
    install_crate(src)
    tc = toolchain(src)
    print(f"building the playground engine: SolidCraft {version}, rust {tc}, {TARGET}",
          file=sys.stderr)
    subprocess.run(["cargo", f"+{tc}", "build", "--release", "--target", TARGET,
                    "-p", "sc-playground"], cwd=src, check=True)
    built = src / "target" / TARGET / "release/sc-playground.wasm"
    stripped = strip_custom_sections(built.read_bytes())
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_bytes(stripped)
    packed = gzip.compress(stripped, 9, mtime=0)
    args.output.with_suffix(".wasm.gz").write_bytes(packed)
    print(f"wrote {args.output} ({len(stripped) / 1e6:.1f} MB, {len(packed) / 1e6:.1f} MB gzipped)",
          file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
