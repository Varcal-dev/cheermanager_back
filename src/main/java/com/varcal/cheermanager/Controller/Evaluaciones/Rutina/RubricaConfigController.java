package com.varcal.cheermanager.Controller.Evaluaciones.Rutina;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Evaluaciones.Rutina.DriverSubCriterio;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.EscalonDriverNivel;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.NivelCompetencia;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.SubCriterioRubrica;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TablaCantidadNivel;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TopeDriverNivel;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TopeSeccionNivel;
import com.varcal.cheermanager.Service.Evaluaciones.Rutina.RubricaConfigService;
import com.varcal.cheermanager.Utils.RequiresPermission;

@RestController
@RequestMapping("/api/rubrica-config")
public class RubricaConfigController {

    @Autowired
    private RubricaConfigService configService;

    // 400 dato invalido, 409 duplicado o con datos asociados, 404 no encontrado.
    private static ResponseEntity<String> error(RuntimeException e) {
        HttpStatus status = e instanceof IllegalArgumentException ? HttpStatus.BAD_REQUEST
                : e instanceof IllegalStateException ? HttpStatus.CONFLICT
                : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(e.getMessage());
    }

    // ── Niveles ──────────────────────────────────────────────────────────────
    @GetMapping("/niveles")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<NivelCompetencia>> listarNiveles() {
        return ResponseEntity.ok(configService.listarNiveles());
    }

    @PostMapping("/niveles")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> crearNivel(@RequestBody NivelCompetencia nivel) {
        try {
            nivel.setId(null);
            return ResponseEntity.status(201).body(configService.guardarNivel(nivel));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @PutMapping("/niveles/{id}")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> actualizarNivel(@PathVariable Integer id, @RequestBody NivelCompetencia nivel) {
        nivel.setId(id);
        try {
            return ResponseEntity.ok(configService.guardarNivel(nivel));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @DeleteMapping("/niveles/{id}")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> eliminarNivel(@PathVariable Integer id) {
        try {
            configService.eliminarNivel(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    // ── Sub-criterios ────────────────────────────────────────────────────────
    @GetMapping("/sub-criterios")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<SubCriterioRubrica>> listarSubCriterios() {
        return ResponseEntity.ok(configService.listarSubCriterios());
    }

    @PostMapping("/sub-criterios")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> crearSubCriterio(@RequestBody SubCriterioRubrica sc) {
        try {
            sc.setId(null);
            return ResponseEntity.status(201).body(configService.guardarSubCriterio(sc));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    // ── Tablas de cantidad ───────────────────────────────────────────────────
    @GetMapping("/tabla-cantidad/{nivelId}")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<TablaCantidadNivel>> listarTablaCantidad(@PathVariable Integer nivelId) {
        return ResponseEntity.ok(configService.listarTablaCantidad(nivelId));
    }

    @PostMapping("/tabla-cantidad")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> crearFilaTablaCantidad(@RequestBody TablaCantidadNivel fila) {
        try {
            fila.setId(null);
            return ResponseEntity.status(201).body(configService.guardarTablaCantidad(fila));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @PutMapping("/tabla-cantidad/{id}")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> actualizarFilaTablaCantidad(@PathVariable Integer id, @RequestBody TablaCantidadNivel fila) {
        fila.setId(id);
        try {
            return ResponseEntity.ok(configService.guardarTablaCantidad(fila));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @DeleteMapping("/tabla-cantidad/{id}")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> eliminarFilaTablaCantidad(@PathVariable Integer id) {
        configService.eliminarTablaCantidad(id);
        return ResponseEntity.noContent().build();
    }

    // ── Topes de sección ─────────────────────────────────────────────────────
    @GetMapping("/topes-seccion")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<TopeSeccionNivel>> listarTopesSeccion(
            @RequestParam Integer nivelId, @RequestParam Integer subCriterioId) {
        return ResponseEntity.ok(configService.listarTopesSeccion(nivelId, subCriterioId));
    }

    // S7: todos los escalones de un nivel en una sola llamada.
    @GetMapping("/topes-seccion/nivel/{nivelId}")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<TopeSeccionNivel>> listarTopesSeccionPorNivel(@PathVariable Integer nivelId) {
        return ResponseEntity.ok(configService.listarTopesSeccionPorNivel(nivelId));
    }

    @PostMapping("/topes-seccion")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> crearTopeSeccion(@RequestBody TopeSeccionNivel tope) {
        try {
            tope.setId(null);
            return ResponseEntity.status(201).body(configService.guardarTopeSeccion(tope));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @PutMapping("/topes-seccion/{id}")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> actualizarTopeSeccion(@PathVariable Integer id, @RequestBody TopeSeccionNivel tope) {
        tope.setId(id);
        try {
            return ResponseEntity.ok(configService.guardarTopeSeccion(tope));
        } catch (RuntimeException e) {
            return error(e);
        }
    }

    @DeleteMapping("/topes-seccion/{id}")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> eliminarTopeSeccion(@PathVariable Integer id) {
        configService.eliminarTopeSeccion(id);
        return ResponseEntity.noContent().build();
    }

    // ── Drivers ──────────────────────────────────────────────────────────────
    @GetMapping("/drivers/{subCriterioId}")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<DriverSubCriterio>> listarDrivers(@PathVariable Integer subCriterioId) {
        return ResponseEntity.ok(configService.listarDrivers(subCriterioId));
    }

    @PostMapping("/drivers")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> crearDriver(@RequestBody DriverSubCriterio driver) {
        return ResponseEntity.status(201).body(configService.guardarDriver(driver));
    }

    @GetMapping("/topes-driver")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<TopeDriverNivel> obtenerTopeDriver(
            @RequestParam Integer driverId, @RequestParam Integer nivelId) {
        TopeDriverNivel tope = configService.obtenerTopeDriver(driverId, nivelId);
        return tope != null ? ResponseEntity.ok(tope) : ResponseEntity.noContent().build();
    }

    @PostMapping("/topes-driver")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> crearTopeDriver(@RequestBody TopeDriverNivel tope) {
        return ResponseEntity.status(201).body(configService.guardarTopeDriver(tope));
    }

    @PutMapping("/topes-driver/{id}")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> actualizarTopeDriver(@PathVariable Integer id, @RequestBody TopeDriverNivel tope) {
        tope.setId(id);
        return ResponseEntity.ok(configService.guardarTopeDriver(tope));
    }

    @GetMapping("/escalones-driver")
    @RequiresPermission("ver_evaluacion")
    public ResponseEntity<List<EscalonDriverNivel>> listarEscalonesDriver(
            @RequestParam Integer driverId, @RequestParam Integer nivelId) {
        return ResponseEntity.ok(configService.listarEscalonesDriver(driverId, nivelId));
    }

    @PostMapping("/escalones-driver")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> crearEscalonDriver(@RequestBody EscalonDriverNivel escalon) {
        return ResponseEntity.status(201).body(configService.guardarEscalonDriver(escalon));
    }

    @DeleteMapping("/escalones-driver/{id}")
    @RequiresPermission("configurar_rubrica")
    public ResponseEntity<?> eliminarEscalonDriver(@PathVariable Integer id) {
        configService.eliminarEscalonDriver(id);
        return ResponseEntity.noContent().build();
    }
}