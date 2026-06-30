package com.varcal.cheermanager.Service.Evaluaciones.Rutina.Calculadoras;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaDTO.DriverConstruccionDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaDTO.RegistroSubCriterioDTO;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.DriverSubCriterio;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.EscalonDriverNivel;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.NivelCompetencia;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.RegistroDeduccionDriver;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.RegistroDriverSubCriterio;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.RegistroSubCriterio;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.SubCriterioRubrica;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TablaCantidadNivel;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TopeDriverNivel;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TopeSeccionNivel;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.DriverSubCriterioRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.EscalonDriverNivelRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TablaCantidadNivelRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TopeDriverNivelRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TopeSeccionNivelRepository;

// Implementa la lógica de:
//   - Rúbrica Élite N1-N4, "DIFICULTAD DE ELEVACIONES" y "DIFICULTAD DE PIRÁMIDES"
//   - Rúbrica Prep, mismas secciones con distintos topes
//
// Flujo (igual en la rúbrica):
//   1. Determinar el ESCALÓN BASE: recorrer TopeSeccionNivel (desc) y elegir
//      el primero cuyas condicionHabilidadesDiferentes y condicionGrupoMinimo
//      sean satisfechas por lo que reportó el juez (habilidadesDiferentes +
//      grupoMinimoAlcanzado contra TablaCantidadNivel del nivel).
//   2. Calcular GRADO DE DIFICULTAD (0 a topeMaximo): por cada habilidad
//      marcada por el juez (DriverConstruccionDTO con numeroHabilidad), sumar
//      valorNivelBasico o valorNivelAlto de TopeDriverNivel según esNivelAlto.
//   3. Calcular PARTICIPACIÓN MÁXIMA: si el juez seleccionó un
//      EscalonDriverNivel, tomar su valor directamente.
//   4. Puntaje total = puntajeBase + min(gradoDificultad, topeGrado) + valorParticipacion.
@Component
public class CalculadoraEscalonHabilidadesGrupos implements CalculadoraSubCriterio {

    @Autowired
    private TablaCantidadNivelRepository tablaCantidadRepo;

    @Autowired
    private TopeSeccionNivelRepository topeSeccionRepo;

    @Autowired
    private DriverSubCriterioRepository driverRepo;

    @Autowired
    private TopeDriverNivelRepository topeDriverRepo;

    @Autowired
    private EscalonDriverNivelRepository escalonDriverRepo;

    @Override
    public SubCriterioRubrica.TipoCalculoSubCriterio getTipo() {
        return SubCriterioRubrica.TipoCalculoSubCriterio.ESCALON_HABILIDADES_GRUPOS;
    }

