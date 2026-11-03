package com.varcal.cheermanager.Service.Caja;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Lectura de las columnas de un extracto bancario en CSV (fechas y valores en formato colombiano o ISO). Pura. */
public final class ExtractoParseo {

    private static final List<DateTimeFormatter> FORMATOS = List.of(
            DateTimeFormatter.ofPattern("uuuu-M-d").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d-M-uuuu").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("uuuu/M/d").withResolverStyle(ResolverStyle.STRICT),
            DateTimeFormatter.ofPattern("d/M/uu").withResolverStyle(ResolverStyle.STRICT));

    /** Nombres de columna aceptados (ya normalizados) -> nombre oficial: fecha, valor, descripcion, referencia. */
    static final Map<String, String> ALIAS = Map.ofEntries(
            Map.entry("fecha", "fecha"), Map.entry("fecha_movimiento", "fecha"), Map.entry("fecha_transaccion", "fecha"),
            Map.entry("fecha_operacion", "fecha"), Map.entry("date", "fecha"),
            Map.entry("valor", "valor"), Map.entry("monto", "valor"), Map.entry("importe", "valor"),
            Map.entry("credito", "valor"), Map.entry("abono", "valor"), Map.entry("valor_credito", "valor"),
            Map.entry("amount", "valor"),
            Map.entry("descripcion", "descripcion"), Map.entry("detalle", "descripcion"), Map.entry("concepto", "descripcion"),
            Map.entry("referencia", "referencia"), Map.entry("ref", "referencia"), Map.entry("comprobante", "referencia"),
            Map.entry("numero_documento", "referencia"), Map.entry("documento", "referencia"),
            Map.entry("no_documento", "referencia"), Map.entry("n_documento", "referencia"),
            Map.entry("nro_documento", "referencia"), Map.entry("numero_de_documento", "referencia"), Map.entry("reference", "referencia"));

    private ExtractoParseo() {
    }

    static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_").replaceAll("^_+|_+$", "");
    }

    /** Nombre oficial de la columna, o la columna normalizada si no se reconoce. */
    static String nombreColumna(String encabezado) {
        String n = normalizar(encabezado);
        return ALIAS.getOrDefault(n, n);
    }

    static LocalDate fecha(String s) {
        String t = s == null ? "" : s.trim();
        // algunos bancos agregan la hora: "2026-10-30 14:05:00"
        int espacio = t.indexOf(' ');
        if (espacio > 0) {
            t = t.substring(0, espacio);
        }
        int T = t.indexOf('T');
        if (T > 0) {
            t = t.substring(0, T);
        }
        for (DateTimeFormatter f : FORMATOS) {
            try {
                return LocalDate.parse(t, f);
            } catch (DateTimeParseException e) {
                // siguiente formato
            }
        }
        throw new IllegalArgumentException("fecha '" + (s == null ? "" : s.trim()) + "' no valida");
    }

    /**
     * Valor en pesos. Acepta "1.234.567,89", "1,234,567.89", "$ 150.000", "(150000)", "-150000", "150000-".
     * Regla de los separadores: si hay punto Y coma, el ultimo es el decimal; si hay uno solo, es de miles cuando
     * se repite o cuando le siguen exactamente 3 digitos ("1.500" = 1500), y decimal en otro caso ("12,5" = 12.5).
     */
    static BigDecimal monto(String s) {
        String t = s == null ? "" : s.trim();
        boolean negativo = false;
        if (t.startsWith("(") && t.endsWith(")") && t.length() > 2) {
            negativo = true;
            t = t.substring(1, t.length() - 1);
        }
        t = t.replace("COP", "").replace("$", "").replace(" ", "").replace(" ", "");
        if (t.startsWith("-")) {
            negativo = true;
            t = t.substring(1);
        } else if (t.startsWith("+")) {
            t = t.substring(1);
        }
        if (t.endsWith("-")) {
            negativo = true;
            t = t.substring(0, t.length() - 1);
        }
        if (t.isEmpty() || !t.matches("[0-9.,]+") || !t.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("valor '" + (s == null ? "" : s.trim()) + "' no valido");
        }
        int ultimoPunto = t.lastIndexOf('.');
        int ultimaComa = t.lastIndexOf(',');
        String entero = t;
        String decimales = "";
        if (ultimoPunto >= 0 && ultimaComa >= 0) {
            int sep = Math.max(ultimoPunto, ultimaComa);
            entero = t.substring(0, sep);
            decimales = t.substring(sep + 1);
        } else if (ultimoPunto >= 0 || ultimaComa >= 0) {
            char c = ultimoPunto >= 0 ? '.' : ',';
            int sep = t.lastIndexOf(c);
            long repeticiones = t.chars().filter(x -> x == c).count();
            String cola = t.substring(sep + 1);
            if (repeticiones == 1 && cola.length() != 3) {
                entero = t.substring(0, sep);
                decimales = cola;
            }
        }
        entero = entero.replace(".", "").replace(",", "");
        if (!decimales.matches("\\d*")) {
            throw new IllegalArgumentException("valor '" + s.trim() + "' no valido");
        }
        BigDecimal v = new BigDecimal((entero.isEmpty() ? "0" : entero) + (decimales.isEmpty() ? "" : "." + decimales));
        return negativo ? v.negate() : v;
    }
}
