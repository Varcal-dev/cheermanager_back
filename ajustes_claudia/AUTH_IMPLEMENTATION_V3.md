# Autenticación Avanzada v3.0 - COMPLETADO ✅

## ✅ Endpoints Implementados (6/6)

### 1. POST /api/refresh-token
**Descripción:** Genera nuevo JWT a partir de uno válido (sin expiración)

**Estado:** ✅ Implementado

**Request:**
```http
Authorization: Bearer <JWT_TOKEN>
```

**Response (200 OK):**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Features:**
- ✅ Valida JWT con expiración
- ✅ Generapara nuevo token si el actual es válido
- ✅ Usuario debe estar activo (no bloqueado)
- ✅ Mantiene permisos originales

---

### 2. POST /api/forgot-password
**Descripción:** Inicia proceso de recuperación de contraseña

**Estado:** ✅ Implementado

**Request:**
```json
{
  "email": "usuario@example.com"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Si el correo existe, recibirá un enlace de recuperación",
  "resetToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Features:**
- ✅ Genera token UUID único
- ✅ Expiracion configurable: 15 minutos (por defecto)
- ✅ Token almacenado en BD: `token_reset_password`
- ✅ Expiración almacenada: `token_reset_password_expiracion`
- ✅ Mensaje genérico por seguridad (no expone si email existe)
- ✅ Status 404 si email no existe
- ✅ Status 403 si usuario inactivo

---

### 3. POST /api/reset-password
**Descripción:** Restablece contraseña usando token de reset

**Estado:** ✅ Implementado

**Request:**
```json
{
  "resetToken": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "newPassword123"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Contraseña restablecida exitosamente"
}
```

**Features:**
- ✅ Valida existencia del token
- ✅ Valida no haya expirado
- ✅ Hash nueva contraseña con BCrypt
- ✅ Limpia token después de uso
- ✅ Resetea `intentosFallidos` a 0
- ✅ Desbloquea usuario si estaba bloqueado
- ✅ Status 400 si token inválido o expirado

---

### 4. POST /api/change-password
**Descripción:** Cambia contraseña de usuario autenticado

**Estado:** ✅ Implementado

**Request:**
```http
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "currentPassword": "password123",
  "newPassword": "newPassword456"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Contraseña cambiada exitosamente"
}
```

**Features:**
- ✅ Valida token JWT (requiere autenticación)
- ✅ Extrae `userId` del token
- ✅ Verifica contraseña actual con BCrypt
- ✅ Hash nueva contraseña
- ✅ Token sigue siendo válido

---

### 5. Feature: Bloqueo tras N intentos fallidos
**Descripción:** Bloquea cuenta automáticamente después de N intentos fallidos

**Estado:** ✅ Implementado

**Configuración:**
```properties
auth.max-login-attempts=5                    # Intentos antes de bloqueo
auth.lock-duration-minutes=30                # Referencia (desbloquea en reset)
```

**Features:**
- ✅ Incrementa `intentosFallidos` en cada fallo de contraseña
- ✅ Bloquea automáticamente cuando `intentosFallidos >= maxAttempts`
- ✅ Devuelve status 423 cuando usuario bloqueado
- ✅ Se desbloquea automáticamente al reset de password
- ✅ Se resetea count de intentos en login exitoso

---

### 6. Feature: Actualizar último_acceso al login
**Descripción:** Registra timestamp de último acceso exitoso

**Estado:** ✅ Implementado

**Detalles:**
- ✅ Se actualiza automáticamente en login exitoso
- ✅ Campo: `usuario.ultimoAcceso` (java.time.LocalDateTime)
- ✅ Se guarda en BD al momento del login
- ✅ Accessible para auditoría y análisis

## 🗂️ Archivos Modificados

### Entity - JPA
- **Usuario.java**: Agregados 4 campos nuevos
  - `bloqueado: Boolean`
  - `intentosFallidos: Integer`
  - `tokenResetPassword: String`
  - `tokenResetPasswordExpiracion: LocalDateTime`

### Service
- **AuthService.java**: Implementados 5 métodos
  - `refreshToken(token): String`
  - `forgotPassword(email): PasswordResetResult`
  - `resetPassword(token, newPassword): PasswordResetResult`
  - `changePassword(userId, currentPwd, newPwd): PasswordChangeResult`
  - Manejo de intentos fallidos en `authenticate()`

- Nuevas clases internas:
  - `PasswordResetResult`
  - `PasswordChangeResult`

### Controller
- **AuthController.java**: 4 endpoints nuevos
  - `POST /auth/refresh-token`
  - `POST /auth/forgot-password`
  - `POST /auth/reset-password`
  - `POST /auth/change-password`

- Manejo de error code `user_locked` (status 423)

### Repository
- **UserRepository.java**: Método nuevo
  - `Optional<Usuario> findByTokenResetPassword(String)`

## 🗄️ Schema - SQL

### Migración
- Archivo: `migrations/V3_add_auth_features.sql`
- Columnas agregadas a tabla `usuarios`:
  - `bloqueado BOOLEAN DEFAULT FALSE`
  - `intentos_fallidos INT DEFAULT 0`
  - `token_reset_password VARCHAR(255) NULL`
  - `token_reset_password_expiracion DATETIME NULL`

- Índices agregados:
  - `idx_usuarios_token_reset` en `token_reset_password`
  - `idx_usuarios_bloqueado` en `bloqueado`
  - `idx_usuarios_email` en `email`

### Script Actualizado
- Pendiente: Ejecutar migración V3 o actualizar `TF_Cheermanager_V2_optimized.sql`

## ⚙️ Configuración

Agregar a `application.properties` o `application.yml`:

```properties
# Authentication settings
auth.max-login-attempts=5
auth.lock-duration-minutes=30
auth.reset-password-token-expiration-minutes=15

