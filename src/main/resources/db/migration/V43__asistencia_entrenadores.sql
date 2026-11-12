-- Asistencia del personal (entrenadores): una fila por entrenador y dia, con hora de entrada y de salida.
-- La primera lectura del carnet del dia marca la entrada y la siguiente la salida. origen: ESCANEO o MANUAL.
CREATE TABLE IF NOT EXISTS asistencia_entrenador (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  club_id        INT NOT NULL,
  entrenador_id  INT NOT NULL,
  fecha          DATE NOT NULL,
  hora_entrada   TIME NOT NULL,
  hora_salida    TIME NULL,
  origen         VARCHAR(10) NOT NULL DEFAULT 'ESCANEO',
  registrado_por INT NULL,
  UNIQUE KEY uk_asist_entrenador_dia (club_id, entrenador_id, fecha),
  KEY idx_asist_entrenador_fecha (club_id, fecha),
  CONSTRAINT fk_asist_ent_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_asist_ent_entrenador FOREIGN KEY (entrenador_id) REFERENCES entrenadores (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
