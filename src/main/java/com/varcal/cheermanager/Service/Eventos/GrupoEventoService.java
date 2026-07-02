package com.varcal.cheermanager.Service.Eventos;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Eventos.GrupoEventoDTO;
import com.varcal.cheermanager.DTO.Eventos.GrupoEventoResponseDTO;
import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.GrupoEvento;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Eventos.GrupoEventoRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;

@Service
public class GrupoEventoService {

    @Autowired
    private GrupoEventoRepository grupoEventoRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private GrupoEntrenamientoRepository grupoEntrenamientoRepository;

    public GrupoEvento inscribir(GrupoEventoDTO dto) {
        Evento evento = eventoRepository.findById(dto.getEventoId())
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + dto.getEventoId()));

        GrupoEntrenamiento grupo = grupoEntrenamientoRepository.findById(dto.getGrupoEntrenamientoId())
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado con ID: " + dto.getGrupoEntrenamientoId()));

        if (grupoEventoRepository.existsByEventoIdAndGrupoEntrenamientoId(evento.getId(), grupo.getId())) {
            throw new RuntimeException(
                    "El grupo '" + grupo.getNombre() + "' ya está inscrito en el evento '" + evento.getNombre() + "'");
        }

        GrupoEvento ge = new GrupoEvento();
        ge.setEvento(evento);
        ge.setGrupoEntrenamiento(grupo);
        return grupoEventoRepository.save(ge);
    }

    public List<GrupoEventoResponseDTO> listarPorEvento(Integer eventoId) {
        return grupoEventoRepository.findByEventoId(eventoId).stream().map(this::toDTO).collect(Collectors.toList());
    }

    // Historial de competencias en las que ha participado un grupo — la base
    // para la línea de tiempo/progresión de un equipo.
    public List<GrupoEventoResponseDTO> listarPorGrupo(Integer grupoEntrenamientoId) {
        return grupoEventoRepository.findByGrupoEntrenamientoId(grupoEntrenamientoId).stream()
                .map(this::toDTO).collect(Collectors.toList());
    }

    public void desinscribir(Integer id) {
        if (!grupoEventoRepository.existsById(id)) {
            throw new RuntimeException("Inscripción de grupo a evento no encontrada con ID: " + id);
        }
        grupoEventoRepository.deleteById(id);
    }

    private GrupoEventoResponseDTO toDTO(GrupoEvento ge) {
        GrupoEventoResponseDTO dto = new GrupoEventoResponseDTO();
        dto.setId(ge.getId());
        dto.setGrupoEntrenamientoId(ge.getGrupoEntrenamiento().getId());
        dto.setNombreGrupo(ge.getGrupoEntrenamiento().getNombre());
        dto.setEventoId(ge.getEvento().getId());
        dto.setNombreEvento(ge.getEvento().getNombre());
        dto.setFechaEvento(ge.getEvento().getFecha());
        return dto;
    }
}