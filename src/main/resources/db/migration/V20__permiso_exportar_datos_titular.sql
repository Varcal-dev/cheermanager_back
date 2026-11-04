-- =============================================================================
-- V20: copia de los datos de un titular (derecho de acceso, bloque legal L6).
-- Permiso nuevo exportar_datos_titular: incluye datos de salud, por eso solo se da al Administrador de cada club
-- (los clubes nuevos lo reciben solos; los demas roles se ajustan desde Roles y Permisos). Idempotente.
-- =============================================================================

INSERT IGNORE INTO permisos (nombre) VALUES ('exportar_datos_titular');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre = 'exportar_datos_titular'
 WHERE r.nombre = 'Administrador';
