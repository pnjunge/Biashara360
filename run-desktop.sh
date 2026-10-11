#!/usr/bin/env bash
set -euo pipefail

# Use the system Java 17 runtime that matches the project target.
export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
export PATH="$JAVA_HOME/bin:$PATH"

# Strip environment variables injected by VS Code / Snap that break the native GTK/JVM runtime.
unset LD_LIBRARY_PATH
unset LD_PRELOAD
unset LIBGL_DRIVERS_PATH
unset GTK_PATH
unset GTK_IM_MODULE_FILE
unset GTK_EXE_PREFIX
unset GTK_MODULES
unset _JAVA_OPTIONS

# Ensure a desktop display is available when running locally.
export DISPLAY="${DISPLAY:-:0}"
export GDK_BACKEND="${GDK_BACKEND:-x11}"

cd "$(dirname "$0")"
./gradlew :desktopApp:run
