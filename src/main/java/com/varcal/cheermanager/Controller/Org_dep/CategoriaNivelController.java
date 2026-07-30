package com.varcal.cheermanager.Controller.Org_dep;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Org_dep.CategoriaNivel;
import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.repository.Org_dep.CategoriaNivelRepository;

@RestController
@RequestMapping("/api/categorias-nivel")
public class CategoriaNivelController {

    private final CategoriaNivelRepository categoriaNivelRepo;

    public CategoriaNivelController(CategoriaNivelRepository categoriaNivelRepo) {
        this.categoriaNivelRepo = categoriaNivelRepo;
    }

    @GetMapping
    @RequiresPermission("ver_categoria_nivel")
    public List<CategoriaNivel> listarTodos() {
        return categoriaNivelRepo.findAll();
    }
}
