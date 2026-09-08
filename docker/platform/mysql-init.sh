#!/bin/bash
set -euo pipefail
for service in identity academic teaching portfolio media notification engagement commerce; do
  variable="${service^^}_DB_PASSWORD"
  password="${!variable}"
  [[ "$password" =~ ^[a-zA-Z0-9]{24,}$ ]] || { echo "Service DB passwords must contain at least 24 alphanumeric characters" >&2; exit 1; }
  MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot <<SQL
CREATE DATABASE IF NOT EXISTS eduze_${service} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'eduze_${service}'@'%' IDENTIFIED BY '${password}';
GRANT ALL PRIVILEGES ON eduze_${service}.* TO 'eduze_${service}'@'%';
SQL
done
