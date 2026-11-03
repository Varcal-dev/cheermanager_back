#!/usr/bin/env bash
# Prueba AUTOMATICA de restauracion: toma el ultimo respaldo (o el que indiques), lo restaura en una base de
# prueba aparte, comprueba que quedo bien y la borra. Un respaldo que nunca se restauro no es un respaldo.
# Uso (mensual, por cron):
#   DB_NAME=cheermanager DB_USER=backup DB_PASSWORD=... BACKUP_DIR=/var/backups/cheermanager ./ops/probar_restauracion.sh
#   ./ops/probar_restauracion.sh /ruta/archivo.sql.gz          # un respaldo concreto
# El usuario de MySQL necesita permiso para crear/borrar la base "<DB_NAME>_restore_test" (o RESTORE_DB).
# Escribe el resultado en $BACKUP_DIR/ultima_prueba_restauracion.txt (lo lee verificar_salud.sh).
# KEEP=1 conserva la base de prueba para inspeccionarla. Nunca toca la base real.
set -uo pipefail
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
: "${DB_NAME:?falta DB_NAME}" "${DB_USER:?falta DB_USER}" "${DB_PASSWORD:?falta DB_PASSWORD}"
DB_HOST="${DB_HOST:-localhost}"; DB_PORT="${DB_PORT:-3306}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
RESTORE_DB="${RESTORE_DB:-${DB_NAME}_restore_test}"
SELLO="$BACKUP_DIR/ultima_prueba_restauracion.txt"
[ "$RESTORE_DB" != "$DB_NAME" ] || { echo "RESTORE_DB no puede ser la base real ($DB_NAME)" >&2; exit 2; }

FILE="${1:-$(ls -1t "$BACKUP_DIR/${DB_NAME}"_*.sql.gz 2>/dev/null | head -n1 || true)}"
fallo() {
  echo "PRUEBA DE RESTAURACION FALLIDA: $1" >&2
  mkdir -p "$BACKUP_DIR"; printf '%s FALLO %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$1" > "$SELLO"
  "$AQUI/alertar.sh" "La prueba de restauracion del respaldo FALLO: $1" || true
  exit 1
}
[ -n "$FILE" ] && [ -f "$FILE" ] || fallo "no hay respaldo para probar en $BACKUP_DIR"

CNF="$(mktemp)"; trap 'rm -f "$CNF"' EXIT
printf '[client]\nuser=%s\npassword=%s\nhost=%s\nport=%s\n' "$DB_USER" "$DB_PASSWORD" "$DB_HOST" "$DB_PORT" > "$CNF"
sql() { mysql --defaults-extra-file="$CNF" -N -B "$@"; }
limpiar() { [ "${KEEP:-0}" = "1" ] || sql -e "DROP DATABASE IF EXISTS \`$RESTORE_DB\`" >/dev/null 2>&1 || true; }

if [ -f "$FILE.sha256" ]; then
  ( cd "$(dirname "$FILE")" && sha256sum -c "$(basename "$FILE").sha256" >/dev/null 2>&1 ) || fallo "el respaldo no coincide con su suma de verificacion ($FILE)"
fi
gzip -t "$FILE" || fallo "el archivo $FILE esta corrupto"

echo "Restaurando $(basename "$FILE") en la base de prueba '$RESTORE_DB'..."
sql -e "DROP DATABASE IF EXISTS \`$RESTORE_DB\`; CREATE DATABASE \`$RESTORE_DB\` CHARACTER SET utf8mb4" || fallo "no se pudo crear la base de prueba (permisos?)"
if ! gunzip -c "$FILE" | mysql --defaults-extra-file="$CNF" "$RESTORE_DB"; then limpiar; fallo "mysql devolvio error al cargar el respaldo"; fi

TABLAS="$(sql -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$RESTORE_DB' AND table_type='BASE TABLE'")" || { limpiar; fallo "no se pudo leer la base restaurada"; }
[ "${TABLAS:-0}" -ge 10 ] || { limpiar; fallo "la base restaurada solo tiene ${TABLAS:-0} tablas"; }
MALAS="$(sql -e "SELECT COUNT(*) FROM \`$RESTORE_DB\`.flyway_schema_history WHERE success = 0" 2>/dev/null || echo "x")"
[ "$MALAS" = "0" ] || { limpiar; fallo "el historial de migraciones restaurado esta incompleto (success=0: $MALAS)"; }
VERSION="$(sql -e "SELECT MAX(CAST(version AS UNSIGNED)) FROM \`$RESTORE_DB\`.flyway_schema_history" 2>/dev/null || echo "?")"
CLUBES="$(sql -e "SELECT COUNT(*) FROM \`$RESTORE_DB\`.clubes" 2>/dev/null || echo 0)"
[ "${CLUBES:-0}" -ge 1 ] || { limpiar; fallo "la tabla clubes quedo vacia"; }

echo "Comparacion (informativa: el respaldo es una foto anterior a hoy)  real -> restaurada"
for T in clubes personas deportistas facturas pagos; do
  R="$(sql -e "SELECT COUNT(*) FROM \`$DB_NAME\`.\`$T\`" 2>/dev/null || echo "-")"
  S="$(sql -e "SELECT COUNT(*) FROM \`$RESTORE_DB\`.\`$T\`" 2>/dev/null || echo "-")"
  printf '  %-12s %8s -> %s\n' "$T" "$R" "$S"
done
limpiar
RESUMEN="OK $(basename "$FILE") tablas=$TABLAS migracion=V$VERSION clubes=$CLUBES"
mkdir -p "$BACKUP_DIR"; printf '%s %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$RESUMEN" > "$SELLO"
echo "PRUEBA DE RESTAURACION OK: $RESUMEN"
