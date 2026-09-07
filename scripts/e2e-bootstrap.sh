#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "${ROOT}"

echo "[e2e] building backend jar (skip tests)..."
./mvnw -q -DskipTests package

echo "[e2e] starting docker compose (mysql, redis, app, nginx)..."
docker compose -f docker/docker-compose.yml --env-file docker/.env.example up -d mysql redis

echo "[e2e] waiting for mysql..."
until docker compose -f docker/docker-compose.yml exec -T mysql mysqladmin ping -h localhost --silent 2>/dev/null; do
  sleep 2
done

echo "[e2e] run backend locally against docker mysql/redis..."
export SPRING_PROFILES_ACTIVE=dev
export DB_HOST=127.0.0.1
export DB_PASSWORD="${DB_PASSWORD:-eduze_dev}"
export JWT_SECRET="${JWT_SECRET:-test-jwt-secret-minimum-32-chars-long}"
./mvnw -q spring-boot:run &
APP_PID=$!

cleanup() {
  kill "${APP_PID}" 2>/dev/null || true
}
trap cleanup EXIT

echo "[e2e] waiting for app health..."
for _ in $(seq 1 60); do
  if curl -sf http://localhost:8080/actuator/health >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

cd web
echo "[e2e] running playwright..."
pnpm e2e
