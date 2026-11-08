-- C4: vencimientos de documentos del deportista (certificado medico, EPS, seguro, licencia federacion).
-- Solo guarda fechas y datos del documento; NO el contenido medico. Cada registro nuevo de un tipo reemplaza al anterior
-- (el anterior queda como historial).
CREATE TABLE IF NOT EXISTS documentos_deportista (
  id                 INT AUTO_INCREMENT PRIMARY KEY,
  club_id            INT NOT NULL,
  deportista_id      INT NOT NULL,
  tipo               VARCHAR(20) NOT NULL,
  entidad            VARCHAR(120) NULL,
  numero             VARCHAR(60)  NULL,
  fecha_emision      DATE NULL,
  fecha_vencimiento  DATE NULL,
  observaciones      VARCHAR(300) NULL,
  registrado_por     INT NOT NULL,
  fecha_registro     DATETIME NOT NULL,
  KEY idx_docs_dep (club_id, deportista_id, tipo),
  CONSTRAINT fk_docs_dep_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_docs_dep_deportista FOREIGN KEY (deportista_id) REFERENCES deportistas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
