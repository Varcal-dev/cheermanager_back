-- V8: el CRUD de criterios de evaluacion ya tiene endpoints (crear/modificar/eliminar).
-- El Director Deportivo ya podia crear criterios; ahora tambien puede editarlos.
-- Eliminar queda para quien lo tenga (Administrador) y solo procede si el criterio no tiene evaluaciones.
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
  FROM roles r
  JOIN permisos p ON p.nombre = 'modificar_criterio_evaluacion'
 WHERE r.nombre = 'Director Deportivo';
