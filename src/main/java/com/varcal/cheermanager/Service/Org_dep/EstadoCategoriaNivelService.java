package com.varcal.cheermanager.Service.Org_dep;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Org_dep.EstadoCategoriaNivel;
import com.varcal.cheermanager.repository.Org_dep.EstadoCategoriaNivelRepository;

@Service
public class EstadoCategoriaNivelService {
    @Autowired
    private EstadoCategoriaNivelRepository estadoCategoriaNivelRepository;

    public List<EstadoCategoriaNivel> obtenerTodos() {
        return estadoCategoriaNivelRepository.findAll();
    }

    public EstadoCategoriaNivel obtenerPorId(Integer id) {
        return estadoCategoriaNivelRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de categoría no encontrado con ID: " + id));
    }

    public EstadoCategoriaNivel crear(EstadoCategoriaNivel estado) {
        return estadoCategoriaNivelRepository.save(estado);
    }

    public EstadoCategoriaNivel actualizar(Integer id, EstadoCategoriaNivel actualizado) {
        EstadoCategoriaNivel existente = obtenerPorId(id);
        existente.setNombre(actualizado.getNombre());
        return estadoCategoriaNivelRepository.save(existente);
    }

    public void eliminar(Integer id) {
        if (!estadoCategoriaNivelRepository.existsById(id)) {
            throw new RuntimeException("Estado de categoría no encontrado con ID: " + id);
        }
        estadoCategoriaNivelRepository.deleteById(id);
    }
}