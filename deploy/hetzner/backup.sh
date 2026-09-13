#!/usr/bin/env bash
set -Eeuo pipefail

DEPLOY_DIR="${DEPLOY_DIR:-/opt/biashara360/deploy/hetzner}"
BACKUP_DIR="${BACKUP_DIR:-/opt/biashara360/backups}"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"

cd "$DEPLOY_DIR"
mkdir -p "$BACKUP_DIR"
set -a
source .env
set +a

docker compose -f compose.yml exec -T postgres \
  pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc \
  > "$BACKUP_DIR/biashara360-$STAMP.dump"

find "$BACKUP_DIR" -type f -name 'biashara360-*.dump' -mtime +7 -delete

if [[ -n "${RESTIC_REPOSITORY:-}" ]]; then
  docker run --rm \
    -v "$BACKUP_DIR:/backups:ro" \
    -e RESTIC_REPOSITORY -e RESTIC_PASSWORD \
    -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY \
    restic/restic:0.18.0 backup /backups
  docker run --rm \
    -e RESTIC_REPOSITORY -e RESTIC_PASSWORD \
    -e AWS_ACCESS_KEY_ID -e AWS_SECRET_ACCESS_KEY \
    restic/restic:0.18.0 forget --keep-daily 7 --keep-weekly 4 --keep-monthly 6 --prune
fi
