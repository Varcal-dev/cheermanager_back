package com.varcal.cheermanager.Service.Clubes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
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

/** S1b-3: cambiar el plan de un club abre las ventanas de solo lectura (FuncionesService.alCambiarPlan). */
class SuscripcionVentanasTest {

    static class LookupFalso extends TenantLookupRepository {
        @Override
        public long contarDeportistasActivos(int clubId) { return 5; }
    }

    static PlanSaas plan(int id) {
        PlanSaas p = new PlanSaas();
        p.setId(id);
        p.setCodigo("P" + id);
        p.setNombre("Plan " + id);
        p.setMaxDeportistas(100);
        return p;
    }

    final List<String> llamadas = new ArrayList<>();

    SuscripcionService servicio(Integer planActual) {
        Club club = new Club();
        club.setId(2);
        club.setNombre("Club");
        club.setSlug("club");
        club.setPlanId(planActual);
        Repo clubes = new Repo().on("findById", a -> Optional.of(club)).on("save", a -> a[0]);
        Repo planes = new Repo().on("findById", a -> Optional.of(plan((Integer) a[0])));
        SuscripcionService s = new SuscripcionService(Fakes.proxy(ClubRepository.class, clubes),
                Fakes.proxy(PlanSaasRepository.class, planes), Fakes.proxy(SuscripcionPagoRepository.class, new Repo()),
                new LookupFalso());
        s.setFunciones(new FuncionesService(null) {
            @Override
            public void alCambiarPlan(int clubId, Integer anterior, Integer nuevo) {
                llamadas.add(clubId + ":" + anterior + "->" + nuevo);
            }
        });
        return s;
    }

    SuscripcionService.ActualizarRequest soloPlan(int planId) {
        return new SuscripcionService.ActualizarRequest(planId, null, null, null, null, null, null, null);
    }

    @Test
    void cambiarDePlanAvisaConElPlanAnteriorYElNuevo() {
        servicio(3).actualizar(2, soloPlan(1));
        assertEquals(List.of("2:3->1"), llamadas);
    }

    @Test
    void editarOtrosDatosNoTocaLasVentanas() {
        servicio(3).actualizar(2, new SuscripcionService.ActualizarRequest(null, null, null, null, null, null, null, "nota"));
        assertTrue(llamadas.isEmpty());
    }
}
