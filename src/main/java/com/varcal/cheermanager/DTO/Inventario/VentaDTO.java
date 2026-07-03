package com.varcal.cheermanager.DTO.Inventario;

import java.util.List;

import lombok.Data;

@Data
public class VentaDTO {
    // Exactamente uno de los dos: personaId (comprador del club) o
    // nombreCompradorExterno (comprador externo). El service rechaza si
    // vienen ambos o ninguno.
    private Integer personaId;
    private String nombreCompradorExterno;
    private String contactoCompradorExterno;

    private String metodoPago;
    private Integer facturaId; // opcional, solo si se carga a la cuenta del deportista

    private List<ItemVentaDTO> items;

    @Data
    public static class ItemVentaDTO {
        private Integer productoVarianteId;
        private Integer cantidad;
    }
}