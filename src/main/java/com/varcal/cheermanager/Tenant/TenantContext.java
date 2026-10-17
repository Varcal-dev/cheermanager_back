package com.varcal.cheermanager.tenant;

import java.util.function.Supplier;

/**
 * Club (tenant) de la operacion en curso, por hilo.
 *
 * - Lo fija JwtTokenFilter desde el claim "clubId" del token (peticiones autenticadas) o
 *   el controller de auth tras resolver el club del usuario (login / recuperar contrasena).
 * - Hibernate lo lee al abrir cada sesion (TenantIdentifierResolver): toda lectura de una
 *   entidad con @TenantId se filtra por club_id y toda escritura se etiqueta con el.
 * - NO_TENANT (0) = "ningun club". No existe un club 0, asi que cualquier INSERT de una
 *   entidad de club sin tenant falla por la llave foranea en vez de guardarse "en ningun lado".
 */
public final class TenantContext {

    public static final int NO_TENANT = 0;

    private static final ThreadLocal<Integer> CURRENT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Integer clubId) {
        if (clubId == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(clubId);
        }
    }

    /** Club actual o NO_TENANT si no hay. */
    public static int get() {
        Integer c = CURRENT.get();
        return c == null ? NO_TENANT : c;
    }

    public static boolean isSet() {
        return CURRENT.get() != null;
    }

    /** Club actual; lanza si no hay (para codigo que jamas debe correr sin club). */
    public static int require() {
        Integer c = CURRENT.get();
        if (c == null) {
            throw new IllegalStateException("No hay club (tenant) en el contexto de esta operacion");
        }
        return c;
    }

    public static void clear() {
        CURRENT.remove();
    }

    /** Ejecuta con otro club y restaura el anterior al terminar (aunque falle). */
    public static <T> T runAs(Integer clubId, Supplier<T> action) {
        Integer previo = CURRENT.get();
        try {
            set(clubId);
            return action.get();
        } finally {
            set(previo);
        }
    }

    public static void runAs(Integer clubId, Runnable action) {
        runAs(clubId, () -> {
            action.run();
            return null;
        });
    }
}
