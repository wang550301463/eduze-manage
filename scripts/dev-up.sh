#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
if [[ ! -f docker/platform/.env.dev ]]; then python3 scripts/platform-env.py; fi
./mvnw package
pnpm --dir web install --frozen-lockfile
pnpm --dir web build
node miniapp/build.mjs
exec docker compose --env-file docker/platform/.env.dev -f docker/platform/compose.yml up -d --build --wait
