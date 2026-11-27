package com.varcal.cheermanager.DTO.Org_dep;

import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * Cuerpo de {@code PUT /api/inscripciones/{id}}. Antes el endpoint recibia la entidad {@code Inscripcion} completa
 * aunque el servicio solo usa estos cinco campos; ahora cualquier otra cosa que llegue en el cuerpo (deportista,
 * temporada, clubId...) se ignora.
 *
 * <p>Conserva la forma JSON que ya envia el front: los planes llegan como objeto con id
 * ({@code "planPago": {"id": 5}}), no como {@code planPagoId}.
 * <pre>
 * { "planPago": {"id": 5}, "planMensualidad": {"id": 8}, "fechaInscripcion": "2026-07-12",
 *   "fechaVencimiento": "2026-08-12", "estado": "Activa" }
 * </pre>
 *
 * <p>{@code planPago} y {@code planMensualidad} son opcionales: ausentes (o null) significan "no cambiar ese plan".
 */
@Data
public class InscripcionActualizacionDTO {

    /** Referencia a un plan de pago por id. */
    @Data
    public static class PlanRef {
        @NotNull(message = "El id del plan es obligatorio")
        private Integer id;
    }

    @Valid
    private PlanRef planPago;

    @Valid
    private PlanRef planMensualidad;

    @NotNull(message = "La fecha de inscripción es obligatoria")
    private LocalDate fechaInscripcion;

    private LocalDate fechaVencimiento;

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "Activa|Inactiva", message = "El estado debe ser Activa o Inactiva")
    private String estado;
}
