package com.varcal.cheermanager.DTO.Financiero;

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
    private String nombre;
    private Integer tipoPlanId;
    private String tipoPlanNombre; // solo lectura, para mostrar en tablas
    private String categoria; // solo lectura: "inscripcion" | "mensualidad"
    private String descripcion;
    private BigDecimal valorMensual;
    private BigDecimal descuentoPorcentaje;
    private Integer sesionesSemanales;
    private Boolean activo;
    private LocalDate fechaVigenciaInicio;
    private LocalDate fechaVigenciaFin;
    private Long deportistasInscritos; // solo lectura, calculado
}