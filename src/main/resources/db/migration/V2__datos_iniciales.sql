-- Flyway V2: datos iniciales (script 02 del proyecto: clubes, roles, permisos, catalogos y rubrica).
-- NO EDITAR una vez aplicado.
-- =============================================================================
-- CheerManager - SCRIPT 2: DATOS INICIALES
-- Ejecutar DESPUES de 01_cheermanager_base.sql (requiere AUTO_INCREMENT en `id`).
-- Contenido: roles, permisos, asignacion permisos->roles, catalogos deportivos
-- FEDECOLCHEER / IASF 2026, catalogos operativos y rubrica Elite N1-N4 2026.
--
-- Roles:
--   Super Administrador -> global/sistema, TODOS los permisos.
--   Administrador       -> administrador de un club, sin permisos de sistema
--                          (roles/permisos, configuracion, parametros, logs del
--                          sistema, dashboard global, importar/exportar datos).
-- Fase 1 (multi-club): clubes 1=Plataforma y 2=Ratels All Stars; Super Administrador
-- en la Plataforma; roles, niveles, divisiones y categorias_nivel sembrados en el club 2.
-- Permisos de plataforma (gestionar_clubes, gestionar_catalogos, configurar_rubrica)
-- solo para el Super Administrador.
-- Permisos de Fase 3 (acudientes, temporadas, consentimientos, onboarding, clave temporal):
-- Administrador y Super Administrador los reciben todos; Entrenador solo ver_acudientes y ver_temporadas.
-- Permisos nuevos de Fase 0 incluidos. Singular = un registro concreto;
-- plural = pagina general (listado).
-- Pensado para una base vacia. Los permisos/roles son idempotentes (INSERT
-- IGNORE); algunos catalogos de rubrica (seccion 10) no lo son: no repetir.
-- =============================================================================

SET NAMES utf8mb4;
-- =============================================================================
-- 9. Datos iniciales
-- =============================================================================

-- 9.0 Clubes (Fase 1 multi-club)
-- 1 = Plataforma (solo Super Administrador); 2 = Ratels All Stars (club real).
INSERT IGNORE INTO clubes (id, nombre, slug) VALUES
(1, 'Plataforma', 'plataforma'),
(2, 'Ratels All Stars', 'ratels-all-stars');

-- 9.1 Roles predefinidos
-- Super Administrador = global/sistema (todos los permisos), pertenece a la Plataforma (club 1).
-- Administrador       = administrador de UN club (sin permisos de sistema).
-- Los demas roles pertenecen al club 2 (cada club tiene su propio juego de roles).
INSERT IGNORE INTO roles (club_id, nombre, descripcion) VALUES
(1, 'Super Administrador', 'Administrador global del sistema, configuración, seguridad y permisos'),
(2, 'Administrador', 'Administrador de club, con acceso a todos los módulos operativos'),
(2, 'Director Deportivo', 'Supervisa entrenadores, equipos, categorías, evaluaciones y organización deportiva'),
(2, 'Entrenador', 'Entrenador principal responsable de uno o varios equipos'),
(2, 'Asistente', 'Asistente del entrenador con permisos operativos limitados'),
(2, 'Coordinador Administrativo', 'Gestiona personas, inscripciones, documentos, horarios y organización administrativa'),
(2, 'Tesorero', 'Responsable de finanzas, pagos, facturación y planes de pago'),
(2, 'Encargado de Inventario', 'Gestiona productos, inventario, movimientos y ventas'),
(2, 'Deportista', 'Integrante del club con acceso únicamente a su información personal');

-- 9.2 Permisos predefinidos
INSERT IGNORE INTO permisos (nombre) VALUES
-- Administración del sistema
('administrar_roles'),
('administrar_permisos'),
('configurar_sistema'),
('gestionar_parametros'),
('ver_dashboard_global'),

-- Seguridad
('restablecer_contrasena'),
('desbloquear_usuario'),
('ver_logs_sistema'),

-- Finanzas
('aprobar_pago'),
('rechazar_pago'),
('cerrar_factura'),
('reabrir_factura'),

-- Eventos
('cerrar_evento'),
('publicar_resultados'),

-- Evaluaciones
('aprobar_evaluacion'),

-- Organización deportiva
('crear_temporada'),
('cerrar_temporada'),

-- Datos
('exportar_datos'),
('importar_datos'),

-- Anular operaciones
('anular_factura'),
('anular_pago'),
('anular_venta'),

-- Asignar (relaciones)
('asignar_a_grupo'),
('asignar_categoria_nivel'),
('asignar_descuento_factura'),
('asignar_destinatario_notificacion'),
('asignar_entrenador_grupo'),
('asignar_entrenador_horario'),
('asignar_grupo_evento'),
('asignar_horario_grupo'),
('asignar_roles'),

-- Cambiar estado
('cambiar_estado_objetivo'),
('cambiar_nivel_deportista'),

-- Crear entidades
('crear_categoria_nivel'),
('crear_contacto_emergencia'),
('crear_convenio'),
('crear_criterio_evaluacion'),
('crear_deportista'),
('crear_descuento'),
('crear_division'),
('crear_entrenador'),
('crear_evento'),
('crear_factura'),
('crear_grupo_entrenamiento'),
('crear_historial_medico'),
('crear_horario'),
('crear_nivel'),
('crear_notificacion'),
('crear_objetivo'),
('crear_persona'),
('crear_plan_pago'),
('crear_premio'),
('crear_producto'),
('crear_regla_categoria'),
('crear_usuario'),

-- Eliminar entidades
('eliminar_asistencia'),
('eliminar_categoria_nivel'),
('eliminar_contacto_emergencia'),
('eliminar_convenio'),
('eliminar_criterio_evaluacion'),
('eliminar_deportista'),
('eliminar_descuento'),
('eliminar_division'),
('eliminar_documento'),
('eliminar_entrenador'),
('eliminar_evaluacion'),
('eliminar_evento'),
('eliminar_premio'),
('eliminar_grupo_entrenamiento'),
('eliminar_historial_medico'),
('eliminar_horario'),
('eliminar_nivel'),
('eliminar_notificacion'),
('eliminar_objetivo'),
('eliminar_persona'),
('eliminar_plan_pago'),
('eliminar_producto'),
('eliminar_regla_categoria'),
('eliminar_usuario'),

-- Generar reportes
('generar_reporte_asistencia'),
('generar_reporte_evaluacion'),
('generar_reporte_financiero'),
('generar_reporte_inventario'),

-- Gestionar
('gestionar_inscripciones'),

