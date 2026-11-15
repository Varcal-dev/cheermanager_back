package com.varcal.cheermanager.Service.Auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

/** El techo de permisos: nadie da ni toca mas de lo que tiene. La parte con sesion se prueba contra la base. */
class JerarquiaAccesoServiceTest {

    @Test
    void unRolQueEsSubconjuntoSePuedeOtorgar() {
        assertTrue(JerarquiaAccesoService.faltantes(Set.of("ver_deportista", "ver_grupo", "ver_pago"),
                Set.of("ver_deportista", "ver_grupo")).isEmpty());
    }

    @Test
    void sePidenLosPermisosQueFaltan() {
        assertEquals(Set.of("gestionar_roles", "ver_roles"),
                JerarquiaAccesoService.faltantes(Set.of("ver_deportista"),
                        Set.of("ver_deportista", "ver_roles", "gestionar_roles")));
    }

    @Test
    void elPortalDeFamiliasNoCuentaComoPoderAdministrativo() {
        // El Administrador no tiene acceder_portal, pero debe poder asignar los roles Deportista y Acudiente.
        assertTrue(JerarquiaAccesoService.faltantes(Set.of("ver_deportista"), Set.of("acceder_portal")).isEmpty());
    }

    @Test
    void elAdministradorSeReconocePorNombre() {
        assertEquals("Administrador", JerarquiaAccesoService.ROL_ADMINISTRADOR);
    }
}
