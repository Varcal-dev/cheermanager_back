package com.varcal.cheermanager.Controller.Eventos;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Eventos.TipoEvento;
import com.varcal.cheermanager.repository.Eventos.TipoEventoRepository;

@RestController
@RequestMapping("/api/tipos-evento")
public class TipoEventoController {

    private final TipoEventoRepository tipoEventoRepo;

    public TipoEventoController(TipoEventoRepository tipoEventoRepo) {
        this.tipoEventoRepo = tipoEventoRepo;
    }

    @GetMapping
    public List<TipoEvento> listarTodos() {
        return tipoEventoRepo.findAll();
    }
}