package com.varcal.cheermanager.Service.Eventos;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Eventos.EventoDTO;
import com.varcal.cheermanager.DTO.Eventos.EventoResponseDTO;
import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.TipoEvento;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Eventos.GrupoEventoRepository;
import com.varcal.cheermanager.repository.Eventos.PremioEventoRepository;
import com.varcal.cheermanager.repository.Eventos.ResultadoCompetenciaRepository;
import com.varcal.cheermanager.repository.Eventos.TipoEventoRepository;

@Service
public class EventoService {

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private TipoEventoRepository tipoEventoRepository;

    @Autowired
    private GrupoEventoRepository grupoEventoRepository;

    @Autowired
    private PremioEventoRepository premioEventoRepository;

    @Autowired
    private ResultadoCompetenciaRepository resultadoCompetenciaRepository;

    public List<EventoResponseDTO> listarTodos() {
        return eventoRepository.findAll().stream().map(this::toResponseDTO).collect(Collectors.toList());
    }

    public EventoResponseDTO obtenerPorId(Integer id) {
        return toResponseDTO(buscarOFallar(id));
    }

    // Calendario: próximos eventos, para mostrar en un dashboard o notificar
    // a entrenadores/deportistas de lo que se viene.
    public List<EventoResponseDTO> proximos() {
        return eventoRepository.findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate.now()).stream()
                .map(this::toResponseDTO).collect(Collectors.toList());
    }

    public List<EventoResponseDTO> historial() {
        return eventoRepository.findByFechaLessThanOrderByFechaDesc(LocalDate.now()).stream()
                .map(this::toResponseDTO).collect(Collectors.toList());
    }

    public Evento crear(EventoDTO dto) {
        Evento evento = new Evento();
        aplicarDatos(evento, dto);
        return eventoRepository.save(evento);
    }

    public Evento actualizar(Integer id, EventoDTO dto) {
        Evento evento = buscarOFallar(id);
        aplicarDatos(evento, dto);
        return eventoRepository.save(evento);
    }

    // Se protege contra el borrado accidental de un evento que ya tiene
    // resultados oficiales registrados — perder esa trazabilidad afecta el
    // historial competitivo del club, no es un simple registro de calendario.
    @Transactional
    public void eliminar(Integer id) {
        Evento evento = buscarOFallar(id);
        boolean tieneResultados = !resultadoCompetenciaRepository.findByEventoIdOrderByPosicionAsc(id).isEmpty();
        if (tieneResultados) {
            throw new RuntimeException(
                    "No se puede eliminar el evento '" + evento.getNombre() +
                    "': ya tiene resultados de competencia registrados. Elimínalos primero si de verdad quieres borrar el evento.");
        }
        eventoRepository.delete(evento);
    }

    private void aplicarDatos(Evento evento, EventoDTO dto) {
        evento.setNombre(dto.getNombre());
        evento.setFecha(dto.getFecha());
        evento.setUbicacion(dto.getUbicacion());
        evento.setTieneResultados(dto.getTieneResultados() != null ? dto.getTieneResultados() : Boolean.FALSE);

        if (dto.getTipoEventoId() != null) {
            TipoEvento tipo = tipoEventoRepository.findById(dto.getTipoEventoId())
                    .orElseThrow(() -> new RuntimeException("Tipo de evento no encontrado con ID: " + dto.getTipoEventoId()));
            evento.setTipoEvento(tipo);
        } else {
            evento.setTipoEvento(null);
        }
    }

    private Evento buscarOFallar(Integer id) {
        return eventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + id));
    }

    private EventoResponseDTO toResponseDTO(Evento e) {
        EventoResponseDTO dto = new EventoResponseDTO();
        dto.setId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setFecha(e.getFecha());
        dto.setTipoEventoId(e.getTipoEvento() != null ? e.getTipoEvento().getId() : null);
        dto.setTipoEvento(e.getTipoEvento() != null ? e.getTipoEvento().getEvento() : null);
        dto.setUbicacion(e.getUbicacion());
        dto.setTieneResultados(e.getTieneResultados());
        dto.setCantidadGruposInscritos(grupoEventoRepository.findByEventoId(e.getId()).size());
        dto.setCantidadPremios(premioEventoRepository.findByEventoId(e.getId()).size());
        return dto;
    }
}