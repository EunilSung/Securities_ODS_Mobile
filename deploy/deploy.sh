#!/usr/bin/env bash
# 서버를 빌드해 인스턴스에 올리고 다시 띄운다. 배포하는 PC 에서 돌린다.
#   DEPLOY_HOST=ubuntu@<공인IP> deploy/deploy.sh
# 서버의 ~/ods/.env 는 건드리지 않는다. 처음 한 번은 .env 준비가 먼저다(.env.example 참고).
# HTTPS 는 같은 인스턴스의 Talk Caddy 가 맡는다 — 공용 네트워크 edge 와 ~/caddy-sites/ods.caddy 로 이어 준다.
set -euo pipefail

host="${DEPLOY_HOST:?DEPLOY_HOST=ubuntu@<공인IP> 를 준다}"
key="${DEPLOY_KEY:-$HOME/.ssh/oracle_test_server}"
root="$(cd "$(dirname "$0")/.." && pwd)"
stage="$root/build/deploy"
ssh_cmd="ssh -i $key -o StrictHostKeyChecking=accept-new"

cd "$root"
SERVER_ONLY=1 ./gradlew :server:installDist --quiet

rm -rf "$stage"
mkdir -p "$stage"
cp deploy/Dockerfile deploy/docker-compose.yml deploy/.env.example deploy/backup.sh "$stage/"
cp -R server/build/install/server "$stage/server"

rsync -az --delete --exclude .env -e "$ssh_cmd" "$stage/" "$host:ods/"

$ssh_cmd "$host" 'bash -s' <<'REMOTE'
set -euo pipefail
cd ~/ods
test -f .env || { echo "~/ods/.env 가 없다. .env.example 을 복사해 채운다" >&2; exit 1; }
domain=$(sed -n 's/^DOMAIN=//p' .env)
test -n "$domain" || { echo "~/ods/.env 의 DOMAIN 이 비었다" >&2; exit 1; }

docker network create edge >/dev/null 2>&1 || true
docker compose up -d --build --remove-orphans

mkdir -p ~/caddy-sites
cat > ~/caddy-sites/ods.caddy <<SITE
$domain {
	encode gzip
	reverse_proxy ods-server:8080
}
SITE

for _ in $(seq 90); do
  if curl -fs http://127.0.0.1:8095/health; then
    echo
    if docker ps --format '{{.Names}}' | grep -qx talk-caddy-1; then
      docker exec talk-caddy-1 caddy reload --config /etc/caddy/Caddyfile
      echo "배포 완료 — https://$domain"
    else
      echo "배포 완료 — Talk Caddy 가 떠 있지 않아 HTTPS 는 아직 열리지 않았다" >&2
    fi
    exit 0
  fi
  sleep 2
done
docker compose logs --tail 100 server
echo "서버가 /health 에 답하지 않는다" >&2
exit 1
REMOTE
