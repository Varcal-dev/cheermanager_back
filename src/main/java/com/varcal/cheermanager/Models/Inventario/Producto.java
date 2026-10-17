package com.varcal.cheermanager.Models.Inventario;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

// Ficha del artículo del catálogo (ej. "Camiseta entrenamiento"). NO tiene
// precio ni stock propios — esos viven en ProductoVariante, porque el
// catálogo real vende por talla/color con stock independiente por
// combinación. Precio aquí es solo un rango de referencia para costeo.
@Data
@NoArgsConstructor
@Entity
@Table(name = "productos", uniqueConstraints = {@jakarta.persistence.UniqueConstraint(name = "uk_producto_club_sku", columnNames = {"club_id", "sku"})})
public class Producto {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // SKU base del artículo, formato RAT-[CAT]-[COD] (sin el SEQ de 3
    // dígitos, ese vive en la variante si aplica). Ej: RAT-ROP-CAE.
    @Column(name = "sku", nullable = false, length = 20)
    private String sku;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "categoria_id", nullable = false)
    private CategoriaProducto categoriaProducto;

    // Rango de costo de referencia (lo que trae el catálogo de proveedores).
    // El precio de venta real, el que se cobra, vive en cada ProductoVariante
    // — puede diferir del costo por margen, y varía por talla/tier.
    @Column(name = "precio_min_referencia", precision = 10, scale = 2)
    private BigDecimal precioMinReferencia;

    @Column(name = "precio_max_referencia", precision = 10, scale = 2)
    private BigDecimal precioMaxReferencia;

    // NOTA: existía un campo "cantidadStock" acá que duplicaba la info de
    // stock, pero nunca lo tocaba ni la venta ni MovimientoInventarioService
    // (el stock real vive solo en ProductoVariante.stockActual, ver arriba).
    // Se quitó el mapeo porque daba una falsa sensación de estar fijando el
    // stock inicial desde el formulario de producto. La columna
    // "cantidad_stock" puede seguir existiendo en la BD (ddl-auto=update no
    // la borra sola) — si quieres limpiarla del todo, hazlo con una
    // migración manual: ALTER TABLE productos DROP COLUMN cantidad_stock.

    @Column(name = "origen")
    private String origen; // ej. "Medellín", "Bogotá", "Interno"

    @Column(name = "notas_proveedor", length = 500)
    private String notasProveedor;

    // false para los artículos digitales (categoría DIG: membresía, clase
    // suelta, foto/certificado digital) — no tienen unidades físicas, así
    // que no participan de MovimientoInventario ni de alertas de stock bajo.
    @Column(name = "requiere_control_stock", nullable = false)
    private Boolean requiereControlStock = true;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductoVariante> variantes;
}
