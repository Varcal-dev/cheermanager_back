-- C2: roster de competencia (quien compite por un grupo en un evento).
CREATE TABLE IF NOT EXISTS rosters_competencia (
  id                  INT AUTO_INCREMENT PRIMARY KEY,
  club_id             INT NOT NULL,
  evento_id           INT NOT NULL,
  grupo_id            INT NOT NULL,
  estado              VARCHAR(12) NOT NULL,
  cupo_maximo         INT NULL,
  creado_por          INT NOT NULL,
  fecha_creacion      DATETIME NOT NULL,
  fecha_confirmacion  DATETIME NULL,
  UNIQUE KEY uk_roster_evento_grupo (club_id, evento_id, grupo_id),
  CONSTRAINT fk_roster_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_roster_evento FOREIGN KEY (evento_id) REFERENCES eventos (id),
  CONSTRAINT fk_roster_grupo FOREIGN KEY (grupo_id) REFERENCES grupos_entrenamiento (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS roster_integrantes (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  club_id         INT NOT NULL,
  roster_id       INT NOT NULL,
  deportista_id   INT NOT NULL,
  rol             VARCHAR(10) NOT NULL,
  posicion        VARCHAR(40) NULL,
  fecha_agregado  DATETIME NOT NULL,
  UNIQUE KEY uk_roster_deportista (roster_id, deportista_id),
  KEY idx_roster_int (club_id, roster_id),
  CONSTRAINT fk_roster_int_roster FOREIGN KEY (roster_id) REFERENCES rosters_competencia (id) ON DELETE CASCADE,
  CONSTRAINT fk_roster_int_deportista FOREIGN KEY (deportista_id) REFERENCES deportistas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
