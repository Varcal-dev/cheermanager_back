package com.varcal.cheermanager.Controller.Clubes;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Service.Clubes.WompiService;
import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.Utils.SinPermisoEspecifico;
import com.varcal.cheermanager.tenant.TenantContext;

/**
 * Pago en linea de la suscripcion con Wompi (V46).
 *  - GET  /api/suscripcion/pagar/estado: si el pago esta disponible y cuanto se cobra.
 *  - POST /api/suscripcion/pagar: inicia el pago de N meses y devuelve la URL del checkout. Esta ruta esta exenta del
 *    bloqueo de solo lectura: un club vencido tiene que poder pagar.
 *  - GET  /api/suscripcion/pagar/{referencia}: estado de un pago del propio club (la pagina lo consulta al volver).
 *  - POST /api/wompi/eventos: webhook de Wompi, sin sesion; se acepta solo con checksum valido.
 */
@RestController
@RequestMapping("/api")
public class PagosEnLineaController {

    private static final Logger log = LoggerFactory.getLogger(PagosEnLineaController.class);

    public record PagarRequest(
            @jakarta.validation.constraints.NotNull(message = "Indica cuantos meses pagar") @jakarta.validation.constraints.Min(value = 1, message = "Minimo 1 mes") @jakarta.validation.constraints.Max(value = 12, message = "Maximo 12 meses") Integer meses) {
    }

    private final WompiService wompi;

    public PagosEnLineaController(WompiService wompi) {
        this.wompi = wompi;
    }

    @GetMapping("/suscripcion/pagar/estado")
    @RequiresPermission("pagar_suscripcion")
    public WompiService.Estado estado() {
        return wompi.estado(TenantContext.require());
    }

    @PostMapping("/suscripcion/pagar")
    @RequiresPermission("pagar_suscripcion")
    public WompiService.Cobro pagar(@jakarta.validation.Valid @RequestBody PagarRequest request) {
        String quien = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getName()
                : null;
        return wompi.crearCobro(TenantContext.require(), request.meses() == null ? 1 : request.meses(), quien);
    }

    @GetMapping("/suscripcion/pagar/{referencia}")
    @RequiresPermission("pagar_suscripcion")
    public WompiService.Transaccion consultar(@PathVariable String referencia) {
        return wompi.transaccionDelClub(TenantContext.require(), referencia);
    }

    @PostMapping("/wompi/eventos")
    @SinPermisoEspecifico("lo llama Wompi sin sesion; se valida con el checksum del evento (secreto de eventos)")
    public ResponseEntity<Void> evento(@RequestBody Map<String, Object> evento,
            @RequestHeader(value = "X-Event-Checksum", required = false) String checksum) {
        try {
            String resultado = wompi.procesarEvento(evento, checksum);
            log.info("Evento Wompi procesado: {}", resultado);
            return ResponseEntity.ok().build();
        } catch (SecurityException e) {
            log.warn("Evento Wompi rechazado: checksum invalido");
            return ResponseEntity.status(401).build();
        }
    }
}
