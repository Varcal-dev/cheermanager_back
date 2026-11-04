package com.varcal.cheermanager.Service.Clubes;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Models.Clubes.PlanSaas;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.repository.Clubes.PlanSaasRepository;
import com.varcal.cheermanager.repository.Clubes.SuscripcionPagoRepository;
import com.varcal.cheermanager.tenant.TenantLookupRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** L10: bajar de plan o poner un limite menor solo es posible si caben los deportistas activos. */
class SuscripcionBajaPlanTest {

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

    final Club club = new Club();
    int guardados = 0;

    SuscripcionService servicio(long activos, Integer planActual) {
        club.setId(2);
        club.setNombre("Club");
        club.setSlug("club");
        club.setPlanId(planActual);
        club.setLimiteDeportistas(null);
        guardados = 0;
        PlanSaas chico = plan(1, 30), grande = plan(2, 100), ilimitado = plan(3, null);
        Repo clubes = new Repo()
                .on("findById", a -> Optional.of(club))
                .on("save", a -> { guardados++; return a[0]; });
        Repo planes = new Repo().on("findById", a -> {
            int id = (Integer) a[0];
            return Optional.of(id == 1 ? chico : id == 2 ? grande : ilimitado);
        });
        Repo pagos = new Repo();
        return new SuscripcionService(Fakes.proxy(ClubRepository.class, clubes), Fakes.proxy(PlanSaasRepository.class, planes),
                Fakes.proxy(SuscripcionPagoRepository.class, pagos), new LookupFalso(activos));
    }

    SuscripcionService.ActualizarRequest soloPlan(int planId) {
        return new SuscripcionService.ActualizarRequest(planId, null, null, null, null, null, null, null);
    }

    SuscripcionService.ActualizarRequest soloLimite(int limite) {
        return new SuscripcionService.ActualizarRequest(null, null, null, null, null, limite, null, null);
    }

    @Test
    void bajar_a_un_plan_que_no_alcanza_se_rechaza_y_no_guarda() {
        SuscripcionService s = servicio(45, 2);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> s.actualizar(2, soloPlan(1)));
        assertTrue(ex.getMessage().contains("45"));
        assertTrue(ex.getMessage().contains("30"));
        assertEquals(0, guardados);
    }

    @Test
    void bajar_a_un_plan_donde_caben_se_permite() {
        SuscripcionService s = servicio(30, 2);
        s.actualizar(2, soloPlan(1));
        assertEquals(1, guardados);
        assertEquals(1, club.getPlanId());
    }

    @Test
    void limite_propio_menor_a_los_activos_se_rechaza() {
        SuscripcionService s = servicio(20, 2);
        assertThrows(RuntimeException.class, () -> s.actualizar(2, soloLimite(10)));
        assertEquals(0, guardados);
    }

    @Test
    void plan_ilimitado_siempre_cabe() {
        SuscripcionService s = servicio(500, 1);
        s.actualizar(2, soloPlan(3));
        assertEquals(1, guardados);
    }
}
