package com.varcal.cheermanager.Models.Inventario;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "detalles_venta")
public class DetalleVenta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Antes apuntaba directo a Factura y nunca a Venta — no existía forma de
    // agrupar las líneas de una misma venta. Se corrige: cada línea
    // pertenece a UNA Venta, y es la Venta la que opcionalmente se liga a
    // una Factura (ver Venta.factura).
    @ManyToOne
    @JoinColumn(name = "venta_id", nullable = false)
    private Venta venta;

    @ManyToOne
    @JoinColumn(name = "producto_variante_id", nullable = false)
    private ProductoVariante productoVariante;

    @Column(name = "cantidad", nullable = false)
    private Integer cantidad;

    // Precio unitario AL MOMENTO de la venta — se copia desde
    // ProductoVariante.precioVenta al crear el detalle. Si el precio del
    // producto cambia después, esta venta histórica no debe verse afectada.
    @Column(name = "precio_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitario;

    @Column(name = "subtotal", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;
}