package com.varcal.cheermanager.Models.Eventos;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;

@Data
@NoArgsConstructor
@Entity
@Table(name = "resultados_competencia")
public class ResultadoCompetencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne
    @JoinColumn(name = "grupo_id")
    private GrupoEntrenamiento grupoEntrenamiento;

    @Column(name = "posicion")
    private Integer posicion;

    @Column(name = "puntaje", precision = 10, scale = 2)
    private BigDecimal puntaje;

    @Column(name = "premio_id")
    private Integer premioId;

    @Column(name = "observaciones")
    private String observaciones;
}