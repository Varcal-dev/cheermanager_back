package com.varcal.cheermanager.Models.Evaluaciones;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "criterios_evaluacion")
public class CriterioEvaluacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "categoria_id", nullable = false)
    private Integer categoriaId;
}