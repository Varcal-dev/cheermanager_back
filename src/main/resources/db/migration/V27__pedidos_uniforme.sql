-- =============================================================================
-- V27: pedido de uniformes por grupo con cobro en cuotas (C9).
--  pedidos_uniforme         : por club. Un pedido = nombre, grupo, valor por deportista, numero de cuotas y
--                             fecha de la primera cuota.
--  pedidos_uniforme_cobros  : por club. Une cada cuota facturada (factura concepto UNIFORME) con su pedido y
--                             persona; sirve para no facturar dos veces al mismo deportista.
-- =============================================================================

CREATE TABLE IF NOT EXISTS pedidos_uniforme (
  id                  INT AUTO_INCREMENT PRIMARY KEY,
  club_id             INT NOT NULL,
  nombre              VARCHAR(120)  NOT NULL,
  grupo_id            INT NOT NULL,
  valor_por_deportista DECIMAL(10,2) NOT NULL,
  numero_cuotas       INT NOT NULL,
  primer_vencimiento  DATE NOT NULL,
  fecha_creacion      DATE NOT NULL,
  usuario_id          INT NULL,
  CONSTRAINT fk_pu_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_pu_grupo FOREIGN KEY (grupo_id) REFERENCES grupos_entrenamiento (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS pedidos_uniforme_cobros (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  club_id     INT NOT NULL,
  pedido_id   INT NOT NULL,
  persona_id  INT NOT NULL,
  cuota       INT NOT NULL,
  factura_id  INT NOT NULL,
  UNIQUE KEY uk_pu_cobro (club_id, pedido_id, persona_id, cuota),
  CONSTRAINT fk_puc_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_puc_pedido FOREIGN KEY (pedido_id) REFERENCES pedidos_uniforme (id),
  CONSTRAINT fk_puc_factura FOREIGN KEY (factura_id) REFERENCES facturas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
