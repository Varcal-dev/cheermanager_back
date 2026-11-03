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

## 4. Recuperarse de un desastre (restaurar en producción)

1. Avisa a los clubes que el sistema está en mantenimiento.
2. `docker compose stop api` (la base sigue arriba).
3. Elige el respaldo más reciente bueno (`ls -lt /var/backups/cheermanager`). Verifica su suma: `sha256sum -c archivo.sql.gz.sha256`.
4. Restaura en una base **nueva** (no sobre la dañada) con `ops/restore.sh`: `DB_NAME=cheermanager_nueva ... ops/restore.sh archivo.sql.gz`.
5. Comprueba: `SELECT COUNT(*) FROM clubes;` y `SELECT MAX(version) FROM flyway_schema_history;`.
6. Cambia `DB_URL` del API a la base nueva (o renombra las bases) y `docker compose up -d api`. Flyway aplica lo que falte.
7. Revisa `curl http://127.0.0.1:8081/actuator/health` y entra con un usuario de prueba.
8. Anota qué pasó y cuántas horas de datos se perdieron: los clubes deben reconstruirlas (por ejemplo asistencias o pagos de ese lapso).

Esto se ensaya con `ops/probar_restauracion.sh` cada mes. **No esperes a un desastre para ver si funciona.**

## 5. Sesiones y tokens

- Cerrar sesión o renovar la sesión revoca el token. Desde la migración V14 la revocación **se guarda en la base** y sobrevive a reinicios (se guarda solo el SHA-256 del token).
- El front renueva la sesión solo mientras el usuario trabaja (cuando quedan menos de 30 minutos). Un usuario inactivo más de 8 horas debe volver a entrar.
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
- [ ] Monitor externo configurado.
- [ ] CI en verde en `main`.
- [ ] Correo (SMTP) configurado — pendiente: se deja para los últimos ajustes.