-- Modificar entidades
('modificar_asistencia'),
('modificar_categoria_nivel'),
('modificar_contacto_emergencia'),
('modificar_convenio'),
('modificar_criterio_evaluacion'),
('modificar_deportista'),
('modificar_descuento'),
('modificar_entrenador'),
('modificar_division'),
('modificar_documento'),
('modificar_evaluacion'),
('modificar_evento'),
('modificar_premio'),
('modificar_factura'),
('modificar_grupo_entrenamiento'),
('modificar_historial_medico'),
('modificar_horario'),
('modificar_nivel'),
('modificar_notificacion'),
('modificar_objetivo'),
('modificar_pago'),
('modificar_persona'),
('modificar_plan_pago'),
('modificar_producto'),
('modificar_regla_categoria'),
('modificar_usuario'),
('modificar_venta'),

-- Registrar operaciones
('registrar_asistencia'),
('registrar_asistencia_evento'),
('registrar_evaluacion'),
('registrar_movimiento_inventario'),
('registrar_pago'),
('registrar_resultado_competencia'),
('registrar_venta'),

-- Retirar/Subir
('retirar_de_grupo'),
('subir_documento'),

-- Ver entidades y dashboards
('ver_asistencia'),
('ver_auditoria_descuentos_convenios'),
('ver_auditoria_evaluaciones'),
('ver_auditoria_eventos'),
('ver_auditoria_financiera'),
('ver_auditoria_historial_medico'),
('ver_auditoria_inventario'),
('ver_auditoria_personas'),
('ver_auditoria_usuarios'),
('ver_categoria_nivel'),
('ver_contacto_emergencia'),
('ver_convenio'),
('ver_criterio_evaluacion'),
('ver_dash_administracion'),
('ver_dash_evaluacion'),
('ver_dash_eventos'),
('ver_dash_finanzas'),
('ver_dash_horarios'),
('ver_dash_inicio'),
('ver_dash_organizacion'),
('ver_dash_personas'),
('ver_dash_tienda'),
('ver_dashboard'),
('ver_deportista'),
('ver_descuento'),
('ver_division'),
('ver_documento'),
('ver_entrenador'),
('ver_evaluacion'),
('ver_evento'),
('ver_factura'),
('ver_grupo_entrenamiento'),
('ver_historial_evaluacion_deportista'),
('ver_historial_medico'),
('ver_historial_nivel_deportista'),
('ver_horario'),
('ver_logs_acceso'),
('ver_mi_perfil'),
('ver_movimiento_inventario'),
('ver_nivel'),
('ver_notificacion'),
('enviar_notificacion'),
('ver_objetivo'),
('ver_pago'),
('ver_permisos'),
('ver_persona'),
('ver_plan_pago'),
('ver_premio'),
('ver_producto'),
('ver_regla_categoria'),
('ver_roles'),
('ver_usuarios'),
('ver_venta'),
-- Nuevos (Fase 0). Singular = acceso a un registro concreto; plural = acceso a la pagina general (listado).
('asignar_permiso'),
('configurar_rubrica'),
('crear_evaluacion'),
('crear_evaluacion_rutina'),
('crear_grupo'),
('eliminar_factura'),
('eliminar_grupo'),
('eliminar_pago'),
('gestionar_catalogos'),
('gestionar_clubes'),
('gestionar_convenio'),
('gestionar_descuento'),
('gestionar_division'),
('gestionar_plan_pago'),
('gestionar_roles'),
('modificar_grupo'),
('modificar_inscripcion'),
('ver_deportistas'),
('ver_evaluaciones'),
('ver_grupo'),
('ver_inscripcion'),
('ver_objetivos'),
('ver_usuario'),
-- Fase 3: acudientes, temporadas, consentimientos (Ley 1581), onboarding y clave temporal
('ver_acudientes'),
('gestionar_acudientes'),
('ver_temporadas'),
('gestionar_temporadas'),
('ver_consentimientos'),
('gestionar_consentimientos'),
('ver_onboarding'),
('restablecer_clave_usuario');

-- =============================================================================
-- 9.3 Asignación de permisos por rol
-- =============================================================================

-- SUPER ADMINISTRADOR (global/sistema): TODOS los permisos, incluidos los nuevos.
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.nombre = 'Super Administrador';

-- ADMINISTRADOR (de club): gestiona todo lo de "su club" (usuarios, roles asignados,
-- personas, deportistas, finanzas, eventos, catálogos deportivos, etc.), pero NO la
-- configuración global del sistema ni la gestión de roles/permisos, que quedan
-- reservadas al Super Administrador.
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'Administrador' AND p.nombre NOT IN (
    -- Administración global del sistema -> solo Super Administrador
    'administrar_permisos',
    'administrar_roles',
    'asignar_permiso',
    'gestionar_roles',
    'configurar_sistema',
    'gestionar_parametros',
    'ver_dashboard_global',
    'ver_logs_sistema',
    'exportar_datos',
    'importar_datos',
    -- Plataforma multi-club (Fase 1): catalogos, rubrica y alta de clubes son globales
    'gestionar_clubes',
    'gestionar_catalogos',
    'configurar_rubrica'
);

-- DIRECTOR DEPORTIVO
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'Director Deportivo' AND p.nombre IN (
-- Deportistas
    'crear_deportista', 'modificar_deportista', 'ver_deportista', 'ver_deportistas',
-- Entrenador
    'crear_entrenador', 'modificar_entrenador', 'ver_entrenador',
-- Grupos de entrenamiento
    'crear_grupo_entrenamiento', 'modificar_grupo_entrenamiento', 'ver_grupo_entrenamiento', 'crear_grupo', 'modificar_grupo', 'ver_grupo',
    'asignar_a_grupo', 'retirar_de_grupo', 'asignar_entrenador_grupo',
-- Horarios
    'crear_horario', 'modificar_horario', 'ver_horario', 'asignar_horario_grupo', 'asignar_entrenador_horario',
-- Eventos
    'crear_evento', 'modificar_evento', 'ver_evento', 'asignar_grupo_evento', 'registrar_resultado_competencia', 'publicar_resultados',
-- Evaluaciones
    'crear_criterio_evaluacion', 'registrar_evaluacion', 'crear_evaluacion', 'crear_evaluacion_rutina', 'modificar_evaluacion', 'ver_evaluacion', 'ver_evaluaciones',
    'crear_objetivo', 'modificar_objetivo', 'cambiar_estado_objetivo', 'ver_objetivo', 'ver_objetivos', 'cambiar_nivel_deportista',
-- Asistencia
    'registrar_asistencia', 'registrar_asistencia_evento', 'modificar_asistencia', 'ver_asistencia', 'eliminar_asistencia',
-- Dashboards
    'ver_dashboard', 'ver_dash_inicio', 'ver_dash_horarios', 'ver_dash_organizacion', 'ver_dash_evaluacion'
);

