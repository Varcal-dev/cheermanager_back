#!/usr/bin/env bash
# Respaldo logico de MySQL con rotacion. Uso (cron diario):
#   DB_HOST=localhost DB_NAME=cheermanager DB_USER=backup DB_PASSWORD=... \
#   BACKUP_DIR=/var/backups/cheermanager KEEP_DAYS=14 BACKUP_AGE_RECIPIENT=age1... ./ops/backup.sh
# --single-transaction: copia consistente sin bloquear escrituras (InnoDB).
# --routines/--triggers/--events: incluye funciones, procedimientos y triggers
#   (la BD tiene funciones/vistas que NO estan en el repo; sin esto se perderian).
# CIFRADO (recomendado): con BACKUP_AGE_RECIPIENT (una o varias claves PUBLICAS de age, separadas por espacio) el
#   respaldo sale cifrado como <base>_<fecha>.sql.gz.age y NUNCA existe en claro en disco. En el servidor solo vive la
#   clave publica: la privada se guarda fuera (ver docs/GUIA_OPERACION.md, "Cifrado de los respaldos").
#   Si hay RCLONE_REMOTE y no hay clave, el respaldo FALLA (no se sube una copia en claro a un tercero) salvo que se
#   ponga BACKUP_ALLOW_PLAINTEXT=1 de forma explicita.
# Opcional: RCLONE_REMOTE=remoto:carpeta copia cada respaldo FUERA del servidor con rclone (recomendado).
# Si algo falla se avisa con ops/alertar.sh (ver ALERTA_* en ese archivo).
set -euo pipefail
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
avisar() { "$AQUI/alertar.sh" "$1" || true; }
CNF=""; TMP=""
limpiar() { rm -f "$CNF" "$TMP"; }
trap limpiar EXIT
trap 'avisar "FALLO el respaldo de la base ${DB_NAME:-?} (linea $LINENO). Revisa el servidor."' ERR
fallo() { avisar "FALLO el respaldo de la base ${DB_NAME:-?}: $1"; echo "ERROR: $1" >&2; exit 1; }
: "${DB_NAME:?falta DB_NAME}" "${DB_USER:?falta DB_USER}" "${DB_PASSWORD:?falta DB_PASSWORD}"
DB_HOST="${DB_HOST:-localhost}"; DB_PORT="${DB_PORT:-3306}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"; KEEP_DAYS="${KEEP_DAYS:-14}"

# --- Cifrado: se decide y se valida ANTES de tocar la base ---
CIFRAR=0; AGE_ARGS=()
DESTINATARIOS="${BACKUP_AGE_RECIPIENT:-}"
if [ -n "${DESTINATARIOS//[[:space:]]/}" ]; then
  command -v age >/dev/null 2>&1 || fallo "BACKUP_AGE_RECIPIENT esta definido pero 'age' no esta instalado (apt install age)"
  read -r -a LISTA <<<"$DESTINATARIOS"
  for R in "${LISTA[@]}"; do
    # Solo claves PUBLICAS (age1...). No se muestra el valor: si alguien pego la privada por error, no debe ir al log.
    [[ "$R" =~ ^age1[0-9a-z]{50,70}$ ]] || fallo "BACKUP_AGE_RECIPIENT debe contener solo claves PUBLICAS de age (empiezan por age1). Revisa ops.env."
    AGE_ARGS+=(-r "$R")
  done
  CIFRAR=1
elif [ -n "${RCLONE_REMOTE:-}" ] && [ "${BACKUP_ALLOW_PLAINTEXT:-0}" != "1" ]; then
  fallo "hay RCLONE_REMOTE pero no BACKUP_AGE_RECIPIENT: no se sube un respaldo SIN CIFRAR a un tercero. Configura la clave publica (docs/GUIA_OPERACION.md) o, bajo tu responsabilidad, BACKUP_ALLOW_PLAINTEXT=1"
else
  echo "ADVERTENCIA: respaldo SIN CIFRAR (BACKUP_AGE_RECIPIENT vacio). Ver docs/GUIA_OPERACION.md, 'Cifrado de los respaldos'." >&2
fi

mkdir -p "$BACKUP_DIR"; umask 077
if [ "$CIFRAR" = 1 ]; then EXT="sql.gz.age"; else EXT="sql.gz"; fi
OUT="$BACKUP_DIR/${DB_NAME}_$(date +%Y%m%d_%H%M%S).$EXT"
# Se escribe en un archivo temporal y solo se publica (mv) si queda valido: un fallo a medias nunca deja un
# respaldo truncado con aspecto de reciente (verificar_salud.sh lo daria por bueno).
TMP="$OUT.parcial"
# La clave va por archivo temporal, no por linea de comandos (se veria en `ps`).
CNF="$(mktemp)"
printf '[client]\nuser=%s\npassword=%s\nhost=%s\nport=%s\n' "$DB_USER" "$DB_PASSWORD" "$DB_HOST" "$DB_PORT" > "$CNF"
volcar() {
  mysqldump --defaults-extra-file="$CNF" --single-transaction --routines --triggers --events \
    --set-gtid-purged=OFF --default-character-set=utf8mb4 "$DB_NAME" | gzip -9
}
if [ "$CIFRAR" = 1 ]; then
  volcar | age "${AGE_ARGS[@]}" > "$TMP"
else
  volcar > "$TMP"
fi

# Verificacion minima. Cifrado: tamano y cabecera de age (el contenido no se puede abrir sin la llave privada, que
# no esta aqui). Sin cifrar: el gzip es valido y no esta vacio.
VALIDO=0
if [ "$CIFRAR" = 1 ]; then
  if [ "$(head -c 21 "$TMP")" = "age-encryption.org/v1" ] && [ "$(stat -c %s "$TMP")" -gt 1024 ]; then VALIDO=1; fi
else
  if gzip -t "$TMP" && [ "$(stat -c %s "$TMP")" -gt 1024 ]; then VALIDO=1; fi
fi
if [ "$VALIDO" != 1 ]; then
  avisar "RESPALDO INVALIDO: $OUT (vacio o corrupto)"; rm -f "$TMP"; echo "RESPALDO INVALIDO" >&2; exit 1
fi
mv "$TMP" "$OUT"
# La suma lleva solo el nombre del archivo (no la ruta del servidor) para poder verificarla en cualquier maquina.
( cd "$BACKUP_DIR" && sha256sum "$(basename "$OUT")" > "$(basename "$OUT").sha256" )
if [ -n "${RCLONE_REMOTE:-}" ]; then
  rclone copy "$OUT" "$RCLONE_REMOTE" && rclone copy "$OUT.sha256" "$RCLONE_REMOTE"
fi
# Rotacion: cubre .sql.gz, .sql.gz.age, sus .sha256 y restos .parcial de intentos fallidos.
find "$BACKUP_DIR" -name "${DB_NAME}_*.sql.gz*" -mtime +"$KEEP_DAYS" -delete
if [ "$CIFRAR" = 1 ]; then echo "OK (cifrado): $OUT"; else echo "OK: $OUT"; fi
