package com.varcal.cheermanager.Service.Financiero;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Financiero.PlanMensualidadResponseDTO;
import com.varcal.cheermanager.Models.Financiero.PlanMensualidad;
import com.varcal.cheermanager.Models.Financiero.TipoPlanPago;
import com.varcal.cheermanager.repository.Financiero.PlanMensualidadRepository;
import com.varcal.cheermanager.repository.Financiero.TipoPlanPagoRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanMensualidadService {

    private final PlanMensualidadRepository planMensualidadRepository;
    private final TipoPlanPagoRepository tipoPlanPagoRepository;

    public PlanMensualidadResponseDTO crearPlan(PlanMensualidadResponseDTO dto) {
        TipoPlanPago tipo = tipoPlanPagoRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de plan no encontrado"));

        PlanMensualidad plan = new PlanMensualidad();
        plan.setNombre(dto.getNombre());
        plan.setTipoPlan(tipo);
        plan.setDescripcion(dto.getDescripcion());
        plan.setValorMensual(dto.getValorMensual());
        plan.setDescuentoPorcentaje(dto.getDescuentoPorcentaje());
        plan.setSesionesSemanales(dto.getSesionesSemanales());
        plan.setFechaVigenciaInicio(dto.getFechaVigenciaInicio());
        plan.setFechaVigenciaFin(dto.getFechaVigenciaFin());
        plan.setActivo(dto.getActivo() != null ? dto.getActivo() : true);

        PlanMensualidad guardado = planMensualidadRepository.save(plan);
        return toDTO(guardado);
    }

    public List<PlanMensualidadResponseDTO> listarPlanes() {
        return planMensualidadRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private PlanMensualidadResponseDTO toDTO(PlanMensualidad plan) {
        PlanMensualidadResponseDTO dto = new PlanMensualidadResponseDTO();
        dto.setId(plan.getId());
        dto.setNombre(plan.getNombre());
        dto.setTipoPlan(plan.getTipoPlan().getNombre());
        dto.setDescripcion(plan.getDescripcion());
        dto.setValorMensual(plan.getValorMensual());
        dto.setDescuentoPorcentaje(plan.getDescuentoPorcentaje());
        dto.setSesionesSemanales(plan.getSesionesSemanales());
        dto.setFechaVigenciaInicio(plan.getFechaVigenciaInicio());
        dto.setFechaVigenciaFin(plan.getFechaVigenciaFin());
        dto.setActivo(plan.getActivo());
        return dto;
    }

    public PlanMensualidadResponseDTO obtenerPorId(Integer id) {
        PlanMensualidad plan = planMensualidadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado"));
        return toDTO(plan);
    }

    public PlanMensualidadResponseDTO actualizarPlan(Integer id, PlanMensualidadResponseDTO dto) {
        PlanMensualidad plan = planMensualidadRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado"));

        TipoPlanPago tipo = tipoPlanPagoRepository.findById(dto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de plan no encontrado"));
        plan.setNombre(dto.getNombre());
        plan.setTipoPlan(tipo);
        plan.setDescripcion(dto.getDescripcion());
        plan.setValorMensual(dto.getValorMensual());
        plan.setDescuentoPorcentaje(dto.getDescuentoPorcentaje());
        plan.setSesionesSemanales(dto.getSesionesSemanales());
        plan.setFechaVigenciaInicio(dto.getFechaVigenciaInicio());
        plan.setFechaVigenciaFin(dto.getFechaVigenciaFin());
        plan.setActivo(dto.getActivo());

        return toDTO(planMensualidadRepository.save(plan));
    }

    public void eliminarPlan(Integer id) {
        if (!planMensualidadRepository.existsById(id)) {
            throw new EntityNotFoundException("Plan no encontrado");
        }
        planMensualidadRepository.deleteById(id);
    }

}