    @Override
    public RegistroSubCriterio calcular(RegistroSubCriterioDTO dto,
            SubCriterioRubrica subCriterio, NivelCompetencia nivel, Integer cantidadAtletas) {

        // 1. Obtener los umbrales de cantidad para este nivel y cantidad de atletas.
        TablaCantidadNivel tablaConteo = tablaCantidadRepo
                .findRangoAplicable(nivel.getId(), "Construcciones", cantidadAtletas)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró tabla de cantidad para nivel '"
                        + nivel.getNombre() + "' con " + cantidadAtletas + " atletas"));

        // 2. Convertir el grupoMinimoAlcanzado a su valor numérico de la tabla,
        //    para comparar contra las condiciones de los escalones.
        int grupoNumeroBruto = resolverGrupoNumerico(dto.getGrupoMinimoAlcanzado(), tablaConteo);

        // 3. Recorrer los escalones del sub-criterio (desc) y elegir el primero
        //    que satisfaga AMBAS condiciones:
        //    - condicionHabilidadesDiferentes <= habilidadesReportadas
        //    - condicionGrupoMinimo resuelto <= grupoNumericoBruto reportado
        List<TopeSeccionNivel> escalones = topeSeccionRepo
                .findByNivelIdAndSubCriterioIdOrderByOrdenDesc(nivel.getId(), subCriterio.getId());

        BigDecimal puntajeBase = BigDecimal.ZERO;
        String detalleEscalon = "No alcanza el mínimo requerido";

        for (TopeSeccionNivel escalon : escalones) {
            boolean cumpleHabilidades = (escalon.getCondicionHabilidadesDiferentes() == null)
                    || (dto.getHabilidadesDiferentes() != null
                        && dto.getHabilidadesDiferentes() >= escalon.getCondicionHabilidadesDiferentes());

            int umbralGrupo = (escalon.getCondicionGrupoMinimo() == null) ? 0
                    : resolverGrupoNumerico(escalon.getCondicionGrupoMinimo(), tablaConteo);

            boolean cumpleGrupo = grupoNumeroBruto >= umbralGrupo;

            if (cumpleHabilidades && cumpleGrupo) {
                puntajeBase = escalon.getValorPuntaje();
                detalleEscalon = escalon.getDescripcionRubrica() != null
                        ? escalon.getDescripcionRubrica()
                        : "Escalón " + escalon.getOrden() + " = " + escalon.getValorPuntaje();
                break;
            }
        }

        // 4. Calcular drivers (Grado de Dificultad + Participación Máxima).
        BigDecimal puntajeDrivers = calcularDrivers(dto, subCriterio, nivel);

        RegistroSubCriterio registro = new RegistroSubCriterio();
        registro.setSubCriterio(subCriterio);
        registro.setHabilidadesDiferentes(dto.getHabilidadesDiferentes());
        registro.setGrupoMinimoAlcanzado(dto.getGrupoMinimoAlcanzado());
        registro.setPuntajeBase(puntajeBase);
        registro.setPuntajeDrivers(puntajeDrivers);
        registro.setPuntajeFinal(puntajeBase.add(puntajeDrivers).setScale(2, RoundingMode.HALF_UP));
        registro.setRegistrosDriver(construirRegistrosDriver(dto, registro));

        return registro;
    }

    // Construye las filas RegistroDriverSubCriterio (Grado de Dificultad +
    // Participación Máxima) a partir de lo que reportó el juez en el DTO,
    // para que EvaluacionRutinaService las asocie y persista junto con el
    // RegistroSubCriterio padre.
    private List<RegistroDriverSubCriterio> construirRegistrosDriver(
            RegistroSubCriterioDTO dto, RegistroSubCriterio registroPadre) {

        List<RegistroDriverSubCriterio> resultado = new java.util.ArrayList<>();
        if (dto.getDriversConstruccion() == null) {
            return resultado;
        }

        for (DriverConstruccionDTO reg : dto.getDriversConstruccion()) {
            RegistroDriverSubCriterio rd = new RegistroDriverSubCriterio();
            rd.setRegistroSubCriterio(registroPadre);
            DriverSubCriterio driverRef = new DriverSubCriterio();
            driverRef.setId(reg.getDriverId());
            rd.setDriver(driverRef);
            rd.setNumeroHabilidad(reg.getNumeroHabilidad());
            rd.setEsNivelAlto(reg.getEsNivelAlto());
            if (reg.getEscalonSeleccionadoId() != null) {
                EscalonDriverNivel escalonRef = new EscalonDriverNivel();
                escalonRef.setId(reg.getEscalonSeleccionadoId());
                rd.setEscalonSeleccionado(escalonRef);
            }
            resultado.add(rd);
        }
        return resultado;
    }

    // "MAYORIA" | "GRAN_PARTE" | "MAXIMO" → número de grupos/atletas de la tabla.
    private int resolverGrupoNumerico(String clave, TablaCantidadNivel tabla) {
        if (clave == null) return 0;
        switch (clave.toUpperCase()) {
            case "MAYORIA":    return tabla.getMayoria();
            case "GRAN_PARTE": return tabla.getGranParte();
            case "MAXIMO":     return tabla.getMaximo();
            default: throw new RuntimeException("Valor de grupo desconocido: " + clave
                    + ". Debe ser MAYORIA, GRAN_PARTE o MAXIMO.");
        }
    }

    private BigDecimal calcularDrivers(RegistroSubCriterioDTO dto,
            SubCriterioRubrica subCriterio, NivelCompetencia nivel) {

        if (dto.getDriversConstruccion() == null || dto.getDriversConstruccion().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalDrivers = BigDecimal.ZERO;
        List<DriverSubCriterio> drivers = driverRepo.findBySubCriterioId(subCriterio.getId());

        for (DriverSubCriterio driver : drivers) {
            TopeDriverNivel tope = topeDriverRepo
                    .findByDriverIdAndNivelId(driver.getId(), nivel.getId())
                    .orElse(null);
            if (tope == null) continue;

            BigDecimal sumDriver = BigDecimal.ZERO;

            if ("Grado de Dificultad".equalsIgnoreCase(driver.getNombre())) {
                // Cada habilidad marcada por el juez suma valorNivelBasico o
                // valorNivelAlto según si fue apropiada o avanzada/élite.
                // Rúbrica Élite N1-N4 GRADO DE DIFICULTAD (0-0.8):
                //   Avanzada por GRAN PARTE: 0.1 | Élite por GRAN PARTE: 0.2
                for (DriverConstruccionDTO reg : dto.getDriversConstruccion()) {
                    if (!driver.getId().equals(reg.getDriverId())) continue;
                    if (reg.getNumeroHabilidad() == null) continue;
                    BigDecimal valor = Boolean.TRUE.equals(reg.getEsNivelAlto())
                            ? tope.getValorNivelAlto()
                            : tope.getValorNivelBasico();
                    if (valor != null) sumDriver = sumDriver.add(valor);
                }
                // Respetar el tope máximo configurado.
                if (sumDriver.compareTo(tope.getTopeMaximo()) > 0) {
                    sumDriver = tope.getTopeMaximo();
                }

            } else if ("Participacion Maxima".equalsIgnoreCase(driver.getNombre())) {
                // El juez selecciona directamente el escalón (0.3/0.5/0.7).
                // Rúbrica Élite N1-N4 PARTICIPACIÓN MÁXIMA (0-0.7):
                //   Apropiada MAX o Avanzada GRAN PARTE: 0.3
                //   Avanzada MAX o Élite GRAN PARTE:     0.5
                //   Élite MAX:                           0.7
                for (DriverConstruccionDTO reg : dto.getDriversConstruccion()) {
                    if (!driver.getId().equals(reg.getDriverId())) continue;
                    if (reg.getEscalonSeleccionadoId() == null) continue;
                    List<EscalonDriverNivel> escalones = escalonDriverRepo
                            .findByDriverIdAndNivelIdOrderByOrden(driver.getId(), nivel.getId());
                    escalones.stream()
                            .filter(e -> e.getId().equals(reg.getEscalonSeleccionadoId()))
                            .findFirst()
                            .ifPresent(e -> {
                                // Evitar lambdas con variables no efectivamente finales.
                            });
                    // Buscar el escalón seleccionado y tomar su valor.
                    BigDecimal valorEscalon = escalones.stream()
                            .filter(e -> e.getId().equals(reg.getEscalonSeleccionadoId()))
                            .map(EscalonDriverNivel::getValor)
                            .findFirst()
                            .orElse(BigDecimal.ZERO);
                    sumDriver = sumDriver.add(valorEscalon);
                    break; // Solo hay un escalón de Participación Máxima por registro.
                }
                if (sumDriver.compareTo(tope.getTopeMaximo()) > 0) {
                    sumDriver = tope.getTopeMaximo();
                }
            }

            totalDrivers = totalDrivers.add(sumDriver);
        }

        return totalDrivers.setScale(2, RoundingMode.HALF_UP);
    }
}