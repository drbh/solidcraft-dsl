#!/bin/sh
# Full feature verification. Missing engine capabilities fail instead of skipping tests.
set -eu
cd "$(dirname "$0")/.."
export SOLIDCRAFT_REQUIRE_FULL_SUPPORT=1
export SOLIDCRAFT_CLI="${SOLIDCRAFT_CLI:-solidcraft-cli}"
"$SOLIDCRAFT_CLI" --version
cargo test --locked --test compatibility -- --nocapture
cargo test --workspace --locked
