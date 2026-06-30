package com.varcal.cheermanager.Models.Evaluaciones.Rutina;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

// Catálogo de los sub-criterios evaluables de la rúbrica (Dificultad de
// Elevaciones, Dificultad de Pirámides, Dificultad de Saltos, Ejecución de
// Elevaciones/Pirámides/Gimnasia, Formaciones y Transiciones, etc.).
//
// El campo `tipoCalculo` identifica qué CalculadoraSubCriterio (en el código,
// no en la BD) sabe procesar este sub-criterio — ver
// Service/Evaluaciones/Rutina/calculadoras/. Agregar un sub-criterio nuevo a
// esta tabla NO activa su cálculo automáticamente: requiere también su
// calculadora en código. Esto es deliberado (ver TipoCalculoSubCriterio):
// las fórmulas oficiales de la rúbrica son lógica, no configuración, y
// cambian solo si FEDECOLCHEER cambia la metodología.
@Data
@NoArgsConstructor
@Entity
@Table(name = "sub_criterios_rubrica")
public class SubCriterioRubrica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Ej: "Dificultad de Elevaciones", "Dificultad de Pirámides",
    // "Dificultad de Lanzamientos", "Dificultad de Saltos",
    // "Dificultad de Gimnasia Estática", "Dificultad de Gimnasia con Carrera",
    // "Ejecución Elevaciones/Pirámides/Gimnasia", "Ejecución Lanzamientos/Saltos",
    // "Formaciones y Transiciones", "Creatividad de Rutina", "Dance Dificultad",
    // "Dance Ejecución", "Showmanship"
    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;

    // Construcciones | Gimnasia | Ejecucion | General
    @Column(name = "seccion", nullable = false)
    private String seccion;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_calculo", nullable = false)
    private TipoCalculoSubCriterio tipoCalculo;

    public enum TipoCalculoSubCriterio {
        // Escalón por # habilidades diferentes + grupos sincronizados, más
        // drivers de grado de dificultad y participación máxima.
        // (Dificultad de Elevaciones, Dificultad de Pirámides)
        ESCALON_HABILIDADES_GRUPOS,

        // Escalón simple por # habilidades conectadas/sincronizadas según
        // tabla de cantidad. (Dificultad de Saltos, Dificultad de Lanzamientos)
        ESCALON_CANTIDAD_SIMPLE,

        // Escalón por habilidad apropiada/avanzada/élite según tabla de
        // cantidad, más grado de dificultad acumulable.
        // (Dificultad de Gimnasia Estática/con Carrera)
        ESCALON_GIMNASIA,

        // Valor inicial fijo menos deducciones por driver (0.1/0.2/0.3 cada
        // uno, con tope configurable de deducción máxima por driver).
        // (Ejecución de cualquier sub-sección)
        VALOR_INICIAL_MENOS_DRIVERS,

        // Rango simple, valor decidido directamente por el juez sin fórmula
        // (Formaciones y Transiciones, Creatividad de Rutina, Dance,
        // Showmanship — estos dependen de percepción del juez, no de conteo).
        RANGO_DIRECTO_JUEZ
    }
}