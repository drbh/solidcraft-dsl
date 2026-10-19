#!/bin/sh
# Install solidcraft-cli from a SolidCraft GitHub release into a directory.
#
#   ci/install-solidcraft-cli.sh latest  DIR     the newest release
#   ci/install-solidcraft-cli.sh 0.15.0  DIR     that version
#
# Downloads are checked against the release's SHA256SUMS.txt. Needs curl, and unzip (macOS)
# or tar (Linux). Prints the installed binary's path last.
set -eu

VERSION="${1:-latest}"
DIR="${2:-$HOME/.local/bin}"
REPO="${SOLIDCRAFT_REPO:-bherbruck/solidcraft}"
API="https://api.github.com/repos/$REPO/releases"

need() { command -v "$1" >/dev/null 2>&1 || { echo "error: $1 is required" >&2; exit 1; }; }
need curl

if [ "$VERSION" = latest ]; then
  TAG="$(curl -fsSL "$API/latest" | sed -n 's/.*"tag_name": *"\([^"]*\)".*/\1/p' | head -1)"
  [ -n "$TAG" ] || { echo "error: cannot find the latest release of $REPO" >&2; exit 1; }
else
  TAG="v${VERSION#v}"
fi
VERSION="${TAG#v}"
BASE="https://github.com/$REPO/releases/download/$TAG"
echo "solidcraft $VERSION from $REPO"

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
cd "$TMP"
curl -fsSL "$BASE/SHA256SUMS.txt" -o SHA256SUMS.txt

verify() {
  sum="$(grep -E " \*?$1\$" SHA256SUMS.txt | awk '{print $1}' | head -1)"
  [ -n "$sum" ] || { echo "error: $1 is not listed in SHA256SUMS.txt" >&2; exit 1; }
  if command -v sha256sum >/dev/null 2>&1; then
    actual="$(sha256sum "$1" | awk '{print $1}')"
  else
    actual="$(shasum -a 256 "$1" | awk '{print $1}')"
  fi
  [ "$actual" = "$sum" ] || { echo "error: checksum mismatch for $1" >&2; exit 1; }
}

mkdir -p "$DIR"
case "$(uname -s)" in
  Darwin)
    need unzip
    ASSET="solidcraft-cli-$VERSION-macos-universal.zip"
    curl -fsSL "$BASE/$ASSET" -o "$ASSET"
    verify "$ASSET"
    unzip -q "$ASSET"
    ;;
  Linux)
    need tar
    case "$(uname -m)" in
      x86_64 | amd64) arch=x86_64 ;;
      aarch64 | arm64) arch=aarch64 ;;
      *) echo "error: no Linux build for $(uname -m)" >&2; exit 1 ;;
    esac
    ASSET="solidcraft-$VERSION-linux-$arch.tar.gz"
    curl -fsSL "$BASE/$ASSET" -o "$ASSET"
    verify "$ASSET"
    tar -xzf "$ASSET"
    ;;
  *) echo "error: unsupported platform $(uname -s)" >&2; exit 1 ;;
esac

CLI="$(find . -type f -name solidcraft-cli | head -1)"
[ -n "$CLI" ] || { echo "error: solidcraft-cli not found in $ASSET" >&2; exit 1; }
install -m 755 "$CLI" "$DIR/solidcraft-cli"
"$DIR/solidcraft-cli" --version 2>/dev/null || true
echo "$DIR/solidcraft-cli"
