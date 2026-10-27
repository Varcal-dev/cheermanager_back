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

    // Solo aplica a compradores del club sin facturaId. null o true = pagada al momento (se crea el pago
    // "Pagado" sobre la factura de tienda); false = se deja a cuenta (factura pendiente, vence a 10 dias).
    private Boolean pagada;

    private List<ItemVentaDTO> items;

    @Data
    public static class ItemVentaDTO {
        private Integer productoVarianteId;
        private Integer cantidad;
    }
}