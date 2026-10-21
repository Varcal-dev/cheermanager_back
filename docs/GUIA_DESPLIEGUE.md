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

## Base de datos que ya existe (producción actual)
Si tu BD fue creada con los scripts 01 y 02 y no tiene tabla `flyway_schema_history`, arranca la app con `baseline-version=2` (ya viene así): Flyway la marca como V2 y aplica solo V3. **Haz respaldo antes.**
Antes de migrar prueba en una copia: `ops/probar_esquema.sh`.

## Actualizar versión
`git pull && docker compose up -d --build`. Las migraciones nuevas corren solas. Regla: **nunca edites una migración ya aplicada**; crea V4, V5…

## Respaldos
Cron diario: `DB_HOST=... DB_NAME=cheermanager DB_USER=... DB_PASSWORD=... BACKUP_DIR=/var/backups/cheermanager ./ops/backup.sh`
Restaurar: `ops/restore.sh`. **Prueba una restauración real al menos una vez**; un respaldo sin probar no es un respaldo. Copia los archivos fuera del servidor.

## Logs
Cada línea lleva `[rid]`, el id de la petición, que también viaja en la cabecera `X-Request-Id`. Si un usuario reporta un error, pídele ese id y busca `docker compose logs api | grep <rid>`.

## Variables útiles
`LOG_LEVEL_APP=DEBUG` (temporal), `CONSENTIMIENTOS_EXIGIR_SALUD=true` (cuando el reporte `/api/consentimientos/pendientes` esté en cero), `JWT_EXPIRATION_MS`.

## Seguridad básica
- No publiques el puerto 3306. El compose ya lo evita.
- `.env` solo con permisos de tu usuario (`chmod 600`).
- Actualiza el sistema operativo y las imágenes periódicamente.
