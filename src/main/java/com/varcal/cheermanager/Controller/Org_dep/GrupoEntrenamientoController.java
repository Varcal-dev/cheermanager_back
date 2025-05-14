package com.varcal.cheermanager.Controller.Org_dep;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.DTO.Org_dep.GrupoConDeportistasDTO;
import com.varcal.cheermanager.DTO.Org_dep.GrupoEntrenamientoDTO;
import com.varcal.cheermanager.DTO.Persona.DeportistaDTO;
import com.varcal.cheermanager.Models.Org_dep.CategoriaNivel;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Org_dep.TipoGrupo;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Org_dep.CategoriaNivelRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Org_dep.TipoGrupoRepository;

@RestController
@RequestMapping("/api/grupos")
public class GrupoEntrenamientoController {

    @Autowired
    private GrupoEntrenamientoRepository grupoRepo;
    @Autowired
    private TipoGrupoRepository tipoGrupoRepo;
    @Autowired
    private CategoriaNivelRepository categoriaNivelRepo;

    @GetMapping
    public List<GrupoEntrenamiento> listar() {
        return grupoRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrupoEntrenamiento> obtener(@PathVariable Integer id) {
        return grupoRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> crear(@RequestBody GrupoEntrenamientoDTO dto) {
        Optional<TipoGrupo> tipo = tipoGrupoRepo.findById(dto.getTipoGrupoId());
        Optional<CategoriaNivel> categoria = categoriaNivelRepo.findById(dto.getCategoriaNivelId());

        if (tipo.isEmpty() || categoria.isEmpty()) {
            return ResponseEntity.badRequest().body("Tipo de grupo o categoría nivel no encontrado.");
        }

        GrupoEntrenamiento grupo = new GrupoEntrenamiento();
        grupo.setNombre(dto.getNombre());
        grupo.setTipoGrupo(tipo.get());
        grupo.setCategoriaNivel(categoria.get());

        return ResponseEntity.ok(grupoRepo.save(grupo));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody GrupoEntrenamientoDTO dto) {
        Optional<GrupoEntrenamiento> grupoOpt = grupoRepo.findById(id);
        Optional<TipoGrupo> tipo = tipoGrupoRepo.findById(dto.getTipoGrupoId());
        Optional<CategoriaNivel> categoria = categoriaNivelRepo.findById(dto.getCategoriaNivelId());

        if (grupoOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (tipo.isEmpty() || categoria.isEmpty()) {
            return ResponseEntity.badRequest().body("Tipo de grupo o categoría nivel no encontrado.");
        }

        GrupoEntrenamiento grupo = grupoOpt.get(); // Ya tiene ID
        grupo.setNombre(dto.getNombre());
        grupo.setTipoGrupo(tipo.get());
        grupo.setCategoriaNivel(categoria.get());

        return ResponseEntity.ok(grupoRepo.save(grupo)); // Actualiza, no crea nuevo
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        if (!grupoRepo.existsById(id))
            return ResponseEntity.notFound().build();
        grupoRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/con-deportistas")
    public List<GrupoConDeportistasDTO> getGruposConDeportistas() {
        List<GrupoEntrenamiento> grupos = grupoRepo.findAll();

        return grupos.stream().map(grupo -> {
            GrupoConDeportistasDTO dto = new GrupoConDeportistasDTO();
            dto.setId(grupo.getId());
            dto.setNombre(grupo.getNombre());
            dto.setTipoGrupo(grupo.getTipoGrupo());
            dto.setCategoriaNivel(grupo.getCategoriaNivel());

            List<DeportistaDTO> deportistas = grupo.getDeportistas().stream()
                    .map(rel -> {
                        Deportista d = rel.getDeportista();
                        DeportistaDTO deportistaDTO = new DeportistaDTO();
                        deportistaDTO.setId(d.getId());

                        // Si tienes la persona relacionada, puedes obtener su nombre, de lo contrario
                        // puedes dejarlo como null o alguna cadena vacía.
                        deportistaDTO.setNombre(d.getPersona() != null ? d.getPersona().getNombre() : "Desconocido");
                        deportistaDTO.setApellidos(d.getPersona() != null ? d.getPersona().getApellidos() : "Desconocido");
                        deportistaDTO.setDireccion(d.getPersona() != null ? d.getPersona().getDireccion() : "Desconocido");
                        deportistaDTO.setTelefono(d.getPersona() != null ? d.getPersona().getTelefono() : "Desconocido");
                        deportistaDTO.setFechaNacimiento(d.getPersona() != null ? d.getPersona().getFechaNacimiento() : LocalDate.now());
                        
                        
                        deportistaDTO.setNivelActualId(d.getNivelActualId());
                        return deportistaDTO;
                    })
                    .collect(Collectors.toList()); // Corrección aquí, cambiamos 'toList()' por
                                                   // 'collect(Collectors.toList())'

            dto.setDeportistas(deportistas);
            return dto;
        }).collect(Collectors.toList()); // También aseguramos que toda la lista sea procesada correctamente
    }

}
