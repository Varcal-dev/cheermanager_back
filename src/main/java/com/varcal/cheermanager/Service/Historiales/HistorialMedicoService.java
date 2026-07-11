package com.varcal.cheermanager.Service.Historiales;

import com.varcal.cheermanager.DTO.Persona.HistorialMedicoDTO;
import com.varcal.cheermanager.Models.Historiales.HistorialMedico;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Historiales.HistorialMedicoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class HistorialMedicoService {

    @Autowired
    private HistorialMedicoRepository historialMedicoRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private DeportistaRepository deportistaRepository;

    // --- Métodos pensados para el controller de deportistas: reciben
    // deportistaId y resuelven personaId internamente, igual que ya hacen
    // HistorialNivelDeportistaService y HistorialPlanesDeportistaService.
    // Antes el controller mandaba deportistaId disfrazado de personaId. ---

    public HistorialMedicoDTO crearHistorialMedicoDeDeportista(Integer deportistaId, HistorialMedicoDTO dto) {
        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + deportistaId));
        dto.setPersonaId(deportista.getPersona().getId());
        return crearHistorialMedico(dto);
    }

    public Page<HistorialMedicoDTO> obtenerHistorialMedicoPorDeportista(Integer deportistaId, Pageable pageable) {
        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + deportistaId));
        return obtenerHistorialMedicoPorPersona(deportista.getPersona().getId(), pageable);
    }

    public HistorialMedicoDTO crearHistorialMedico(HistorialMedicoDTO dto) {
        Persona persona = personaRepository.findById(dto.getPersonaId())
                .orElseThrow(() -> new RuntimeException("Persona no encontrada"));

        HistorialMedico historial = new HistorialMedico();
        historial.setPersona(persona);
        historial.setDescripcion(dto.getDescripcion());
        historial.setFechaRegistro(dto.getFechaRegistro());
        historial.setTipoRegistro(dto.getTipoRegistro());
        historial.setGravedad(dto.getGravedad());
        historial.setMedicoTratante(dto.getMedicoTratante());

        HistorialMedico saved = historialMedicoRepository.save(historial);
        return convertToDTO(saved);
    }

    public Page<HistorialMedicoDTO> obtenerHistorialMedicoPorPersona(Integer personaId, Pageable pageable) {
        Page<HistorialMedico> page = historialMedicoRepository.findByPersonaId(personaId, pageable);
        return page.map(this::convertToDTO);
    }

    public Optional<HistorialMedicoDTO> obtenerHistorialMedicoPorId(Integer id) {
        return historialMedicoRepository.findById(id).map(this::convertToDTO);
    }

    public HistorialMedicoDTO actualizarHistorialMedico(Integer id, HistorialMedicoDTO dto) {
        HistorialMedico historial = historialMedicoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Historial médico no encontrado"));

        historial.setDescripcion(dto.getDescripcion());
        historial.setFechaRegistro(dto.getFechaRegistro());
        historial.setTipoRegistro(dto.getTipoRegistro());
        historial.setGravedad(dto.getGravedad());
        historial.setMedicoTratante(dto.getMedicoTratante());

        HistorialMedico saved = historialMedicoRepository.save(historial);
        return convertToDTO(saved);
    }

    public void eliminarHistorialMedico(Integer id) {
        if (!historialMedicoRepository.existsById(id)) {
            throw new RuntimeException("Historial médico no encontrado");
        }
        historialMedicoRepository.deleteById(id);
    }

    private HistorialMedicoDTO convertToDTO(HistorialMedico historial) {
        HistorialMedicoDTO dto = new HistorialMedicoDTO();
        dto.setId(historial.getId());
        dto.setPersonaId(historial.getPersona().getId());
        dto.setDescripcion(historial.getDescripcion());
        dto.setFechaRegistro(historial.getFechaRegistro());
        dto.setTipoRegistro(historial.getTipoRegistro());
        dto.setGravedad(historial.getGravedad());
        dto.setMedicoTratante(historial.getMedicoTratante());
        return dto;
    }

    public boolean tieneCondicionActiva(Integer personaId) {
        LocalDate hace30Dias = LocalDate.now().minusDays(30);
        List<HistorialMedico> recientes = historialMedicoRepository
                .findByPersonaIdAndFechaRegistroAfterAndGravedadIsNotNull(personaId, hace30Dias);
        return !recientes.isEmpty();
    }
}