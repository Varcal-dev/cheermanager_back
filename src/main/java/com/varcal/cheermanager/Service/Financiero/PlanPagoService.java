package com.varcal.cheermanager.Service.Financiero;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Financiero.PlanPagoDTO;
import com.varcal.cheermanager.Models.Financiero.PlanPago;
import com.varcal.cheermanager.Models.Financiero.TipoPlanPago;
import com.varcal.cheermanager.Models.Org_dep.Inscripcion;
import com.varcal.cheermanager.repository.Financiero.PlanPagoRepository;
import com.varcal.cheermanager.repository.Financiero.TipoPlanPagoRepository;
import com.varcal.cheermanager.repository.Org_dep.InscripcionRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PlanPagoService {

    private final PlanPagoRepository planPagoRepository;
    private final TipoPlanPagoRepository tipoPlanPagoRepository;
    private final InscripcionRepository inscripcionRepository;

    public PlanPagoDTO crearPlan(PlanPagoDTO dto) {
        TipoPlanPago tipo = tipoPlanPagoRepository.findById(dto.getTipoPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de plan no encontrado"));

        PlanPago plan = new PlanPago();
        aplicarDatos(plan, dto, tipo);

        PlanPago guardado = planPagoRepository.save(plan);
        return toDTO(guardado);
    }

    // Sin filtro: todos los planes (uso interno / administración general).
    public List<PlanPagoDTO> listarPlanes() {
        return planPagoRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // Con filtro por categoría ("inscripcion" | "mensualidad"). Es lo que
    // consumen /planes-inscripcion y /planes-mensualidad respectivamente,
    // ambas contra el mismo catálogo planes_pago.
    public List<PlanPagoDTO> listarPlanesPorCategoria(TipoPlanPago.Categoria categoria) {
        return planPagoRepository.findAll().stream()
                .filter(p -> p.getTipoPlan() != null && p.getTipoPlan().getCategoria() == categoria)
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PlanPagoDTO obtenerPorId(Integer id) {
        PlanPago plan = planPagoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado"));
        return toDTO(plan);
    }

    public PlanPagoDTO actualizarPlan(Integer id, PlanPagoDTO dto) {
        PlanPago plan = planPagoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Plan no encontrado"));

        TipoPlanPago tipo = tipoPlanPagoRepository.findById(dto.getTipoPlanId())
                .orElseThrow(() -> new IllegalArgumentException("Tipo de plan no encontrado"));

        aplicarDatos(plan, dto, tipo);

        return toDTO(planPagoRepository.save(plan));
    }

    public void eliminarPlan(Integer id) {
        if (!planPagoRepository.existsById(id)) {
            throw new EntityNotFoundException("Plan no encontrado");
        }
        planPagoRepository.deleteById(id);
    }

    private void aplicarDatos(PlanPago plan, PlanPagoDTO dto, TipoPlanPago tipo) {
        plan.setNombre(dto.getNombre());
        plan.setTipoPlan(tipo);
        plan.setDescripcion(dto.getDescripcion());
        plan.setValorMensual(dto.getValorMensual());
        plan.setDescuentoPorcentaje(dto.getDescuentoPorcentaje());
        plan.setSesionesSemanales(dto.getSesionesSemanales());
        plan.setFechaVigenciaInicio(dto.getFechaVigenciaInicio());
        plan.setFechaVigenciaFin(dto.getFechaVigenciaFin());
        plan.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
    }

    private PlanPagoDTO toDTO(PlanPago plan) {
        PlanPagoDTO dto = new PlanPagoDTO();
        dto.setId(plan.getId());
        dto.setNombre(plan.getNombre());
        dto.setTipoPlanId(plan.getTipoPlan() != null ? plan.getTipoPlan().getId() : null);
        dto.setTipoPlanNombre(plan.getTipoPlan() != null ? plan.getTipoPlan().getNombre() : null);
        dto.setCategoria(
                plan.getTipoPlan() != null && plan.getTipoPlan().getCategoria() != null
                        ? plan.getTipoPlan().getCategoria().name()
                        : null);
        dto.setDescripcion(plan.getDescripcion());
        dto.setValorMensual(plan.getValorMensual());
        dto.setDescuentoPorcentaje(plan.getDescuentoPorcentaje());
        dto.setSesionesSemanales(plan.getSesionesSemanales());
        dto.setActivo(plan.getActivo());
        dto.setFechaVigenciaInicio(plan.getFechaVigenciaInicio());
        dto.setFechaVigenciaFin(plan.getFechaVigenciaFin());
        dto.setDeportistasInscritos(
                inscripcionRepository.countByPlanPagoIdAndEstado(plan.getId(), Inscripcion.EstadoInscripcion.Activa));
        return dto;
    }
}