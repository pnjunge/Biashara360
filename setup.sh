#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# Biashara360 — First-time setup (run from the b360-complete/ root directory)
# bash setup.sh
# ─────────────────────────────────────────────────────────────────────────────
set -e
echo "🔧 Biashara360 Project Setup"
echo "─────────────────────────────"

# Check Java
if ! command -v java &>/dev/null; then
  echo "❌ Java not found. Install Java 25+:"
  echo "   Ubuntu: sudo apt install openjdk-25-jdk"
  echo "   macOS:  brew install openjdk@25"
  exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | awk -F'"' '/version/{split($2, version, "."); print version[1]; exit}')
if (( JAVA_VERSION < 25 )); then
  echo "❌ Java 25 or newer is required; found Java $JAVA_VERSION."
  exit 1
fi
echo "✅ Java $JAVA_VERSION found"

# Download gradle-wrapper.jar for ROOT and BACKEND
download_wrapper_jar() {
  local TARGET_DIR="$1"
  local JAR="$TARGET_DIR/gradle/wrapper/gradle-wrapper.jar"
  if [ -f "$JAR" ]; then
    echo "✅ $TARGET_DIR/gradle/wrapper/gradle-wrapper.jar already exists"
    return
  fi
  echo "⬇️  Downloading gradle-wrapper.jar for $TARGET_DIR..."
  mkdir -p "$TARGET_DIR/gradle/wrapper"
  TMP=$(mktemp /tmp/gradle-XXXX.zip)
  curl -fsSL "https://services.gradle.org/distributions/gradle-9.6.0-bin.zip" -o "$TMP"
  unzip -j "$TMP" "*/gradle-wrapper.jar" -d "$TARGET_DIR/gradle/wrapper/"
  rm "$TMP"
  echo "✅ Done: $JAR"
}

download_wrapper_jar "."
download_wrapper_jar "backend"

chmod +x gradlew backend/gradlew

# Web app .env
if [ ! -f "webApp/.env" ]; then
  echo 'VITE_API_BASE_URL=http://localhost:8080/v1' > webApp/.env
  echo "✅ webApp/.env created"
fi

# Backend .env
if [ ! -f "backend/.env" ]; then
  bash backend/setup.sh
fi

echo ""
echo "──────────────────────────────────────────────"
echo "✅ Setup complete! Next steps:"
echo ""
echo "  BACKEND:  cd backend && export \$(cat .env | grep -v '#' | xargs) && ./gradlew run"
echo "  WEB:      cd webApp  && npm install && npm run dev"
echo "  ANDROID:  Open b360-complete/ in Android Studio"
echo "  iOS:      cd iosApp  && pod install && open iosApp.xcworkspace"
echo "  DESKTOP:  ./gradlew :desktopApp:run"
echo "──────────────────────────────────────────────"
