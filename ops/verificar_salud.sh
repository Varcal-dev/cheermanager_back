#!/usr/bin/env bash
# Vigilante: comprueba que el API responde, que hay un respaldo reciente, que se probo restaurarlo hace poco y que
# el disco no esta lleno. Avisa con ops/alertar.sh (no repite el mismo aviso antes de ALERTA_REPETIR_MIN minutos y
# avisa cuando todo vuelve a la normalidad). Uso (cron cada 5 minutos):
#   BACKUP_DIR=/var/backups/cheermanager DB_NAME=cheermanager ./ops/verificar_salud.sh
# Variables: HEALTH_URL (http://127.0.0.1:8081/actuator/health) · MAX_BACKUP_HORAS (26) · MAX_PRUEBA_DIAS (40)
#            DISCO_MAX_PCT (85) · ALERTA_REPETIR_MIN (60) · ESTADO_DIR (/var/tmp)
# Sale con 1 si encontro algun problema (util tambien para monitores externos).
set -uo pipefail
AQUI="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
HEALTH_URL="${HEALTH_URL:-http://127.0.0.1:8081/actuator/health}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"; DB_NAME="${DB_NAME:-cheermanager}"
MAX_BACKUP_HORAS="${MAX_BACKUP_HORAS:-26}"; MAX_PRUEBA_DIAS="${MAX_PRUEBA_DIAS:-40}"
DISCO_MAX_PCT="${DISCO_MAX_PCT:-85}"; REPETIR_MIN="${ALERTA_REPETIR_MIN:-60}"
ESTADO_DIR="${ESTADO_DIR:-/var/tmp}"; ESTADO="$ESTADO_DIR/cheermanager_salud.estado"
PROBLEMAS=()

# 1) API viva
RESP="$(curl -sS -m 10 "$HEALTH_URL" 2>/dev/null || true)"
echo "$RESP" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"' || PROBLEMAS+=("El API no responde UP en $HEALTH_URL")

# 2) respaldo reciente
ULT="$(ls -1t "$BACKUP_DIR/${DB_NAME}"_*.sql.gz 2>/dev/null | head -n1 || true)"
if [ -z "$ULT" ]; then
  PROBLEMAS+=("No hay ningun respaldo en $BACKUP_DIR")
else
  EDAD_H=$(( ( $(date +%s) - $(stat -c %Y "$ULT") ) / 3600 ))
  [ "$EDAD_H" -le "$MAX_BACKUP_HORAS" ] || PROBLEMAS+=("El ultimo respaldo tiene $EDAD_H horas (maximo $MAX_BACKUP_HORAS)")
fi

# 3) prueba de restauracion reciente y buena
SELLO="$BACKUP_DIR/ultima_prueba_restauracion.txt"
if [ ! -f "$SELLO" ]; then
  PROBLEMAS+=("Nunca se ha probado restaurar un respaldo (ejecuta ops/probar_restauracion.sh)")
else
  grep -q ' OK ' "$SELLO" || PROBLEMAS+=("La ultima prueba de restauracion fallo: $(cat "$SELLO")")
  EDAD_D=$(( ( $(date +%s) - $(stat -c %Y "$SELLO") ) / 86400 ))
  [ "$EDAD_D" -le "$MAX_PRUEBA_DIAS" ] || PROBLEMAS+=("La ultima prueba de restauracion tiene $EDAD_D dias (maximo $MAX_PRUEBA_DIAS)")
fi

# 4) disco
for D in / "$BACKUP_DIR"; do
  [ -d "$D" ] || continue
  PCT="$(df -P "$D" | awk 'NR==2{gsub("%","",$5); print $5}')"
  [ -z "$PCT" ] || [ "$PCT" -lt "$DISCO_MAX_PCT" ] || PROBLEMAS+=("Disco de $D al ${PCT}% (maximo $DISCO_MAX_PCT%)")
done

mkdir -p "$ESTADO_DIR" 2>/dev/null || true
AHORA="$(date +%s)"
if [ "${#PROBLEMAS[@]}" -eq 0 ]; then
  if [ -f "$ESTADO" ]; then rm -f "$ESTADO"; "$AQUI/alertar.sh" "RECUPERADO: todo vuelve a la normalidad." || true; fi
  echo "OK"; exit 0
fi
TEXTO="$(printf -- '- %s\n' "${PROBLEMAS[@]}")"
echo "PROBLEMAS:"; echo "$TEXTO"
ULTIMO=0; [ -f "$ESTADO" ] && ULTIMO="$(cat "$ESTADO" 2>/dev/null || echo 0)"
if [ $(( AHORA - ULTIMO )) -ge $(( REPETIR_MIN * 60 )) ]; then
  "$AQUI/alertar.sh" "PROBLEMAS DETECTADOS:
$TEXTO" || true
  echo "$AHORA" > "$ESTADO"
fi
exit 1