-- ENTRENADOR
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'Entrenador' AND p.nombre IN (
    -- Deportistas
    'crear_deportista', 'modificar_deportista', 'ver_deportista', 'ver_deportistas', 'ver_acudientes', 'ver_temporadas',
    -- Horarios
    'crear_horario', 'modificar_horario', 'ver_horario', 'asignar_horario_grupo',
    -- Asistencia
    'registrar_asistencia', 'registrar_asistencia_evento', 'modificar_asistencia', 'ver_asistencia', 'eliminar_asistencia',
    -- Grupos de entrenamiento
    -- 'crear_grupo_entrenamiento', 'modificar_grupo_entrenamiento', 'ver_grupo_entrenamiento',
    -- 'asignar_a_grupo', 'retirar_de_grupo', 'asignar_entrenador_grupo', 'asignar_entrenador_horario',
    -- Eventos
    'modificar_evento', 'ver_evento', 'asignar_grupo_evento',
    -- Evaluaciones
    'registrar_evaluacion', 'crear_evaluacion', 'crear_evaluacion_rutina', 'modificar_evaluacion', 'ver_evaluacion', 'ver_evaluaciones',
    'crear_objetivo', 'modificar_objetivo', 'cambiar_estado_objetivo', 'ver_objetivo', 'ver_objetivos',
    -- Historiales
    'ver_historial_medico', 'ver_historial_evaluacion_deportista', 'ver_historial_nivel_deportista',
    -- Notificaciones
    'crear_notificacion', 'ver_notificacion',
    -- Nivel y categorías
    'cambiar_nivel_deportista', 'asignar_categoria_nivel', 'ver_categoria_nivel',
    -- Reportes
    'generar_reporte_asistencia', 'generar_reporte_evaluacion',
    -- Dashboards
    'ver_dashboard', 'ver_dash_inicio', 'ver_dash_horarios', 'ver_dash_organizacion', 'ver_dash_evaluacion',
    -- Perfil
    'ver_mi_perfil', 'ver_permisos'
);

-- ASISTENTE
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'Asistente' AND p.nombre IN (
    -- Deportistas
     'ver_deportista', 'ver_deportistas',
    -- Horarios
    'ver_horario',
    -- Asistencia
    'registrar_asistencia', 'registrar_asistencia_evento', 'modificar_asistencia', 'ver_asistencia', 'eliminar_asistencia',
    -- Grupos de entrenamiento
    'ver_grupo_entrenamiento', 'ver_grupo',
    'asignar_a_grupo', 'retirar_de_grupo',
    -- Eventos
     'ver_evento', 'asignar_grupo_evento',
    -- Evaluaciones
    'modificar_evaluacion', 'ver_evaluacion', 'ver_evaluaciones',
    'modificar_objetivo', 'cambiar_estado_objetivo', 'ver_objetivo', 'ver_objetivos',
    -- Historiales
    'ver_historial_medico', 'ver_historial_evaluacion_deportista', 'ver_historial_nivel_deportista',
    -- Notificaciones
    'crear_notificacion', 'ver_notificacion',
    -- Nivel y categorías
    'ver_categoria_nivel',
    -- Reportes
    -- 'generar_reporte_asistencia', 'generar_reporte_evaluacion',
    -- Dashboards
    'ver_dashboard', 'ver_dash_inicio', 'ver_dash_horarios', 'ver_dash_organizacion', 'ver_dash_evaluacion',
    -- Perfil
    'ver_mi_perfil', 'ver_permisos'
);

-- COORDINADOR ADMINISTRATIVO
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'Coordinador Administrativo' AND p.nombre IN (
-- Persona
    'crear_persona', 'modificar_persona', 'ver_persona', 'gestionar_inscripciones', 'modificar_inscripcion', 'ver_inscripcion',
-- Deportistas
    'crear_deportista', 'modificar_deportista', 'ver_deportista', 'ver_deportistas',
-- Entrenador
    'ver_entrenador',
-- Grupos de entrenamiento
    'ver_grupo_entrenamiento', 'ver_grupo',
    'asignar_a_grupo', 'retirar_de_grupo', 'asignar_entrenador_grupo',
-- Horarios
    'crear_horario', 'modificar_horario', 'ver_horario', 'asignar_horario_grupo', 'asignar_entrenador_horario',
-- Eventos
    'crear_evento', 'modificar_evento', 'ver_evento', 'asignar_grupo_evento', 'registrar_resultado_competencia', 'publicar_resultados',
-- Evaluaciones
    'crear_criterio_evaluacion', 'registrar_evaluacion', 'crear_evaluacion', 'crear_evaluacion_rutina', 'modificar_evaluacion', 'ver_evaluacion', 'ver_evaluaciones',
    'crear_objetivo', 'modificar_objetivo', 'cambiar_estado_objetivo', 'ver_objetivo', 'ver_objetivos', 'cambiar_nivel_deportista',
-- Asistencia
    'registrar_asistencia', 'registrar_asistencia_evento', 'modificar_asistencia', 'ver_asistencia', 'eliminar_asistencia',
-- Dashboards
    'ver_dashboard', 'ver_dash_inicio', 'ver_dash_horarios', 'ver_dash_organizacion', 'ver_dash_evaluacion'
);

-- DEPORTISTA
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'Deportista' AND p.nombre IN (
    'ver_mi_perfil',
    'ver_asistencia',
    'ver_evento',
    'ver_horario',
    'ver_notificacion',
    'ver_evaluacion',
    'ver_historial_evaluacion_deportista',
    'ver_historial_nivel_deportista',
    'ver_permisos',
    'ver_dashboard', 'ver_dash_inicio', 'ver_dash_eventos'
);

-- TESORERO
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'Tesorero' AND p.nombre IN (
    -- Facturas
    'crear_factura', 'modificar_factura', 'ver_factura', 'anular_factura',
    -- Pagos
    'registrar_pago', 'modificar_pago', 'ver_pago', 'anular_pago',
    -- Ventas
    'registrar_venta', 'modificar_venta', 'ver_venta', 'anular_venta',
    -- Planes de pago
    'crear_plan_pago', 'modificar_plan_pago', 'ver_plan_pago', 'gestionar_plan_pago', 'eliminar_plan_pago',
    -- Convenios
    'crear_convenio', 'modificar_convenio', 'ver_convenio', 'gestionar_convenio', 'eliminar_convenio',
    -- Descuentos
    'crear_descuento', 'modificar_descuento', 'ver_descuento', 'gestionar_descuento', 'asignar_descuento_factura', 'eliminar_descuento',
    -- Notificaciones
    'crear_notificacion', 'ver_notificacion', 'asignar_destinatario_notificacion',
    -- Auditoría
    'ver_auditoria_financiera', 'ver_auditoria_descuentos_convenios',
    -- Reportes
    'generar_reporte_financiero',
    -- Dashboards
    'ver_dashboard', 'ver_dash_finanzas',
    -- Perfil
    'ver_mi_perfil', 'ver_permisos'
);

