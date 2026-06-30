package com.varcal.cheermanager.Models.Evaluaciones.Rutina;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// Escalones de un driver no-lineal, como "Participación Máxima":
//   orden=1, descripcion="Apropiada por MAX o Avanzada por GRAN PARTE", valor=0.3
//   orden=2, descripcion="Avanzada por MAX o Élite por GRAN PARTE",     valor=0.5
//   orden=3, descripcion="Élite por MAX",                                valor=0.7
// El juez selecciona cuál escalón aplica (ver RegistroDriverSubCriterio);
// esta tabla solo guarda los valores configurables por nivel, no decide cuál
// aplica — esa decisión la toma el juez directamente, ya que depende de su
// criterio sobre la dificultad observada, no de un conteo automatizable.
@Data
@NoArgsConstructor
@Entity
@Table(name = "escalon_driver_nivel")
public class EscalonDriverNivel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private DriverSubCriterio driver;

    @ManyToOne
    @JoinColumn(name = "nivel_id", nullable = false)
    private NivelCompetencia nivel;

    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "descripcion", nullable = false, length = 300)
    private String descripcion;

    @Column(name = "valor", nullable = false, precision = 4, scale = 2)
    private BigDecimal valor;
}