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

// Un escalón de puntaje de un sub-criterio para un nivel específico. Ej: para
// N1/Dificultad de Elevaciones, escalón "orden=2" podría representar el
// renglón "3.0 = 4 habilidades diferentes Apropiadas realizadas por GRAN
// PARTE del equipo" de la rúbrica. La calculadora del sub-criterio (en
// código) recorre estos escalones en orden y elige el que corresponda según
// los datos crudos que ingresó el juez — el VALOR del escalón es dato
// editable, el CRITERIO para elegir entre escalones es lógica de la
// calculadora.
//
// `condicionHabilidadesDiferentes` y `condicionGrupoMinimo` son los datos
// numéricos que la calculadora compara contra lo reportado por el juez
// (ver RegistroSubCriterio). No todos los tipos de cálculo usan ambos campos
// — para sub-criterios de tipo RANGO_DIRECTO_JUEZ esta tabla ni se consulta,
// el juez ingresa el valor final directamente dentro del rango
// [valorMinimo, valorMaximo] del SubCriterioRubrica correspondiente (ver nota
// en EvaluacionRutinaService).
@Data
@NoArgsConstructor
@Entity
@Table(name = "tope_seccion_nivel")
public class TopeSeccionNivel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "nivel_id", nullable = false)
    private NivelCompetencia nivel;

    @ManyToOne
    @JoinColumn(name = "sub_criterio_id", nullable = false)
    private SubCriterioRubrica subCriterio;

    // Orden ascendente del escalón dentro del sub-criterio (1 = el más bajo).
    // La calculadora evalúa de mayor a menor y se queda con el primer
    // escalón cuyas condiciones se cumplan.
    @Column(name = "orden", nullable = false)
    private Integer orden;

    @Column(name = "valor_puntaje", nullable = false, precision = 4, scale = 2)
    private BigDecimal valorPuntaje;

    // Mínimo de habilidades diferentes que deben cumplirse para este escalón
    // (null si el sub-criterio no usa este criterio, ej. Dificultad de Saltos).
    @Column(name = "condicion_habilidades_diferentes")
    private Integer condicionHabilidadesDiferentes;

    // Cuántos grupos (MAYORIA/GRAN_PARTE/MAXIMO de TablaCantidadNivel) deben
    // alcanzar esa cantidad de habilidades para este escalón.
    @Column(name = "condicion_grupo_minimo", length = 20)
    private String condicionGrupoMinimo;

    // Texto literal de la rúbrica para este escalón, solo para trazabilidad
    // y auditoría visual (ej. "4 habilidades diferentes Apropiadas del nivel
    // realizadas por GRAN PARTE|MOST del equipo") — no se usa en el cálculo.
    @Column(name = "descripcion_rubrica", length = 500)
    private String descripcionRubrica;
}