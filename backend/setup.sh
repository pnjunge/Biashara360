#!/bin/bash
# ─────────────────────────────────────────────────────────────────────────────
# Biashara360 Backend — First-time setup script
# Run this once from the backend/ directory:  bash setup.sh
# ─────────────────────────────────────────────────────────────────────────────
set -e

echo "🔧 Biashara360 Backend Setup"
echo "────────────────────────────"

# ── 1. Check Java ─────────────────────────────────────────────────────────────
if ! command -v java &> /dev/null; then
  echo "❌ Java not found. Please install Java 17+:"
  echo "   Ubuntu/Debian: sudo apt install openjdk-17-jdk"
  echo "   macOS:         brew install openjdk@17"
  exit 1
fi
JAVA_VER=$(java -version 2>&1 | awk -F'"' '/version/ {print $2}' | cut -d'.' -f1)
echo "✅ Java $JAVA_VER found"

# ── 2. Download Gradle Wrapper JAR ────────────────────────────────────────────
WRAPPER_JAR="gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  echo "⬇️  Downloading gradle-wrapper.jar..."
  mkdir -p gradle/wrapper
  # Official Gradle wrapper JAR from Maven Central
  curl -fsSL \
    "https://repo1.maven.org/maven2/org/gradle/gradle-wrapper/8.2/gradle-wrapper-8.2.jar" \
    -o "$WRAPPER_JAR" 2>/dev/null || \
  # Fallback: download full Gradle and extract the jar
  (
    echo "   Trying fallback download..."
    TMP_ZIP=$(mktemp /tmp/gradle-XXXX.zip)
    curl -fsSL "https://services.gradle.org/distributions/gradle-8.2-bin.zip" -o "$TMP_ZIP"
    unzip -j "$TMP_ZIP" "*/gradle-wrapper.jar" -d gradle/wrapper/
    rm "$TMP_ZIP"
  )
  echo "✅ gradle-wrapper.jar downloaded"
else
  echo "✅ gradle-wrapper.jar already present"
fi

chmod +x gradlew

# ── 3. Create .env from template ─────────────────────────────────────────────
if [ ! -f ".env" ]; then
  cat > .env << 'ENV'
# ── Database ─────────────────────────────────────────────────────────────────
DATABASE_URL=jdbc:postgresql://localhost:5432/biashara360
DB_USER=biashara360
DB_PASSWORD=changeme

# ── JWT ───────────────────────────────────────────────────────────────────────
JWT_SECRET=change-this-to-a-long-random-secret-in-production

# ── Mpesa Daraja (get from https://developer.safaricom.co.ke) ────────────────
MPESA_CONSUMER_KEY=your_consumer_key
MPESA_CONSUMER_SECRET=your_consumer_secret
MPESA_SHORT_CODE=174379
MPESA_PASS_KEY=your_pass_key
MPESA_CALLBACK_URL=https://your-domain.com/v1/payments/mpesa/callback
MPESA_ENV=sandbox

# ── CyberSource (get from https://business.cybersource.com) ──────────────────
CS_MERCHANT_ID=your_merchant_id
CS_MERCHANT_KEY_ID=your_key_id
CS_MERCHANT_SECRET_KEY=your_shared_secret
CS_ENVIRONMENT=sandbox

# ── KRA eTIMS ─────────────────────────────────────────────────────────────────
KRA_ETIMS_ENV=sandbox

# ── Social Commerce ───────────────────────────────────────────────────────────
META_APP_ID=your_meta_app_id
META_APP_SECRET=your_meta_app_secret
TIKTOK_CLIENT_KEY=your_tiktok_client_key
TIKTOK_CLIENT_SECRET=your_tiktok_client_secret
ANTHROPIC_API_KEY=your_anthropic_api_key
WEBHOOK_BASE_URL=https://your-domain.com/v1/social/webhook
ENV
  echo "✅ .env created — please fill in your API keys"
else
  echo "✅ .env already exists"
fi

# ── 4. PostgreSQL check ───────────────────────────────────────────────────────
if command -v psql &> /dev/null; then
  echo ""
  echo "🐘 PostgreSQL found. Creating database if it doesn't exist..."
  psql -U postgres -tc "SELECT 1 FROM pg_database WHERE datname='biashara360'" | grep -q 1 || \
    psql -U postgres -c "CREATE DATABASE biashara360;" && \
    psql -U postgres -c "CREATE USER biashara360 WITH PASSWORD 'changeme';" 2>/dev/null || true && \
    psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE biashara360 TO biashara360;" 2>/dev/null || true
  echo "✅ Database ready"
else
  echo ""
  echo "⚠️  PostgreSQL not found locally."
  echo "   Option A — Install it:  sudo apt install postgresql"
  echo "   Option B — Use Docker:  docker-compose up -d db"
fi

echo ""
echo "────────────────────────────────────────────────────────"
echo "✅ Setup complete! To start the server:"
echo ""
echo "   # Load env vars and run:"
echo "   export \$(cat .env | grep -v '#' | xargs)"
echo "   ./gradlew run"
echo ""
echo "   # Or with Docker (easiest):"
echo "   docker-compose up"
echo ""
echo "   Server will start on http://localhost:8080"
echo "────────────────────────────────────────────────────────"
