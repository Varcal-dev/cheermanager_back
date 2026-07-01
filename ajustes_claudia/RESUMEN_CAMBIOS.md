# Resumen de cambios — 3 arreglos de CheerManager Backend

## Cómo aplicar estos archivos

Cada archivo de este paquete corresponde 1:1 a un archivo de tu repo. Reemplaza
el contenido del archivo existente con el del mismo nombre aquí (las rutas
completas están abajo). No se modificó nada más.

---

## 1. Secrets fuera del código

| Archivo en este paquete | Reemplaza a |
|---|---|
| `application.properties` | `src/main/resources/application.properties` |
| `application.properties.example` | (archivo nuevo, sí se versiona) |
| `.gitignore` | `.gitignore` (raíz del proyecto) |

**Pasos después de copiar los archivos:**

1. Genera un secret nuevo (el viejo ya quedó expuesto en el historial de git, así que rótalo):
   ```bash
   openssl rand -base64 64
   ```
2. Exporta las variables de entorno en tu máquina/servidor (no las pongas en el archivo):
   ```bash
   export JWT_SECRET="<el valor que generaste arriba>"
   export DB_USERNAME=root
   export DB_PASSWORD="<tu password real>"
   ```
3. Si despliegas en un servicio (Railway, Render, EC2, etc.), configura esas mismas
   variables en el panel de variables de entorno del servicio.
4. **El secret viejo (`691f7a33...`) sigue en el historial de git aunque lo borres del
   archivo actual.** Si el repo es privado y de un solo colaborador no es urgente,
   pero si en algún momento se hace público hay que asumir que ese secret está
   comprometido y debe quedar fuera de uso (ya lo está, porque generaste uno nuevo).

---

## 2. `@Transactional` en operaciones multi-tabla

| Archivo en este paquete | Reemplaza a |
|---|---|
| `PersonaService.java` | `src/main/java/com/varcal/cheermanager/Service/Persona/PersonaService.java` |

Se agregó `@Transactional` a: `registrarUsuario`, `registrarDeportista`,
`modificarDeportista`, `vincularDeportistaConUsuario`,
`desvinculcarDeportistaDeUsuario`, `eliminarDeportista`, `registrarEntrenador`,
`modificarEntrenador`, `eliminarEntrenador`. La lógica interna de cada método
**no cambió** — solo se agregó la anotación y el import correspondiente
(`org.springframework.transaction.annotation.Transactional`).

No requiere cambios en `pom.xml`: `spring-boot-starter-data-jpa` ya trae
`spring-tx`, así que `@Transactional` funciona sin dependencias nuevas.

**Importante:** si tienes otros services con el mismo patrón (Financiero,
Org_dep, etc. — varios hacen 2-3 `save()` encadenados), aplica el mismo criterio
ahí cuando tengas tiempo. Por ahora solo cubrí `PersonaService`, que era el caso
más claro que vimos juntos.

---

## 3. Forzar cambio de contraseña por defecto

| Archivo en este paquete | Reemplaza a |
|---|---|
| `Usuario.java` | `src/main/java/com/varcal/cheermanager/Models/Auth/Usuario.java` |
| `AuthService.java` | `src/main/java/com/varcal/cheermanager/Service/Auth/AuthService.java` |
| `AuthController.java` | `src/main/java/com/varcal/cheermanager/Controller/Auth/AuthController.java` |
| `UsuarioDTO.java` | `src/main/java/com/varcal/cheermanager/DTO/Ath/UsuarioDTO.java` |
| `PersonaService.java` | (ya incluido arriba — marca el flag al crear con password "0000") |

### Qué cambia

- **`Usuario`**: nuevo campo `requiereCambioPassword` (boolean, default `false`).
- **`PersonaService.registrarUsuario`**: si la contraseña recibida es `"0000"`
  (el valor por defecto que usa `registrarDeportista`/`registrarEntrenador`),
  marca `requiereCambioPassword = true` automáticamente.
- **`AuthService.authenticate`**: el login ya no solo devuelve el token, también
  informa si ese usuario debe cambiar su contraseña.
- **`AuthController./api/login`**: la respuesta JSON ahora incluye
  `"requiereCambioPassword": true/false`.
- **`AuthService.changePassword` y `resetPassword`**: al cambiar la contraseña
  exitosamente, el flag se limpia automáticamente (`false`).

### Lo que falta del lado del frontend (no lo hice porque no vi ese código)

El backend ya expone la señal; el frontend debe:
1. Leer `requiereCambioPassword` en la respuesta de `/api/login`.
2. Si es `true`, redirigir a una pantalla que solo permita llamar a
   `/api/change-password` antes de dejar entrar al resto de la app.

Si quieres, en otra sesión revisamos el frontend (`cheermanager_fnt`) y lo
conectamos.

### Base de datos

Con `spring.jpa.hibernate.ddl-auto=update` (que ya tienes), Hibernate va a crear
la columna `requiere_cambio_password` sola la próxima vez que levantes el
backend. No necesitas escribir una migración a mano. Si en algún momento migras
a Flyway/Liquibase (lo cual te recomendaría a futuro, como mencioné en la
revisión inicial), la migración sería:

```sql
ALTER TABLE usuarios
  ADD COLUMN requiere_cambio_password BOOLEAN NOT NULL DEFAULT FALSE;
```

---

## Qué NO toqué

- No modifiqué `DataInitializer.java` (el admin inicial con password `"admin123"`
  sigue igual — ese no pasa por `PersonaService.registrarUsuario`, así que no le
  aplica el flag automáticamente). Si quieres que el admin también lo tenga,
  dime y lo ajustamos.
- No toqué nada del frontend.
- No toqué otros services financieros/organizacionales que también podrían
  beneficiarse de `@Transactional` — quedó fuera del alcance de hoy.
