package com.varcal.cheermanager.Controller.Financiero;

import com.varcal.cheermanager.Utils.RequiresPermission;

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

import com.varcal.cheermanager.Models.Org_dep.Division;
import com.varcal.cheermanager.Service.Org_dep.DivisionService;

@RestController
@RequestMapping("/api/divisiones")
public class DivisionController {

    @Autowired
    private DivisionService divisionService;

    @GetMapping
    @RequiresPermission("ver_division")
    public List<Division> listar() {
        return divisionService.obtenerTodas();
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_division")
    public ResponseEntity<Division> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(divisionService.obtenerPorId(id));
    }

    @PostMapping
    @RequiresPermission("gestionar_division")
    public ResponseEntity<Division> crear(@RequestBody Division division) {
        return ResponseEntity.status(HttpStatus.CREATED).body(divisionService.crear(division));
    }

    @PutMapping("/{id}")
    @RequiresPermission("gestionar_division")
    public ResponseEntity<Division> actualizar(@PathVariable Integer id, @RequestBody Division division) {
        return ResponseEntity.ok(divisionService.actualizar(id, division));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("gestionar_division")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        divisionService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}