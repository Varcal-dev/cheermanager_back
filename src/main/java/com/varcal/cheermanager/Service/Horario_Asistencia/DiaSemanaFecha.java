package com.varcal.cheermanager.Service.Horario_Asistencia;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Locale;

import com.varcal.cheermanager.Models.Org_dep.DiaSemana;

/** Une el dia de la semana de un horario (catalogo "Lunes".."Domingo") con una fecha del calendario. */
public final class DiaSemanaFecha {

    private DiaSemanaFecha() {
    }

    /** DayOfWeek que corresponde al nombre del catalogo ("Miércoles" o "miercoles"), o null si no se reconoce. */
    public static DayOfWeek deNombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        String n = Normalizer.normalize(nombre.trim(), Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        return switch (n) {
            case "lunes" -> DayOfWeek.MONDAY;
            case "martes" -> DayOfWeek.TUESDAY;
            case "miercoles" -> DayOfWeek.WEDNESDAY;
            case "jueves" -> DayOfWeek.THURSDAY;
            case "viernes" -> DayOfWeek.FRIDAY;
            case "sabado" -> DayOfWeek.SATURDAY;
            case "domingo" -> DayOfWeek.SUNDAY;
            default -> null;
        };
    }

    /** true si la fecha cae en el dia del horario. Un dia que no se reconoce no bloquea (devuelve true). */
    public static boolean coincide(DiaSemana dia, LocalDate fecha) {
        if (dia == null || fecha == null) {
            return true;
        }
        DayOfWeek esperado = deNombre(dia.getDia());
        return esperado == null || esperado == fecha.getDayOfWeek();
    }

    /** Mensaje para el usuario cuando la fecha no es un dia de entrenamiento del horario. */
    public static String mensajeNoCoincide(DiaSemana dia, LocalDate fecha) {
        String real = nombreDia(fecha.getDayOfWeek());
        return "El " + fecha + " es " + real + " y este horario entrena los " + dia.getDia().toLowerCase(Locale.ROOT)
                + ". Elige una fecha que caiga en " + dia.getDia().toLowerCase(Locale.ROOT) + ".";
    }

    private static String nombreDia(DayOfWeek d) {
        return switch (d) {
            case MONDAY -> "lunes";
            case TUESDAY -> "martes";
            case WEDNESDAY -> "miércoles";
            case THURSDAY -> "jueves";
            case FRIDAY -> "viernes";
            case SATURDAY -> "sábado";
            case SUNDAY -> "domingo";
        };
    }
}
