# Authentication API v3.0 - Complete Implementation Guide

## Overview
CheerManager now includes a complete authentication system with:
- Secure password hashing (BCrypt)
- Account locking after failed attempts
- JWT token management (issue, refresh, invalidate)
- Password reset with expiring tokens
- Password change functionality
- Automatic last access tracking

---

## 1. Login Endpoint
**POST** `/api/login`

### Request
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

### Success Response (200 OK)
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Error Responses
| Status | Error Code | Message |
|--------|-----------|---------|
| 401 | invalid_password | Contraseña incorrecta |
| 404 | email_not_found | El correo proporcionado no está registrado |
| 423 | account_locked | Cuenta bloqueada por múltiples intentos fallidos |
| 500 | invalid_hash | Error en el formato del hash de la contraseña |

### Features
- ✅ Increments failed attempts counter on wrong password
- ✅ Locks account after 5 failed attempts (configurable)
- ✅ Updates `ultimo_acceso` on successful login
- ✅ Resets failed attempts counter on successful login
- ✅ Generates JWT token with user ID, email, role, and permissions

### Configuration
```properties
auth.max-login-attempts=5              # Number of attempts before lock
auth.lock-duration-minutes=30           # Auto-unlock duration (future feature)
jwt.expiration-ms=86400000             # Token validity: 24 hours
```

---

## 2. Logout Endpoint
**POST** `/api/logout`

### Request
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Logout exitoso"
}
```

### Error Responses
| Status | Message |
|--------|---------|
| 400 | Token no proporcionado |
| 400 | Token inválido o expirado |

### Features
- ✅ Blacklists token immediately
- ✅ Token cannot be used after logout
- ✅ Stores blacklist until token expiration

---

## 3. Refresh Token Endpoint
**POST** `/api/refresh-token`

### Request
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Success Response (200 OK)
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9... (new token)"
}
```

### Error Responses
| Status | Message |
|--------|---------|
| 400 | Token no proporcionado |
| 401 | Token inválido o expirado |
| 500 | Error al generar nuevo token |

### Features
- ✅ Validates existing token
- ✅ Issues new token without session interruption
- ✅ Preserves user role and permissions

---

## 4. Forgot Password Endpoint
**POST** `/api/forgot-password`

### Request
```json
{
  "email": "user@example.com"
}
```

### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Token de recuperación enviado",
  "resetToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

### Error Responses
| Status | Message |
|--------|---------|
| 400 | Email es requerido |
| 404 | El email proporcionado no está registrado |
| 500 | Error al procesar la solicitud |

### Features
- ✅ Generates unique UUID token
- ✅ Sets token expiration (15 minutes by default)
- ✅ No session creation needed
- ✅ Stores token in `token_reset_password` column

### Configuration
```properties
auth.reset-password-token-expiration-minutes=15  # Token validity
```

---

## 5. Reset Password Endpoint
**POST** `/api/reset-password`

### Request
```json
{
  "token": "550e8400-e29b-41d4-a716-446655440000",
  "newPassword": "newpassword123"
}
```

### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Contraseña resetada correctamente"
}
```

### Error Responses
| Status | Error Code | Message |
|--------|-----------|---------|
| 400 | - | Token es requerido |
| 400 | - | Nueva contraseña es requerida |
| 401 | token_expired | Token de recuperación expirado |
| 404 | token_not_found | Token de recuperación inválido |
| 500 | - | Error al resetear contraseña |

### Features
- ✅ Validates token expiration
- ✅ Hashes new password with BCrypt
- ✅ Clears token after use
- ✅ Unlocks account automatically
- ✅ Resets failed attempts counter

---

## 6. Change Password Endpoint
**POST** `/api/change-password`

### Request
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

```json
{
  "currentPassword": "oldpassword123",
  "newPassword": "newpassword456"
}
```

### Success Response (200 OK)
```json
{
  "success": true,
  "message": "Contraseña cambiada correctamente"
}
```

### Error Responses
| Status | Message |
|--------|---------|
| 400 | Token no proporcionado |
| 400 | Contraseña actual es requerida |
| 400 | Nueva contraseña es requerida |
| 401 | Token inválido o expirado |
| 401 | Contraseña actual incorrecta |
| 404 | Usuario no encontrado |
| 500 | Error procesando cambio de contraseña |

### Features
- ✅ Requires valid JWT token
- ✅ Validates current password
- ✅ Hashes new password with BCrypt

---

## Complete User Flow Examples

### Example 1: Normal Login Flow
```bash
# 1. Login
curl -X POST http://localhost:8081/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'

# Response: {"success": true, "token": "eyJ..."}

# 2. Use token for API calls
curl -X GET http://localhost:8081/api/profile \
  -H "Authorization: Bearer eyJ..."

# 3. Logout when done
curl -X POST http://localhost:8081/api/logout \
  -H "Authorization: Bearer eyJ..."