-- ENCARGADO DE INVENTARIO
INSERT IGNORE INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id FROM roles r, permisos p
WHERE r.nombre = 'Encargado de Inventario' AND p.nombre IN (
    -- Productos
    'crear_producto', 'modificar_producto', 'ver_producto', 'eliminar_producto',
    -- Inventario
    'registrar_movimiento_inventario', 'ver_movimiento_inventario',
    -- Ventas
    'registrar_venta', 'modificar_venta', 'ver_venta', 'anular_venta',
    -- Notificaciones
    'ver_notificacion',
    -- Dashboards
    'ver_dashboard', 'ver_dash_inicio', 'ver_dash_tienda',
    -- Perfil
    'ver_mi_perfil', 'ver_permisos'
);

-- =============================================================================
-- 9.4 Catálogo deportivo FEDECOLCHEER / IASF 2026
-- Fuente: "Divisiones y Categorías All Stars 2026" (FEDECOLCHEER, v2, 20-ene-2026)
-- =============================================================================

-- Géneros (valor estándar, no viene explícito en los PDF de la federación)
INSERT IGNORE INTO generos (genero) VALUES
('Femenino'),
('Masculino'),
('Otro'),
('Prefiero no decir');

-- Días de la semana (catálogo estándar para horarios)
INSERT IGNORE INTO dias_semana (dia) VALUES
('Lunes'),('Martes'),('Miércoles'),('Jueves'),('Viernes'),('Sábado'),('Domingo');

-- Estados de categoría de nivel
INSERT IGNORE INTO estados_categorias_nivel (estado_categoria) VALUES
('Activa'),
('Inactiva');

-- Tipos de grupo de entrenamiento: segmentos oficiales FEDECOLCHEER 2026
-- (Novice / Prep / Elite / International — ver "Divisiones All-Star Colombia 2026")
INSERT IGNORE INTO tipos_grupo (nombre) VALUES
('Fundamentacion'),
('Novice'),
('Prep'),
('Elite'),
('International');

-- Niveles de competencia (etiqueta = Segmento + Nivel, según el PDF de divisiones)
INSERT IGNORE INTO niveles (nombre, club_id) VALUES
('Novice N1', 2),
('Prep 1.1', 2),
('Prep 2.1', 2),
('Elite 1', 2),
('Elite 2', 2),
('Elite 3', 2),
('Elite 4', 2),
('Elite 4.2', 2),
('International 5', 2),
('International 6', 2),
('International 7', 2);

-- Divisiones por edad/género (columna "DIVISIÓN" del PDF)
INSERT IGNORE INTO divisiones (nombre, club_id) VALUES
('Tiny', 2),
('Mini', 2),
('Youth', 2),
('Junior', 2),
('Senior', 2),
('Open', 2),
('Open Fem', 2),
('Open Medium', 2),
('Open Large', 2);

-- Categorías de nivel: combinación Nivel × División con rango de año de
-- nacimiento y cantidad de deportistas, según la tabla oficial 2026.
-- NOTA IMPORTANTE: la tabla oficial también exige límites de CANTIDAD DE
-- HOMBRES para "Open Medium" (máx. 8) y "Open Large" (mín. 9) dentro de
-- Elite 4. El esquema actual de categorias_nivel NO tiene una columna para
-- este límite por género; queda documentado aquí pero no se puede validar
-- a nivel de base de datos sin agregar una columna nueva (p. ej.
-- cantidad_hombres_min / cantidad_hombres_max). Avísame si quieres que la
-- agregue.
INSERT INTO categorias_nivel
    (nombre, nivel_id, division_id, año_nacimiento_min, año_nacimiento_max, cantidad_min, cantidad_max, estado_categorias_nivel_id, club_id)
SELECT 'Novice N1 - Tiny', n.id, d.id, 2019, 2021, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Novice N1' AND d.nombre = 'Tiny' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Novice N1 - Mini', n.id, d.id, 2017, 2021, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Novice N1' AND d.nombre = 'Mini' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Novice N1 - Youth', n.id, d.id, 2013, 2018, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Novice N1' AND d.nombre = 'Youth' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Prep 1.1 - Mini', n.id, d.id, 2017, 2021, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Prep 1.1' AND d.nombre = 'Mini' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Prep 1.1 - Youth', n.id, d.id, 2013, 2018, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Prep 1.1' AND d.nombre = 'Youth' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Prep 1.1 - Junior', n.id, d.id, 2009, 2014, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Prep 1.1' AND d.nombre = 'Junior' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Prep 1.1 - Senior', n.id, d.id, 2007, 2012, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Prep 1.1' AND d.nombre = 'Senior' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Prep 2.1 - Junior', n.id, d.id, 2009, 2014, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Prep 2.1' AND d.nombre = 'Junior' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Prep 2.1 - Open', n.id, d.id, NULL, 2011, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Prep 2.1' AND d.nombre = 'Open' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 1 - Tiny', n.id, d.id, 2019, 2021, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 1' AND d.nombre = 'Tiny' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 1 - Mini', n.id, d.id, 2017, 2021, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 1' AND d.nombre = 'Mini' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 1 - Youth', n.id, d.id, 2013, 2018, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 1' AND d.nombre = 'Youth' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 1 - Junior', n.id, d.id, 2009, 2014, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 1' AND d.nombre = 'Junior' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 1 - Senior', n.id, d.id, 2007, 2012, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 1' AND d.nombre = 'Senior' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 2 - Youth', n.id, d.id, 2013, 2018, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 2' AND d.nombre = 'Youth' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 2 - Junior', n.id, d.id, 2009, 2014, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 2' AND d.nombre = 'Junior' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 2 - Senior', n.id, d.id, 2007, 2012, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 2' AND d.nombre = 'Senior' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 2 - Open', n.id, d.id, NULL, 2011, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 2' AND d.nombre = 'Open' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 3 - Youth', n.id, d.id, 2013, 2018, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 3' AND d.nombre = 'Youth' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 3 - Junior', n.id, d.id, 2009, 2014, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 3' AND d.nombre = 'Junior' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 3 - Senior', n.id, d.id, 2007, 2012, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 3' AND d.nombre = 'Senior' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 3 - Open', n.id, d.id, NULL, 2011, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 3' AND d.nombre = 'Open' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 4.2 - Open', n.id, d.id, NULL, 2011, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 4.2' AND d.nombre = 'Open' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 4 - Senior', n.id, d.id, 2007, 2012, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 4' AND d.nombre = 'Senior' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'Elite 4 - Open Fem', n.id, d.id, NULL, 2011, 8, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 4' AND d.nombre = 'Open Fem' AND e.estado_categoria = 'Activa'  -- Femenino
UNION ALL
SELECT 'Elite 4 - Open Medium', n.id, d.id, NULL, 2011, 10, 24, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 4' AND d.nombre = 'Open Medium' AND e.estado_categoria = 'Activa'  -- Mixto, hasta 8 hombres (no modelado en columna)
UNION ALL
SELECT 'Elite 4 - Open Large', n.id, d.id, NULL, 2011, 10, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'Elite 4' AND d.nombre = 'Open Large' AND e.estado_categoria = 'Activa'  -- Mixto, 9+ hombres (no modelado en columna)
UNION ALL
SELECT 'International 5 - Open', n.id, d.id, NULL, 2011, 10, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'International 5' AND d.nombre = 'Open' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'International 6 - Open', n.id, d.id, NULL, 2011, 10, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'International 6' AND d.nombre = 'Open' AND e.estado_categoria = 'Activa'
UNION ALL
SELECT 'International 7 - Open', n.id, d.id, NULL, 2010, 10, 30, e.id, 2
FROM niveles n, divisiones d, estados_categorias_nivel e
WHERE n.nombre = 'International 7' AND d.nombre = 'Open' AND e.estado_categoria = 'Activa';

