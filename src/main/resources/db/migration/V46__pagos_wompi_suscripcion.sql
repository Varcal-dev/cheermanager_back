-- =============================================================================
-- V46 (PAG-1): pago en linea de la suscripcion del club a VarCal con Wompi.
--
--  wompi_transacciones : un intento de pago por fila. TABLA DE PLATAFORMA (sin tenant): club_id es solo un dato.
--                        referencia unica = la que se envia a Wompi; el webhook la usa para saber de quien es el pago.
--                        estado: CREADA -> APROBADA | RECHAZADA | ANULADA | ERROR. Una APROBADA nunca se degrada.
--  Permiso pagar_suscripcion (Administrador del club): iniciar el pago de su plan.
--
-- Las llaves de Wompi NO van aqui: son variables de entorno del servidor. Es idempotente.
-- =============================================================================

CREATE TABLE IF NOT EXISTS wompi_transacciones (
  id               INT AUTO_INCREMENT PRIMARY KEY,
  club_id          INT NOT NULL,
  referencia       VARCHAR(60) NOT NULL,
  meses            INT NOT NULL,
  monto_centavos   BIGINT NOT NULL,
  estado           VARCHAR(12) NOT NULL DEFAULT 'CREADA',
  wompi_id         VARCHAR(60) NULL,
  mensaje          VARCHAR(300) NULL,
  creado_por       VARCHAR(80) NULL,
  creado_en        DATETIME NOT NULL,
  actualizado_en   DATETIME NULL,
  UNIQUE KEY uk_wompi_referencia (referencia),
  KEY idx_wompi_club (club_id, creado_en),
  CONSTRAINT fk_wompi_club FOREIGN KEY (club_id) REFERENCES clubes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO permisos (nombre) VALUES ('pagar_suscripcion');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre = 'pagar_suscripcion'
 WHERE r.nombre = 'Administrador';
