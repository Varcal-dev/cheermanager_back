-- =============================================================================
-- V13: importacion masiva de deportistas y exportes de listados (S3).
--
-- Los permisos importar_datos / exportar_datos ya existian, pero estan reservados a la plataforma
-- (RolesPlantilla.SOLO_PLATAFORMA). Para que cada club importe y exporte SUS datos se crean dos permisos propios:
--   importar_deportistas : cargar deportistas (y su acudiente) desde un archivo CSV.
--   exportar_listados    : descargar listados (deportistas, acudientes) en CSV.
-- Se dan al Administrador de cada club. Los clubes nuevos los reciben solos (el Administrador de plantilla
-- toma todos los permisos que no son de plataforma). Los demas roles se ajustan desde Roles y Permisos.
-- Es idempotente (INSERT IGNORE).
-- =============================================================================

INSERT IGNORE INTO permisos (nombre) VALUES ('importar_deportistas');
INSERT IGNORE INTO permisos (nombre) VALUES ('exportar_listados');

INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r JOIN permisos p ON p.nombre IN ('importar_deportistas', 'exportar_listados')
 WHERE r.nombre = 'Administrador';