-- =============================================================================
-- 9.5 Catálogos operativos base (valores SUGERIDOS, no vienen de FEDECOLCHEER/IASF)
-- =============================================================================
-- Estas tablas son de uso interno de CheerManager (estado de un deportista,
-- tipo de evento del club, etc.) y no están definidas en ningún reglamento.
-- Van con valores típicos de este tipo de sistema; AJÚSTALOS si tu operación
-- real usa otros nombres o estados. No se inventaron datos deportivos/de
-- reglamento — solo catálogos de flujo interno de la app.

INSERT IGNORE INTO estados_persona (estado) VALUES
('Activo'),
('Inactivo'),
('Retirado'),
('Suspendido'),
('Lesionado');

INSERT IGNORE INTO estados_asistencia (estado) VALUES
('Presente'),
('Ausente'),
('Tarde'),
('Justificado');

INSERT IGNORE INTO estados_objetivo (nombre_estado) VALUES
('Pendiente'),
('En progreso'),
('Cumplido'),
('Cancelado');

INSERT IGNORE INTO tipos_evento (evento) VALUES
('Competencia'),
('Exhibición'),
('Clínica / Entrenamiento especial'),
('Evento social'),
('Reunión de padres'),
('Capacitacion');

-- tipos_categoria alimenta criterios_evaluacion; se alinea con las
-- 4 secciones de la rúbrica IASF/FEDECOLCHEER (ver módulo de rúbrica, sección 8)
INSERT IGNORE INTO tipos_categoria (categoria) VALUES
('Construcciones'),
('Gimnasia'),
('Ejecución'),
('General');

INSERT IGNORE INTO metodos_pago (metodo) VALUES
('Efectivo'),
('Transferencia bancaria'),
('Tarjeta de crédito/débito'),
('PSE'),
('Nequi/Daviplata');

INSERT IGNORE INTO tipos_pago (tipo) VALUES
('Mensualidad'),
('Inscripción'),
('Uniforme'),
('Evento/Competencia'),
('Producto de tienda');

-- =============================================================================
-- 10. Datos de la Rúbrica de Calificación Élite N1-N4 2026
-- Fuente: rubrica_datos_iniciales.sql (FEDECOLCHEER / IASF United Scoring System)
-- =============================================================================

-- ----------------------------------------------------------
-- DATOS INICIALES: RÚBRICA ALL STAR ÉLITE N1-N4 2026
-- Fuente: FEDECOLCHEER / IASF United Scoring System 2026
-- ----------------------------------------------------------
-- Este script carga los datos de configuración de la rúbrica.
-- Las fórmulas de cálculo viven en el código (calculadoras/).
-- Solo los VALORES y ESCALONES están aquí — cambiarlos vía API
-- o editando este script cuando FEDECOLCHEER actualice la rúbrica.
-- ----------------------------------------------------------

-- ── 1. NIVELES ───────────────────────────────────────────────
INSERT INTO niveles_competencia (nombre, modalidad, activo) VALUES
('N1',   'Elite', true),
('N2',   'Elite', true),
('N3',   'Elite', true),
('N4',   'Elite', true),
('N4.2', 'Elite', true);
-- (Prep 1.1, Prep 2.1, Novice Tiny, Novice Mini/Youth se agregan
--  como una tanda nueva cuando se incorpore esa modalidad.)

-- ── 2. SUB-CRITERIOS ─────────────────────────────────────────
-- Los tipos de cálculo mapean a TipoCalculoSubCriterio.java.
INSERT INTO sub_criterios_rubrica (nombre, seccion, tipo_calculo) VALUES
-- Construcciones
('Dificultad de Elevaciones',          'Construcciones', 'ESCALON_HABILIDADES_GRUPOS'),
('Dificultad de Piramides',            'Construcciones', 'ESCALON_HABILIDADES_GRUPOS'),
('Dificultad de Lanzamientos',         'Construcciones', 'ESCALON_CANTIDAD_SIMPLE'),
-- Gimnasia
('Dificultad de Saltos',               'Gimnasia',       'ESCALON_CANTIDAD_SIMPLE'),
('Dificultad de Gimnasia Estatica',    'Gimnasia',       'ESCALON_GIMNASIA'),
('Dificultad de Gimnasia con Carrera', 'Gimnasia',       'ESCALON_GIMNASIA'),
-- Ejecución
('Ejecucion Elevaciones Piramides Gimnasia', 'Ejecucion', 'VALOR_INICIAL_MENOS_DRIVERS'),
('Ejecucion Lanzamientos Saltos',            'Ejecucion', 'VALOR_INICIAL_MENOS_DRIVERS'),
-- General
('Formaciones y Transiciones',  'General', 'RANGO_DIRECTO_JUEZ'),
('Creatividad de Rutina',       'General', 'RANGO_DIRECTO_JUEZ'),
('Dance Dificultad Elementos',  'General', 'RANGO_DIRECTO_JUEZ'),
('Dance Ejecucion',             'General', 'RANGO_DIRECTO_JUEZ'),
('Showmanship',                 'General', 'RANGO_DIRECTO_JUEZ');

-- ── 3. TABLAS DE CANTIDAD ────────────────────────────────────
-- Fuente: "TABLA DE CANTIDAD EN CONSTRUCCIÓN" y
--         "TABLA DE CANTIDAD EN SALTOS/GIMNASIA" — Rúbrica Élite N1-N4 2026.
-- Aplica igual para N1, N2, N3, N4 y N4.2.

