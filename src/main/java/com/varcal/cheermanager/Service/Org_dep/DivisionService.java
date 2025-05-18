package com.varcal.cheermanager.Service.Org_dep;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Org_dep.Division;
import com.varcal.cheermanager.repository.Org_dep.DivisionRepository;

@Service
public class DivisionService {
    @Autowired
    private DivisionRepository divisionRepository;

    public List<Division> obtenerTodas() {
        return divisionRepository.findAll();
    }

    public Division obtenerPorId(Integer id) {
        return divisionRepository.findById(id).orElseThrow(() -> new RuntimeException("División no encontrada"));
    }

    public Division crear(Division division) {
        return divisionRepository.save(division);
    }

    public Division actualizar(Integer id, Division divisionActualizada) {
        Division existente = obtenerPorId(id);
        existente.setNombre(divisionActualizada.getNombre());
        return divisionRepository.save(existente);
    }

    public void eliminar(Integer id) {
        divisionRepository.deleteById(id);
    }
}
