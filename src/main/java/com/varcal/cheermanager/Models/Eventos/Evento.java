package com.varcal.cheermanager.Models.Eventos;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "eventos")
public class Evento {
    /** Club (tenant): Hibernate lo asigna solo al guardar y filtra por el toda consulta HQL/JPA. */
    @org.hibernate.annotations.TenantId
    @jakarta.persistence.Column(name = "club_id", nullable = false, updatable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Integer clubId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "fecha")
    private LocalDate fecha;

    @ManyToOne
    @JoinColumn(name = "tipo_evento_id")
    private TipoEvento tipoEvento;

    @Column(name = "ubicacion")
    private String ubicacion;

    @Column(name = "tiene_resultados", nullable = false)
    private Boolean tieneResultados;

    @OneToMany(mappedBy = "evento", cascade = CascadeType.ALL)
    private List<PremioEvento> premios;
}