-- Construcciones (unidad = grupos)
--   # Atletas | MAYORÍA | GRAN PARTE | MÁXIMO
--    5 - 11   |    1    |     2      |    3
--   12 - 17   |    2    |     3      |    4
--   18 - 22   |    3    |     4      |    5
--   23 - 30   |    4    |     5      |    6
--   31 - 38   |    5    |     6      |    7

-- Saltos/Gimnasia (unidad = atletas)
--   # Atletas | MAYORÍA | GRAN PARTE | MÁXIMO
--    5 - 11   |    5    |     6      |   10
--   12 - 17   |    6    |     7      |   12
--   18 - 22   |    9    |    10      |   18
--   23 - 30   |   11    |    12      |   22
--   31 - 38   |   15    |    16      |   30

-- Se inserta una fila por cada nivel × tabla × rango.
-- (En lugar de repetir 5 veces por nivel, usamos un bloque por nivel.)

-- Para no hardcodear los IDs de nivel asumiendo secuencias, usamos
-- subqueries para cada inserción. Hibernate con ddl-auto=update asignará
-- IDs automáticamente, así que esta es la manera segura.

INSERT INTO tabla_cantidad_nivel (nivel_id, tabla, unidad, rango_min_atletas, rango_max_atletas, mayoria, gran_parte, maximo)
SELECT n.id, 'Construcciones', 'grupos', 5, 11, 1, 2, 3 FROM niveles_competencia n WHERE n.nombre = 'N1'
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 12, 17, 2, 3, 4 FROM niveles_competencia n WHERE n.nombre = 'N1'
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 18, 22, 3, 4, 5 FROM niveles_competencia n WHERE n.nombre = 'N1'
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 23, 30, 4, 5, 6 FROM niveles_competencia n WHERE n.nombre = 'N1'
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 31, 38, 5, 6, 7 FROM niveles_competencia n WHERE n.nombre = 'N1'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 5, 11, 5, 6, 10 FROM niveles_competencia n WHERE n.nombre = 'N1'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 12, 17, 6, 7, 12 FROM niveles_competencia n WHERE n.nombre = 'N1'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 18, 22, 9, 10, 18 FROM niveles_competencia n WHERE n.nombre = 'N1'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 23, 30, 11, 12, 22 FROM niveles_competencia n WHERE n.nombre = 'N1'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 31, 38, 15, 16, 30 FROM niveles_competencia n WHERE n.nombre = 'N1'
-- N2 (mismas tablas de cantidad)
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 5, 11, 1, 2, 3 FROM niveles_competencia n WHERE n.nombre = 'N2'
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 12, 17, 2, 3, 4 FROM niveles_competencia n WHERE n.nombre = 'N2'
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 18, 22, 3, 4, 5 FROM niveles_competencia n WHERE n.nombre = 'N2'
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 23, 30, 4, 5, 6 FROM niveles_competencia n WHERE n.nombre = 'N2'
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 31, 38, 5, 6, 7 FROM niveles_competencia n WHERE n.nombre = 'N2'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 5, 11, 5, 6, 10 FROM niveles_competencia n WHERE n.nombre = 'N2'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 12, 17, 6, 7, 12 FROM niveles_competencia n WHERE n.nombre = 'N2'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 18, 22, 9, 10, 18 FROM niveles_competencia n WHERE n.nombre = 'N2'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 23, 30, 11, 12, 22 FROM niveles_competencia n WHERE n.nombre = 'N2'
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 31, 38, 15, 16, 30 FROM niveles_competencia n WHERE n.nombre = 'N2'
-- N3, N4, N4.2 — mismas tablas
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 5, 11, 1, 2, 3 FROM niveles_competencia n WHERE n.nombre IN ('N3','N4','N4.2')
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 12, 17, 2, 3, 4 FROM niveles_competencia n WHERE n.nombre IN ('N3','N4','N4.2')
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 18, 22, 3, 4, 5 FROM niveles_competencia n WHERE n.nombre IN ('N3','N4','N4.2')
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 23, 30, 4, 5, 6 FROM niveles_competencia n WHERE n.nombre IN ('N3','N4','N4.2')
UNION ALL SELECT n.id, 'Construcciones', 'grupos', 31, 38, 5, 6, 7 FROM niveles_competencia n WHERE n.nombre IN ('N3','N4','N4.2')
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 5, 11, 5, 6, 10 FROM niveles_competencia n WHERE n.nombre IN ('N3','N4','N4.2')
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 12, 17, 6, 7, 12 FROM niveles_competencia n WHERE n.nombre IN ('N3','N4','N4.2')
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 18, 22, 9, 10, 18 FROM niveles_competencia n WHERE n.nombre IN ('N3','N4','N4.2')
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 23, 30, 11, 12, 22 FROM niveles_competencia n WHERE n.nombre IN ('N3','N4','N4.2')
UNION ALL SELECT n.id, 'Saltos_Gimnasia', 'atletas', 31, 38, 15, 16, 30 FROM niveles_competencia n WHERE n.nombre IN ('N3','N4','N4.2');

-- ── 4. TOPES DE SECCIÓN ───────────────────────────────────────
-- Fuente: secciones "DIFICULTAD DE ELEVACIONES", "DIFICULTAD DE PIRÁMIDES",
-- "DIFICULTAD DE LANZAMIENTOS", "DIFICULTAD DE SALTOS",
-- "DIFICULTAD DE GIMNASIA ESTÁTICA/CON CARRERA" de la rúbrica Élite N1-N4.
--
-- Las columnas condicion_habilidades_diferentes y condicion_grupo_minimo
-- codifican la condición del escalón en la rúbrica. La calculadora las
-- evalúa contra lo que reportó el juez.

-- Dificultad de Elevaciones (igual para N1, N2, N3, N4, N4.2)
-- orden desc = la calculadora lo evalúa de mayor a menor
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, descripcion_rubrica)
SELECT n.id, sc.id, 5, 4.50, 4, 'GRAN_PARTE',
    '4 habilidades diferentes Apropiadas sincronizadas/canon por GRAN PARTE sin repetir atletas'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Elevaciones'
UNION ALL
SELECT n.id, sc.id, 4, 4.00, 3, 'GRAN_PARTE',
    '3 habilidades diferentes Apropiadas sincronizadas/canon por GRAN PARTE sin repetir atletas'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Elevaciones'
UNION ALL
SELECT n.id, sc.id, 3, 3.50, 2, 'GRAN_PARTE',
    '2 habilidades diferentes Apropiadas sincronizadas/canon por GRAN PARTE sin repetir atletas'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Elevaciones'
