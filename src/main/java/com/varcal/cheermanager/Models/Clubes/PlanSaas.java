package com.varcal.cheermanager.Models.Clubes;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Plan que se le vende a un club. Catalogo GLOBAL de la plataforma (sin club_id, sin @TenantId). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "planes_saas")
public class PlanSaas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "codigo", nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    /** Maximo de deportistas activos (no Retirados). null = ilimitado. */
    @Column(name = "max_deportistas")
    private Integer maxDeportistas;

    /** En pesos colombianos. null = por definir. */
    @Column(name = "precio_mensual", precision = 12, scale = 2)
    private BigDecimal precioMensual;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;
}
