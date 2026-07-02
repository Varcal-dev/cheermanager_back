package com.varcal.cheermanager.Models.Inventario;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "categorias_producto")
public class CategoriaProducto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    // 3 letras usadas en el SKU (RAT-[CAT]-...), ej. ROP, CAL, ACC, DIG.
    // Se valida en el service que siempre sean exactamente 3 letras
    // mayúsculas, para no romper el formato de nomenclatura del catálogo.
    @Column(name = "codigo", nullable = false, unique = true, length = 3)
    private String codigo;

    @Column(name = "descripcion")
    private String descripcion;
}