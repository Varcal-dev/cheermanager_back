#!/usr/bin/env bash
# Prueba el ESQUEMA contra un MySQL 8 real, sin la aplicacion: carga las migraciones (V1..Vn) en una base
# desechable y verifica reglas de aislamiento entre clubes y que los INSERT tipicos de la app funcionen.
# Uso:  DB_USER=root [DB_PASSWORD=...] [DB_HOST=127.0.0.1 DB_PORT=3306 | MYSQL_SOCKET=/tmp/mysql.sock] ./ops/probar_esquema.sh
# NO toca tu base real: crea y borra `cheermanager_prueba`.
set -uo pipefail
DB=cheermanager_prueba
DIR="${MIGRATIONS_DIR:-$(cd "$(dirname "$0")/.." && pwd)/src/main/resources/db/migration}"
ARGS=(-u"${DB_USER:-root}")
[ -n "${DB_PASSWORD:-}" ] && ARGS+=(-p"$DB_PASSWORD")
if [ -n "${MYSQL_SOCKET:-}" ]; then ARGS+=(-S "$MYSQL_SOCKET"); else ARGS+=(-h "${DB_HOST:-127.0.0.1}" -P "${DB_PORT:-3306}"); fi
sql() { mysql "${ARGS[@]}" --batch --skip-column-names "$@"; }
fallos=0
ok()   { if sql "$DB" -e "$2" >/dev/null 2>/tmp/probar.err; then echo "  OK    $1"; else echo "  FALLA $1: $(head -c 200 /tmp/probar.err)"; fallos=$((fallos+1)); fi; }
no()   { if sql "$DB" -e "$2" >/dev/null 2>/tmp/probar.err; then echo "  FALLA $1: debia ser RECHAZADO y se acepto"; fallos=$((fallos+1)); else echo "  OK    $1 (rechazado como debe)"; fi; }
cuenta(){ test "$(sql "$DB" -e "$2")" = "$3" && echo "  OK    $1" || { echo "  FALLA $1: se esperaba $3 y hay $(sql "$DB" -e "$2")"; fallos=$((fallos+1)); }; }

echo "== Cargando migraciones en $DB"
sql -e "DROP DATABASE IF EXISTS $DB; CREATE DATABASE $DB CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" || exit 2
for f in $(ls "$DIR"/V*.sql | sort -V); do
  sql "$DB" < "$f" 2>/tmp/probar.err || { echo "  FALLA al aplicar $(basename "$f"): $(head -c 300 /tmp/probar.err)"; exit 1; }
  echo "  aplicada $(basename "$f")"
done

echo "== Datos de prueba (club 2 = Ratels, club 3 = otro club)"
sql "$DB" -e "INSERT INTO clubes (id,nombre,slug) VALUES (3,'Otro Club','otro-club');
 INSERT INTO niveles (club_id,nombre) VALUES (2,'T-Nivel'),(3,'T-Nivel');
 INSERT INTO personas (club_id,nombre,apellidos,numero_documento,fecha_nacimiento) VALUES (2,'Ana','Uno','D1','2012-01-01'),(3,'Beto','Dos','D1','2012-01-01');" || { echo "fallo preparando datos"; exit 1; }
N2=$(sql "$DB" -e "SELECT id FROM niveles WHERE club_id=2 AND nombre='T-Nivel'")
P2=$(sql "$DB" -e "SELECT id FROM personas WHERE club_id=2 AND numero_documento='D1'")
P3=$(sql "$DB" -e "SELECT id FROM personas WHERE club_id=3 AND numero_documento='D1'")
E=$(sql "$DB" -e "SELECT MIN(id) FROM estados_persona")

