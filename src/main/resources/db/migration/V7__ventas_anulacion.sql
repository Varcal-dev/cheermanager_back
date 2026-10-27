-- =============================================================================
-- V7: anulacion de ventas de tienda.
-- Las ventas no se borran: se ANULAN con motivo, fecha y usuario (igual que facturas y pagos).
-- Al anular se reversa el stock con movimientos de entrada y se anulan su pago y su factura propia.
-- =============================================================================

ALTER TABLE ventas
    ADD COLUMN anulada                 BIT(1)       NOT NULL DEFAULT b'0',
    ADD COLUMN fecha_anulacion         DATETIME(6)  NULL,
    ADD COLUMN motivo_anulacion        VARCHAR(300) NULL,
    ADD COLUMN anulada_por_usuario_id  INT          NULL;
