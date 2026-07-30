package com.varcal.cheermanager.Service.Org_dep;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Org_dep.TipoGrupo;
import com.varcal.cheermanager.repository.Org_dep.TipoGrupoRepository;

@Service
public class TipoGrupoService {
    @Autowired
    private TipoGrupoRepository tipoGrupoRepository;

    public List<TipoGrupo> obtenerTodos() {
        return tipoGrupoRepository.findAll();
    }

    public TipoGrupo obtenerPorId(Integer id) {
        return tipoGrupoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de grupo no encontrado con ID: " + id));
    }

    public TipoGrupo crear(TipoGrupo tipoGrupo) {
        return tipoGrupoRepository.save(tipoGrupo);
    }

    public TipoGrupo actualizar(Integer id, TipoGrupo actualizado) {
        TipoGrupo existente = obtenerPorId(id);
        existente.setNombre(actualizado.getNombre());
        return tipoGrupoRepository.save(existente);
    }

    public void eliminar(Integer id) {
        if (!tipoGrupoRepository.existsById(id)) {
            throw new RuntimeException("Tipo de grupo no encontrado con ID: " + id);
        }
        tipoGrupoRepository.deleteById(id);
    }
}