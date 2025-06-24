package com.varcal.cheermanager.Models.Personas;

import java.time.LocalDate;
import java.util.List;

import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "deportistas")
public class Deportista {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "persona_id", nullable = false)
    private Persona persona;

    @Column(name = "estado_id")
    private Integer estadoId;

    @Column(name = "altura")
    private Integer altura;

    @Column(name = "peso")
    private Integer peso;

    @Column(name = "nivel_actual_id")
    private Integer nivelActualId;

    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro;

    @Column(name = "contacto_emergencia")
    private String contactoEmergencia;

    @Column(name = "convenio_id")
    private Integer convenioId;

    @OneToMany(mappedBy = "deportista", cascade = CascadeType.ALL)
    private List<DeportistaPerteneceGrupo> historialGrupos;

}