#!/usr/bin/env bash
# ARS Plugin Builder — build (if needed) and run.
set -e
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Resolve the runnable jar by glob so a version bump never breaks this script.
JAR="$(ls -t target/ars-plugin-builder-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$' | head -n1)"
if [ -z "$JAR" ]; then
    echo "Jar missing; building…"
    mvn -q clean package -DskipTests
    JAR="$(ls -t target/ars-plugin-builder-*.jar 2>/dev/null | grep -Ev -- '-(sources|javadoc)\.jar$' | head -n1)"
fi

echo "Starting ARS Plugin Builder ($(basename "$JAR"))"
exec java -jar "$JAR" "$@"
