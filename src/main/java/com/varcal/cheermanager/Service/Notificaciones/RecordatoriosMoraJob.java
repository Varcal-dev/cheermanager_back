package com.varcal.cheermanager.Service.Notificaciones;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.varcal.cheermanager.DTO.Notificaciones.RecordatorioMoraResultadoDTO;
import com.varcal.cheermanager.Service.Clubes.FuncionesService;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.tenant.TenantLookupRepository;

/**
 * Recordatorios de mora automaticos: todos los dias a las 8:00 (hora de Bogota), para cada club activo que
 * haya activado el envio automatico, avisa de las facturas en mora por los canales que el club marco.
 * La frecuencia por factura y canal (dias_entre_avisos) evita avisar todos los dias lo mismo.
 *
 * Se apaga del todo con RECORDATORIOS_AUTOMATICOS=false. Pensado para UNA sola instancia del backend.
 */
@Component
public class RecordatoriosMoraJob {

    private static final Logger log = LoggerFactory.getLogger(RecordatoriosMoraJob.class);

    private final TenantLookupRepository lookup;
    private final RecordatorioMoraService service;
    private final FuncionesService funciones;
    private final boolean activa;

    public RecordatoriosMoraJob(TenantLookupRepository lookup, RecordatorioMoraService service,
                                FuncionesService funciones,
                                @Value("${app.recordatorios.automaticos:true}") boolean activa) {
        this.lookup = lookup;
        this.service = service;
        this.funciones = funciones;
        this.activa = activa;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Bogota")
    public void ejecutar() {
        if (!activa) {
            return;
        }
        for (Integer clubId : lookup.idsClubesActivos()) {
            try {
                // El envio manual ya lo frena el filtro de planes; la tarea automatica no pasa por el filtro, asi que
                // consulta el plan ella misma. Un club sin la funcion (o en su ventana de solo lectura) no recibe envios.
                if (!funciones.plena(clubId, "RECORDATORIOS_MORA")) {
                    continue;
                }
                RecordatorioMoraResultadoDTO r = TenantContext.runAs(clubId, () -> service.ejecutarAutomatico());
                if (r != null && (!r.enviados().isEmpty() || !r.fallidos().isEmpty())) {
                    log.info("Recordatorios de mora club {}: enviados {}, fallidos {}", clubId, r.enviados(), r.fallidos());
                }
            } catch (Exception e) {
                // Un club con problemas no debe impedir avisar a los demas.
                log.error("Recordatorios de mora club {} fallo: {}", clubId, e.getMessage(), e);
            }
        }
    }
}