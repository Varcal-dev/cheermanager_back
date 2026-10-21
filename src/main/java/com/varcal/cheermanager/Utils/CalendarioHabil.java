package com.varcal.cheermanager.Utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.Set;

/**
 * Dias habiles de Colombia: lunes a viernes que no sean festivos (Ley 51 de 1983, "Ley Emiliani").
 * Se usa para fijar la emision de la factura mensual en el PRIMER DIA HABIL del mes.
 *
 * Sin dependencias: los festivos se calculan (fijos, trasladados al lunes siguiente y los que
 * dependen de la Pascua), no se guardan en una tabla.
 */
public final class CalendarioHabil {

    private CalendarioHabil() {
    }

    public static boolean esHabil(LocalDate fecha) {
        DayOfWeek d = fecha.getDayOfWeek();
        if (d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY) {
            return false;
        }
        return !festivos(fecha.getYear()).contains(fecha);
    }

    /** Primer dia habil del mes (nunca es null: siempre hay uno en los primeros dias del mes). */
    public static LocalDate primerDiaHabil(YearMonth mes) {
        LocalDate d = mes.atDay(1);
        while (!esHabil(d)) {
            d = d.plusDays(1);
        }
        return d;
    }

    /** Siguiente dia habil estrictamente posterior a la fecha dada. */
    public static LocalDate siguienteDiaHabil(LocalDate fecha) {
        LocalDate d = fecha.plusDays(1);
        while (!esHabil(d)) {
            d = d.plusDays(1);
        }
        return d;
    }

    public static Set<LocalDate> festivos(int anio) {
        Set<LocalDate> f = new HashSet<>();

        // Fijos
        f.add(LocalDate.of(anio, 1, 1));   // Año Nuevo
        f.add(LocalDate.of(anio, 5, 1));   // Dia del Trabajo
        f.add(LocalDate.of(anio, 7, 20));  // Independencia
        f.add(LocalDate.of(anio, 8, 7));   // Batalla de Boyaca
        f.add(LocalDate.of(anio, 12, 8));  // Inmaculada Concepcion
        f.add(LocalDate.of(anio, 12, 25)); // Navidad

        // Trasladados al lunes siguiente (si ya cae lunes, se queda)
        f.add(aLunes(LocalDate.of(anio, 1, 6)));   // Reyes Magos
        f.add(aLunes(LocalDate.of(anio, 3, 19)));  // San Jose
        f.add(aLunes(LocalDate.of(anio, 6, 29)));  // San Pedro y San Pablo
        f.add(aLunes(LocalDate.of(anio, 8, 15)));  // Asuncion de la Virgen
        f.add(aLunes(LocalDate.of(anio, 10, 12))); // Dia de la Raza
        f.add(aLunes(LocalDate.of(anio, 11, 1)));  // Todos los Santos
        f.add(aLunes(LocalDate.of(anio, 11, 11))); // Independencia de Cartagena

        // Dependen de la Pascua
        LocalDate pascua = domingoDePascua(anio);
        f.add(pascua.minusDays(3));            // Jueves Santo
        f.add(pascua.minusDays(2));            // Viernes Santo
        f.add(aLunes(pascua.plusDays(39)));    // Ascension del Señor
        f.add(aLunes(pascua.plusDays(60)));    // Corpus Christi
        f.add(aLunes(pascua.plusDays(68)));    // Sagrado Corazon
        return f;
    }

    private static LocalDate aLunes(LocalDate d) {
        return d.with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    /** Algoritmo gregoriano anonimo (Meeus/Jones/Butcher). */
    static LocalDate domingoDePascua(int anio) {
        int a = anio % 19;
        int b = anio / 100;
        int c = anio % 100;
        int d = b / 4;
        int e = b % 4;
        int g = (8 * b + 13) / 25;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(anio, mes, dia);
    }
}
