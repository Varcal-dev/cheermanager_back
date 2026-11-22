#!/usr/bin/env bash
# Respaldo de las FOTOS Y DOCUMENTOS subidos (volumen `uploads`), cifrado igual que el de la base. Uso (cron diario):
#   BACKUP_DIR=/var/backups/cheermanager KEEP_DAYS=14 BACKUP_AGE_RECIPIENT=age1... ./ops/backup_uploads.sh
# DE DONDE LEE (elige uno):
#   - Por defecto, del contenedor del API con `docker compose exec`: no hace falta saber el nombre del volumen ni ser
#     root, solo poder usar Docker. Variables: COMPOSE_DIR (carpeta del docker-compose.yml, por defecto la del repo),
#     UPLOADS_SERVICE (api), UPLOADS_PATH (/data/uploads). El contenedor debe estar en marcha: si no, falla y avisa.
#     Compose lee el .env de COMPOSE_DIR para interpolar el archivo, asi que ese .env debe existir alli.
#   - UPLOADS_DIR=/ruta : carpeta del propio servidor (API fuera de Docker o volumen montado en el host).
# CIFRADO: con BACKUP_AGE_RECIPIENT (claves PUBLICAS de age) sale <uploads>_<fecha>.tar.gz.age y NUNCA existe en claro
#   en disco. Con RCLONE_REMOTE y sin clave FALLA (no se sube una copia en claro a un tercero) salvo
#   BACKUP_ALLOW_PLAINTEXT=1. Mismas reglas que backup.sh; ver docs/GUIA_OPERACION.md.
# Es un respaldo COMPLETO cada vez (no incremental). Una foto que cambia justo mientras se empaqueta no es un error.
# Si algo falla se avisa con ops/alertar.sh.
set -euo pipefail
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
avisar() { "$AQUI/alertar.sh" "$1" || true; }
TMP=""
limpiar() { rm -f "$TMP"; }
trap limpiar EXIT
trap 'avisar "FALLO el respaldo de las fotos y documentos (linea $LINENO). Revisa el servidor."' ERR
fallo() { avisar "FALLO el respaldo de las fotos y documentos: $1"; echo "ERROR: $1" >&2; exit 1; }
BACKUP_DIR="${BACKUP_DIR:-./backups}"; KEEP_DAYS="${KEEP_DAYS:-14}"

# --- Cifrado: se decide y se valida ANTES de leer nada ---
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
  fallo "hay RCLONE_REMOTE pero no BACKUP_AGE_RECIPIENT: no se suben fotos de menores SIN CIFRAR a un tercero. Configura la clave publica (docs/GUIA_OPERACION.md) o, bajo tu responsabilidad, BACKUP_ALLOW_PLAINTEXT=1"
else
  echo "ADVERTENCIA: respaldo SIN CIFRAR (BACKUP_AGE_RECIPIENT vacio). Ver docs/GUIA_OPERACION.md, 'Cifrado de los respaldos'." >&2
fi

# --- Origen de los archivos ---
if [ -n "${UPLOADS_DIR:-}" ]; then
  [ -d "$UPLOADS_DIR" ] || fallo "UPLOADS_DIR no existe: $UPLOADS_DIR"
  ORIGEN=(tar --warning=no-file-changed -czf - -C "$UPLOADS_DIR" .)
else
  command -v docker >/dev/null 2>&1 || fallo "'docker' no esta instalado o no esta en el PATH (o define UPLOADS_DIR=/ruta)"
  COMPOSE_DIR="${COMPOSE_DIR:-$AQUI/..}"
  [ -f "$COMPOSE_DIR/docker-compose.yml" ] || fallo "no existe $COMPOSE_DIR/docker-compose.yml (define COMPOSE_DIR o UPLOADS_DIR)"
  ORIGEN=(docker compose --project-directory "$COMPOSE_DIR" -f "$COMPOSE_DIR/docker-compose.yml" exec -T "${UPLOADS_SERVICE:-api}"
          tar --warning=no-file-changed -czf - -C "${UPLOADS_PATH:-/data/uploads}" .)
fi
# tar sale con 1 si algun archivo cambio mientras se leia (p. ej. alguien subio una foto): no es un fallo. 2 o mas si lo es.
empaquetar() {
  local rc=0
  "${ORIGEN[@]}" || rc=$?
  if [ "$rc" -eq 1 ]; then echo "AVISO: algun archivo cambio mientras se empaquetaba; el respaldo es valido." >&2; return 0; fi
  return "$rc"
}

mkdir -p "$BACKUP_DIR"; umask 077
if [ "$CIFRAR" = 1 ]; then EXT="tar.gz.age"; else EXT="tar.gz"; fi
OUT="$BACKUP_DIR/uploads_$(date +%Y%m%d_%H%M%S).$EXT"
# Archivo temporal y mv solo si queda valido: un fallo a medias nunca deja un respaldo truncado con aspecto de reciente.
TMP="$OUT.parcial"
if [ "$CIFRAR" = 1 ]; then
  empaquetar | age "${AGE_ARGS[@]}" > "$TMP"
else
  empaquetar > "$TMP"
fi

# Verificacion minima. Cifrado: cabecera de age y tamano (el contenido no se abre sin la llave privada, que no esta
# aqui). Sin cifrar: el tar.gz se puede leer completo. Una carpeta vacia (aun sin fotos) es un respaldo valido y pequeno.
VALIDO=0
if [ "$CIFRAR" = 1 ]; then
  if [ "$(head -c 21 "$TMP")" = "age-encryption.org/v1" ] && [ "$(stat -c %s "$TMP")" -gt 100 ]; then VALIDO=1; fi
else
  if tar -tzf "$TMP" >/dev/null 2>&1; then VALIDO=1; fi
fi
if [ "$VALIDO" != 1 ]; then
  avisar "RESPALDO DE FOTOS INVALIDO: $OUT (vacio o corrupto)"; rm -f "$TMP"; echo "RESPALDO INVALIDO" >&2; exit 1
fi
mv "$TMP" "$OUT"
( cd "$BACKUP_DIR" && sha256sum "$(basename "$OUT")" > "$(basename "$OUT").sha256" )
if [ -n "${RCLONE_REMOTE:-}" ]; then
  rclone copy "$OUT" "$RCLONE_REMOTE" && rclone copy "$OUT.sha256" "$RCLONE_REMOTE"
fi
# Rotacion: cubre .tar.gz, .tar.gz.age, sus .sha256 y restos .parcial de intentos fallidos.
find "$BACKUP_DIR" -name "uploads_*.tar.gz*" -mtime +"$KEEP_DAYS" -delete
if [ "$CIFRAR" = 1 ]; then echo "OK (cifrado): $OUT"; else echo "OK: $OUT"; fi
