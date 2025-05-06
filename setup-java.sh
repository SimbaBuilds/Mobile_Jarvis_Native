#!/bin/bash

# Set JAVA_HOME to JDK 17
export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"

# Set Android SDK home
export ANDROID_HOME="$HOME/Library/Android/sdk"
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$PATH"

# Verify Java version
java -version

echo "JDK 17 is now active for this terminal session"
echo "ANDROID_HOME is set to: $ANDROID_HOME"
echo "To use this setup in a new terminal, run: source setup-java.sh"
