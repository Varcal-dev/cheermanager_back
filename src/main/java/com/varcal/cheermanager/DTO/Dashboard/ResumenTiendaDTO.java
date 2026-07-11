package com.varcal.cheermanager.DTO.Dashboard;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Data;

@Data
public class ResumenTiendaDTO {
    private BigDecimal ingresoMesActual;
    private long unidadesVendidasMesActual;
    private long ventasMesActual; // número de transacciones, no de unidades

    private Map<String, BigDecimal> ingresoPorCategoria; // ej. {"Ropa deportiva": 450000, "Digital / Servicios": 80000}

    private List<ProductoMasVendidoDTO> topProductosVendidos; // top 5 del mes, por unidades
    private List<VarianteStockBajoDTO> variantesConStockBajo;

    @Data
    public static class ProductoMasVendidoDTO {
        private String nombreProducto;
        private long unidadesVendidas;
        private BigDecimal ingresoGenerado;
    }

    @Data
    public static class VarianteStockBajoDTO {
        private Integer productoVarianteId;
        private String skuVariante;
        private String nombreProducto;
        private String talla;
        private String color;
        private Integer stockActual;
    }
}