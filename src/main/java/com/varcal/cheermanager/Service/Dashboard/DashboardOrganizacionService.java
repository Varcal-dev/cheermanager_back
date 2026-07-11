package com.varcal.cheermanager.Service.Dashboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Dashboard.ResumenOrganizacionDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenOrganizacionDTO.GrupoOcupacionDTO;
import com.varcal.cheermanager.Models.Org_dep.CategoriaNivel;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Genero;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Personas.GeneroRepository;

@Service
public class DashboardOrganizacionService {

    private static final String GENERO_MASCULINO = "Masculino";

    @Autowired
    private GrupoEntrenamientoRepository grupoEntrenamientoRepository;

    @Autowired
    private DeportistaPerteneceGrupoRepository deportistaPerteneceGrupoRepository;

    @Autowired
    private GeneroRepository generoRepository;

    public ResumenOrganizacionDTO obtenerResumen() {
        Map<Integer, String> catalogoGenero = generoRepository.findAll().stream()
                .collect(Collectors.toMap(Genero::getId, Genero::getGenero));

        List<GrupoOcupacionDTO> ocupacion = new ArrayList<>();
        Map<String, Long> porNivel = new HashMap<>();

        for (GrupoEntrenamiento grupo : grupoEntrenamientoRepository.findAll()) {
            GrupoOcupacionDTO item = calcularOcupacion(grupo, catalogoGenero);
            ocupacion.add(item);

            String etiquetaNivel = grupo.getCategoriaNivel() != null && grupo.getCategoriaNivel().getNivel() != null
                    ? grupo.getCategoriaNivel().getNivel().getNombre()
                    : "Sin nivel configurado";
            porNivel.merge(etiquetaNivel, 1L, Long::sum);
        }

        ResumenOrganizacionDTO dto = new ResumenOrganizacionDTO();
        dto.setGruposConOcupacion(ocupacion);
        // Solo entran los grupos que SÍ tienen una regla de cantidad mínima
        // cargada (cantidadMinRequerida != null) — sin eso no hay con qué
        // comparar, y no tendría sentido "alertar" sin una referencia real.
        dto.setGruposPorDebajoDelMinimo(ocupacion.stream()
                .filter(g -> g.getCantidadMinRequerida() != null && g.getDeportistasActivos() < g.getCantidadMinRequerida())
                .collect(Collectors.toList()));
        dto.setGruposPorNivel(porNivel);

        return dto;
    }

    private GrupoOcupacionDTO calcularOcupacion(GrupoEntrenamiento grupo, Map<Integer, String> catalogoGenero) {
        List<DeportistaPerteneceGrupo> activos = deportistaPerteneceGrupoRepository.findByGrupoIdAndFechaFinIsNull(grupo.getId());

        long hombres = 0;
        long mujeres = 0;
        for (DeportistaPerteneceGrupo p : activos) {
            Integer generoId = p.getDeportista().getPersona().getGeneroId();
            String nombreGenero = generoId != null ? catalogoGenero.get(generoId) : null;
            if (GENERO_MASCULINO.equalsIgnoreCase(nombreGenero)) {
                hombres++;
            } else if (nombreGenero != null) {
                mujeres++;
            }
            // generos "Sin especificar" o desconocidos no se cuentan en
            // ninguna de las dos columnas, para no distorsionar el dato.
        }

        GrupoOcupacionDTO dto = new GrupoOcupacionDTO();
        dto.setGrupoId(grupo.getId());
        dto.setNombreGrupo(grupo.getNombre());
        dto.setDeportistasActivos(activos.size());
        dto.setHombres(hombres);
        dto.setMujeres(mujeres);
        dto.setMixto(hombres > 0 && mujeres > 0);

        CategoriaNivel categoria = grupo.getCategoriaNivel();
        if (categoria != null) {
            dto.setNivel(categoria.getNivel() != null ? categoria.getNivel().getNombre() : null);
            dto.setDivision(categoria.getDivision() != null ? categoria.getDivision().getNombre() : null);
            dto.setCantidadMinRequerida(categoria.getCantidadMin());
            dto.setCantidadMaxSugerida(categoria.getCantidadMax());
        }

        return dto;
    }
}