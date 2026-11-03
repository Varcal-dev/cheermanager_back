package com.varcal.cheermanager.Service.Caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Cruza las lineas de un extracto bancario con los pagos registrados. Es solo una SUGERENCIA: nadie queda
 * conciliado hasta que una persona la confirma.
 *
 * 1) REFERENCIA: misma referencia (>= 4 letras/numeros, sin importar mayusculas ni simbolos) y mismo valor.
 * 2) MONTO_FECHA: mismo valor y fecha a no mas de "ventana" dias; se elige la mas cercana.
 *    Si hay mas de una opcion posible (o varias lineas compiten por el mismo pago) la sugerencia sale como AMBIGUA.
 * Cada linea y cada pago se usan una sola vez. Pura: sin base de datos.
 */
public final class ConciliacionMatcher {

    public static final String REFERENCIA = "REFERENCIA";
    public static final String MONTO_FECHA = "MONTO_FECHA";
    public static final String AMBIGUA = "AMBIGUA";

    public record Linea(int indice, LocalDate fecha, BigDecimal monto, String referencia) {
    }

    public record Candidato(int pagoId, LocalDate fecha, BigDecimal monto, String referencia) {
    }

    public record Coincidencia(int indiceLinea, int pagoId, String criterio, int diasDiferencia) {
    }

    public record Resultado(List<Coincidencia> coincidencias, List<Integer> lineasSinPago, List<Integer> pagosSinLinea) {
    }

    private ConciliacionMatcher() {
    }

    static String clave(String ref) {
        if (ref == null) {
            return "";
        }
        String k = ref.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return k.length() >= 4 ? k : "";
    }

    private static int dias(LocalDate a, LocalDate b) {
        return (int) Math.abs(ChronoUnit.DAYS.between(a, b));
    }

    public static Resultado cruzar(List<Linea> lineas, List<Candidato> candidatos, int ventanaDias) {
        List<Coincidencia> out = new ArrayList<>();
        Set<Integer> lineasUsadas = new HashSet<>();
        Set<Integer> pagosUsados = new HashSet<>();

        // --- 1) por referencia
        for (Linea l : lineas) {
            String k = clave(l.referencia());
            if (k.isEmpty()) {
                continue;
            }
            Candidato mejor = null;
            for (Candidato c : candidatos) {
                if (pagosUsados.contains(c.pagoId()) || !k.equals(clave(c.referencia()))
                        || c.monto().compareTo(l.monto()) != 0) {
                    continue;
                }
                if (mejor == null || dias(l.fecha(), c.fecha()) < dias(l.fecha(), mejor.fecha())
                        || (dias(l.fecha(), c.fecha()) == dias(l.fecha(), mejor.fecha()) && c.pagoId() < mejor.pagoId())) {
                    mejor = c;
                }
            }
            if (mejor != null) {
                out.add(new Coincidencia(l.indice(), mejor.pagoId(), REFERENCIA, dias(l.fecha(), mejor.fecha())));
                lineasUsadas.add(l.indice());
                pagosUsados.add(mejor.pagoId());
            }
        }

        // --- 2) por valor y fecha: primero se calcula con quien podria cruzar cada linea
        Map<Integer, List<Candidato>> posibles = new HashMap<>();
        Map<Integer, Integer> lineasPorPago = new HashMap<>();
        for (Linea l : lineas) {
            if (lineasUsadas.contains(l.indice())) {
                continue;
            }
            List<Candidato> lista = new ArrayList<>();
            for (Candidato c : candidatos) {
                if (!pagosUsados.contains(c.pagoId()) && c.monto().compareTo(l.monto()) == 0
                        && dias(l.fecha(), c.fecha()) <= ventanaDias) {
                    lista.add(c);
                    lineasPorPago.merge(c.pagoId(), 1, Integer::sum);
                }
            }
            posibles.put(l.indice(), lista);
        }
        for (Linea l : lineas) {
            List<Candidato> lista = posibles.get(l.indice());
            if (lista == null || lista.isEmpty()) {
                continue;
            }
            boolean ambigua = lista.size() > 1 || lista.stream().anyMatch(c -> lineasPorPago.get(c.pagoId()) > 1);
            Candidato elegido = null;
            for (Candidato c : lista) {
                if (pagosUsados.contains(c.pagoId())) {
                    continue;
                }
                if (elegido == null || dias(l.fecha(), c.fecha()) < dias(l.fecha(), elegido.fecha())
                        || (dias(l.fecha(), c.fecha()) == dias(l.fecha(), elegido.fecha()) && c.pagoId() < elegido.pagoId())) {
                    elegido = c;
                }
            }
            if (elegido != null) {
                out.add(new Coincidencia(l.indice(), elegido.pagoId(), ambigua ? AMBIGUA : MONTO_FECHA,
                        dias(l.fecha(), elegido.fecha())));
                lineasUsadas.add(l.indice());
                pagosUsados.add(elegido.pagoId());
            }
        }

        List<Integer> lineasSin = new ArrayList<>();
        LocalDate min = null, max = null;
        for (Linea l : lineas) {
            if (!lineasUsadas.contains(l.indice())) {
                lineasSin.add(l.indice());
            }
            if (min == null || l.fecha().isBefore(min)) min = l.fecha();
            if (max == null || l.fecha().isAfter(max)) max = l.fecha();
        }
        List<Integer> pagosSin = new ArrayList<>();
        for (Candidato c : candidatos) {
            // solo los pagos que caen dentro del periodo que cubre el extracto
            if (!pagosUsados.contains(c.pagoId()) && min != null && !c.fecha().isBefore(min) && !c.fecha().isAfter(max)) {
                pagosSin.add(c.pagoId());
            }
        }
        out.sort((a, b) -> Integer.compare(a.indiceLinea(), b.indiceLinea()));
        return new Resultado(out, lineasSin, pagosSin);
    }
}
