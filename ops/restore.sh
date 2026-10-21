#!/usr/bin/env bash
# Restaura un respaldo en una BD. PRUEBALO regularmente en una BD vacia de prueba:
# un respaldo que nunca se ha restaurado no es un respaldo.
#   DB_NAME=cheermanager_restore_test DB_USER=... DB_PASSWORD=... ./ops/restore.sh backups/archivo.sql.gz
set -euo pipefail
: "${DB_NAME:?}" "${DB_USER:?}" "${DB_PASSWORD:?}"; FILE="${1:?uso: restore.sh archivo.sql.gz}"
DB_HOST="${DB_HOST:-localhost}"; DB_PORT="${DB_PORT:-3306}"
echo "Se va a ESCRIBIR sobre la base '$DB_NAME' en $DB_HOST. Escribe el nombre de la base para continuar:"
read -r CONFIRM; [ "$CONFIRM" = "$DB_NAME" ] || { echo "Cancelado."; exit 1; }
CNF="$(mktemp)"; trap 'rm -f "$CNF"' EXIT
printf '[client]\nuser=%s\npassword=%s\nhost=%s\nport=%s\n' "$DB_USER" "$DB_PASSWORD" "$DB_HOST" "$DB_PORT" > "$CNF"
mysql --defaults-extra-file="$CNF" -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4"
gunzip -c "$FILE" | mysql --defaults-extra-file="$CNF" "$DB_NAME"
echo "Restaurado en $DB_NAME. Verifica: SELECT COUNT(*) FROM personas; SELECT COUNT(*) FROM clubes;"
