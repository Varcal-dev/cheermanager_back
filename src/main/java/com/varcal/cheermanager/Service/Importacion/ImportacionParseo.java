package com.varcal.cheermanager.Service.Importacion;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Conversiones puras (sin Spring ni base de datos) que usa la importacion de deportistas. */
final class ImportacionParseo {

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ofPattern("uuuu-M-d").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT));

    /** Nombres de columna alternativos que se aceptan (ya normalizados) -> nombre oficial. */
    static final Map<String, String> ALIAS_COLUMNAS = Map.ofEntries(
            Map.entry("nombres", "nombre"),
            Map.entry("apellido", "apellidos"),
            Map.entry("documento", "numero_documento"),
            Map.entry("no_documento", "numero_documento"),
            Map.entry("identificacion", "numero_documento"),
            Map.entry("cedula", "numero_documento"),
            Map.entry("fecha_de_nacimiento", "fecha_nacimiento"),
            Map.entry("nacimiento", "fecha_nacimiento"),
            Map.entry("sexo", "genero"),
            Map.entry("celular", "telefono"),
            Map.entry("acudiente_celular", "acudiente_telefono"),
            Map.entry("acudiente_correo", "acudiente_email"),
            Map.entry("acudiente_parentezco", "acudiente_parentesco"),
            Map.entry("parentesco", "acudiente_parentesco"),
            Map.entry("acudiente_numero_documento", "acudiente_documento"),
            Map.entry("acudiente_cedula", "acudiente_documento"));

    private ImportacionParseo() {
    }

    /** minusculas, sin tildes, y todo lo que no sea letra o numero pasa a "_" (sin "_" en los extremos). */
    static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        String n = sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        return n.replaceAll("^_+|_+$", "");
    }

    static String nombreColumna(String encabezado) {
        String n = normalizar(encabezado);
        return ALIAS_COLUMNAS.getOrDefault(n, n);
    }

    /** Quita espacios, puntos y comas: "1.006.123.456" -> "1006123456". Vacio -> null. */
    static String documento(String s) {
        if (s == null) {
            return null;
        }
        String d = s.replaceAll("[\\s.,]", "");
        return d.isEmpty() ? null : d;
    }

    /** Acepta 2012-03-25, 25/03/2012 y 25-03-2012. Lanza IllegalArgumentException con un mensaje legible. */
    static LocalDate fecha(String s, LocalDate hoy) {
        String t = s.trim();
        for (DateTimeFormatter f : FORMATOS_FECHA) {
            try {
                LocalDate d = LocalDate.parse(t, f);
                if (d.isAfter(hoy)) {
                    throw new IllegalArgumentException("La fecha de nacimiento '" + t + "' es futura");
                }
                if (d.getYear() < 1930) {
                    throw new IllegalArgumentException("La fecha de nacimiento '" + t + "' no es valida");
                }
                return d;
            } catch (DateTimeParseException e) {
                // prueba el siguiente formato
            }
        }
        throw new IllegalArgumentException("Fecha de nacimiento '" + t + "' no valida (usa AAAA-MM-DD o DD/MM/AAAA)");
    }

    /** Entero entre min y max; acepta decimales con coma o punto (se redondea). */
    static Integer entero(String s, String campo, int min, int max) {
        BigDecimal v;
        try {
            v = new BigDecimal(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(campo + " '" + s.trim() + "' no es un numero");
        }
        BigDecimal redondeado = v.setScale(0, RoundingMode.HALF_UP);
        if (redondeado.compareTo(BigDecimal.valueOf(min)) < 0 || redondeado.compareTo(BigDecimal.valueOf(max)) > 0) {
            throw new IllegalArgumentException(campo + " " + redondeado.toPlainString() + " fuera de rango (" + min + " a " + max + ")");
        }
        return redondeado.intValue();
    }

    static boolean correoPlausible(String s) {
        return s.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    }
}
