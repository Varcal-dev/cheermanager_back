# CheerManager — Backend

API REST (Spring Boot) de **CheerManager**, plataforma **multi-club** para gestionar clubes de porrismo (cheerleading): deportistas y acudientes, temporadas, grupos y horarios, inscripciones, facturación y pagos, caja y conciliación, asistencia, evaluaciones (incluida la rúbrica FEDECOLCHEER / IASF 2026), eventos y competencias, tienda e inventario, portal de solo lectura, suscripciones SaaS por club, cumplimiento de la Ley 1581 de 2012 y dashboards.

Lo consume el frontend **CheerManager Front** (Next.js), proyecto hermano.

## Contenido

- [Stack](#stack)
- [Arquitectura](#arquitectura)
- [Multi-club (tenant)](#multi-club-tenant)
- [Seguridad y permisos](#seguridad-y-permisos)
- [Roles plantilla](#roles-plantilla)
- [Suscripción SaaS y bloque legal](#suscripción-saas-y-bloque-legal)
- [Flujos de negocio y su estado](#flujos-de-negocio-y-su-estado)
- [Reglas de negocio clave](#reglas-de-negocio-clave)
- [Endpoints por dominio](#endpoints-por-dominio)
- [Configuración (variables de entorno)](#configuración-variables-de-entorno)
- [Ejecución local](#ejecución-local)
- [Base de datos y migraciones](#base-de-datos-y-migraciones)
- [Despliegue y operación](#despliegue-y-operación)
- [Pruebas y CI](#pruebas-y-ci)
- [Pendientes conocidos](#pendientes-conocidos)

## Stack

| Componente | Detalle |
|---|---|
| Lenguaje | Java 21 (`pom.xml` y `Dockerfile` alineados) |
| Framework | Spring Boot 3.4.5 (Web, Data JPA, Security, Validation, AOP, Mail, Actuator), Lombok |
| Base de datos | MySQL 8 |
| Migraciones | Flyway (`V1`–`V21`, sin `V17`, en `src/main/resources/db/migration`) |
| Seguridad | Spring Security stateless + JWT (`jjwt` 0.11.5), BCrypt |
| Build | Maven (`mvnw` / `mvnw.cmd`) |
| Contenedores | `Dockerfile` multi-etapa (Temurin 21, usuario no root, healthcheck) + `docker-compose.yml` (MySQL + API) |
| Pruebas | JUnit 5, Spring Security Test, H2 y Testcontainers/MySQL real |
| CI | GitHub Actions (`.github/workflows/ci.yml`) |
| Puerto | `8081` (variable `PORT`) |

Cifras: ~70 controllers, 369 endpoints, 83 clases en `Models/`, 76 repositorios, 89 clases en `Service/`, 31 clases de prueba.

## Arquitectura

```
src/main/java/com/varcal/cheermanager/
├── Controller/    # REST por dominio: Auth, Caja, Clubes, Consentimientos, Dashboard, Evaluaciones (+Rutina),
│                  #   Eventos, Financiero, Funcionalidad, Historiales, Horario_Asistencia, Importacion,
│                  #   Inventario, Legal, Notificaciones, Org_dep, Persona, Portal
├── DTO/           # Request/response, mismo agrupamiento
├── Models/        # Entidades JPA (casi todas con club_id / @TenantId) y enums de dominio
├── repository/    # Spring Data JPA
├── Service/       # Lógica de negocio (+ Evaluaciones/Rutina/Calculadoras, Financiero/FacturacionMensualJob,
│                  #   Caja/*, Notificaciones/*Sender)
├── security/      # JwtUtil, JwtTokenFilter, TokenBlacklistService, TokenRevocadoJdbcAlmacen
├── tenant/        # TenantContext, TenantConfig, TenantIdentifierResolver, ReferenciasClub, TenantLookupRepository
├── config/        # SecurityConfig, WebConfig, PermissionInterceptor, DataInitializer,
│                  #   GlobalExceptionHandler, RequestIdFilter
├── aspect/        # AuditoriaAspect (registra altas/bajas de repositorios)
└── Utils/         # AuthenticatedUserUtil, CalendarioHabil, ClaveTemporal, @RequiresPermission, @SinPermisoEspecifico, ...
ops/               # backup, restore, probar_restauracion, verificar_salud, alertar, probar_esquema, exportar_baseline
docs/              # GUIA_DESPLIEGUE.md, GUIA_OPERACION.md, GUIA_USUARIO_ADMIN_CLUB.md
ajustes_claudia/   # Bitácora histórica de decisiones y resúmenes de cambios por módulo
```

Orden de capas al construir un módulo: **Model → Repository → DTO → Service → Controller**. Los controllers devuelven DTO, nunca entidades JPA.

## Multi-club (tenant)

- Cada club es un *tenant*. El `clubId` viaja en el JWT; `JwtTokenFilter` lo fija en `TenantContext` y Hibernate (`@TenantId` sobre `club_id`) filtra toda lectura y etiqueta toda escritura.
- El club con id `1` es la **Plataforma** (superadmin). No se puede desactivar.
- Un club suspendido deja de autenticar (el filtro rechaza tokens de clubes inactivos).
- Catálogos globales: permisos, planes SaaS, estados de persona, géneros, rúbrica de rutinas y similares.
- Las llaves foráneas son compuestas `(club_id, id)`: un registro de un club no puede apuntar a otro.
- Los procesos automáticos recorren los clubes activos con `TenantContext.runAs(clubId, ...)`.
- Pruebas clave: `AislamientoEntreClubesTest` y `ops/probar_esquema.sh`.

Alta de un club: `POST /api/clubes` (solo superadmin) crea el club, los roles plantilla, el usuario administrador (con cambio de clave obligatorio) y, opcionalmente, una plantilla editable de Niveles 1–6 y divisiones Tiny/Mini/Youth/Junior/Senior. `GET /api/onboarding` devuelve la lista de 9 pasos de puesta en marcha calculada con datos reales del club.

## Seguridad y permisos

- Stateless: JWT en `Authorization: Bearer`, expiración por defecto 8 h, `refresh-token` (revalida al usuario, recalcula permisos y revoca el token anterior).
- **Tokens revocados persistentes** (V14): logout y renovación guardan el hash SHA-256 del token en base de datos, así que un reinicio no los revive. Un job horario purga los vencidos.
- Login por **correo**; bloqueo por intentos fallidos (5 intentos → 15 min por defecto); respuestas indistinguibles entre correo inexistente y clave incorrecta.
- Recuperación de clave: `forgot-password` nunca devuelve el token; lo entrega `PasswordResetNotifier` por correo (SMTP) o, sin SMTP, lo escribe en el log (solo desarrollo).
- Clave temporal: con `restablecer_clave_usuario` se puede generar una; la persona debe cambiarla al entrar (`requiereCambioPassword`).
- Autorización por **permisos granulares** con `@RequiresPermission("...")` (o `@SinPermisoEspecifico("motivo")`). `PermisosArquitecturaTest` y `PermisosContraMigracionesTest` fallan si un endpoint queda sin declarar o usa un permiso inexistente en las migraciones.
- Portal (`acceder_portal`): cada consulta valida que el deportista pertenezca al usuario (propio o a cargo como acudiente).
- Endurecimiento: CSRF desactivado (sin cookies de sesión), `X-Frame-Options: deny`, `nosniff`, CORS limitado a `CORS_ALLOWED_ORIGINS`, Actuator solo con `health`.
- Errores: `GlobalExceptionHandler` produce `message` controlado; `server.error.include-*` está en `never`. Cada petición lleva `X-Request-Id`, visible en los logs como `[rid]`.
- Auditoría: `AuditoriaAspect` registra altas/bajas; `LogAcceso` registra login, logout y bloqueos.
- Códigos relevantes: `401` sin sesión, `402` suscripción vencida (escrituras), `428` términos legales pendientes (escrituras), `207` registros masivos con fallos parciales.

## Roles plantilla

Los roles son por club. `RolesPlantilla` los crea al dar de alta un club con los mismos permisos que los roles de Ratels: **Administrador, Director Deportivo, Coordinador Administrativo, Entrenador, Asistente, Tesorero, Encargado de Inventario, Deportista y Acudiente** (estos dos solo con `acceder_portal`). El **Super Administrador** pertenece a la plataforma.

Permisos reservados a la plataforma: `gestionar_clubes`, `gestionar_catalogos`, `configurar_rubrica`, `administrar_roles`, `administrar_permisos`, `configurar_sistema`, `gestionar_parametros`, `importar_datos`, `exportar_datos`, `ver_dashboard_global`, `ver_logs_sistema`.

Permisos propios del club añadidos por migración: `importar_deportistas`, `exportar_listados` (V13), `ver_caja`, `cerrar_caja`, `reabrir_caja`, `conciliar_pagos` (V15), `gestionar_recordatorios_mora` (V9), `exportar_datos_titular` (V20) y `anonimizar_titular` (V21).

## Suscripción SaaS y bloque legal

**Planes** (`planes_saas`, V11/V12):

| Plan | Deportistas activos | Precio mensual (COP) |
|---|---|---|
| Start | 75 | 99.900 |
| Pro | 200 | 199.900 |
| Elite | 500 | 349.900 |
| Enterprise | sin tope de plan | desde 599.900 (por contrato) |

Las ampliaciones se aplican subiendo el *límite propio* de deportistas del club y registrando el cobro (`/api/clubes/{id}/suscripcion/pagos`).

**Vigencia** (`VigenciaSuscripcion`): `ILIMITADA`, `VIGENTE`, `EN_GRACIA` (vencida dentro de los días de gracia; funciona con aviso) y `SOLO_LECTURA` (vencida o cancelada: GET permitido, el resto responde `402`). Quedan exentos: logout, refresh-token, change-password, `/api/portal/notificaciones` y `/api/legal`. El club consulta su estado en `GET /api/suscripcion`.

**Aceptación legal por club** (V19): cada club debe aceptar Términos y Condiciones y Política de Tratamiento de Datos por versión (vigente `2026-11`, constante en `LegalService`). Mientras esté pendiente, las escrituras responden `428`. Se guarda quién aceptó, cuándo, desde qué IP y qué versión.

**Derechos del titular (Ley 1581):** consentimientos (solo se agregan y se revocan), copia de datos (`GET /api/personas/{id}/copia-datos`) y anonimización irreversible (`POST /api/personas/{id}/anonimizar`, con constancia en `anonimizaciones`).

## Flujos de negocio y su estado

Leyenda: ✅ completo · 🟡 completo en el back, con observaciones · ❌ falta.

| # | Flujo | Estado | Notas |
|---|---|---|---|
| 1 | Acceso y sesión (login, logout, refresh, recuperar/cambiar clave, clave temporal) | ✅ | Sin SMTP la recuperación solo escribe el enlace en el log: configurar correo antes de producción. |
| 2 | Alta y suspensión de clubes (plataforma) | ✅ | Sin edición de nombre/slug de un club ya creado. |
| 3 | Planes y suscripciones por club | ✅ | Solo lectura al vencer; días de gracia; registro de pagos de suscripción. |
| 4 | Aceptación legal por club | ✅ | Bloquea escrituras con `428` hasta aceptar la versión vigente. |
| 5 | Onboarding del club | ✅ | 9 pasos calculados con datos reales. |
| 6 | Usuarios, roles y permisos por club | ✅ | Permisos y catálogos globales solo los gestiona la plataforma. |
| 7 | Personas: deportistas, entrenadores, acudientes, foto, convenio, historial de estado | ✅ | Retiro cierra inscripción, saca del grupo y desactiva usuario. |
| 8 | Reactivación de un deportista retirado | 🟡 | Vuelve a Activo; la inscripción y el grupo se recuperan con una matrícula nueva. |
| 9 | Consentimientos (Ley 1581), copia de datos y anonimización | 🟡 | `CONSENTIMIENTOS_EXIGIR_SALUD=false` por defecto; activarlo cuando `/api/consentimientos/pendientes` esté en cero. |
| 10 | Importación de deportistas (CSV) y exportes de listados | ✅ | Plantilla, validación previa e importación; exportes siguen funcionando en solo lectura. |
| 11 | Organización: niveles, divisiones, categorías, temporadas, grupos, horarios | 🟡 | Las categorías por nivel siguen de solo lectura (sin CRUD). |
| 12 | Matrícula (inscripción + factura + primera mensualidad) | ✅ | Descuentos de plan y convenio se combinan de forma multiplicativa. |
| 13 | Renovación de temporada por solicitud (portal → aprobar/rechazar) | ✅ | Aprobar crea la inscripción y sus facturas. |
| 14 | Facturación mensual automática y cartera en mora | ✅ | Cron diario 6:00 (Bogotá), primer día hábil con festivos de Colombia, idempotente, vence a 10 días. |
| 15 | Facturas manuales (UNIFORME, COMPETENCIA, EVENTO, OTRO) y anulación con motivo | ✅ | Facturas y pagos no se borran. |
| 16 | Pagos (registro, estado, anulación, saldo) | ✅ | Lock pesimista: no se puede pagar de más ni con registros simultáneos. |
| 17 | Cierre de caja y conciliación bancaria | ✅ | Un cierre por club y día; conciliación contra extracto (`/api/caja`, `/api/conciliacion`). |
| 18 | Recordatorios de mora (sistema · correo · WhatsApp) | ✅ | `/api/recordatorios-mora`; envío automático diario 8:00 (Bogotá). |
| 19 | Descuentos, convenios, planes y tipos de plan | ✅ | |
| 20 | Asistencia (individual y masiva, % por deportista, riesgo) | ✅ | Registro masivo tolera fallos parciales (HTTP 207). |
| 21 | Evaluaciones de habilidades, objetivos y criterios | ✅ | Individual, masiva, progresión y promedios; CRUD de criterios. |
| 22 | Evaluación de rutina (rúbrica FEDECOLCHEER/IASF 2026) | ✅ | Elite N1–N4, Prep y Novice; vista previa, registro, historial y configuración (solo plataforma). |
| 23 | Eventos: grupos, premios, resultados y cobro por lote | ✅ | `POST /api/eventos/{id}/cobro-lote` (`crear_factura`), con `simular=true`, repetible sin duplicar. |
| 24 | Tienda: productos, variantes, movimientos, ventas | 🟡 | Venta a miembro genera factura `TIENDA` y pago; se puede anular con motivo devolviendo stock. |
| 25 | Portal de deportista/acudiente (solo lectura) | ✅ | Facturas, pagos, asistencias, evaluaciones, objetivos, rutinas, solicitudes y autorizaciones. |
| 26 | Dashboards | ✅ | 9 servicios, uno por permiso `ver_dash_*`. |
| 27 | Auditoría y logs de acceso | 🟡 | Hay endpoints de `logs-acceso` sin consumidor en el front. |
| 28 | Operación: migraciones, respaldos, alertas, despliegue, pruebas | ✅ | Ver [Despliegue y operación](#despliegue-y-operación). |

## Reglas de negocio clave

- **Factura mensual**: se emite el primer día hábil del mes (lunes a viernes, sin festivos de Colombia) y vence a los 10 días; después está **EN MORA**. Estados: `PENDIENTE`, `ABONO_PARCIAL`, `PAGADA`, `EN_MORA`, `ANULADA`.
- **Idempotencia**: no se factura dos veces el mismo periodo; una factura anulada cuenta como «ya facturado». No se cobran meses anteriores a la inscripción ni al inicio de la temporada, ni posteriores a su fin.
- **Descuentos**: el del plan y el del convenio vigente se aplican en cadena (no se suman los porcentajes).
- **Cambio de plan**: recalcula la factura de inscripción, pero se rechaza si ya tiene pagos confirmados.
- **Eliminar inscripción**: las facturas sin pagos se anulan; las que tienen pagos se conservan.
- **Retiro**: estado «Retirado» con historial; inscripciones activas pasan a inactivas, membresías de grupo se cierran, usuario desactivado; las facturas pendientes quedan en cartera. «Retirado» solo se entra con Retirar y se sale con Reactivar.
- **Pagos y facturas no se borran**: se anulan con motivo (mínimo 5 caracteres), fecha y usuario. Una factura con pagos confirmados exige anular primero esos pagos.
- **Caja**: con el día cerrado no se registran, confirman ni anulan pagos «Pagado» de esa fecha; hay que reabrirlo.
- **Grupos**: elegibilidad por año de nacimiento y cupo máximo de la categoría; quitar de un grupo cierra la membresía, no la borra.
- **Temporadas**: solo una activa por club.
- **Anular una venta**: motivo obligatorio; devuelve el stock con movimientos de entrada y anula su pago y su factura propia. Las ventas anuladas no cuentan en los dashboards.
- **Inventario**: el stock solo cambia por movimientos; SKU `RAT-[CAT]-[COD]-[SEQ]`; productos digitales con `requiereControlStock=false`.
- **Rúbrica de rutinas**: global (sin `club_id`); un cambio afecta las evaluaciones nuevas de todos los clubes, las guardadas conservan su cálculo. Los topes de 0,1 por deducción están fijos en código.
- **Consentimientos**: son evidencia, solo se agregan y se revocan.
- **Recordatorios de mora**: un mensaje por persona con todas sus facturas; no repite la misma factura por el mismo canal antes de N días (7 por defecto).

## Endpoints por dominio

Todos cuelgan de `/api`. El detalle y el permiso de cada uno están en los controllers.

| Dominio | Rutas principales |
|---|---|
| Auth | `POST /login` · `/logout` · `/refresh-token` · `/forgot-password` · `/reset-password` · `/change-password` |
| Usuarios / Roles | `/usuario/*` (`registrar`, `listar`, `me`, `permisos`, `{id}/asignar-roles`, `{id}/clave-temporal`) · `/roles` · `/permisos` |
| Clubes y suscripción | `/clubes` (+ `PATCH /{id}/activo`) · `/onboarding` · `/clubes/planes` · `/clubes/suscripciones` · `/clubes/{id}/suscripcion` (+ `/pagos`) · `GET /suscripcion` |
| Legal | `/legal/estado` · `/legal/historial` · `POST /legal/aceptar` |
| Personas | `/personas` · `/deportistas` (+ `foto`, `convenio`, `usuario`, `reactivar`, `historial-estado`, `grupos-elegibles`, `validar-documento`) · `/entrenadores` · `/acudientes` |
| Titular (Ley 1581) | `/consentimientos` (+ `persona/{id}`, `pendientes`, `{id}/revocar`) · `/personas/{id}/copia-datos` · `/personas/{id}/anonimizar` |
| Organización | `/niveles` · `/categorias-nivel` · `/temporadas` (+ `activa`, `activar`, `cerrar`) · `/grupos` · `/inscripciones` · `/solicitudes-inscripcion` · `/tipo-grupos` · `/dias-semana` |
| Horarios y asistencia | `/horarios` (+ `asignar-grupo`) · `/asistencias` (+ `masivo`, `grupo/{id}`, `deportista/{id}/porcentaje`) · `/estados-asistencia` |
| Financiero | `/planes-pago` · `/tipos-planes-pago` · `/descuentos` · `/convenios` · `/divisiones` · `/facturas` (+ `mora`, `{id}/anular`) · `/pagos` (+ `factura/{id}/saldo`, `{id}/estado`, `{id}/anular`) · `/metodos-pago` · `/tipos-pago` |
| Caja y conciliación | `/caja/resumen` · `/caja/cierres` (+ `{fecha}/reabrir`) · `/conciliacion/pagos` · `/conciliacion/marcar` · `/conciliacion/pagos/{id}/referencia` · `/conciliacion/extracto` |
| Recordatorios | `/recordatorios-mora` · `/portal/notificaciones` |
| Evaluaciones | `/evaluaciones` · `/objetivos` · `/criterios-evaluacion` · `/tipos-categoria` · `/estados-objetivo` · `/evaluaciones-rutina` (+ `preview`, `grupo/{id}`) · `/rubrica-config/*` |
| Eventos | `/eventos` (+ `proximos`, `historial`, `{id}/cobro-lote`) · `/grupos-evento` · `/premios-evento` · `/resultados-competencia` · `/tipos-evento` |
| Tienda | `/categorias-producto` · `/productos` · `/producto-variantes` · `/movimientos-inventario` · `/ventas` (+ `{id}/anular`) |
| Historiales | `/historial-medico` · `/historial-nivel` · `/historial-planes` |
| Importación / exportes | `/importaciones/deportistas/{plantilla,validar}` · `POST /importaciones/deportistas` · `/exportes/deportistas` · `/exportes/acudientes` |
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
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:3000` | Orígenes del frontend, separados por coma, sin barra final |
| `FRONTEND_RESET_URL` | No | `http://localhost:3000/reset-password` | Base del enlace de recuperación |
| `APP_BOOTSTRAP_ADMIN_USERNAME` / `_EMAIL` / `_PASSWORD` | Para crear el superadmin | `superadmin` / vacío / vacío | Se crea solo si no existe y la clave tiene ≥ 10 caracteres |
| `AUTH_MAX_LOGIN_ATTEMPTS` / `AUTH_LOCK_DURATION_MINUTES` | No | `5` / `15` | Bloqueo por intentos fallidos |
| `AUTH_RESET_TOKEN_EXPIRATION_MINUTES` | No | `15` | Vigencia del token de recuperación |
| `CONSENTIMIENTOS_EXIGIR_SALUD` | No | `false` | Exigir consentimiento `DATOS_SALUD` para historial médico |
| `FACTURACION_AUTOMATICA` | No | `true` | Cron de facturación mensual (pensado para **una sola instancia**) |
| `RECORDATORIOS_AUTOMATICOS` | No | `true` | `false` apaga el envío automático en todo el servidor |
| `MAIL_FROM` + `SPRING_MAIL_HOST/PORT/USERNAME/PASSWORD` (+ `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH`, `..._STARTTLS_ENABLE`) | No | `no-reply@cheermanager.local` | SMTP: recuperación de clave y recordatorios |
| `WHATSAPP_TOKEN`, `WHATSAPP_PHONE_NUMBER_ID`, `WHATSAPP_TEMPLATE`, `WHATSAPP_LANGUAGE` | No | plantilla `recordatorio_mora`, idioma `es` | WhatsApp Business Cloud API (plantilla aprobada por Meta con 3 variables). Sin esto, se generan enlaces `wa.me` |
| `FORWARD_HEADERS_STRATEGY` | No | `none` | `framework` detrás de Nginx/Caddy/Railway |
| `UPLOAD_DIR` | No | `./uploads` | Fotos y documentos (máx. 10 MB) |
| `JPA_DDL_AUTO` / `FLYWAY_ENABLED` | No | `validate` / `true` | Nunca `update` fuera de tu máquina |
| `LOG_LEVEL_ROOT` / `LOG_LEVEL_APP` | No | `INFO` | No subir a `DEBUG` Hibernate SQL ni Spring Security en producción |
| `PORT` | No | `8081` | |

`env.example` es la plantilla para `.env` (con Docker Compose).

## Ejecución local

Requisitos: JDK 21 y MySQL 8.

```bash
git clone https://github.com/Varcal-dev/cheermanager_back
cd cheermanager_back

# 1. Crear una base MySQL vacía (Flyway crea el esquema al arrancar)
# 2. Definir DB_URL, DB_USERNAME, DB_PASSWORD y JWT_SECRET (y el superadmin, si lo quieres)
#    como variables de entorno o en application-local.properties (ignorado por git)

./mvnw test
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

API en `http://localhost:8081`. Con Docker: `cp env.example .env`, completar y `docker compose up -d --build`.

## Base de datos y migraciones

- **Flyway es la fuente de verdad del esquema**; Hibernate solo valida (`ddl-auto=validate`).
- **Nunca edites una migración ya aplicada**: crea la siguiente (la próxima es `V22`).
- Una BD preexistente sin historial de Flyway se marca como versión 2 (`baseline-version=2`) y solo aplica `V3` en adelante. Haz respaldo antes.
- Antes de migrar una base real: `ops/probar_esquema.sh` carga las migraciones en una BD desechable y verifica el aislamiento entre clubes.

| Migración | Contenido |
|---|---|
| V1 | Esquema base |
| V2 | Datos iniciales: roles, permisos, catálogos FEDECOLCHEER/IASF 2026, rúbrica Elite |
| V3 | Alineación del esquema con el modelo JPA |
| V4 | Booleanos a `BIT(1)` |
| V5 | Cartera: vencimiento, mora, facturas manuales, anulaciones |
| V6 | Portal de acudiente/deportista y solicitudes de inscripción |
| V7 | Anulación de ventas |
| V8 | Permiso de editar criterios al Director Deportivo |
| V9 | Recordatorios de mora |
| V10 | Permisos de menú y apoyo por rol |
| V11 | Planes y suscripción por club |
| V12 | Planes comerciales (Start/Pro/Elite/Enterprise) y finanzas para el Coordinador |
| V13 | Permisos de importación y exportes |
| V14 | Tokens revocados persistentes |
| V15 | Cierre de caja y conciliación |
| V16 | Rúbrica 2026 completa (Prep y Novice) y ajustes de Elite |
| V18 | Gimnasia estática: dificultad por número de habilidad |
| V19 | Aceptación de términos y política por club |
| V20 | Permiso `exportar_datos_titular` |
| V21 | Anonimización de titulares |

La numeración salta de V16 a V18 (no existe V17).

## Despliegue y operación

Guías completas en [`docs/GUIA_DESPLIEGUE.md`](docs/GUIA_DESPLIEGUE.md), [`docs/GUIA_OPERACION.md`](docs/GUIA_OPERACION.md) y, para quien administra un club, [`docs/GUIA_USUARIO_ADMIN_CLUB.md`](docs/GUIA_USUARIO_ADMIN_CLUB.md). Resumen:

1. VPS con Docker y HTTPS delante de la API (Caddy o Nginx, con `FORWARD_HEADERS_STRATEGY=framework`).
2. `docker compose up -d --build`: MySQL 8 (puerto 3306 no publicado, volumen `dbdata`) + API en `127.0.0.1:8081`. Logs de Docker con rotación.
3. Entrar como `superadmin`, cambiar la clave y crear el primer club con `POST /api/clubes`.
4. Salud: `GET /actuator/health`. Cada línea de log lleva `[rid]`.

**Procesos programados** (zona `America/Bogota`): `FacturacionMensualJob` diario 6:00 · `RecordatoriosMoraJob` diario 8:00 · purga de tokens revocados cada hora.

**Scripts de `ops/`:**

| Script | Función |
|---|---|
| `backup.sh` | Respaldo lógico con rotación (`KEEP_DAYS`, 14 por defecto) y copia externa opcional con rclone |
| `restore.sh` | Restaura un respaldo (pide confirmar el nombre de la base) |
| `probar_restauracion.sh` | Restaura el último respaldo en una base de prueba y la verifica |
| `verificar_salud.sh` | Vigilante: API, respaldo reciente, prueba de restauración reciente y disco |
| `alertar.sh` | Alertas por webhook (Slack/Discord/Mattermost) o Telegram |
| `probar_esquema.sh` | Prueba las migraciones contra un MySQL desechable |
| `exportar_baseline.sh` | Genera un baseline del esquema desde una base existente |
| `crontab.ejemplo` / `ops.env.ejemplo` | Ejemplo de cron (respaldo 2:30, prueba de restauración mensual, vigilante cada 5 min) |

Un respaldo que nunca se restauró no es un respaldo: prueba una restauración real.

## Pruebas y CI

| Área | Clases |
|---|---|
| Aislamiento entre clubes | `AislamientoEntreClubesTest`, `ReferenciasClubTest` |
| Permisos | `PermisosArquitecturaTest`, `PermisosContraMigracionesTest` |
| Autenticación y tokens | `AuthServiceTest`, `TokenBlacklistServiceTest` |
| Suscripción y roles | `VigenciaSuscripcionTest`, `SuscripcionBajaPlanTest`, `RolesPlantillaTest` |
| Finanzas | `Factura*Test`, `FacturacionMensualJobTest`, `PagoServiceTest`, `CobroEventoServiceTest`, `CalendarioHabilTest` |
| Caja y conciliación | `CajaReglasTest`, `ConciliacionMatcherTest`, `ExtractoParseoTest` |
| Cumplimiento | `ConsentimientoServiceTest`, `AutorizacionPortalServiceTest`, `LegalServiceTest`, `AnonimizacionServiceTest`, `CopiaDatosServiceTest` |
| Importación y notificaciones | `CsvUtilTest`, `ImportacionParseoTest`, `RecordatorioMoraServiceTest` |
| Evaluación e inventario | `RubricaValidacionTest`, `EvaluacionReglasTest`, `VentaServiceTest` |
| MySQL real | `MySqlRealIT` (Testcontainers; se omite sin Docker) |

**CI** (GitHub Actions): `mvn verify` con Java 21, construcción de la imagen Docker y revisión de `ops/*.sh` con `bash -n` y `shellcheck`.

## Pendientes conocidos

1. **CRUD de categorías por nivel** para que cada club las personalice.
2. Unificar tipos `Long`/`Integer` en repositorios (diferido a propósito).
3. OpenAPI/Swagger y un `CHANGELOG` que consolide `ajustes_claudia/`.
4. Más pruebas: evaluación de rutinas (cálculo completo), inscripciones y matrícula.
5. Operación: contratar el SMTP y, si se quiere envío automático por WhatsApp, abrir la cuenta de WhatsApp Business y aprobar la plantilla.
6. Revisar que `Utils/PasswordHashTest.java` (en `src/main`) no sea código de apoyo olvidado, y la referencia a `FASE4_LEEME.md` en `application.properties` (el archivo no está en el repo).