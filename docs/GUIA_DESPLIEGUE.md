# Guía de despliegue (VPS con Docker)

## Requisitos
Un servidor Linux (2 GB RAM mínimo), Docker + Docker Compose, un dominio y HTTPS delante de la API (Caddy o Nginx).

## Primer despliegue
1. Clona el repositorio en el servidor.
2. `cp .env.example .env` y rellena todo. Genera el secreto JWT con `openssl rand -base64 64`.
3. `docker compose up -d --build`
4. Flyway crea el esquema (V1), los datos iniciales (V2) y la alineación (V3) en la BD vacía. Revisa con `docker compose logs -f api` hasta ver "Started CheermanagerApplication".
5. Comprueba: `curl http://127.0.0.1:8081/actuator/health` → `{"status":"UP"}`.
6. Entra con el usuario `superadmin` (clave = `APP_BOOTSTRAP_ADMIN_PASSWORD`) y **cámbiala**.
7. Crea el primer club con `POST /api/clubes` (marca `cargarPlantilla` para niveles y divisiones de ejemplo).

## HTTPS (Caddy, lo más simple)
```
api.tudominio.com {
    reverse_proxy 127.0.0.1:8081
}
```
Pon `CORS_ALLOWED_ORIGINS` con el dominio exacto del frontend.

**Límite de intentos por IP (login y recuperar clave):** la API toma la IP del cliente de `X-Forwarded-For` solo si quien conecta es un proxy local o de red privada. Caddy ya envía el header. Con Nginx añade `proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;`. **No publiques el puerto 8081**: debe seguir en `127.0.0.1`, de modo que solo el proxy llegue a la API. Si algún día corres más de una instancia de la API, el contador no se comparte entre ellas.

## Base de datos que ya existe (producción actual)
Si tu BD fue creada con los scripts 01 y 02 y no tiene tabla `flyway_schema_history`, arranca la app con `baseline-version=2` (ya viene así): Flyway la marca como V2 y aplica solo V3. **Haz respaldo antes.**
Antes de migrar prueba en una copia: `ops/probar_esquema.sh`.

## Actualizar versión
`git pull && docker compose up -d --build`. Las migraciones nuevas corren solas. Regla: **nunca edites una migración ya aplicada**; crea V4, V5…

## Respaldos y vigilancia
Todo está descrito en **`docs/GUIA_OPERACION.md`**. Resumen: instala `ops/crontab.ejemplo` (respaldo diario, prueba mensual de restauración y vigilante cada 5 minutos), crea `/etc/cheermanager/ops.env` (permisos 600) con la base y los canales de alerta (`ALERTA_TELEGRAM_*` o `ALERTA_WEBHOOK_URL`) y configura `RCLONE_REMOTE` para copiar los respaldos **fuera del servidor**.
Restaurar a mano: `ops/restore.sh`. Un respaldo que nunca se restauró no es un respaldo: la prueba mensual lo hace sola.

## Logs
Docker rota los logs solos (10 MB x 5 archivos por servicio, ver `docker-compose.yml`). Cada línea lleva `[rid]`, el id de la petición, que también viaja en la cabecera `X-Request-Id`. Si un usuario reporta un error, pídele ese id y busca `docker compose logs api | grep <rid>`.

## Configuración opcional y fotos
- **Variables:** el servicio `api` del compose pasa al contenedor las variables opcionales (Wompi, SMTP, WhatsApp, límites de intentos, logs, etc.) **solo si están en tu `.env`**; las que no pongas usan el valor por defecto de la aplicación. La lista completa y sus ejemplos están en `env.example`. Tras editar `.env`: `docker compose up -d` y comprueba con `docker compose config` (debe mostrar tus valores y ninguna variable vacía de más).
- **Fotos y documentos** viven en el volumen Docker `uploads` (`/data/uploads` dentro del contenedor) y sobreviven a `docker compose up --build` y a recrear la API. **`docker compose down -v` borra los volúmenes, incluidas las fotos y la base de datos: no lo uses en producción.**
- **Pendiente:** el respaldo diario (`ops/backup.sh`) cubre solo la base de datos; las fotos del volumen `uploads` todavía no tienen copia de seguridad.

## Variables útiles
`LOG_LEVEL_APP=DEBUG` (temporal), `CONSENTIMIENTOS_EXIGIR_SALUD=true` (cuando el reporte `/api/consentimientos/pendientes` esté en cero), `JWT_EXPIRATION_MS`.

## Seguridad básica
- No publiques el puerto 3306. El compose ya lo evita.
- `.env` solo con permisos de tu usuario (`chmod 600`).
- Actualiza el sistema operativo y las imágenes periódicamente.
