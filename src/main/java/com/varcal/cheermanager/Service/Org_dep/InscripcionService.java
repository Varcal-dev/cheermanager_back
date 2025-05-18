package com.varcal.cheermanager.Service.Org_dep;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Org_dep.InscripcionDto;
import com.varcal.cheermanager.DTO.Org_dep.InscripcionSimpleDTO;
import com.varcal.cheermanager.Models.Financiero.PlanPago;
import com.varcal.cheermanager.Models.Org_dep.Inscripcion;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Financiero.PlanPagoRepository;
import com.varcal.cheermanager.repository.Org_dep.InscripcionRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

@Service
public class InscripcionService {

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private DeportistaRepository deportistaRepository;

    @Autowired
    private PlanPagoRepository planPagoRepository;

    public List<Object[]> obtenerDetalleInscripciones() {
        return inscripcionRepository.obtenerVistaInscripcionesDetalle();
    }

    public Inscripcion obtenerPorId(Integer id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscripción no encontrada"));
    }

    public Inscripcion crearInscripcion(InscripcionDto dto) {
        Inscripcion inscripcion = new Inscripcion();

        // Buscar el deportista por ID
        Deportista deportista = deportistaRepository.findById(dto.getDeportistaId())
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado"));

        // Buscar el plan de pago por ID
        PlanPago planPago = planPagoRepository.findById(dto.getPlanPagoId())
                .orElseThrow(() -> new RuntimeException("Plan de pago no encontrado"));

        // Setear las relaciones
        inscripcion.setDeportista(deportista);
        inscripcion.setPlanPago(planPago);  
        inscripcion.setFechaInscripcion(dto.getFechaInscripcion());
        inscripcion.setFechaVencimiento(dto.getFechaVencimiento());
        inscripcion.setEstado(Inscripcion.EstadoInscripcion.valueOf(dto.getEstado()));

        return inscripcionRepository.save(inscripcion);
    }

    public Inscripcion actualizarInscripcion(Integer id, Inscripcion nuevaInscripcion) {
        Inscripcion existente = obtenerPorId(id);

        existente.setFechaInscripcion(nuevaInscripcion.getFechaInscripcion());
        existente.setFechaVencimiento(nuevaInscripcion.getFechaVencimiento());
        existente.setEstado(nuevaInscripcion.getEstado());

        if (nuevaInscripcion.getPlanPago() != null) {
            PlanPago plan = planPagoRepository.findById(
                    nuevaInscripcion.getPlanPago().getId())
                    .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
            existente.setPlanPago(plan);
        }

        return inscripcionRepository.save(existente);
    }

    public void eliminarInscripcion(Integer id) {
        inscripcionRepository.deleteById(id);
    }

    public List<InscripcionSimpleDTO> listarInscripcionesSimples() {
        return inscripcionRepository.findAll().stream().map(ins -> {
            InscripcionSimpleDTO dto = new InscripcionSimpleDTO();
            dto.setId(ins.getId().intValue());
            dto.setDeportista(ins.getDeportista().getPersona().getNombre() + " "
                    + ins.getDeportista().getPersona().getApellidos());
            dto.setFechaInscripcion(ins.getFechaInscripcion());
            dto.setPlanPago(ins.getPlanPago().getTipoPlan().getNombre());
            dto.setEstado(ins.getEstado().name());
            return dto;
        }).collect(Collectors.toList());
    }

    public List<Object[]> obtenerDeportistasNoInscritos() {
        return inscripcionRepository.obtenerVistaDeportistasNoInscritos();
    }
}
