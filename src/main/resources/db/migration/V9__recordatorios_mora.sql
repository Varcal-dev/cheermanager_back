-- =============================================================================
-- V9: recordatorios de mora por tres canales (sistema, correo, WhatsApp).
--
--  * recordatorios_config: una fila por club. El aviso dentro del sistema es el estandar (activo por defecto).
--  * recordatorios_envios: bitacora de cada aviso (por factura y canal). Sirve para no avisar de la misma
--    factura por el mismo canal antes de dias_entre_avisos y para mostrar el historial.
--  * Permiso gestionar_recordatorios_mora: enviar recordatorios y configurarlos.
--  * notificaciones (ya existia): se agrega un indice para consultar las de cada usuario.
-- =============================================================================

CREATE TABLE IF NOT EXISTS recordatorios_config (
  id               INT NOT NULL AUTO_INCREMENT,
  club_id          INT NOT NULL,
  automatico       BIT(1) NOT NULL DEFAULT b'0',
  canal_sistema    BIT(1) NOT NULL DEFAULT b'1',
  canal_correo     BIT(1) NOT NULL DEFAULT b'0',
  canal_whatsapp   BIT(1) NOT NULL DEFAULT b'0',
  dias_entre_avisos INT NOT NULL DEFAULT 7,
  PRIMARY KEY (id),
  UNIQUE KEY uk_recordatorios_config_club (club_id),
  CONSTRAINT fk_recordatorios_config_club FOREIGN KEY (club_id) REFERENCES clubes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS recordatorios_envios (
  id                    INT NOT NULL AUTO_INCREMENT,
  club_id               INT NOT NULL,
  factura_id            INT NOT NULL,
  persona_id            INT NOT NULL,
  canal                 VARCHAR(12) NOT NULL,
  destino               VARCHAR(160) NULL,
  estado                VARCHAR(12) NOT NULL,
  detalle               VARCHAR(300) NULL,
  fecha                 DATETIME(6) NOT NULL,
  automatico            BIT(1) NOT NULL DEFAULT b'0',
  enviado_por_usuario_id INT NULL,
  PRIMARY KEY (id),
  KEY idx_recordatorios_envios_factura (club_id, factura_id, canal, estado, fecha),
  KEY idx_recordatorios_envios_fecha (club_id, fecha),
  CONSTRAINT fk_recordatorios_envios_club FOREIGN KEY (club_id) REFERENCES clubes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET @idx := (SELECT COUNT(*) FROM information_schema.statistics
              WHERE table_schema = DATABASE() AND table_name = 'notificaciones' AND index_name = 'idx_notificaciones_usuario');
SET @sql := IF(@idx = 0, 'CREATE INDEX idx_notificaciones_usuario ON notificaciones (usuario_id, vista, fecha)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT IGNORE INTO permisos (nombre) VALUES ('gestionar_recordatorios_mora');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre = 'gestionar_recordatorios_mora'
 WHERE r.nombre IN ('Super Administrador', 'Administrador', 'Tesorero', 'Coordinador Administrativo');
