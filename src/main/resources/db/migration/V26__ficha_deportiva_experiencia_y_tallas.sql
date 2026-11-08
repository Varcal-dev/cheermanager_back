-- =============================================================================
-- V26: ficha deportiva del deportista (C9 + pre-inscripcion mejorada).
--  fichas_deportivas              : por club. Experiencia previa (si/no, anios, nivel que cree tener) y tallas
--                                   de uniforme/ropa/calzado. Una fila por deportista.
--  ficha_habilidades_declaradas   : por club. Habilidades del glosario (V25) que el deportista DICE tener.
--                                   Son una declaracion, no una validacion: la validacion es la evaluacion (C1).
-- =============================================================================

CREATE TABLE IF NOT EXISTS fichas_deportivas (
  id                 INT AUTO_INCREMENT PRIMARY KEY,
  club_id            INT NOT NULL,
  deportista_id      INT NOT NULL,
  tiene_experiencia  BIT(1)      NULL,
  anios_experiencia  INT         NULL,
  nivel_declarado    VARCHAR(10) NULL,
  talla_uniforme     VARCHAR(10) NULL,
  talla_falda_short  VARCHAR(10) NULL,
  talla_camiseta     VARCHAR(10) NULL,
  talla_buzo         VARCHAR(10) NULL,
  talla_calzado      VARCHAR(10) NULL,
  UNIQUE KEY uk_ficha_deportista (club_id, deportista_id),
  CONSTRAINT fk_ficha_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_ficha_deportista FOREIGN KEY (deportista_id) REFERENCES deportistas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ficha_habilidades_declaradas (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  club_id        INT NOT NULL,
  deportista_id  INT NOT NULL,
  habilidad_id   INT NOT NULL,
  UNIQUE KEY uk_ficha_hab (club_id, deportista_id, habilidad_id),
  CONSTRAINT fk_fhd_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_fhd_deportista FOREIGN KEY (deportista_id) REFERENCES deportistas (id),
  CONSTRAINT fk_fhd_habilidad FOREIGN KEY (habilidad_id) REFERENCES habilidades_catalogo (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
