package com.varcal.cheermanager.DTO.Notificaciones;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Resultado (o vista previa) de un envio de recordatorios de mora.
 * estado de cada canal: ENVIADO | FALLIDO | SIN_DESTINO | MANUAL (WhatsApp sin API: usa el enlace) | PROGRAMADO (vista previa).
 */
public record RecordatorioMoraResultadoDTO(
        boolean simulacion,
        int facturasEnMora,
        int personas,
        int omitidosPorFrecuencia,
        Map<String, Integer> enviados,
        Map<String, Integer> fallidos,
        List<Aviso> avisos,
        List<Enlace> enlacesWhatsapp,
        List<String> advertencias) {

    public record Aviso(Integer personaId, String nombre, int facturas, BigDecimal saldo, List<CanalAviso> canales) {
    }

    public record CanalAviso(String canal, String destino, String estado, String detalle) {
    }

    public record Enlace(String nombre, String telefono, String url) {
    }
}
