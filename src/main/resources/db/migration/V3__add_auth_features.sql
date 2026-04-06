-- Add authentication features to usuarios table
-- V3 Migration - 2026-04-06

ALTER TABLE usuarios ADD COLUMN bloqueado BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuarios ADD COLUMN intentos_fallidos INT NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN token_reset_password VARCHAR(255) NULL;
ALTER TABLE usuarios ADD COLUMN token_reset_password_expiracion DATETIME NULL;

-- Create indexes for performance
CREATE INDEX idx_usuarios_bloqueado ON usuarios(bloqueado);
CREATE INDEX idx_usuarios_intentos_fallidos ON usuarios(intentos_fallidos);
CREATE INDEX idx_usuarios_token_reset ON usuarios(token_reset_password);
