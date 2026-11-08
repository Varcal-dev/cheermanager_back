-- L8: registro de consultas y reclamos de titulares (Ley 1581 de 2012, arts. 14 y 15).
CREATE TABLE IF NOT EXISTS solicitudes_titular (
  id                      INT AUTO_INCREMENT PRIMARY KEY,
  club_id                 INT NOT NULL,
  tipo                    VARCHAR(20)  NOT NULL,
  estado                  VARCHAR(15)  NOT NULL,
  canal                   VARCHAR(12)  NOT NULL,
  solicitante_nombre      VARCHAR(160) NOT NULL,
  solicitante_documento   VARCHAR(40)  NULL,
  solicitante_contacto    VARCHAR(160) NULL,
  persona_id              INT          NULL,
  descripcion             VARCHAR(2000) NOT NULL,
  fecha_recepcion         DATE         NOT NULL,
  fecha_limite            DATE         NOT NULL,
  prorrogada              BIT(1)       NOT NULL DEFAULT b'0',
  motivo_prorroga         VARCHAR(500) NULL,
  respuesta               VARCHAR(2000) NULL,
  fecha_respuesta         DATE         NULL,
  registrada_por          INT          NOT NULL,
  respondida_por          INT          NULL,
  KEY idx_solicitudes_titular_club (club_id, estado, fecha_limite),
  CONSTRAINT fk_solicitudes_titular_club FOREIGN KEY (club_id) REFERENCES clubes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
