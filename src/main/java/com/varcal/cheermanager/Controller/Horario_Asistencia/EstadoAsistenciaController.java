package com.varcal.cheermanager.Controller.Horario_Asistencia;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Horario_Asistencia.EstadoAsistencia;
import com.varcal.cheermanager.repository.Horario_Asistencia.EstadoAsistenciaRepository;

// Catálogo de solo lectura, mismo motivo que DiaSemanaController: el
// repository ya existía (lo usa AsistenciaService) pero nada lo exponía por
// HTTP, así que el frontend no tenía cómo pintar el selector de "Presente /
// Ausente / ..." al registrar asistencia.
@RestController
@RequestMapping("/api/estados-asistencia")
public class EstadoAsistenciaController {

    @Autowired
    private EstadoAsistenciaRepository estadoAsistenciaRepository;

    @GetMapping
    public List<EstadoAsistencia> listar() {
        return estadoAsistenciaRepository.findAll();
    }
}