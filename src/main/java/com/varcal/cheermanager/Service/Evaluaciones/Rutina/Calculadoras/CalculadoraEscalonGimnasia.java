package com.varcal.cheermanager.Service.Evaluaciones.Rutina.Calculadoras;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaDTO.DriverConstruccionDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaDTO.RegistroSubCriterioDTO;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.DriverSubCriterio;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.NivelCompetencia;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.RegistroSubCriterio;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.SubCriterioRubrica;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TablaCantidadNivel;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TopeDriverNivel;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TopeSeccionNivel;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.DriverSubCriterioRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TablaCantidadNivelRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TopeDriverNivelRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TopeSeccionNivelRepository;

// Implementa la lógica de:
//   - Rúbrica Élite N1-N4, "DIFICULTAD DE GIMNASIA ESTÁTICA":
//       1.5 = no alcanza 2.0
//       2.0 = menos de MAYORÍA realiza 1 habilidad/pase apropiado
//       2.5 = MAYORÍA realiza 1 habilidad/pase apropiado
//       3.0 = GRAN PARTE realiza 1 habilidad/pase Apropiado del nivel
//       + GRADO DE DIFICULTAD (0-1.0): 2 habilidades diferentes acumulables
//         Habilidad 1: Apropiada por MAYORÍA=0.2, Avanzada/Élite por MAYORÍA=0.4
//         Habilidad 2: Avanzada por GRAN PARTE=0.4, Élite por GRAN PARTE=0.6
//   - Rúbrica Élite N1-N4, "DIFICULTAD DE GIMNASIA CON CARRERA":
//       Mismos escalones + GRADO DE DIFICULTAD CON CARRERA (0-0.5) +
//       MÁXIMA PARTICIPACIÓN (0-0.5)
//   - Rúbrica Prep: misma estructura, escalones distintos en TopeSeccionNivel.
//
// Nota de la rúbrica: "La Dificultad de la Gimnasia Estática/con Carrera y el
// driver de Grado de Dificultad pueden lograrse de forma acumulativa" — las
// habilidades/pases del Grado de Dificultad son acumulables durante toda la
// rutina. El juez reporta habilidades distintas y para cada una marca si es
// Apropiada, Avanzada o Élite, y si la realizó MAYORÍA o GRAN PARTE (que en
// TopeDriverNivel se mapea en valorNivelBasico/valorNivelAlto).
@Component
public class CalculadoraEscalonGimnasia implements CalculadoraSubCriterio {

    @Autowired
    private TablaCantidadNivelRepository tablaCantidadRepo;

    @Autowired
    private TopeSeccionNivelRepository topeSeccionRepo;

    @Autowired
    private DriverSubCriterioRepository driverRepo;

    @Autowired
    private TopeDriverNivelRepository topeDriverRepo;

    @Override
    public SubCriterioRubrica.TipoCalculoSubCriterio getTipo() {
        return SubCriterioRubrica.TipoCalculoSubCriterio.ESCALON_GIMNASIA;
    }

