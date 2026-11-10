-- C11: sedes, espacios de entrenamiento y asignacion de espacio a cada horario de un grupo.
CREATE TABLE IF NOT EXISTS sedes (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  club_id    INT NOT NULL,
  nombre     VARCHAR(100) NOT NULL,
  direccion  VARCHAR(200) NULL,
  ciudad     VARCHAR(80) NULL,
  notas      VARCHAR(300) NULL,
  activa     BIT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_sedes_nombre (club_id, nombre),
  CONSTRAINT fk_sedes_club FOREIGN KEY (club_id) REFERENCES clubes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS espacios (
  id                  INT AUTO_INCREMENT PRIMARY KEY,
  club_id             INT NOT NULL,
  sede_id             INT NOT NULL,
  nombre              VARCHAR(100) NOT NULL,
  tipo                VARCHAR(20) NOT NULL,
  capacidad           INT NULL,
  grupos_simultaneos  INT NOT NULL DEFAULT 1,
  activo              BIT NOT NULL DEFAULT 1,
  UNIQUE KEY uk_espacios_nombre (sede_id, nombre),
  KEY idx_espacios_club (club_id, sede_id),
  CONSTRAINT fk_espacios_sede FOREIGN KEY (sede_id) REFERENCES sedes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS asignaciones_espacio (
  id                        INT AUTO_INCREMENT PRIMARY KEY,
  club_id                   INT NOT NULL,
  horario_entrenamiento_id  INT NOT NULL,
  espacio_id                INT NOT NULL,
  UNIQUE KEY uk_asignacion_horario (club_id, horario_entrenamiento_id),
  KEY idx_asignacion_espacio (club_id, espacio_id),
  CONSTRAINT fk_asignacion_he FOREIGN KEY (horario_entrenamiento_id) REFERENCES horarios_entrenamiento (id) ON DELETE CASCADE,
  CONSTRAINT fk_asignacion_espacio FOREIGN KEY (espacio_id) REFERENCES espacios (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO permisos (nombre) VALUES ('ver_sedes'), ('gestionar_sedes');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre IN ('ver_sedes', 'gestionar_sedes')
 WHERE r.nombre = 'Administrador';
