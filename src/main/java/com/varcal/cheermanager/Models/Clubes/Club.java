package com.varcal.cheermanager.Models.Clubes;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un club (cliente del SaaS). NO es una tabla de club (sin tenant): es la tabla raiz de la que cuelgan los demas.
 * Convencion: id 1 = club "Plataforma" (solo el superadmin); los clubes reales empiezan en 2.
 *
 * Suscripcion (V11): un club NUEVO nace en PRUEBA por 30 dias (valores por defecto de abajo); los clubes
 * existentes se cargan desde la base con lo que dejo la migracion V11 (fundador, ACTIVA, sin vencimiento).
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "clubes")
public class Club {

    public static final int PLATAFORMA_ID = 1;
    public static final int DIAS_PRUEBA = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    /** Identificador corto y unico (url / referencia): "ratels-all-stars". */
    @Column(name = "slug", nullable = false, unique = true, length = 60)
    private String slug;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    // ---- Suscripcion (V11) ----

    /** Plan contratado (planes_saas.id). */
    @Column(name = "plan_id")
    private Integer planId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_suscripcion", nullable = false, length = 12)
    private EstadoSuscripcion estadoSuscripcion = EstadoSuscripcion.PRUEBA;

    /** Ultimo dia vigente (inclusive). Vacio = sin vencimiento. */
    @Column(name = "vigente_hasta")
    private LocalDate vigenteHasta = LocalDate.now().plusDays(DIAS_PRUEBA);

    @Column(name = "dias_gracia", nullable = false)
    private Integer diasGracia = 5;

    /** Limite propio de deportistas activos; si es null se usa el del plan (que tambien puede ser ilimitado). */
    @Column(name = "limite_deportistas")
    private Integer limiteDeportistas;

    @Column(name = "notas_suscripcion", length = 500)
    private String notasSuscripcion;
}
