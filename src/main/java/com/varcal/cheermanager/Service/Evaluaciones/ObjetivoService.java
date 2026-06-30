package com.varcal.cheermanager.Service.Evaluaciones;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Evaluaciones.ObjetivoDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.ObjetivoResponseDTO;
import com.varcal.cheermanager.Models.Evaluaciones.EstadoObjetivo;
import com.varcal.cheermanager.Models.Evaluaciones.Objetivo;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Evaluaciones.EstadoObjetivoRepository;
import com.varcal.cheermanager.repository.Evaluaciones.ObjetivoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

@Service
public class ObjetivoService {

    @Autowired
    private ObjetivoRepository objetivoRepository;

    @Autowired
    private DeportistaRepository deportistaRepository;

    @Autowired
    private EstadoObjetivoRepository estadoObjetivoRepository;

    public ObjetivoResponseDTO crear(ObjetivoDTO dto) {
        Deportista deportista = deportistaRepository.findById(dto.getDeportistaId())
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + dto.getDeportistaId()));

        EstadoObjetivo estado = estadoObjetivoRepository.findById(dto.getEstadoObjetivoId())
                .orElseThrow(() -> new RuntimeException("Estado de objetivo no encontrado con ID: " + dto.getEstadoObjetivoId()));

        Objetivo objetivo = new Objetivo();
        objetivo.setDeportista(deportista);
        objetivo.setNombre(dto.getNombre());
        objetivo.setDescripcion(dto.getDescripcion());
        objetivo.setFechaCreacion(dto.getFechaCreacion() != null ? dto.getFechaCreacion() : LocalDate.now());
        objetivo.setEstadoObjetivo(estado);

        return toResponseDTO(objetivoRepository.save(objetivo));
    }

    public List<ObjetivoResponseDTO> listarPorDeportista(Integer deportistaId) {
        if (!deportistaRepository.existsById(deportistaId)) {
            throw new RuntimeException("Deportista no encontrado con ID: " + deportistaId);
        }
        return objetivoRepository.findByDeportistaIdOrderByFechaCreacionDesc(deportistaId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<ObjetivoResponseDTO> listarPorDeportistaYEstado(Integer deportistaId, Integer estadoId) {
        return objetivoRepository.findByDeportistaIdAndEstadoObjetivoId(deportistaId, estadoId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Útil para un panel del entrenador: todos los objetivos "En progreso"
    // de todos los deportistas, sin filtrar por uno en particular.
    public List<ObjetivoResponseDTO> listarPorEstado(Integer estadoId) {
        return objetivoRepository.findByEstadoObjetivoId(estadoId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public ObjetivoResponseDTO actualizar(Integer id, ObjetivoDTO dto) {
        Objetivo objetivo = objetivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Objetivo no encontrado con ID: " + id));

        if (dto.getNombre() != null) {
            objetivo.setNombre(dto.getNombre());
        }
        if (dto.getDescripcion() != null) {
            objetivo.setDescripcion(dto.getDescripcion());
        }
        if (dto.getEstadoObjetivoId() != null) {
            EstadoObjetivo estado = estadoObjetivoRepository.findById(dto.getEstadoObjetivoId())
                    .orElseThrow(() -> new RuntimeException("Estado de objetivo no encontrado con ID: " + dto.getEstadoObjetivoId()));
            objetivo.setEstadoObjetivo(estado);
        }

        return toResponseDTO(objetivoRepository.save(objetivo));
    }

    // Atajo para el caso más común: marcar un objetivo como cumplido (o
    // cualquier cambio de estado) sin tener que reenviar nombre/descripción.
    public ObjetivoResponseDTO cambiarEstado(Integer id, Integer nuevoEstadoId) {
        Objetivo objetivo = objetivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Objetivo no encontrado con ID: " + id));

        EstadoObjetivo estado = estadoObjetivoRepository.findById(nuevoEstadoId)
                .orElseThrow(() -> new RuntimeException("Estado de objetivo no encontrado con ID: " + nuevoEstadoId));

        objetivo.setEstadoObjetivo(estado);
        return toResponseDTO(objetivoRepository.save(objetivo));
    }

    public void eliminar(Integer id) {
        if (!objetivoRepository.existsById(id)) {
            throw new RuntimeException("Objetivo no encontrado con ID: " + id);
        }
        objetivoRepository.deleteById(id);
    }

    private ObjetivoResponseDTO toResponseDTO(Objetivo o) {
        ObjetivoResponseDTO dto = new ObjetivoResponseDTO();
        dto.setId(o.getId());
        dto.setDeportistaId(o.getDeportista().getId());
        dto.setNombreDeportista(o.getDeportista().getPersona().getNombre() + " "
                + o.getDeportista().getPersona().getApellidos());
        dto.setNombre(o.getNombre());
        dto.setDescripcion(o.getDescripcion());
        dto.setFechaCreacion(o.getFechaCreacion());
        dto.setEstadoObjetivoId(o.getEstadoObjetivo().getId());
        dto.setEstadoObjetivo(o.getEstadoObjetivo().getNombreEstado());
        return dto;
    }
}