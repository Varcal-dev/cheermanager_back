package com.varcal.cheermanager.Controller.Auth;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.Models.Auth.Permiso;
import com.varcal.cheermanager.Service.Auth.PermisoService;

@RestController
@RequestMapping("/api/permisos")
public class PermisoController {

    @Autowired
    private PermisoService permisoService;

    @GetMapping("/listar")
    public ResponseEntity<Object> listarPermisos() {
        return ResponseEntity.ok(permisoService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Permiso> obtenerPermisoPorId(@PathVariable Integer id) {
        Optional<Permiso> permiso = permisoService.obtenerPorId(id);
        return permiso.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Permiso> crearPermiso(@RequestBody Permiso permiso) {
        Permiso nuevoPermiso = permisoService.guardar(permiso);
        return ResponseEntity.ok(nuevoPermiso);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Permiso> actualizarPermiso(@PathVariable Integer id, @RequestBody Permiso permiso) {
        Optional<Permiso> permisoExistente = permisoService.obtenerPorId(id);
        if (permisoExistente.isPresent()) {
            permiso.setId(id);
            Permiso permisoActualizado = permisoService.guardar(permiso);
            return ResponseEntity.ok(permisoActualizado);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarPermiso(@PathVariable Integer id) {
        if (permisoService.obtenerPorId(id).isPresent()) {
            permisoService.eliminar(id);
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}