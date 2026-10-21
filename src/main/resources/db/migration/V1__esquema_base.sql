-- Flyway V1: esquema base (script 01 del proyecto, sin CREATE DATABASE/USE: Flyway usa la base configurada).
-- NO EDITAR una vez aplicado: Flyway verifica el checksum. Los cambios van en una migracion nueva (V4, V5...).
-- =============================================================================
-- CheerManager - SCRIPT 1: BASE (estructura general del sistema)
-- Motor: MySQL 8 / MariaDB 10.x (InnoDB, utf8mb4)
-- Basado en TF_Cheermanager_V4-2 (sin datos) + ajustes de Fase 0:
--   * Sin prefijo de schema en las tablas (se usa USE).
--   * AUTO_INCREMENT en todas las PK simples `id` (67 tablas).
--   * usuarios.bloqueado_hasta (Fase 0, bloqueo temporal de cuentas).
--   * Indices duplicados/redundantes eliminados (el duplicado de usuario_roles
--     provocaba el error 1061 y cortaba el script).
--   * Fase 1 (multi-club): tabla `clubes` + club_id NOT NULL (indice y FK a clubes)
--     en 45 tablas, y unicidades POR CLUB (roles.nombre, personas.numero_documento,
--     facturas.numero_factura, sku, ...). Los datos de clubes van en el script 2.
--   * Fase 2: integridad referencial entre clubes (FK compuestas club_id+id en
--     deportistas.nivel_actual_id/convenio_id, historial_nivel_deportista.nivel_id,
--     historial_planes_deportista.plan_pago_id) y unicidad de asistencias por dia.
--   * Fase 3: acudientes, deportista_acudientes, temporadas y consentimientos (FK
--     compuestas club_id+id), inscripciones.temporada_id, y las vistas
--     vista_deportistas_no_inscritos / vista_inscripciones_detalle (con club_id).
-- Pensado para una base vacia (los indices y llaves foraneas no son re-ejecutables).
-- Ejecutar ANTES del script de datos.
-- Si tu base se llama distinto, cambia las dos primeras sentencias.
-- =============================================================================

SET NAMES utf8mb4;
-- Fase 1 (multi-club): cada club es un tenant. clubes.id=1 es la "Plataforma"
-- (solo Super Administrador); los clubes reales empiezan en id=2.
CREATE TABLE IF NOT EXISTS `clubes` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(120) NOT NULL,
    `slug` VARCHAR(60) NOT NULL,
    `activo` BIT(1) NOT NULL DEFAULT b'1',
    `fecha_creacion` DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_clubes_slug` (`slug`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tipos_planes_pago` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `categoria` VARCHAR(50) NOT NULL,
    `descripcion` TEXT,
    `duracion_meses` INT NOT NULL,
    `nombre` VARCHAR(255),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `grupos_entrenamiento` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL,
    `tipo_grupo_id` INT NOT NULL,
    `categorias_nivel_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_grupos_entrenamiento_club`
ON `grupos_entrenamiento` (`club_id`);

CREATE INDEX `idx_grupos_entrenamiento_tipo_grupo_id`
ON `grupos_entrenamiento` (`tipo_grupo_id`);

CREATE INDEX `idx_grupos_entrenamiento_categorias_nivel_id`
ON `grupos_entrenamiento` (`categorias_nivel_id`);

CREATE TABLE IF NOT EXISTS `eventos` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL,
    `fecha` DATE NOT NULL,
    `tipo_evento_id` INT NOT NULL,
    `ubicacion` VARCHAR(255),
    `tiene_resultados` tinyint NOT NULL DEFAULT 0,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_eventos_club`
ON `eventos` (`club_id`);

CREATE INDEX `idx_eventos_fecha`
ON `eventos` (`fecha`);

CREATE INDEX `idx_eventos_tipo_evento_id`
ON `eventos` (`tipo_evento_id`);

CREATE TABLE IF NOT EXISTS `registro_driver_sub_criterio` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `es_nivel_alto` BIT,
    `numero_habilidad` INT,
    `driver_id` INT NOT NULL,
    `escalon_seleccionado_id` INT,
    `registro_sub_criterio_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_registro_driver_sub_criterio_club`
ON `registro_driver_sub_criterio` (`club_id`);

CREATE INDEX `idx_registro_driver_sub_criterio_driver_id`
ON `registro_driver_sub_criterio` (`driver_id`);

CREATE INDEX `idx_registro_driver_sub_criterio_escalon_seleccionado_id`
ON `registro_driver_sub_criterio` (`escalon_seleccionado_id`);

CREATE INDEX `idx_registro_driver_sub_criterio_registro_sub_criterio_id`
ON `registro_driver_sub_criterio` (`registro_sub_criterio_id`);

CREATE TABLE IF NOT EXISTS `premios_evento` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `evento_id` INT NOT NULL,
    `descripcion` VARCHAR(255),
    `premio` VARCHAR(255),
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_premios_evento_club`
ON `premios_evento` (`club_id`);

CREATE INDEX `idx_premios_evento_evento_id`
ON `premios_evento` (`evento_id`);

CREATE TABLE IF NOT EXISTS `permisos` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sub_criterios_rubrica` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL UNIQUE,
    `seccion` VARCHAR(255) NOT NULL,
    `tipo_calculo` VARCHAR(50) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `tipos_evento` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `evento` VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `movimientos_inventario` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `producto_id` INT NOT NULL,
    `tipo` VARCHAR(50) NOT NULL,
    `cantidad` INT NOT NULL,
    `fecha` DATETIME NOT NULL,
    `usuario_id` INT NOT NULL,
    `motivo` VARCHAR(300),
    `producto_variante_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_movimientos_inventario_club`
ON `movimientos_inventario` (`club_id`);

CREATE INDEX `idx_movimientos_inventario_producto_id`
ON `movimientos_inventario` (`producto_id`);

CREATE INDEX `idx_movimientos_inventario_usuario_id`
ON `movimientos_inventario` (`usuario_id`);

CREATE INDEX `idx_movimientos_inventario_producto_variante_id`
ON `movimientos_inventario` (`producto_variante_id`);

CREATE TABLE IF NOT EXISTS `tipos_pago` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `tipo` VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `planes_pago` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(100) NOT NULL,
    `descripcion` TEXT,
    `valor_mensual` DECIMAL(10, 2) NOT NULL,
    `fecha_vigencia_inicio` DATE NOT NULL,
    `fecha_vigencia_fin` DATE,
    `activo` tinyint NOT NULL DEFAULT 1,
    `descuento_porcentaje` DECIMAL(5, 2),
    `sesiones_semanales` INT,
    `tipo_plan_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_planes_pago_club`
ON `planes_pago` (`club_id`);
CREATE UNIQUE INDEX `uk_planes_pago_club_id`
ON `planes_pago` (`club_id`, `id`);  -- Fase 2: destino de FK compuesta (club_id, id)

CREATE INDEX `idx_planes_pago_tipo_plan_id`
ON `planes_pago` (`tipo_plan_id`);

CREATE TABLE IF NOT EXISTS `historial_estado_deportista` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `deportista_id` INT NOT NULL,
    `estado_anterior_id` INT,
    `estado_nuevo_id` INT NOT NULL,
    `fecha_cambio` DATETIME NOT NULL,
    `usuario_id` INT,
    `motivo_cambio` VARCHAR(255),
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_historial_estado_deportista_club`
ON `historial_estado_deportista` (`club_id`);

CREATE INDEX `idx_historial_estado_deportista_estado_anterior_id`
ON `historial_estado_deportista` (`estado_anterior_id`);

CREATE INDEX `idx_historial_estado_deportista_usuario_id`
ON `historial_estado_deportista` (`usuario_id`);

CREATE INDEX `idx_historial_estado_deportista_deportista_id`
ON `historial_estado_deportista` (`deportista_id`);

CREATE INDEX `idx_historial_estado_deportista_fecha_cambio`
ON `historial_estado_deportista` (`fecha_cambio`);

CREATE INDEX `idx_historial_estado_deportista_estado_nuevo_id`
ON `historial_estado_deportista` (`estado_nuevo_id`);

CREATE TABLE IF NOT EXISTS `estados_categorias_nivel` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `estado_categoria` VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `categorias_nivel` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL,
    `nivel_id` INT NOT NULL,
    `division_id` INT NOT NULL,
    `año_nacimiento_min` INT,
    `año_nacimiento_max` INT,
    `cantidad_min` INT,
    `cantidad_max` INT,
    `estado_categorias_nivel_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_categorias_nivel_club`
ON `categorias_nivel` (`club_id`);

