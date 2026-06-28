#!/usr/bin/env bash
# ------------------------------------------------------------------
# build-with-java17.sh – build the Android "screen-lock" project with JDK 17
#
# Usage:  ./build-with-java17.sh
#
# Requirements:
#   • JDK 11 or newer installed and reachable via the environment variable
#     $JAVA17_HOME (e.g. /Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home)
#   • The project’s Gradle version is declared in the root build.gradle.kts
#
# This script works even when ~/.gradle/gradle.properties forces Java 8,
# because we pass the overriding property on the command line.
# ------------------------------------------------------------------

# ---- 1️⃣  Verify $JAVA17_HOME ------------------------------------------------
if [[ -z "$JAVA17_HOME" ]]; then
  echo "❌ ERROR: JAVA17_HOME is not set."
  echo "   Export the path to a JDK 11+ and try again, e.g.:"
  echo "   export JAVA17_HOME=\$(/usr/libexec/java_home -v 17)"
  exit 1
fi

# ---- 2️⃣  Set JAVA_HOME for any subprocesses --------------------------------
export JAVA_HOME="$JAVA17_HOME"
echo "✅ Using Java home: $JAVA_HOME"

# ---- 3️⃣  Generate Gradle wrapper if missing ---------------------------------
if [[ ! -f "./gradlew" ]]; then
  echo "⚙️  Gradle wrapper not found – generating one with Java $JAVA17_HOME..."
  # Generate the wrapper using the global gradle with Java 17 override
  # This creates gradlew script that will use the correct Gradle version
  gradle wrapper \
    --gradle-version=8.11.1 \
    -Dorg.gradle.java.home="$JAVA17_HOME"
fi

# ---- 4️⃣  Run the build using the wrapper (which uses Java 17) -----------------
echo "🚀 Starting clean build with JDK 17..."
export GRADLE_OPTS="-Xmx4096m -XX:MaxMetaspaceSize=512m"
./gradlew clean build -Dorg.gradle.java.home="$JAVA17_HOME" --no-daemon --warning-mode all
BUILD_STATUS=$?

if [[ $BUILD_STATUS -eq 0 ]]; then
  echo "🎉 Build succeeded!"
else
  echo "❌ Build failed (exit code $BUILD_STATUS)."
fi

exit $BUILD_STATUS
