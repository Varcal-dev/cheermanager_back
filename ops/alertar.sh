#!/usr/bin/env bash
# Envia una alerta de operacion. Uso: ops/alertar.sh "mensaje"
# Canales (configura UNO o ambos con variables de entorno; sin ninguno solo escribe en pantalla y en syslog):
#   ALERTA_WEBHOOK_URL            URL de un webhook (Slack, Discord, Mattermost...). Se envia {"text":..., "content":...}
#   ALERTA_TELEGRAM_TOKEN + ALERTA_TELEGRAM_CHAT_ID    bot de Telegram (BotFather) y chat donde avisar
#   ALERTA_PREFIJO                texto al inicio del mensaje (por defecto "[CheerManager]")
# Nunca falla el script que lo llama: si el envio no funciona, lo dice por stderr y sale con 0.
set -uo pipefail
MSG="${1:?uso: alertar.sh \"mensaje\"}"
PREFIJO="${ALERTA_PREFIJO:-[CheerManager]}"
TEXTO="$PREFIJO $(hostname 2>/dev/null || echo servidor): $MSG"
echo "ALERTA: $TEXTO" >&2
command -v logger >/dev/null 2>&1 && logger -t cheermanager-ops "$TEXTO" || true

# Escapa para JSON: barra, comillas y saltos de linea.
json_escape() { printf '%s' "$1" | sed -e 's/\\/\\\\/g' -e 's/"/\\"/g' | awk 'BEGIN{ORS="\\n"} {print}' | sed 's/\\n$//'; }
J="$(json_escape "$TEXTO")"

if [ -n "${ALERTA_WEBHOOK_URL:-}" ]; then
  curl -sS -m 15 -H 'Content-Type: application/json' -d "{\"text\":\"$J\",\"content\":\"$J\"}" \
    "$ALERTA_WEBHOOK_URL" >/dev/null || echo "No se pudo enviar la alerta al webhook" >&2
fi
if [ -n "${ALERTA_TELEGRAM_TOKEN:-}" ] && [ -n "${ALERTA_TELEGRAM_CHAT_ID:-}" ]; then
  curl -sS -m 15 "https://api.telegram.org/bot${ALERTA_TELEGRAM_TOKEN}/sendMessage" \
    --data-urlencode "chat_id=${ALERTA_TELEGRAM_CHAT_ID}" --data-urlencode "text=$TEXTO" >/dev/null \
    || echo "No se pudo enviar la alerta a Telegram" >&2
fi
exit 0
