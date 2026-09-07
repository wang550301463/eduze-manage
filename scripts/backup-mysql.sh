#!/usr/bin/env bash
set -euo pipefail

# 用法：DB_PASSWORD=xxx ./scripts/backup-mysql.sh
# Docker：docker compose -f docker/docker-compose.yml exec -e DB_PASSWORD -e DB_HOST=127.0.0.1 \
#           mysql sh -c '...' 或在已挂载 /backups 的宿主机上对内网 mysql 执行

BACKUP_DIR="${BACKUP_DIR:-/backups}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-eduze}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:?DB_PASSWORD is required}"

mkdir -p "${BACKUP_DIR}"
STAMP="$(date +%F-%H%M)"
OUT_FILE="${BACKUP_DIR}/eduze-${STAMP}.sql.gz"
SUM_FILE="${OUT_FILE}.sha256"

echo "[backup] dumping ${DB_NAME} -> ${OUT_FILE}"
mysqldump \
  --host="${DB_HOST}" \
  --port="${DB_PORT}" \
  --user="${DB_USER}" \
  --password="${DB_PASSWORD}" \
  --single-transaction \
  --routines \
  --events \
  --databases "${DB_NAME}" \
  | gzip > "${OUT_FILE}"

if [[ ! -s "${OUT_FILE}" ]]; then
  echo "[backup] ERROR: backup file is empty: ${OUT_FILE}" >&2
  exit 1
fi

if ! gunzip -t "${OUT_FILE}"; then
  echo "[backup] ERROR: gzip integrity check failed: ${OUT_FILE}" >&2
  exit 1
fi

# 校验和文件内容便于 restore 侧 sha256sum -c
(
  cd "$(dirname "${OUT_FILE}")"
  sha256sum "$(basename "${OUT_FILE}")" > "$(basename "${SUM_FILE}")"
)

echo "[backup] integrity ok; checksum -> ${SUM_FILE}"

echo "[backup] pruning backups older than 30 days in ${BACKUP_DIR}"
find "${BACKUP_DIR}" -name 'eduze-*.sql.gz' -mtime +30 -delete
find "${BACKUP_DIR}" -name 'eduze-*.sql.gz.sha256' -mtime +30 -delete

echo "[backup] done: ${OUT_FILE}"
