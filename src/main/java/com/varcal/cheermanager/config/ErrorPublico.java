package com.varcal.cheermanager.config;

/**
 * SEC-03: decide si el mensaje de una excepcion se puede mostrar al usuario o hay que ocultarlo.
 *
 * <p>El codigo de negocio lanza IllegalArgumentException / IllegalStateException / RuntimeException con un
 * texto escrito para el usuario ("Ya existe una sede con ese nombre"). Pero esos mismos tipos tambien los
 * lanzan las librerias con texto interno ({@code For input string: "abc"}, {@code No enum constant ...}), y
 * otras excepciones (DataAccess, NullPointer, ClassCast...) llevan SQL, nombres de tablas o de clases.
 *
 * <p>Un mensaje es publico solo si lo escribimos nosotros:
 * <ul>
 *   <li>la clase de la excepcion es nuestra ({@code com.varcal.cheermanager.*}); o</li>
 *   <li>es exactamente IllegalArgumentException, IllegalStateException o RuntimeException (no una subclase
 *       como NumberFormatException) y se creo en la capa de negocio: el primer frame de la pila, que es el
 *       codigo que hizo {@code new}, esta en {@code Service}, {@code Controller}, {@code DTO} o
 *       {@code Models}. Un {@code Integer.parseInt} o un {@code Enum.valueOf} fallan dentro de
 *       {@code java.lang}, asi que se ocultan; tampoco se muestran las de {@code Tenant}, {@code security}
 *       o {@code config}, que son de infraestructura.</li>
 * </ul>
 * Ademas se oculta el mensaje vacio y el patron {@code new IllegalStateException(causa)}, cuyo mensaje es
 * {@code causa.toString()}. Ante la duda, se oculta.
 */
public final class ErrorPublico {

    /** Texto que se devuelve cuando el mensaje real no se puede mostrar. */
    public static final String GENERICO = "Error interno del servidor";

    private static final String BASE = "com.varcal.cheermanager.";
    private static final String[] CAPAS_DE_NEGOCIO = {
            BASE + "Service.", BASE + "Controller.", BASE + "DTO.", BASE + "Models." };

    private ErrorPublico() {
    }

    public static boolean esPublico(Throwable e) {
        if (e == null) return false;
        String mensaje = e.getMessage();
        if (mensaje == null || mensaje.isBlank()) return false;
        Throwable causa = e.getCause();
        if (causa != null && mensaje.equals(causa.toString())) return false;

        Class<?> tipo = e.getClass();
        if (tipo.getName().startsWith(BASE)) return true;
        if (tipo != IllegalArgumentException.class && tipo != IllegalStateException.class
                && tipo != RuntimeException.class) {
            return false;
        }
        StackTraceElement[] pila = e.getStackTrace();
        if (pila.length == 0) return false;
        String origen = pila[0].getClassName();
        for (String capa : CAPAS_DE_NEGOCIO) {
            if (origen.startsWith(capa)) return true;
        }
        return false;
    }

    /** El mensaje de la excepcion si es publico; si no, {@link #GENERICO}. Nunca devuelve null. */
    public static String de(Throwable e) {
        return esPublico(e) ? e.getMessage() : GENERICO;
    }
}
