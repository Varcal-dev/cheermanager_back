package com.varcal.cheermanager.Service.Eventos;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Eventos.TipoEvento;
import com.varcal.cheermanager.repository.Eventos.TipoEventoRepository;

@Service
public class TipoEventoService {

    @Autowired
    private TipoEventoRepository tipoEventoRepository;

    public List<TipoEvento> listarTodos() {
        return tipoEventoRepository.findAll();
    }

    public TipoEvento obtenerPorId(Integer id) {
        return tipoEventoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de evento no encontrado con ID: " + id));
    }

    public TipoEvento crear(String nombre) {
        tipoEventoRepository.findByEvento(nombre).ifPresent(t -> {
            throw new RuntimeException("Ya existe un tipo de evento llamado '" + nombre + "'");
        });
        TipoEvento tipo = new TipoEvento();
        tipo.setEvento(nombre);
        return tipoEventoRepository.save(tipo);
    }

    public void eliminar(Integer id) {
        if (!tipoEventoRepository.existsById(id)) {
            throw new RuntimeException("Tipo de evento no encontrado con ID: " + id);
        }
        tipoEventoRepository.deleteById(id);
    }
}