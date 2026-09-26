#!/usr/bin/env bash
set -Eeuo pipefail

cd "$(dirname "$0")"
[[ -f .env ]] || { echo "Missing deploy/hetzner/.env; copy .env.example and fill it in." >&2; exit 1; }

docker compose -f compose.yml config --quiet
docker compose -f compose.yml build --pull
docker compose -f compose.yml up -d --remove-orphans
docker compose -f compose.yml ps
