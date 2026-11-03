package com.varcal.cheermanager.Service.Clubes;

import java.util.Set;

/**
 * Roles que se crean automaticamente al dar de alta un club, y que permisos reciben.
 * El club puede luego ajustarlos o crear los suyos (los roles son por club).
 *
 * TODOS los roles de plantilla (Entrenador, Director, Coordinador, Asistente, Tesorero e Inventario) tienen los mismos
 * permisos que los roles de Ratels (V2 + V8 + V9 + V10 + V12), para que un club nuevo nazca con el mismo menu y las
 * mismas pantallas. Decision de Leandro (31-oct-2026): sin restricciones adicionales por rol.
 * RolesPlantillaTest comprueba que cada permiso exista en el catalogo y que no se pidan permisos de plataforma.
 */
public final class RolesPlantilla {

    private RolesPlantilla() {
    }

    /** Nombres IGUALES a los del script de datos (roles de Ratels): un club nuevo usa los mismos. */
    public static final String ADMIN = "Administrador";
    public static final String ENTRENADOR = "Entrenador";
    public static final String DEPORTISTA = "Deportista";
    public static final String ACUDIENTE = "Acudiente";
    public static final String DIRECTOR = "Director Deportivo";
    public static final String COORDINADOR = "Coordinador Administrativo";
    public static final String ASISTENTE = "Asistente";
    public static final String TESORERO = "Tesorero";
    public static final String INVENTARIO = "Encargado de Inventario";
    /** Acceso al portal de solo lectura (cada usuario ve unicamente lo suyo / de sus deportistas). */
    public static final String PERMISO_PORTAL = "acceder_portal";
    public static final String SUPERADMIN = "Super Administrador";

    /** Permisos que SOLO tiene la plataforma (catalogos globales, rubrica global, gestion de clubes). */
    public static final Set<String> SOLO_PLATAFORMA = Set.of(
            "gestionar_clubes", "gestionar_catalogos", "configurar_rubrica",
            // reservados al Super Administrador segun el script de datos (hoy sin endpoint que los use)
            "administrar_roles", "administrar_permisos", "configurar_sistema", "gestionar_parametros",
            "importar_datos", "exportar_datos", "ver_dashboard_global", "ver_logs_sistema");

    /**
     * Entrenador: opera el dia a dia. Igual al rol Entrenador de Ratels (V2 + V10), incluido el historial medico.
     */
    public static final Set<String> ENTRENADOR_PERMISOS = Set.of(
            "asignar_categoria_nivel", "asignar_grupo_evento", "asignar_horario_grupo", "cambiar_estado_objetivo",
            "cambiar_nivel_deportista", "crear_deportista", "crear_evaluacion", "crear_evaluacion_rutina",
            "crear_horario", "crear_notificacion", "crear_objetivo", "eliminar_asistencia",
            "generar_reporte_asistencia", "generar_reporte_evaluacion", "modificar_asistencia",
            "modificar_deportista", "modificar_evaluacion", "modificar_evento", "modificar_horario",
            "modificar_objetivo", "registrar_asistencia", "registrar_asistencia_evento", "registrar_evaluacion",
            "ver_acudientes", "ver_asistencia", "ver_categoria_nivel", "ver_dash_evaluacion", "ver_dash_horarios",
            "ver_dash_inicio", "ver_dash_organizacion", "ver_dash_personas", "ver_dashboard", "ver_deportista",
            "ver_deportistas", "ver_evaluacion", "ver_evaluaciones", "ver_evento", "ver_grupo",
            "ver_historial_evaluacion_deportista", "ver_historial_medico", "ver_historial_nivel_deportista",
            "ver_horario", "ver_mi_perfil", "ver_nivel", "ver_notificacion", "ver_objetivo", "ver_objetivos",
            "ver_permisos", "ver_temporadas");

    /**
     * Director Deportivo: supervisa entrenadores, grupos, horarios, eventos y evaluaciones. Igual que en Ratels.
     */
    public static final Set<String> DIRECTOR_PERMISOS = Set.of(
            "asignar_a_grupo", "asignar_entrenador_grupo", "asignar_entrenador_horario", "asignar_grupo_evento",
            "asignar_horario_grupo", "cambiar_estado_objetivo", "cambiar_nivel_deportista",
            "crear_criterio_evaluacion", "crear_deportista", "crear_entrenador", "crear_evaluacion",
            "crear_evaluacion_rutina", "crear_evento", "crear_grupo", "crear_grupo_entrenamiento", "crear_horario",
            "crear_objetivo", "eliminar_asistencia", "modificar_asistencia", "modificar_criterio_evaluacion",
            "modificar_deportista", "modificar_entrenador", "modificar_evaluacion", "modificar_evento",
            "modificar_grupo", "modificar_grupo_entrenamiento", "modificar_horario", "modificar_objetivo",
            "publicar_resultados", "registrar_asistencia", "registrar_asistencia_evento", "registrar_evaluacion",
            "registrar_resultado_competencia", "retirar_de_grupo", "ver_asistencia", "ver_dash_evaluacion",
            "ver_dash_eventos", "ver_dash_horarios", "ver_dash_inicio", "ver_dash_organizacion", "ver_dash_personas",
            "ver_dashboard", "ver_deportista", "ver_deportistas", "ver_entrenador", "ver_evaluacion",
            "ver_evaluaciones", "ver_evento", "ver_grupo", "ver_grupo_entrenamiento", "ver_horario", "ver_nivel",
            "ver_objetivo", "ver_objetivos");

