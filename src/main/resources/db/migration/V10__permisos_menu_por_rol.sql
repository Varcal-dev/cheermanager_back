-- V10: permisos de menu y de apoyo que faltaban en los roles sembrados.
-- Hallazgos del manual de usuario (29-oct-2026):
--  * ver_dash_personas solo lo tenian Administrador y Super Administrador: Coordinador, Director,
--    Entrenador y Asistente tenian permiso para Deportistas/Consentimientos pero no veian el menu Personas.
--  * ver_nivel nadie lo tenia salvo Administrador: la pantalla Niveles quedaba cerrada para el resto.
--  * El Entrenador sembrado no tenia ver_grupo, que usa la pantalla de Asistencias (/api/grupos/con-deportistas).
--  * Director y Coordinador no veian el menu Eventos; el Tesorero tampoco veia Eventos ni Tienda.
-- Es idempotente (INSERT IGNORE) y se aplica por NOMBRE de rol en cada club, como V8 y V9.
-- Los roles personalizados de un club no se tocan: se ajustan desde Roles y Permisos.
-- NO se da ver_dash_finanzas al Coordinador Administrativo: decision pendiente del club.

-- Menu Personas y pantalla Niveles para quienes operan el dia a dia
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre IN ('ver_dash_personas', 'ver_nivel')
 WHERE r.nombre IN ('Coordinador Administrativo', 'Director Deportivo', 'Entrenador', 'Asistente');

-- El Entrenador necesita ver los grupos para tomar asistencia
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre = 'ver_grupo'
 WHERE r.nombre = 'Entrenador';

-- Eventos: Director y Coordinador ya crean eventos; el Tesorero necesita abrir Eventos para cobrarlos
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre = 'ver_dash_eventos'
 WHERE r.nombre IN ('Director Deportivo', 'Coordinador Administrativo', 'Tesorero');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre = 'ver_evento'
 WHERE r.nombre = 'Tesorero';

-- Tienda para el Tesorero: ya puede registrar y anular ventas; ver productos es necesario para armar una venta
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre IN ('ver_dash_tienda', 'ver_producto')
 WHERE r.nombre = 'Tesorero';
