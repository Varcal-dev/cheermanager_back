package com.varcal.cheermanager.Service.Evaluaciones;

import java.time.LocalDate;
import java.util.Locale;

import org.springframework.data.domain.Sort;

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

    /**
     * Orden del historial. orden: fecha | deportista | categoria | criterio | puntaje (por defecto fecha);
     * direccion: asc | desc (por defecto: fecha desc, el resto asc). Solo se aceptan esos valores, asi nunca
     * llega al ORDER BY un texto del cliente. Los desempates son estables para que la paginacion no repita filas.
     * Las propiedades son rutas desde Evaluacion.
     */
    public static Sort ordenBusqueda(String orden, String direccion) {
        String clave = orden == null || orden.isBlank() ? "fecha" : orden.trim().toLowerCase(Locale.ROOT);
        Sort.Direction dir;
        if (direccion == null || direccion.isBlank()) {
            dir = clave.equals("fecha") ? Sort.Direction.DESC : Sort.Direction.ASC;
        } else if (direccion.trim().equalsIgnoreCase("asc")) {
            dir = Sort.Direction.ASC;
        } else if (direccion.trim().equalsIgnoreCase("desc")) {
            dir = Sort.Direction.DESC;
        } else {
            throw new IllegalArgumentException("La direccion del orden debe ser asc o desc");
        }
        Sort principal;
        switch (clave) {
            case "fecha":
                // Mismo desempate que tenia el historial: lo mas reciente (o lo mas antiguo) primero.
                return Sort.by(dir, "fecha", "id");
            case "deportista":
                principal = Sort.by(
                        new Sort.Order(dir, "deportista.persona.apellidos").ignoreCase(),
                        new Sort.Order(dir, "deportista.persona.nombre").ignoreCase());
                break;
            case "categoria":
                principal = Sort.by(new Sort.Order(dir, "criterioEvaluacion.categoria.categoria").ignoreCase());
                break;
            case "criterio":
                principal = Sort.by(new Sort.Order(dir, "criterioEvaluacion.nombre").ignoreCase());
                break;
            case "puntaje":
                principal = Sort.by(dir, "puntajeObtenido");
                break;
            default:
                throw new IllegalArgumentException(
                        "Orden no valido: use fecha, deportista, categoria, criterio o puntaje");
        }
        return principal.and(Sort.by(Sort.Direction.DESC, "fecha", "id"));
    }

    /** Tamano de pagina acotado a 1..100 (50 por defecto). */
    public static int tamanoPagina(Integer tam) {
        if (tam == null) {
            return 50;
        }
        return Math.max(1, Math.min(tam, 100));
    }
}