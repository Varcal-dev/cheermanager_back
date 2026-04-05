# CheerManager (Backend)

CheerManager es un sistema de gestión para clubes deportivos y academias de cheerleading. Su objetivo es ofrecer un backend completo para manejar:
- usuarios, roles y permisos
- personas (deportistas, entrenadores, personal)
- planes de pago, inscripciones y mensualidades
- facturación, descuentos y convenios
- grupos, niveles, categorías, horarios, inscripciones
- gestión de inventarios y ventas

## 1. Arquitectura

- Java + Spring Boot (3.4.5)
- Maven (wrapper en `.mvn/wrapper`) con Maven 3.9.9
- JDK: configurado a Java 25 LTS
- Spring Data JPA con Hibernate 6.2.6
- MySQL (configuración en `src/main/resources/application.properties`)
- REST API (`@RestController`) con rutas `api/*`
- Seguridad: `SecurityConfig` con `SecurityFilterChain` actual que permite todos los requests (dev)

## 2. Estructura del proyecto

- `src/main/java/com/varcal/cheermanager`
  - `Controller/` - controladores REST distribuidos en `Auth`, `Financiero`, `Persona`, `Org_dep`
  - `config/SecurityConfig.java` - configuración Spring Security
  - `CheermanagerApplication.java` - clase main Spring Boot
  - `service/` - lógica de negocio (servicios)
  - `repository/` - repositorios Spring Data
  - `entity/` - entidades JPA

- `src/main/resources/application.properties` - parámetros de DB, JPA, multipart, upload

## 3. Endpoints disponibles (lista principal)

### Autenticación
- POST `/api/login` - login
- POST `/api/logout` - logout

### Usuarios y Roles
- POST `/api/usuario/registrar`
- GET `/api/usuario/{id}`
- GET `/api/usuario/listar`
- GET `/api/usuario/me`
- GET `/api/usuario/permisos`
- GET `/api/usuario/genero/listar`
- GET `/api/usuario/rol/listar`
- GET `/api/usuario/rol/listar-con-usuarios`

- GET `/api/roles`
- GET `/api/roles/{id}`
- POST `/api/roles`
- POST `/api/roles/asignar-permisos`

- GET `/api/permisos/listar`
- GET `/api/permisos/{id}`
- POST `/api/permisos`

### Personas y relaciones
- POST `/api/personas`
- GET `/api/personas`
- GET `/api/personas/buscar`
- GET `/api/personas/estados`

- POST `/api/entrenadores`
- GET `/api/entrenadores`

- POST `/api/deportistas`
- GET `/api/deportistas`

- GET `/api/persona` (tal vez alias)

### Organización de la dependencia
- GET `/api/categorias-nivel`
- GET `/api/niveles` + `/api/niveles/detallado` + `/api/niveles/{id}`
- POST `/api/niveles`

- GET `/api/inscripciones` + `/api/inscripciones/detalle` + `/api/inscripciones/{id}`
- POST `/api/inscripciones`
- GET `/api/inscripciones/simple`
- GET `/api/inscripciones/inscritos`
- GET `/api/inscripciones/no-inscritos`

- GET `/api/grupos`, `/api/grupos/{id}`
- POST `/api/grupos`
- GET `/api/grupos/con-deportistas`
- GET `/api/grupos/{grupoId}/verificar-elegibilidad`
- POST `/api/grupos/{grupoId}/agregar-deportistas`
- POST `/api/grupos/{grupoId}/asignar-entrenadores`

- GET `/api/tipo-grupos`

### Financiero
- GET/POST `/api/convenios`
- GET/POST `/api/divisiones`
- GET/POST `/api/descuentos`

- GET `/api/facturas`, `/api/facturas/{id}`
- POST `/api/facturas`

- GET `/api/planes-inscripcion/listar`
- GET `/api/planes-inscripcion/{id}`

- GET `/api/planes-mensualidad`, `/api/planes-mensualidad/{id}`
- POST `/api/planes-mensualidad`
- GET `/api/planes-mensualidad/tipos`

## 4. Configuración local y ejecución

1. Clonar repo:
   ```bash
   git clone https://github.com/Varcal-dev/cheermanager_back
   cd CheerManager/cheermanager
   ```

2. Ajustar base de datos en `src/main/resources/application.properties`:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/cheermanager_db
   spring.datasource.username=root
   spring.datasource.password=root
   spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
   ```

3. Compilar y tests:
   ```bash
   .\mvnw clean test-compile
   .\mvnw test
   ```

4. Ejecutar app:
   ```bash
   .\mvnw spring-boot:run
   ```

5. Acceder:
   - Backend: `http://localhost:8081`

## 5. Requisitos de entorno

- Java >= 25 (recomendado JDK 25)
- Maven Wrapper (incluido)
- MySQL 8+

## 6. Estado actual del desarrollo

- Funcional endpoints CRUD listos para la mayoría de recursos.
- Seguridad base inyectada (permitAll) - requiere producción hardening.
- Ajuste de fields y validaciones (posible trabajo pendiente en entidades/DTOs).
- El módulo front (`cheermanager_fnt`) consume esta API.

## 7. Faltantes / mejoras sugeridas

1. Seguridad real con JWT y roles/authority en `SecurityConfig`.
2. Validación y saneamiento de payloads (`@Valid`, DTOs, `@ControllerAdvice`).
3. Documentación swagger (`springdoc-openapi`).
4. Pruebas unitarias/integración robustas y coverage.
5. CI/CD (GitHub Actions): `mvn test`, `mvn verify`, análisis de dependencias.
6. Manejo de migraciones con Flyway/Liquibase en lugar de `hibernate.ddl-auto=update`.
7. API versión (e.g., `/api/v1`) y paginación de listados.

## 8. Dependencias clave

- Spring Boot Starter Data JPA
- Spring Boot Starter Web
- Spring Boot Starter Security
- Spring Boot Starter Validation
- MySQL Connector Java
- Hibernate ORM 6.2.6.Final
- Lombok

## 9. Upgrade Java realizado

- De Java 24 a Java 25 en `pom.xml` (`<java.version>25</java.version>`).
- `maven-compiler-plugin` con `source=25`, `target=25`, `release=25`.
- Verificaciones pasadas:
  - `./mvnw -q clean test-compile` ✅
  - `./mvnw -q test` ✅
  - `./mvnw -q verify` ✅
  - `appmod-validate-cves-for-java` ✅

## 10. Notas finales

- No hay commits automáticos porque el workspace no es git: `GIT_AVAILABLE=false`.
- Para producción, actualizar `SecurityFilterChain` y mover credenciales fuera de properties.

---

**Autor**: Varcal Tech / GitHub Copilot (Raptor mini)
**Fecha**: 2026-04-01
