package com.varcal.cheermanager.Service.Historiales;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Historiales.HistorialNivelDeportistaDTO;
import com.varcal.cheermanager.Models.Historiales.HistorialNivelDeportista;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Historiales.HistorialNivelDeportistaRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

@Service
public class HistorialNivelDeportistaService {

    @Autowired
    private HistorialNivelDeportistaRepository historialNivelDeportistaRepository;

    @Autowired
    private DeportistaRepository deportistaRepository;

    // Pensado para llamarse tanto desde un endpoint manual como desde otro
    // service (ej. PersonaService cuando cambie nivelActualId) sin duplicar lógica.
    public HistorialNivelDeportista registrarCambio(Integer deportistaId, Integer nivelId, LocalDate fechaCambio, String motivo) {
        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + deportistaId));

        HistorialNivelDeportista historial = new HistorialNivelDeportista();
        historial.setDeportista(deportista);
        historial.setNivelId(nivelId);
        historial.setFechaCambio(fechaCambio != null ? fechaCambio : LocalDate.now());
        historial.setMotivo(motivo);

        return historialNivelDeportistaRepository.save(historial);
    }

    public List<HistorialNivelDeportistaDTO> listarPorDeportista(Integer deportistaId) {
        if (!deportistaRepository.existsById(deportistaId)) {
            throw new RuntimeException("Deportista no encontrado con ID: " + deportistaId);
        }
        return historialNivelDeportistaRepository.findByDeportistaIdOrderByFechaCambioDesc(deportistaId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private HistorialNivelDeportistaDTO toDTO(HistorialNivelDeportista h) {
        HistorialNivelDeportistaDTO dto = new HistorialNivelDeportistaDTO();
        dto.setId(h.getId());
        dto.setDeportistaId(h.getDeportista().getId());
        dto.setNombreDeportista(h.getDeportista().getPersona().getNombre() + " "
                + h.getDeportista().getPersona().getApellidos());
        dto.setNivelId(h.getNivelId());
        dto.setFechaCambio(h.getFechaCambio());
        dto.setMotivo(h.getMotivo());
        return dto;
    }
}