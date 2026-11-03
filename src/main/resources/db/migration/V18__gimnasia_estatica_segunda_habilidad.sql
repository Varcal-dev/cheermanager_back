-- =============================================================================
-- V18: Gimnasia Estatica (Elite) — Grado de Dificultad por numero de habilidad.
--   Habilidad/Pase 1: 0.2 (Apropiada por MAYORIA) o 0.4 (Avanzada/Elite por MAYORIA)
--   Habilidad/Pase 2: 0.4 (Avanzada por GRAN PARTE) o 0.6 (Elite por GRAN PARTE)
--   Tope del driver: 1.0
-- Hasta V17 el sistema aplicaba 0.2/0.4 a las dos habilidades (perdia hasta 0.2).
-- Prep NO cambia: una sola habilidad de 0.3 o 0.5 (tope 0.5). Gimnasia con carrera tampoco.
-- =============================================================================
ALTER TABLE tope_driver_nivel ADD COLUMN valor_basico_habilidad2 DECIMAL(4, 2) NULL;
ALTER TABLE tope_driver_nivel ADD COLUMN valor_alto_habilidad2 DECIMAL(4, 2) NULL;

UPDATE tope_driver_nivel
   SET valor_basico_habilidad2 = 0.40, valor_alto_habilidad2 = 0.60
 WHERE nivel_id IN (SELECT id FROM niveles_competencia WHERE modalidad = 'Elite')
   AND driver_id IN (
        SELECT d.id FROM drivers_sub_criterio d
          JOIN sub_criterios_rubrica sc ON sc.id = d.sub_criterio_id
         WHERE d.nombre = 'Grado de Dificultad' AND sc.nombre = 'Dificultad de Gimnasia Estatica');
