package com.varcal.cheermanager.security;

import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.varcal.cheermanager.tenant.TenantContext;

/**
 * PERF-02: cache de autorizacion por usuario. Evita 5 a 9 consultas SQL en CADA peticion autenticada (comprobar que el
 * usuario existe y cargar su rol, roles y permisos).
 *
 * <p>Que se guarda: por (club, usuario), el conjunto de permisos. Que el usuario EXISTA es que haya entrada: un usuario
 * inexistente o inactivo nunca se guarda.
 *
 * <p>Cuanto puede tardar un cambio en notarse: 30 s como MAXIMO (TTL). Pero todo cambio que afecta a la existencia, el
 * rol o los permisos de un usuario debe llamar a {@code olvidar...} y entonces es INMEDIATO en esta instancia; el test
 * de arquitectura (InvalidacionAutorizacionArquitecturaTest) falla si se anade una escritura sin invalidar. Con varias
 * instancias de la API, las demas se enteran solo al vencer el TTL.
 *
 * <p>Es estatico a proposito: las escrituras estan repartidas por muchas clases y asi invalidar no obliga a cambiar
 * sus constructores. Las invalidaciones se repiten tambien DESPUES DEL COMMIT: una peticion que leyera entre el flush y
 * el commit vio el dato viejo y podria haberlo guardado.
 */
public final class AutorizacionCache {

    public static final long TTL_MS = 30_000;
    public static final int MAXIMO_ENTRADAS = 10_000;

    record Clave(int clubId, int usuarioId) {
    }

    private static volatile CacheTtl<Clave, Set<String>> usuarios = nuevo();

    private AutorizacionCache() {
    }

    private static CacheTtl<Clave, Set<String>> nuevo() {
        return new CacheTtl<>(TTL_MS, MAXIMO_ENTRADAS, System::currentTimeMillis);
    }

    /**
     * Permisos vigentes del usuario o vacio si no existe / esta inactivo. {@code cargar} se ejecuta solo si no hay un
     * valor vigente en el cache.
     */
    public static Optional<Set<String>> permisos(int clubId, int usuarioId, Supplier<Optional<Set<String>>> cargar) {
        Set<String> valor = usuarios.obtener(new Clave(clubId, usuarioId), () -> cargar.get().orElse(null));
        return Optional.ofNullable(valor);
    }

    /** El usuario cambio (rol, roles, activo/inactivo, eliminado...). */
    public static void olvidarUsuario(Integer usuarioId) {
        if (usuarioId == null) return;
        accionYRepetirTrasCommit(() -> usuarios.invalidarSi(k -> k.usuarioId() == usuarioId));
    }

    /** Cambiaron roles de un club (sus permisos o sus datos): afecta a todos sus usuarios. */
    public static void olvidarClub(Integer clubId) {
        if (clubId == null) {
            olvidarTodo();
            return;
        }
        accionYRepetirTrasCommit(() -> usuarios.invalidarSi(k -> k.clubId() == clubId));
    }

    /** Cambiaron roles dentro del club actual (o, si no hay club en contexto, todo). */
    public static void olvidarRolesDelClubActual() {
        if (TenantContext.isSet()) {
            olvidarClub(TenantContext.get());
        } else {
            olvidarTodo();
        }
    }

    /** Cambio global (catalogo de permisos, eliminacion de un club...). */
    public static void olvidarTodo() {
        accionYRepetirTrasCommit(() -> usuarios.invalidarTodo());
    }

    /** Solo para pruebas: deja el cache vacio y nuevo. */
    static void reiniciar() {
        usuarios = nuevo();
    }

    static int tamano() {
        return usuarios.tamano();
    }

    private static void accionYRepetirTrasCommit(Runnable accion) {
        accion.run(); // ya: asi lo nota la siguiente peticion aunque la transaccion tarde en cerrar
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    accion.run();
                }
            });
        }
    }
}
