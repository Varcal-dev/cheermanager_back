-- =============================================================================
-- V5: cartera (vencimiento / mora), facturas manuales y anulacion con trazabilidad.
-- Reglas de negocio (oct 2026):
--   * La factura mensual se emite el primer dia habil del mes y vence a los 10 dias; despues esta EN MORA.
--   * Se pueden crear facturas manuales (uniforme, competencia, evento, ...), ligables a un evento.
--   * Facturas y pagos NO se borran: se ANULAN con motivo, fecha y usuario (se conserva el historial).
-- =============================================================================

ALTER TABLE facturas
    ADD COLUMN fecha_vencimiento       DATE         NULL,
    ADD COLUMN concepto                VARCHAR(30)  NULL,
    ADD COLUMN evento_id               INT          NULL,
    ADD COLUMN anulada                 BIT(1)       NOT NULL DEFAULT b'0',
    ADD COLUMN fecha_anulacion         DATETIME(6)  NULL,
    ADD COLUMN motivo_anulacion        VARCHAR(300) NULL,
    ADD COLUMN anulada_por_usuario_id  INT          NULL;

ALTER TABLE pagos
    ADD COLUMN fecha_anulacion         DATETIME(6)  NULL,
    ADD COLUMN motivo_anulacion        VARCHAR(300) NULL,
    ADD COLUMN anulado_por_usuario_id  INT          NULL;

-- FK compuesta (mismo club) hacia eventos: necesita un indice unico (club_id, id) como destino.
SET @s = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE()
              AND table_name = 'eventos' AND index_name = 'uk_eventos_club_id') = 0,
            'CREATE UNIQUE INDEX uk_eventos_club_id ON eventos (club_id, id)', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

ALTER TABLE facturas
    ADD CONSTRAINT fk_facturas_evento FOREIGN KEY (club_id, evento_id) REFERENCES eventos (club_id, id);

CREATE INDEX idx_facturas_vencimiento ON facturas (club_id, fecha_vencimiento);

-- Datos existentes: vencimiento = emision + 10 dias; concepto deducido de como se generaron.
-- OJO: las facturas ANTIGUAS con saldo pendiente pasaran a figurar EN MORA si ya pasaron 10 dias.
UPDATE facturas SET fecha_vencimiento = DATE_ADD(fecha_emision, INTERVAL 10 DAY)
 WHERE fecha_vencimiento IS NULL AND id > 0;

UPDATE facturas SET concepto = CASE
        WHEN periodo IS NOT NULL THEN 'MENSUALIDAD'
        WHEN inscripcion_id IS NOT NULL THEN 'INSCRIPCION'
        WHEN numero_factura LIKE 'FAC-VTA-%' THEN 'TIENDA'
        ELSE 'OTRO' END
 WHERE concepto IS NULL AND id > 0;
