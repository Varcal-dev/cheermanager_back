package com.varcal.cheermanager.Controller.Horario_Asistencia;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Horario_Asistencia.AsignarHorarioGrupoDTO;
import com.varcal.cheermanager.DTO.Horario_Asistencia.HorarioDTO;
import com.varcal.cheermanager.DTO.Horario_Asistencia.HorarioResponseDTO;
import com.varcal.cheermanager.Models.Horario_Asistencia.Horario;
import com.varcal.cheermanager.Models.Horario_Asistencia.HorarioEntrenamiento;
import com.varcal.cheermanager.Service.Horario_Asistencia.HorarioService;

// NOTA de estilo: este controller, igual que su hermano GrupoEntrenamientoController
// en Org_dep, no usa @RequiresPermission. Mantengo la misma convención que ya
// tiene esa carpeta en vez de introducir un estándar nuevo a medias.
@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

    @Autowired
    private HorarioService horarioService;

    @PostMapping
    public ResponseEntity<Horario> crear(@RequestBody HorarioDTO dto) {
        Horario horario = horarioService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(horario);
    }

    @GetMapping
    public ResponseEntity<List<HorarioResponseDTO>> listar() {
        return ResponseEntity.ok(horarioService.listarConGrupos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HorarioResponseDTO> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(horarioService.obtenerPorId(id));
    }

    // Todos los bloques de horario de un grupo (ej. para mostrar "este grupo
    // entrena martes y jueves de 4 a 6 pm").
    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<Horario>> listarPorGrupo(@PathVariable Integer grupoId) {
        return ResponseEntity.ok(horarioService.listarPorGrupo(grupoId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Horario> actualizar(@PathVariable Integer id, @RequestBody HorarioDTO dto) {
        return ResponseEntity.ok(horarioService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        horarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // Asignar un horario existente a un grupo existente (ej. "el horario de
    // martes/jueves 4-6pm aplica también para el grupo Nivel 3").
    @PostMapping("/asignar-grupo")
    public ResponseEntity<HorarioEntrenamiento> asignarAGrupo(@RequestBody AsignarHorarioGrupoDTO dto) {
        HorarioEntrenamiento he = horarioService.asignarAGrupo(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(he);
    }

    @DeleteMapping("/asignacion/{horarioEntrenamientoId}")
    public ResponseEntity<Void> desasignarDeGrupo(@PathVariable Integer horarioEntrenamientoId) {
        horarioService.desasignarDeGrupo(horarioEntrenamientoId);
        return ResponseEntity.noContent().build();
    }
} 