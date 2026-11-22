package com.varcal.cheermanager.DTO.Financiero;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

// DTO único para el catálogo de planes de pago (crear, editar, listar).
// Reemplaza a PlanPagoDTO y PlanPagoResponseDTO, que estaban duplicados y
// ninguno de los dos traía tipoPlanId — por eso crear/editar un plan
// terminaba usando el id equivocado para resolver el TipoPlanPago.
//
// "categoria" viene de TipoPlanPago.categoria ("inscripcion" | "mensualidad")
// y es lo que separan las páginas /planes-inscripcion y /planes-mensualidad
// del front: AMBAS leen de este mismo catálogo (planes_pago), filtrando por
// categoria, en vez de que /planes-inscripcion use la tabla huérfana
// planes_inscripcion (sin ninguna FK real en toda la base de datos).
@Data
public class PlanPagoDTO {
    private Integer id;
    @NotBlank(message = "El nombre del plan es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar 100 caracteres")
    private String nombre;
    @NotNull(message = "Selecciona un tipo de plan")
    private Integer tipoPlanId;
    private String tipoPlanNombre; // solo lectura, para mostrar en tablas
    private String categoria; // solo lectura: "inscripcion" | "mensualidad"
    private String descripcion;
    @NotNull(message = "El valor mensual es obligatorio")
    @DecimalMin(value = "0.00", message = "El valor mensual no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El valor mensual admite hasta 8 enteros y 2 decimales")
    private BigDecimal valorMensual;
    @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El descuento no puede superar 100%")
    @Digits(integer = 3, fraction = 2, message = "El descuento admite hasta 2 decimales")
    private BigDecimal descuentoPorcentaje;
    @PositiveOrZero(message = "Las sesiones semanales no pueden ser negativas")
    private Integer sesionesSemanales;
    private Boolean activo;
    @NotNull(message = "La fecha de inicio de vigencia es obligatoria")
    private LocalDate fechaVigenciaInicio;
    private LocalDate fechaVigenciaFin;
    private Long deportistasInscritos; // solo lectura, calculado
}