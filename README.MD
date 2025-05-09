# CheerManager

CheerManager es una aplicación para la gestión de gimnasios y equipos de cheerleading. Proporciona funcionalidades para manejar usuarios, entrenadores, deportistas, horarios, eventos, inventarios y más.

## Características

- Gestión de usuarios con roles y permisos.
- Autenticación y autorización con JWT.
- Gestión de deportistas, entrenadores y personal.
- Administración de horarios y asistencia.
- Gestión de eventos y resultados de competencias.
- Manejo de inventarios y ventas.
- Gestión financiera, incluyendo planes de pago, facturas y descuentos.

## Requisitos previos

- **Java 24** o superior.
- **Maven** (incluido el wrapper en el proyecto).
- **MySQL** como base de datos.

## Configuración

1. Clona este repositorio:
   ```bash
   git clone https://github.com/Varcal-dev/cheermanager_back
   cd CheerManager

2. Configura la base de datos en el archivo
    src/main/resources/application.properties:
    ```bash
    - spring.datasource.url=jdbc:mysql://localhost:3306/cheermanager_db
    - spring.datasource.username=TU_USUARIO
    - spring.datasource.password=TU_CONTRASEÑA

3. Ejecuta el siguiente comando para instalar las dependencias y compilar el proyecto:

   ./mvnw clean install

## Ejecución
   Para iniciar la aplicación, ejecuta:
    ```bash
   - ./mvnw spring-boot:run

   La aplicación estará disponible en http://localhost:8080.

## Endpoints principales

   - **Autenticación**: /auth/login
   - **Usuarios**: /usuarios
   - **Eventos**: /eventos
   - **Inventario**: /productos, /ventas

## Tecnologías utilizadas

   - **Spring Boot**: Framework principal.
   - **Spring Security**: Para autenticación y autorización.
   - **Hibernate**: ORM para la gestión de la base de datos.
   - **JWT**: Para la autenticación basada en tokens.
   - **Lombok**: Para reducir el código repetitivo.
   - **MySQL**: Base de datos relacional.

## Contribuciones

   ¡Las contribuciones son bienvenidas! Por favor, abre un issue o envía un pull request.

## Licencia

   Este proyecto está bajo la licencia Apache 2.0.

**Autor** : Varcal Tech