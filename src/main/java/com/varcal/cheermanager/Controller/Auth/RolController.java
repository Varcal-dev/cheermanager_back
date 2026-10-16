package com.varcal.cheermanager.Controller.Auth;

import com.varcal.cheermanager.Utils.RequiresPermission;

import java.util.List;
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

import com.varcal.cheermanager.DTO.Ath.CrearRolConPermisosDTO;
import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Service.Auth.RolService;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final RolService rolService;

    @Autowired
    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    @RequiresPermission("ver_roles")
    public ResponseEntity<List<Rol>> getAllRoles() {
        List<Rol> roles = rolService.listar();
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_roles")
    public ResponseEntity<Rol> getRolById(@PathVariable Integer id) {
        Rol rol = rolService.getRolById(id);
        return rol != null ? ResponseEntity.ok(rol) : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/permisos")
    @RequiresPermission("ver_roles")
    public ResponseEntity<Object> getPermisosByRol(@PathVariable Integer id) {
        Rol rol = rolService.getRolById(id);
        if (rol == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rol.getPermisos());
    }

    @PostMapping
    @RequiresPermission("gestionar_roles")
    public ResponseEntity<Rol> createRol(@RequestBody Rol rol) {
        Rol createdRol = rolService.createRol(rol);
        return ResponseEntity.ok(createdRol);
    }

    @PutMapping("/{id}")
    @RequiresPermission("gestionar_roles")
    public ResponseEntity<Rol> modificarRol(@PathVariable Integer id, @RequestBody Rol rolDetails) {
        Rol updatedRol = rolService.modificarRol(id, rolDetails);
        return updatedRol != null ? ResponseEntity.ok(updatedRol) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("gestionar_roles")
    public ResponseEntity<Void> deleteRol(@PathVariable Integer id) {
        rolService.deleteRol(id);
        return ResponseEntity.ok().build();
    }

    /*
     * @GetMapping("/user-counts")
     * public ResponseEntity<List<Map<String, Object>>> getRolesWithUserCount() {
     * List<Map<String, Object>> rolesWithUserCount =
     * rolService.getRolesWithUserCount();
     * return ResponseEntity.ok(rolesWithUserCount);
     * }
     */

    @PostMapping("/asignar-permisos")
    @RequiresPermission("ASIGNAR_PERMISO")
    public ResponseEntity<Rol> asignarPermisosARol(@RequestBody CrearRolConPermisosDTO request) {
        Rol rolActualizado = rolService.assignPermissionsToRol(request.getRolId(), request.getPermisoIds());
        return ResponseEntity.ok(rolActualizado);
    }
}