UNION ALL
SELECT n.id, sc.id, 2, 3.00, 4, 'GRAN_PARTE',
    '4 habilidades diferentes Apropiadas del nivel realizadas por GRAN PARTE del equipo'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Elevaciones'
UNION ALL
SELECT n.id, sc.id, 1, 2.50, 0, 'MAYORIA',
    'Las habilidades no cumplen con los requerimientos de 3.0'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Elevaciones';

-- Dificultad de Pirámides
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, descripcion_rubrica)
SELECT n.id, sc.id, 4, 4.00, 4, 'GRAN_PARTE',
    'ALTO: 4 habilidades Apropiadas y 2 estructuras realizadas por GRAN PARTE'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Piramides'
UNION ALL
SELECT n.id, sc.id, 3, 3.50, 3, 'GRAN_PARTE',
    'ALTO-MEDIO: 3+ habilidades Apropiadas y 2 estructuras por GRAN PARTE'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Piramides'
UNION ALL
SELECT n.id, sc.id, 2, 3.00, 3, 'GRAN_PARTE',
    'MEDIO: 3 habilidades Apropiadas y 2 estructuras realizadas por GRAN PARTE'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Piramides'
UNION ALL
SELECT n.id, sc.id, 1, 2.50, 2, 'MAYORIA',
    'BAJO: 2 habilidades Apropiadas y 2 estructuras'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Piramides';

-- Dificultad de Lanzamientos
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, descripcion_rubrica)
SELECT n.id, sc.id, 3, 2.00, 1, 'MAYORIA',
    'MAYORÍA realiza en la MISMA SECCIÓN un lanzamiento Apropiado sincronizado/canon'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Lanzamientos'
UNION ALL
SELECT n.id, sc.id, 2, 1.50, 1, 'MAYORIA',
    'MAYORÍA realiza un lanzamiento Apropiado del nivel'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Lanzamientos'
UNION ALL
SELECT n.id, sc.id, 1, 1.00, 0, 'MAYORIA',
    'Menos de la MAYORÍA realiza un lanzamiento'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Lanzamientos';

-- Dificultad de Saltos
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, descripcion_rubrica)
SELECT n.id, sc.id, 4, 2.00, 3, 'MAXIMO',
    'MÁXIMO realiza 3 saltos avanzados conectados o 2 conectados + 1 adicional (sincronizados, con variedad)'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Saltos'
UNION ALL
SELECT n.id, sc.id, 3, 1.50, 2, 'GRAN_PARTE',
    'GRAN PARTE realiza 2 saltos avanzados conectados (sincronizados, con variedad)'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Saltos'
UNION ALL
SELECT n.id, sc.id, 2, 1.00, 1, 'GRAN_PARTE',
    'GRAN PARTE realiza 1 salto avanzado'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Saltos'
UNION ALL
SELECT n.id, sc.id, 1, 0.50, 0, 'MAYORIA',
    'No alcanza el requerimiento de 1.0'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dificultad de Saltos';

-- Dificultad de Gimnasia Estática y con Carrera (escalones idénticos)
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, descripcion_rubrica)
SELECT n.id, sc.id, 3, 3.00, 1, 'GRAN_PARTE',
    'GRAN PARTE realiza 1 habilidad/pase Apropiado del nivel'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2')
  AND sc.nombre IN ('Dificultad de Gimnasia Estatica','Dificultad de Gimnasia con Carrera')
UNION ALL
SELECT n.id, sc.id, 2, 2.50, 1, 'MAYORIA',
    'MAYORÍA realiza 1 habilidad/pase apropiado del nivel'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2')
  AND sc.nombre IN ('Dificultad de Gimnasia Estatica','Dificultad de Gimnasia con Carrera')
UNION ALL
SELECT n.id, sc.id, 1, 2.00, 1, 'MAYORIA',
    'Menos de la MAYORÍA realiza 1 habilidad/pase apropiado del nivel'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2')
  AND sc.nombre IN ('Dificultad de Gimnasia Estatica','Dificultad de Gimnasia con Carrera');

-- Rango Ejecución (valor inicial — usado por CalculadoraValorInicialMenosDrivers)
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, descripcion_rubrica)
SELECT n.id, sc.id, 1, 4.00, NULL, NULL, 'Valor inicial Ejecucion Elevaciones/Piramides/Gimnasia'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2')
  AND sc.nombre = 'Ejecucion Elevaciones Piramides Gimnasia'
UNION ALL
SELECT n.id, sc.id, 1, 2.00, NULL, NULL, 'Valor inicial Ejecucion Lanzamientos/Saltos'
FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2')
  AND sc.nombre = 'Ejecucion Lanzamientos Saltos';

-- Rangos General (min y max para CalculadoraRangoDirectoJuez)
INSERT INTO tope_seccion_nivel (nivel_id, sub_criterio_id, orden, valor_puntaje,
    condicion_habilidades_diferentes, condicion_grupo_minimo, descripcion_rubrica)
-- Formaciones y Transiciones: 1.0 - 2.0
SELECT n.id, sc.id, 1, 1.00, NULL, NULL, 'Minimo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Formaciones y Transiciones'
UNION ALL
SELECT n.id, sc.id, 2, 2.00, NULL, NULL, 'Maximo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Formaciones y Transiciones'
-- Creatividad de Rutina: 1.5 - 2.0
UNION ALL
SELECT n.id, sc.id, 1, 1.50, NULL, NULL, 'Minimo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Creatividad de Rutina'
UNION ALL
SELECT n.id, sc.id, 2, 2.00, NULL, NULL, 'Maximo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Creatividad de Rutina'
-- Dance Dificultad Elementos: 0.5 - 1.0
UNION ALL
SELECT n.id, sc.id, 1, 0.50, NULL, NULL, 'Minimo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dance Dificultad Elementos'
UNION ALL
SELECT n.id, sc.id, 2, 1.00, NULL, NULL, 'Maximo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dance Dificultad Elementos'
-- Dance Ejecución: 0.5 - 1.0
UNION ALL
SELECT n.id, sc.id, 1, 0.50, NULL, NULL, 'Minimo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dance Ejecucion'
UNION ALL
SELECT n.id, sc.id, 2, 1.00, NULL, NULL, 'Maximo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Dance Ejecucion'
-- Showmanship: 1.0 - 2.0
UNION ALL
SELECT n.id, sc.id, 1, 1.00, NULL, NULL, 'Minimo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Showmanship'
UNION ALL
SELECT n.id, sc.id, 2, 2.00, NULL, NULL, 'Maximo' FROM niveles_competencia n, sub_criterios_rubrica sc
WHERE n.nombre IN ('N1','N2','N3','N4','N4.2') AND sc.nombre = 'Showmanship';

