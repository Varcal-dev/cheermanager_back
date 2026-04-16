package com.varcal.cheermanager.Service.Persona;

import com.varcal.cheermanager.DTO.Persona.HistorialMedicoDTO;
import com.varcal.cheermanager.Models.Historiales.HistorialMedico;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Historiales.HistorialMedicoRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List; 

@Service
public class HistorialMedicoService {

    @Autowired
    private HistorialMedicoRepository historialMedicoRepository;

    @Autowired
    private PersonaRepository personaRepository;

    public HistorialMedicoDTO crearHistorialMedico(HistorialMedicoDTO dto) {
        java.util.Optional<Persona> personaOpt = personaRepository.findById(dto.getPersonaId());
        if (personaOpt.isEmpty()) {
            throw new RuntimeException("Persona no encontrada");
        }

        HistorialMedico historial = new HistorialMedico();
        historial.setPersona(personaOpt.get());
        historial.setDescripcion(dto.getDescripcion());
        historial.setFechaRegistro(dto.getFechaRegistro());
        historial.setTipoRegistro(dto.getTipoRegistro());
        historial.setGravedad(dto.getGravedad());
        historial.setMedicoTratante(dto.getMedicoTratante());

        HistorialMedico saved = historialMedicoRepository.save(historial);
        return convertToDTO(saved);
    }

    public Page<HistorialMedicoDTO> obtenerHistorialMedicoPorPersona(Integer personaId, org.springframework.data.domain.Pageable pageable) {
        Page<HistorialMedico> page = historialMedicoRepository.findByPersonaId(personaId, pageable);
        return page.map(this::convertToDTO);
    }

    public java.util.Optional<HistorialMedicoDTO> obtenerHistorialMedicoPorId(Integer id) {
        return historialMedicoRepository.findById(id).map(this::convertToDTO);
    }

    public HistorialMedicoDTO actualizarHistorialMedico(Integer id, HistorialMedicoDTO dto) {
        java.util.Optional<HistorialMedico> opt = historialMedicoRepository.findById(id);
        if (opt.isEmpty()) {
            throw new RuntimeException("Historial médico no encontrado");
        }

        HistorialMedico historial = opt.get();
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
        List<HistorialMedico> recientes = historialMedicoRepository.findByPersonaIdAndFechaRegistroAfterAndGravedadIsNotNull(personaId, hace30Dias);
        return !recientes.isEmpty();
    }
}