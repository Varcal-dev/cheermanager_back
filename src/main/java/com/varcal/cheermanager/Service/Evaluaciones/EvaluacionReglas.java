package com.varcal.cheermanager.Service.Evaluaciones;

import java.time.LocalDate;

/** Reglas puras de una evaluacion por criterio (sin base de datos). */
public final class EvaluacionReglas {

    /** Escala de la evaluacion por criterio: 0 a 10 (la misma que muestra la ficha del deportista). */
    public static final int PUNTAJE_MAXIMO = 10;
    public static final int MAX_REGISTROS_POR_LOTE = 200;

    private EvaluacionReglas() {
    }

    public static void validarPuntaje(Integer puntaje) {
        if (puntaje == null || puntaje < 0 || puntaje > PUNTAJE_MAXIMO) {
            throw new IllegalArgumentException("El puntaje debe ser un numero entero entre 0 y " + PUNTAJE_MAXIMO);
        }
    }

    /** Sin fecha = hoy. No se evalua hacia el futuro. */
    public static LocalDate validarFecha(LocalDate fecha, LocalDate hoy) {
        if (fecha == null) {
            return hoy;
        }
        if (fecha.isAfter(hoy)) {
            throw new IllegalArgumentException("La fecha de la evaluacion no puede ser futura");
        }
        return fecha;
    }

    public static void validarLote(int cantidad) {
        if (cantidad == 0) {
            throw new IllegalArgumentException("Indica al menos un registro");
        }
        if (cantidad > MAX_REGISTROS_POR_LOTE) {
            throw new IllegalArgumentException("Maximo " + MAX_REGISTROS_POR_LOTE + " registros por envio");
        }
    }

    /** Texto de busqueda listo para LIKE: minusculas, sin comodines propios; null si no hay texto. */
    public static String patronBusqueda(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.trim().toLowerCase(java.util.Locale.ROOT).replace("%", "").replace("_", "");
        return t.isEmpty() ? null : "%" + t + "%";
    }

    /** Tamano de pagina acotado a 1..100 (50 por defecto). */
    public static int tamanoPagina(Integer tam) {
        if (tam == null) {
            return 50;
        }
        return Math.max(1, Math.min(tam, 100));
    }
}
