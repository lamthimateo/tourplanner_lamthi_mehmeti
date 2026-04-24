#!/usr/bin/env bash
# Start the Spring Boot backend.
#
# - Loads backend/.env if present, so settings like ORS_API_KEY end up in the
#   process environment before Spring reads them.
# - Auto-detects a Java 17 runtime (required by the build) on macOS and Linux.
#   The Maven Wrapper (`mvnw`) relies on JAVA_HOME at runtime; without it the
#   user's default "java" may be a newer version that fails on some libraries.

set -euo pipefail
cd "$(dirname "$0")/backend"

# ---- JAVA_HOME detection (Java 17) -----------------------------------------
if [ -z "${JAVA_HOME:-}" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "17'; then
  if command -v /usr/libexec/java_home >/dev/null 2>&1; then
    # macOS: ask the JDK registry for a Java 17 installation.
    if JAVA17_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null)"; then
      export JAVA_HOME="$JAVA17_HOME"
    fi
  fi

  # Fallback: scan common Linux install paths for a Java 17 JDK.
  if [ -z "${JAVA_HOME:-}" ] || ! "$JAVA_HOME/bin/java" -version 2>&1 | grep -q 'version "17'; then
    for candidate in /usr/lib/jvm/java-17-openjdk* /usr/lib/jvm/temurin-17-jdk*; do
      if [ -d "$candidate" ]; then
        export JAVA_HOME="$candidate"
        break
      fi
    done
  fi
fi

if [ -n "${JAVA_HOME:-}" ]; then
  export PATH="$JAVA_HOME/bin:$PATH"
  echo "Using JAVA_HOME=$JAVA_HOME"
else
  echo "WARNING: could not locate Java 17. The build may fail if the default java is incompatible." >&2
fi

# ---- Load backend/.env -----------------------------------------------------
if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

./mvnw clean spring-boot:run
