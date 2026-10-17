package com.varcal.cheermanager.Models.Inventario;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

// La combinación real que se vende y de la que se controla stock: una talla
// + color de una camiseta, un tier de membresía digital, etc. Un Producto
// sin variantes reales (ej. "Sticker pack", talla única) igual tiene UNA
// ProductoVariante — así toda la lógica de precio/stock/venta pasa siempre
// por aquí, sin casos especiales para productos "simples".
@Data
@NoArgsConstructor
@Entity
@Table(name = "producto_variantes", uniqueConstraints = {@jakarta.persistence.UniqueConstraint(name = "uk_productovariante_club_sku_variante", columnNames = {"club_id", "sku_variante"})})
public class ProductoVariante {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    // Nullable: no todo artículo tiene talla (ej. llavero, sticker pack).
    @Column(name = "talla", length = 20)
    private String talla;

    // Nullable: no todo artículo tiene color, o el color es fijo del producto.
    // Para DIG también se usa este campo para el "tier" (Básico/Competencia/Elite).
    @Column(name = "color", length = 30)
    private String color;

    // SKU completo para etiqueta/código de barras, ej. RAT-ROP-CAE-001-NEG-M.
    // Se genera en el service a partir del SKU del producto + secuencia.
    @Column(name = "sku_variante", length = 40)
    private String skuVariante;

    // Precio de venta real (a diferencia del rango de referencia en Producto).
    @Column(name = "precio_venta", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioVenta;

    // Nullable cuando producto.requiereControlStock = false (artículos
    // digitales) — no tiene sentido llevar unidades de una membresía.
    @Column(name = "stock_actual")
    private Integer stockActual;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
