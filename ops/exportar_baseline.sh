#!/usr/bin/env bash
# Genera src/main/resources/db/migration/V1__baseline.sql desde TU base actual (ya con Fase 0 y 1 aplicadas).
# Es lo que permite crear una base NUEVA (otro servidor, pruebas, un cliente) con Flyway.
# Incluye tablas, vistas, funciones, procedimientos y triggers que Hibernate nunca creo.
#   DB_NAME=cheermanager DB_USER=... DB_PASSWORD=... ./ops/exportar_baseline.sh
set -euo pipefail
: "${DB_NAME:?}" "${DB_USER:?}" "${DB_PASSWORD:?}"
DB_HOST="${DB_HOST:-localhost}"; DB_PORT="${DB_PORT:-3306}"
OUT="src/main/resources/db/migration/V1__baseline.sql"
CNF="$(mktemp)"; trap 'rm -f "$CNF"' EXIT
printf '[client]\nuser=%s\npassword=%s\nhost=%s\nport=%s\n' "$DB_USER" "$DB_PASSWORD" "$DB_HOST" "$DB_PORT" > "$CNF"
# --no-data: solo estructura. Se quitan DEFINER (si no, falla al restaurar con otro usuario) y AUTO_INCREMENT=n.
mysqldump --defaults-extra-file="$CNF" --no-data --routines --triggers --skip-add-drop-table \
  --skip-comments --set-gtid-purged=OFF "$DB_NAME" \
  | sed -E 's/DEFINER=`[^`]+`@`[^`]+`//g; s/ AUTO_INCREMENT=[0-9]+//g' > "$OUT"
echo "Generado $OUT. REVISALO antes de subirlo a git. Seguidamente, anade los datos semilla"
echo "que necesite una base nueva (catalogos globales: generos, estados, permisos, rubrica FEDECOLCHEER)."
