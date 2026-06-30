package com.varcal.cheermanager.Models.Evaluaciones.Rutina;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// Una deducción de Ejecución. La rúbrica define drivers fijos por
// sub-sección (Flyer, Bases/Spotters, Transiciones, Sincronización para
// Elevaciones/Pirámides; Aproximación, Control del Cuerpo, Aterrizajes,
// Sincronización para Gimnasia; etc.) — estos nombres de driver NO están en
// una tabla configurable porque son fijos en la metodología de la rúbrica;
// viven como un enum simple aquí. Lo que SÍ varía es el tope de deducción
// (normalmente 0.3, pero 0.1 para Sincronización de Saltos y Altura de
// Lanzamientos) — ver CalculadoraEjecucion para esa excepción puntual citada
// en la rúbrica ("No puede descontarse más de 0.1").
@Data
@NoArgsConstructor
@Entity
@Table(name = "registro_deduccion_driver")
public class RegistroDeduccionDriver {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "registro_sub_criterio_id", nullable = false)
    private RegistroSubCriterio registroSubCriterio;

    // Flyer | Bases_Spotters | Transiciones | Sincronizacion | Aproximacion |
    // Control_Cuerpo | Aterrizajes | Posicion_Brazos | Posicion_Piernas | Altura
    @Column(name = "nombre_driver", nullable = false, length = 50)
    private String nombreDriver;

    // 0 = sin problema, 1 = -0.1 (menor), 2 = -0.2 (múltiples), 3 = -0.3 (generalizado)
    @Column(name = "nivel_deduccion", nullable = false)
    private Integer nivelDeduccion;
}