-- =============================================================================
-- V15: cierre de caja y conciliacion de pagos (S6).
--
--  * pagos: quien lo registro y cuando (para auditar la caja), referencia del comprobante bancario y marca de
--    conciliacion con el extracto del banco. Los pagos anteriores quedan con esos datos en NULL.
--  * cierres_caja: un cierre por club y dia. Mientras un dia esta CERRADA no se registran, confirman ni anulan
--    pagos "Pagado" de esa fecha; para tocarlo hay que REABRIR el dia (con motivo y permiso propio).
--  * permisos nuevos: ver_caja, cerrar_caja, reabrir_caja, conciliar_pagos.
--      Administrador: los cuatro. Tesorero: ver_caja, cerrar_caja y conciliar_pagos. Los clubes nuevos los
--      reciben por RolesPlantilla (Administrador toma todo lo que no es de plataforma).
-- Es idempotente en los datos (INSERT IGNORE); el ALTER/CREATE se aplican una sola vez, como cualquier migracion.
-- =============================================================================

ALTER TABLE pagos
    ADD COLUMN registrado_por_usuario_id INT          NULL,
    ADD COLUMN creado_en                 DATETIME(6)  NULL,
    ADD COLUMN referencia                VARCHAR(100) NULL,
    ADD COLUMN conciliado_en             DATETIME(6)  NULL,
    ADD COLUMN conciliado_por_usuario_id INT          NULL;

CREATE INDEX idx_pagos_club_fecha ON pagos (club_id, fecha);

CREATE TABLE IF NOT EXISTS cierres_caja (
    id                       INT           NOT NULL AUTO_INCREMENT,
    club_id                  INT           NOT NULL,
    fecha                    DATE          NOT NULL,
    estado                   VARCHAR(20)   NOT NULL,
    revision                 INT           NOT NULL DEFAULT 1,
    total_pagado             DECIMAL(12,2) NOT NULL,
    efectivo_esperado        DECIMAL(12,2) NOT NULL,
    efectivo_contado         DECIMAL(12,2) NULL,
    diferencia               DECIMAL(12,2) NULL,
    detalle_json             VARCHAR(4000) NULL,
    nota                     VARCHAR(300)  NULL,
    cerrado_por_usuario_id   INT           NULL,
    cerrado_en               DATETIME(6)   NULL,
    reabierto_por_usuario_id INT           NULL,
    reabierto_en             DATETIME(6)   NULL,
    motivo_reapertura        VARCHAR(300)  NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cierre_club_fecha (club_id, fecha),
    KEY idx_cierres_club (club_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO permisos (nombre) VALUES ('ver_caja');
INSERT IGNORE INTO permisos (nombre) VALUES ('cerrar_caja');
INSERT IGNORE INTO permisos (nombre) VALUES ('reabrir_caja');
INSERT IGNORE INTO permisos (nombre) VALUES ('conciliar_pagos');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre IN ('ver_caja', 'cerrar_caja', 'reabrir_caja', 'conciliar_pagos')
 WHERE r.nombre = 'Administrador';

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre IN ('ver_caja', 'cerrar_caja', 'conciliar_pagos')
 WHERE r.nombre = 'Tesorero';
