#!/usr/bin/env python3
"""Compare how long two engines take to build each example, and which commands got slower.

    ci/engine-timings.py OLD_CLI NEW_CLI [--json timings.json] [examples/foo.sc ...]

Each example is built with `solidcraft-dsl run <example> --timings` (which fits the script to the
engine, as `evaluate` does) on one engine, then the other, one at a time so the numbers don't
compete for the CPU. It prints a Markdown table of the build times, slowest regression first, and
for each example that got noticeably slower the commands that account for most of the change.
Run it from the repository root after `cargo build`. See docs/engine-performance.md.
"""
import glob
import json
import os
import subprocess
import sys
import time

TIMEOUT_S = 1500
DSL = "target/debug/solidcraft-dsl"


def build(example, cli):
    start = time.time()
    try:
        p = subprocess.run([DSL, "run", example, "--timings"], capture_output=True, text=True, timeout=TIMEOUT_S,
                           env=dict(os.environ, SOLIDCRAFT_CLI=cli))
    except subprocess.TimeoutExpired:
        return {"wall_s": time.time() - start, "error": f"timed out after {TIMEOUT_S} s"}
    out = p.stdout
    if p.returncode != 0 or "{" not in out:
        return {"wall_s": time.time() - start, "error": (p.stderr.strip().splitlines() or ["no output"])[-1][:200]}
    r = json.loads(out[out.index("{"):])
    return {"wall_s": time.time() - start, "total_ms": r["total_ms"],
            "commands": [(t["index"], t["command"], t["ms"]) for t in r["timings"]]}


def names(example):
    p = subprocess.run([DSL, "compile", example], capture_output=True, text=True)
    try:
        return [(c.get("params") or {}).get("name") for c in json.loads(p.stdout)["commands"]]
    except (ValueError, KeyError):
        return []


def main():
    args = sys.argv[1:]
    out = None
    if "--json" in args:
        i = args.index("--json")
        out = args[i + 1]
        del args[i:i + 2]
    if len(args) < 2:
        sys.exit(__doc__)
    old, new, examples = args[0], args[1], args[2:] or sorted(glob.glob("examples/*.sc"))
    version = lambda cli: subprocess.run([cli, "--version"], capture_output=True, text=True).stdout.split()[-1]
    vo, vn = version(old), version(new)
    rows = {}
    for e in examples:
        n = os.path.basename(e)[:-3]
        rows[n] = {"old": build(e, old), "new": build(e, new), "names": names(e)}
        o, w = rows[n]["old"], rows[n]["new"]
        print(f"{n}: {o.get('total_ms', o.get('error'))} -> {w.get('total_ms', w.get('error'))}", file=sys.stderr, flush=True)
        if out:
            json.dump({"old": vo, "new": vn, "examples": rows}, open(out, "w"))

    def ratio(r):
        a, b = r["old"].get("total_ms"), r["new"].get("total_ms")
        return b / a if a and b else 0

    print(f"| Example | {vo} | {vn} | Change |\n|---|---:|---:|---:|")
    for n, r in sorted(rows.items(), key=lambda kv: -ratio(kv[1])):
        cell = lambda b: f"{b['total_ms'] / 1000:.1f} s" if "total_ms" in b else b["error"]
        print(f"| {n} | {cell(r['old'])} | {cell(r['new'])} | {f'x{ratio(r):.2f}' if ratio(r) else ''} |")
    for n, r in sorted(rows.items(), key=lambda kv: -ratio(kv[1])):
        a, b = r["old"].get("commands"), r["new"].get("commands")
        if not a or not b or ratio(r) < 1.2 or r["new"]["total_ms"] < 2000:
            continue
        print(f"\n{n}, the commands that slowed it most ({vo} -> {vn}):")
        deltas = sorted(((y[2] - x[2], x) for x, y in zip(a, b) if x[1] == y[1]), reverse=True)[:5]
        for d, (i, cmd, ms) in deltas:
            label = r["names"][i] if i < len(r["names"]) and r["names"][i] else ""
            print(f"- step {i + 1} `{cmd}` {label}: {ms / 1000:.2f} s -> {(ms + d) / 1000:.2f} s")


if __name__ == "__main__":
    main()
