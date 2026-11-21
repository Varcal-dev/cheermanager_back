-- =============================================================================
-- V47 (FIN-01): una sola factura de mensualidad por inscripcion y periodo, impuesta por la BD.
--
-- Antes la unica guardia era una consulta en InscripcionService.generarCobrosMensualidad ("ya existe?")
-- seguida de un INSERT: si el job diario y el boton "Generar cobros" corrian a la vez, ambos leian
-- "no existe" y ambos insertaban (el acudiente veia dos mensualidades del mismo mes).
--
-- UNIQUE (club_id, inscripcion_id, periodo). Las facturas sin periodo (inscripcion, tienda, manuales)
-- tienen periodo NULL y MySQL permite varios NULL en un indice UNIQUE: no se ven afectadas.
-- Una factura ANULADA tambien ocupa su periodo (el codigo ya la cuenta como "ya facturado").
--
-- SI YA HAY DUPLICADOS la migracion se DETIENE (no borra ni cambia nada) con un error que nombra
-- FIN01_HAY_FACTURAS_DUPLICADAS...: revisalos antes de desplegar con la consulta de
-- docs/GUIA_OPERACION.md (seccion "FIN-01"), anula la factura sobrante con su motivo y reintenta.
-- Es re-ejecutable.
-- =============================================================================

SET @dup = (SELECT COUNT(*) FROM (
              SELECT 1 FROM facturas
               WHERE periodo IS NOT NULL AND inscripcion_id IS NOT NULL
               GROUP BY club_id, inscripcion_id, periodo
              HAVING COUNT(*) > 1) d);
SET @s = IF(@dup > 0,
            'SELECT * FROM FIN01_HAY_FACTURAS_DUPLICADAS_REVISAR_ANTES_DE_MIGRAR',
            'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

SET @s = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE()
              AND table_name = 'facturas' AND index_name = 'uk_factura_inscripcion_periodo') = 0,
            'CREATE UNIQUE INDEX uk_factura_inscripcion_periodo ON facturas (club_id, inscripcion_id, periodo)',
            'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
