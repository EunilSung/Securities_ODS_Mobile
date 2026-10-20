#!/usr/bin/env bash
# PostgreSQL 을 덤프해 ~/backups 에 7개까지 남긴다. cron 에 걸어 하루 한 번 돌린다.
set -euo pipefail
dir="$HOME/backups"
mkdir -p "$dir"
docker compose -f "$HOME/ods/docker-compose.yml" exec -T db pg_dump -U ods -d ods | gzip > "$dir/ods-$(date +%F).sql.gz"
ls -1t "$dir"/ods-*.sql.gz | tail -n +8 | xargs -r rm --
