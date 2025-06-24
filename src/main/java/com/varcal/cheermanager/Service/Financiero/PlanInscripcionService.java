package com.varcal.cheermanager.Service.Financiero;

import com.varcal.cheermanager.Models.Financiero.PlanInscripcion;
import com.varcal.cheermanager.repository.Financiero.PlanInscripcionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlanInscripcionService {

    @Autowired
    private PlanInscripcionRepository planInscripcionRepository;

    public List<PlanInscripcion> listarPlanes() {
        return planInscripcionRepository.findAll();
    }

    public PlanInscripcion obtenerPorId(Integer id) {
        return planInscripcionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan de inscripción no encontrado con ID: " + id));
    }
}
