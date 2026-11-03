#!/usr/bin/env bash
# Respaldo logico de MySQL con rotacion. Uso (cron diario):
#   DB_HOST=localhost DB_NAME=cheermanager DB_USER=backup DB_PASSWORD=... \
#   BACKUP_DIR=/var/backups/cheermanager KEEP_DAYS=14 ./ops/backup.sh
# --single-transaction: copia consistente sin bloquear escrituras (InnoDB).
# --routines/--triggers/--events: incluye funciones, procedimientos y triggers
#   (la BD tiene funciones/vistas que NO estan en el repo; sin esto se perderian).
# Opcional: RCLONE_REMOTE=remoto:carpeta copia cada respaldo FUERA del servidor con rclone (recomendado).
# Si algo falla se avisa con ops/alertar.sh (ver ALERTA_* en ese archivo).
set -euo pipefail
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
avisar() { "$AQUI/alertar.sh" "$1" || true; }
trap 'avisar "FALLO el respaldo de la base ${DB_NAME:-?} (linea $LINENO). Revisa el servidor."' ERR
: "${DB_NAME:?falta DB_NAME}" "${DB_USER:?falta DB_USER}" "${DB_PASSWORD:?falta DB_PASSWORD}"
DB_HOST="${DB_HOST:-localhost}"; DB_PORT="${DB_PORT:-3306}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"; KEEP_DAYS="${KEEP_DAYS:-14}"
mkdir -p "$BACKUP_DIR"; umask 077
OUT="$BACKUP_DIR/${DB_NAME}_$(date +%Y%m%d_%H%M%S).sql.gz"
# La clave va por archivo temporal, no por linea de comandos (se veria en `ps`).
CNF="$(mktemp)"; trap 'rm -f "$CNF"' EXIT
trap 'avisar "FALLO el respaldo de la base ${DB_NAME} (linea $LINENO). Revisa el servidor."; rm -f "$CNF"' ERR
printf '[client]\nuser=%s\npassword=%s\nhost=%s\nport=%s\n' "$DB_USER" "$DB_PASSWORD" "$DB_HOST" "$DB_PORT" > "$CNF"
mysqldump --defaults-extra-file="$CNF" --single-transaction --routines --triggers --events \
  --set-gtid-purged=OFF --default-character-set=utf8mb4 "$DB_NAME" | gzip -9 > "$OUT"
# Verificacion minima: el gzip es valido y no esta vacio.
if ! { gzip -t "$OUT" && [ "$(stat -c %s "$OUT")" -gt 1024 ]; }; then
  avisar "RESPALDO INVALIDO: $OUT (vacio o corrupto)"; rm -f "$OUT"; echo "RESPALDO INVALIDO" >&2; exit 1
fi
sha256sum "$OUT" > "$OUT.sha256"
if [ -n "${RCLONE_REMOTE:-}" ]; then
  rclone copy "$OUT" "$RCLONE_REMOTE" && rclone copy "$OUT.sha256" "$RCLONE_REMOTE"
fi
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz" -mtime +"$KEEP_DAYS" -delete
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz.sha256" -mtime +"$KEEP_DAYS" -delete
echo "OK: $OUT"
