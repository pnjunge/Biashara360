#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-$(cd "$(dirname "$0")" && pwd)}"
cd "$DEPLOY_DIR"
set -a
source .env
set +a

# Reuse the application's source connection settings when they are present in
# .env. SOURCE_* overrides remain useful for migrations from another database.
SOURCE_DATABASE_URL="${SOURCE_DATABASE_URL:-${DATABASE_URL:-}}"
SOURCE_DB_USER="${SOURCE_DB_USER:-${DB_USER:-}}"
SOURCE_DB_PASSWORD="${SOURCE_DB_PASSWORD:-${DB_PASSWORD:-}}"
SOURCE_DATABASE_URL="${SOURCE_DATABASE_URL#jdbc:}"

if [[ -z "$SOURCE_DATABASE_URL" || -z "$SOURCE_DB_USER" || -z "$SOURCE_DB_PASSWORD" ]]; then
  echo "Set SOURCE_DATABASE_URL, SOURCE_DB_USER, and SOURCE_DB_PASSWORD (or DATABASE_URL, DB_USER, and DB_PASSWORD in .env)." >&2
  exit 1
fi

mkdir -p migration
chmod 700 migration
echo "Exporting the source database..."
docker run --rm --network host \
  -e SOURCE_DATABASE_URL -e SOURCE_DB_USER -e PGPASSWORD="$SOURCE_DB_PASSWORD" \
  -v "$PWD/migration:/migration" postgres:18-alpine \
  sh -c 'pg_dump "$SOURCE_DATABASE_URL" --username="$SOURCE_DB_USER" --format=custom --no-owner --no-acl --file=/migration/rds.dump'

echo "Stopping the API and restoring PostgreSQL..."
docker compose -f compose.yml stop api
docker compose -f compose.yml exec -T postgres \
  dropdb -U "$POSTGRES_USER" --if-exists "$POSTGRES_DB"
docker compose -f compose.yml exec -T postgres \
  createdb -U "$POSTGRES_USER" "$POSTGRES_DB"
docker compose -f compose.yml exec -T postgres \
  pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner --no-acl --exit-on-error \
  < migration/rds.dump
docker compose -f compose.yml up -d api

echo "Restore finished. Verify https://${API_DOMAIN}/v1/health before changing DNS."
