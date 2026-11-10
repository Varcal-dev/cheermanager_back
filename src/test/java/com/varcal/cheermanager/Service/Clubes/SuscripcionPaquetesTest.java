package com.varcal.cheermanager.Service.Clubes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Models.Clubes.PlanSaas;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.repository.Clubes.PlanSaasRepository;
import com.varcal.cheermanager.repository.Clubes.SuscripcionPagoRepository;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.tenant.TenantLookupRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** S1b-2: los paquetes de deportistas suman al cupo del plan; al 100 % solo se frena el alta de nuevos. */
class SuscripcionPaquetesTest {

    static class LookupFalso extends TenantLookupRepository {
        final long activos;
        LookupFalso(long activos) { this.activos = activos; }
        @Override
        public long contarDeportistasActivos(int clubId) { return activos; }
    }

    static PlanSaas plan(int id, Integer max) {
        PlanSaas p = new PlanSaas();
        p.setId(id);
        p.setCodigo("P" + id);
        p.setNombre("Plan " + id);
        p.setMaxDeportistas(max);
        return p;
    }

    SuscripcionService servicio(long activos, Integer planId, Integer limitePropio, int extra) {
        Club club = new Club();
        club.setId(2);
        club.setNombre("Club");
        club.setSlug("club");
        club.setPlanId(planId);
        club.setLimiteDeportistas(limitePropio);
        PlanSaas start = plan(1, 75), ilimitado = plan(3, null);
        Repo clubes = new Repo().on("findById", a -> Optional.of(club)).on("save", a -> a[0]);
        Repo planes = new Repo().on("findById", a -> Optional.of((Integer) a[0] == 1 ? start : ilimitado));
        SuscripcionService s = new SuscripcionService(Fakes.proxy(ClubRepository.class, clubes),
                Fakes.proxy(PlanSaasRepository.class, planes), Fakes.proxy(SuscripcionPagoRepository.class, new Repo()),
                new LookupFalso(activos));
        s.setPaquetes(new PaquetesService(null, null) {
            @Override
            public int cupoExtra(int clubId, int plan) { return extra; }
        });
        return s;
    }

    @Test
    void elPaqueteSumaAlCupoDelPlan() {
        TenantContext.set(2);
        try {
            assertEquals(30, servicio(70, 1, null, 25).cupoDisponible()); // 75 + 25 - 70
            assertEquals(5, servicio(70, 1, null, 0).cupoDisponible());
            assertEquals(40, servicio(70, 1, 85, 25).cupoDisponible()); // limite propio 85 + 25 - 70
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void alLlenarseElCupoConPaqueteSeFrenaElAlta() {
        TenantContext.set(2);
        try {
            servicio(99, 1, null, 25).exigirCupoDeportista(); // 99 de 100: aun cabe uno
            RuntimeException ex = assertThrows(RuntimeException.class, () -> servicio(100, 1, null, 25).exigirCupoDeportista());
            assertTrue(ex.getMessage().contains("100"));
            assertTrue(ex.getMessage().contains("paquete"));
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void elPlanIlimitadoNoSumaNiFrena() {
        TenantContext.set(2);
        try {
            assertNull(servicio(900, 3, null, 25).cupoDisponible());
            servicio(900, 3, null, 25).exigirCupoDeportista();
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void elResumenInformaLosEfectivosConPaquetes() {
        SuscripcionService.Resumen r = servicio(10, 1, null, 25).resumen(2);
        assertEquals(75, r.limiteDelPlan());
        assertEquals(100, r.limiteEfectivo());
        assertEquals(25, r.cupoExtraPaquetes());
    }
}
