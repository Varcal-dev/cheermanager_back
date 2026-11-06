-- Los clubes dados de alta desde Plataforma nacian con niveles y divisiones pero SIN categorias por nivel
-- (V2 solo las sembro para el club 2), y no hay pantalla para crearlas: sin ellas no se puede crear ningun grupo.
-- Para cada club real (id <> 1) que no tenga ninguna, se crea la combinacion nivel x division, sin rango de
-- edad ni cupo (sin limite). Idempotente: un club con al menos una categoria no se toca.

INSERT IGNORE INTO estados_categorias_nivel (estado_categoria) VALUES ('Activa'), ('Inactiva');

INSERT INTO categorias_nivel (nombre, nivel_id, division_id, estado_categorias_nivel_id, club_id)
SELECT CONCAT(n.nombre, ' - ', d.nombre), n.id, d.id, e.id, n.club_id
FROM niveles n
JOIN divisiones d ON d.club_id = n.club_id
JOIN estados_categorias_nivel e ON e.estado_categoria = 'Activa'
WHERE n.club_id <> 1
  AND NOT EXISTS (SELECT 1 FROM categorias_nivel c WHERE c.club_id = n.club_id);
