# Guía de operación (producción)

Qué corre solo, cómo te enteras de que algo falló y cómo recuperarte. Pensada para un servidor único con Docker (ver `GUIA_DESPLIEGUE.md`).

## 1. Lo que corre solo

| Qué | Cuándo | Script | Si falla |
|---|---|---|---|
| Respaldo de la base (con suma de verificación y copia externa opcional) | Diario 2:30 | `ops/backup.sh` | Alerta inmediata |
| Prueba de restauración en una base aparte | Día 1 de cada mes | `ops/probar_restauracion.sh` | Alerta inmediata |
| Vigilante: API viva, respaldo reciente, prueba reciente, disco | Cada 5 min | `ops/verificar_salud.sh` | Alerta (sin repetir antes de 60 min) y aviso al recuperarse |
| Limpieza de tokens revocados vencidos | Cada hora (dentro del API) | automático | Queda en el log |

El crontab de ejemplo está en `ops/crontab.ejemplo`. Las claves y variables `ALERTA_*` van en un archivo con permisos 600 (por ejemplo `/etc/cheermanager/ops.env`), nunca en el repositorio.

## 2. Alertas

`ops/alertar.sh` avisa por **Telegram** o por un **webhook** (Slack, Discord, Mattermost). Mientras no haya correo (SMTP) este es el canal.

Telegram, 5 minutos: habla con `@BotFather`, crea un bot y copia el token; escríbele un mensaje al bot; abre `https://api.telegram.org/bot<TOKEN>/getUpdates` y copia el `chat.id`. Luego en `ops.env`:

```
ALERTA_TELEGRAM_TOKEN=123456:ABC...
ALERTA_TELEGRAM_CHAT_ID=987654321
```

Prueba: `set -a; . /etc/cheermanager/ops.env; set +a; ops/alertar.sh "prueba"`. Debe llegarte el mensaje.

**Vigilancia desde fuera.** El vigilante interno no te avisa si el servidor entero se cae. Crea un monitor gratuito (UptimeRobot, Better Stack) que consulte cada minuto `https://api.tudominio.com/actuator/health` y espere `"status":"UP"`, con aviso a tu teléfono.

## 3. Cuánto se puede perder y cuánto se tarda

- **Pérdida máxima (RPO):** hasta 24 horas de datos (el respaldo es diario). Si necesitas menos, sube la frecuencia del respaldo (ej. cada 6 horas); la base es pequeña.
- **Tiempo de recuperación (RTO):** con el servidor sano, unos 30 minutos. Con servidor perdido: lo que tardes en levantar uno nuevo con Docker (≈1 hora) más la restauración.
- Los respaldos **solo en el mismo servidor no protegen** de perder el servidor. Configura `RCLONE_REMOTE` (rclone hacia Google Drive, S3, Backblaze…) para copiar cada respaldo fuera.

### Cifrado de los respaldos

