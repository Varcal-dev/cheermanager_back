-- V42 — Plantillas de evaluación (tryout): un conjunto de criterios con nombre, reutilizable.
--
-- Los criterios se toman de la rúbrica (solo modalidades Prep y Elite; Novice no tiene) o del glosario de habilidades,
-- y se materializan como criterios_evaluacion del club: así los puntajes siguen guardándose en `evaluaciones` y el
-- historial, la progresión y el portal funcionan igual que antes. La plantilla solo guarda QUÉ criterios la componen.

-- Categorías globales para los criterios que salen de la rúbrica y del glosario (tipos_categoria.categoria es UNIQUE).
INSERT IGNORE INTO tipos_categoria (categoria) VALUES ('Rúbrica Elite'), ('Rúbrica Prep'), ('Habilidades');

CREATE TABLE IF NOT EXISTS `evaluacion_plantillas` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `club_id` INT NOT NULL,
    `nombre` VARCHAR(120) NOT NULL,
    `tipo` VARCHAR(20) NOT NULL DEFAULT 'TRYOUT',
    `modalidad` VARCHAR(20) NULL,
    `descripcion` VARCHAR(300) NULL,
    `activa` TINYINT(1) NOT NULL DEFAULT 1,
    `creada_en` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_evaluacion_plantillas_club_nombre` (`club_id`, `nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `evaluacion_plantilla_criterios` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `club_id` INT NOT NULL,
    `plantilla_id` INT NOT NULL,
    `criterio_id` INT NOT NULL,
    `orden` INT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uq_plantilla_criterio` (`plantilla_id`, `criterio_id`),
    KEY `idx_plantilla_criterios_club` (`club_id`),
    CONSTRAINT `fk_plantilla_criterios_plantilla` FOREIGN KEY (`plantilla_id`)
        REFERENCES `evaluacion_plantillas` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_plantilla_criterios_criterio` FOREIGN KEY (`criterio_id`)
        REFERENCES `criterios_evaluacion` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
