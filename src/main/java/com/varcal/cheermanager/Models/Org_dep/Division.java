package com.varcal.cheermanager.Models.Org_dep;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "divisiones", uniqueConstraints = {@jakarta.persistence.UniqueConstraint(name = "uk_division_club_nombre", columnNames = {"club_id", "nombre"})})
public class Division {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    @NotBlank(message = "El nombre de la división es obligatorio")
    @Size(max = 255, message = "El nombre de la división no puede superar 255 caracteres")
    private String nombre;
}