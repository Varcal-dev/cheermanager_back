-- Tipos de plan de pago base (catalogo de duraciones). Sin ellos, el selector "Tipo de plan" de Planes de
-- Inscripcion / Mensualidad queda vacio y no se puede crear ningun plan (los tipos no se sembraban en ninguna migracion).
-- Es idempotente: solo inserta el tipo si no existe ya uno con ese nombre y esa categoria.
-- Nota: tipos_planes_pago es un catalogo GLOBAL (sin club_id): estos tipos los ven todos los clubes.

INSERT INTO tipos_planes_pago (nombre, categoria, duracion_meses, descripcion)
SELECT t.nombre, t.categoria, t.duracion_meses, t.descripcion
FROM (
    SELECT 'Inscripción anual' AS nombre, 'inscripcion' AS categoria, 12 AS duracion_meses,
           'Cobro único al inscribir al deportista en la temporada' AS descripcion
    UNION ALL
    SELECT 'Mensualidad', 'mensualidad', 1, 'Cuota que se factura cada mes'
    UNION ALL
    SELECT 'Mensualidad trimestral', 'mensualidad', 3, 'Cuota que cubre tres meses'
    UNION ALL
    SELECT 'Mensualidad semestral', 'mensualidad', 6, 'Cuota que cubre seis meses'
    UNION ALL
    SELECT 'Mensualidad anual', 'mensualidad', 12, 'Cuota que cubre doce meses'
) AS t
WHERE NOT EXISTS (
    SELECT 1 FROM tipos_planes_pago x WHERE x.nombre = t.nombre AND x.categoria = t.categoria
);
