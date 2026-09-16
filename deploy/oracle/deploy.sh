#!/usr/bin/env bash
set -Eeuo pipefail

cd "$(dirname "$0")"
[[ "$(uname -m)" == "aarch64" ]] || {
  echo "This deployment must run on the OCI Ampere ARM64 VM (aarch64)." >&2
  exit 1
}
[[ -f .env ]] || {
  echo "Missing .env; copy ../hetzner/.env.example here and fill it in." >&2
  exit 1
}

docker compose -f compose.yml config --quiet
docker compose -f compose.yml build --pull
docker compose -f compose.yml up -d --remove-orphans
docker compose -f compose.yml ps
