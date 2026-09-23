#!/usr/bin/env sh
# Gradle wrapper - compatible with classpath execution (no manifest required)
set -e
APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
DIR=$(dirname "$0")
if [ -f "$DIR/gradle/wrapper/gradle-wrapper.jar" ]; then
  exec java -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
else
  exec gradle "$@"
fi
