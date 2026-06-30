package com.varcal.cheermanager.Service.Historiales;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Historiales.HistorialPlanesDeportistaDTO;
import com.varcal.cheermanager.Models.Financiero.PlanPago;
import com.varcal.cheermanager.Models.Historiales.HistorialPlanesDeportista;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Financiero.PlanPagoRepository;
import com.varcal.cheermanager.repository.Historiales.HistorialPlanesDeportistaRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

@Service
public class HistorialPlanesDeportistaService {

    @Autowired
    private HistorialPlanesDeportistaRepository historialPlanesDeportistaRepository;

    @Autowired
    private DeportistaRepository deportistaRepository;

    @Autowired
    private PlanPagoRepository planPagoRepository;

    // Cierra el registro de plan vigente (si existe, poniéndole fechaFin) y
    // abre uno nuevo sin fechaFin. Pensado para llamarse desde
    // InscripcionService.actualizarInscripcion cuando detecta un cambio de
    // plan, y también puede usarse de forma manual vía el controller.
    //
    // @Transactional: cerrar el registro viejo y abrir el nuevo deben verse
    // como una sola operación — si solo una de las dos se guarda, quedarías
    // con dos planes "vigentes" a la vez o con ninguno.
    @Transactional
    public HistorialPlanesDeportista registrarCambioDePlan(Integer deportistaId, Integer planPagoNuevoId,
                                                             LocalDate fechaCambio, String motivo) {
        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + deportistaId));

        PlanPago planNuevo = planPagoRepository.findById(planPagoNuevoId)
                .orElseThrow(() -> new RuntimeException("Plan de pago no encontrado con ID: " + planPagoNuevoId));

        LocalDate fecha = fechaCambio != null ? fechaCambio : LocalDate.now();

        // Cerrar el plan vigente anterior, si tenía uno.
        historialPlanesDeportistaRepository.findByDeportistaIdAndFechaFinIsNull(deportistaId)
                .ifPresent(planVigente -> {
                    planVigente.setFechaFin(fecha);
                    historialPlanesDeportistaRepository.save(planVigente);
                });

        HistorialPlanesDeportista nuevo = new HistorialPlanesDeportista();
        nuevo.setDeportista(deportista);
        nuevo.setPlanPagoId(planNuevo.getId());
        nuevo.setFechaInicio(fecha);
        nuevo.setFechaFin(null);
        nuevo.setMotivoCambio(motivo);

        return historialPlanesDeportistaRepository.save(nuevo);
    }

    public List<HistorialPlanesDeportistaDTO> listarPorDeportista(Integer deportistaId) {
        if (!deportistaRepository.existsById(deportistaId)) {
            throw new RuntimeException("Deportista no encontrado con ID: " + deportistaId);
        }
        return historialPlanesDeportistaRepository.findByDeportistaIdOrderByFechaInicioDesc(deportistaId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private HistorialPlanesDeportistaDTO toDTO(HistorialPlanesDeportista h) {
        HistorialPlanesDeportistaDTO dto = new HistorialPlanesDeportistaDTO();
        dto.setId(h.getId());
        dto.setDeportistaId(h.getDeportista().getId());
        dto.setNombreDeportista(h.getDeportista().getPersona().getNombre() + " "
                + h.getDeportista().getPersona().getApellidos());
        dto.setPlanPagoId(h.getPlanPagoId());
        planPagoRepository.findById(h.getPlanPagoId()).ifPresent(p -> dto.setNombrePlan(p.getNombre()));
        dto.setFechaInicio(h.getFechaInicio());
        dto.setFechaFin(h.getFechaFin());
        dto.setMotivoCambio(h.getMotivoCambio());
        return dto;
    }
}