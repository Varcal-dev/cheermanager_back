-- Tipos de plan de pago POR CLUB (antes eran un catalogo global editable por cualquier club).
-- 1) Cada (tipo, club) que ya usan planes_pago pasa a ser una copia propia de ese club y los planes se reapuntan.
-- 2) Los tipos globales que quedan (sin uso, p.ej. los sembrados en V22) se eliminan.
-- 3) Cada club real (id <> 1) que quede sin tipos recibe los tipos base.
-- 4) club_id pasa a NOT NULL con llave foranea a clubes.

ALTER TABLE tipos_planes_pago ADD COLUMN club_id INT NULL;
ALTER TABLE tipos_planes_pago ADD COLUMN tmp_old_id INT NULL;

INSERT INTO tipos_planes_pago (nombre, categoria, duracion_meses, descripcion, club_id, tmp_old_id)
SELECT t.nombre, t.categoria, t.duracion_meses, t.descripcion, m.club_id, t.id
FROM tipos_planes_pago t
JOIN (SELECT DISTINCT tipo_plan_id, club_id FROM planes_pago) m ON m.tipo_plan_id = t.id
WHERE t.club_id IS NULL;

UPDATE planes_pago p
JOIN tipos_planes_pago n ON n.tmp_old_id = p.tipo_plan_id AND n.club_id = p.club_id
SET p.tipo_plan_id = n.id;

DELETE FROM tipos_planes_pago WHERE club_id IS NULL;

ALTER TABLE tipos_planes_pago DROP COLUMN tmp_old_id;

INSERT INTO tipos_planes_pago (nombre, categoria, duracion_meses, descripcion, club_id)
SELECT d.nombre, d.categoria, d.duracion_meses, d.descripcion, c.id
FROM clubes c
CROSS JOIN (
    SELECT 'Inscripción anual' AS nombre, 'inscripcion' AS categoria, 12 AS duracion_meses,
           'Cobro único al inscribir al deportista en la temporada' AS descripcion
    UNION ALL SELECT 'Mensualidad', 'mensualidad', 1, 'Cuota que se factura cada mes'
    UNION ALL SELECT 'Mensualidad trimestral', 'mensualidad', 3, 'Cuota que cubre tres meses'
    UNION ALL SELECT 'Mensualidad semestral', 'mensualidad', 6, 'Cuota que cubre seis meses'
    UNION ALL SELECT 'Mensualidad anual', 'mensualidad', 12, 'Cuota que cubre doce meses'
) d
WHERE c.id <> 1
  AND NOT EXISTS (SELECT 1 FROM tipos_planes_pago x WHERE x.club_id = c.id);

ALTER TABLE tipos_planes_pago MODIFY club_id INT NOT NULL;
CREATE INDEX idx_tipos_planes_pago_club ON tipos_planes_pago (club_id);
ALTER TABLE tipos_planes_pago ADD CONSTRAINT fk_tipos_planes_pago_club FOREIGN KEY (club_id) REFERENCES clubes (id);
