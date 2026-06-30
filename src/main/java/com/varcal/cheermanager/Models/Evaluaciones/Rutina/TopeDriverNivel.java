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

// Tope y valores de un DriverSubCriterio para un nivel específico. Ej: para
// N1/Dificultad de Elevaciones/Grado de Dificultad: topeMaximo=0.8,
// valorPorHabilidadApropiada=0.1, valorPorHabilidadAvanzadaOElite=0.2 (según
// la tabla "GRADO DE DIFICULTAD (0-0.8)" de la rúbrica Élite N1-N4). Para
// Prep, la misma fila tendría topeMaximo=0.6.
@Data
@NoArgsConstructor
@Entity
@Table(name = "tope_driver_nivel")
public class TopeDriverNivel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private DriverSubCriterio driver;

    @ManyToOne
    @JoinColumn(name = "nivel_id", nullable = false)
    private NivelCompetencia nivel;

    @Column(name = "tope_maximo", nullable = false, precision = 4, scale = 2)
    private BigDecimal topeMaximo;

    // Para "Grado de Dificultad": valor que suma cada habilidad de
    // dificultad básica/apropiada (ej. 0.1).
    @Column(name = "valor_nivel_basico", precision = 4, scale = 2)
    private BigDecimal valorNivelBasico;

    // Para "Grado de Dificultad": valor que suma cada habilidad avanzada/élite (ej. 0.2).
    // Para "Participación Máxima": no se usa este campo, ver valoresEscalon en su lugar.
    @Column(name = "valor_nivel_alto", precision = 4, scale = 2)
    private BigDecimal valorNivelAlto;
}