-- ── 5. DRIVERS ────────────────────────────────────────────────
-- Grado de Dificultad y Participación Máxima para Elevaciones y Pirámides.
INSERT INTO drivers_sub_criterio (sub_criterio_id, nombre)
SELECT sc.id, 'Grado de Dificultad'  FROM sub_criterios_rubrica sc WHERE sc.nombre IN ('Dificultad de Elevaciones','Dificultad de Piramides','Dificultad de Gimnasia Estatica','Dificultad de Gimnasia con Carrera')
UNION ALL
SELECT sc.id, 'Participacion Maxima' FROM sub_criterios_rubrica sc WHERE sc.nombre IN ('Dificultad de Elevaciones','Dificultad de Piramides','Dificultad de Gimnasia con Carrera');

-- Topes de drivers para N1-N4.2 (Élite):
-- Elevaciones/Pirámides: Grado de Dificultad 0-0.8, Participación Máxima 0-0.7
-- Gimnasia Estática:     Grado de Dificultad 0-1.0 (sin Participación Máxima)
-- Gimnasia con Carrera:  Grado de Dificultad 0-0.5, Participación Máxima 0-0.5

INSERT INTO tope_driver_nivel (driver_id, nivel_id, tope_maximo, valor_nivel_basico, valor_nivel_alto)
-- Grado de Dificultad Elevaciones/Pirámides: tope 0.8, basico=0.1, alto=0.2
SELECT d.id, n.id, 0.80, 0.10, 0.20
FROM drivers_sub_criterio d
JOIN sub_criterios_rubrica sc ON d.sub_criterio_id = sc.id
JOIN niveles_competencia n ON n.nombre IN ('N1','N2','N3','N4','N4.2')
WHERE d.nombre = 'Grado de Dificultad'
  AND sc.nombre IN ('Dificultad de Elevaciones','Dificultad de Piramides')
UNION ALL
-- Participación Máxima Elevaciones/Pirámides: tope 0.7 (escalones 0.3/0.5/0.7)
SELECT d.id, n.id, 0.70, NULL, NULL
FROM drivers_sub_criterio d
JOIN sub_criterios_rubrica sc ON d.sub_criterio_id = sc.id
JOIN niveles_competencia n ON n.nombre IN ('N1','N2','N3','N4','N4.2')
WHERE d.nombre = 'Participacion Maxima'
  AND sc.nombre IN ('Dificultad de Elevaciones','Dificultad de Piramides')
UNION ALL
-- Grado de Dificultad Gimnasia Estática: tope 1.0, basico=0.2, alto=0.4 (habilidad 1) / 0.4-0.6 (habilidad 2)
-- Nota: la rúbrica diferencia el valor según número de habilidad. Usamos basico=0.2, alto=0.4
-- para la primera; basico=0.4, alto=0.6 para la segunda. Esto lo maneja la calculadora
-- en función de numeroHabilidad. Aquí ponemos los valores del primer escalón como referencia;
-- para la habilidad 2 se puede cargar un segundo registro de driver con nombre distinguible,
-- o la calculadora puede leer el número de habilidad y aplicar la lógica correcta.
-- Por ahora: 1 fila de tope con los valores de habilidad 1 (revisión futura si se necesita más granularidad).
SELECT d.id, n.id, 1.00, 0.20, 0.40
FROM drivers_sub_criterio d
JOIN sub_criterios_rubrica sc ON d.sub_criterio_id = sc.id
JOIN niveles_competencia n ON n.nombre IN ('N1','N2','N3','N4','N4.2')
WHERE d.nombre = 'Grado de Dificultad'
  AND sc.nombre = 'Dificultad de Gimnasia Estatica'
UNION ALL
-- Grado de Dificultad Gimnasia con Carrera: tope 0.5, basico=0.3, alto=0.5
SELECT d.id, n.id, 0.50, 0.30, 0.50
FROM drivers_sub_criterio d
JOIN sub_criterios_rubrica sc ON d.sub_criterio_id = sc.id
JOIN niveles_competencia n ON n.nombre IN ('N1','N2','N3','N4','N4.2')
WHERE d.nombre = 'Grado de Dificultad'
  AND sc.nombre = 'Dificultad de Gimnasia con Carrera'
UNION ALL
-- Participación Máxima Gimnasia con Carrera: tope 0.5, basico=0.3, alto=0.5
SELECT d.id, n.id, 0.50, 0.30, 0.50
FROM drivers_sub_criterio d
JOIN sub_criterios_rubrica sc ON d.sub_criterio_id = sc.id
JOIN niveles_competencia n ON n.nombre IN ('N1','N2','N3','N4','N4.2')
WHERE d.nombre = 'Participacion Maxima'
  AND sc.nombre = 'Dificultad de Gimnasia con Carrera';

-- Escalones de Participación Máxima para Elevaciones y Pirámides
-- Fuente: "PARTICIPACIÓN MÁXIMA (0-0.7)" — Rúbrica Élite N1-N4 2026
INSERT INTO escalon_driver_nivel (driver_id, nivel_id, orden, descripcion, valor)
SELECT d.id, n.id, 1,
    'Habilidad Apropiada del nivel por MAXIMO o Avanzada por GRAN PARTE', 0.30
FROM drivers_sub_criterio d
JOIN sub_criterios_rubrica sc ON d.sub_criterio_id = sc.id
JOIN niveles_competencia n ON n.nombre IN ('N1','N2','N3','N4','N4.2')
WHERE d.nombre = 'Participacion Maxima'
  AND sc.nombre IN ('Dificultad de Elevaciones','Dificultad de Piramides')
UNION ALL
SELECT d.id, n.id, 2,
    'Habilidad Avanzada por MAXIMO o Elite por GRAN PARTE', 0.50
FROM drivers_sub_criterio d
JOIN sub_criterios_rubrica sc ON d.sub_criterio_id = sc.id
JOIN niveles_competencia n ON n.nombre IN ('N1','N2','N3','N4','N4.2')
WHERE d.nombre = 'Participacion Maxima'
  AND sc.nombre IN ('Dificultad de Elevaciones','Dificultad de Piramides')
UNION ALL
SELECT d.id, n.id, 3,
    'Habilidad Elite por MAXIMO', 0.70
FROM drivers_sub_criterio d
JOIN sub_criterios_rubrica sc ON d.sub_criterio_id = sc.id
JOIN niveles_competencia n ON n.nombre IN ('N1','N2','N3','N4','N4.2')
WHERE d.nombre = 'Participacion Maxima'
  AND sc.nombre IN ('Dificultad de Elevaciones','Dificultad de Piramides');

-- =============================================================================
-- FIN DEL SCRIPT DE DATOS
-- =============================================================================