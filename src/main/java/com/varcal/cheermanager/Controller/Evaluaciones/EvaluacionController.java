package com.varcal.cheermanager.Controller.Evaluaciones;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.varcal.cheermanager.DTO.Evaluaciones.EvaluacionActualizarDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.EvaluacionDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.EvaluacionPorCriterioDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.EvaluacionResponseDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.PaginaEvaluacionesDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.PromedioCriterioDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.RegistroEvaluacionMasivoDTO;
import com.varcal.cheermanager.Service.Evaluaciones.EvaluacionService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/evaluaciones")
public class EvaluacionController {

    @Autowired
    private EvaluacionService evaluacionService;

    // 400 dato invalido, 409 duplicado/regla, 404 no encontrado.
    private static ResponseEntity<String> error(RuntimeException e) {
        HttpStatus status = e instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST
                : e instanceof IllegalStateException ? HttpStatus.CONFLICT
                : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(e.getMessage());
    }

    private static HttpStatus estadoLote(EvaluacionService.ResultadoRegistroMasivo r) {
        return r.getErrores().isEmpty() ? HttpStatus.CREATED : HttpStatus.MULTI_STATUS;
    }

    @PostMapping
    @RequiresPermission("crear_evaluacion")
    public ResponseEntity<?> registrar(@RequestBody EvaluacionDTO dto) {
        try {
            return ResponseEntity.status(201).body(evaluacionService.registrar(dto));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    // Sin filtros ni paginacion: se conserva por compatibilidad. La pantalla de Evaluaciones usa /buscar.
    @GetMapping
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<EvaluacionResponseDTO>> listarTodas() {
        return ResponseEntity.ok(evaluacionService.obtenerTodas());
    }

    // S7: busqueda paginada. Todos los filtros son opcionales.
    @GetMapping("/buscar")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<?> buscar(
            @RequestParam(required = false) Integer deportistaId,
            @RequestParam(required = false) Integer grupoId,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) Integer criterioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Integer pagina,
            @RequestParam(required = false) Integer tamano,
            // orden: fecha | deportista | categoria | criterio | puntaje. direccion: asc | desc.
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String direccion) {
        try {
            PaginaEvaluacionesDTO p = evaluacionService.buscar(deportistaId, grupoId, categoriaId, criterioId, desde,
                    hasta, texto, pagina, tamano, orden, direccion);
            return ResponseEntity.ok(p);
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    // S7: promedio por criterio (opcionalmente de un grupo, una categoria y un rango de fechas).
    @GetMapping("/promedios")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<?> promedios(
            @RequestParam(required = false) Integer grupoId,
            @RequestParam(required = false) Integer categoriaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        try {
            List<PromedioCriterioDTO> lista = evaluacionService.promedios(grupoId, categoriaId, desde, hasta);
            return ResponseEntity.ok(lista);
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    // S7: quienes estan hoy en el grupo (para armar el formulario de captura).
    @GetMapping("/grupo/{grupoId}/deportistas")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<Map<String, Object>>> deportistasDelGrupo(@PathVariable Integer grupoId) {
        return ResponseEntity.ok(evaluacionService.deportistasDelGrupo(grupoId));
    }

    // El caso de uso real: evaluar a un deportista en varios criterios de una sola sesion. Devuelve 207 si algun
    // criterio del lote fallo mientras el resto si se guardo.
    @PostMapping("/masivo")
    @RequiresPermission("crear_evaluacion")
    public ResponseEntity<?> registrarMasivo(@RequestBody RegistroEvaluacionMasivoDTO dto) {
        try {
            EvaluacionService.ResultadoRegistroMasivo r = evaluacionService.registrarMasivo(dto);
            return ResponseEntity.status(estadoLote(r)).body(r);
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    // S7: un criterio evaluado a varios deportistas el mismo dia (p. ej. todo un grupo). Los errores salen por deportistaId.
    @PostMapping("/por-criterio")
    @RequiresPermission("crear_evaluacion")
    public ResponseEntity<?> registrarPorCriterio(@RequestBody EvaluacionPorCriterioDTO dto) {
        try {
            EvaluacionService.ResultadoRegistroMasivo r = evaluacionService.registrarPorCriterio(dto);
            return ResponseEntity.status(estadoLote(r)).body(r);
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    // S7: corregir puntaje y fecha. Permiso modificar_evaluacion (ya existia en el catalogo).
    @PutMapping("/{id}")
    @RequiresPermission("modificar_evaluacion")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody EvaluacionActualizarDTO dto) {
        try {
            return ResponseEntity.ok(evaluacionService.actualizar(id, dto));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @GetMapping("/deportista/{deportistaId}")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<?> listarPorDeportista(@PathVariable Integer deportistaId) {
        try {
            return ResponseEntity.ok(evaluacionService.listarPorDeportista(deportistaId));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @GetMapping("/deportista/{deportistaId}/rango")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<EvaluacionResponseDTO>> listarPorRango(
            @PathVariable Integer deportistaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(evaluacionService.listarPorDeportistaYRango(deportistaId, desde, hasta));
    }

    // Para graficar la evolucion de un deportista en un criterio especifico.
    @GetMapping("/deportista/{deportistaId}/criterio/{criterioId}/progresion")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<EvaluacionResponseDTO>> progresionPorCriterio(
            @PathVariable Integer deportistaId, @PathVariable Integer criterioId) {
        return ResponseEntity.ok(evaluacionService.progresionPorCriterio(deportistaId, criterioId));
    }

    // Foto tecnica actual: ultima evaluacion en cada criterio de una categoria.
    @GetMapping("/deportista/{deportistaId}/categoria/{categoriaId}/actual")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<EvaluacionResponseDTO>> ultimaEvaluacionPorCategoria(
            @PathVariable Integer deportistaId, @PathVariable Integer categoriaId) {
        return ResponseEntity.ok(evaluacionService.ultimaEvaluacionPorCategoria(deportistaId, categoriaId));
    }

    @GetMapping("/deportista/{deportistaId}/criterio/{criterioId}/promedio")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<Map<String, Object>> promedioPorCriterio(
            @PathVariable Integer deportistaId, @PathVariable Integer criterioId) {
        Double promedio = evaluacionService.promedioPorCriterio(deportistaId, criterioId);
        return ResponseEntity.ok(Map.of("deportistaId", deportistaId, "criterioId", criterioId, "promedio", promedio));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("eliminar_evaluacion")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            evaluacionService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return error(e);
        }
    }
}