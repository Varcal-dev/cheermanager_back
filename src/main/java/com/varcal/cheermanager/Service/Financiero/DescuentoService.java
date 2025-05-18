package com.varcal.cheermanager.Service.Financiero;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Financiero.Descuento;
import com.varcal.cheermanager.repository.Financiero.DescuentoRepository;

@Service
public class DescuentoService {
    @Autowired
    private DescuentoRepository repository;

    public List<Descuento> obtenerTodos() {
        return repository.findAll();
    }

    public Optional<Descuento> obtenerPorId(Integer id) {
        return repository.findById(id);
    }

    public Descuento crear(Descuento descuento) {
        return repository.save(descuento);
    }

    public Optional<Descuento> actualizar(Integer id, Descuento nuevo) {
        return repository.findById(id).map(descuento -> {
            descuento.setNombre(nuevo.getNombre());
            descuento.setDescripcion(nuevo.getDescripcion());
            descuento.setPorcentaje(nuevo.getPorcentaje());
            descuento.setFechaInicio(nuevo.getFechaInicio());
            descuento.setFechaFin(nuevo.getFechaFin());
            descuento.setActivo(nuevo.getActivo());
            return repository.save(descuento);
        });
    }

    public boolean eliminar(Integer id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            return true;
        }
        return false;
    }
}
