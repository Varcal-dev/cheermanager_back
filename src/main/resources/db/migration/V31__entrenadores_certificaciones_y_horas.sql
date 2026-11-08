-- C10: certificaciones de entrenadores (con vencimiento) y horas dictadas (base para la liquidacion, que va despues).
CREATE TABLE IF NOT EXISTS certificaciones_entrenador (
  id                 INT AUTO_INCREMENT PRIMARY KEY,
  club_id            INT NOT NULL,
  entrenador_id      INT NOT NULL,
  tipo               VARCHAR(24) NOT NULL,
  nombre             VARCHAR(120) NULL,
  entidad            VARCHAR(120) NULL,
  fecha_emision      DATE NULL,
  fecha_vencimiento  DATE NULL,
  registrado_por     INT NOT NULL,
  fecha_registro     DATETIME NOT NULL,
  KEY idx_cert_entrenador (club_id, entrenador_id, tipo),
  CONSTRAINT fk_cert_ent_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_cert_ent_entrenador FOREIGN KEY (entrenador_id) REFERENCES entrenadores (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS horas_entrenador (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  club_id         INT NOT NULL,
  entrenador_id   INT NOT NULL,
  grupo_id        INT NULL,
  fecha           DATE NOT NULL,
  horas           DECIMAL(4,2) NOT NULL,
  descripcion     VARCHAR(200) NULL,
  origen          VARCHAR(10) NOT NULL,
  registrado_por  INT NOT NULL,
  KEY idx_horas_entrenador (club_id, fecha),
  KEY idx_horas_entrenador_ent (club_id, entrenador_id, fecha),
  CONSTRAINT fk_horas_ent_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_horas_ent_entrenador FOREIGN KEY (entrenador_id) REFERENCES entrenadores (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
