#!/usr/bin/env python3
"""Check that every command an engine lists reaches it through the `engine` statement.

    ci/engine-sweep.py [SOLIDCRAFT_CLI]

1. Every command id from `solidcraft-cli commands` is compiled from `engine <id> {}` and must come
   out as exactly that command.
2. Every solid, surface, construct and sheet command is run (after a box) with empty parameters,
   and every sketch command inside a sketch block. The engine must recognise each one: refusing
   the empty parameters is expected; "unknown command" is a failure.

Other families (settings, config, file, render, view, ...) are only compiled, not run: some act
outside the design. Run from the repository root after `cargo build`.
"""
import collections
import json
import os
import subprocess
import sys
import tempfile

DSL = "target/debug/solidcraft-dsl"


def main():
    cli = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("SOLIDCRAFT_CLI", "solidcraft-cli")
    ids = [c["id"] for c in json.loads(subprocess.run([cli, "commands"], capture_output=True, text=True, check=True).stdout)]
    tmp = tempfile.mkdtemp()
    src = os.path.join(tmp, "s.sc")
    failed = []
    for i in ids:
        open(src, "w").write(f"design S\nengine {i} {{}}\n")
        p = subprocess.run([DSL, "compile", src], capture_output=True, text=True)
        try:
            if not any(c["command"] == i for c in json.loads(p.stdout)["commands"]):
                failed.append(f"{i}: not in the compiled script")
        except ValueError:
            failed.append(f"{i}: {p.stderr.strip()[:100]}")
    print(f"compiled: {len(ids) - len(failed)} of {len(ids)} pass through unchanged")
    env = dict(os.environ, SOLIDCRAFT_CLI=cli)
    counts = collections.Counter()
    for i in ids:
        if i.startswith(("solid.", "surface.", "construct.", "sheet.")):
            body = f"design S\nbox B size 10 10 10 -> body B\nengine {i} {{}}\n"
        elif i.startswith("sketch."):
            body = f"design S\nsketch K on XY {{\n line a = (0,0) (10,0)\n engine {i} {{}}\n}}\n"
        else:
            continue
        open(src, "w").write(body)
        p = subprocess.run([DSL, "run", src], capture_output=True, text=True, timeout=120, env=env)
        out = (p.stdout + p.stderr).lower()
        if "unknown command" in out or "no such command" in out:
            failed.append(f"{i}: the engine doesn't know it")
            counts["unknown"] += 1
        else:
            counts["ran" if p.returncode == 0 else "recognised, parameters refused"] += 1
    print(f"run: {dict(counts)}")
    for f in failed:
        print("FAILED", f)
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
