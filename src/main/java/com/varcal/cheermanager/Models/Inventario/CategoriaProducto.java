package com.varcal.cheermanager.Models.Inventario;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "categorias_producto", uniqueConstraints = {@jakarta.persistence.UniqueConstraint(name = "uk_categoriaproducto_club_nombre", columnNames = {"club_id", "nombre"}), @jakarta.persistence.UniqueConstraint(name = "uk_categoriaproducto_club_codigo", columnNames = {"club_id", "codigo"})})
public class CategoriaProducto {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    // 3 letras usadas en el SKU (RAT-[CAT]-...), ej. ROP, CAL, ACC, DIG.
    // Se valida en el service que siempre sean exactamente 3 letras
    // mayúsculas, para no romper el formato de nomenclatura del catálogo.
    @Column(name = "codigo", nullable = false, length = 3)
    private String codigo;

    @Column(name = "descripcion")
    private String descripcion;
}