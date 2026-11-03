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

    // Solo si la SEGUNDA habilidad vale distinto que la primera (Gimnasia Estatica Elite: 1ra 0.2/0.4,
    // 2da 0.4/0.6). Si son null, todas las habilidades usan valorNivelBasico/valorNivelAlto.
    @Column(name = "valor_basico_habilidad2", precision = 4, scale = 2)
    private BigDecimal valorBasicoHabilidad2;

    @Column(name = "valor_alto_habilidad2", precision = 4, scale = 2)
    private BigDecimal valorAltoHabilidad2;

    /** Valor que suma una habilidad segun su numero (1, 2...) y si fue de nivel alto. */
    public BigDecimal valorPara(Integer numeroHabilidad, boolean alto) {
        if (numeroHabilidad != null && numeroHabilidad == 2) {
            BigDecimal v2 = alto ? valorAltoHabilidad2 : valorBasicoHabilidad2;
            if (v2 != null) {
                return v2;
            }
        }
        return alto ? valorNivelAlto : valorNivelBasico;
    }
}
