package com.varcal.cheermanager.Service.Financiero;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.varcal.cheermanager.DTO.Financiero.ResumenGeneracionCobrosDTO;
import com.varcal.cheermanager.Service.Org_dep.InscripcionService;
import com.varcal.cheermanager.Utils.CalendarioHabil;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.tenant.TenantLookupRepository;

/**
 * Facturacion mensual automatica: desde el primer dia habil del mes (Colombia) genera, para cada
 * club activo, la factura de mensualidad de todas las inscripciones activas.
 *
 * Corre TODOS los dias a las 6:00 (hora de Bogota) y es idempotente: InscripcionService no duplica
 * un periodo ya facturado. Asi, si el servidor estuvo caido el primer dia habil, el siguiente dia
 * "se pone al dia" solo. El boton manual de Pagos sigue existiendo para casos puntuales.
 *
 * Se apaga con FACTURACION_AUTOMATICA=false. Pensado para UNA sola instancia del backend.
 */
@Component
public class FacturacionMensualJob {

    private static final Logger log = LoggerFactory.getLogger(FacturacionMensualJob.class);
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");

    private final TenantLookupRepository lookup;
    private final InscripcionService inscripcionService;
    private final boolean activa;

    public FacturacionMensualJob(TenantLookupRepository lookup, InscripcionService inscripcionService,
            @Value("${app.facturacion.automatica:true}") boolean activa) {
        this.lookup = lookup;
        this.inscripcionService = inscripcionService;
        this.activa = activa;
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "America/Bogota")
    public void ejecutar() {
        ejecutarPara(LocalDate.now(BOGOTA));
    }

    /** Separado de ejecutar() para poder probarlo con una fecha concreta. */
    public void ejecutarPara(LocalDate hoy) {
        if (!activa) {
            return;
        }
        YearMonth periodo = YearMonth.from(hoy);
        if (hoy.isBefore(CalendarioHabil.primerDiaHabil(periodo))) {
            return; // todavia no es el primer dia habil del mes
        }

        for (Integer clubId : lookup.idsClubesActivos()) {
            try {
                ResumenGeneracionCobrosDTO r = TenantContext.runAs(clubId, () -> {
                    return inscripcionService.generarCobrosMensualidad(periodo.toString());
                });
                if (r.getFacturasGeneradas() > 0 || !r.getErrores().isEmpty()) {
                    log.info("Facturacion {} club {}: {} generadas, {} ya facturadas, {} errores",
                            periodo, clubId, r.getFacturasGeneradas(), r.getYaFacturadas(), r.getErrores().size());
                }
                for (String error : r.getErrores()) {
                    log.warn("Facturacion {} club {}: {}", periodo, clubId, error);
                }
            } catch (Exception e) {
                // Un club con problemas no debe impedir que se facture a los demas.
                log.error("Facturacion {} club {} fallo: {}", periodo, clubId, e.getMessage(), e);
            }
        }
    }
}
