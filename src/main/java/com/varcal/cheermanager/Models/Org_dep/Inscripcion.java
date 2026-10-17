package com.varcal.cheermanager.Models.Org_dep;

import java.time.LocalDate;

import com.varcal.cheermanager.Models.Financiero.PlanPago;
import com.varcal.cheermanager.Models.Personas.Deportista;

import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inscripciones")
public class Inscripcion {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "deportista_id")
    private Deportista deportista;

    // Plan de INSCRIPCIÓN (cobro único, ej. matrícula/temporada). Se mantiene
    // el nombre `planPago` tal cual para no romper el resto del código que ya
    // lo usa (vistas SQL, DTOs, etc.) — conceptualmente es "el plan de
    // inscripción", ahora que existe planMensualidad como su contraparte.
    @ManyToOne(optional = false)
    @JoinColumn(name = "plan_pago_id")
    private PlanPago planPago;

    // Nuevo: plan de MENSUALIDAD (cobro recurrente mes a mes). Opcional
    // porque puede haber inscripciones antiguas o casos especiales (ej.
    // clínicas/camps de una sola vez) sin mensualidad asociada.
    @ManyToOne(optional = true)
    @JoinColumn(name = "plan_mensualidad_id")
    private PlanPago planMensualidad;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Enumerated(EnumType.STRING)
    private EstadoInscripcion estado;

    public enum EstadoInscripcion {
        Activa,
        Inactiva
    }
}
