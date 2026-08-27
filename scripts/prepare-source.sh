#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK_DIR="${VIROS_WORK_DIR:-$ROOT_DIR/.work}"
SOURCE_DIR="$WORK_DIR/winlator-app"

UPSTREAM_APP_REPO="${WINLATOR_APP_REPO:-https://github.com/brunodev85/winlator-app.git}"
UPSTREAM_APP_SHA="${WINLATOR_APP_SHA:-c03f6ab558c6f94cbac6ec0c791b12f3428fbdf6}"

rm -rf "$SOURCE_DIR"
mkdir -p "$WORK_DIR"

echo "==> Cloning Winlator app source"
git clone --filter=blob:none --no-checkout "$UPSTREAM_APP_REPO" "$SOURCE_DIR"

git -C "$SOURCE_DIR" fetch --depth=1 origin "$UPSTREAM_APP_SHA"
git -C "$SOURCE_DIR" checkout --detach "$UPSTREAM_APP_SHA"

echo "==> Upstream app pinned at $(git -C "$SOURCE_DIR" rev-parse HEAD)"

if compgen -G "$ROOT_DIR/patches/*.patch" > /dev/null; then
  echo "==> Applying Viros patch stack"
  for patch in "$ROOT_DIR"/patches/*.patch; do
    echo "    $(basename "$patch")"
    git -C "$SOURCE_DIR" apply --index --3way "$patch"
  done
fi

if [[ -d "$ROOT_DIR/overlay" ]]; then
  echo "==> Applying source overlay"
  cp -a "$ROOT_DIR/overlay/." "$SOURCE_DIR/"
fi

echo "==> Prepared source: $SOURCE_DIR"
echo "Build with: cd '$SOURCE_DIR' && ./gradlew assembleDebug"