    @Override
    public RegistroSubCriterio calcular(RegistroSubCriterioDTO dto,
            SubCriterioRubrica subCriterio, NivelCompetencia nivel, Integer cantidadAtletas) {

        TablaCantidadNivel tablaConteo = tablaCantidadRepo
                .findRangoAplicable(nivel.getId(), "Saltos_Gimnasia", cantidadAtletas)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró tabla de cantidad (Saltos/Gimnasia) para nivel '"
                        + nivel.getNombre() + "' con " + cantidadAtletas + " atletas"));

        int grupoNumerico = resolverGrupoNumerico(dto.getGrupoMinimoAlcanzado(), tablaConteo);

        List<TopeSeccionNivel> escalones = topeSeccionRepo
                .findByNivelIdAndSubCriterioIdOrderByOrdenDesc(nivel.getId(), subCriterio.getId());

        BigDecimal puntajeBase = BigDecimal.ZERO;
        for (TopeSeccionNivel escalon : escalones) {
            int umbralGrupo = (escalon.getCondicionGrupoMinimo() == null) ? 0
                    : resolverGrupoNumerico(escalon.getCondicionGrupoMinimo(), tablaConteo);
            if (grupoNumerico >= umbralGrupo) {
                puntajeBase = escalon.getValorPuntaje();
                break;
            }
        }

        // Grado de Dificultad: acumula por cada habilidad/pase distinto reportado.
        // Participación Máxima (Gimnasia con Carrera): el juez selecciona el escalón.
        BigDecimal puntajeDrivers = calcularDriversGimnasia(dto, subCriterio, nivel, tablaConteo);

        RegistroSubCriterio registro = new RegistroSubCriterio();
        registro.setSubCriterio(subCriterio);
        registro.setGrupoMinimoAlcanzado(dto.getGrupoMinimoAlcanzado());
        registro.setPuntajeBase(puntajeBase);
        registro.setPuntajeDrivers(puntajeDrivers);
        registro.setPuntajeFinal(puntajeBase.add(puntajeDrivers).setScale(2, RoundingMode.HALF_UP));
        return registro;
    }

    private BigDecimal calcularDriversGimnasia(RegistroSubCriterioDTO dto,
            SubCriterioRubrica subCriterio, NivelCompetencia nivel, TablaCantidadNivel tabla) {

        if (dto.getDriversConstruccion() == null || dto.getDriversConstruccion().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        List<DriverSubCriterio> drivers = driverRepo.findBySubCriterioId(subCriterio.getId());

        for (DriverSubCriterio driver : drivers) {
            TopeDriverNivel tope = topeDriverRepo
                    .findByDriverIdAndNivelId(driver.getId(), nivel.getId())
                    .orElse(null);
            if (tope == null) continue;

            BigDecimal sumDriver = BigDecimal.ZERO;

            if ("Grado de Dificultad".equalsIgnoreCase(driver.getNombre())) {
                // Rúbrica Élite GRADO DE DIFICULTAD GIMNASIA ESTÁTICA (0-1.0):
                //   Habilidad/Pase 1: Apropiada por MAYORÍA=0.2, Avanzada/Élite MAYORÍA=0.4
                //   Habilidad/Pase 2: Avanzada por GRAN PARTE=0.4, Élite GRAN PARTE=0.6
                // Rúbrica Élite GRADO DE DIFICULTAD GIMNASIA CON CARRERA (0-0.5):
                //   Habilidad/Pase: Avanzada GRAN PARTE=0.3, Élite GRAN PARTE=0.5
                // En ambos casos: valorNivelBasico = Apropiada/Avanzada, valorNivelAlto = Avanzada/Élite.
                for (DriverConstruccionDTO reg : dto.getDriversConstruccion()) {
                    if (!driver.getId().equals(reg.getDriverId())) continue;
                    if (reg.getNumeroHabilidad() == null) continue;
                    BigDecimal valor = Boolean.TRUE.equals(reg.getEsNivelAlto())
                            ? tope.getValorNivelAlto()
                            : tope.getValorNivelBasico();
                    if (valor != null) sumDriver = sumDriver.add(valor);
                }
                if (sumDriver.compareTo(tope.getTopeMaximo()) > 0) {
                    sumDriver = tope.getTopeMaximo();
                }

            } else if ("Participacion Maxima".equalsIgnoreCase(driver.getNombre())) {
                // MÁXIMA PARTICIPACIÓN GIMNASIA CON CARRERA (0-0.5):
                //   Apropiada del nivel por MÁXIMO: 0.3
                //   Avanzada/Élite por MÁXIMO:      0.5
                for (DriverConstruccionDTO reg : dto.getDriversConstruccion()) {
                    if (!driver.getId().equals(reg.getDriverId())) continue;
                    if (reg.getEscalonSeleccionadoId() == null) continue;
                    // El valor vive en EscalonDriverNivel — si la configuración lo tiene.
                    // Para Prep, este driver podría no existir para Gimnasia Estática.
                    sumDriver = BigDecimal.valueOf(
                            Boolean.TRUE.equals(reg.getEsNivelAlto())
                                    ? tope.getValorNivelAlto().doubleValue()
                                    : tope.getValorNivelBasico().doubleValue());
                    break;
                }
                if (sumDriver.compareTo(tope.getTopeMaximo()) > 0) {
                    sumDriver = tope.getTopeMaximo();
                }
            }

            total = total.add(sumDriver);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private int resolverGrupoNumerico(String clave, TablaCantidadNivel tabla) {
        if (clave == null) return 0;
        switch (clave.toUpperCase()) {
            case "MAYORIA":    return tabla.getMayoria();
            case "GRAN_PARTE": return tabla.getGranParte();
            case "MAXIMO":     return tabla.getMaximo();
            default: throw new RuntimeException(
                    "Valor de grupo desconocido: " + clave + ". Debe ser MAYORIA, GRAN_PARTE o MAXIMO.");
        }
    }
}