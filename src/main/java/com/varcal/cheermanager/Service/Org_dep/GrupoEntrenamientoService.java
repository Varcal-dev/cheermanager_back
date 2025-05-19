package com.varcal.cheermanager.Service.Org_dep;

import java.sql.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Persona.AsignacionEntrenadorDTO;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;

@Service
public class GrupoEntrenamientoService {

    @Autowired
    private GrupoEntrenamientoRepository grupoEntrenamientoRepository;

    public GrupoEntrenamiento crearGrupoEntrenamiento(GrupoEntrenamiento grupo) throws Exception {
        try {
            return grupoEntrenamientoRepository.save(grupo);
        } catch (Exception e) {
            // Captura otras excepciones inesperadas
            throw new Exception("Error al crear el grupo de entrenamiento.", e);
        }
    }

    public void GrupoService(GrupoEntrenamientoRepository grupoRepository) {
        this.grupoEntrenamientoRepository = grupoRepository;
    }

    public String verificarElegibilidadDeportista(Integer grupoId, Integer deportistaId) {
        return grupoEntrenamientoRepository.verificarElegibilidadDeportista(deportistaId, grupoId);
    }

    public String agregarDeportistaAGrupo(Integer deportistaId, Integer grupoId, String observaciones) {
        return grupoEntrenamientoRepository.agregarDeportistaAGrupo(deportistaId, grupoId, observaciones);
    }

    /* 
    public String asignarEntrenadorAGrupo2(Integer entrenadorId, Integer grupoId, String observaciones) {

        return grupoEntrenamientoRepository.agregarDeportistaAGrupo(entrenadorId, grupoId, observaciones);
    }*/

    public String asignarEntrenadorAGrupo(AsignacionEntrenadorDTO dto) {try {
            Date fechaInicio = Date.valueOf(dto.getFechaInicio());
            Date fechaFin = dto.getFechaFin() != null && !dto.getFechaFin().isEmpty()
                    ? Date.valueOf(dto.getFechaFin())
                    : null;

            return grupoEntrenamientoRepository.asignarEntrenadorAGrupo(
                dto.getEntrenadorId(),
                dto.getGrupoId(),
                fechaInicio,
                fechaFin,
                dto.getRol()
        );
    } catch (Exception e) {
        return "Error al asignar entrenador: " + e.getMessage();
    }
    }

    // Otros métodos del servicio (actualizar, eliminar, buscar, etc.)
}
