# Biashara360 Backend

Ktor + PostgreSQL REST API. 56+ endpoints covering: Auth, Inventory, Orders, Customers, Expenses, Mpesa, CyberSource, Tax, KRA eTIMS, Social Commerce (WhatsApp/Instagram/Facebook/TikTok).

## Quick Start (Linux/macOS)

### Option 1 — Docker (recommended, no Java needed)
```bash
cd backend/
docker-compose up
```
Server starts at `http://localhost:8080`. PostgreSQL included.

### Option 2 — Run directly (requires Java 17+)

**Step 1: Run the setup script (first time only)**
```bash
cd backend/
bash setup.sh
```
This downloads the Gradle wrapper JAR, creates `.env`, and sets up the database.

**Step 2: Fill in your API keys in `.env`**

**Step 3: Start the server**
```bash
# Load env vars
export $(cat .env | grep -v '#' | xargs)

# Run
./gradlew run
```

### Option 3 — Manual gradlew setup (if setup.sh fails)
```bash
cd backend/
mkdir -p gradle/wrapper

# Download gradle-wrapper.jar
curl -fsSL "https://services.gradle.org/distributions/gradle-8.2-bin.zip" -o /tmp/gradle.zip
unzip -j /tmp/gradle.zip "*/gradle-wrapper.jar" -d gradle/wrapper/

# Make gradlew executable
chmod +x gradlew

# Run
export $(cat .env | grep -v '#' | xargs)
./gradlew run
```

## Windows

```bat
cd backend
gradlew.bat run
```
If `gradle-wrapper.jar` is missing, download Gradle 8.2 from https://gradle.org/releases/ and extract `gradle-wrapper.jar` to `gradle/wrapper/`.

## Environment Variables

See `.env` (created by setup.sh) or `src/main/resources/application.conf` for all variables:

| Variable | Description |
|---|---|
| `DATABASE_URL` | PostgreSQL JDBC URL |
| `JWT_SECRET` | Long random string for JWT signing |
| `MPESA_CONSUMER_KEY/SECRET` | From developer.safaricom.co.ke |
| `CS_MERCHANT_ID/KEY_ID/SECRET_KEY` | From business.cybersource.com |
| `KRA_ETIMS_ENV` | `sandbox` or `production` |
| `ANTHROPIC_API_KEY` | For AI social auto-replies |
| `META_APP_ID/SECRET` | From developers.facebook.com |
| `TIKTOK_CLIENT_KEY/SECRET` | From developers.tiktok.com |

## API Base URL
```
http://localhost:8080/v1/
```
