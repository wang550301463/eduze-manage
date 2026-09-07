#!/usr/bin/env bash
set -euo pipefail

# 用法：
#   CONFIRM_RESTORE=YES APP_STOPPED=YES DB_PASSWORD=xxx \
#     ./scripts/restore-mysql.sh /backups/eduze-2026-05-19-0300.sql.gz
#
# 危险操作：会覆盖目标库数据。恢复前务必停止应用写流量。

BACKUP_FILE="${1:?usage: CONFIRM_RESTORE=YES APP_STOPPED=YES restore-mysql.sh <backup.sql.gz>}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-eduze}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:?DB_PASSWORD is required}"

echo "WARNING: MySQL restore will OVERWRITE database '${DB_NAME}' on ${DB_HOST}:${DB_PORT}." >&2
echo "WARNING: Stop the application first (set APP_STOPPED=YES after docker compose stop app)." >&2

if [[ "${CONFIRM_RESTORE:-}" != "YES" ]]; then
  echo "Refusing restore: set CONFIRM_RESTORE=YES to proceed." >&2
  exit 1
fi

if [[ "${APP_STOPPED:-}" != "YES" ]]; then
  echo "Refusing restore: set APP_STOPPED=YES to confirm the app (and writers) are stopped." >&2
  echo "Example: docker compose -f docker/docker-compose.yml stop app && APP_STOPPED=YES CONFIRM_RESTORE=YES ..." >&2
  exit 1
fi

if [[ ! -f "${BACKUP_FILE}" ]]; then
  echo "backup file not found: ${BACKUP_FILE}" >&2
  exit 1
fi

SUM_FILE="${BACKUP_FILE}.sha256"
if [[ -f "${SUM_FILE}" ]]; then
  echo "[restore] verifying checksum ${SUM_FILE}"
  (
    cd "$(dirname "${BACKUP_FILE}")"
    sha256sum -c "$(basename "${SUM_FILE}")"
  )
else
  echo "WARNING: no .sha256 beside backup; skipping checksum verify." >&2
fi

if ! gunzip -t "${BACKUP_FILE}"; then
  echo "[restore] ERROR: gzip integrity check failed: ${BACKUP_FILE}" >&2
  exit 1
fi

echo "[restore] ${BACKUP_FILE} -> ${DB_NAME} on ${DB_HOST}:${DB_PORT}"
gunzip -c "${BACKUP_FILE}" | mysql \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --user="${DB_USER}" \
  --password="${DB_PASSWORD}"

echo "[restore] done — restart app and verify critical tables before serving traffic"
