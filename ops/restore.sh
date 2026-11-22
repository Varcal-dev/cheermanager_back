#!/usr/bin/env bash
# Restaura un respaldo en una BD. PRUEBALO regularmente en una BD vacia de prueba:
# un respaldo que nunca se ha restaurado no es un respaldo.
#   DB_NAME=cheermanager_restore_test DB_USER=... DB_PASSWORD=... ./ops/restore.sh backups/archivo.sql.gz
# Respaldos cifrados (.sql.gz.age): hace falta la llave PRIVADA, que NO esta en el servidor:
#   BACKUP_AGE_IDENTITY=/ruta/a/llave-privada.txt DB_NAME=... DB_USER=... DB_PASSWORD=... ./ops/restore.sh archivo.sql.gz.age
set -euo pipefail
: "${DB_NAME:?}" "${DB_USER:?}" "${DB_PASSWORD:?}"; FILE="${1:?uso: restore.sh archivo.sql.gz[.age]}"
DB_HOST="${DB_HOST:-localhost}"; DB_PORT="${DB_PORT:-3306}"
[ -f "$FILE" ] || { echo "No existe el archivo $FILE" >&2; exit 2; }
if [[ "$FILE" == *.age ]]; then
  command -v age >/dev/null 2>&1 || { echo "Falta 'age' (apt install age): el respaldo esta cifrado." >&2; exit 2; }
  : "${BACKUP_AGE_IDENTITY:?el respaldo esta cifrado: define BACKUP_AGE_IDENTITY=/ruta/a/tu/llave-privada.txt}"
  [ -r "$BACKUP_AGE_IDENTITY" ] || { echo "No se puede leer la llave BACKUP_AGE_IDENTITY ($BACKUP_AGE_IDENTITY)" >&2; exit 2; }
  leer() { age -d -i "$BACKUP_AGE_IDENTITY" "$FILE" | gunzip -c; }
else
  leer() { gunzip -c "$FILE"; }
fi
# Se lee TODO el respaldo antes de tocar la base: asi un archivo corrupto o una llave equivocada falla aqui y no a medio restaurar.
echo "Comprobando que el respaldo se puede leer completo..."
leer >/dev/null || { echo "No se pudo leer el respaldo (corrupto, o llave equivocada si esta cifrado). No se toco la base." >&2; exit 1; }
echo "Se va a ESCRIBIR sobre la base '$DB_NAME' en $DB_HOST. Escribe el nombre de la base para continuar:"
read -r CONFIRM; [ "$CONFIRM" = "$DB_NAME" ] || { echo "Cancelado."; exit 1; }
CNF="$(mktemp)"; trap 'rm -f "$CNF"' EXIT
printf '[client]\nuser=%s\npassword=%s\nhost=%s\nport=%s\n' "$DB_USER" "$DB_PASSWORD" "$DB_HOST" "$DB_PORT" > "$CNF"
mysql --defaults-extra-file="$CNF" -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4"
leer | mysql --defaults-extra-file="$CNF" "$DB_NAME"
echo "Restaurado en $DB_NAME. Verifica: SELECT COUNT(*) FROM personas; SELECT COUNT(*) FROM clubes;"
