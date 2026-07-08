#!/bin/sh
# gradlew shim.
#
# The Gradle wrapper jar (gradle/wrapper/gradle-wrapper.jar) IS
# committed in this repo (matching the standard Android project layout).
# On first run it will download the Gradle 8.11.1 distribution itself
# into ~/.gradle/wrapper/dists/, then cache it for all subsequent
# builds. You only need a JDK 17 on your machine.

set -e
DIR=$(cd "$(dirname "$0")" && pwd)
CLASSPATH="$DIR/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$CLASSPATH" ]; then
  cat <<EOF >&2
ERROR: gradle-wrapper.jar not found.

Fix one of these:
  - Open the project in Android Studio and let it sync (recommended).
  - Or run:  gradle wrapper --gradle-version 8.11.1
    (requires 'gradle' on your PATH; e.g. \`brew install gradle\`).
EOF
  exit 1
fi

if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA="$JAVA_HOME/bin/java"
else
  JAVA=$(command -v java || true)
fi
if [ -z "$JAVA" ]; then
  echo "ERROR: Java 17+ is required. Set JAVA_HOME or install OpenJDK 17." >&2
  exit 1
fi

exec "$JAVA" -classpath "$CLASSPATH" \
  -Dorg.gradle.appname=gradlew \
  org.gradle.wrapper.GradleWrapperMain "$@"
