package com.varcal.cheermanager.Controller.Org_dep;

import java.util.List;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Org_dep.TipoGrupo;
import com.varcal.cheermanager.repository.Org_dep.TipoGrupoRepository;

@RestController
@RequestMapping("/api/tipo-grupos")
@CrossOrigin(origins = "*")
public class TipoGrupoController {

    private final TipoGrupoRepository tipoGrupoRepo;

    public TipoGrupoController(TipoGrupoRepository tipoGrupoRepo) {
        this.tipoGrupoRepo = tipoGrupoRepo;
    }

    @GetMapping
    public List<TipoGrupo> listarTodos() {
        return tipoGrupoRepo.findAll();
    }
}
