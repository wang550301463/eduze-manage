#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
python3 scripts/check-boundaries.py
python3 -m unittest discover -s scripts/tests
./mvnw -Pquality spotless:check verify
python3 scripts/verify-packaged-platform.py
pnpm --dir packages/contracts install --frozen-lockfile
pnpm --dir packages/contracts check
pnpm --dir web install --frozen-lockfile
pnpm --dir web lint
pnpm --dir web test
pnpm --dir web build
node miniapp/build.mjs
git diff --check
