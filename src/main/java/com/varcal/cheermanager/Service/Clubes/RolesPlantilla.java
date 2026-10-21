package com.varcal.cheermanager.Service.Clubes;

import java.util.Set;

/**
 * Roles que se crean automaticamente al dar de alta un club, y que permisos reciben.
 * El club puede luego ajustarlos o crear los suyos (los roles son por club).
 */
public final class RolesPlantilla {

    private RolesPlantilla() {
    }

    /** Nombres IGUALES a los del script de datos (roles de Ratels): un club nuevo usa los mismos. */
    public static final String ADMIN = "Administrador";
    public static final String ENTRENADOR = "Entrenador";
    public static final String DEPORTISTA = "Deportista";
    public static final String SUPERADMIN = "Super Administrador";

    /** Permisos que SOLO tiene la plataforma (catalogos globales, rubrica global, gestion de clubes). */
    public static final Set<String> SOLO_PLATAFORMA = Set.of(
            "gestionar_clubes", "gestionar_catalogos", "configurar_rubrica",
            // reservados al Super Administrador segun el script de datos (hoy sin endpoint que los use)
            "administrar_roles", "administrar_permisos", "configurar_sistema", "gestionar_parametros",
            "importar_datos", "exportar_datos", "ver_dashboard_global", "ver_logs_sistema");

    /**
     * Entrenador: opera el dia a dia (asistencia, evaluaciones, consulta de atletas/grupos).
     * Deliberadamente SIN historial medico ni finanzas: son datos sensibles (Ley 1581 de 2012);
     * el admin del club puede concederlos por rol si decide hacerlo.
     */
    public static final Set<String> ENTRENADOR_PERMISOS = Set.of(
            "ver_deportista", "ver_deportistas", "ver_persona", "ver_entrenador",
            "ver_grupo", "ver_horario", "ver_asistencia", "registrar_asistencia",
            "ver_evaluacion", "crear_evaluacion", "crear_evaluacion_rutina",
            "ver_objetivo", "ver_objetivos", "crear_objetivo", "modificar_objetivo",
            "ver_nivel", "ver_categoria_nivel", "ver_evento", "ver_acudientes", "ver_temporadas",
            "ver_dash_inicio", "ver_dash_horarios", "ver_dash_evaluacion");
}
