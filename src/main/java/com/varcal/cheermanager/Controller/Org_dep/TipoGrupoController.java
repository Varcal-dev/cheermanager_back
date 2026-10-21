package com.varcal.cheermanager.Controller.Org_dep;

import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.Utils.SinPermisoEspecifico;

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

import com.varcal.cheermanager.Models.Org_dep.TipoGrupo;
import com.varcal.cheermanager.Service.Org_dep.TipoGrupoService;

@RestController
@RequestMapping("/api/tipo-grupos")
public class TipoGrupoController {

    @Autowired
    private TipoGrupoService tipoGrupoService;

    @GetMapping
    @SinPermisoEspecifico("catalogo de lectura")
    public List<TipoGrupo> listarTodos() {
        return tipoGrupoService.obtenerTodos();
    }

    @GetMapping("/{id}")
    @SinPermisoEspecifico("catalogo de lectura")
    public ResponseEntity<?> obtener(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(tipoGrupoService.obtenerPorId(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping
    @RequiresPermission("gestionar_catalogos")
    public ResponseEntity<TipoGrupo> crear(@RequestBody TipoGrupo tipoGrupo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoGrupoService.crear(tipoGrupo));
    }

    @PutMapping("/{id}")
    @RequiresPermission("gestionar_catalogos")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody TipoGrupo tipoGrupo) {
        try {
            return ResponseEntity.ok(tipoGrupoService.actualizar(id, tipoGrupo));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("gestionar_catalogos")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            tipoGrupoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }
}