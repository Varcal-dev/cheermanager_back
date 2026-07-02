package com.varcal.cheermanager.Models.Evaluaciones.Rutina;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// La evaluación de UNA rutina completa de UN grupo, en UNA fecha/evento
// (puede ser una competencia real o un simulacro/práctica interna). El
// puntaje final se compone de los RegistroSubCriterio asociados — ver
// EvaluacionRutinaService para el detalle del cálculo y la suma final.
@Data
@NoArgsConstructor
@Entity
@Table(name = "evaluaciones_rutina")
public class EvaluacionRutina {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "grupo_entrenamiento_id", nullable = false)
    private GrupoEntrenamiento grupo;

    @ManyToOne
    @JoinColumn(name = "nivel_id", nullable = false)
    private NivelCompetencia nivel;

    @Column(name = "fecha", nullable = false)
    private LocalDate fecha;

    // Nombre libre del evento o "Práctica interna" / "Simulacro" si no es una
    // competencia oficial — el sistema sirve igual para preparación. Se
    // conserva para no romper evaluaciones ya cargadas y para el caso de
    // simulacros que nunca deberían crear un registro en `eventos`.
    @Column(name = "evento", length = 200)
    private String evento;

    // FK opcional al Evento real del módulo Eventos, cuando esta evaluación
    // corresponde a una competencia oficial ya registrada en el sistema (no
    // a un simulacro/práctica interna). Permite construir la progresión de
    // puntaje de un grupo evento-a-evento sin depender de que el texto libre
    // de arriba coincida exactamente. Se deja nullable y separado del campo
    // `evento` (String) a propósito, para no forzar una migración de datos
    // sobre evaluaciones ya existentes.
    @ManyToOne
    @JoinColumn(name = "evento_oficial_id")
    private Evento eventoOficial;

    // Cantidad de atletas que participaron en ESTA presentación específica
    // (puede no coincidir con el total de DeportistaPerteneceGrupo si alguien
    // no asistió ese día) — necesario porque las tablas de cantidad dependen
    // del número real de atletas en el piso.
    @Column(name = "cantidad_atletas", nullable = false)
    private Integer cantidadAtletas;

    // Suma final de todos los RegistroSubCriterio. Se recalcula cada vez que
    // se agrega/edita un registro (ver EvaluacionRutinaService.recalcularTotal).
    @Column(name = "puntaje_total", precision = 6, scale = 2)
    private BigDecimal puntajeTotal;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @OneToMany(mappedBy = "evaluacionRutina", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistroSubCriterio> registros;
}