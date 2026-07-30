package com.varcal.cheermanager.Controller.Org_dep;

import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.DTO.Financiero.ResumenGeneracionCobrosDTO;
import com.varcal.cheermanager.DTO.Org_dep.DeportistaInscritoDTO;
import com.varcal.cheermanager.DTO.Org_dep.DeportistaNoInscritoDTO;
import com.varcal.cheermanager.DTO.Org_dep.InscripcionDetalleDto;
import com.varcal.cheermanager.DTO.Org_dep.InscripcionDto;
import com.varcal.cheermanager.DTO.Org_dep.InscripcionSimpleDTO;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Org_dep.Inscripcion;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.Service.Org_dep.InscripcionService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/inscripciones")
public class InscripcionController {

    @Autowired
    private InscripcionService inscripcionService;

    // Necesario para mapear las Facturas crudas del endpoint de abajo al
    // mismo DTO plano que ya usa FacturaController (ver toResponseDTO).
    @Autowired
    private FacturaService facturaService;

    @GetMapping("/detalle")
    @RequiresPermission("gestionar_inscripciones")
    public ResponseEntity<List<InscripcionDetalleDto>> getDetalleInscripciones() {
        return ResponseEntity.ok(inscripcionService.obtenerDetalleInscripcionesDTO());
    }

    @GetMapping("/simple")
    @RequiresPermission("gestionar_inscripciones")
    public ResponseEntity<List<InscripcionSimpleDTO>> obtenerInscripciones() {
        return ResponseEntity.ok(inscripcionService.listarInscripcionesSimples());
    }

    @GetMapping("/no-inscritos")
    @RequiresPermission("gestionar_inscripciones")
    public ResponseEntity<List<DeportistaNoInscritoDTO>> getDeportistasNoInscritos() {
        return ResponseEntity.ok(inscripcionService.obtenerDeportistasNoInscritosDTO());
    }

    // Faltaba: el front de /grupos (DeportistaElegibleItem) depende de esto
    // para saber a quién se le puede asignar un grupo.
    @GetMapping("/inscritos")
    @RequiresPermission("gestionar_inscripciones")  
    public ResponseEntity<List<DeportistaInscritoDTO>> getDeportistasInscritos() {
        return ResponseEntity.ok(inscripcionService.obtenerDeportistasInscritosDTO());
    }

    @GetMapping("/{id}")
    @RequiresPermission("gestionar_inscripciones")
    public ResponseEntity<?> obtenerUna(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(inscripcionService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // Antes era /{id}/factura (singular) y devolvía una sola Factura cruda.
    // Ahora una inscripción puede tener varias facturas a lo largo del
    // tiempo (la de inscripción + una por cada mes de mensualidad ya
    // facturado), así que pasa a ser plural y devuelve una lista de DTOs
    // planos — el mismo FacturaResponseDTO que usa FacturaController, para
    // no reintroducir el problema de serialización que ya arreglamos ahí.
    @GetMapping("/{id}/facturas")
    @RequiresPermission("gestionar_inscripciones")
    public ResponseEntity<?> obtenerFacturas(@PathVariable Integer id) {
        try {
            List<Factura> facturas = inscripcionService.obtenerFacturasDeInscripcion(id);
            List<FacturaResponseDTO> dtos = facturas.stream()
                    .map(facturaService::toResponseDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // Dispara la generación de cobros de mensualidad de un período (formato
    // yyyy-MM). Si no se indica `periodo`, usa el mes actual. Es manual a
    // propósito — ver comentario en InscripcionService.generarCobrosMensualidad.
    @PostMapping("/generar-cobros-mensualidad")
    @RequiresPermission("gestionar_inscripciones")
    public ResponseEntity<?> generarCobrosMensualidad(@RequestParam(required = false) String periodo) {
        try {
            String periodoFinal = (periodo != null && !periodo.isBlank())
                    ? periodo
                    : YearMonth.now().toString();
            ResumenGeneracionCobrosDTO resumen = inscripcionService.generarCobrosMensualidad(periodoFinal);
            return ResponseEntity.ok(resumen);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        }
    }

    @PostMapping
    @RequiresPermission("gestionar_inscripciones")
    public ResponseEntity<?> crear(@RequestBody InscripcionDto inscripcionDto) {
        try {
            Inscripcion nueva = inscripcionService.crearInscripcion(inscripcionDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al crear la inscripción: " + e.getMessage());
        }
    }

    // Ojo con el body: el service espera un Inscripcion parcial, no el DTO
    // plano. Debe llegar así:
    // { "planPago": { "id": 5 }, "planMensualidad": { "id": 8 },
    // "fechaInscripcion": "2026-07-12", "fechaVencimiento": "2026-08-12",
    // "estado": "Activa" }
    // (el service solo usa esos campos; no toca al deportista existente).
    @PutMapping("/{id}")
    @RequiresPermission("gestionar_inscripciones")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody Inscripcion inscripcion) {
        try {
            Inscripcion actualizada = inscripcionService.actualizarInscripcion(id, inscripcion);
            return ResponseEntity.ok(actualizada);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al actualizar la inscripción: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("gestionar_inscripciones")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            inscripcionService.eliminarInscripcion(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}