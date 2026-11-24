#!/usr/bin/env sh
# Lightweight Gradle launcher. It downloads the pinned Gradle distribution when needed.
set -eu
APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
GRADLE_VERSION=8.9
GRADLE_USER_HOME=${GRADLE_USER_HOME:-"$HOME/.gradle"}
DIST_DIR="$GRADLE_USER_HOME/wrapper/custom/gradle-$GRADLE_VERSION"
ZIP_DIR="$GRADLE_USER_HOME/wrapper/dists/custom-gradle-$GRADLE_VERSION"
GRADLE_BIN="$DIST_DIR/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$ZIP_DIR" "$(dirname "$DIST_DIR")"
  ZIP_FILE="$ZIP_DIR/gradle-$GRADLE_VERSION-bin.zip"
  if [ ! -f "$ZIP_FILE" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    if command -v curl >/dev/null 2>&1; then
      curl -fL --retry 3 -o "$ZIP_FILE" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    elif command -v wget >/dev/null 2>&1; then
      wget -O "$ZIP_FILE" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
    else
      echo "curl or wget is required to download Gradle." >&2
      exit 1
    fi
  fi
  TMP_DIR="$ZIP_DIR/extract"
  rm -rf "$TMP_DIR"
  mkdir -p "$TMP_DIR"
  unzip -q "$ZIP_FILE" -d "$TMP_DIR"
  rm -rf "$DIST_DIR"
  mv "$TMP_DIR/gradle-$GRADLE_VERSION" "$DIST_DIR"
  rm -rf "$TMP_DIR"
fi

exec "$GRADLE_BIN" -p "$APP_HOME" "$@"
