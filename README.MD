# CheerManager — Backend

API REST desarrollada con **Spring Boot** para la gestión integral de un club deportivo de porrismo (cheerleading): personas, grupos, niveles y categorías, inscripciones, pagos y facturación, horarios y asistencia, evaluaciones, eventos, inventario/ventas, y autenticación con roles y permisos.

Este backend es consumido por el frontend **CheerManager** (Next.js), disponible como proyecto hermano.

## Tabla de contenido

- [Stack técnico](#stack-técnico)
- [Estructura del proyecto](#estructura-del-proyecto)
- [Módulos funcionales](#módulos-funcionales)
- [Autenticación y seguridad](#autenticación-y-seguridad)
- [Requisitos previos](#requisitos-previos)
- [Configuración](#configuración)
- [Ejecución local](#ejecución-local)
- [Endpoints principales](#endpoints-principales)
- [Base de datos](#base-de-datos)
- [Notas de desarrollo](#notas-de-desarrollo)

## Stack técnico

| Componente | Detalle |
|---|---|
| Lenguaje | Java 24 |
| Framework | Spring Boot 3.4.5 |
| Persistencia | Spring Data JPA / Hibernate (dialect MySQL8) |
| Base de datos | MySQL 8+ |
| Seguridad | Spring Security + JWT (`jjwt` 0.11.5) |
| Build | Maven (con Maven Wrapper `mvnw` / `mvnw.cmd`) |
| Puerto por defecto | `8081` |

## Estructura del proyecto

```
src/main/java/com/varcal/cheermanager/
├── Controller/          # Controladores REST (@RestController), agrupados por dominio
│   ├── Auth/            # Login, roles, permisos
│   ├── Dashboard/       # Resúmenes/KPIs para el panel principal
│   ├── Evaluaciones/    # Evaluaciones, criterios, objetivos, rutinas y rúbricas
│   ├── Eventos/         # Eventos, grupos en evento, premios, resultados
│   ├── Financiero/      # Convenios, descuentos, divisiones, facturas, pagos, planes
│   ├── Funcionalidad/   # Logs de acceso / auditoría
│   ├── Historiales/     # Historial médico, de niveles y de planes del deportista
│   ├── Horario_Asistencia/  # Horarios y asistencia
│   ├── Inventario/      # Categorías, productos, variantes, movimientos, ventas
│   ├── Org_dep/         # Organización deportiva: niveles, categorías, grupos, inscripciones
│   └── Persona/         # Personas, deportistas, entrenadores, usuarios
├── DTO/                 # Objetos de transferencia (request/response), mismo agrupamiento por dominio
├── Models/              # Entidades JPA
├── repository/          # Repositorios Spring Data JPA
├── Service/             # Lógica de negocio
├── security/            # JwtUtil, JwtTokenFilter, TokenBlacklistService
├── config/              # SecurityConfig, WebConfig, PermissionInterceptor, DataInitializer
├── Utils/                # Utilidades comunes
└── CheermanagerApplication.java
```

También existe una carpeta `ajustes_claudia/` con documentos de seguimiento (resúmenes de cambios, estado consolidado del proyecto, propuestas de KPIs, cabos sueltos pendientes) útiles como bitácora del desarrollo.

## Módulos funcionales

- **Autenticación y autorización**: login/logout con JWT, blacklist de tokens, roles y permisos, recuperación/cambio de contraseña.
- **Personas**: gestión de personas, deportistas, entrenadores y usuarios del sistema.
- **Organización deportiva**: divisiones, niveles, categorías, tipos de grupo, grupos de entrenamiento (con entrenadores y deportistas asignados), inscripciones.
- **Financiero**: planes de pago (inscripción y mensualidad), tipos de plan, métodos y tipos de pago, pagos, facturas, descuentos y convenios.
- **Horarios y asistencia**: horarios por grupo/día, registro de asistencia (individual y masivo), estados de asistencia.
- **Evaluaciones**: evaluaciones por criterio y objetivo, evaluación de rutinas con rúbricas configurables.
- **Eventos**: eventos, grupos participantes, premios y resultados de competencia.
- **Historiales**: historial médico, historial de niveles y de planes por deportista.
- **Inventario y ventas**: categorías de producto, productos y variantes, movimientos de inventario, ventas.
- **Dashboard**: DTOs de resumen para las distintas secciones (administración, evaluación, eventos, finanzas, horarios, organización, personas, tienda).
- **Auditoría**: log de accesos (`LogAcceso`).

## Autenticación y seguridad

- `SecurityConfig` configura una cadena de filtros **stateless** (sin sesiones), con CSRF deshabilitado y CORS habilitado para `http://localhost:3000`.
- Rutas públicas: `/api/login`, `/api/logout`, `/api/refresh-token`, `/api/forgot-password`, `/api/reset-password`. El resto requiere autenticación.
- `JwtTokenFilter` valida el token JWT en cada petición; `JwtUtil` genera/valida tokens; `TokenBlacklistService` invalida tokens tras logout.
- `PermissionInterceptor` complementa la autorización a nivel de permisos por rol.
- Contraseñas hasheadas con `BCryptPasswordEncoder`.
- `DataInitializer` crea automáticamente un usuario `admin` (rol ADMIN, id=1) si no existe al arrancar la aplicación.

## Requisitos previos

- JDK 24+
- MySQL 8+ en ejecución
- Maven (opcional; el proyecto incluye Maven Wrapper)

## Configuración

La configuración vive en `src/main/resources/application.properties`. Los valores por defecto sirven para desarrollo local; en producción **siempre** deben sobreescribirse mediante variables de entorno:

| Variable de entorno | Por defecto (dev) | Descripción |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/cheermanager_I` | URL de conexión a MySQL |
| `DB_USERNAME` | `root` | Usuario de la base de datos |
| `DB_PASSWORD` | `root` | Contraseña de la base de datos |
| `JWT_SECRET` | (clave de desarrollo embebida) | Secreto para firmar los JWT — **rotar en cualquier entorno real** |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Tiempo de expiración del token |
| `AUTH_MAX_LOGIN_ATTEMPTS` | `5` | Intentos de login antes de bloqueo |
| `AUTH_LOCK_DURATION_MINUTES` | `30` | Duración del bloqueo por intentos fallidos |
| `AUTH_RESET_TOKEN_EXPIRATION_MINUTES` | `15` | Expiración del token de reseteo de contraseña |

Otros ajustes relevantes:
- `spring.jpa.hibernate.ddl-auto=update`: **Hibernate es la fuente de verdad del esquema**; cualquier script SQL de referencia debe generarse/validarse contra el esquema real que produce Hibernate, no al revés.
- `spring.jpa.show-sql=true` y logging de SQL/parámetros en `DEBUG`/`TRACE` para depuración.
- Subida de archivos: `app.documents.upload-dir=./uploads`, límite `10MB` por archivo/petición.
- `server.error.include-message=always`: los mensajes de error de negocio (p. ej. validaciones de `PagoService`) se exponen al frontend para mostrarlos en los toasts de error.

## Ejecución local

```bash
git clone https://github.com/Varcal-dev/cheermanager_back
cd cheermanager

# Configurar la base de datos MySQL (crear el schema, p. ej. cheermanager_I)
# y ajustar variables de entorno o application.properties si es necesario.

# Compilar y correr tests
./mvnw clean test-compile
./mvnw test

# Levantar la aplicación
./mvnw spring-boot:run
```

La API queda disponible en `http://localhost:8081`.

## Endpoints principales

Todos los endpoints cuelgan de `/api`. Agrupados por dominio (lista no exhaustiva; ver los controladores en `Controller/` para el detalle completo de cada recurso):

**Autenticación / Usuarios / Roles**
`POST /api/login` · `POST /api/logout` · `POST /api/usuario/registrar` · `GET /api/usuario/me` · `PUT /api/usuario/me` · `GET /api/usuario/permisos` · `GET /api/usuario/listar` · `GET /api/roles` · `POST /api/roles/asignar-permisos` · `GET /api/permisos/listar`

**Personas**
`GET|POST /api/personas` · `GET|POST /api/entrenadores` · `GET|POST /api/deportistas`

**Organización deportiva**
`GET|POST /api/niveles` · `GET /api/categorias-nivel` · `GET|POST /api/grupos` (+ `/con-deportistas`, `/verificar-elegibilidad`, `/agregar-deportistas`, `/asignar-entrenadores`) · `GET|POST /api/inscripciones` (+ `/inscritos`, `/no-inscritos`, `/simple`) · `GET /api/tipo-grupos`

**Financiero**
`GET|POST /api/convenios` · `GET|POST /api/divisiones` · `GET|POST /api/descuentos` · `GET|POST /api/facturas` · `GET|POST /api/pagos` · `GET /api/planes-inscripcion/listar` · `GET|POST /api/planes-mensualidad` (+ `/tipos`)

**Horarios y asistencia**
`GET|POST /api/horarios` · `GET|POST /api/asistencias`

**Evaluaciones**
`GET|POST /api/evaluaciones` · `GET|POST /api/objetivos` · `GET|POST /api/criterios-evaluacion` · `GET|POST /api/evaluaciones-rutina` · `GET|POST /api/rubricas`

**Eventos**
`GET|POST /api/eventos` (+ grupos, premios, resultados de competencia)

**Historiales**
`GET /api/historial-medico` · `GET /api/historial-niveles` · `GET /api/historial-planes`

**Inventario y ventas**
`GET|POST /api/productos` (+ variantes, categorías) · `GET|POST /api/movimientos-inventario` · `GET|POST /api/ventas`

**Dashboard / Auditoría**
`GET /api/dashboard/*` (resúmenes por sección) · `GET /api/logs-acceso`

## Base de datos

- Motor: MySQL 8+, esquema por defecto en desarrollo `cheermanager_I` (ajustable vía `DB_URL`).
- El esquema real es generado y mantenido por Hibernate (`ddl-auto=update`); cualquier script SQL versionado (data dictionary, script V5, etc.) debe tratarse como documentación derivada del modelo de entidades, no como fuente de verdad.
- Toda la lógica de consultas y negocio se mantiene versionada en código (servicios/repositorios), evitando vistas o procedimientos almacenados en la base de datos.

## Notas de desarrollo

- La seguridad ya usa JWT + roles/permisos (no `permitAll` global); revisar `SecurityConfig` para las rutas exceptuadas.
- Pendientes conocidos y decisiones de diseño se documentan en `ajustes_claudia/` (p. ej. `RESUMEN_CABOS_SUELTOS.md`, `Estado_Consolidado_CheerManager.md`).
- Mejoras sugeridas a futuro: documentación OpenAPI/Swagger, versionado de API (`/api/v1`), migraciones formales (Flyway/Liquibase) en lugar de `ddl-auto=update`, mayor cobertura de pruebas, y CI/CD.