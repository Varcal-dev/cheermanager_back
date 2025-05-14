package com.varcal.cheermanager.Models.Org_dep;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "reglas_categoria")
public class ReglaCategoria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "categoria_nivel_id", nullable = false)
    private CategoriaNivel categoriaNivel;

    @Column(nullable = false)
    private Integer añoAplicacion;

    @Column
    private Integer añoNacimientoMin;

    @Column
    private Integer añoNacimientoMax;

    @Column
    private Integer cantidadMin;

    @Column
    private Integer cantidadMax;

    // Getters y setters
}