El respaldo contiene **todos los clubes**: datos de menores, salud, finanzas y hashes de contraseñas. Por eso sale **cifrado con [age](https://github.com/FiloSottile/age)** y con clave **pública**: el servidor solo puede *cifrar*; **descifrar exige la llave privada, que vive fuera del servidor**. Así, quien robe el disco o acceda a la copia en la nube no puede leerlo.

**Configuración (una sola vez):**

1. En el servidor: `sudo apt install age`.
2. En **tu computador** (no en el servidor): `age-keygen -o llave-respaldos.txt`. Imprime la clave pública (`age1...`).
3. Guarda `llave-respaldos.txt` en **al menos dos sitios distintos y seguros** (gestor de contraseñas + una copia impresa guardada bajo llave). **Si se pierde, los respaldos cifrados no se pueden recuperar.** Opcional y recomendable: genera una segunda llave (otra persona de confianza o caja fuerte) y pon **ambas** claves públicas.
4. En `ops.env` del servidor: `BACKUP_AGE_RECIPIENT=age1...` (varias separadas por espacio). **Solo claves públicas**; el script rechaza cualquier otra cosa.
5. Ejecuta `ops/backup.sh` a mano: debe terminar en `OK (cifrado): ...sql.gz.age`.
6. Restauración de ensayo **completa**, con la llave, antes de confiar en el sistema (ver abajo).
7. Cuando el primer respaldo cifrado esté bien, **borra los `.sql.gz` en claro** que queden en el servidor y en `RCLONE_REMOTE` (los anteriores al cambio).

**Reglas que aplica el script:**
- Con `RCLONE_REMOTE` configurado y sin `BACKUP_AGE_RECIPIENT`, el respaldo **falla y avisa**: no sube datos en claro a un tercero. Solo `BACKUP_ALLOW_PLAINTEXT=1` lo permite (no recomendado).
- Sin clave y sin `RCLONE_REMOTE` sigue funcionando como antes, con una advertencia en el log.
- El archivo se publica solo si queda válido; un fallo a medias no deja un respaldo truncado.

**La prueba mensual automática es PARCIAL** (suma de verificación y formato; el sello dice `PARCIAL`, no `OK`), porque no tiene la llave. Eso **no** demuestra que el respaldo se pueda restaurar. Por eso, **cada trimestre** haz una restauración completa a mano en tu computador o en un servidor temporal, con una base MySQL vacía de prueba:

```bash
# Copia antes el último respaldo (.sql.gz.age) y su .sha256 a esa máquina.
BACKUP_AGE_IDENTITY=~/llave-respaldos.txt DB_NAME=cheermanager DB_USER=... DB_PASSWORD=... \
  RESTORE_DB=cheermanager_restore_test BACKUP_DIR=./copia ./ops/probar_restauracion.sh ./copia/cheermanager_FECHA.sql.gz.age
```

Debe terminar en `PRUEBA DE RESTAURACION OK`. Anota la fecha.

## 4. Recuperarse de un desastre (restaurar en producción)

1. Avisa a los clubes que el sistema está en mantenimiento.
2. `docker compose stop api` (la base sigue arriba).
3. Elige el respaldo más reciente bueno (`ls -lt /var/backups/cheermanager`). Verifica su suma: `sha256sum -c archivo.sql.gz.age.sha256` (o `archivo.sql.gz.sha256` si no está cifrado).
4. Restaura en una base **nueva** (no sobre la dañada) con `ops/restore.sh`: `DB_NAME=cheermanager_nueva ... ops/restore.sh archivo.sql.gz`. Si el respaldo es `.sql.gz.age`, necesitas tu llave privada: `BACKUP_AGE_IDENTITY=/ruta/llave-respaldos.txt DB_NAME=cheermanager_nueva ... ops/restore.sh archivo.sql.gz.age` (**ten la llave a mano antes de que ocurra el desastre**).
5. Comprueba: `SELECT COUNT(*) FROM clubes;` y `SELECT MAX(version) FROM flyway_schema_history;`.
6. Cambia `DB_URL` del API a la base nueva (o renombra las bases) y `docker compose up -d api`. Flyway aplica lo que falte.
7. Revisa `curl http://127.0.0.1:8081/actuator/health` y entra con un usuario de prueba.
8. Anota qué pasó y cuántas horas de datos se perdieron: los clubes deben reconstruirlas (por ejemplo asistencias o pagos de ese lapso).

Esto se ensaya con `ops/probar_restauracion.sh` cada mes. **No esperes a un desastre para ver si funciona.**

## 5. Sesiones y tokens

- Cerrar sesión o renovar la sesión revoca el token. Desde la migración V14 la revocación **se guarda en la base** y sobrevive a reinicios (se guarda solo el SHA-256 del token).
- El front renueva la sesión solo mientras el usuario trabaja (cuando quedan menos de 30 minutos). Un usuario inactivo más de 60 minutos debe volver a entrar.
- Límite: el API consulta las revocaciones en memoria. Con **varias instancias** del API, cada una vería las revocaciones nuevas de las otras solo al reiniciar; si algún día escalas a varias instancias, esa consulta debe pasar a la base.
- Si sospechas que un token se filtró y necesitas invalidar **todas** las sesiones de inmediato: cambia `JWT_SECRET` y reinicia el API (todos tendrán que volver a entrar).

## 6. Integración continua (GitHub Actions)

Cada `push` y cada pull request ejecutan: back → `mvn verify` y construcción de la imagen Docker; revisión de los scripts de `ops/`; front → `tsc --noEmit` y `next build`. Activa la protección de la rama principal para exigir que estén en verde antes de unir.

## 7. Si hay un incidente de seguridad (plantilla)

1. **Contener:** revoca accesos (cambia `JWT_SECRET`, desactiva el usuario afectado, cierra puertos).
2. **Preservar:** guarda los logs (`docker compose logs api > incidente.log`) y un respaldo de la base antes de tocar nada.
3. **Evaluar:** qué datos, de qué clubes y de cuántas personas (hay datos de menores y de salud).
4. **Avisar:** a los clubes afectados (ellos son los responsables de los datos) y, con tu abogado, a la autoridad y a los titulares según corresponda. Define de antemano quién decide y en cuánto tiempo.
5. **Corregir y documentar:** causa, arreglo y qué cambia para que no se repita.

## 8. Lista antes de abrir a clubes reales

- [ ] HTTPS delante del API (Caddy/Nginx) y `CORS_ALLOWED_ORIGINS` con el dominio exacto.
- [ ] `ops.env` creado, crontab instalado y una alerta de prueba recibida.
- [ ] Un respaldo hecho, **copiado fuera del servidor** y una prueba de restauración en OK (`ultima_prueba_restauracion.txt`).
- [ ] Respaldos **cifrados** (`BACKUP_AGE_RECIPIENT`), llave privada guardada en **dos sitios** fuera del servidor y una restauración **completa** probada con ella.
- [ ] Monitor externo configurado.
- [ ] CI en verde en `main`.
- [ ] Correo (SMTP) configurado — pendiente: se deja para los últimos ajustes.

## 9. Antes de desplegar la migración V47 (FIN-01: mensualidad única por periodo)

La migración `V47` crea un índice único `(club_id, inscripcion_id, periodo)` en `facturas`. **Si ya existen dos facturas del mismo mes para una inscripción, la migración se detiene** (no borra ni cambia nada) con un error que nombra `FIN01_HAY_FACTURAS_DUPLICADAS_REVISAR_ANTES_DE_MIGRAR`, y el API no arranca hasta resolverlo. Por eso, **antes de desplegar**, en la base de producción:

```sql
SELECT f.club_id, f.inscripcion_id, f.periodo, COUNT(*) AS cuantas,
       GROUP_CONCAT(CONCAT(f.id, ':', f.numero_factura, ':', IF(f.anulada = 1, 'anulada', 'vigente')) ORDER BY f.id) AS facturas
  FROM facturas f
 WHERE f.periodo IS NOT NULL AND f.inscripcion_id IS NOT NULL
 GROUP BY f.club_id, f.inscripcion_id, f.periodo
HAVING COUNT(*) > 1;
```

Si devuelve **0 filas**, despliega con tranquilidad. Si devuelve filas: revisa cada caso (¿cuál tiene pagos?), **anula la sobrante desde la app con su motivo** (no se borran facturas). OJO: una factura anulada sigue ocupando su periodo, así que el índice igual fallaría. Para cada caso hay que dejar **una sola** fila con ese `(inscripción, periodo)`; el arreglo de datos (p. ej. vaciar el `periodo` de la sobrante anulada y sin pagos) se hace a mano y con respaldo previo: escríbeme el caso y preparo el script. Haz un respaldo antes (sección 3).

