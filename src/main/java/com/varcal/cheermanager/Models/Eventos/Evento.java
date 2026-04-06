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