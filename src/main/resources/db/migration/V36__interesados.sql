-- C6: pre-inscripcion publica por club (clase de prueba, inscripcion, lista de espera).
CREATE TABLE IF NOT EXISTS interesados (
  id                      INT AUTO_INCREMENT PRIMARY KEY,
  club_id                 INT NOT NULL,
  tipo                    VARCHAR(15) NOT NULL,
  estado                  VARCHAR(16) NOT NULL,
  contacto_nombre         VARCHAR(120) NOT NULL,
  contacto_correo         VARCHAR(150) NOT NULL,
  contacto_telefono       VARCHAR(30) NOT NULL,
  deportista_nombre       VARCHAR(120) NOT NULL,
  deportista_nacimiento   DATE NOT NULL,
  experiencia             VARCHAR(300) NULL,
  mensaje                 VARCHAR(500) NULL,
  grupo_id                INT NULL,
  fecha_prueba            DATE NULL,
  notas                   VARCHAR(500) NULL,
  version_consentimiento  VARCHAR(20) NOT NULL,
  fecha_consentimiento    DATETIME NOT NULL,
  ip                      VARCHAR(64) NULL,
  fecha_creacion          DATETIME NOT NULL,
  fecha_actualizacion     DATETIME NOT NULL,
  KEY idx_interesados_club_estado (club_id, estado),
  KEY idx_interesados_club_fecha (club_id, fecha_creacion),
  CONSTRAINT fk_interesados_club FOREIGN KEY (club_id) REFERENCES clubes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO permisos (nombre) VALUES ('ver_interesados'), ('gestionar_interesados');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre IN ('ver_interesados', 'gestionar_interesados')
 WHERE r.nombre = 'Administrador';
