package com.varcal.cheermanager.Service.Financiero;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Financiero.PlanPagoRequestDTO;
import com.varcal.cheermanager.DTO.Financiero.PlanPagoResponseDTO;
import com.varcal.cheermanager.Models.Financiero.PlanPago;
import com.varcal.cheermanager.Models.Financiero.TipoPlanPago;
import com.varcal.cheermanager.repository.Financiero.PlanPagoRepository;
import com.varcal.cheermanager.repository.Financiero.TipoPlanPagoRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanPagoService {

    private final PlanPagoRepository planPagoRepository;
    private final TipoPlanPagoRepository tipoPlanPagoRepository;

    public PlanPagoResponseDTO crearPlan(PlanPagoRequestDTO dto) {
        TipoPlanPago tipo = tipoPlanPagoRepository.findById(dto.getTipoPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de plan no encontrado"));

        PlanPago plan = new PlanPago();
        plan.setTipoPlan(tipo);
        plan.setDescripcion(dto.getDescripcion());
        plan.setValorMensual(dto.getValorMensual());
        plan.setFechaVigenciaInicio(dto.getFechaVigenciaInicio());
        plan.setFechaVigenciaFin(dto.getFechaVigenciaFin());
        plan.setActivo(dto.getActivo() != null ? dto.getActivo() : true);

        PlanPago guardado = planPagoRepository.save(plan);
        return toDTO(guardado);
    }

    public List<PlanPagoResponseDTO> listarPlanes() {
        return planPagoRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    private PlanPagoResponseDTO toDTO(PlanPago plan) {
        PlanPagoResponseDTO dto = new PlanPagoResponseDTO();
        dto.setId(plan.getId());
        dto.setTipoPlan(plan.getTipoPlan().getNombre());
        dto.setDescripcion(plan.getDescripcion());
        dto.setValorMensual(plan.getValorMensual());
        dto.setFechaVigenciaInicio(plan.getFechaVigenciaInicio());
        dto.setFechaVigenciaFin(plan.getFechaVigenciaFin());
        dto.setActivo(plan.getActivo());
        return dto;
    }

    public PlanPagoResponseDTO obtenerPorId(Integer id) {
        PlanPago plan = planPagoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado"));
        return toDTO(plan);
    }

    public PlanPagoResponseDTO actualizarPlan(Integer id, PlanPagoRequestDTO dto) {
        PlanPago plan = planPagoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado"));

        TipoPlanPago tipo = tipoPlanPagoRepository.findById(dto.getTipoPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de plan no encontrado"));

        plan.setTipoPlan(tipo);
        plan.setDescripcion(dto.getDescripcion());
        plan.setValorMensual(dto.getValorMensual());
        plan.setFechaVigenciaInicio(dto.getFechaVigenciaInicio());
        plan.setFechaVigenciaFin(dto.getFechaVigenciaFin());
        plan.setActivo(dto.getActivo());

        return toDTO(planPagoRepository.save(plan));
    }

    public void eliminarPlan(Integer id) {
        if (!planPagoRepository.existsById(id)) {
            throw new EntityNotFoundException("Plan no encontrado");
        }
        planPagoRepository.deleteById(id);
    }

}
