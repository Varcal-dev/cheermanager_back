-- =============================================================================
-- V12: planes comerciales definitivos y panel de finanzas para el Coordinador.
--
--  1. El Coordinador Administrativo recibe ver_dash_finanzas en todos los clubes (decision del 31-oct-2026).
--     La plantilla de clubes nuevos (RolesPlantilla) ya lo trae.
--  2. Los planes sembrados en V11 pasan a los planes comerciales:
--       Start      75 deportistas activos   $ 99.900 COP/mes
--       Pro       200                       $199.900
--       Elite     500                       $349.900
--       Enterprise sin tope de plan         desde $599.900 (precio y limite se pactan en el contrato:
--                                           se fijan por club en "limite propio de deportistas")
--     Los codigos 'inicial' y 'club' de V11 se renombran a 'start' y 'pro' (los clubes ya asignados los conservan).
--     Las ampliaciones por bloques (Start +25 por $25.000, Pro +50 por $35.000) se aplican subiendo el
--     "limite propio" del club desde la pantalla de Clubes y registrando el cobro.
-- Es idempotente: si los codigos ya fueron renombrados, los UPDATE no hacen nada.
-- =============================================================================

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre = 'ver_dash_finanzas'
 WHERE r.nombre = 'Coordinador Administrativo';

UPDATE planes_saas SET codigo = 'start', nombre = 'CheerManager Start', max_deportistas = 75,
       precio_mensual = 99900, orden = 1 WHERE codigo = 'inicial';
UPDATE planes_saas SET codigo = 'pro', nombre = 'CheerManager Pro', max_deportistas = 200,
       precio_mensual = 199900, orden = 2 WHERE codigo = 'club';
UPDATE planes_saas SET nombre = 'CheerManager Elite', max_deportistas = 500,
       precio_mensual = 349900, orden = 3 WHERE codigo = 'elite';

INSERT IGNORE INTO planes_saas (codigo, nombre, max_deportistas, precio_mensual, orden)
VALUES ('enterprise', 'CheerManager Enterprise', NULL, 599900, 4);
