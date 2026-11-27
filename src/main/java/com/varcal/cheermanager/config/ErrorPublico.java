package com.varcal.cheermanager.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.EntityNotFoundException;

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
 *   <li>es exactamente IllegalArgumentException, IllegalStateException, RuntimeException o
 *       jakarta.persistence.EntityNotFoundException (no una subclase como NumberFormatException; las de
 *       Hibernate llevan "Unable to find com.varcal...Entity" pero se crean en org.hibernate, asi que se ocultan)
 *       y se creo en la capa de negocio: el primer frame de la pila, que es el
 *       codigo que hizo {@code new}, esta en {@code Service}, {@code Controller}, {@code DTO} o
 *       {@code Models}. Un {@code Integer.parseInt} o un {@code Enum.valueOf} fallan dentro de
 *       {@code java.lang}, asi que se ocultan; tampoco se muestran las de {@code Tenant}, {@code security}
 *       o {@code config}, que son de infraestructura.</li>
 * </ul>
 * Ademas se oculta el mensaje vacio y el patron {@code new IllegalStateException(causa)}, cuyo mensaje es
 * {@code causa.toString()}. Ante la duda, se oculta.
 */
public final class ErrorPublico {

    private static final Logger log = LoggerFactory.getLogger(ErrorPublico.class);

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
                && tipo != RuntimeException.class && tipo != EntityNotFoundException.class) {
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

    /**
     * El mensaje de la excepcion si es publico; si no, {@link #GENERICO}. Nunca devuelve null. Es lo que usan los
     * controladores en sus {@code catch}: como ahi nadie mas registra el error, lo que se oculta al cliente se deja
     * en el log (con la pila) para poder diagnosticarlo.
     */
    public static String de(Throwable e) {
        return de(e, GENERICO);
    }

    /**
     * Igual que {@link #de(Throwable)} pero con un texto propio cuando el mensaje no se puede mostrar, para los
     * sitios donde "Error interno del servidor" dice menos que el contexto (p. ej. "No se pudo guardar la fila").
     */
    public static String de(Throwable e, String textoSiSeOculta) {
        if (esPublico(e)) return e.getMessage();
        if (e != null) log.error("Detalle de un error que no se muestra al cliente: {}", e.toString(), e);
        return textoSiSeOculta;
    }
}
