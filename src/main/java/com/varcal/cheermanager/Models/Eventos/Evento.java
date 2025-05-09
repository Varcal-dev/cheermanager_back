package com.varcal.cheermanager.Models.Eventos;

import java.time.LocalDate;

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

    @Column(name = "tipo_evento_id")
    private Integer tipoEventoId;

    @Column(name = "ubicacion")
    private String ubicacion;

    @Column(name = "tiene_resultados", nullable = false)
    private Boolean tieneResultados;
}