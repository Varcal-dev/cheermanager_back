#!/usr/bin/env bash
# Registra en el SERVIDOR que hiciste una restauracion COMPLETA de prueba de un respaldo cifrado (la que exige la llave
# privada y por eso se hace fuera del servidor, p. ej. en tu computador). Sin esto el vigilante (verificar_salud.sh)
# no puede saber que se probo y avisa cuando pasan MAX_RESTAURACION_COMPLETA_DIAS dias.
# Uso, DESPUES de restaurar y comprobar de verdad:
#   BACKUP_DIR=/var/backups/cheermanager ./ops/registrar_restauracion_completa.sh base  "restaure cheermanager_20261101 en mi PC"
#   BACKUP_DIR=/var/backups/cheermanager ./ops/registrar_restauracion_completa.sh fotos "restaure uploads_20261101 y abri 3 fotos"
#   base  = respaldo de la base de datos (docs/GUIA_OPERACION.md, "Cifrado de los respaldos")
#   fotos = respaldo de fotos y documentos (ops/restore_uploads.sh)
# No restaura nada ni toca datos: solo escribe un sello de fecha. Pide confirmacion (o CONFIRMAR=SI).
set -euo pipefail
QUE="${1:-}"; NOTA="${2:-sin nota}"
case "$QUE" in
  base)  SELLO_NOMBRE="ultima_restauracion_completa.txt" ;;
  fotos) SELLO_NOMBRE="ultima_restauracion_fotos.txt" ;;
  *) echo "uso: registrar_restauracion_completa.sh base|fotos [\"nota\"]" >&2; exit 2 ;;
esac
BACKUP_DIR="${BACKUP_DIR:-./backups}"
[ -d "$BACKUP_DIR" ] || { echo "No existe BACKUP_DIR ($BACKUP_DIR)" >&2; exit 2; }
if [ "${CONFIRMAR:-}" != "SI" ]; then
  echo "Vas a registrar que RESTAURASTE y COMPROBASTE un respaldo de '$QUE' (no se restaura nada aqui)."
  echo "Si no lo hiciste, el vigilante dejara de avisarte y nadie sabra que el respaldo no se ha probado. Escribe SI:"
  read -r CONFIRM; [ "$CONFIRM" = "SI" ] || { echo "Cancelado."; exit 1; }
fi
printf '%s COMPLETA %s %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$QUE" "$NOTA" > "$BACKUP_DIR/$SELLO_NOMBRE"
echo "Registrado: $BACKUP_DIR/$SELLO_NOMBRE"
