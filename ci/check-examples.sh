#!/bin/sh
# Build every checked-in model and its authored print recipe; keep review reports.
# Extended check recipes can be replayed separately with observe --requests.
set -eu
cd "$(dirname "$0")/.."
OUTPUT="${1:-$PWD/build/example-compatibility}"
export SOLIDCRAFT_REQUIRE_FULL_SUPPORT=1
export SOLIDCRAFT_CLI="${SOLIDCRAFT_CLI:-solidcraft-cli}"
cargo test --locked --test compatibility
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
