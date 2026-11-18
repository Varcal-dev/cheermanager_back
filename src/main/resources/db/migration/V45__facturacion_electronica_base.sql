-- =============================================================================
-- V45 (DIAN-1): base de la facturacion electronica DIAN.
--
--  facturacion_config      : datos fiscales del club (emisor) y como emite. Una fila por club. Nace APAGADA
--                            (activa = false): mientras este apagada el sistema factura exactamente como hasta hoy.
--  documentos_electronicos : cada factura (o nota credito) electronica enviada al proveedor, con su estado,
--                            CUFE y enlace al PDF. La referencia es unica por club: reintentar no duplica la factura.
--  Permiso gestionar_facturacion_electronica (Administrador): ver/editar la configuracion y ver los documentos.
--
-- Las credenciales del proveedor (Factus) NO van aqui: son variables de entorno del servidor.
-- Es idempotente.
-- =============================================================================

CREATE TABLE IF NOT EXISTS facturacion_config (
  id                   INT AUTO_INCREMENT PRIMARY KEY,
  club_id              INT NOT NULL,
  activa               BOOLEAN NOT NULL DEFAULT FALSE,
  proveedor            VARCHAR(20) NOT NULL DEFAULT 'FACTUS',
  ambiente             VARCHAR(12) NOT NULL DEFAULT 'PRUEBAS',
  emitir_al            VARCHAR(10) NOT NULL DEFAULT 'PAGO',
  tipo_persona         VARCHAR(10) NULL,
  nit                  VARCHAR(15) NULL,
  dv                   VARCHAR(1) NULL,
  razon_social         VARCHAR(160) NULL,
  nombre_comercial     VARCHAR(160) NULL,
  responsable_iva      BOOLEAN NOT NULL DEFAULT FALSE,
  tratamiento_iva      VARCHAR(10) NOT NULL DEFAULT 'EXCLUIDO',
  tarifa_iva           DECIMAL(5,2) NOT NULL DEFAULT 0,
  municipio_codigo     VARCHAR(10) NULL,
  direccion            VARCHAR(200) NULL,
  telefono             VARCHAR(30) NULL,
  email                VARCHAR(150) NULL,
  rango_numeracion_id  INT NULL,
  prefijo              VARCHAR(10) NULL,
  actualizado_en       DATETIME NULL,
  UNIQUE KEY uk_fact_config_club (club_id),
  CONSTRAINT fk_fact_config_club FOREIGN KEY (club_id) REFERENCES clubes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS documentos_electronicos (
  id             INT AUTO_INCREMENT PRIMARY KEY,
  club_id        INT NOT NULL,
  factura_id     INT NOT NULL,
  pago_id        INT NULL,
  tipo           VARCHAR(15) NOT NULL DEFAULT 'FACTURA',
  estado         VARCHAR(12) NOT NULL DEFAULT 'PENDIENTE',
  referencia     VARCHAR(60) NOT NULL,
  ambiente       VARCHAR(12) NOT NULL DEFAULT 'PRUEBAS',
  numero         VARCHAR(30) NULL,
  cufe           VARCHAR(160) NULL,
  url_pdf        VARCHAR(400) NULL,
  mensaje_error  VARCHAR(500) NULL,
  intentos       INT NOT NULL DEFAULT 0,
  creado_en      DATETIME NOT NULL,
  emitido_en     DATETIME NULL,
  UNIQUE KEY uk_doc_elec_referencia (club_id, referencia),
  KEY idx_doc_elec_factura (club_id, factura_id),
  KEY idx_doc_elec_estado (club_id, estado),
  CONSTRAINT fk_doc_elec_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_doc_elec_factura FOREIGN KEY (factura_id) REFERENCES facturas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO permisos (nombre) VALUES ('gestionar_facturacion_electronica');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre = 'gestionar_facturacion_electronica'
 WHERE r.nombre = 'Administrador';
