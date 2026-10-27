package com.varcal.cheermanager.DTO.Financiero;

import java.math.BigDecimal;
import java.util.List;

/** Resultado (o vista previa) del cobro de un evento por lote. */
public record CobroEventoLoteResultadoDTO(
        boolean simulacion,
        Integer eventoId,
        String eventoNombre,
        BigDecimal montoPorDeportista,
        int totalACobrar,
        BigDecimal valorTotal,
        int yaCobrados,
        List<Linea> cobrados,
        List<Linea> omitidos) {

    /** Una persona del lote. motivo solo viene en los omitidos. */
    public record Linea(Integer personaId, String nombre, String grupo, String numeroFactura, String motivo) {
    }
}