CREATE INDEX `idx_categorias_nivel_nivel_id`
ON `categorias_nivel` (`nivel_id`);

CREATE INDEX `idx_categorias_nivel_division_id`
ON `categorias_nivel` (`division_id`);

CREATE INDEX `idx_categorias_nivel_estado_categorias_nivel_id`
ON `categorias_nivel` (`estado_categorias_nivel_id`);

CREATE TABLE IF NOT EXISTS `inscripciones` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `deportista_id` INT NOT NULL,
    `plan_pago_id` INT NOT NULL,
    `fecha_inscripcion` DATE NOT NULL,
    `fecha_vencimiento` DATE,
    `estado` VARCHAR(50),
    `club_id` INT NOT NULL,
    `temporada_id` INT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_inscripciones_club`
ON `inscripciones` (`club_id`);

CREATE INDEX `idx_inscripciones_deportista_id`
ON `inscripciones` (`deportista_id`);

CREATE INDEX `idx_inscripciones_plan_pago_id`
ON `inscripciones` (`plan_pago_id`);

CREATE TABLE IF NOT EXISTS `objetivos` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL,
    `deportista_id` INT NOT NULL,
    `descripcion` VARCHAR(255),
    `fecha_creacion` DATE NOT NULL,
    `estado_objetivo_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_objetivos_club`
ON `objetivos` (`club_id`);

CREATE INDEX `idx_objetivos_deportista_id`
ON `objetivos` (`deportista_id`);

CREATE INDEX `idx_objetivos_estado_objetivo_id`
ON `objetivos` (`estado_objetivo_id`);

CREATE TABLE IF NOT EXISTS `asistencias_evento` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `deportista_id` INT NOT NULL,
    `evento_id` INT NOT NULL,
    `estado_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX `idx_asistencias_evento_deportista_id_evento_id_unique`
ON `asistencias_evento` (`deportista_id`, `evento_id`);

CREATE INDEX `idx_asistencias_evento_evento_id`
ON `asistencias_evento` (`evento_id`);

CREATE INDEX `idx_asistencias_evento_estado_id`
ON `asistencias_evento` (`estado_id`);

CREATE TABLE IF NOT EXISTS `registro_sub_criterio` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `grupo_minimo_alcanzado` VARCHAR(20),
    `habilidades_diferentes` INT,
    `puntaje_base` DECIMAL(4, 2),
    `puntaje_drivers` DECIMAL(4, 2),
    `puntaje_final` DECIMAL(4, 2),
    `puntaje_final_manual` DECIMAL(4, 2),
    `evaluacion_rutina_id` INT NOT NULL,
    `sub_criterio_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_registro_sub_criterio_club`
ON `registro_sub_criterio` (`club_id`);

CREATE INDEX `idx_registro_sub_criterio_evaluacion_rutina_id`
ON `registro_sub_criterio` (`evaluacion_rutina_id`);

CREATE INDEX `idx_registro_sub_criterio_sub_criterio_id`
ON `registro_sub_criterio` (`sub_criterio_id`);

CREATE TABLE IF NOT EXISTS `generos` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `genero` VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `niveles_competencia` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL UNIQUE,
    `modalidad` VARCHAR(255) NOT NULL,
    `activo` tinyint NOT NULL DEFAULT 1,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `facturas` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `numero_factura` VARCHAR(255) NOT NULL,
    `fecha_emision` DATE NOT NULL,
    `persona_id` INT NOT NULL,
    `inscripcion_id` INT,
    `descripcion` VARCHAR(255),
    `descuento_id` INT,
    `total` DECIMAL(10, 2) NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_facturas_club`
ON `facturas` (`club_id`);
CREATE UNIQUE INDEX `uk_factura_club_numero_factura`
ON `facturas` (`club_id`, `numero_factura`);

CREATE INDEX `idx_facturas_persona_id`
ON `facturas` (`persona_id`);

CREATE INDEX `idx_facturas_inscripcion_id`
ON `facturas` (`inscripcion_id`);

CREATE INDEX `idx_facturas_descuento_id`
ON `facturas` (`descuento_id`);

CREATE TABLE IF NOT EXISTS `planes_inscripcion` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `activo` BIT NOT NULL,
    `descripcion` TEXT,
    `fecha_creacion` DATE NOT NULL,
    `fecha_vigencia_fin` DATE,
    `fecha_vigencia_inicio` DATE NOT NULL,
    `incluye_camiseta` BIT,
    `incluye_seguro` BIT,
    `nombre` VARCHAR(100) NOT NULL,
    `precio` DECIMAL(10, 2) NOT NULL,
    `precio_base_calculado` DECIMAL(10, 2),
    `vigencia_dias` INT,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `resultados_competencia` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `evento_id` INT NOT NULL,
    `grupo_id` INT,
    `posicion` INT,
    `puntaje` DECIMAL(10, 2),
    `premio_id` INT,
    `observaciones` VARCHAR(255),
    `evaluacion_rutina_id` INT,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_resultados_competencia_club`
ON `resultados_competencia` (`club_id`);

CREATE INDEX `idx_resultados_competencia_premio_id`
ON `resultados_competencia` (`premio_id`);

CREATE INDEX `idx_resultados_competencia_evento_id`
ON `resultados_competencia` (`evento_id`);

CREATE INDEX `idx_resultados_competencia_grupo_id`
ON `resultados_competencia` (`grupo_id`);

CREATE INDEX `idx_resultados_competencia_evaluacion_rutina_id`
ON `resultados_competencia` (`evaluacion_rutina_id`);

CREATE TABLE IF NOT EXISTS `convenios` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre_empresa` VARCHAR(100) NOT NULL,
    `descuento_id` INT NOT NULL,
    `fecha_inicio` DATE,
    `fecha_fin` DATE,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_convenios_club`
ON `convenios` (`club_id`);
CREATE UNIQUE INDEX `uk_convenios_club_id`
ON `convenios` (`club_id`, `id`);  -- Fase 2: destino de FK compuesta (club_id, id)

CREATE INDEX `idx_convenios_descuento_id`
ON `convenios` (`descuento_id`);

CREATE TABLE IF NOT EXISTS `criterios_evaluacion` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL,
    `categoria_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_criterios_evaluacion_club`
ON `criterios_evaluacion` (`club_id`);

CREATE INDEX `idx_criterios_evaluacion_categoria_id`
ON `criterios_evaluacion` (`categoria_id`);

