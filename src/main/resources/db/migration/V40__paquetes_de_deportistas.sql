-- =============================================================================
-- V40 (S1b-2): paquetes de deportistas adicionales.
--
--  Cupo efectivo de un club = (limite propio del club, o el del plan) + paquetes vigentes DE SU PLAN.
--  Si el club cambia de plan, los paquetes del plan anterior dejan de sumar (no se borran).
--  planes_saas.max_paquetes_deportistas limita cuantos paquetes (unidades) puede tener vigentes un club.
--  Precio del paquete = precio final del plan / cupo del plan * tamano del paquete (mismo valor por deportista).
--  Se cobran dentro de la mensualidad; por ahora los asigna el superadmin (el cobro en linea llega con la pasarela).
--
--  Precios de planes: se suma el margen de la pasarela (comision 2,65% + $700 + IVA) para que lo recibido tras la
--  comision equivalga al precio anterior: nuevo = (objetivo + 833) / 0,968465, redondeado a terminacion 900.
--  Solo se actualizan si siguen en el valor que dejo V12.
--
--  Tablas de PLATAFORMA: sin @TenantId, se leen con SQL directo. Idempotente.
-- =============================================================================

SET @c := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'planes_saas' AND column_name = 'max_paquetes_deportistas');
SET @sql := IF(@c = 0, 'ALTER TABLE planes_saas ADD COLUMN max_paquetes_deportistas INT NOT NULL DEFAULT 2', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Enterprise se pacta por contrato y el plan fundador es ilimitado: sin paquetes
UPDATE planes_saas SET max_paquetes_deportistas = 0 WHERE codigo IN ('enterprise', 'fundador');

UPDATE planes_saas SET precio_mensual = 104900 WHERE codigo = 'start'      AND precio_mensual = 99900;
UPDATE planes_saas SET precio_mensual = 207900 WHERE codigo = 'pro'        AND precio_mensual = 199900;
UPDATE planes_saas SET precio_mensual = 362900 WHERE codigo = 'elite'      AND precio_mensual = 349900;
UPDATE planes_saas SET precio_mensual = 620900 WHERE codigo = 'enterprise' AND precio_mensual = 599900;

CREATE TABLE IF NOT EXISTS paquetes_deportistas_saas (
  id                INT           NOT NULL AUTO_INCREMENT,
  plan_id           INT           NOT NULL,
  nombre            VARCHAR(80)   NOT NULL,
  deportistas_extra INT           NOT NULL,
  precio_mensual    DECIMAL(12,2) NOT NULL,
  activo            BIT(1)        NOT NULL DEFAULT b'1',
  PRIMARY KEY (id),
  UNIQUE KEY uk_paquete_plan_tam (plan_id, deportistas_extra),
  CONSTRAINT fk_paq_plan FOREIGN KEY (plan_id) REFERENCES planes_saas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO paquetes_deportistas_saas (plan_id, nombre, deportistas_extra, precio_mensual)
SELECT id, 'Paquete +25 deportistas',  25,  34900 FROM planes_saas WHERE codigo = 'start'
UNION ALL
SELECT id, 'Paquete +50 deportistas',  50,  51900 FROM planes_saas WHERE codigo = 'pro'
UNION ALL
SELECT id, 'Paquete +100 deportistas', 100, 72600 FROM planes_saas WHERE codigo = 'elite';

CREATE TABLE IF NOT EXISTS clubes_paquetes_deportistas (
  id              INT           NOT NULL AUTO_INCREMENT,
  club_id         INT           NOT NULL,
  paquete_id      INT           NOT NULL,
  cantidad        INT           NOT NULL DEFAULT 1,
  fecha_inicio    DATE          NOT NULL,
  fecha_fin       DATE          NULL,
  precio_acordado DECIMAL(12,2) NULL,
  registrado_por  VARCHAR(80)   NULL,
  creado_en       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  KEY idx_club_paquetes (club_id, fecha_fin),
  CONSTRAINT fk_clubpaq_club FOREIGN KEY (club_id)    REFERENCES clubes (id),
  CONSTRAINT fk_clubpaq_paq  FOREIGN KEY (paquete_id) REFERENCES paquetes_deportistas_saas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
