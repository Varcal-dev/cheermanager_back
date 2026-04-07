package com.varcal.cheermanager.Controller.Persona;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.varcal.cheermanager.DTO.Persona.DeportistaDTO;
import com.varcal.cheermanager.DTO.Persona.DeportistaVistaDTO;
import com.varcal.cheermanager.DTO.Persona.DeportistaPerfilCompletoDTO;
import com.varcal.cheermanager.DTO.Persona.VincularDeportistaUsuarioDTO;
import com.varcal.cheermanager.Service.Persona.HistorialMedicoService;
import com.varcal.cheermanager.DTO.Persona.HistorialMedicoDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@RestController
@RequestMapping("/api/deportistas")
public class DeprotistasController {
    @Autowired
    private PersonaService personaService;

    @Autowired
    private ConvenioRepository convenioRepository;

    @Autowired
    private HistorialMedicoService historialMedicoService;

    // Método para registrar un deportista
    @PostMapping()
    @RequiresPermission("crear_deportista")
    public ResponseEntity<?> registrarDeportista(@RequestBody DeportistaDTO deportistaDTO) {
        try {
            Deportista deportista = personaService.registrarDeportista(deportistaDTO);
            return ResponseEntity.ok(deportista);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al registrar el deportista: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @RequiresPermission("modificar_deportista")
    public ResponseEntity<?> modificarDeportista(@PathVariable Integer id, @RequestBody DeportistaDTO deportistaDTO) {
        try {
            Deportista deportista = personaService.modificarDeportista(id, deportistaDTO);
            return ResponseEntity.ok(deportista); // Devolver el deportista actualizado
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al modificar el deportista: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/perfil-completo")
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> obtenerPerfilCompleto(@PathVariable Integer id) {
        try {
            DeportistaPerfilCompletoDTO perfil = personaService.obtenerPerfilCompleto(id);
            return ResponseEntity.ok(perfil);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al obtener el perfil completo: " + e.getMessage());
        }
    }

    @GetMapping()
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> listarDeportistas(
            @RequestParam(value = "estado", required = false) Integer estadoId,
            @RequestParam(value = "nivel", required = false) Integer nivelId,
            @RequestParam(value = "grupo", required = false) Integer grupoId) {
        try {
            List<DeportistaVistaDTO> deportistas;

            // Si hay filtros, usar método con filtros
            if (estadoId != null || nivelId != null || grupoId != null) {
                deportistas = personaService.listarDeportistasConFiltros(estadoId, nivelId, grupoId);
            } else {
                // Si no hay filtros, listar todos los deportistas con detalles
                deportistas = personaService.listarDeportistasConDetalles();
            }

            return ResponseEntity.ok(deportistas);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al listar los deportistas: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    @RequiresPermission("eliminar_deportista")
    public ResponseEntity<?> eliminarDeportista(@PathVariable Integer id) {
        try {
            personaService.eliminarDeportista(id);
            return ResponseEntity.ok("Deportista eliminado exitosamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al eliminar el deportista: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/convenio")
    public ResponseEntity<?> asignarConvenio(
            @PathVariable Integer id,
            @RequestBody Integer convenioId) {
        Optional<Deportista> optional = deportistaRepository.findById(id);
        Optional<Convenio> convenio = convenioRepository.findById(convenioId);

        if (optional.isEmpty() || convenio.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Deportista deportista = optional.get();
        deportista.setConvenioId(convenio.get().getId());
        deportistaRepository.save(deportista);

        return ResponseEntity.ok("Convenio asignado correctamente.");
    }

    @PostMapping("/{id}/usuario")
    @RequiresPermission("modificar_deportista")
    public ResponseEntity<?> vincularUsuario(
            @PathVariable Integer id,
            @RequestBody VincularDeportistaUsuarioDTO dto) {
        try {
            personaService.vincularDeportistaConUsuario(id, dto.getUsuarioId());
            return ResponseEntity.ok("Deportista vinculado al usuario exitosamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al vincular deportista con usuario: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/usuario")
    @RequiresPermission("modificar_deportista")
    public ResponseEntity<?> desvinculcarUsuario(@PathVariable Integer id) {
        try {
            personaService.desvinculcarDeportistaDeUsuario(id);
            return ResponseEntity.ok("Deportista desvinculado del usuario exitosamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al desvinculcar deportista: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/usuario")
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> obtenerUsuarioVinculado(@PathVariable Integer id) {
        try {
            Usuario usuario = personaService.obtenerUsuarioDeDeportista(id);
            return ResponseEntity.ok(usuario);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al obtener usuario vinculado: " + e.getMessage());
        }
    }

    @PostMapping("/validar-documento")
    public ResponseEntity<?> validarDocumento(@RequestParam(value = "documento") String numeroDocumento) {
        try {
            ValidarDocumentoDTO resultado = personaService.validarDocumentoUnico(numeroDocumento);
            return ResponseEntity.ok(resultado);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al validar documento: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/historial-estado")
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> obtenerHistorialEstado(@PathVariable Integer id) {
        try {
            List<HistorialDeportistaEstadoDTO> historial = personaService.obtenerHistorialEstadoDeportista(id);
            return ResponseEntity.ok(historial);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al obtener historial de estado: " + e.getMessage());
        }
    }

    @GetMapping("/historial-estado/todos")
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> obtenerHistorialEstadoGlobal() {
        try {
            List<HistorialDeportistaEstadoDTO> historial = personaService.obtenerHistorialEstadoGlobal();
            return ResponseEntity.ok(historial);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al obtener historial global de estado: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/foto")
    @RequiresPermission("modificar_deportista")
    public ResponseEntity<?> guardarFoto(
            @PathVariable Integer id,
            @RequestParam("archivo") MultipartFile archivo) {
        try {
            var resultado = personaService.guardarFotoDeportista(id, archivo);
            if (resultado.isExitoso()) {
                return ResponseEntity.ok(resultado);
            } else {
                return ResponseEntity.status(400).body(resultado);
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al guardar la foto: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/foto")
    @RequiresPermission("ver_deportista")
    public ResponseEntity<?> obtenerFoto(@PathVariable Integer id) {
        try {
            byte[] imagen = personaService.obtenerFotoDeportista(id);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(imagen);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al obtener la foto: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/foto")
    @RequiresPermission("modificar_deportista")
    public ResponseEntity<?> eliminarFoto(@PathVariable Integer id) {
        try {
            personaService.eliminarFotoDeportista(id);
            return ResponseEntity.ok("Foto eliminada exitosamente");
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al eliminar la foto: " + e.getMessage());
        }
    }

    // Endpoints para historial médico
    @PostMapping("/{id}/historial-medico")
    @RequiresPermission("crear_historial_medico")
    public ResponseEntity<?> crearHistorialMedico(@PathVariable Integer id, @RequestBody HistorialMedicoDTO dto) {
        try {
            dto.setPersonaId(id);
            HistorialMedicoDTO result = historialMedicoService.crearHistorialMedico(dto);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al crear historial médico: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/historial-medico")
    @RequiresPermission("ver_historial_medico")
    public ResponseEntity<?> obtenerHistorialMedico(@PathVariable Integer id,
                                                    @RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<HistorialMedicoDTO> result = historialMedicoService.obtenerHistorialMedicoPorPersona(id, pageable);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al obtener historial médico: " + e.getMessage());
        }
    }

}