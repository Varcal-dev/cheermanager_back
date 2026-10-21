package com.varcal.cheermanager.Models.Org_dep;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Temporada deportiva del club (ej. "2026"). Solo una puede estar activa a la vez. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "temporadas", uniqueConstraints = {
        @UniqueConstraint(name = "uk_temporada_club_nombre", columnNames = {"club_id", "nombre"})})
public class Temporada {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "activa", nullable = false)
    private Boolean activa = false;
}