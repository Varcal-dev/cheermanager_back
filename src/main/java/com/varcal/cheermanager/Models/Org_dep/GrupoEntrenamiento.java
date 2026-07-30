package com.varcal.cheermanager.Models.Org_dep;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "grupos_entrenamiento")
public class GrupoEntrenamiento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "tipo_grupo_id")
    private TipoGrupo tipoGrupo;

    @ManyToOne
    @JoinColumn(name = "categorias_nivel_id")
    private CategoriaNivel categoriaNivel;

    // @JsonIgnore: CRÍTICO. Sin esto, Jackson entra en un ciclo infinito al
    // serializar cualquier Deportista completo:
    //   Deportista.historialGrupos -> DeportistaPerteneceGrupo.grupo ->
    //   GrupoEntrenamiento.deportistas -> DeportistaPerteneceGrupo.deportista ->
    //   (el mismo Deportista otra vez) -> ...
    // Esto reventó GET /api/facturas cuando la factura trae la inscripción
    // (que a su vez trae el deportista completo). No afecta el código Java
    // existente que llama a grupo.getDeportistas() para construir DTOs
    // (ej. GrupoConDeportistasDTO) — @JsonIgnore solo bloquea la
    // serialización JSON directa, no el acceso normal al getter en Java.
    @OneToMany(mappedBy = "grupo")
    @JsonIgnore
    private List<DeportistaPerteneceGrupo> deportistas;
}