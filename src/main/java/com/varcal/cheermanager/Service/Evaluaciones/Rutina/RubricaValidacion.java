package com.varcal.cheermanager.Service.Evaluaciones.Rutina;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Reglas puras de la configuracion de la rubrica (sin base de datos). La rubrica es GLOBAL: lo que se guarda aqui
 * lo usan todos los clubes, asi que un dato absurdo (rango invertido, filas que se pisan) dana la evaluacion de todos.
 */
public final class RubricaValidacion {

    public static final Set<String> TABLAS = Set.of("Construcciones", "Saltos_Gimnasia");
    public static final Set<String> GRUPOS_MINIMOS = Set.of("MENOS_MAYORIA", "MAYORIA", "GRAN_PARTE", "MAXIMO");
    public static final Set<String> SECCIONES = Set.of("Construcciones", "Gimnasia", "Ejecucion", "General");
    public static final BigDecimal PUNTAJE_MAXIMO_ESCALON = new BigDecimal("10");

    /** Rango ya guardado, para comprobar solapes: {id, min, max}. */
    public record RangoExistente(Integer id, int min, int max) {
    }

    private RubricaValidacion() {
    }

    private static void requerido(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(nombre + " es obligatorio");
        }
    }

    public static String nombreNivel(String nombre, String modalidad) {
        requerido(nombre, "El nombre del nivel");
        requerido(modalidad, "La modalidad");
        String n = nombre.trim();
        if (n.length() > 50 || modalidad.trim().length() > 50) {
            throw new IllegalArgumentException("El nombre y la modalidad no pueden pasar de 50 caracteres");
        }
        return n;
    }

    /** La tabla de Construcciones cuenta grupos; la de Saltos/Gimnasia cuenta atletas. */
    public static String unidadEsperada(String tabla) {
        return "Construcciones".equals(tabla) ? "grupos" : "atletas";
    }

    public static void tablaCantidad(String tabla, String unidad, Integer min, Integer max, Integer mayoria,
            Integer granParte, Integer maximo, List<RangoExistente> otrosRangos) {
        if (tabla == null || !TABLAS.contains(tabla)) {
            throw new IllegalArgumentException("La tabla debe ser una de: " + String.join(", ", new java.util.TreeSet<>(TABLAS)));
        }
        if (!unidadEsperada(tabla).equals(unidad)) {
            throw new IllegalArgumentException("La tabla " + tabla + " usa la unidad '" + unidadEsperada(tabla) + "'");
        }
        if (min == null || max == null || min < 1 || max < min) {
            throw new IllegalArgumentException("El rango de atletas debe ser un minimo de 1 o mas y un maximo no menor al minimo");
        }
        if (mayoria == null || granParte == null || maximo == null || mayoria < 1) {
            throw new IllegalArgumentException("Mayoria, gran parte y maximo son obligatorios y mayores a cero");
        }
        if (mayoria > granParte || granParte > maximo) {
            throw new IllegalArgumentException("Debe cumplirse: mayoria <= gran parte <= maximo");
        }
        if (otrosRangos != null) {
            for (RangoExistente r : otrosRangos) {
                if (min <= r.max() && max >= r.min()) {
                    throw new IllegalArgumentException("El rango " + min + "-" + max + " se cruza con el rango " + r.min()
                            + "-" + r.max() + " ya configurado para este nivel y tabla");
                }
            }
        }
    }

    public static void topeSeccion(Integer orden, BigDecimal valor, Integer habilidades, String grupoMinimo,
            String descripcion, List<Integer> otrosOrdenes) {
        if (orden == null || orden < 1) {
            throw new IllegalArgumentException("El orden del escalon debe ser 1 o mayor");
        }
        if (valor == null || valor.signum() < 0 || valor.compareTo(PUNTAJE_MAXIMO_ESCALON) > 0) {
            throw new IllegalArgumentException("El valor del escalon debe estar entre 0 y " + PUNTAJE_MAXIMO_ESCALON);
        }
        if (habilidades != null && habilidades < 1) {
            throw new IllegalArgumentException("Las habilidades diferentes deben ser 1 o mas (o dejarlas vacias)");
        }
        if (grupoMinimo != null && !grupoMinimo.isBlank() && !GRUPOS_MINIMOS.contains(grupoMinimo)) {
            throw new IllegalArgumentException("El grupo minimo debe ser MENOS_MAYORIA, MAYORIA, GRAN_PARTE o MAXIMO (o quedar vacio)");
        }
        if (descripcion != null && descripcion.length() > 500) {
            throw new IllegalArgumentException("La descripcion no puede pasar de 500 caracteres");
        }
        if (otrosOrdenes != null && otrosOrdenes.contains(orden)) {
            throw new IllegalArgumentException("Ya existe un escalon con orden " + orden + " para este nivel y sub-criterio");
        }
    }

    public static void subCriterio(String nombre, String seccion, Object tipoCalculo) {
        requerido(nombre, "El nombre del sub-criterio");
        if (seccion == null || !SECCIONES.contains(seccion)) {
            throw new IllegalArgumentException("La seccion debe ser una de: Construcciones, Ejecucion, Gimnasia, General");
        }
        if (tipoCalculo == null) {
            throw new IllegalArgumentException("El tipo de calculo es obligatorio");
        }
    }
}
