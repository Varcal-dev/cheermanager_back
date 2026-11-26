package com.varcal.cheermanager.Service;

import java.util.function.Supplier;

/**
 * SEC-03: ErrorPublico decide por el origen de la excepcion (el primer frame de la pila es el codigo que hizo
 * {@code new}). Esta clase vive en el paquete {@code Service} para fabricar excepciones "como si" las lanzara un
 * service de negocio; {@code Tenant/ErroresDeInfraFixture} hace lo mismo desde infraestructura.
 */
public final class ErroresDeServicioFixture {

    private ErroresDeServicioFixture() {
    }

    public static IllegalArgumentException iae(String mensaje) {
        return new IllegalArgumentException(mensaje);
    }

    public static IllegalStateException ise(String mensaje) {
        return new IllegalStateException(mensaje);
    }

    /** {@code new IllegalStateException(causa)}: su mensaje es {@code causa.toString()}. */
    public static IllegalStateException iseConCausa(Throwable causa) {
        return new IllegalStateException(causa);
    }

    public static RuntimeException runtime(String mensaje) {
        return new RuntimeException(mensaje);
    }

    public static RuntimeException runtimeConCausa(String mensaje, Throwable causa) {
        return new RuntimeException(mensaje, causa);
    }

    public static RuntimeException runtimeSinMensaje() {
        return new RuntimeException();
    }

    public static NumberFormatException nfe(String mensaje) {
        return new NumberFormatException(mensaje);
    }

    public static IllegalArgumentException iaeDesdeLambda(String mensaje) {
        Supplier<IllegalArgumentException> s = () -> new IllegalArgumentException(mensaje);
        return s.get();
    }

    /** Excepcion con clase propia, como AsignacionInvalidaException o TooManyRequests. */
    public static class ErrorPropio extends RuntimeException {
        public ErrorPropio(String mensaje) {
            super(mensaje);
        }
    }
}
