package com.varcal.cheermanager.Service.Legal;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/** Dias habiles de Colombia: lunes a viernes sin festivos (Ley 51 de 1983, "Ley Emiliani", y Semana Santa). */
public final class DiasHabiles {

    private DiasHabiles() {
    }

    public static Set<LocalDate> festivos(int anio) {
        Set<LocalDate> f = new HashSet<>();
        // Fijos
        for (int[] md : new int[][] { { 1, 1 }, { 5, 1 }, { 7, 20 }, { 8, 7 }, { 12, 8 }, { 12, 25 } }) {
            f.add(LocalDate.of(anio, md[0], md[1]));
        }
        // Trasladados al lunes siguiente
        for (int[] md : new int[][] { { 1, 6 }, { 3, 19 }, { 6, 29 }, { 8, 15 }, { 10, 12 }, { 11, 1 }, { 11, 11 } }) {
            f.add(alLunes(LocalDate.of(anio, md[0], md[1])));
        }
        // Dependen de la Pascua
        LocalDate pascua = pascua(anio);
        f.add(pascua.minusDays(3)); // jueves santo
        f.add(pascua.minusDays(2)); // viernes santo
        f.add(alLunes(pascua.plusDays(39))); // Ascension
        f.add(alLunes(pascua.plusDays(60))); // Corpus Christi
        f.add(alLunes(pascua.plusDays(68))); // Sagrado Corazon
        return f;
    }

    static LocalDate alLunes(LocalDate d) {
        int resto = (8 - d.getDayOfWeek().getValue()) % 7; // dias hasta el proximo lunes (0 si ya es lunes)
        return d.plusDays(resto);
    }

    /** Domingo de Pascua (algoritmo gregoriano anonimo). */
    static LocalDate pascua(int y) {
        int a = y % 19, b = y / 100, c = y % 100, d = b / 4, e = b % 4, f = (b + 8) / 25, g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30, i = c / 4, k = c % 4, l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31, dia = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(y, mes, dia);
    }

    public static boolean esHabil(LocalDate d) {
        DayOfWeek w = d.getDayOfWeek();
        return w != DayOfWeek.SATURDAY && w != DayOfWeek.SUNDAY && !festivos(d.getYear()).contains(d);
    }

    /** Fecha que resulta de contar n dias habiles DESPUES de la fecha dada (el dia de recepcion no cuenta). */
    public static LocalDate sumar(LocalDate desde, int n) {
        LocalDate d = desde;
        int faltan = n;
        while (faltan > 0) {
            d = d.plusDays(1);
            if (esHabil(d)) {
                faltan--;
            }
        }
        return d;
    }

    /** Dias habiles que faltan de hoy a la fecha limite (negativo si ya paso). */
    public static int restantes(LocalDate hoy, LocalDate limite) {
        int n = 0;
        if (!limite.isBefore(hoy)) {
            for (LocalDate d = hoy.plusDays(1); !d.isAfter(limite); d = d.plusDays(1)) {
                if (esHabil(d)) n++;
            }
            return n;
        }
        for (LocalDate d = limite.plusDays(1); !d.isAfter(hoy); d = d.plusDays(1)) {
            if (esHabil(d)) n--;
        }
        return n;
    }
}
