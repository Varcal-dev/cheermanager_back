package com.varcal.cheermanager.Controller.Org_dep;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Org_dep.DiaSemana;
import com.varcal.cheermanager.repository.Org_dep.DiaSemanaRepository;

// Catálogo de solo lectura. DiaSemanaRepository ya existía (lo agregó
// HorarioService en su momento), pero no tenía Controller — el frontend no
// tenía forma de pedir la lista de días para el selector de "Nuevo Horario".
// Misma convención que TipoGrupoController: sin @RequiresPermission.
@RestController
@RequestMapping("/api/dias-semana")
public class DiaSemanaController {

    @Autowired
    private DiaSemanaRepository diaSemanaRepository;

    @GetMapping
    public List<DiaSemana> listar() {
        return diaSemanaRepository.findAll();
    }
}