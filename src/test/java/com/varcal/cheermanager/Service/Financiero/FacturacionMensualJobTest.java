package com.varcal.cheermanager.Service.Financiero;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.DTO.Financiero.ResumenGeneracionCobrosDTO;
import com.varcal.cheermanager.Service.Org_dep.InscripcionService;
import com.varcal.cheermanager.Utils.CalendarioHabil;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.tenant.TenantLookupRepository;

/** Facturacion mensual automatica: cuando corre, a quien, y que un club con fallas no frena a los demas. */
class FacturacionMensualJobTest {

    static class LookupFalso extends TenantLookupRepository {
        final List<Integer> clubes;
        LookupFalso(List<Integer> clubes) { this.clubes = clubes; }
        @Override public List<Integer> idsClubesActivos() { return clubes; }
    }

    static class InscripcionesFalso extends InscripcionService {
        final List<String> llamadas = new ArrayList<>();
        final List<Integer> clubesVistos = new ArrayList<>();
        final List<Integer> fallan;
        InscripcionesFalso(List<Integer> fallan) { this.fallan = fallan; }
        @Override
        public ResumenGeneracionCobrosDTO generarCobrosMensualidad(String periodo) {
            int club = TenantContext.get();
            clubesVistos.add(club);
            llamadas.add(club + ":" + periodo);
            if (fallan.contains(club)) throw new RuntimeException("club " + club + " con datos danados");
            ResumenGeneracionCobrosDTO r = new ResumenGeneracionCobrosDTO();
            r.setPeriodo(periodo);
            r.setFacturasGeneradas(3);
            return r;
        }
    }

    static final YearMonth NOV = YearMonth.of(2026, 11);

    @Test
    void antesDelPrimerDiaHabilNoFactura() {
        InscripcionesFalso insc = new InscripcionesFalso(List.of());
        FacturacionMensualJob job = new FacturacionMensualJob(new LookupFalso(List.of(2)), insc, true);
        job.ejecutarPara(CalendarioHabil.primerDiaHabil(NOV).minusDays(1));
        assertEquals(0, insc.llamadas.size());
    }

    @Test
    void elPrimerDiaHabilFacturaElPeriodoDelMesParaCadaClub() {
        InscripcionesFalso insc = new InscripcionesFalso(List.of());
        FacturacionMensualJob job = new FacturacionMensualJob(new LookupFalso(List.of(2, 3)), insc, true);
        job.ejecutarPara(CalendarioHabil.primerDiaHabil(NOV));
        assertEquals(List.of("2:2026-11", "3:2026-11"), insc.llamadas);
    }

    @Test
    void sigueFacturandoDespuesDelPrimerDiaHabilParaPonerseAlDia() {
        InscripcionesFalso insc = new InscripcionesFalso(List.of());
        FacturacionMensualJob job = new FacturacionMensualJob(new LookupFalso(List.of(2)), insc, true);
        job.ejecutarPara(LocalDate.of(2026, 11, 20));
        assertEquals(List.of("2:2026-11"), insc.llamadas);
    }

    @Test
    void unClubQueFallaNoImpideFacturarAlosDemas() {
        InscripcionesFalso insc = new InscripcionesFalso(List.of(2));
        FacturacionMensualJob job = new FacturacionMensualJob(new LookupFalso(List.of(2, 3, 4)), insc, true);
        job.ejecutarPara(LocalDate.of(2026, 11, 20));
        assertEquals(List.of(2, 3, 4), insc.clubesVistos);
    }

    @Test
    void apagadaNoHaceNada() {
        InscripcionesFalso insc = new InscripcionesFalso(List.of());
        FacturacionMensualJob job = new FacturacionMensualJob(new LookupFalso(List.of(2)), insc, false);
        job.ejecutarPara(LocalDate.of(2026, 11, 20));
        assertEquals(0, insc.llamadas.size());
    }

    @Test
    void cadaClubSeFacturaDentroDeSuPropioContextoYDespuesSeRestaura() {
        InscripcionesFalso insc = new InscripcionesFalso(List.of());
        FacturacionMensualJob job = new FacturacionMensualJob(new LookupFalso(List.of(5)), insc, true);
        TenantContext.set(null);
        job.ejecutarPara(LocalDate.of(2026, 11, 20));
        assertEquals(List.of(5), insc.clubesVistos);
        assertFalse(TenantContext.isSet(), "el contexto de club no debe quedar puesto al terminar");
    }
}
