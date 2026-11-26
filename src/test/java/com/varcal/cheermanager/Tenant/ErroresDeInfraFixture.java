package com.varcal.cheermanager.Tenant;

/**
 * SEC-03: fabrica excepciones "como si" las lanzara codigo de infraestructura (Tenant, security, config), cuyo
 * mensaje no esta escrito para el usuario (p. ej. "No hay club (tenant) en el contexto de esta operacion").
 */
public final class ErroresDeInfraFixture {

    private ErroresDeInfraFixture() {
    }

    public static IllegalArgumentException iae(String mensaje) {
        return new IllegalArgumentException(mensaje);
    }

    public static IllegalStateException ise(String mensaje) {
        return new IllegalStateException(mensaje);
    }

    public static RuntimeException runtime(String mensaje) {
        return new RuntimeException(mensaje);
    }

    public static jakarta.persistence.EntityNotFoundException entidadNoEncontrada(String mensaje) {
        return new jakarta.persistence.EntityNotFoundException(mensaje);
    }

    /** Una clase propia es publica aunque se cree en infraestructura: existir en nuestro codigo prueba que la escribimos. */
    public static class ErrorPropioDeInfra extends RuntimeException {
        public ErrorPropioDeInfra(String mensaje) {
            super(mensaje);
        }
    }
}
