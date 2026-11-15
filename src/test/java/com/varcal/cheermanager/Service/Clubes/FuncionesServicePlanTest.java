package com.varcal.cheermanager.Service.Clubes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Reglas de plan que se agregaron al cerrar S1b: roles personalizados, dashboards avanzados y el catalogo del candado. */
class FuncionesServicePlanTest {

    @Test
    void rolesPersonalizadosFrenaLaEscrituraPeroDejaLeerLosRoles() {
        assertTrue(FuncionesService.reglaDe("/api/roles", "GET").isEmpty());
        assertTrue(FuncionesService.reglaDe("/api/roles/3/permisos", "GET").isEmpty());
        assertEquals("ROLES_PERSONALIZADOS", FuncionesService.reglaDe("/api/roles", "POST").orElseThrow().funcion());
        assertEquals("ROLES_PERSONALIZADOS", FuncionesService.reglaDe("/api/roles/3", "PUT").orElseThrow().funcion());
        assertEquals("ROLES_PERSONALIZADOS", FuncionesService.reglaDe("/api/roles/3", "DELETE").orElseThrow().funcion());
        assertEquals("ROLES_PERSONALIZADOS",
                FuncionesService.reglaDe("/api/roles/asignar-permisos", "POST").orElseThrow().funcion());
    }

    @Test
    void asignarRolesAUsuariosNoDependeDelPlan() {
        // Un club en Start debe poder darle a cada persona uno de los roles de la plantilla.
        assertTrue(FuncionesService.reglaDe("/api/usuario/7/asignar-roles", "POST").isEmpty());
        assertTrue(FuncionesService.reglaDe("/api/usuario/rol/listar", "GET").isEmpty());
    }

    @Test
    void dashboardsAvanzadosFrenaSoloFinanzasYAdministracion() {
        assertEquals("DASHBOARDS_AVANZADOS", FuncionesService.reglaDe("/api/dashboard/finanzas", "GET").orElseThrow().funcion());
        assertEquals("DASHBOARDS_AVANZADOS",
                FuncionesService.reglaDe("/api/dashboard/administracion", "GET").orElseThrow().funcion());
        assertTrue(FuncionesService.reglaDe("/api/dashboard/inicio", "GET").isEmpty());
        assertTrue(FuncionesService.reglaDe("/api/dashboard/personas", "GET").isEmpty());
        assertTrue(FuncionesService.reglaDe("/api/dashboard/tienda", "GET").isEmpty());
    }

    @Test
    void soloEscrituraSeDeduceDeLasReglas() {
        assertTrue(FuncionesService.soloEscritura("CONVENIOS"));
        assertTrue(FuncionesService.soloEscritura("RECORDATORIOS_MORA"));
        assertTrue(FuncionesService.soloEscritura("ROLES_PERSONALIZADOS"));
        assertFalse(FuncionesService.soloEscritura("VIAJES"));
        assertFalse(FuncionesService.soloEscritura("DASHBOARDS_AVANZADOS"));
        assertFalse(FuncionesService.soloEscritura("SIN_REGLAS"));
    }

    @Test
    void laEtiquetaDelPlanQuedaCorta() {
        assertEquals("Pro", FuncionesService.nombreCorto("CheerManager Pro"));
        assertEquals("Elite", FuncionesService.nombreCorto("CheerManager Elite"));
        assertEquals("Inicial", FuncionesService.nombreCorto("Inicial"));
        assertNull(FuncionesService.nombreCorto(null));
    }
}
