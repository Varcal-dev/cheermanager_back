-- =============================================================================
-- V14: tokens revocados persistentes (S5).
-- Hasta ahora la lista de tokens revocados (logout, renovacion de sesion) vivia solo en memoria:
-- al reiniciar el servidor un token cerrado volvia a ser valido hasta 8 horas. Ahora se guarda aqui.
--  * token_hash: SHA-256 (hex) del token; NUNCA el token en claro.
--  * expira_ms : instante (epoch en ms) en que el token vence por si solo; pasado ese momento ya no hace falta
--                recordarlo y una tarea horaria lo borra.
-- Tabla global (no lleva club_id): el token ya identifica su club y el hash no revela nada.
-- =============================================================================
CREATE TABLE IF NOT EXISTS `tokens_revocados` (
    `token_hash` CHAR(64) NOT NULL,
    `expira_ms`  BIGINT   NOT NULL,
    PRIMARY KEY (`token_hash`),
    KEY `idx_tokens_revocados_expira` (`expira_ms`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
