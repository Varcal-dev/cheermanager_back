package com.varcal.cheermanager.Service.Evaluaciones.Rutina.Calculadoras;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaDTO.RegistroSubCriterioDTO;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.NivelCompetencia;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.RegistroSubCriterio;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.SubCriterioRubrica;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TopeSeccionNivel;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TopeSeccionNivelRepository;

// Implementa la lógica para sub-criterios cuyo valor final lo decide el juez
// directamente dentro de un rango (no hay fórmula de conteo):
//
//   - Formaciones y Transiciones:  1.0 - 2.0 (inicia en 2.0, -0.1 por cada
//     precisión faltante — el juez reporta el valor ya calculado)
//   - Creatividad de Rutina:       1.5 - 2.0 (promedio de 3 jueces)
//   - Dance Dificultad:            0.5 - 1.0
//   - Dance Ejecución:             0.5 - 1.0
//   - Showmanship:                 1.0 - 2.0 (promedio de 3 jueces)
//
// Esta calculadora solo valida que puntajeFinalManual esté dentro del rango
// configurado [valorMínimo, valorMáximo] del TopeSeccionNivel (orden=1 para
// mínimo, orden=2 para máximo). No hace ningún otro cálculo.
@Component
public class CalculadoraRangoDirectoJuez implements CalculadoraSubCriterio {

    @Autowired
    private TopeSeccionNivelRepository topeSeccionRepo;

    @Override
    public SubCriterioRubrica.TipoCalculoSubCriterio getTipo() {
        return SubCriterioRubrica.TipoCalculoSubCriterio.RANGO_DIRECTO_JUEZ;
    }

    @Override
    public RegistroSubCriterio calcular(RegistroSubCriterioDTO dto,
            SubCriterioRubrica subCriterio, NivelCompetencia nivel, Integer cantidadAtletas) {

        // Creatividad y Showmanship son el promedio de 3 jueces (Construccion, Gimnasia, General):
        // si vienen los puntajes por juez, se promedian (cada uno se valida contra el rango).
        // Si no, se usa el valor unico que ingreso el juez.
        List<BigDecimal> porJuez = dto.getPuntajesJueces() == null ? List.of()
                : dto.getPuntajesJueces().stream().filter(java.util.Objects::nonNull).toList();
        BigDecimal valor;
        if (!porJuez.isEmpty()) {
            if (porJuez.size() > 3) {
                throw new RuntimeException("'" + subCriterio.getNombre() + "' admite como maximo 3 puntajes de juez.");
            }
            valor = porJuez.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(BigDecimal.valueOf(porJuez.size()), 2, RoundingMode.HALF_UP);
        } else {
            valor = dto.getPuntajeFinalManual();
        }
        if (valor == null) {
            throw new RuntimeException("El sub-criterio '" + subCriterio.getNombre()
                    + "' requiere que el juez ingrese el puntaje directamente (puntajeFinalManual).");
        }

        // Validar que este dentro del rango configurado (cada puntaje de juez y el promedio).
        List<TopeSeccionNivel> rangos = topeSeccionRepo
                .findByNivelIdAndSubCriterioIdOrderByOrdenDesc(nivel.getId(), subCriterio.getId());

        if (!rangos.isEmpty()) {
            BigDecimal valorMax = rangos.stream().map(TopeSeccionNivel::getValorPuntaje)
                    .max(BigDecimal::compareTo).orElse(new BigDecimal("99"));
            BigDecimal valorMin = rangos.stream().map(TopeSeccionNivel::getValorPuntaje)
                    .min(BigDecimal::compareTo).orElse(BigDecimal.ZERO);

            List<BigDecimal> aValidar = new java.util.ArrayList<>(porJuez);
            aValidar.add(valor);
            for (BigDecimal v : aValidar) {
                if (v.compareTo(valorMin) < 0 || v.compareTo(valorMax) > 0) {
                    throw new RuntimeException("El valor " + v + " para '" + subCriterio.getNombre()
                            + "' esta fuera del rango permitido [" + valorMin + " - " + valorMax + "].");
                }
            }
        }

        RegistroSubCriterio registro = new RegistroSubCriterio();
        registro.setSubCriterio(subCriterio);
        registro.setPuntajeFinalManual(valor);
        registro.setPuntajeBase(valor);
        registro.setPuntajeDrivers(BigDecimal.ZERO);
        registro.setPuntajeFinal(valor.setScale(2, RoundingMode.HALF_UP));
        registro.setDetalleCalculo(porJuez.isEmpty()
                ? "Valor asignado directamente por el juez: " + valor
                : "Promedio de " + porJuez.size() + " juez(ces) " + porJuez + " = " + valor);
        return registro;
    }
}
