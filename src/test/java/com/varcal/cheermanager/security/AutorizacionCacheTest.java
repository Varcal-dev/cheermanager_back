package com.varcal.cheermanager.security;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.varcal.cheermanager.tenant.TenantContext;

/** PERF-02: la fachada de autorizacion (que se guarda, que invalida cada cambio, y el repaso tras el commit). */
class AutorizacionCacheTest {

    private final AtomicInteger cargas = new AtomicInteger();

    private Optional<Set<String>> cargar(Set<String> permisos) {
        cargas.incrementAndGet();
        return Optional.of(permisos);
    }

    @BeforeEach
    void limpio() {
        AutorizacionCache.reiniciar();
        TenantContext.clear();
    }

    @AfterEach
    void restaurar() {
        TenantContext.clear();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void guardaLosPermisosPorClubYUsuario() {
        AutorizacionCache.permisos(2, 10, () -> cargar(Set.of("ver_pago")));
        AutorizacionCache.permisos(2, 10, () -> cargar(Set.of("otro")));
        assertEquals(1, cargas.get());
        // otro club con el mismo id de usuario NO comparte entrada
        assertEquals(Set.of("x"), AutorizacionCache.permisos(3, 10, () -> cargar(Set.of("x"))).orElseThrow());
        assertEquals(2, cargas.get());
    }

    @Test
    void unUsuarioInexistenteNuncaSeGuarda() {
        assertTrue(AutorizacionCache.permisos(2, 10, () -> Optional.empty()).isEmpty());
        assertEquals(0, AutorizacionCache.tamano());
        assertEquals(Set.of("ver_pago"), AutorizacionCache.permisos(2, 10, () -> cargar(Set.of("ver_pago"))).orElseThrow());
    }

    @Test
    void unUsuarioSinPermisosSiSeGuardaYSigueExistiendo() {
        Optional<Set<String>> r = AutorizacionCache.permisos(2, 10, () -> cargar(Set.of()));
        assertTrue(r.isPresent(), "existe aunque no tenga ningun permiso");
        AutorizacionCache.permisos(2, 10, () -> cargar(Set.of("no debe verse")));
        assertEquals(1, cargas.get());
    }

    @Test
    void olvidarUsuarioQuitaSoloAEseUsuario() {
        AutorizacionCache.permisos(2, 10, () -> cargar(Set.of("a")));
        AutorizacionCache.permisos(2, 11, () -> cargar(Set.of("b")));
        AutorizacionCache.olvidarUsuario(10);
        assertEquals(Set.of("nuevo"), AutorizacionCache.permisos(2, 10, () -> cargar(Set.of("nuevo"))).orElseThrow());
        assertEquals(Set.of("b"), AutorizacionCache.permisos(2, 11, () -> cargar(Set.of("otro"))).orElseThrow());
    }

    @Test
    void olvidarClubQuitaATodosSusUsuariosYSoloDeEseClub() {
        AutorizacionCache.permisos(2, 10, () -> cargar(Set.of("a")));
        AutorizacionCache.permisos(2, 11, () -> cargar(Set.of("b")));
        AutorizacionCache.permisos(3, 12, () -> cargar(Set.of("c")));
        AutorizacionCache.olvidarClub(2);
        assertEquals(1, AutorizacionCache.tamano());
        assertEquals(Set.of("c"), AutorizacionCache.permisos(3, 12, () -> cargar(Set.of("otro"))).orElseThrow());
    }

    @Test
    void olvidarClubConNuloEquivaleATodo() {
        AutorizacionCache.permisos(2, 10, () -> cargar(Set.of("a")));
        AutorizacionCache.olvidarClub(null);
        assertEquals(0, AutorizacionCache.tamano());
    }

    @Test
    void olvidarRolesDelClubActualUsaElClubEnContextoOTodoSiNoHay() {
        AutorizacionCache.permisos(2, 10, () -> cargar(Set.of("a")));
        AutorizacionCache.permisos(3, 12, () -> cargar(Set.of("c")));

        TenantContext.set(2);
        AutorizacionCache.olvidarRolesDelClubActual();
        assertEquals(1, AutorizacionCache.tamano(), "solo el club 2");

        TenantContext.clear();
        AutorizacionCache.olvidarRolesDelClubActual();
        assertEquals(0, AutorizacionCache.tamano(), "sin club en contexto: todo");
    }

    @Test
    void olvidarTodoVaciaElCache() {
        AutorizacionCache.permisos(2, 10, () -> cargar(Set.of("a")));
        AutorizacionCache.permisos(3, 12, () -> cargar(Set.of("c")));
        AutorizacionCache.olvidarTodo();
        assertEquals(0, AutorizacionCache.tamano());
    }

    @Test
    void siHayTransaccionLaInvalidacionSeRepiteDespuesDelCommit() {
        TransactionSynchronizationManager.initSynchronization();
        AutorizacionCache.permisos(2, 10, () -> cargar(Set.of("viejo")));

        AutorizacionCache.olvidarUsuario(10);                 // dentro de la transaccion
        assertEquals(0, AutorizacionCache.tamano());

        // Antes del commit otra peticion lee el dato VIEJO (aun no confirmado) y lo guarda...
        AutorizacionCache.permisos(2, 10, () -> cargar(Set.of("viejo")));
        assertEquals(1, AutorizacionCache.tamano());

        // ...y al confirmarse la transaccion se vuelve a invalidar.
        for (TransactionSynchronization s : TransactionSynchronizationManager.getSynchronizations()) {
            s.afterCommit();
        }
        assertEquals(0, AutorizacionCache.tamano());
    }

    @Test
    void sinTransaccionNoRegistraNada() {
        assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
        AutorizacionCache.olvidarUsuario(10);   // no debe lanzar
    }
}
