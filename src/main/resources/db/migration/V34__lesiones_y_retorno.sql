-- C3: lesiones de deportistas y retorno al juego (dato de salud: permisos propios).
CREATE TABLE IF NOT EXISTS lesiones (
  id                     INT AUTO_INCREMENT PRIMARY KEY,
  club_id                INT NOT NULL,
  deportista_id          INT NOT NULL,
  fecha_lesion           DATE NOT NULL,
  zona                   VARCHAR(30) NOT NULL,
  descripcion            VARCHAR(300) NOT NULL,
  gravedad               VARCHAR(10) NOT NULL,
  restriccion            VARCHAR(20) NOT NULL,
  estado                 VARCHAR(12) NOT NULL,
  fecha_estimada_retorno DATE NULL,
  fecha_alta             DATE NULL,
  aval_medico            BIT NOT NULL DEFAULT 0,
  aval_detalle           VARCHAR(150) NULL,
  registrado_por         INT NOT NULL,
  fecha_registro         DATETIME NOT NULL,
  KEY idx_lesiones_dep (club_id, deportista_id),
  KEY idx_lesiones_estado (club_id, estado),
  CONSTRAINT fk_lesiones_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_lesiones_deportista FOREIGN KEY (deportista_id) REFERENCES deportistas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS lesion_seguimientos (
  id                  INT AUTO_INCREMENT PRIMARY KEY,
  club_id             INT NOT NULL,
  lesion_id           INT NOT NULL,
  fecha               DATE NOT NULL,
  nota                VARCHAR(500) NOT NULL,
  estado              VARCHAR(12) NOT NULL,
  restriccion         VARCHAR(20) NOT NULL,
  usuario_id          INT NOT NULL,
  fecha_registro      DATETIME NOT NULL,
  KEY idx_lesion_seg (club_id, lesion_id),
  CONSTRAINT fk_lesion_seg_lesion FOREIGN KEY (lesion_id) REFERENCES lesiones (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO permisos (nombre) VALUES ('ver_lesiones'), ('gestionar_lesiones');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre IN ('ver_lesiones', 'gestionar_lesiones')
 WHERE r.nombre = 'Administrador';
