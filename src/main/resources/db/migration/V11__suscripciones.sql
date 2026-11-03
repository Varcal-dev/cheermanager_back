-- =============================================================================
-- V11: plan y suscripcion por club (el SaaS cobra a cada club).
--
--  * planes_saas: catalogo GLOBAL de planes (sin club_id). max_deportistas NULL = ilimitado.
--    precio_mensual NULL = "por definir" (se fija desde la base o desde la pantalla de suscripciones).
--  * clubes: plan, estado de la suscripcion, hasta cuando esta vigente, dias de gracia y un limite propio
--    opcional (limite_deportistas) que prevalece sobre el del plan.
--  * suscripciones_pagos: bitacora de los cobros manuales (renovaciones). Es tabla de plataforma, no de club.
--
-- Regla de vigencia (VigenciaSuscripcion):
--    estado CANCELADA                                  -> solo lectura
--    vigente_hasta NULL                                -> sin vencimiento
--    hoy <= vigente_hasta                              -> vigente
--    ACTIVA y hoy <= vigente_hasta + dias_gracia       -> en gracia (todo funciona, con aviso)
--    cualquier otro caso (incluida la PRUEBA vencida)  -> solo lectura (GET si, POST/PUT/PATCH/DELETE no)
-- El club Plataforma (id 1) nunca vence.
--
-- Los limites y precios sembrados son valores de partida EDITABLES: ajustalos antes de vender.
-- Los clubes que ya existen (Plataforma y Ratels) quedan en el plan "fundador": ilimitado y sin vencimiento.
-- =============================================================================

CREATE TABLE IF NOT EXISTS planes_saas (
  id               INT NOT NULL AUTO_INCREMENT,
  codigo           VARCHAR(30) NOT NULL,
  nombre           VARCHAR(80) NOT NULL,
  max_deportistas  INT NULL,
  precio_mensual   DECIMAL(12,2) NULL,
  activo           BIT(1) NOT NULL DEFAULT b'1',
  orden            INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_planes_saas_codigo (codigo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO planes_saas (codigo, nombre, max_deportistas, precio_mensual, orden) VALUES
  ('fundador', 'Fundador (sin limite)', NULL, NULL, 0),
  ('inicial',  'Inicial',               50,   NULL, 1),
  ('club',     'Club',                  150,  NULL, 2),
  ('elite',    'Elite',                 400,  NULL, 3);

-- Columnas nuevas en clubes (idempotente: se agregan solo si faltan)
SET @c := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'clubes' AND column_name = 'plan_id');
SET @sql := IF(@c = 0, 'ALTER TABLE clubes ADD COLUMN plan_id INT NULL', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'clubes' AND column_name = 'estado_suscripcion');
SET @sql := IF(@c = 0, 'ALTER TABLE clubes ADD COLUMN estado_suscripcion VARCHAR(12) NOT NULL DEFAULT ''PRUEBA''', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'clubes' AND column_name = 'vigente_hasta');
SET @sql := IF(@c = 0, 'ALTER TABLE clubes ADD COLUMN vigente_hasta DATE NULL', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'clubes' AND column_name = 'dias_gracia');
SET @sql := IF(@c = 0, 'ALTER TABLE clubes ADD COLUMN dias_gracia INT NOT NULL DEFAULT 5', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'clubes' AND column_name = 'limite_deportistas');
SET @sql := IF(@c = 0, 'ALTER TABLE clubes ADD COLUMN limite_deportistas INT NULL', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'clubes' AND column_name = 'notas_suscripcion');
SET @sql := IF(@c = 0, 'ALTER TABLE clubes ADD COLUMN notas_suscripcion VARCHAR(500) NULL', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @c := (SELECT COUNT(*) FROM information_schema.table_constraints
            WHERE table_schema = DATABASE() AND table_name = 'clubes' AND constraint_name = 'fk_clubes_plan_saas');
SET @sql := IF(@c = 0, 'ALTER TABLE clubes ADD CONSTRAINT fk_clubes_plan_saas FOREIGN KEY (plan_id) REFERENCES planes_saas (id)', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Clubes existentes: plan fundador, activos y sin vencimiento (solo los que aun no tienen plan)
UPDATE clubes
   SET plan_id = (SELECT id FROM planes_saas WHERE codigo = 'fundador'),
       estado_suscripcion = 'ACTIVA',
       vigente_hasta = NULL
 WHERE plan_id IS NULL;

CREATE TABLE IF NOT EXISTS suscripciones_pagos (
  id             INT NOT NULL AUTO_INCREMENT,
  club_id        INT NOT NULL,
  fecha_pago     DATE NOT NULL,
  monto          DECIMAL(12,2) NOT NULL,
  meses          INT NOT NULL,
  metodo         VARCHAR(40) NULL,
  referencia     VARCHAR(120) NULL,
  periodo_desde  DATE NOT NULL,
  periodo_hasta  DATE NOT NULL,
  nota           VARCHAR(300) NULL,
  registrado_por VARCHAR(80) NULL,
  creado_en      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_suscripciones_pagos_club (club_id, fecha_pago),
  CONSTRAINT fk_suscripciones_pagos_club FOREIGN KEY (club_id) REFERENCES clubes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
