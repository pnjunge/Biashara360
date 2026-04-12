#!/bin/sh
set -e

# Export environment variables (application.conf will use them or defaults)
export DATABASE_URL="${DATABASE_URL:-jdbc:postgresql://postgres:5432/biashara360}"
export DB_USER="${DB_USER:-biashara360}"
export DB_PASSWORD="${DB_PASSWORD:-password}"
export JWT_SECRET="${JWT_SECRET:-biashara360-super-secret-jwt-key-change-in-production}"
export MPESA_ENV="${MPESA_ENV:-sandbox}"

echo "Starting Biashara360 API..."
echo "DATABASE_URL: $DATABASE_URL"

# Wait for services to initialize
echo "Waiting for services to be ready..."
sleep 5

echo "Connecting to database..."

# Run the application (HikariCP will handle connection retries)
exec java \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -Dconfig.file=/app/application.conf \
  -jar app.jar
