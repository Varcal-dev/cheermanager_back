-- =============================================================================
-- V6: portal de solo lectura para deportistas y acudientes + solicitudes de inscripcion a temporada.
--
--  * acudientes.usuario_id: un acudiente puede tener usuario propio (rol "Acudiente").
--  * Permiso acceder_portal: da acceso a /api/portal/** (cada usuario ve SOLO lo suyo: se valida por
--    pertenencia, no por permiso general). Se concede a los roles Deportista y Acudiente.
--  * SEGURIDAD: el rol Deportista sembrado en V2 traia permisos de LISTADO del club entero (ver_asistencia,
--    ver_evaluacion, ver_dash_inicio...). Con ellos un deportista podia consultar datos de otros menores
--    y las cifras del club. Se le quitan; su informacion personal ahora sale por el portal.
--  * solicitudes_inscripcion: el deportista/acudiente SOLICITA inscribirse a una temporada y el club
--    aprueba o rechaza una por una.
-- =============================================================================

ALTER TABLE acudientes
    ADD COLUMN usuario_id INT NULL,
    ADD UNIQUE KEY uk_acudiente_usuario (usuario_id),
    ADD CONSTRAINT fk_acudiente_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id);

INSERT IGNORE INTO permisos (nombre) VALUES ('acceder_portal');

-- Rol Acudiente en cada club real que aun no lo tenga
INSERT INTO roles (nombre, descripcion, club_id)
SELECT 'Acudiente', 'Padre, madre o tutor con acceso de solo lectura a la informacion de sus deportistas', c.id
  FROM clubes c
 WHERE c.id <> 1
   AND NOT EXISTS (SELECT 1 FROM roles r WHERE r.club_id = c.id AND r.nombre = 'Acudiente');

-- Quitar permisos de listado general al rol Deportista (de todos los clubes)
DELETE rp FROM rol_permisos rp
  JOIN roles r    ON r.id = rp.rol_id
  JOIN permisos p ON p.id = rp.permiso_id
 WHERE r.nombre = 'Deportista' AND r.club_id <> 1
   AND p.nombre IN ('ver_asistencia', 'ver_evaluacion', 'ver_historial_evaluacion_deportista',
                    'ver_historial_nivel_deportista', 'ver_dashboard', 'ver_dash_inicio', 'ver_dash_eventos',
                    'ver_notificacion');

-- Portal para Deportista y Acudiente
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre = 'acceder_portal'
 WHERE r.nombre IN ('Deportista', 'Acudiente') AND r.club_id <> 1;

CREATE TABLE IF NOT EXISTS solicitudes_inscripcion (
  id                        INT NOT NULL AUTO_INCREMENT,
  club_id                   INT NOT NULL,
  deportista_id             INT NOT NULL,
  temporada_id              INT NOT NULL,
  estado                    VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  observaciones             VARCHAR(300) NULL,
  fecha_solicitud           DATETIME(6) NOT NULL,
  solicitada_por_usuario_id INT NULL,
  fecha_resolucion          DATETIME(6) NULL,
  resuelta_por_usuario_id   INT NULL,
  motivo_resolucion         VARCHAR(300) NULL,
  inscripcion_id            INT NULL,
  PRIMARY KEY (id),
  KEY idx_solic_club_estado (club_id, estado),
  KEY idx_solic_deportista (club_id, deportista_id),
  CONSTRAINT fk_solic_club        FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_solic_deportista  FOREIGN KEY (club_id, deportista_id) REFERENCES deportistas (club_id, id),
  CONSTRAINT fk_solic_temporada   FOREIGN KEY (club_id, temporada_id)  REFERENCES temporadas (club_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
