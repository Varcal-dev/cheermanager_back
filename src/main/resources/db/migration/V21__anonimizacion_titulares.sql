-- =============================================================================
-- V21: anonimizacion de titulares (bloque legal, L7).
-- Tabla de constancia (sin datos personales: solo quien, cuando, a que persona y por que motivo) y permiso nuevo
-- anonimizar_titular (irreversible: solo Administrador). Idempotente.
-- =============================================================================

CREATE TABLE IF NOT EXISTS anonimizaciones (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  club_id     INT NOT NULL,
  persona_id  INT NOT NULL,
  usuario_id  INT NOT NULL,
  fecha       DATETIME NOT NULL,
  motivo      VARCHAR(300) NOT NULL,
  KEY idx_anonimizaciones_club (club_id, fecha),
  CONSTRAINT fk_anonimizaciones_club FOREIGN KEY (club_id) REFERENCES clubes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO permisos (nombre) VALUES ('anonimizar_titular');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre = 'anonimizar_titular'
 WHERE r.nombre = 'Administrador';
