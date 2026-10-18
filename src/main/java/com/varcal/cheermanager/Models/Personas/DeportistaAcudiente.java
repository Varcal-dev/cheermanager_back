package com.varcal.cheermanager.Models.Personas;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Vinculo deportista - acudiente, con el parentesco y si es el responsable principal. */
@Data
@NoArgsConstructor
@Entity
@Table(name = "deportista_acudientes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_dep_acud", columnNames = {"deportista_id", "acudiente_id"})})
public class DeportistaAcudiente {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "deportista_id", nullable = false)
    private Deportista deportista;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "acudiente_id", nullable = false)
    private Acudiente acudiente;

    @Column(name = "parentesco", length = 40)
    private String parentesco;

    @Column(name = "principal", nullable = false)
    private Boolean principal = false;

    @Column(name = "autoriza_recoger", nullable = false)
    private Boolean autorizaRecoger = false;
}
