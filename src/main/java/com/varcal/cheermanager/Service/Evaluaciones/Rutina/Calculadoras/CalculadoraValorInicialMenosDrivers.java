package com.varcal.cheermanager.Service.Evaluaciones.Rutina.Calculadoras;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaDTO.DeduccionDriverDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaDTO.RegistroSubCriterioDTO;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.NivelCompetencia;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.RegistroDeduccionDriver;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.RegistroSubCriterio;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.SubCriterioRubrica;

// Implementa la lógica de EJECUCIÓN para cualquier sub-sección:
//   - Elevaciones/Pirámides/Gimnasia: valor inicial 4.0
//   - Lanzamientos/Saltos:            valor inicial 2.0
//
// Ambos están configurados en TopeSeccionNivel (escalón único, orden=1) como
// valorPuntaje, así que no los hardcodeamos aquí — la calculadora lee ese
// valor de la tabla de configuración (ya es un escalón de orden=1 en la BD).
// Pero el VALOR INICIAL real de Ejecución viene en forma de un solo escalón
// en TopeSeccionNivel — para este tipo, la calculadora simplemente lo usa
// como base y aplica las deducciones.
//
// Rúbrica Élite N1-N4, sección EJECUCIÓN:
//   "Tiene un valor inicial y se reduce en 0.1, 0.2 o 0.3 basado en el grado
//   de deficiencia técnica de cada DRIVER."
//   "Se puede descontar un máximo de 0.3 por cada DRIVER. Excepto en:
//     - Sincronización de Saltos:       no puede descontarse más de 0.1
//     - Altura de Lanzamientos:         no puede descontarse más de 0.1"
//   (Regla fija en el código, no configurable, por decisión tuya.)
@Component
public class CalculadoraValorInicialMenosDrivers implements CalculadoraSubCriterio {

    // Drivers cuyo tope de deducción es 0.1 en vez del 0.3 estándar.
    // Fijo en código porque es una regla puntual de la metodología oficial
    // (no cambia por nivel ni por temporada ordinaria).
    private static final Set<String> DRIVERS_CON_TOPE_01 = Set.of(
            "Sincronizacion",       // En Saltos
            "Altura"                // En Lanzamientos
    );

    private static final BigDecimal TOPE_STANDARD = new BigDecimal("0.3");
    private static final BigDecimal TOPE_ESPECIAL = new BigDecimal("0.1");
    private static final BigDecimal DEC_01 = new BigDecimal("0.1");
    private static final BigDecimal DEC_02 = new BigDecimal("0.2");
    private static final BigDecimal DEC_03 = new BigDecimal("0.3");

    @Override
    public SubCriterioRubrica.TipoCalculoSubCriterio getTipo() {
        return SubCriterioRubrica.TipoCalculoSubCriterio.VALOR_INICIAL_MENOS_DRIVERS;
    }

    @Override
    public RegistroSubCriterio calcular(RegistroSubCriterioDTO dto,
            SubCriterioRubrica subCriterio, NivelCompetencia nivel, Integer cantidadAtletas) {

        // El valor inicial de Ejecución viene como puntajeFinalManual (el juez
        // lo tiene precargado según el sub-criterio: 4.0 o 2.0). Alternativamente
        // el service lo puede precargar al armar el DTO — aquí solo lo leemos.
        BigDecimal valorInicial = dto.getPuntajeFinalManual() != null
                ? dto.getPuntajeFinalManual()
                : BigDecimal.ZERO;

        BigDecimal totalDeduccion = BigDecimal.ZERO;
        List<RegistroDeduccionDriver> registrosDeduccion = new ArrayList<>();

        if (dto.getDeducciones() != null) {
            for (DeduccionDriverDTO ded : dto.getDeducciones()) {
                BigDecimal deduccionBruta = nivelADeduccion(ded.getNivelDeduccion());
                BigDecimal topeAplicable = DRIVERS_CON_TOPE_01.contains(ded.getNombreDriver())
                        ? TOPE_ESPECIAL
                        : TOPE_STANDARD;
                BigDecimal deduccionFinal = deduccionBruta.min(topeAplicable);

                totalDeduccion = totalDeduccion.add(deduccionFinal);

                RegistroDeduccionDriver rdd = new RegistroDeduccionDriver();
                rdd.setNombreDriver(ded.getNombreDriver());
                rdd.setNivelDeduccion(ded.getNivelDeduccion());
                registrosDeduccion.add(rdd);
            }
        }

        BigDecimal puntajeFinal = valorInicial.subtract(totalDeduccion);
        // El puntaje de Ejecución nunca puede ser negativo.
        if (puntajeFinal.compareTo(BigDecimal.ZERO) < 0) {
            puntajeFinal = BigDecimal.ZERO;
        }

        RegistroSubCriterio registro = new RegistroSubCriterio();
        registro.setSubCriterio(subCriterio);
        registro.setPuntajeBase(valorInicial);
        registro.setPuntajeDrivers(totalDeduccion.negate()); // negativo = deducción
        registro.setPuntajeFinal(puntajeFinal.setScale(2, RoundingMode.HALF_UP));
        registro.setDeduccionesDriver(registrosDeduccion);
        return registro;
    }

    private BigDecimal nivelADeduccion(Integer nivelDeduccion) {
        if (nivelDeduccion == null || nivelDeduccion == 0) return BigDecimal.ZERO;
        switch (nivelDeduccion) {
            case 1: return DEC_01;  // problemas menores
            case 2: return DEC_02;  // múltiples problemas
            case 3: return DEC_03;  // problemas generalizados
            default: throw new RuntimeException(
                    "Nivel de deducción inválido: " + nivelDeduccion + ". Debe ser 0, 1, 2 o 3.");
        }
    }
}