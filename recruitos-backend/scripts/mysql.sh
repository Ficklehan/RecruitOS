#!/usr/bin/env bash
# MySQL CLI wrapper — always use utf8mb4 to avoid Chinese mojibake on import.
set -euo pipefail

MYSQL_BIN="${MYSQL_BIN:-/usr/local/mysql/bin/mysql}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-12345678}"
DB_NAME="${DB_NAME:-recruit_os}"

exec "$MYSQL_BIN" \
  --default-character-set=utf8mb4 \
  -u"$DB_USER" \
  -p"$DB_PASS" \
  "$DB_NAME" \
  "$@"
