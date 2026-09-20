#!/usr/bin/env sh
# Minimal Gradle wrapper fallback - uses system gradle if wrapper jar missing
set -e
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
DIR=$(dirname "$0")
if [ -f "$DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -jar "$DIR/gradle/wrapper/gradle-wrapper.jar" "$@"
else
  exec gradle "$@"
fi