    /**
     * Coordinador Administrativo: personas, inscripciones, horarios, eventos y panel de finanzas (ver_dash_finanzas,
     * agregado a Ratels en V12). Igual que en Ratels.
     */
    public static final Set<String> COORDINADOR_PERMISOS = Set.of(
            "asignar_a_grupo", "asignar_entrenador_grupo", "asignar_entrenador_horario", "asignar_grupo_evento",
            "asignar_horario_grupo", "cambiar_estado_objetivo", "cambiar_nivel_deportista",
            "crear_criterio_evaluacion", "crear_deportista", "crear_evaluacion", "crear_evaluacion_rutina",
            "crear_evento", "crear_horario", "crear_objetivo", "crear_persona", "eliminar_asistencia",
            "gestionar_inscripciones", "gestionar_recordatorios_mora", "modificar_asistencia",
            "modificar_deportista", "modificar_evaluacion", "modificar_evento", "modificar_horario",
            "modificar_inscripcion", "modificar_objetivo", "modificar_persona", "publicar_resultados",
            "registrar_asistencia", "registrar_asistencia_evento", "registrar_evaluacion",
            "registrar_resultado_competencia", "retirar_de_grupo", "ver_asistencia", "ver_dash_evaluacion",
            "ver_dash_eventos", "ver_dash_finanzas", "ver_dash_horarios", "ver_dash_inicio", "ver_dash_organizacion",
            "ver_dash_personas", "ver_dashboard", "ver_deportista", "ver_deportistas", "ver_entrenador",
            "ver_evaluacion", "ver_evaluaciones", "ver_evento", "ver_grupo", "ver_grupo_entrenamiento",
            "ver_horario", "ver_inscripcion", "ver_nivel", "ver_objetivo", "ver_objetivos", "ver_persona");

    /**
     * Asistente del entrenador: asistencia y consulta. Igual que en Ratels, incluido el historial medico.
     */
    public static final Set<String> ASISTENTE_PERMISOS = Set.of(
            "asignar_a_grupo", "asignar_grupo_evento", "cambiar_estado_objetivo", "crear_notificacion",
            "eliminar_asistencia", "modificar_asistencia", "modificar_evaluacion", "modificar_objetivo",
            "registrar_asistencia", "registrar_asistencia_evento", "retirar_de_grupo", "ver_asistencia",
            "ver_categoria_nivel", "ver_dash_evaluacion", "ver_dash_horarios", "ver_dash_inicio",
            "ver_dash_organizacion", "ver_dash_personas", "ver_dashboard", "ver_deportista", "ver_deportistas",
            "ver_evaluacion", "ver_evaluaciones", "ver_evento", "ver_grupo", "ver_grupo_entrenamiento",
            "ver_historial_evaluacion_deportista", "ver_historial_medico", "ver_historial_nivel_deportista",
            "ver_horario", "ver_mi_perfil", "ver_nivel", "ver_notificacion", "ver_objetivo", "ver_objetivos",
            "ver_permisos");

    /**
     * Tesorero: facturas, pagos, ventas, planes, convenios, descuentos y recordatorios de mora. Igual que en Ratels.
     */
    public static final Set<String> TESORERO_PERMISOS = Set.of(
            "anular_factura", "anular_pago", "anular_venta", "asignar_descuento_factura",
            "asignar_destinatario_notificacion", "crear_convenio", "crear_descuento", "crear_factura",
            "crear_notificacion", "crear_plan_pago", "eliminar_convenio", "eliminar_descuento", "eliminar_plan_pago",
            "generar_reporte_financiero", "gestionar_convenio", "gestionar_descuento", "gestionar_plan_pago",
            "gestionar_recordatorios_mora", "modificar_convenio", "modificar_descuento", "modificar_factura",
            "modificar_pago", "modificar_plan_pago", "modificar_venta", "registrar_pago", "registrar_venta",
            "ver_auditoria_descuentos_convenios", "ver_auditoria_financiera", "ver_convenio", "ver_dash_eventos",
            "ver_dash_finanzas", "ver_dash_tienda", "ver_dashboard", "ver_descuento", "ver_evento", "ver_factura",
            "ver_mi_perfil", "ver_notificacion", "ver_pago", "ver_permisos", "ver_plan_pago", "ver_producto",
            "ver_venta", "ver_caja", "cerrar_caja", "conciliar_pagos");

    /**
     * Encargado de Inventario: productos, movimientos y ventas de tienda. Igual que en Ratels.
     */
    public static final Set<String> INVENTARIO_PERMISOS = Set.of(
            "anular_venta", "crear_producto", "eliminar_producto", "modificar_producto", "modificar_venta",
            "registrar_movimiento_inventario", "registrar_venta", "ver_dash_inicio", "ver_dash_tienda",
            "ver_dashboard", "ver_mi_perfil", "ver_movimiento_inventario", "ver_notificacion", "ver_permisos",
            "ver_producto", "ver_venta");

    /** Rol adicional de plantilla: nombre, descripcion y permisos. */
    public record RolBase(String nombre, String descripcion, Set<String> permisos) {
    }

    /** Roles operativos adicionales que se crean en cada club nuevo (ademas de Administrador, Entrenador, Deportista y Acudiente). */
    public static final java.util.List<RolBase> ROLES_ADICIONALES = java.util.List.of(
            new RolBase(DIRECTOR, "Supervisa entrenadores, equipos, categorias, evaluaciones y organizacion deportiva", DIRECTOR_PERMISOS),
            new RolBase(COORDINADOR, "Gestiona personas, inscripciones, horarios y organizacion administrativa", COORDINADOR_PERMISOS),
            new RolBase(ASISTENTE, "Asistente del entrenador con permisos operativos limitados", ASISTENTE_PERMISOS),
            new RolBase(TESORERO, "Responsable de finanzas, pagos, facturacion y planes de pago", TESORERO_PERMISOS),
            new RolBase(INVENTARIO, "Gestiona productos, inventario, movimientos y ventas", INVENTARIO_PERMISOS));
}
