#!/usr/bin/env bash
# 一键启动本地开发环境：MySQL + Redis（Compose）+ Spring Boot
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

echo ">>> 启动 MySQL / Redis ..."
docker compose -f docker/docker-compose.dev.yml --env-file docker/.env.dev up -d

echo ">>> 等待 MySQL 就绪 ..."
for i in $(seq 1 30); do
  status=$(docker inspect eduze-mysql-dev --format '{{.State.Health.Status}}' 2>/dev/null || echo "missing")
  if [ "$status" = "healthy" ]; then
    break
  fi
  sleep 2
done

if [ "$(docker inspect eduze-mysql-dev --format '{{.State.Health.Status}}' 2>/dev/null)" != "healthy" ]; then
  echo "MySQL 未就绪，请检查: docker logs eduze-mysql-dev"
  exit 1
fi

if [ -d web/dist ]; then
  mkdir -p src/main/resources/static
  cp -R web/dist/* src/main/resources/static/
fi

echo ">>> 启动应用（8080）..."
echo "    账号: admin / admin@123"
echo "    访问: http://localhost:8080/login"
export DB_USER=eduze DB_PASSWORD=root123 REDIS_PASSWORD=
exec ./mvnw spring-boot:run -Dskip.frontend.build=true
