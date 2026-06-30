package com.varcal.cheermanager.Service.Evaluaciones.Rutina;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaDTO.RegistroSubCriterioDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaResponseDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.Rutina.EvaluacionRutinaResponseDTO.RegistroSubCriterioResponseDTO;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.EvaluacionRutina;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.NivelCompetencia;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.RegistroSubCriterio;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.SubCriterioRubrica;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Service.Evaluaciones.Rutina.Calculadoras.CalculadoraSubCriterio;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.EvaluacionRutinaRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.NivelCompetenciaRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.RegistroSubCriterioRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.SubCriterioRubricaRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;

@Service
public class EvaluacionRutinaService {

    @Autowired
    private EvaluacionRutinaRepository evaluacionRutinaRepo;

    @Autowired
    private RegistroSubCriterioRepository registroSubCriterioRepo;

    @Autowired
    private NivelCompetenciaRepository nivelCompetenciaRepo;

    @Autowired
    private SubCriterioRubricaRepository subCriterioRepo;

    @Autowired
    private GrupoEntrenamientoRepository grupoEntrenamientoRepo;

    // Mapa tipoCalculo → calculadora. Spring inyecta la lista de todas las
    // CalculadoraSubCriterio del contexto y la indexamos por tipo.
    private final Map<SubCriterioRubrica.TipoCalculoSubCriterio, CalculadoraSubCriterio> calculadoras;

    @Autowired
    public EvaluacionRutinaService(List<CalculadoraSubCriterio> listaCalculadoras) {
        calculadoras = new EnumMap<>(SubCriterioRubrica.TipoCalculoSubCriterio.class);
        listaCalculadoras.forEach(c -> calculadoras.put(c.getTipo(), c));
    }

    // @Transactional: guarda EvaluacionRutina + todos los RegistroSubCriterio +
    // sus deduccionesDriver y registrosDriver como una sola unidad. Si cualquier
    // cálculo falla, no queda una evaluación a medio guardar.
    @Transactional
    public EvaluacionRutinaResponseDTO registrar(EvaluacionRutinaDTO dto) {
        GrupoEntrenamiento grupo = grupoEntrenamientoRepo.findById(dto.getGrupoId())
                .orElseThrow(() -> new RuntimeException(
                        "Grupo de entrenamiento no encontrado con ID: " + dto.getGrupoId()));

        NivelCompetencia nivel = nivelCompetenciaRepo.findById(dto.getNivelId())
                .orElseThrow(() -> new RuntimeException(
                        "Nivel de competencia no encontrado con ID: " + dto.getNivelId()));

        EvaluacionRutina evaluacion = new EvaluacionRutina();
        evaluacion.setGrupo(grupo);
        evaluacion.setNivel(nivel);
        evaluacion.setFecha(dto.getFecha());
        evaluacion.setEvento(dto.getEvento());
        evaluacion.setCantidadAtletas(dto.getCantidadAtletas());
        evaluacion.setObservaciones(dto.getObservaciones());

        // Guardar cabecera primero para obtener el ID necesario para asociar
        // los RegistroSubCriterio.
        EvaluacionRutina guardada = evaluacionRutinaRepo.save(evaluacion);

        BigDecimal total = BigDecimal.ZERO;

        for (RegistroSubCriterioDTO regDTO : dto.getRegistros()) {
            SubCriterioRubrica subCriterio = subCriterioRepo.findById(regDTO.getSubCriterioId())
                    .orElseThrow(() -> new RuntimeException(
                            "Sub-criterio no encontrado con ID: " + regDTO.getSubCriterioId()));

            CalculadoraSubCriterio calculadora = calculadoras.get(subCriterio.getTipoCalculo());
            if (calculadora == null) {
                throw new RuntimeException("No hay calculadora registrada para el tipo '"
                        + subCriterio.getTipoCalculo() + "' del sub-criterio '"
                        + subCriterio.getNombre() + "'.");
            }

            RegistroSubCriterio registro = calculadora.calcular(
                    regDTO, subCriterio, nivel, dto.getCantidadAtletas());
            registro.setEvaluacionRutina(guardada);

            // Asociar las deduccionesDriver al RegistroSubCriterio antes de guardar.
            if (registro.getDeduccionesDriver() != null) {
                registro.getDeduccionesDriver().forEach(d -> d.setRegistroSubCriterio(registro));
            }
            if (registro.getRegistrosDriver() != null) {
                registro.getRegistrosDriver().forEach(d -> d.setRegistroSubCriterio(registro));
            }

            registroSubCriterioRepo.save(registro);

            if (registro.getPuntajeFinal() != null) {
                total = total.add(registro.getPuntajeFinal());
            }
        }

        guardada.setPuntajeTotal(total.setScale(2, RoundingMode.HALF_UP));
        evaluacionRutinaRepo.save(guardada);

        return toResponseDTO(guardada);
    }

    public List<EvaluacionRutinaResponseDTO> listarPorGrupo(Integer grupoId) {
        if (!grupoEntrenamientoRepo.existsById(grupoId)) {
            throw new RuntimeException("Grupo de entrenamiento no encontrado con ID: " + grupoId);
        }
        return evaluacionRutinaRepo.findByGrupoIdOrderByFechaDesc(grupoId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public EvaluacionRutinaResponseDTO obtenerPorId(Integer id) {
        return evaluacionRutinaRepo.findById(id)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("Evaluación de rutina no encontrada con ID: " + id));
    }

    public void eliminar(Integer id) {
        if (!evaluacionRutinaRepo.existsById(id)) {
            throw new RuntimeException("Evaluación de rutina no encontrada con ID: " + id);
        }
        evaluacionRutinaRepo.deleteById(id);
    }

    private EvaluacionRutinaResponseDTO toResponseDTO(EvaluacionRutina e) {
        EvaluacionRutinaResponseDTO dto = new EvaluacionRutinaResponseDTO();
        dto.setId(e.getId());
        dto.setGrupoId(e.getGrupo().getId());
        dto.setNombreGrupo(e.getGrupo().getNombre());
        dto.setNivelId(e.getNivel().getId());
        dto.setNivel(e.getNivel().getNombre());
        dto.setFecha(e.getFecha());
        dto.setEvento(e.getEvento());
        dto.setCantidadAtletas(e.getCantidadAtletas());
        dto.setPuntajeTotal(e.getPuntajeTotal());
        dto.setObservaciones(e.getObservaciones());

        if (e.getRegistros() != null) {
            dto.setRegistros(e.getRegistros().stream().map(r -> {
                RegistroSubCriterioResponseDTO rd = new RegistroSubCriterioResponseDTO();
                rd.setId(r.getId());
                rd.setSubCriterioId(r.getSubCriterio().getId());
                rd.setNombreSubCriterio(r.getSubCriterio().getNombre());
                rd.setSeccion(r.getSubCriterio().getSeccion());
                rd.setPuntajeBase(r.getPuntajeBase());
                rd.setPuntajeDrivers(r.getPuntajeDrivers());
                rd.setPuntajeFinal(r.getPuntajeFinal());
                return rd;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}