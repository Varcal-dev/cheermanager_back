-- =============================================================================
-- V19: aceptacion de terminos y politica por el club (bloque legal, L1).
--
-- Cada club debe aceptar los Terminos y Condiciones y la Politica de Tratamiento de Datos antes de operar.
-- La aceptacion es POR CLUB y POR VERSION de documento: queda quien acepto, cuando, desde que IP y que version.
-- Cuando se publique una version nueva (constante en LegalService) el club vuelve a quedar pendiente.
-- Permiso nuevo: aceptar_terminos (solo quien tiene este permiso puede aceptar en nombre del club); se da al
-- Administrador. Los clubes nuevos lo reciben solos (plantilla = todos los permisos que no son de plataforma).
-- Los clubes existentes quedan PENDIENTES (deben aceptar al primer ingreso). Idempotente.
-- =============================================================================

CREATE TABLE IF NOT EXISTS aceptaciones_legales (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  club_id     INT NOT NULL,
  usuario_id  INT NOT NULL,
  tipo        VARCHAR(20) NOT NULL,
  version     VARCHAR(20) NOT NULL,
  fecha       DATETIME NOT NULL,
  ip          VARCHAR(64) NULL,
  UNIQUE KEY uq_aceptacion_club_tipo_version (club_id, tipo, version),
  KEY idx_aceptacion_club (club_id, fecha),
  CONSTRAINT fk_aceptacion_club FOREIGN KEY (club_id) REFERENCES clubes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO permisos (nombre) VALUES ('aceptar_terminos');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre = 'aceptar_terminos'
 WHERE r.nombre = 'Administrador';
