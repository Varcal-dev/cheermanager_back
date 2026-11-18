package com.varcal.cheermanager.Service.Org_dep;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Reglas para que un deportista este en varios grupos de entrenamiento a la vez:
 * (1) las sesiones por semana de todos sus grupos no pueden superar las del plan de mensualidad, y
 * (2) los horarios de los grupos no se pueden cruzar. Clase pura (sin base de datos) para poder probarla.
 */
public final class PlanGruposReglas {

    private PlanGruposReglas() {
    }

    /** Un bloque de entrenamiento de un grupo: dia de la semana y rango de horas. */
    public record Franja(int diaId, String dia, LocalTime inicio, LocalTime fin) {
    }

    /** Un grupo con su nombre y sus bloques de entrenamiento. */
    public record GrupoConFranjas(Integer grupoId, String nombre, List<Franja> franjas) {
        public int sesionesSemanales() {
            Set<Integer> dias = new HashSet<>();
            for (Franja f : franjas) {
                dias.add(f.diaId());
            }
            return dias.size();
        }
    }

    /** Dos franjas se cruzan si son el mismo dia y sus horas se pisan (que una termine cuando empieza la otra no cuenta). */
    public static boolean solapan(Franja a, Franja b) {
        if (a.diaId() != b.diaId() || a.inicio() == null || a.fin() == null || b.inicio() == null || b.fin() == null) {
            return false;
        }
        return a.inicio().isBefore(b.fin()) && b.inicio().isBefore(a.fin());
    }

    /**
     * Motivo por el que el deportista NO puede entrar al grupo {@code nuevo}, o null si puede.
     *
     * @param sesionesPlan sesiones por semana que permite su plan de mensualidad (null = sin dato, no se limita)
     * @param actuales     grupos en los que ya esta
     */
    public static String motivoNoPuedeEntrar(Integer sesionesPlan, List<GrupoConFranjas> actuales, GrupoConFranjas nuevo) {
        for (GrupoConFranjas g : actuales) {
            if (g.grupoId() != null && g.grupoId().equals(nuevo.grupoId())) {
                return "Ya pertenece a este grupo";
            }
        }
        if (nuevo.franjas().isEmpty()) {
            return null; // grupo sin horarios todavia: no hay nada que cruzar ni contar
        }
        for (GrupoConFranjas g : actuales) {
            for (Franja a : g.franjas()) {
                for (Franja b : nuevo.franjas()) {
                    if (solapan(a, b)) {
                        return "Su horario se cruza con el grupo " + g.nombre() + " (" + a.dia() + " "
                                + hora(a.inicio()) + "–" + hora(a.fin()) + ")";
                    }
                }
            }
        }
        if (sesionesPlan != null) {
            int usadas = actuales.stream().mapToInt(GrupoConFranjas::sesionesSemanales).sum();
            int nuevas = nuevo.sesionesSemanales();
            if (usadas + nuevas > sesionesPlan) {
                return "Su plan de mensualidad permite " + sesionesPlan + (sesionesPlan == 1 ? " sesión" : " sesiones")
                        + " por semana; ya usa " + usadas + " y este grupo suma " + nuevas;
            }
        }
        return null;
    }

    private static String hora(LocalTime t) {
        return t == null ? "" : String.format("%02d:%02d", t.getHour(), t.getMinute());
    }
}
