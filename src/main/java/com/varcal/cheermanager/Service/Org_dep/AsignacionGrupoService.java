package com.varcal.cheermanager.Service.Org_dep;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Org_dep.AsignacionGrupoDTO;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

@Service
public class AsignacionGrupoService {

    private final DeportistaRepository deportistaRepository;
    private final GrupoEntrenamientoRepository grupoRepository;
    private final DeportistaPerteneceGrupoRepository asignacionRepository;

    public AsignacionGrupoService(
        DeportistaRepository deportistaRepository,
        GrupoEntrenamientoRepository grupoRepository,
        DeportistaPerteneceGrupoRepository asignacionRepository) {
        this.deportistaRepository = deportistaRepository;
        this.grupoRepository = grupoRepository;
        this.asignacionRepository = asignacionRepository;
    }

    public void asignar(AsignacionGrupoDTO dto) {
        // Validación ejemplo: el deportista no puede estar ya en un grupo activo
        boolean yaAsignado = asignacionRepository.existsByDeportistaIdAndFechaFinIsNull(dto.getDeportistaId());
        if (yaAsignado) {
            throw new AsignacionInvalidaException("El deportista ya está asignado a un grupo activo.");
        }

        DeportistaPerteneceGrupo asignacion = new DeportistaPerteneceGrupo();
        asignacion.setDeportistaId(dto.getDeportistaId());
        asignacion.setGrupoId(dto.getGrupoId());
        asignacion.setFechaInicio(dto.getFechaInicio());
        asignacion.setObservaciones(dto.getObservaciones());

        asignacionRepository.save(asignacion);
    }
}
