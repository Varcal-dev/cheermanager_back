package com.varcal.cheermanager.Models.Evaluaciones.Rutina;

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

// Lo que el juez marca para un Driver (Grado de Dificultad o Participación
// Máxima) dentro de un RegistroSubCriterio de Construcciones. Para "Grado de
// Dificultad": una fila por cada habilidad (1, 2, 3, 4), marcando si fue
// Apropiada o Avanzada/Élite (ver TopeDriverNivel.valorNivelBasico/AltO).
// Para "Participación Máxima": una sola fila, indicando qué EscalonDriverNivel
// aplica (ver `escalonSeleccionado`).
@Data
@NoArgsConstructor
@Entity
@Table(name = "registro_driver_sub_criterio")
public class RegistroDriverSubCriterio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "registro_sub_criterio_id", nullable = false)
    private RegistroSubCriterio registroSubCriterio;

    @ManyToOne
    @JoinColumn(name = "driver_id", nullable = false)
    private DriverSubCriterio driver;

    // Solo para "Grado de Dificultad": número de habilidad (1, 2, 3, 4...).
    @Column(name = "numero_habilidad")
    private Integer numeroHabilidad;

    // Solo para "Grado de Dificultad": true = nivel alto (Avanzada/Élite),
    // false = nivel básico (Apropiada). Determina si se usa
    // TopeDriverNivel.valorNivelBasico o valorNivelAlto.
    @Column(name = "es_nivel_alto")
    private Boolean esNivelAlto;

    // Solo para "Participación Máxima": qué EscalonDriverNivel.id seleccionó
    // el juez (0.3 / 0.5 / 0.7 según el caso).
    @ManyToOne
    @JoinColumn(name = "escalon_seleccionado_id")
    private EscalonDriverNivel escalonSeleccionado;
}