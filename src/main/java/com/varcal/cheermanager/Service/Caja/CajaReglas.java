package com.varcal.cheermanager.Service.Caja;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Reglas puras del cierre de caja (sin base de datos). */
final class CajaReglas {

    static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private CajaReglas() {
    }

    /** Contado menos esperado: positivo = sobra efectivo, negativo = falta. */
    static BigDecimal diferencia(BigDecimal esperado, BigDecimal contado) {
        return contado.subtract(esperado);
    }

    /** Reglas del cierre. Devuelve la nota limpia (o null). Lanza RuntimeException con un mensaje claro. */
    static String validarCierre(LocalDate fecha, LocalDate hoy, BigDecimal contado, BigDecimal esperado,
            String nota, String estadoActual) {
        if (fecha == null) {
            throw new RuntimeException("La fecha del cierre es obligatoria");
        }
        if (fecha.isAfter(hoy)) {
            throw new RuntimeException("No se puede cerrar la caja de una fecha futura");
        }
        if ("CERRADA".equals(estadoActual)) {
            throw new RuntimeException("La caja del " + fecha.format(FMT) + " ya esta cerrada");
        }
        if (contado == null || contado.signum() < 0) {
            throw new RuntimeException("Indica el efectivo contado (cero o mayor)");
        }
        String n = nota == null ? "" : nota.trim();
        if (n.length() > 300) {
            throw new RuntimeException("La nota no puede pasar de 300 caracteres");
        }
        if (diferencia(esperado, contado).signum() != 0 && n.isEmpty()) {
            throw new RuntimeException("El efectivo contado no coincide con el esperado: escribe una nota que explique la diferencia");
        }
        return n.isEmpty() ? null : n;
    }
}
