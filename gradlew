#!/bin/sh
# Gradle wrapper script for Unix

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")

# Determine the Java command
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Determine the Gradle home from the wrapper properties
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
WRAPPER_PROPERTIES="$SCRIPT_DIR/gradle/wrapper/gradle-wrapper.properties"

# Read distribution URL
DIST_URL=$(grep "distributionUrl" "$WRAPPER_PROPERTIES" | sed 's/distributionUrl=//' | sed 's/\\//g' | tr -d '\r')
DIST_NAME=$(basename "$DIST_URL" .zip)
DIST_DIR="$GRADLE_USER_HOME/wrapper/dists/$DIST_NAME"
GRADLE_HOME="$DIST_DIR/$DIST_NAME"

# Download if not present
if [ ! -d "$GRADLE_HOME" ]; then
    echo "Downloading Gradle $DIST_NAME ..."
    mkdir -p "$DIST_DIR"
    TMP_ZIP="$DIST_DIR/gradle.zip"
    if command -v curl > /dev/null 2>&1; then
        curl -L -o "$TMP_ZIP" "$DIST_URL"
    elif command -v wget > /dev/null 2>&1; then
        wget -O "$TMP_ZIP" "$DIST_URL"
    else
        echo "Error: curl or wget required to download Gradle"
        exit 1
    fi
    unzip -q "$TMP_ZIP" -d "$DIST_DIR"
    rm "$TMP_ZIP"
fi

# Set ANDROID_HOME if not set
if [ -z "$ANDROID_HOME" ]; then
    export ANDROID_HOME="$HOME/android-sdk"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
