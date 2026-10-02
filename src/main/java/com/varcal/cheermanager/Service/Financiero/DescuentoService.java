package com.varcal.cheermanager.Service.Financiero;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Financiero.Descuento;
import com.varcal.cheermanager.repository.Financiero.ConvenioRepository;
import com.varcal.cheermanager.repository.Financiero.DescuentoRepository;

@Service
public class DescuentoService {
    @Autowired
    private DescuentoRepository repository;

    @Autowired
    private ConvenioRepository convenioRepository;

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
        if (!repository.existsById(id)) {
            return false;
        }

        // Antes esto dejaba que MySQL rechazara el DELETE por la FK
        // convenios.descuento_id, lo que Spring traduce en un 500 genérico
        // de integridad de datos sin mensaje útil. Validamos acá primero
        // para devolver un mensaje claro que el frontend ya sabe mostrar
        // en un toast. Si el descuento está en uso, lo correcto es
        // desactivarlo (activo=false vía actualizar), no borrarlo.
        if (convenioRepository.existsByDescuentoId(id)) {
            throw new RuntimeException(
                    "No se puede eliminar: este descuento está en uso por al menos un convenio. Desactívalo en su lugar.");
        }

        repository.deleteById(id);
        return true;
    }
}