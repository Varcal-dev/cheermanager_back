package com.varcal.cheermanager.DTO.Inventario;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

import lombok.Data;

@Data
public class VentaDTO {
    // Exactamente uno de los dos: personaId (comprador del club) o
    // nombreCompradorExterno (comprador externo). El service rechaza si
    // vienen ambos o ninguno.
    private Integer personaId;
    @Size(max = 150, message = "El nombre del comprador admite maximo 150 caracteres")
    private String nombreCompradorExterno;
    @Size(max = 150, message = "El contacto del comprador admite maximo 150 caracteres")
    private String contactoCompradorExterno;

    @Size(max = 30, message = "El metodo de pago admite maximo 30 caracteres")
    private String metodoPago;
    private Integer facturaId; // opcional, solo si se carga a la cuenta del deportista

    // Solo aplica a compradores del club sin facturaId. null o true = pagada al momento (se crea el pago
    // "Pagado" sobre la factura de tienda); false = se deja a cuenta (factura pendiente, vence a 10 dias).
    private Boolean pagada;

    @NotEmpty(message = "La venta debe tener al menos un item")
    @Valid
    private List<ItemVentaDTO> items;

    @Data
    public static class ItemVentaDTO {
        @NotNull(message = "Indica la variante del producto")
        private Integer productoVarianteId;
        @NotNull(message = "Indica la cantidad")
        @Positive(message = "La cantidad debe ser mayor a cero")
        private Integer cantidad;
    }
}