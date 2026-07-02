package com.varcal.cheermanager.Service.Eventos;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Eventos.ResultadoCompetenciaDTO;
import com.varcal.cheermanager.DTO.Eventos.ResultadoCompetenciaResponseDTO;
import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.PremioEvento;
import com.varcal.cheermanager.Models.Eventos.ResultadoCompetencia;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.EvaluacionRutina;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Eventos.PremioEventoRepository;
import com.varcal.cheermanager.repository.Eventos.ResultadoCompetenciaRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.EvaluacionRutinaRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;

@Service
public class ResultadoCompetenciaService {

    @Autowired
    private ResultadoCompetenciaRepository resultadoCompetenciaRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private GrupoEntrenamientoRepository grupoEntrenamientoRepository;

    @Autowired
    private PremioEventoRepository premioEventoRepository;

    @Autowired
    private EvaluacionRutinaRepository evaluacionRutinaRepository;

    public ResultadoCompetenciaResponseDTO registrar(ResultadoCompetenciaDTO dto) {
        Evento evento = eventoRepository.findById(dto.getEventoId())
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + dto.getEventoId()));

        GrupoEntrenamiento grupo = null;
        if (dto.getGrupoEntrenamientoId() != null) {
            grupo = grupoEntrenamientoRepository.findById(dto.getGrupoEntrenamientoId())
                    .orElseThrow(() -> new RuntimeException(
                            "Grupo de entrenamiento no encontrado con ID: " + dto.getGrupoEntrenamientoId()));
        }

        PremioEvento premio = resolverPremio(dto.getPremioId(), evento);

        EvaluacionRutina evaluacionRutina = null;
        if (dto.getEvaluacionRutinaId() != null) {
            evaluacionRutina = evaluacionRutinaRepository.findById(dto.getEvaluacionRutinaId())
                    .orElseThrow(() -> new RuntimeException(
                            "Evaluación de rutina no encontrada con ID: " + dto.getEvaluacionRutinaId()));
        }

        ResultadoCompetencia resultado = new ResultadoCompetencia();
        resultado.setEvento(evento);
        resultado.setGrupoEntrenamiento(grupo);
        resultado.setPosicion(dto.getPosicion());
        resultado.setPuntaje(dto.getPuntaje());
        resultado.setPremio(premio);
        resultado.setEvaluacionRutina(evaluacionRutina);
        resultado.setObservaciones(dto.getObservaciones());

        return toDTO(resultadoCompetenciaRepository.save(resultado));
    }

    public List<ResultadoCompetenciaResponseDTO> listarPorEvento(Integer eventoId) {
        if (!eventoRepository.existsById(eventoId)) {
            throw new RuntimeException("Evento no encontrado con ID: " + eventoId);
        }
        return resultadoCompetenciaRepository.findByEventoIdOrderByPosicionAsc(eventoId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public List<ResultadoCompetenciaResponseDTO> listarPorGrupo(Integer grupoId) {
        if (!grupoEntrenamientoRepository.existsById(grupoId)) {
            throw new RuntimeException("Grupo de entrenamiento no encontrado con ID: " + grupoId);
        }
        return resultadoCompetenciaRepository.findByGrupoEntrenamientoId(grupoId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ResultadoCompetenciaResponseDTO actualizar(Integer id, ResultadoCompetenciaDTO dto) {
        ResultadoCompetencia resultado = resultadoCompetenciaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resultado no encontrado con ID: " + id));

        if (dto.getGrupoEntrenamientoId() != null) {
            GrupoEntrenamiento grupo = grupoEntrenamientoRepository.findById(dto.getGrupoEntrenamientoId())
                    .orElseThrow(() -> new RuntimeException(
                            "Grupo de entrenamiento no encontrado con ID: " + dto.getGrupoEntrenamientoId()));
            resultado.setGrupoEntrenamiento(grupo);
        }

        PremioEvento premio = resolverPremio(dto.getPremioId(), resultado.getEvento());
        resultado.setPremio(premio);

        if (dto.getEvaluacionRutinaId() != null) {
            EvaluacionRutina evaluacionRutina = evaluacionRutinaRepository.findById(dto.getEvaluacionRutinaId())
                    .orElseThrow(() -> new RuntimeException(
                            "Evaluación de rutina no encontrada con ID: " + dto.getEvaluacionRutinaId()));
            resultado.setEvaluacionRutina(evaluacionRutina);
        } else {
            resultado.setEvaluacionRutina(null);
        }

        resultado.setPosicion(dto.getPosicion());
        resultado.setPuntaje(dto.getPuntaje());
        resultado.setObservaciones(dto.getObservaciones());

        return toDTO(resultadoCompetenciaRepository.save(resultado));
    }

    public void eliminar(Integer id) {
        if (!resultadoCompetenciaRepository.existsById(id)) {
            throw new RuntimeException("Resultado no encontrado con ID: " + id);
        }
        resultadoCompetenciaRepository.deleteById(id);
    }

    // Centraliza la búsqueda + validación de pertenencia del premio al evento,
    // usada tanto en registrar() como en actualizar() para evitar duplicar
    // la regla de negocio (y el bug de que actualizar() no la aplicaba).
    private PremioEvento resolverPremio(Integer premioId, Evento evento) {
        if (premioId == null) {
            return null;
        }
        return premioEventoRepository.findById(premioId)
                .filter(p -> p.getEvento().getId().equals(evento.getId()))
                .orElseThrow(() -> new RuntimeException(
                        "El premio con ID " + premioId + " no existe o no pertenece a este evento"));
    }

    private ResultadoCompetenciaResponseDTO toDTO(ResultadoCompetencia r) {
        ResultadoCompetenciaResponseDTO dto = new ResultadoCompetenciaResponseDTO();
        dto.setId(r.getId());
        dto.setEventoId(r.getEvento().getId());
        dto.setNombreEvento(r.getEvento().getNombre());
        dto.setFechaEvento(r.getEvento().getFecha());
        dto.setGrupoEntrenamientoId(r.getGrupoEntrenamiento() != null ? r.getGrupoEntrenamiento().getId() : null);
        dto.setNombreGrupo(r.getGrupoEntrenamiento() != null ? r.getGrupoEntrenamiento().getNombre() : null);
        dto.setPosicion(r.getPosicion());
        dto.setPuntaje(r.getPuntaje());
        dto.setPremioId(r.getPremio() != null ? r.getPremio().getId() : null);
        dto.setDescripcionPremio(r.getPremio() != null ? r.getPremio().getDescripcion() : null);
        dto.setEvaluacionRutinaId(r.getEvaluacionRutina() != null ? r.getEvaluacionRutina().getId() : null);
        dto.setObservaciones(r.getObservaciones());
        return dto;
    }
}