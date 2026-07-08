#!/bin/sh
# gradlew shim.
#
# The Gradle wrapper jar (gradle/wrapper/gradle-wrapper.jar) is not
# committed because it's a binary. You have two easy options:
#
#   1. Open the project in Android Studio once — it will generate the
#      wrapper jar automatically during Gradle sync.
#
#   2. Run `gradle wrapper --gradle-version 8.11.1` from the project
#      root with a system Gradle (brew install gradle / apt install gradle).
#
# Once the jar exists, this script simply invokes it with the JVM you
# have configured.

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