echo "== Aislamiento entre clubes"
ok "mismo numero de documento en dos clubes"        "SELECT 1"
no "mismo documento dos veces en el MISMO club"     "INSERT INTO personas (club_id,nombre,apellidos,numero_documento) VALUES (2,'X','Y','D1')"
ok "deportista del club 2 con nivel del club 2"     "INSERT INTO deportistas (club_id,persona_id,estado_id,nivel_actual_id) VALUES (2,$P2,$E,$N2)"
no "deportista del club 3 con nivel del club 2"     "INSERT INTO deportistas (club_id,persona_id,estado_id,nivel_actual_id) VALUES (3,$P3,$E,$N2)"
D2=$(sql "$DB" -e "SELECT id FROM deportistas WHERE club_id=2")
sql "$DB" -e "INSERT INTO acudientes (club_id,nombre,apellidos,telefono) VALUES (2,'Mama','Uno','300')"
A2=$(sql "$DB" -e "SELECT id FROM acudientes WHERE club_id=2")
ok "consentimiento del club 2 con acudiente del club 2" "INSERT INTO consentimientos (club_id,titular_id,acudiente_id,tipo,version_texto,canal,fecha_otorgado) VALUES (2,$P2,$A2,'DATOS_SALUD','v1','PAPEL',NOW(6))"
no "consentimiento del club 3 con acudiente del club 2" "INSERT INTO consentimientos (club_id,titular_id,acudiente_id,tipo,version_texto,canal,fecha_otorgado) VALUES (3,$P3,$A2,'DATOS_SALUD','v1','PAPEL',NOW(6))"
no "vinculo acudiente del club 2 a deportista del club 3" "INSERT INTO deportista_acudientes (club_id,deportista_id,acudiente_id) VALUES (3,$D2,$A2)"
cuenta "vista no-inscritos filtra por club (club 3 = 0)" "SELECT COUNT(*) FROM vista_deportistas_no_inscritos WHERE club_id=3" 0
cuenta "vista no-inscritos filtra por club (club 2 = 1)" "SELECT COUNT(*) FROM vista_deportistas_no_inscritos WHERE club_id=2" 1

echo "== INSERT tipicos de la app (antes fallaban por columnas heredadas)"
ok "contacto de emergencia (sin deportista_id)"      "INSERT INTO contactos_emergencia (club_id,persona_id,nombre,telefono) VALUES (2,$P2,'Tia','300')"
CAT=$(sql "$DB" -e "SELECT id FROM categorias_producto LIMIT 1"); [ -z "$CAT" ] && { sql "$DB" -e "INSERT INTO categorias_producto (club_id,nombre,codigo) VALUES (2,'Ropa','ROP')"; CAT=$(sql "$DB" -e "SELECT id FROM categorias_producto LIMIT 1"); }
ok "producto (sin precio ni cantidad_stock)"         "INSERT INTO productos (club_id,nombre,categoria_id,activo,requiere_control_stock,sku) VALUES (2,'Buso',$CAT,1,0,'SKU1')"
ok "asignar entrenador a grupo con historial"        "INSERT INTO grupo_entrenador (club_id,grupo_entrenamiento_id,entrenador_id,fecha_inicio,rol) VALUES (2,1,1,CURDATE(),'Principal'),(2,1,1,CURDATE(),'Principal')"
sql "$DB" -e "INSERT INTO dias_semana (nombre) VALUES ('Lunes')" 2>/dev/null
DIA=$(sql "$DB" -e "SELECT MIN(id) FROM dias_semana"); EA=$(sql "$DB" -e "SELECT MIN(id) FROM estados_asistencia")
sql "$DB" -e "INSERT INTO horarios (club_id,dia_id,hora_inicio,hora_fin) VALUES (2,$DIA,'16:00','18:00')"
H=$(sql "$DB" -e "SELECT MIN(id) FROM horarios WHERE club_id=2")
ok "asistencia (sin horario_entrenamiento_id)"       "INSERT INTO asistencias (club_id,deportista_id,horario_id,fecha,estado_asistencia_id) VALUES (2,$D2,$H,CURDATE(),$EA)"
no "la misma asistencia dos veces el mismo dia"      "INSERT INTO asistencias (club_id,deportista_id,horario_id,fecha,estado_asistencia_id) VALUES (2,$D2,$H,CURDATE(),$EA)"
ok "inscripcion con plan de mensualidad (columna nueva)" "SELECT plan_mensualidad_id, temporada_id FROM inscripciones LIMIT 1"
ok "factura con periodo"                              "SELECT periodo FROM facturas LIMIT 1"

echo "== Permisos"
cuenta "Administrador de Ratels puede gestionar roles" "SELECT COUNT(*) FROM rol_permisos rp JOIN roles r ON r.id=rp.rol_id JOIN permisos p ON p.id=rp.permiso_id WHERE r.nombre='Administrador' AND r.club_id=2 AND p.nombre IN ('gestionar_roles','asignar_permiso')" 2
cuenta "Administrador NO tiene gestionar_clubes"       "SELECT COUNT(*) FROM rol_permisos rp JOIN roles r ON r.id=rp.rol_id JOIN permisos p ON p.id=rp.permiso_id WHERE r.nombre='Administrador' AND p.nombre='gestionar_clubes'" 0

sql -e "DROP DATABASE IF EXISTS $DB"
echo; [ "$fallos" -eq 0 ] && echo "TODO OK" || { echo "$fallos prueba(s) fallaron"; exit 1; }
