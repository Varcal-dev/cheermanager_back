package com.varcal.cheermanager.Service.Eventos;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Eventos.PremioEventoDTO;
import com.varcal.cheermanager.DTO.Eventos.PremioEventoResponseDTO;
import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.PremioEvento;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Eventos.PremioEventoRepository;

@Service
public class PremioEventoService {

    @Autowired
    private PremioEventoRepository premioEventoRepository;

    @Autowired
    private EventoRepository eventoRepository;

    public PremioEventoResponseDTO crear(PremioEventoDTO dto) {
        Evento evento = eventoRepository.findById(dto.getEventoId())
                .orElseThrow(() -> new RuntimeException("Evento no encontrado con ID: " + dto.getEventoId()));

        PremioEvento premio = new PremioEvento();
        premio.setEvento(evento);
        premio.setDescripcion(dto.getDescripcion());
        premio.setPremio(dto.getPremio());

        return toDTO(premioEventoRepository.save(premio));
    }

    public List<PremioEventoResponseDTO> listarPorEvento(Integer eventoId) {
        if (!eventoRepository.existsById(eventoId)) {
            throw new RuntimeException("Evento no encontrado con ID: " + eventoId);
        }
        return premioEventoRepository.findByEventoId(eventoId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PremioEventoResponseDTO actualizar(Integer id, PremioEventoDTO dto) {
        PremioEvento premio = premioEventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Premio no encontrado con ID: " + id));

        premio.setDescripcion(dto.getDescripcion());
        premio.setPremio(dto.getPremio());

        return toDTO(premioEventoRepository.save(premio));
    }

    public void eliminar(Integer id) {
        if (!premioEventoRepository.existsById(id)) {
            throw new RuntimeException("Premio no encontrado con ID: " + id);
        }
        premioEventoRepository.deleteById(id);
    }

    private PremioEventoResponseDTO toDTO(PremioEvento p) {
        PremioEventoResponseDTO dto = new PremioEventoResponseDTO();
        dto.setId(p.getId());
        dto.setEventoId(p.getEvento().getId());
        dto.setNombreEvento(p.getEvento().getNombre());
        dto.setDescripcion(p.getDescripcion());
        dto.setPremio(p.getPremio());
        return dto;
    }
}