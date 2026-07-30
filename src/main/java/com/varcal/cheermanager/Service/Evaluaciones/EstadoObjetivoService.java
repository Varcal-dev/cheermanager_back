package com.varcal.cheermanager.Service.Evaluaciones;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Evaluaciones.EstadoObjetivo;
import com.varcal.cheermanager.repository.Evaluaciones.EstadoObjetivoRepository;

@Service
public class EstadoObjetivoService {
    @Autowired
    private EstadoObjetivoRepository estadoObjetivoRepository;

    public List<EstadoObjetivo> obtenerTodos() {
        return estadoObjetivoRepository.findAll();
    }

    public EstadoObjetivo obtenerPorId(Integer id) {
        return estadoObjetivoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estado de objetivo no encontrado"));
    }
}