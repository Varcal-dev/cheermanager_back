package com.varcal.cheermanager.Controller.Horario_Asistencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Horario_Asistencia.AsistenciaDTO;
import com.varcal.cheermanager.DTO.Horario_Asistencia.AsistenciaResponseDTO;
import com.varcal.cheermanager.DTO.Horario_Asistencia.RegistroAsistenciaMasivoDTO;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Service.Horario_Asistencia.AsistenciaService;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;

@RestController
@RequestMapping("/api/asistencias")
public class AsistenciaController {

    @Autowired
    private AsistenciaService asistenciaService;

    @Autowired
    private DeportistaPerteneceGrupoRepository deportistaPerteneceGrupoRepository;

    @PostMapping
    public ResponseEntity<AsistenciaResponseDTO> registrar(@RequestBody AsistenciaDTO dto) {
        AsistenciaResponseDTO registrada = asistenciaService.registrar(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(registrada);
    }

    // El endpoint del día a día: el entrenador pasa lista de todo el grupo de
    // una vez. Devuelve 207 (Multi-Status) si algún deportista del lote falló
    // (ej. ID inexistente) mientras el resto sí se guardó correctamente.
    @PostMapping("/masivo")
    public ResponseEntity<AsistenciaService.ResultadoRegistroMasivo> registrarMasivo(@RequestBody RegistroAsistenciaMasivoDTO dto) {
        AsistenciaService.ResultadoRegistroMasivo resultado = asistenciaService.registrarMasivo(dto);
        HttpStatus status = resultado.getErrores().isEmpty() ? HttpStatus.CREATED : HttpStatus.MULTI_STATUS;
        return ResponseEntity.status(status).body(resultado);
    }

    // Antes de pasar lista: quiénes son los deportistas activos de este
    // grupo, para que el frontend construya el formulario de asistencia.
    @GetMapping("/grupo/{grupoId}/deportistas-activos")
    public ResponseEntity<List<Map<String, Object>>> deportistasActivosDelGrupo(@PathVariable Integer grupoId) {
        List<DeportistaPerteneceGrupo> activos = deportistaPerteneceGrupoRepository.findByGrupoIdAndFechaFinIsNull(grupoId);
        List<Map<String, Object>> resultado = activos.stream().map(dpg -> {
            var deportista = dpg.getDeportista();
            return Map.<String, Object>of(
                    "deportistaId", deportista.getId(),
                    "nombre", deportista.getPersona().getNombre() + " " + deportista.getPersona().getApellidos());
        }).collect(Collectors.toList());
        return ResponseEntity.ok(resultado);
    }

    // Lista ya tomada de un grupo en una fecha (para revisar o corregir).
    @GetMapping("/grupo/{grupoId}")
    public ResponseEntity<List<AsistenciaResponseDTO>> listarPorGrupoYFecha(
            @PathVariable Integer grupoId,
            @RequestParam LocalDate fecha) {
        return ResponseEntity.ok(asistenciaService.listarPorGrupoYFecha(grupoId, fecha));
    }

    @GetMapping("/deportista/{deportistaId}")
    public ResponseEntity<List<AsistenciaResponseDTO>> listarPorDeportista(@PathVariable Integer deportistaId) {
        return ResponseEntity.ok(asistenciaService.listarPorDeportista(deportistaId));
    }

    @GetMapping("/deportista/{deportistaId}/rango")
    public ResponseEntity<List<AsistenciaResponseDTO>> listarPorDeportistaYRango(
            @PathVariable Integer deportistaId,
            @RequestParam LocalDate desde,
            @RequestParam LocalDate hasta) {
        return ResponseEntity.ok(asistenciaService.listarPorDeportistaYRango(deportistaId, desde, hasta));
    }

    // % de asistencia de un deportista en un rango (ej. para reportes
    // mensuales o para detectar deportistas con baja asistencia).
    @GetMapping("/deportista/{deportistaId}/porcentaje")
    public ResponseEntity<Map<String, Object>> porcentajeAsistencia(
            @PathVariable Integer deportistaId,
            @RequestParam LocalDate desde,
            @RequestParam LocalDate hasta) {
        double porcentaje = asistenciaService.calcularPorcentajeAsistencia(deportistaId, desde, hasta);
        return ResponseEntity.ok(Map.of(
                "deportistaId", deportistaId,
                "desde", desde,
                "hasta", hasta,
                "porcentajeAsistencia", porcentaje));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        asistenciaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}