package com.varcal.cheermanager.Controller.Financiero;

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

import com.varcal.cheermanager.DTO.Financiero.ConvenioDto;
import com.varcal.cheermanager.Models.Financiero.Convenio;
import com.varcal.cheermanager.Models.Financiero.Descuento;
import com.varcal.cheermanager.Service.Financiero.ConvenioService;
import com.varcal.cheermanager.repository.Financiero.ConvenioRepository;
import com.varcal.cheermanager.repository.Financiero.DescuentoRepository;

@RestController
@RequestMapping("/api/convenios")
public class ConvenioController {

    @Autowired
    private ConvenioService convenioService;

    @Autowired
    private DescuentoRepository descuentoRepository;

    @Autowired
    private ConvenioRepository convenioRepository;

    @GetMapping
    public List<Convenio> listar() {
        return convenioService.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Convenio> obtener(@PathVariable Integer id) {
        Convenio convenio = convenioService.obtenerPorId(id);
        return convenio != null ? ResponseEntity.ok(convenio) : ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<Convenio> crearConvenio(@RequestBody ConvenioDto dto) {
        Descuento descuento = descuentoRepository.findById(dto.getDescuentoId())
                .orElseThrow(() -> new RuntimeException("Descuento no encontrado"));

        Convenio convenio = new Convenio();
        convenio.setNombreEmpresa(dto.getNombreEmpresa());
        convenio.setDescuento(descuento); // <- asigna la entidad completa aquí
        convenio.setFechaInicio(dto.getFechaInicio());
        convenio.setFechaFin(dto.getFechaFin());

        Convenio saved = convenioRepository.save(convenio);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Convenio> actualizar(@PathVariable Integer id, @RequestBody Convenio convenio) {
        Convenio actualizado = convenioService.actualizarConvenio(id, convenio);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        convenioService.eliminarConvenio(id);
        return ResponseEntity.noContent().build();
    }
}
