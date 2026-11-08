package com.varcal.cheermanager.Models.Financiero;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cuota ya facturada de un pedido para una persona (evita facturar dos veces). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "pedidos_uniforme_cobros")
public class PedidoUniformeCobro {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "pedido_id", nullable = false)
    private Integer pedidoId;

    @Column(name = "persona_id", nullable = false)
    private Integer personaId;

    @Column(name = "cuota", nullable = false)
    private Integer cuota;

    @Column(name = "factura_id", nullable = false)
    private Integer facturaId;
}
