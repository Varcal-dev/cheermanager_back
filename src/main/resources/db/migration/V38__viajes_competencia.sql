-- C5: viaje a competencia (delegacion, autorizaciones de menores y abonos).
CREATE TABLE IF NOT EXISTS viajes_competencia (
  id                  INT AUTO_INCREMENT PRIMARY KEY,
  club_id             INT NOT NULL,
  evento_id           INT NOT NULL,
  destino             VARCHAR(150) NOT NULL,
  fecha_salida        DATE NOT NULL,
  fecha_regreso       DATE NOT NULL,
  punto_encuentro     VARCHAR(150) NULL,
  transporte          VARCHAR(150) NULL,
  alojamiento         VARCHAR(200) NULL,
  costo_por_persona   DECIMAL(12,2) NULL,
  notas               VARCHAR(500) NULL,
  estado              VARCHAR(10) NOT NULL,
  creado_por          INT NOT NULL,
  fecha_creacion      DATETIME NOT NULL,
  UNIQUE KEY uk_viaje_evento (club_id, evento_id),
  CONSTRAINT fk_viaje_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_viaje_evento FOREIGN KEY (evento_id) REFERENCES eventos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS viaje_participantes (
  id                    INT AUTO_INCREMENT PRIMARY KEY,
  club_id               INT NOT NULL,
  viaje_id              INT NOT NULL,
  tipo                  VARCHAR(12) NOT NULL,
  deportista_id         INT NULL,
  acompanante_nombre    VARCHAR(120) NULL,
  acompanante_documento VARCHAR(30) NULL,
  acompanante_rol       VARCHAR(12) NULL,
  estado                VARCHAR(12) NOT NULL,
  autorizacion_viaje    BIT NOT NULL DEFAULT 0,
  fecha_autorizacion    DATE NULL,
  notas                 VARCHAR(300) NULL,
  KEY idx_viaje_part (club_id, viaje_id),
  CONSTRAINT fk_viaje_part_viaje FOREIGN KEY (viaje_id) REFERENCES viajes_competencia (id) ON DELETE CASCADE,
  CONSTRAINT fk_viaje_part_dep FOREIGN KEY (deportista_id) REFERENCES deportistas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS viaje_abonos (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  club_id          INT NOT NULL,
  participante_id  INT NOT NULL,
  monto            DECIMAL(12,2) NOT NULL,
  fecha            DATE NOT NULL,
  nota             VARCHAR(200) NULL,
  registrado_por   INT NOT NULL,
  KEY idx_viaje_abono (club_id, participante_id),
  CONSTRAINT fk_viaje_abono_part FOREIGN KEY (participante_id) REFERENCES viaje_participantes (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO permisos (nombre) VALUES ('ver_viajes'), ('gestionar_viajes');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre IN ('ver_viajes', 'gestionar_viajes')
 WHERE r.nombre = 'Administrador';