CREATE TABLE IF NOT EXISTS `grupo_entrenador` (
    `grupo_entrenamiento_id` INT NOT NULL,
    `entrenador_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`grupo_entrenamiento_id`, `entrenador_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_grupo_entrenador_club`
ON `grupo_entrenador` (`club_id`);

CREATE TABLE IF NOT EXISTS `pagos` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `factura_id` INT NOT NULL,
    `tipo_pago_id` INT NOT NULL,
    `metodo_pago_id` INT NOT NULL,
    `monto` DECIMAL(10, 2) NOT NULL,
    `fecha` DATE NOT NULL,
    `estado` VARCHAR(50) NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_pagos_club`
ON `pagos` (`club_id`);

CREATE INDEX `idx_pagos_factura_id`
ON `pagos` (`factura_id`);

CREATE INDEX `idx_pagos_tipo_pago_id`
ON `pagos` (`tipo_pago_id`);

CREATE INDEX `idx_pagos_metodo_pago_id`
ON `pagos` (`metodo_pago_id`);

CREATE TABLE IF NOT EXISTS `metodos_pago` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `metodo` VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `drivers_sub_criterio` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `sub_criterio_id` INT NOT NULL,
    `nombre` VARCHAR(255) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_drivers_sub_criterio_sub_criterio_id`
ON `drivers_sub_criterio` (`sub_criterio_id`);

CREATE TABLE IF NOT EXISTS `log_acceso` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `accion` VARCHAR(30) NOT NULL,
    `detalle` VARCHAR(255),
    `email_intento` VARCHAR(255) NOT NULL,
    `fecha` DATETIME NOT NULL,
    `ip_origen` VARCHAR(45),
    `usuario_id` INT,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_log_acceso_club`
ON `log_acceso` (`club_id`);

CREATE INDEX `idx_log_acceso_usuario_id`
ON `log_acceso` (`usuario_id`);

CREATE TABLE IF NOT EXISTS `contactos_emergencia` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `deportista_id` INT NOT NULL,
    `nombre` VARCHAR(255),
    `telefono` VARCHAR(255),
    `parentesco` VARCHAR(255),
    `persona_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_contactos_emergencia_club`
ON `contactos_emergencia` (`club_id`);

CREATE INDEX `idx_contactos_emergencia_deportista_id`
ON `contactos_emergencia` (`deportista_id`);

CREATE INDEX `idx_contactos_emergencia_persona_id`
ON `contactos_emergencia` (`persona_id`);

CREATE TABLE IF NOT EXISTS `categorias_producto` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL,
    `descripcion` VARCHAR(255),
    `codigo` VARCHAR(3) NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_categorias_producto_club`
ON `categorias_producto` (`club_id`);
CREATE UNIQUE INDEX `uk_categoriaproducto_club_nombre`
ON `categorias_producto` (`club_id`, `nombre`);
CREATE UNIQUE INDEX `uk_categoriaproducto_club_codigo`
ON `categorias_producto` (`club_id`, `codigo`);

CREATE TABLE IF NOT EXISTS `evaluaciones` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `deportista_id` INT NOT NULL,
    `fecha` DATE NOT NULL,
    `criterio_id` INT NOT NULL,
    `puntaje_obtenido` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_evaluaciones_club`
ON `evaluaciones` (`club_id`);

CREATE INDEX `idx_evaluaciones_deportista_id`
ON `evaluaciones` (`deportista_id`);

CREATE INDEX `idx_evaluaciones_criterio_id`
ON `evaluaciones` (`criterio_id`);

CREATE TABLE IF NOT EXISTS `notificaciones` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `usuario_id` INT NOT NULL,
    `mensaje` TEXT NOT NULL,
    `fecha` DATETIME NOT NULL,
    `vista` tinyint NOT NULL DEFAULT 0,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_notificaciones_club`
ON `notificaciones` (`club_id`);

CREATE INDEX `idx_notificaciones_usuario_id`
ON `notificaciones` (`usuario_id`);

CREATE TABLE IF NOT EXISTS `productos` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL,
    `descripcion` VARCHAR(255),
    `precio` DECIMAL(10, 2) NOT NULL,
    `cantidad_stock` INT NOT NULL,
    `categoria_id` INT NOT NULL,
    `activo` BIT NOT NULL,
    `notas_proveedor` VARCHAR(500),
    `origen` VARCHAR(255),
    `precio_max_referencia` DECIMAL(10, 2),
    `precio_min_referencia` DECIMAL(10, 2),
    `requiere_control_stock` BIT NOT NULL,
    `sku` VARCHAR(20) NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_productos_club`
ON `productos` (`club_id`);
CREATE UNIQUE INDEX `uk_producto_club_sku`
ON `productos` (`club_id`, `sku`);

CREATE INDEX `idx_productos_categoria_id`
ON `productos` (`categoria_id`);

CREATE TABLE IF NOT EXISTS `producto_variantes` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `activo` BIT NOT NULL,
    `color` VARCHAR(30),
    `precio_venta` DECIMAL(10, 2) NOT NULL,
    `sku_variante` VARCHAR(40),
    `stock_actual` INT,
    `talla` VARCHAR(20),
    `producto_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_producto_variantes_club`
ON `producto_variantes` (`club_id`);
CREATE UNIQUE INDEX `uk_productovariante_club_sku_variante`
ON `producto_variantes` (`club_id`, `sku_variante`);

CREATE INDEX `idx_producto_variantes_producto_id`
ON `producto_variantes` (`producto_id`);

CREATE TABLE IF NOT EXISTS `registro_deduccion_driver` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nivel_deduccion` INT NOT NULL,
    `nombre_driver` VARCHAR(50) NOT NULL,
    `registro_sub_criterio_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_registro_deduccion_driver_club`
ON `registro_deduccion_driver` (`club_id`);

CREATE INDEX `idx_registro_deduccion_driver_registro_sub_criterio_id`
ON `registro_deduccion_driver` (`registro_sub_criterio_id`);

CREATE TABLE IF NOT EXISTS `historial_planes_deportista` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `fecha_fin` DATE,
    `fecha_inicio` DATE NOT NULL,
    `motivo_cambio` VARCHAR(255),
    `plan_pago_id` INT NOT NULL,
    `deportista_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_historial_planes_deportista_club`
ON `historial_planes_deportista` (`club_id`);

CREATE INDEX `idx_historial_planes_deportista_deportista_id`
ON `historial_planes_deportista` (`deportista_id`);

CREATE TABLE IF NOT EXISTS `tipos_grupo` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `descuentos` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL,
    `descripcion` VARCHAR(255),
    `porcentaje` DECIMAL(38, 2),
    `fecha_inicio` DATE NOT NULL,
    `fecha_fin` DATE,
    `activo` tinyint NOT NULL DEFAULT 1,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_descuentos_club`
ON `descuentos` (`club_id`);

CREATE TABLE IF NOT EXISTS `grupos_evento` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `grupo_entrenamiento_id` INT NOT NULL,
    `evento_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_grupos_evento_club`
ON `grupos_evento` (`club_id`);

CREATE UNIQUE INDEX `idx_grupos_evento_grupo_entrenamiento_id_evento_id_unique`
ON `grupos_evento` (`grupo_entrenamiento_id`, `evento_id`);

CREATE INDEX `idx_grupos_evento_grupo_entrenamiento_id`
ON `grupos_evento` (`grupo_entrenamiento_id`);

CREATE INDEX `idx_grupos_evento_evento_id`
ON `grupos_evento` (`evento_id`);

CREATE TABLE IF NOT EXISTS `historial_medico` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `persona_id` INT NOT NULL,
    `descripcion` VARCHAR(255) NOT NULL,
    `fecha_registro` DATE NOT NULL,
    `tipo_registro` VARCHAR(255),
    `gravedad` VARCHAR(255),
    `medico_tratante` VARCHAR(255),
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_historial_medico_club`
ON `historial_medico` (`club_id`);

CREATE INDEX `idx_historial_medico_persona_id`
ON `historial_medico` (`persona_id`);

CREATE TABLE IF NOT EXISTS `usuarios` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `username` VARCHAR(255) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `email` VARCHAR(255) NOT NULL UNIQUE,
    `persona_id` INT,
    `ultimo_acceso` DATETIME,
    `activo` tinyint NOT NULL DEFAULT 1,
    `bloqueado` tinyint NOT NULL DEFAULT 0,
    `bloqueado_hasta` DATETIME NULL,
    `intentos_fallidos` INT NOT NULL DEFAULT 0,
    `token_reset_password` VARCHAR(255),
    `token_reset_password_expiracion` DATETIME,
    `requiere_cambio_password` BIT NOT NULL DEFAULT b'0',
    `rol_id` INT,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_usuarios_club`
ON `usuarios` (`club_id`);

CREATE INDEX `idx_usuarios_persona_id`
ON `usuarios` (`persona_id`);

CREATE INDEX `idx_usuarios_token_reset_password`
ON `usuarios` (`token_reset_password`);

CREATE INDEX `idx_usuarios_bloqueado`
ON `usuarios` (`bloqueado`);

CREATE INDEX `idx_usuarios_intentos_fallidos`
ON `usuarios` (`intentos_fallidos`);

CREATE INDEX `idx_usuarios_rol_id`
ON `usuarios` (`rol_id`);

CREATE TABLE IF NOT EXISTS `escalon_driver_nivel` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `driver_id` INT NOT NULL,
    `nivel_id` INT NOT NULL,
    `orden` INT NOT NULL,
    `descripcion` VARCHAR(300) NOT NULL,
    `valor` DECIMAL(4, 2) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_escalon_driver_nivel_driver_id`
ON `escalon_driver_nivel` (`driver_id`);

CREATE INDEX `idx_escalon_driver_nivel_nivel_id`
ON `escalon_driver_nivel` (`nivel_id`);

CREATE TABLE IF NOT EXISTS `deportistas` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `persona_id` INT NOT NULL UNIQUE,
    `estado_id` INT NOT NULL,
    `nivel_actual_id` INT,
    `fecha_inscripcion` DATE,
    `convenio_id` INT,
    `foto_perfil` VARCHAR(255),
    `altura` INT,
    `contacto_emergencia` VARCHAR(255),
    `peso` INT,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_deportistas_club`
ON `deportistas` (`club_id`);
CREATE UNIQUE INDEX `uk_deportistas_club_id`
ON `deportistas` (`club_id`, `id`);  -- Fase 3: destino de FK compuesta (club_id, id)

CREATE INDEX `idx_deportistas_estado_id`
ON `deportistas` (`estado_id`);

CREATE INDEX `idx_deportistas_nivel_actual_id`
ON `deportistas` (`nivel_actual_id`);

CREATE INDEX `idx_deportistas_convenio_id`
ON `deportistas` (`convenio_id`);

CREATE INDEX `idx_deportistas_foto_perfil`
ON `deportistas` (`foto_perfil`);

CREATE TABLE IF NOT EXISTS `entrenadores` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `persona_id` INT NOT NULL UNIQUE,
    `fecha_contratacion` DATE NOT NULL,
    `estado_id` INT NOT NULL,
    `especializacion` VARCHAR(255),
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_entrenadores_club`
ON `entrenadores` (`club_id`);

CREATE INDEX `idx_entrenadores_estado_id`
ON `entrenadores` (`estado_id`);

CREATE TABLE IF NOT EXISTS `estados_persona` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `estado` VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `asistencias` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `deportista_id` INT NOT NULL,
    `horario_entrenamiento_id` INT NOT NULL,
    `fecha` DATE NOT NULL,
    `estado_asistencia_id` INT NOT NULL,
    `horario_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_asistencias_club`
ON `asistencias` (`club_id`);
-- Fase 2 (opcional, aplicado): evita asistencias duplicadas por doble clic/reintento (la app responde 409).
CREATE UNIQUE INDEX `uk_asistencia_dia`
ON `asistencias` (`club_id`, `deportista_id`, `horario_id`, `fecha`);

CREATE INDEX `idx_asistencias_estado_asistencia_id`
ON `asistencias` (`estado_asistencia_id`);

CREATE INDEX `idx_asistencias_deportista_id`
ON `asistencias` (`deportista_id`);

CREATE INDEX `idx_asistencias_fecha`
ON `asistencias` (`fecha`);

CREATE INDEX `idx_asistencias_horario_entrenamiento_id`
ON `asistencias` (`horario_entrenamiento_id`);

CREATE INDEX `idx_asistencias_horario_id`
ON `asistencias` (`horario_id`);

CREATE TABLE IF NOT EXISTS `ventas` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `contacto_comprador_externo` VARCHAR(150),
    `fecha` DATE NOT NULL,
    `metodo_pago` VARCHAR(30),
    `nombre_comprador_externo` VARCHAR(150),
    `total` DECIMAL(10, 2) NOT NULL,
    `factura_id` INT,
    `persona_id` INT,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_ventas_club`
ON `ventas` (`club_id`);

CREATE INDEX `idx_ventas_factura_id`
ON `ventas` (`factura_id`);

CREATE INDEX `idx_ventas_persona_id`
ON `ventas` (`persona_id`);

CREATE TABLE IF NOT EXISTS `horarios_entrenamiento` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `horario_id` INT NOT NULL,
    `grupo_entrenamiento_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_horarios_entrenamiento_club`
ON `horarios_entrenamiento` (`club_id`);

CREATE INDEX `idx_horarios_entrenamiento_horario_id`
ON `horarios_entrenamiento` (`horario_id`);

CREATE INDEX `idx_horarios_entrenamiento_grupo_entrenamiento_id`
ON `horarios_entrenamiento` (`grupo_entrenamiento_id`);

CREATE TABLE IF NOT EXISTS `auditoria` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `usuario_id` INT,
    `tabla_afectada` VARCHAR(50) NOT NULL,
    `accion` VARCHAR(50) NOT NULL,
    `registro_id` INT,
    `valores_anteriores` TEXT,
    `nuevos_valores` TEXT,
    `fecha` DATETIME NOT NULL,
    `ip_usuario` VARCHAR(45),
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_auditoria_club`
ON `auditoria` (`club_id`);

CREATE INDEX `idx_auditoria_usuario_id`
ON `auditoria` (`usuario_id`);

CREATE TABLE IF NOT EXISTS `estados_objetivo` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre_estado` VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `grupo_deportista` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `grupo_entrenamiento_id` INT NOT NULL,
    `deportista_id` INT NOT NULL,
    `fecha_fin` DATE,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_grupo_deportista_club`
ON `grupo_deportista` (`club_id`);

CREATE UNIQUE INDEX `idx_grupo_deportista_grupo_entrenamiento_id_deportista_id_unique`
ON `grupo_deportista` (`grupo_entrenamiento_id`, `deportista_id`);

CREATE INDEX `idx_grupo_deportista_grupo_entrenamiento_id`
ON `grupo_deportista` (`grupo_entrenamiento_id`);

CREATE INDEX `idx_grupo_deportista_deportista_id`
ON `grupo_deportista` (`deportista_id`);

CREATE TABLE IF NOT EXISTS `usuario_roles` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `usuario_id` INT NOT NULL,
    `rol_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX `idx_usuario_roles_usuario_id_rol_id_unique`
ON `usuario_roles` (`usuario_id`, `rol_id`);

CREATE INDEX `idx_usuario_roles_usuario_id`
ON `usuario_roles` (`usuario_id`);

CREATE INDEX `idx_usuario_roles_rol_id`
ON `usuario_roles` (`rol_id`);

CREATE TABLE IF NOT EXISTS `roles` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL,
    `descripcion` VARCHAR(255),
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_roles_club`
ON `roles` (`club_id`);
CREATE UNIQUE INDEX `uk_rol_club_nombre`
ON `roles` (`club_id`, `nombre`);

CREATE TABLE IF NOT EXISTS `niveles` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_niveles_club`
ON `niveles` (`club_id`);
CREATE UNIQUE INDEX `uk_niveles_club_id`
ON `niveles` (`club_id`, `id`);  -- Fase 2: destino de FK compuesta (club_id, id)
CREATE UNIQUE INDEX `uk_nivel_club_nombre`
ON `niveles` (`club_id`, `nombre`);

CREATE TABLE IF NOT EXISTS `detalles_venta` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `factura_id` INT NOT NULL,
    `producto_id` INT NOT NULL,
    `cantidad` INT NOT NULL,
    `precio_unitario` DECIMAL(10, 2) NOT NULL,
    `subtotal` DECIMAL(10, 2) NOT NULL,
    `producto_variante_id` INT NOT NULL,
    `venta_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_detalles_venta_club`
ON `detalles_venta` (`club_id`);

CREATE INDEX `idx_detalles_venta_factura_id`
ON `detalles_venta` (`factura_id`);

CREATE INDEX `idx_detalles_venta_producto_id`
ON `detalles_venta` (`producto_id`);

CREATE INDEX `idx_detalles_venta_producto_variante_id`
ON `detalles_venta` (`producto_variante_id`);

CREATE INDEX `idx_detalles_venta_venta_id`
ON `detalles_venta` (`venta_id`);

CREATE TABLE IF NOT EXISTS `rol_permisos` (
    `rol_id` INT NOT NULL,
    `permiso_id` INT NOT NULL,
    PRIMARY KEY (`rol_id`, `permiso_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_rol_permisos_permiso_id`
ON `rol_permisos` (`permiso_id`);

CREATE TABLE IF NOT EXISTS `estados_asistencia` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `estado` VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `personas` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL,
    `tipo_documento` VARCHAR(255),
    `numero_documento` VARCHAR(255),
    `direccion` VARCHAR(255),
    `telefono` VARCHAR(255),
    `fecha_nacimiento` DATE,
    `genero_id` INT,
    `apellidos` VARCHAR(255) NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_personas_club`
ON `personas` (`club_id`);
CREATE UNIQUE INDEX `uk_personas_club_id`
ON `personas` (`club_id`, `id`);  -- Fase 3: destino de FK compuesta (club_id, id)
CREATE UNIQUE INDEX `uk_persona_club_numero_documento`
ON `personas` (`club_id`, `numero_documento`);

CREATE INDEX `idx_personas_genero_id`
ON `personas` (`genero_id`);

CREATE INDEX `idx_personas_tipo_documento`
ON `personas` (`tipo_documento`);

CREATE INDEX `idx_personas_nombre`
ON `personas` (`nombre`);

CREATE TABLE IF NOT EXISTS `tipos_categoria` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `categoria` VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `divisiones` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nombre` VARCHAR(255) NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_divisiones_club`
ON `divisiones` (`club_id`);
CREATE UNIQUE INDEX `uk_division_club_nombre`
ON `divisiones` (`club_id`, `nombre`);

CREATE TABLE IF NOT EXISTS `historial_nivel_deportista` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `fecha_cambio` DATE NOT NULL,
    `motivo` VARCHAR(255),
    `nivel_id` INT NOT NULL,
    `deportista_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_historial_nivel_deportista_club`
ON `historial_nivel_deportista` (`club_id`);

CREATE INDEX `idx_historial_nivel_deportista_deportista_id`
ON `historial_nivel_deportista` (`deportista_id`);

CREATE TABLE IF NOT EXISTS `dias_semana` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `dia` VARCHAR(255) NOT NULL UNIQUE,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `horarios` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `dia_id` INT NOT NULL,
    `hora_inicio` TIME,
    `hora_fin` TIME,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_horarios_club`
ON `horarios` (`club_id`);

CREATE INDEX `idx_horarios_dia_id`
ON `horarios` (`dia_id`);

CREATE TABLE IF NOT EXISTS `tabla_cantidad_nivel` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nivel_id` INT NOT NULL,
    `tabla` VARCHAR(255) NOT NULL,
    `unidad` VARCHAR(255) NOT NULL,
    `rango_min_atletas` INT NOT NULL,
    `rango_max_atletas` INT NOT NULL,
    `mayoria` INT NOT NULL,
    `gran_parte` INT NOT NULL,
    `maximo` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_tabla_cantidad_nivel_nivel_id`
ON `tabla_cantidad_nivel` (`nivel_id`);

CREATE TABLE IF NOT EXISTS `evaluaciones_rutina` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `cantidad_atletas` INT NOT NULL,
    `evento` VARCHAR(200),
    `fecha` DATE NOT NULL,
    `observaciones` VARCHAR(1000),
    `puntaje_total` DECIMAL(6, 2),
    `evento_oficial_id` INT,
    `grupo_entrenamiento_id` INT NOT NULL,
    `nivel_id` INT NOT NULL,
    `club_id` INT NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE INDEX `idx_evaluaciones_rutina_club`
ON `evaluaciones_rutina` (`club_id`);

CREATE INDEX `idx_evaluaciones_rutina_evento_oficial_id`
ON `evaluaciones_rutina` (`evento_oficial_id`);

CREATE INDEX `idx_evaluaciones_rutina_grupo_entrenamiento_id`
ON `evaluaciones_rutina` (`grupo_entrenamiento_id`);

CREATE INDEX `idx_evaluaciones_rutina_nivel_id`
ON `evaluaciones_rutina` (`nivel_id`);

CREATE TABLE IF NOT EXISTS `tope_seccion_nivel` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `nivel_id` INT NOT NULL,
    `sub_criterio_id` INT NOT NULL,
    `orden` INT NOT NULL,
    `valor_puntaje` DECIMAL(4, 2) NOT NULL,
    `condicion_habilidades_diferentes` INT,
    `condicion_grupo_minimo` VARCHAR(20),
    `descripcion_rubrica` VARCHAR(500),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_tope_seccion_nivel_nivel_id`
ON `tope_seccion_nivel` (`nivel_id`);

CREATE INDEX `idx_tope_seccion_nivel_sub_criterio_id`
ON `tope_seccion_nivel` (`sub_criterio_id`);

CREATE TABLE IF NOT EXISTS `tope_driver_nivel` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `driver_id` INT NOT NULL,
    `nivel_id` INT NOT NULL,
    `tope_maximo` DECIMAL(4, 2) NOT NULL,
    `valor_nivel_basico` DECIMAL(4, 2),
    `valor_nivel_alto` DECIMAL(4, 2),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_tope_driver_nivel_driver_id`
ON `tope_driver_nivel` (`driver_id`);

CREATE INDEX `idx_tope_driver_nivel_nivel_id`
ON `tope_driver_nivel` (`nivel_id`);

-- Foreign key constraints

ALTER TABLE `asistencias_evento`
ADD CONSTRAINT `fk_asistencias_evento_deportista_id` FOREIGN KEY(`deportista_id`) REFERENCES `deportistas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `asistencias_evento`
ADD CONSTRAINT `fk_asistencias_evento_estado_id` FOREIGN KEY(`estado_id`) REFERENCES `estados_asistencia`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `asistencias_evento`
ADD CONSTRAINT `fk_asistencias_evento_evento_id` FOREIGN KEY(`evento_id`) REFERENCES `eventos`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `asistencias`
ADD CONSTRAINT `fk_asistencias_deportista_id` FOREIGN KEY(`deportista_id`) REFERENCES `deportistas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `asistencias`
ADD CONSTRAINT `fk_asistencias_estado_asistencia_id` FOREIGN KEY(`estado_asistencia_id`) REFERENCES `estados_asistencia`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `asistencias`
ADD CONSTRAINT `fk_asistencias_horario_entrenamiento_id` FOREIGN KEY(`horario_entrenamiento_id`) REFERENCES `horarios_entrenamiento`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `auditoria`
ADD CONSTRAINT `fk_auditoria_usuario_id` FOREIGN KEY(`usuario_id`) REFERENCES `usuarios`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `categorias_nivel`
ADD CONSTRAINT `fk_categorias_nivel_division_id` FOREIGN KEY(`division_id`) REFERENCES `divisiones`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `categorias_nivel`
ADD CONSTRAINT `fk_categorias_nivel_estado_categorias_nivel_id` FOREIGN KEY(`estado_categorias_nivel_id`) REFERENCES `estados_categorias_nivel`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `categorias_nivel`
ADD CONSTRAINT `fk_categorias_nivel_nivel_id` FOREIGN KEY(`nivel_id`) REFERENCES `niveles`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `contactos_emergencia`
ADD CONSTRAINT `fk_contactos_emergencia_deportista_id` FOREIGN KEY(`deportista_id`) REFERENCES `deportistas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `convenios`
ADD CONSTRAINT `fk_convenios_descuento_id` FOREIGN KEY(`descuento_id`) REFERENCES `descuentos`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `criterios_evaluacion`
ADD CONSTRAINT `fk_criterios_evaluacion_categoria_id` FOREIGN KEY(`categoria_id`) REFERENCES `tipos_categoria`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `deportistas`
ADD CONSTRAINT `fk_deportistas_convenio_id` FOREIGN KEY(`convenio_id`) REFERENCES `convenios`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `deportistas`
ADD CONSTRAINT `fk_deportistas_estado_id` FOREIGN KEY(`estado_id`) REFERENCES `estados_persona`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `deportistas`
ADD CONSTRAINT `fk_deportistas_nivel_actual_id` FOREIGN KEY(`nivel_actual_id`) REFERENCES `niveles`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `deportistas`
ADD CONSTRAINT `fk_deportistas_persona_id` FOREIGN KEY(`persona_id`) REFERENCES `personas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `detalles_venta`
ADD CONSTRAINT `fk_detalles_venta_factura_id` FOREIGN KEY(`factura_id`) REFERENCES `facturas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `detalles_venta`
ADD CONSTRAINT `fk_detalles_venta_producto_id` FOREIGN KEY(`producto_id`) REFERENCES `productos`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `drivers_sub_criterio`
ADD CONSTRAINT `fk_drivers_sub_criterio_sub_criterio_id` FOREIGN KEY(`sub_criterio_id`) REFERENCES `sub_criterios_rubrica`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `escalon_driver_nivel`
ADD CONSTRAINT `fk_escalon_driver_nivel_driver_id` FOREIGN KEY(`driver_id`) REFERENCES `drivers_sub_criterio`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `escalon_driver_nivel`
ADD CONSTRAINT `fk_escalon_driver_nivel_nivel_id` FOREIGN KEY(`nivel_id`) REFERENCES `niveles_competencia`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `entrenadores`
ADD CONSTRAINT `fk_entrenadores_estado_id` FOREIGN KEY(`estado_id`) REFERENCES `estados_persona`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `entrenadores`
ADD CONSTRAINT `fk_entrenadores_persona_id` FOREIGN KEY(`persona_id`) REFERENCES `personas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `evaluaciones`
ADD CONSTRAINT `fk_evaluaciones_criterio_id` FOREIGN KEY(`criterio_id`) REFERENCES `criterios_evaluacion`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `evaluaciones`
ADD CONSTRAINT `fk_evaluaciones_deportista_id` FOREIGN KEY(`deportista_id`) REFERENCES `deportistas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `eventos`
ADD CONSTRAINT `fk_eventos_tipo_evento_id` FOREIGN KEY(`tipo_evento_id`) REFERENCES `tipos_evento`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `facturas`
ADD CONSTRAINT `fk_facturas_descuento_id` FOREIGN KEY(`descuento_id`) REFERENCES `descuentos`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `facturas`
ADD CONSTRAINT `fk_facturas_inscripcion_id` FOREIGN KEY(`inscripcion_id`) REFERENCES `inscripciones`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `facturas`
ADD CONSTRAINT `fk_facturas_persona_id` FOREIGN KEY(`persona_id`) REFERENCES `personas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `grupo_deportista`
ADD CONSTRAINT `fk_grupo_deportista_deportista_id` FOREIGN KEY(`deportista_id`) REFERENCES `deportistas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `grupo_deportista`
ADD CONSTRAINT `fk_grupo_deportista_grupo_entrenamiento_id` FOREIGN KEY(`grupo_entrenamiento_id`) REFERENCES `grupos_entrenamiento`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `grupos_entrenamiento`
ADD CONSTRAINT `fk_grupos_entrenamiento_categorias_nivel_id` FOREIGN KEY(`categorias_nivel_id`) REFERENCES `categorias_nivel`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `grupos_entrenamiento`
ADD CONSTRAINT `fk_grupos_entrenamiento_tipo_grupo_id` FOREIGN KEY(`tipo_grupo_id`) REFERENCES `tipos_grupo`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `grupos_evento`
ADD CONSTRAINT `fk_grupos_evento_evento_id` FOREIGN KEY(`evento_id`) REFERENCES `eventos`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `grupos_evento`
ADD CONSTRAINT `fk_grupos_evento_grupo_entrenamiento_id` FOREIGN KEY(`grupo_entrenamiento_id`) REFERENCES `grupos_entrenamiento`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `historial_estado_deportista`
ADD CONSTRAINT `fk_historial_estado_deportista_deportista_id` FOREIGN KEY(`deportista_id`) REFERENCES `deportistas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `historial_estado_deportista`
ADD CONSTRAINT `fk_historial_estado_deportista_estado_anterior_id` FOREIGN KEY(`estado_anterior_id`) REFERENCES `estados_persona`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `historial_estado_deportista`
ADD CONSTRAINT `fk_historial_estado_deportista_estado_nuevo_id` FOREIGN KEY(`estado_nuevo_id`) REFERENCES `estados_persona`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `historial_estado_deportista`
ADD CONSTRAINT `fk_historial_estado_deportista_usuario_id` FOREIGN KEY(`usuario_id`) REFERENCES `usuarios`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `historial_medico`
ADD CONSTRAINT `fk_historial_medico_persona_id` FOREIGN KEY(`persona_id`) REFERENCES `personas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `horarios`
ADD CONSTRAINT `fk_horarios_dia_id` FOREIGN KEY(`dia_id`) REFERENCES `dias_semana`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `horarios_entrenamiento`
ADD CONSTRAINT `fk_horarios_entrenamiento_grupo_entrenamiento_id` FOREIGN KEY(`grupo_entrenamiento_id`) REFERENCES `grupos_entrenamiento`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `horarios_entrenamiento`
ADD CONSTRAINT `fk_horarios_entrenamiento_horario_id` FOREIGN KEY(`horario_id`) REFERENCES `horarios`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `inscripciones`
ADD CONSTRAINT `fk_inscripciones_deportista_id` FOREIGN KEY(`deportista_id`) REFERENCES `deportistas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `inscripciones`
ADD CONSTRAINT `fk_inscripciones_plan_pago_id` FOREIGN KEY(`plan_pago_id`) REFERENCES `planes_pago`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `movimientos_inventario`
ADD CONSTRAINT `fk_movimientos_inventario_producto_id` FOREIGN KEY(`producto_id`) REFERENCES `productos`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `movimientos_inventario`
ADD CONSTRAINT `fk_movimientos_inventario_usuario_id` FOREIGN KEY(`usuario_id`) REFERENCES `usuarios`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `notificaciones`
ADD CONSTRAINT `fk_notificaciones_usuario_id` FOREIGN KEY(`usuario_id`) REFERENCES `usuarios`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `objetivos`
ADD CONSTRAINT `fk_objetivos_deportista_id` FOREIGN KEY(`deportista_id`) REFERENCES `deportistas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `objetivos`
ADD CONSTRAINT `fk_objetivos_estado_objetivo_id` FOREIGN KEY(`estado_objetivo_id`) REFERENCES `estados_objetivo`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `pagos`
ADD CONSTRAINT `fk_pagos_factura_id` FOREIGN KEY(`factura_id`) REFERENCES `facturas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `pagos`
ADD CONSTRAINT `fk_pagos_metodo_pago_id` FOREIGN KEY(`metodo_pago_id`) REFERENCES `metodos_pago`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `pagos`
ADD CONSTRAINT `fk_pagos_tipo_pago_id` FOREIGN KEY(`tipo_pago_id`) REFERENCES `tipos_pago`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `personas`
ADD CONSTRAINT `fk_personas_genero_id` FOREIGN KEY(`genero_id`) REFERENCES `generos`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `premios_evento`
ADD CONSTRAINT `fk_premios_evento_evento_id` FOREIGN KEY(`evento_id`) REFERENCES `eventos`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `productos`
ADD CONSTRAINT `fk_productos_categoria_id` FOREIGN KEY(`categoria_id`) REFERENCES `categorias_producto`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `resultados_competencia`
ADD CONSTRAINT `fk_resultados_competencia_evento_id` FOREIGN KEY(`evento_id`) REFERENCES `eventos`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `resultados_competencia`
ADD CONSTRAINT `fk_resultados_competencia_grupo_id` FOREIGN KEY(`grupo_id`) REFERENCES `grupos_entrenamiento`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `resultados_competencia`
ADD CONSTRAINT `fk_resultados_competencia_premio_id` FOREIGN KEY(`premio_id`) REFERENCES `premios_evento`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `rol_permisos`
ADD CONSTRAINT `fk_rol_permisos_permiso_id` FOREIGN KEY(`permiso_id`) REFERENCES `permisos`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `rol_permisos`
ADD CONSTRAINT `fk_rol_permisos_rol_id` FOREIGN KEY(`rol_id`) REFERENCES `roles`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `tabla_cantidad_nivel`
ADD CONSTRAINT `fk_tabla_cantidad_nivel_nivel_id` FOREIGN KEY(`nivel_id`) REFERENCES `niveles_competencia`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `tope_driver_nivel`
ADD CONSTRAINT `fk_tope_driver_nivel_driver_id` FOREIGN KEY(`driver_id`) REFERENCES `drivers_sub_criterio`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `tope_driver_nivel`
ADD CONSTRAINT `fk_tope_driver_nivel_nivel_id` FOREIGN KEY(`nivel_id`) REFERENCES `niveles_competencia`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `tope_seccion_nivel`
ADD CONSTRAINT `fk_tope_seccion_nivel_nivel_id` FOREIGN KEY(`nivel_id`) REFERENCES `niveles_competencia`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `tope_seccion_nivel`
ADD CONSTRAINT `fk_tope_seccion_nivel_sub_criterio_id` FOREIGN KEY(`sub_criterio_id`) REFERENCES `sub_criterios_rubrica`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `usuario_roles`
ADD CONSTRAINT `fk_usuario_roles_rol_id` FOREIGN KEY(`rol_id`) REFERENCES `roles`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `usuario_roles`
ADD CONSTRAINT `fk_usuario_roles_usuario_id` FOREIGN KEY(`usuario_id`) REFERENCES `usuarios`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `usuarios`
ADD CONSTRAINT `fk_usuarios_persona_id` FOREIGN KEY(`persona_id`) REFERENCES `personas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `planes_pago`
ADD CONSTRAINT `fk_planes_pago_tipo_plan_id` FOREIGN KEY(`tipo_plan_id`) REFERENCES `tipos_planes_pago`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `detalles_venta`
ADD CONSTRAINT `fk_detalles_venta_venta_id` FOREIGN KEY(`venta_id`) REFERENCES `ventas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `historial_planes_deportista`
ADD CONSTRAINT `fk_historial_planes_deportista_deportista_id` FOREIGN KEY(`deportista_id`) REFERENCES `deportistas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `registro_sub_criterio`
ADD CONSTRAINT `fk_registro_sub_criterio_sub_criterio_id` FOREIGN KEY(`sub_criterio_id`) REFERENCES `sub_criterios_rubrica`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `registro_driver_sub_criterio`
ADD CONSTRAINT `fk_registro_driver_sub_criterio_escalon_seleccionado_id` FOREIGN KEY(`escalon_seleccionado_id`) REFERENCES `escalon_driver_nivel`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `historial_nivel_deportista`
ADD CONSTRAINT `fk_historial_nivel_deportista_deportista_id` FOREIGN KEY(`deportista_id`) REFERENCES `deportistas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `producto_variantes`
ADD CONSTRAINT `fk_producto_variantes_producto_id` FOREIGN KEY(`producto_id`) REFERENCES `productos`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `registro_sub_criterio`
ADD CONSTRAINT `fk_registro_sub_criterio_evaluacion_rutina_id` FOREIGN KEY(`evaluacion_rutina_id`) REFERENCES `evaluaciones_rutina`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `movimientos_inventario`
ADD CONSTRAINT `fk_movimientos_inventario_producto_variante_id` FOREIGN KEY(`producto_variante_id`) REFERENCES `producto_variantes`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `registro_deduccion_driver`
ADD CONSTRAINT `fk_registro_deduccion_driver_registro_sub_criterio_id` FOREIGN KEY(`registro_sub_criterio_id`) REFERENCES `registro_sub_criterio`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `asistencias`
ADD CONSTRAINT `fk_asistencias_horario_id` FOREIGN KEY(`horario_id`) REFERENCES `horarios`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `evaluaciones_rutina`
ADD CONSTRAINT `fk_evaluaciones_rutina_evento_oficial_id` FOREIGN KEY(`evento_oficial_id`) REFERENCES `eventos`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `ventas`
ADD CONSTRAINT `fk_ventas_factura_id` FOREIGN KEY(`factura_id`) REFERENCES `facturas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `log_acceso`
ADD CONSTRAINT `fk_log_acceso_usuario_id` FOREIGN KEY(`usuario_id`) REFERENCES `usuarios`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `resultados_competencia`
ADD CONSTRAINT `fk_resultados_competencia_evaluacion_rutina_id` FOREIGN KEY(`evaluacion_rutina_id`) REFERENCES `evaluaciones_rutina`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `registro_driver_sub_criterio`
ADD CONSTRAINT `fk_registro_driver_sub_criterio_registro_sub_criterio_id` FOREIGN KEY(`registro_sub_criterio_id`) REFERENCES `registro_sub_criterio`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `detalles_venta`
ADD CONSTRAINT `fk_detalles_venta_producto_variante_id` FOREIGN KEY(`producto_variante_id`) REFERENCES `producto_variantes`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `contactos_emergencia`
ADD CONSTRAINT `fk_contactos_emergencia_persona_id` FOREIGN KEY(`persona_id`) REFERENCES `personas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `ventas`
ADD CONSTRAINT `fk_ventas_persona_id` FOREIGN KEY(`persona_id`) REFERENCES `personas`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `evaluaciones_rutina`
ADD CONSTRAINT `fk_evaluaciones_rutina_nivel_id` FOREIGN KEY(`nivel_id`) REFERENCES `niveles_competencia`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `usuarios`
ADD CONSTRAINT `fk_usuarios_rol_id` FOREIGN KEY(`rol_id`) REFERENCES `roles`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `evaluaciones_rutina`
ADD CONSTRAINT `fk_evaluaciones_rutina_grupo_entrenamiento_id` FOREIGN KEY(`grupo_entrenamiento_id`) REFERENCES `grupos_entrenamiento`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

ALTER TABLE `registro_driver_sub_criterio`
ADD CONSTRAINT `fk_registro_driver_sub_criterio_driver_id` FOREIGN KEY(`driver_id`) REFERENCES `drivers_sub_criterio`(`id`)
ON UPDATE CASCADE ON DELETE RESTRICT;

-- Fase 1: llave foranea club_id -> clubes en cada tabla de club
ALTER TABLE `roles`
ADD CONSTRAINT `fk_roles_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `usuarios`
ADD CONSTRAINT `fk_usuarios_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `criterios_evaluacion`
ADD CONSTRAINT `fk_criterios_evaluacion_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `evaluaciones`
ADD CONSTRAINT `fk_evaluaciones_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `objetivos`
ADD CONSTRAINT `fk_objetivos_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `evaluaciones_rutina`
ADD CONSTRAINT `fk_evaluaciones_rutina_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `registro_deduccion_driver`
ADD CONSTRAINT `fk_registro_deduccion_driver_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `registro_driver_sub_criterio`
ADD CONSTRAINT `fk_registro_driver_sub_criterio_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `registro_sub_criterio`
ADD CONSTRAINT `fk_registro_sub_criterio_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `eventos`
ADD CONSTRAINT `fk_eventos_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `grupos_evento`
ADD CONSTRAINT `fk_grupos_evento_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `premios_evento`
ADD CONSTRAINT `fk_premios_evento_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `resultados_competencia`
ADD CONSTRAINT `fk_resultados_competencia_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `convenios`
ADD CONSTRAINT `fk_convenios_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `descuentos`
ADD CONSTRAINT `fk_descuentos_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `facturas`
ADD CONSTRAINT `fk_facturas_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `pagos`
ADD CONSTRAINT `fk_pagos_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `planes_pago`
ADD CONSTRAINT `fk_planes_pago_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `auditoria`
ADD CONSTRAINT `fk_auditoria_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `log_acceso`
ADD CONSTRAINT `fk_log_acceso_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `notificaciones`
ADD CONSTRAINT `fk_notificaciones_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `historial_medico`
ADD CONSTRAINT `fk_historial_medico_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `historial_nivel_deportista`
ADD CONSTRAINT `fk_historial_nivel_deportista_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `historial_planes_deportista`
ADD CONSTRAINT `fk_historial_planes_deportista_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `asistencias`
ADD CONSTRAINT `fk_asistencias_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `horarios`
ADD CONSTRAINT `fk_horarios_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `horarios_entrenamiento`
ADD CONSTRAINT `fk_horarios_entrenamiento_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `categorias_producto`
ADD CONSTRAINT `fk_categorias_producto_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `detalles_venta`
ADD CONSTRAINT `fk_detalles_venta_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `movimientos_inventario`
ADD CONSTRAINT `fk_movimientos_inventario_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `productos`
ADD CONSTRAINT `fk_productos_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `producto_variantes`
ADD CONSTRAINT `fk_producto_variantes_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `ventas`
ADD CONSTRAINT `fk_ventas_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `categorias_nivel`
ADD CONSTRAINT `fk_categorias_nivel_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `grupo_deportista`
ADD CONSTRAINT `fk_grupo_deportista_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `divisiones`
ADD CONSTRAINT `fk_divisiones_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `grupo_entrenador`
ADD CONSTRAINT `fk_grupo_entrenador_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `grupos_entrenamiento`
ADD CONSTRAINT `fk_grupos_entrenamiento_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `inscripciones`
ADD CONSTRAINT `fk_inscripciones_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `niveles`
ADD CONSTRAINT `fk_niveles_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `contactos_emergencia`
ADD CONSTRAINT `fk_contactos_emergencia_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `deportistas`
ADD CONSTRAINT `fk_deportistas_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `entrenadores`
ADD CONSTRAINT `fk_entrenadores_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `historial_estado_deportista`
ADD CONSTRAINT `fk_historial_estado_deportista_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

ALTER TABLE `personas`
ADD CONSTRAINT `fk_personas_club` FOREIGN KEY (`club_id`) REFERENCES `clubes` (`id`);

-- =============================================================================
-- Fase 2: integridad referencial ENTRE CLUBES. Claves foraneas compuestas
-- (club_id, id): fila hija y fila padre deben ser del mismo club. Un NULL en la
-- columna hija no se valida (nivel y convenio son opcionales).
-- =============================================================================
ALTER TABLE `deportistas`
  ADD CONSTRAINT `fk_deportistas_nivel_club`
    FOREIGN KEY (`club_id`, `nivel_actual_id`) REFERENCES `niveles` (`club_id`, `id`),
  ADD CONSTRAINT `fk_deportistas_convenio_club`
    FOREIGN KEY (`club_id`, `convenio_id`) REFERENCES `convenios` (`club_id`, `id`);

ALTER TABLE `historial_nivel_deportista`
  ADD CONSTRAINT `fk_hist_nivel_nivel_club`
    FOREIGN KEY (`club_id`, `nivel_id`) REFERENCES `niveles` (`club_id`, `id`);

ALTER TABLE `historial_planes_deportista`
  ADD CONSTRAINT `fk_hist_planes_plan_club`
    FOREIGN KEY (`club_id`, `plan_pago_id`) REFERENCES `planes_pago` (`club_id`, `id`);

-- =============================================================================
-- Fase 3: acudientes, temporadas y consentimientos (Ley 1581 de proteccion de datos).
-- Toda tabla de club lleva club_id y sus relaciones con otras tablas de club son
-- claves foraneas COMPUESTAS (club_id, id), igual que en la Fase 2.
-- (Las tablas replican el modelo JPA; ddl-auto=validate las comprueba al arrancar.)
-- =============================================================================

CREATE TABLE IF NOT EXISTS acudientes (
  id               INT NOT NULL AUTO_INCREMENT,
  club_id          INT NOT NULL,
  nombre           VARCHAR(100) NOT NULL,
  apellidos        VARCHAR(100) NOT NULL,
  tipo_documento   VARCHAR(20),
  numero_documento VARCHAR(30),
  telefono         VARCHAR(30),
  email            VARCHAR(150),
  direccion        VARCHAR(200),
  PRIMARY KEY (id),
  UNIQUE KEY uk_acudiente_club_documento (club_id, numero_documento),
  UNIQUE KEY uk_acudientes_club_id (club_id, id),
  CONSTRAINT fk_acudientes_club FOREIGN KEY (club_id) REFERENCES clubes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS deportista_acudientes (
  id               INT NOT NULL AUTO_INCREMENT,
  club_id          INT NOT NULL,
  deportista_id    INT NOT NULL,
  acudiente_id     INT NOT NULL,
  parentesco       VARCHAR(40),
  principal        BIT(1) NOT NULL DEFAULT b'0',
  autoriza_recoger BIT(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (id),
  UNIQUE KEY uk_dep_acud (deportista_id, acudiente_id),
  CONSTRAINT fk_depacud_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_depacud_deportista FOREIGN KEY (club_id, deportista_id) REFERENCES deportistas (club_id, id),
  CONSTRAINT fk_depacud_acudiente  FOREIGN KEY (club_id, acudiente_id)  REFERENCES acudientes (club_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS temporadas (
  id           INT NOT NULL AUTO_INCREMENT,
  club_id      INT NOT NULL,
  nombre       VARCHAR(60) NOT NULL,
  fecha_inicio DATE NOT NULL,
  fecha_fin    DATE NOT NULL,
  activa       BIT(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (id),
  UNIQUE KEY uk_temporada_club_nombre (club_id, nombre),
  UNIQUE KEY uk_temporadas_club_id (club_id, id),
  CONSTRAINT fk_temporadas_club FOREIGN KEY (club_id) REFERENCES clubes (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS consentimientos (
  id                       INT NOT NULL AUTO_INCREMENT,
  club_id                  INT NOT NULL,
  titular_id               INT NOT NULL,
  acudiente_id             INT NULL,
  tipo                     VARCHAR(30) NOT NULL,
  version_texto            VARCHAR(40) NOT NULL,
  canal                    VARCHAR(30) NOT NULL,
  fecha_otorgado           DATETIME(6) NOT NULL,
  fecha_revocado           DATETIME(6) NULL,
  motivo_revocacion        VARCHAR(300),
  registrado_por_usuario_id INT NULL,
  observaciones            VARCHAR(300),
  PRIMARY KEY (id),
  KEY idx_consent_titular_tipo (club_id, titular_id, tipo),
  CONSTRAINT fk_consent_club FOREIGN KEY (club_id) REFERENCES clubes (id),
  CONSTRAINT fk_consent_titular   FOREIGN KEY (club_id, titular_id)   REFERENCES personas (club_id, id),
  CONSTRAINT fk_consent_acudiente FOREIGN KEY (club_id, acudiente_id) REFERENCES acudientes (club_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `inscripciones`
  ADD CONSTRAINT `fk_inscripciones_temporada`
    FOREIGN KEY (`club_id`, `temporada_id`) REFERENCES `temporadas` (`club_id`, `id`);

-- =============================================================================
-- Vistas (existian en la estructura original). Se les agrego `club_id` como ULTIMA
-- columna para poder filtrar por club (multi-club); el resto es identico.
-- =============================================================================

CREATE OR REPLACE VIEW `vista_deportistas_no_inscritos` AS
SELECT d.id AS deportista_id,
       CONCAT(p.nombre, ' ', p.apellidos) AS nombre_completo,
       n.nombre AS nivel,
       d.club_id AS club_id
FROM deportistas d
JOIN personas p ON p.id = d.persona_id
LEFT JOIN niveles n ON n.id = d.nivel_actual_id
WHERE NOT EXISTS (SELECT 1 FROM inscripciones i
                  WHERE i.deportista_id = d.id AND i.estado = 'Activa');

CREATE OR REPLACE VIEW `vista_inscripciones_detalle` AS
SELECT i.id AS inscripcion_id,
       d.id AS deportista_id,
       CONCAT(p.nombre, ' ', p.apellidos) AS deportista,
       n.nombre AS nivel,
       i.fecha_inscripcion AS fecha_inscripcion,
       i.fecha_vencimiento AS fecha_vencimiento,
       i.estado AS estado,
       pp.nombre AS plan_pago,
       ge.nombre AS grupo_asignado,
       NULL AS documentos_completos,
       i.club_id AS club_id
FROM inscripciones i
JOIN deportistas d ON d.id = i.deportista_id
JOIN personas p ON p.id = d.persona_id
LEFT JOIN niveles n ON n.id = d.nivel_actual_id
JOIN planes_pago pp ON pp.id = i.plan_pago_id
LEFT JOIN grupo_deportista gd ON gd.deportista_id = d.id AND gd.fecha_fin IS NULL
LEFT JOIN grupos_entrenamiento ge ON ge.id = gd.grupo_entrenamiento_id;

-- =============================================================================
-- Fase 0 (esquema): saneamiento de cuentas. Aplica a TODOS los usuarios
-- registrados. En una base nueva no afecta filas (no hay usuarios); sirve al
-- re-ejecutar el script sobre una base con datos. WHERE sobre `id` para que
-- funcione con el modo "safe updates" de Workbench.
-- =============================================================================

-- Invalidar tokens de recuperacion antiguos (ahora se guarda el hash).
UPDATE usuarios SET token_reset_password = NULL, token_reset_password_expiracion = NULL WHERE id > 0;

-- Liberar cuentas bloqueadas "para siempre" por el comportamiento anterior.
UPDATE usuarios SET bloqueado = 0, intentos_fallidos = 0 WHERE id > 0 AND bloqueado = 1 AND bloqueado_hasta IS NULL;

-- =============================================================================
-- Fase 2: verificacion de huerfanos. SOLO para ACTUALIZAR UNA BASE EXISTENTE con datos.
-- Ejecutar estas consultas ANTES de crear las FK compuestas de la Fase 2: cada una
-- debe devolver 0 filas (si no, el id apunta a algo inexistente o de otro club:
-- corregirlo o ponerlo en NULL). En una base nueva es innecesario (no hay datos) y,
-- con las FK ya creadas, nunca devolveran filas.
-- =============================================================================
-- SELECT 'deportistas.nivel_actual_id' AS columna, d.id, d.nivel_actual_id AS valor
-- FROM deportistas d LEFT JOIN niveles n ON n.id = d.nivel_actual_id AND n.club_id = d.club_id
-- WHERE d.nivel_actual_id IS NOT NULL AND n.id IS NULL;
--
-- SELECT 'deportistas.convenio_id' AS columna, d.id, d.convenio_id AS valor
-- FROM deportistas d LEFT JOIN convenios c ON c.id = d.convenio_id AND c.club_id = d.club_id
-- WHERE d.convenio_id IS NOT NULL AND c.id IS NULL;
--
-- SELECT 'historial_nivel_deportista.nivel_id' AS columna, h.id, h.nivel_id AS valor
-- FROM historial_nivel_deportista h LEFT JOIN niveles n ON n.id = h.nivel_id AND n.club_id = h.club_id
-- WHERE h.nivel_id IS NOT NULL AND n.id IS NULL;
--
-- SELECT 'historial_planes_deportista.plan_pago_id' AS columna, h.id, h.plan_pago_id AS valor
-- FROM historial_planes_deportista h LEFT JOIN planes_pago p ON p.id = h.plan_pago_id AND p.club_id = h.club_id
-- WHERE h.plan_pago_id IS NOT NULL AND p.id IS NULL;
--

-- =============================================================================
-- Fase 3: verificaciones. SOLO para ACTUALIZAR UNA BASE EXISTENTE con datos
-- (en una base nueva no hay filas y devolverian 0 resultados).
-- A) ANTES de crear los indices unicos (club_id, id) de personas y deportistas:
--    deben devolver 0 filas.
-- =============================================================================
-- SELECT 'personas sin club' AS problema, id FROM personas WHERE club_id IS NULL;
-- SELECT 'deportistas sin club' AS problema, id FROM deportistas WHERE club_id IS NULL;
-- SELECT nombre FROM permisos WHERE nombre IN ('ver_acudientes','gestionar_acudientes','ver_temporadas',
--   'gestionar_temporadas','ver_consentimientos','gestionar_consentimientos','ver_onboarding','restablecer_clave_usuario');

-- =============================================================================
-- B) Usuarios que probablemente TODAVIA tienen la clave por defecto "0000" (creados
--    antes de la Fase 3): obligados a cambiar la clave y que nunca han ingresado.
--    Genera para cada uno una clave temporal desde la app
--    (POST /api/usuario/{id}/clave-temporal) o desactivalos si ya no se usan.
-- =============================================================================
-- SELECT u.club_id, u.id, u.username, u.email, u.ultimo_acceso
-- FROM usuarios u
-- WHERE u.requiere_cambio_password = 1 AND u.ultimo_acceso IS NULL
-- ORDER BY u.club_id, u.id;
