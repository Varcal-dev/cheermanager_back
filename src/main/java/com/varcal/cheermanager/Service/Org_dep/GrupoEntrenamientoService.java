package com.varcal.cheermanager.Service.Org_dep;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

import jakarta.transaction.Transactional;

@Service
public class GrupoEntrenamientoService {

    @Autowired
    private GrupoEntrenamientoRepository grupoEntrenamientoRepository;
    @Autowired
    private Deportista deportista;
    @Autowired
    private DeportistaRepository deportistaRepository;

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

    // Otros métodos del servicio (actualizar, eliminar, buscar, etc.)
}
