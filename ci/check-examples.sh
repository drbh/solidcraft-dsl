#!/bin/sh
# Build every checked-in model and its authored print recipe; keep review reports.
# Extended check recipes can be replayed separately with observe --requests.
# Runs on any engine: assemblies fall back to the engine's own occurrence names where it can't
# take authored ones (docs/assemblies.md#engine-compatibility).
set -eu
cd "$(dirname "$0")/.."
OUTPUT="${1:-$PWD/build/example-compatibility}"
export SOLIDCRAFT_CLI="${SOLIDCRAFT_CLI:-solidcraft-cli}"
cargo build --locked
status=0
for source in examples/*.sc; do
  name="$(basename "$source" .sc)"
  set -- target/debug/solidcraft-dsl evaluate "$source" --output-dir "$OUTPUT/$name" --no-previews
  recipe="${source%.sc}.print.json"
  if [ -f "$recipe" ]; then
    set -- "$@" --requests "$recipe"
  fi
  "$@" || status=1
done
exit "$status"
