-- =============================================================================
-- V41 (S1b-3): ventanas al bajar de plan y plan de prueba "todo incluido".
--
--  1. Al bajar de plan, cada funcion que el club pierde pasa 30 dias en SOLO LECTURA (ver y descargar, no crear ni
--     editar) y luego queda bloqueada. Los datos NUNCA se borran y se pueden descargar siempre (autoservicio).
--     Los dias salen de planes_saas.dias_solo_lectura_al_bajar del plan de origen (30 por defecto).
--  2. Plan "prueba": todas las funciones y el cupo de Start (75). Los clubes nuevos nacen con el.
--     Al pasar a un plan de pago, lo que ese plan no trae entra en la ventana de solo lectura.
--
--  Tablas de PLATAFORMA (sin @TenantId): se leen con SQL directo. Idempotente.
-- =============================================================================

SET @c := (SELECT COUNT(*) FROM information_schema.columns
            WHERE table_schema = DATABASE() AND table_name = 'planes_saas' AND column_name = 'dias_solo_lectura_al_bajar');
SET @sql := IF(@c = 0, 'ALTER TABLE planes_saas ADD COLUMN dias_solo_lectura_al_bajar INT NOT NULL DEFAULT 30', 'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS clubes_caracteristicas_transicion (
  id                 INT         NOT NULL AUTO_INCREMENT,
  club_id            INT         NOT NULL,
  caracteristica     VARCHAR(40) NOT NULL,
  plan_origen_id     INT         NULL,
  solo_lectura_hasta DATE        NOT NULL,
  creado_en          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  PRIMARY KEY (id),
  UNIQUE KEY uk_club_carac_transicion (club_id, caracteristica),
  CONSTRAINT fk_cctrans_club  FOREIGN KEY (club_id)        REFERENCES clubes (id),
  CONSTRAINT fk_cctrans_carac FOREIGN KEY (caracteristica) REFERENCES caracteristicas_saas (codigo),
  CONSTRAINT fk_cctrans_plan  FOREIGN KEY (plan_origen_id) REFERENCES planes_saas (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO planes_saas (codigo, nombre, max_deportistas, precio_mensual, orden)
VALUES ('prueba', 'Prueba (todo incluido)', 75, 0, 0);

-- sin paquetes durante la prueba (la columna la crea V40)
UPDATE planes_saas SET max_paquetes_deportistas = 0 WHERE codigo = 'prueba';

INSERT IGNORE INTO planes_caracteristicas (plan_id, caracteristica, habilitada)
SELECT p.id, c.codigo, 1
  FROM planes_saas p
 CROSS JOIN caracteristicas_saas c
 WHERE p.codigo = 'prueba';