```

### Example 2: Forgot Password Flow
```bash
# 1. Request password reset
curl -X POST http://localhost:8081/api/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com"}'

# Response: {"success": true, "resetToken": "uuid..."}

# 2. User receives email with token (TODO: email service)

# 3. Reset password with token
curl -X POST http://localhost:8081/api/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "uuid-from-email",
    "newPassword": "newpassword123"
  }'

# 4. Login with new password
curl -X POST http://localhost:8081/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "newpassword123"
  }'
```

### Example 3: Account Lock and Unlock
```bash
# 1. Try login 5 times with wrong password
for i in {1..5}; do
  curl -X POST http://localhost:8081/api/login \
    -H "Content-Type: application/json" \
    -d '{"email": "user@example.com", "password": "wrongpassword"}'
done

# Response on 5th attempt: 423 Account Locked

# 2. User can't login even with correct password
curl -X POST http://localhost:8081/api/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "password123"}'
# Still returns: 423 Account Locked

# 3. Reset password to unlock
curl -X POST http://localhost:8081/api/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com"}'
# Then follow forgot password flow above
# Account is unlocked after reset

# 4. Now can login again
curl -X POST http://localhost:8081/api/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "newpassword"}'
```

### Example 4: Change Password While Logged In
```bash
# Must have valid token
curl -X POST http://localhost:8081/api/change-password \
  -H "Authorization: Bearer eyJ..." \
  -H "Content-Type: application/json" \
  -d '{
    "currentPassword": "oldpassword",
    "newPassword": "newpassword"
  }'
```

---

## Database Schema

### Column Changes
```sql
ALTER TABLE usuarios ADD COLUMN bloqueado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN intentos_fallidos INT NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN token_reset_password VARCHAR(255) NULL;
ALTER TABLE usuarios ADD COLUMN token_reset_password_expiracion DATETIME NULL;
```

### Indexes Created
```sql
CREATE INDEX idx_usuarios_bloqueado ON usuarios(bloqueado);
CREATE INDEX idx_usuarios_intentos_fallidos ON usuarios(intentos_fallidos);
CREATE INDEX idx_usuarios_token_reset ON usuarios(token_reset_password);
```

---

## Security Features

1. **BCrypt Password Hashing**
   - Salt is generated for each password (gensalt())
   - Work factor of 10+ (default)

2. **Account Locking**
   - After 5 failed login attempts (configurable)
   - Requires password reset to unlock

3. **JWT Token Blacklist**
   - Tokens are blacklisted on logout
   - Blacklist checked on every authenticated request

4. **Reset Token Expiration**
   - Tokens expire after 15 minutes (configurable)
   - Cannot be reused

5. **Password Validation**
   - Current password must match for reset
   - BCrypt comparison prevents timing attacks

---

## Configuration Reference

### application.properties
```properties
# JWT Configuration
jwt.secret=691f7a33edc25a5b7b9d08d64f7db87fc873f787f6cae7fe1a702d2e155d8316
jwt.expiration-ms=86400000

# Authentication Configuration
auth.max-login-attempts=5
auth.lock-duration-minutes=30
auth.reset-password-token-expiration-minutes=15
```

---

## HTTP Status Codes Used

| Code | Meaning | Use Case |
|------|---------|----------|
| 200 | OK | Successful request |
| 400 | Bad Request | Invalid input (missing fields, etc.) |
| 401 | Unauthorized | Invalid token, expired token, wrong password |
| 404 | Not Found | User/email/token not found |
| 423 | Locked | Account locked due to too many failed attempts |
| 500 | Server Error | Internal errors |

---

## Remaining TODOs

1. **Email Service** (Optional)
   - Send reset token via email instead of returning it in response
   - Use Spring Boot Mail starter
   - Create email templates

2. **Login Attempt Auditing** (Optional)
   - Create `audit_login_attempts` table
   - Log IP addresses, timestamps, user agents
   - Alert on suspicious patterns

3. **Token Refresh Strategy** (Future)
   - Current: Manual refresh endpoint
   - Future: Automatic refresh via cookie/refresh token

4. **Two-Factor Authentication** (Future)
   - SMS/Email OTP
   - TOTP apps (Google Authenticator)

---

## Testing Checklist

- [ ] Login with correct credentials (200)
- [ ] Login with wrong password (401, account locks after N attempts)
- [ ] Login with non-existent email (404)
- [ ] Logout with valid token (200)
- [ ] Logout without token (400)
- [ ] Refresh token with valid JWT (200)
- [ ] Refresh token with expired JWT (401)
- [ ] Forgot password with valid email (200)
- [ ] Forgot password with invalid email (404)
- [ ] Reset password with valid token (200)
- [ ] Reset password with expired token (401)
- [ ] Reset password with invalid token (404)
- [ ] Change password while logged in (200)
- [ ] Change password with invalid current password (401)
- [ ] Change password without token (400)