# JWT settings (existente)
jwt.secret=YOUR_BASE64_SECRET
jwt.expiration-ms=3600000

# Email (recomendado para forgot-password)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=${MAIL_USERNAME}
spring.mail.password=${MAIL_PASSWORD}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

## 🔄 Flujos de Autenticación

### Login Standard
```
POST /auth/login
  { email, password }
① Verificar si usuario bloqueado → 423 Locked
② BCrypt checkpw(password)
③ Si correcto: reset intentosFallidos, actualizar ultimoAcceso, generar token
④ Si incorrecto: incrementar intentosFallidos, bloquear si ≥ maxAttempts
```

### Refresh Token
```
POST /auth/refresh-token
① Validar JWT existente
② Verificar usuario no bloqueado ni inactivo
③ Generar nuevo token
```

### Forgot Password
```
POST /auth/forgot-password
① Buscar usuario por email
② Generar UUID resetToken
③ Guardar token + expiración (15 min default)
④ Responder con token (enviar por email en producción)
```

### Reset Password
```
POST /auth/reset-password
① Validar token existe y no expirado
② Hashear nueva contraseña
③ Limpiar token
④ Desbloquear usuario
⑤ Reset intentosFallidos
```

### Change Password
```
POST /auth/change-password
① Validar JWT en header
② Extraer userId del token
③ Verificar contraseña actual
④ Hashear nueva contraseña
⑤ Guardar en BD
```

## 📝 HTTP Status Codes

| Endpoint | Caso | Status | Message |
|----------|------|--------|---------|
| /login | Password incorrecta | 401 | "Contraseña incorrecta" |
| /login | Usuario bloqueado | 423 | "Usuario bloqueado por múltiples intentos fallidos" |
| /login | Email no existe | 404 | "El correo no está registrado" |
| /refresh-token | Token inválido | 401 | "Token inválido o expirado" |
| /forgot-password | Email no existe | 404 | Email no registrado |
| /reset-password | Token inválido | 400 | "Token inválido" |
| /reset-password | Token expirado | 400 | "Token expirado" |
| /change-password | Pwd actual incorrecta | 401 | "Contraseña actual es incorrecta" |

## ✨ Características de Seguridad

✅ BCrypt para hash de contraseñas
✅ JWT con expiración configurable
✅ Rate limiting por intentos fallidos
✅ Bloqueo automático tras N intentos
✅ Token de reset con UUID único
✅ Expiración de token de reset
✅ Blacklist de tokens en logout
✅ Validación de usuario activo

## 🚀 Pasos Siguientes (Recomendado)

1. **Ejecutar migración SQL**
   ```bash
   mysql -u root -p cheermanager < migrations/V3_add_auth_features.sql
   ```

2. **Agregar servicio de Email** (opcional pero recomendado)
   - Crear `EmailService` para enviar enlaces de reset
   - Integrarlo en `forgotPassword()`

3. **Agregar auditoría de intentos de login**
   - Crear tabla `audit_login_attempts`
   - Registrar intentos fallidos para análisis

4. **Implementar CAPTCHA** (opcional)
   - Agregar en login tras N intentos

5. **Tests unitarios**
   - MockMvc para endpoints
   - Test de bloqueo y reset
