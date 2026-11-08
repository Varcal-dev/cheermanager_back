package com.varcal.cheermanager.Models.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Pedido de uniformes de un grupo, cobrado en cuotas (una factura concepto UNIFORME por cuota). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "pedidos_uniforme")
public class PedidoUniforme {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "grupo_id", nullable = false)
    private Integer grupoId;

    @Column(name = "valor_por_deportista", nullable = false)
    private BigDecimal valorPorDeportista;

    @Column(name = "numero_cuotas", nullable = false)
    private Integer numeroCuotas;

    @Column(name = "primer_vencimiento", nullable = false)
    private LocalDate primerVencimiento;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDate fechaCreacion;

    @Column(name = "usuario_id")
    private Integer usuarioId;
}
