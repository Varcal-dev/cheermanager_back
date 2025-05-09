package com.varcal.cheermanager.Models.Financiero;

import java.time.LocalDate;

import com.varcal.cheermanager.Models.Personas.Deportista;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "inscripciones")
public class Inscripcion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "deportista_id", nullable = false)
    private Deportista deportista;

    @ManyToOne
    @JoinColumn(name = "plan_pago_id", nullable = false)
    private PlanPago planPago;

    @Column(name = "fecha_inscripcion", nullable = false)
    private LocalDate fechaInscripcion;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    @Column(name = "estado", nullable = false)
    private String estado; // Puede ser "Activa" o "Inactiva".
}