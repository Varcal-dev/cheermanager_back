#!/usr/bin/env bash
# Restaura un respaldo de fotos y documentos (uploads_*.tar.gz[.age]). Solo AGREGA o SOBRESCRIBE archivos con el mismo
# nombre; no borra los que ya existan. PRUEBALO en un entorno de prueba antes de necesitarlo.
# Respaldos cifrados (.age): hace falta la llave PRIVADA, que NO esta en el servidor:
#   BACKUP_AGE_IDENTITY=/ruta/a/llave-privada.txt ./ops/restore_uploads.sh uploads_20261121_024500.tar.gz.age
# DESTINO (elige uno):
#   - Por defecto, el contenedor del API (docker compose exec; mismas variables COMPOSE_DIR, UPLOADS_SERVICE y
#     UPLOADS_PATH que backup_uploads.sh). Los archivos quedan con el dueño del API.
#   - UPLOADS_DIR=/ruta : una carpeta (por ejemplo en tu computador para revisar el contenido). Quedan con TU usuario.
set -euo pipefail
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FILE="${1:?uso: restore_uploads.sh archivo.tar.gz[.age]}"
[ -f "$FILE" ] || { echo "No existe el archivo $FILE" >&2; exit 2; }
if [[ "$FILE" == *.age ]]; then
  command -v age >/dev/null 2>&1 || { echo "Falta 'age' (apt install age): el respaldo esta cifrado." >&2; exit 2; }
  : "${BACKUP_AGE_IDENTITY:?el respaldo esta cifrado: define BACKUP_AGE_IDENTITY=/ruta/a/tu/llave-privada.txt}"
  [ -r "$BACKUP_AGE_IDENTITY" ] || { echo "No se puede leer la llave BACKUP_AGE_IDENTITY ($BACKUP_AGE_IDENTITY)" >&2; exit 2; }
  leer() { age -d -i "$BACKUP_AGE_IDENTITY" "$FILE"; }
else
  leer() { cat "$FILE"; }
fi
if [ -n "${UPLOADS_DIR:-}" ]; then
  DESTINO="la carpeta $UPLOADS_DIR"
  mkdir -p "$UPLOADS_DIR"
  EXTRAER=(tar -xzf - -C "$UPLOADS_DIR")
else
  command -v docker >/dev/null 2>&1 || { echo "'docker' no esta instalado (o define UPLOADS_DIR=/ruta)" >&2; exit 2; }
  COMPOSE_DIR="${COMPOSE_DIR:-$AQUI/..}"
  [ -f "$COMPOSE_DIR/docker-compose.yml" ] || { echo "No existe $COMPOSE_DIR/docker-compose.yml (define COMPOSE_DIR o UPLOADS_DIR)" >&2; exit 2; }
  DESTINO="el contenedor '${UPLOADS_SERVICE:-api}' (${UPLOADS_PATH:-/data/uploads})"
  EXTRAER=(docker compose --project-directory "$COMPOSE_DIR" -f "$COMPOSE_DIR/docker-compose.yml" exec -T "${UPLOADS_SERVICE:-api}"
           tar -xzf - -C "${UPLOADS_PATH:-/data/uploads}")
fi
# Se lee TODO el respaldo antes de escribir: un archivo corrupto o una llave equivocada falla aqui y no a medio restaurar.
echo "Comprobando que el respaldo se puede leer completo..."
N="$(leer | tar -tzf - | wc -l)" || { echo "No se pudo leer el respaldo (corrupto, o llave equivocada si esta cifrado). No se escribio nada." >&2; exit 1; }
echo "El respaldo contiene $N entradas. Se van a ESCRIBIR en $DESTINO (sin borrar lo existente). Escribe RESTAURAR para continuar:"
read -r CONFIRM; [ "$CONFIRM" = "RESTAURAR" ] || { echo "Cancelado."; exit 1; }
leer | "${EXTRAER[@]}"
echo "Restaurado en $DESTINO. Verifica abriendo una foto de un deportista en la aplicacion."
