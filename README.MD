# CheerManager — Backend

API REST (Spring Boot) de **CheerManager**, plataforma **multi-club** para gestionar clubes de porrismo (cheerleading): atletas y acudientes, temporadas, grupos y horarios, inscripciones, facturación y pagos, asistencia, evaluaciones (incluida la rúbrica FEDECOLCHEER / IASF), eventos y competencias, tienda e inventario, portal de solo lectura para deportistas y acudientes, consentimientos de datos (Ley 1581 de 2012) y dashboards.

Lo consume el frontend **CheerManager Front** (Next.js), proyecto hermano.

## Contenido

- [Stack](#stack)
- [Arquitectura](#arquitectura)
- [Multi-club (tenant)](#multi-club-tenant)
- [Seguridad y permisos](#seguridad-y-permisos)
- [Flujos de negocio y su estado](#flujos-de-negocio-y-su-estado)
- [Reglas de negocio clave](#reglas-de-negocio-clave)
- [Endpoints por dominio](#endpoints-por-dominio)
- [Configuración (variables de entorno)](#configuración-variables-de-entorno)
- [Ejecución local](#ejecución-local)
- [Base de datos y migraciones](#base-de-datos-y-migraciones)
- [Despliegue y operación](#despliegue-y-operación)
- [Pruebas](#pruebas)
- [Pendientes conocidos](#pendientes-conocidos)

## Stack

| Componente | Detalle |
|---|---|
| Lenguaje | Java 21 (`pom.xml` y `Dockerfile` alineados) |
| Framework | Spring Boot 3.4.5 (Web, Data JPA, Security, Validation, AOP, Mail, Actuator) |
| Base de datos | MySQL 8 |
| Migraciones | Flyway (`V1`–`V6` en `src/main/resources/db/migration`) |
| Seguridad | Spring Security + JWT (`jjwt` 0.11.5), BCrypt |
| Build | Maven (`mvnw` / `mvnw.cmd`) |
| Contenedores | `Dockerfile` (multi-etapa, usuario no root) + `docker-compose.yml` (MySQL + API) |
| Pruebas | JUnit 5, Spring Security Test, H2 y Testcontainers/MySQL real |
| Puerto | `8081` (variable `PORT`) |

## Arquitectura

```
src/main/java/com/varcal/cheermanager/
├── Controller/    # REST por dominio: Auth, Clubes, Consentimientos, Dashboard, Evaluaciones (+Rutina),
│                  #   Eventos, Financiero, Funcionalidad, Historiales, Horario_Asistencia, Inventario,
│                  #   Org_dep, Persona, Portal
├── DTO/           # Request/response, mismo agrupamiento
├── Models/        # Entidades JPA (casi todas con club_id / @TenantId)
├── repository/    # Spring Data JPA
├── Service/       # Lógica de negocio (+ Evaluaciones/Rutina/Calculadoras, Financiero/FacturacionMensualJob)
├── security/      # JwtUtil, JwtTokenFilter, TokenBlacklistService
├── tenant/        # TenantContext, TenantConfig, TenantIdentifierResolver, ReferenciasClub, TenantLookupRepository
├── config/        # SecurityConfig, WebConfig, PermissionInterceptor, DataInitializer,
│                  #   GlobalExceptionHandler, RequestIdFilter
├── aspect/        # AuditoriaAspect (registra save/delete de repositorios)
└── Utils/         # AuthenticatedUserUtil, CalendarioHabil, ClaveTemporal, @RequiresPermission, ...
ops/               # backup.sh, restore.sh, probar_esquema.sh, exportar_baseline.sh
docs/              # GUIA_DESPLIEGUE.md, GUIA_USUARIO_ADMIN_CLUB.md
ajustes_claudia/   # Bitácora de decisiones y resúmenes de cambios por módulo (histórico)
```

Orden de capas al construir un módulo: **Model → Repository → DTO → Service → Controller**. Los controllers devuelven DTO (`toResponseDTO`), nunca entidades JPA, para evitar problemas de serialización.

## Multi-club (tenant)

- Cada club es un *tenant*. El club del usuario viaja en el claim `clubId` del JWT; `JwtTokenFilter` lo fija en `TenantContext` y Hibernate (`@TenantId` sobre `club_id`) filtra toda lectura y etiqueta toda escritura.
- El club con id `1` es la **Plataforma** (superadmin). No se puede desactivar.
- Un club suspendido deja de autenticar (el filtro rechaza tokens de clubes inactivos).
- Catálogos globales (compartidos): permisos, estados de persona, géneros y similares.
- Las llaves foráneas del esquema son compuestas `(club_id, id)` para que un registro de un club no pueda apuntar a otro.
- Procesos automáticos (facturación mensual) recorren los clubes activos con `TenantContext.runAs(clubId, ...)`.
- Prueba clave: `AislamientoEntreClubesTest` (dos clubes, API real).

Alta de un club: `POST /api/clubes` (solo superadmin) crea el club, los roles plantilla (**Administrador, Entrenador, Deportista, Acudiente**), el usuario administrador (con cambio de clave obligatorio) y, opcionalmente, una plantilla editable de Niveles 1–6 y divisiones Tiny/Mini/Youth/Junior/Senior.

## Seguridad y permisos

- Stateless: JWT en `Authorization: Bearer`, expiración por defecto 8 h, *blacklist* tras logout, `refresh-token`.
- Login por **correo**; bloqueo por intentos fallidos (5 intentos → 15 min por defecto); respuestas indistinguibles entre correo inexistente y clave incorrecta (anti-enumeración).
- Recuperación de clave: `forgot-password` nunca devuelve el token; lo entrega `PasswordResetNotifier` por correo (SMTP) o, sin SMTP, lo escribe en el log del servidor (solo desarrollo).
- Clave temporal: un administrador con `restablecer_clave_usuario` puede generar una; la persona debe cambiarla al entrar (`requiereCambioPassword`).
- Autorización por **permisos granulares** con `@RequiresPermission("...")` (o `@SinPermisoEspecifico("motivo")` en catálogos/endpoints propios). `PermisosArquitecturaTest` y `PermisosContraMigracionesTest` fallan si un endpoint queda sin declarar o usa un permiso inexistente en las migraciones.
- `AuthenticatedUserUtil` entrega el usuario actual desde el contexto de seguridad; los DTO ya no reciben `usuarioId`.
- Portal (`acceder_portal`): cada consulta valida que el deportista pertenezca al usuario (propio o a cargo como acudiente); el permiso por sí solo no da acceso a datos ajenos.
- Errores: `GlobalExceptionHandler` produce `message` controlado; `server.error.include-*` está en `never`. Cada petición lleva un `X-Request-Id` que aparece en los logs (`[rid]`).
- Auditoría: `AuditoriaAspect` registra altas/bajas de repositorios; `LogAcceso` registra login/logout/bloqueos.

## Flujos de negocio y su estado

Leyenda: ✅ completo · 🟡 completo en el back, con observaciones · ❌ falta.

| # | Flujo | Estado | Notas |
|---|---|---|---|
| 1 | Acceso y sesión (login, logout, refresh, recuperar/cambiar clave, clave temporal) | ✅ | Sin SMTP la recuperación solo escribe el enlace en el log: configurar correo antes de producción. |
| 2 | Alta y suspensión de clubes (plataforma) | ✅ | Sin edición de nombre/slug de un club ya creado. |
| 3 | Onboarding del club (`/api/onboarding`) | ✅ | 9 pasos calculados con datos reales del club. |
| 4 | Usuarios, roles y permisos por club | ✅ | Permisos y catálogos globales solo los gestiona la plataforma (`gestionar_catalogos`). |
| 5 | Personas: deportistas, entrenadores, acudientes, foto, convenio, historial de estado | ✅ | Retiro cierra inscripción, saca del grupo y desactiva usuario. |
| 6 | Reactivación de un deportista retirado | ❌ | No hay flujo: la inscripción queda inactiva y el usuario desactivado; hoy solo se cambia el estado editando al deportista. |
| 7 | Consentimientos (Ley 1581) y exigencia para historial médico | 🟡 | `CONSENTIMIENTOS_EXIGIR_SALUD=false` por defecto; activarlo cuando `/api/consentimientos/pendientes` esté en cero. |
| 8 | Organización: niveles, divisiones, categorías, temporadas, grupos, horarios | 🟡 | Categorías por nivel y criterios de evaluación son de solo lectura (sin CRUD). |
| 9 | Matrícula (inscripción + factura de inscripción + primera mensualidad) | ✅ | Descuentos de plan y convenio se combinan de forma multiplicativa. |
| 10 | Renovación de temporada por solicitud (portal → aprobar/rechazar) | ✅ | Aprobar crea la inscripción y sus facturas. |
| 11 | Facturación mensual automática y cartera en mora | ✅ | Cron diario 6:00 (Bogotá), primer día hábil con festivos de Colombia, idempotente, vence a 10 días. Sin recordatorios/notificaciones de mora. |
| 12 | Facturas manuales (UNIFORME, COMPETENCIA, EVENTO, OTRO) y anulación con motivo | ✅ | Facturas y pagos no se borran. |
| 13 | Pagos (registro, cambio de estado, anulación, saldo) | ✅ | Lock pesimista: no se puede pagar de más ni siquiera con registros simultáneos. Sin conciliación bancaria. |
| 14 | Descuentos, convenios, planes y tipos de plan | ✅ | |
| 15 | Asistencia (individual y masiva, % por deportista, riesgo) | ✅ | Registro masivo tolera fallos parciales (HTTP 207). |
| 16 | Evaluaciones de habilidades y objetivos | 🟡 | Individual, masiva, progresión y promedios. Criterios solo lectura. |
| 17 | Evaluación de rutina (rúbrica FEDECOLCHEER/IASF 2026) | ✅ | Vista previa, registro, historial por grupo y configuración de rúbrica (solo plataforma). |
| 18 | Eventos: grupos participantes, premios, resultados | 🟡 | El cobro de inscripción a un evento es manual por deportista (factura `EVENTO`); no hay generación masiva por evento. |
| 19 | Tienda: productos, variantes, movimientos, ventas | 🟡 | La venta a un miembro genera factura `TIENDA` **sin pago asociado**: una venta de contado quedará pendiente y pasará a mora a los 10 días. Falta anular venta. |
| 20 | Portal de deportista/acudiente (solo lectura) | ✅ | Facturas, pagos, asistencias, evaluaciones, objetivos, rutinas y solicitudes de temporada. |
| 21 | Dashboards (inicio, organización, personas, horarios, evaluación, finanzas, eventos, tienda, administración) | ✅ | 9 servicios, uno por permiso `ver_dash_*`. |
| 22 | Auditoría y logs de acceso | 🟡 | Hay endpoints de `logs-acceso` sin consumidor en el front. |
| 23 | Operación: migraciones, respaldos, despliegue, logs, pruebas | ✅ | Ver [Despliegue y operación](#despliegue-y-operación). |

## Reglas de negocio clave

- **Factura mensual**: se emite el primer día hábil del mes (lunes a viernes, sin festivos de Colombia) y vence a los 10 días; después está **EN MORA**. Estados de cuenta: `PENDIENTE`, `ABONO_PARCIAL`, `PAGADA`, `EN_MORA`, `ANULADA`.
- **Idempotencia**: no se factura dos veces el mismo periodo; una factura anulada de un periodo cuenta como "ya facturado". No se cobran meses anteriores a la inscripción, anteriores al inicio de la temporada ni posteriores a su fin.
- **Descuentos**: el del plan y el del convenio vigente se aplican en cadena (no se suman los porcentajes).
- **Cambio de plan**: recalcula la factura de inscripción, pero se **rechaza** si ya tiene pagos confirmados; las mensualidades ya emitidas no cambian.
- **Eliminar inscripción**: las facturas sin pagos se anulan; las que tienen pagos se conservan.
- **Retiro de un deportista**: estado "Retirado" (con historial), inscripciones activas → inactivas, membresías de grupo cerradas (`fechaFin = hoy`), usuario desactivado; las facturas pendientes se dejan visibles en cartera.
- **Pagos y facturas no se borran**: se anulan con motivo (mínimo 5 caracteres), fecha y usuario. Una factura con pagos confirmados exige anular primero esos pagos.
- **Grupos**: elegibilidad por año de nacimiento y cupo máximo de la categoría; quitar de un grupo cierra la membresía, no la borra.
- **Temporadas**: solo una activa por club; las inscripciones nuevas se asocian a ella.
- **Inventario**: el stock solo cambia por movimientos; SKU `RAT-[CAT]-[COD]-[SEQ]`; productos digitales con `requiereControlStock=false`.
- **Rúbrica de rutinas**: los topes de 0,1 por deducción (sincronización de saltos y altura de lanzamientos) están fijos en código; el resto de escalones y topes por nivel (N1–N4.2) son configurables.
- **Consentimientos**: son evidencia, solo se agregan y se revocan, nunca se editan ni se borran.

## Endpoints por dominio

Todos cuelgan de `/api`. Resumen (el detalle y el permiso de cada uno están en los controllers):

| Dominio | Rutas principales |
|---|---|
| Auth | `POST /login` · `/logout` · `/refresh-token` · `/forgot-password` · `/reset-password` · `/change-password` |
| Usuarios / Roles | `/usuario/*` (`registrar`, `listar`, `me`, `permisos`, `{id}/asignar-roles`, `{id}/clave-temporal`) · `/roles` · `/permisos` |
| Clubes | `/clubes` (+ `PATCH /{id}/activo`) · `/onboarding` |
| Personas | `/personas` · `/deportistas` (+ `foto`, `convenio`, `usuario`, `historial-estado`, `grupos-elegibles`, `validar-documento`) · `/entrenadores` · `/acudientes` (+ `deportista/{id}`, `{id}/acceso`) |
| Consentimientos | `/consentimientos` (+ `persona/{id}`, `pendientes`, `{id}/revocar`) |
| Organización | `/niveles` · `/categorias-nivel` · `/temporadas` (+ `activa`, `activar`, `cerrar`) · `/grupos` (+ `candidatos`, `agregar-deportistas`, `asignar-entrenadores`) · `/inscripciones` (+ `detalle`, `generar-cobros-mensualidad`, `{id}/facturas`) · `/solicitudes-inscripcion` |
| Horarios y asistencia | `/horarios` (+ `asignar-grupo`) · `/asistencias` (+ `masivo`, `grupo/{id}`, `deportista/{id}/porcentaje`) |
| Financiero | `/planes-pago` · `/tipos-planes-pago` · `/descuentos` · `/convenios` · `/divisiones` · `/facturas` (+ `mora`, `{id}/anular`) · `/pagos` (+ `factura/{id}/saldo`, `{id}/estado`, `{id}/anular`) · catálogos `/metodos-pago`, `/tipos-pago` |
| Evaluaciones | `/evaluaciones` (+ `masivo`, progresión, promedio) · `/objetivos` · `/criterios-evaluacion` · `/estados-objetivo` · `/evaluaciones-rutina` (+ `preview`, `grupo/{id}`) · `/rubrica-config/*` |
| Eventos | `/eventos` (+ `proximos`, `historial`) · `/grupos-evento` · `/premios-evento` · `/resultados-competencia` · `/tipos-evento` |
| Tienda | `/categorias-producto` · `/productos` · `/producto-variantes` · `/movimientos-inventario` · `/ventas` |
| Historiales | `/historial-medico` · `/historial-nivel` · `/historial-planes` |
| Portal | `/portal/yo` · `/portal/deportistas/{id}/{facturas,pagos,asistencias,evaluaciones,objetivos,rutinas,solicitudes}` · `/portal/temporadas-abiertas` · `/portal/solicitudes/{id}/cancelar` |
| Dashboard / Auditoría | `/dashboard/*` · `/auditoria` · `/logs-acceso/*` |
| Salud | `GET /actuator/health` |

## Configuración (variables de entorno)

Los secretos **no tienen valor por defecto**: si falta una obligatoria, la app no arranca.

| Variable | Obligatoria | Por defecto | Descripción |
|---|---|---|---|
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Sí | — | Conexión a MySQL |
| `JWT_SECRET` | Sí | — | Mínimo 32 bytes (`openssl rand -base64 64`) |
| `JWT_EXPIRATION_MS` | No | `28800000` (8 h) | Vigencia del token |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:3000` | Orígenes del frontend, separados por coma |
| `FRONTEND_RESET_URL` | No | `http://localhost:3000/reset-password` | Base del enlace de recuperación |
| `APP_BOOTSTRAP_ADMIN_USERNAME` / `_EMAIL` / `_PASSWORD` | Para crear el superadmin | `superadmin` / vacío / vacío | Se crea solo si no existe y la clave tiene ≥ 10 caracteres |
| `AUTH_MAX_LOGIN_ATTEMPTS` / `AUTH_LOCK_DURATION_MINUTES` | No | `5` / `15` | Bloqueo por intentos fallidos |
| `AUTH_RESET_TOKEN_EXPIRATION_MINUTES` | No | `15` | Vigencia del token de recuperación |
| `CONSENTIMIENTOS_EXIGIR_SALUD` | No | `false` | Exigir consentimiento `DATOS_SALUD` para historial médico |
| `FACTURACION_AUTOMATICA` | No | `true` | Cron de facturación mensual (pensado para **una sola instancia**) |
| `JPA_DDL_AUTO` | No | `validate` | Nunca `update` fuera de tu máquina |
| `FLYWAY_ENABLED` | No | `true` | |
| `MAIL_FROM` + `spring.mail.*` | No | `no-reply@cheermanager.local` | SMTP para recuperación de clave |
| `FORWARD_HEADERS_STRATEGY` | No | `none` | `framework` detrás de Nginx/Caddy/Railway |
| `UPLOAD_DIR` | No | `./uploads` | Fotos y documentos (máx. 10 MB) |
| `LOG_LEVEL_ROOT` / `LOG_LEVEL_APP` | No | `INFO` | No subir a `DEBUG` `org.hibernate.SQL` ni Spring Security en producción |
| `PORT` | No | `8081` | |

Para desarrollo local copia `application-local.properties.example` a `application-local.properties` (ignorado por git) y arranca con el perfil `local`.

## Ejecución local

```bash
git clone https://github.com/Varcal-dev/cheermanager_back
cd cheermanager_back

# 1. Crear una base MySQL vacía (Flyway crea el esquema al arrancar)
# 2. Definir DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET (y el superadmin, si lo quieres)

./mvnw test                                   # pruebas
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

API en `http://localhost:8081`. Con Docker: `cp .env.example .env && docker compose up -d --build`.

## Base de datos y migraciones

- **Flyway es la fuente de verdad del esquema**; Hibernate solo valida (`ddl-auto=validate`).
- `V1` esquema base · `V2` datos iniciales (roles, permisos, catálogos) · `V3` alineación con el modelo JPA · `V4` booleanos a `BIT` · `V5` cartera (vencimiento, mora, facturas manuales, anulaciones) · `V6` portal de acudiente/deportista y solicitudes de inscripción.
- **Nunca edites una migración ya aplicada**: crea la siguiente (`V7`, ...).
- Una BD preexistente sin historial de Flyway se marca como versión 2 (`baseline-version=2`) y solo aplica `V3` en adelante. Haz respaldo antes.
- Antes de migrar una base real: `ops/probar_esquema.sh` carga las migraciones en una BD desechable y verifica el aislamiento entre clubes.

## Despliegue y operación

Guía completa en [`docs/GUIA_DESPLIEGUE.md`](docs/GUIA_DESPLIEGUE.md). Resumen:

1. VPS con Docker + HTTPS delante de la API (Caddy o Nginx).
2. `docker compose up -d --build`: MySQL 8 (puerto 3306 no publicado) + API en `127.0.0.1:8081`.
3. Entrar como `superadmin`, cambiar la clave y crear el primer club con `POST /api/clubes`.
4. Respaldos: `ops/backup.sh` (cron diario, con rotación) y `ops/restore.sh`. **Prueba una restauración real.**
5. Logs: cada línea lleva `[rid]`; el usuario puede reportar el `X-Request-Id` del error.
6. Salud: `GET /actuator/health`.

Guía para quien administra un club: [`docs/GUIA_USUARIO_ADMIN_CLUB.md`](docs/GUIA_USUARIO_ADMIN_CLUB.md).

## Pruebas

| Clase | Qué cubre |
|---|---|
| `AislamientoEntreClubesTest`, `ReferenciasClubTest` | Un club nunca ve ni toca datos de otro |
| `PermisosArquitecturaTest`, `PermisosContraMigracionesTest` | Todo endpoint declara permiso y existe en las migraciones |
| `AuthServiceTest` | Autenticación |
| `FacturaEstadoCuentaTest`, `CalendarioHabilTest` | Estados de cuenta, mora y días hábiles |
| `ConsentimientoServiceTest` | Otorgar, revocar y exigir consentimiento |
| `MySqlRealIT` | Pruebas de concurrencia contra MySQL real (Testcontainers) |

No hay pruebas automatizadas de los módulos de evaluación de rutinas, inventario/ventas ni inscripciones, ni CI configurado.

## Pendientes conocidos

1. **Ventas de contado a miembros**: crear el pago al registrar la venta (o permitir marcarla como pagada) para que no figure en mora.
2. **Anular venta** (`anular_venta`): reversar con movimientos de entrada compensatorios; hoy no tiene lógica.
3. **Reactivar deportista retirado**: reabrir estado, usuario e inscripción en un solo flujo.
4. **SMTP real** y **recordatorios de mora** (la entidad `Notificacion` existe pero no se usa).
5. **CRUD de criterios de evaluación y de categorías por nivel** para que cada club los personalice.
6. **Conciliación / cierre de caja** de pagos.
7. **Facturación de evento por lote** a los deportistas de los grupos participantes.
8. Diferido a propósito: unificar tipos `Long`/`Integer` en repositorios (limpieza aparte, para no romper en cascada).
9. Calidad: OpenAPI/Swagger, CI/CD, más pruebas (rutinas, inventario, inscripciones), y registro de seguimiento de `ajustes_claudia/` a un `CHANGELOG`.