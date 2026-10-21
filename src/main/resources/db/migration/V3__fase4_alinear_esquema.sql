-- =============================================================================
-- V3 (Fase 4): alinea el esquema de los scripts 01/02 con lo que las entidades JPA esperan.
-- Detectado al cargar los scripts en un MySQL 8 real y compararlos con el modelo:
--   * faltaban columnas que antes creaba ddl-auto=update (facturas.periodo,
--     inscripciones.plan_mensualidad_id, grupo_entrenador: id/fecha_inicio/fecha_fin/rol);
--   * quedaban columnas heredadas NOT NULL que el modelo ya no usa: cualquier INSERT de la app
--     fallaba (asistencias, contactos de emergencia, ventas, inventario, productos);
--   * el Administrador de cada club no tenia gestionar_roles/asignar_permiso.
-- Es re-ejecutable (cada cambio comprueba antes si ya esta hecho), asi sirve tanto para una base
-- nueva (V1+V2+V3) como para una base que ya se creo con los scripts (baseline = 2 -> solo V3).
-- =============================================================================

-- 1) facturas.periodo (YYYY-MM; evita cobrar dos veces el mismo mes) ------------------------------
SET @s = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
              AND table_name = 'facturas' AND column_name = 'periodo') = 0,
            'ALTER TABLE facturas ADD COLUMN periodo VARCHAR(7) NULL', 'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 2) inscripciones.plan_mensualidad_id (+ FK compuesta: mismo club) -------------------------------
SET @s = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
              AND table_name = 'inscripciones' AND column_name = 'plan_mensualidad_id') = 0,
            'ALTER TABLE inscripciones ADD COLUMN plan_mensualidad_id INT NULL, ADD CONSTRAINT fk_insc_plan_mensualidad_club FOREIGN KEY (club_id, plan_mensualidad_id) REFERENCES planes_pago (club_id, id)',
            'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;

-- 3) grupo_entrenador: de tabla puente simple a historial de asignaciones (id, fechas, rol) --------
SET @s = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
              AND table_name = 'grupo_entrenador' AND column_name = 'id') = 0,
            'ALTER TABLE grupo_entrenador ADD INDEX idx_ge_grupo (grupo_entrenamiento_id), ADD INDEX idx_ge_entrenador (entrenador_id)',
            'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
SET @s = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE()
              AND table_name = 'grupo_entrenador' AND column_name = 'id') = 0,
            'ALTER TABLE grupo_entrenador DROP PRIMARY KEY, ADD COLUMN id INT NOT NULL AUTO_INCREMENT FIRST, ADD PRIMARY KEY (id), ADD COLUMN fecha_inicio DATE NULL, ADD COLUMN fecha_fin DATE NULL, ADD COLUMN rol VARCHAR(255) NULL',
            'DO 0');
PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
-- Las asignaciones que ya existian no tenian fecha: se toman como vigentes desde hoy (fecha_fin NULL = activa).
UPDATE grupo_entrenador SET fecha_inicio = CURDATE() WHERE fecha_inicio IS NULL AND id > 0;

-- 4) Columnas heredadas que el modelo actual no escribe: dejan de bloquear los INSERT --------------
ALTER TABLE asistencias           MODIFY horario_entrenamiento_id INT NULL;
ALTER TABLE contactos_emergencia  MODIFY deportista_id            INT NULL;
ALTER TABLE detalles_venta        MODIFY factura_id               INT NULL,
                                  MODIFY producto_id              INT NULL;
ALTER TABLE movimientos_inventario MODIFY producto_id             INT NULL;
ALTER TABLE productos             MODIFY precio          DECIMAL(10,2) NOT NULL DEFAULT 0,
                                  MODIFY cantidad_stock  INT           NOT NULL DEFAULT 0;

-- 5) Permisos de gestion de roles para el Administrador de cada club ------------------------------
-- DECISION: el script de datos original los reservaba al Super Administrador, pero un club nuevo
-- (POST /api/clubes) ya recibe a su Administrador con ellos y la app impide conceder permisos de
-- plataforma desde un club. Asi el Administrador de Ratels queda igual que el de cualquier club.
-- Para revertirlo: DELETE rp FROM rol_permisos rp JOIN permisos p ON p.id = rp.permiso_id
--                  JOIN roles r ON r.id = rp.rol_id
--                  WHERE r.nombre = 'Administrador' AND p.nombre IN ('gestionar_roles','asignar_permiso');
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r JOIN permisos p ON p.nombre IN ('gestionar_roles', 'asignar_permiso')
WHERE r.nombre = 'Administrador' AND r.club_id <> 1;
