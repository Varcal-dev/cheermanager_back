package com.varcal.cheermanager.Models.Horario_Asistencia;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lugar fisico donde entrena el club (C11). */
@Data
@NoArgsConstructor
@Entity
@Table(name = "sedes")
public class Sede {
    @org.hibernate.annotations.TenantId
    @Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 100) private String nombre;
    @Column(name = "direccion", length = 200) private String direccion;
    @Column(name = "ciudad", length = 80) private String ciudad;
    @Column(name = "notas", length = 300) private String notas;
    @Column(name = "activa", nullable = false) private boolean activa = true;
}
