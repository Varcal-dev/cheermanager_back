package com.varcal.cheermanager.Models.Eventos;

import java.math.BigDecimal;

import com.varcal.cheermanager.Models.Evaluaciones.Rutina.EvaluacionRutina;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    // Antes era un Integer suelto sin relación real (premioId). Se corrige a
    // FK propiamente dicha para poder navegar el objeto Premio completo y que
    // JPA valide la integridad referencial. Nullable porque no todo resultado
    // implica premio (ej. un grupo que queda en posición 12 de 20).
    @ManyToOne
    @JoinColumn(name = "premio_id")
    private PremioEvento premio;

    // Enlaza el resultado (posición + puntaje oficial del evento) con el
    // desglose técnico completo que ya calculó el motor de EvaluacionRutina,
    // si es que esa competencia se evaluó dentro del sistema (no todas lo
    // estarán, ej. eventos de otros productores donde solo se registra el
    // resultado final). Nullable a propósito: permite cargar resultados
    // históricos/externos sin exigir que exista una EvaluacionRutina.
    @ManyToOne
    @JoinColumn(name = "evaluacion_rutina_id")
    private EvaluacionRutina evaluacionRutina;

    @Column(name = "observaciones")
    private String observaciones;
}