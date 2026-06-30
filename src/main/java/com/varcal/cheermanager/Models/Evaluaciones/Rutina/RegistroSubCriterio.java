package com.varcal.cheermanager.Models.Evaluaciones.Rutina;

import java.math.BigDecimal;
import java.util.List;

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

// Lo que el juez realmente ingresa para UN sub-criterio (ej. "Dificultad de
// Elevaciones") de UNA evaluación de rutina. Los campos de "datos crudos" no
// son todos obligatorios — cada CalculadoraSubCriterio (en código) solo lee
// los que necesita según el tipoCalculo del SubCriterioRubrica. Ver el
// catálogo TipoCalculoSubCriterio para qué campos usa cada tipo:
//
//   ESCALON_HABILIDADES_GRUPOS / ESCALON_GIMNASIA:
//     usa habilidadesDiferentes + grupoMinimoAlcanzado
//   ESCALON_CANTIDAD_SIMPLE:
//     usa habilidadesDiferentes + grupoMinimoAlcanzado (más simple, sin drivers)
//   VALOR_INICIAL_MENOS_DRIVERS:
//     usa los RegistroDeduccionDriver asociados (uno por driver: Flyer,
//     Bases/Spotters, Transiciones, Sincronización, etc.)
//   RANGO_DIRECTO_JUEZ:
//     usa directamente puntajeFinalManual, sin pasar por escalones
@Data
@NoArgsConstructor
@Entity
@Table(name = "registro_sub_criterio")
public class RegistroSubCriterio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "evaluacion_rutina_id", nullable = false)
    private EvaluacionRutina evaluacionRutina;

    @ManyToOne
    @JoinColumn(name = "sub_criterio_id", nullable = false)
    private SubCriterioRubrica subCriterio;

    // --- Datos crudos para tipos ESCALON_* ---
    @Column(name = "habilidades_diferentes")
    private Integer habilidadesDiferentes;

    // "MAYORIA" | "GRAN_PARTE" | "MAXIMO" — qué columna de TablaCantidadNivel
    // alcanzó el grupo con esa cantidad de habilidades diferentes.
    @Column(name = "grupo_minimo_alcanzado", length = 20)
    private String grupoMinimoAlcanzado;

    // --- Datos crudos para tipo RANGO_DIRECTO_JUEZ ---
    // El juez decide el valor final directamente (ej. Showmanship = 1.7),
    // sin que el sistema lo calcule por fórmula. El sistema solo valida que
    // esté dentro de [valorMinimo, valorMaximo] del SubCriterioRubrica.
    @Column(name = "puntaje_final_manual", precision = 4, scale = 2)
    private BigDecimal puntajeFinalManual;

    // --- Resultado calculado (lo llena el sistema, no el juez) ---
    @Column(name = "puntaje_base", precision = 4, scale = 2)
    private BigDecimal puntajeBase;

    @Column(name = "puntaje_drivers", precision = 4, scale = 2)
    private BigDecimal puntajeDrivers;

    @Column(name = "puntaje_final", precision = 4, scale = 2)
    private BigDecimal puntajeFinal;

    @OneToMany(mappedBy = "registroSubCriterio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistroDeduccionDriver> deduccionesDriver;

    @OneToMany(mappedBy = "registroSubCriterio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RegistroDriverSubCriterio> registrosDriver;
}