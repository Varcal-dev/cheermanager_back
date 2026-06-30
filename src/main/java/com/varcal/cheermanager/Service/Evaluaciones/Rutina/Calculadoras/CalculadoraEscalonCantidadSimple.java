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
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TablaCantidadNivel;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TopeSeccionNivel;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TablaCantidadNivelRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.TopeSeccionNivelRepository;

// Implementa la lógica de:
//   - Rúbrica Élite N1-N4, "DIFICULTAD DE LANZAMIENTOS":
//       1.0 = menos de MAYORÍA realiza un lanzamiento
//       1.5 = MAYORÍA realiza un lanzamiento Apropiado del nivel
//       2.0 = MAYORÍA realiza en la MISMA SECCIÓN, sincronizado/canon, sin repetir
//   - Rúbrica Élite N1-N4, "DIFICULTAD DE SALTOS":
//       0.5 = no alcanza 1.0 | 1.0 = GRAN PARTE realiza 1 salto avanzado
//       1.5 = GRAN PARTE realiza 2 saltos avanzados conectados (con variedad)
//       2.0 = MÁXIMO realiza 3 saltos avanzados conectados, etc.
//   - Rúbrica Prep, mismas secciones (escalones distintos en TopeSeccionNivel).
//
// No tiene drivers adicionales de Grado de Dificultad ni Participación Máxima
// (esos son exclusivos de Construcciones/Gimnasia). El puntaje final = puntajeBase.
@Component
public class CalculadoraEscalonCantidadSimple implements CalculadoraSubCriterio {

    @Autowired
    private TablaCantidadNivelRepository tablaCantidadRepo;

    @Autowired
    private TopeSeccionNivelRepository topeSeccionRepo;

    @Override
    public SubCriterioRubrica.TipoCalculoSubCriterio getTipo() {
        return SubCriterioRubrica.TipoCalculoSubCriterio.ESCALON_CANTIDAD_SIMPLE;
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
        String detalleEscalon = "No alcanza el mínimo requerido";

        for (TopeSeccionNivel escalon : escalones) {
            boolean cumpleHabilidades = escalon.getCondicionHabilidadesDiferentes() == null
                    || (dto.getHabilidadesDiferentes() != null
                        && dto.getHabilidadesDiferentes() >= escalon.getCondicionHabilidadesDiferentes());

            int umbralGrupo = (escalon.getCondicionGrupoMinimo() == null) ? 0
                    : resolverGrupoNumerico(escalon.getCondicionGrupoMinimo(), tablaConteo);

            if (cumpleHabilidades && grupoNumerico >= umbralGrupo) {
                puntajeBase = escalon.getValorPuntaje();
                detalleEscalon = escalon.getDescripcionRubrica() != null
                        ? escalon.getDescripcionRubrica()
                        : "Escalón " + escalon.getOrden() + " = " + escalon.getValorPuntaje();
                break;
            }
        }

        RegistroSubCriterio registro = new RegistroSubCriterio();
        registro.setSubCriterio(subCriterio);
        registro.setHabilidadesDiferentes(dto.getHabilidadesDiferentes());
        registro.setGrupoMinimoAlcanzado(dto.getGrupoMinimoAlcanzado());
        registro.setPuntajeBase(puntajeBase);
        registro.setPuntajeDrivers(BigDecimal.ZERO);
        registro.setPuntajeFinal(puntajeBase.setScale(2, RoundingMode.HALF_UP));
        return registro;
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