package com.varcal.cheermanager.Service.Auth;

import com.varcal.cheermanager.tenant.TenantContext;

import com.varcal.cheermanager.Service.Clubes.RolesPlantilla;

import com.varcal.cheermanager.Models.Clubes.Club;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Ath.RolConConteoDTO;
import com.varcal.cheermanager.Models.Auth.Permiso;
import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.repository.Auth.PermisoRepository;
import com.varcal.cheermanager.repository.Auth.RolRepository;

@Service
public class RolService {
    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;

    public RolService(RolRepository rolRepository, PermisoRepository permisoRepository) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
    }

    public List<Rol> listar() {
        return rolRepository.findAll();
    }

    public List<RolConConteoDTO> listarRolesConConteo() {
        List<Object[]> resultados = rolRepository.listarRolesConConteo();

        return resultados.stream()
                .map(obj -> new RolConConteoDTO(
                        ((Number) obj[0]).intValue(),
                        String.valueOf(obj[1]),
                        ((Number) obj[2]).intValue()))
                .collect(Collectors.toList());
    }

    public Rol getRolById(Integer id) {
        return rolRepository.findById(id).orElse(null);
    }

    public Rol createRol(Rol rol) {
        // El body llega como entidad: se ignoran id y permisos (los permisos se asignan SOLO por
        // assignPermissionsToRol, que valida; asi no se cuela un permiso de plataforma por el body).
        rol.setId(null);
        rol.setPermisos(new HashSet<>());
        return rolRepository.save(rol);
    }

    public Rol modificarRol(Integer id, Rol rolDetails) {
        return rolRepository.findById(id)
                .map(rol -> {
                    rol.setNombre(rolDetails.getNombre());
                    // Antes solo se actualizaba el nombre — "Editar Rol" en
                    // el front no tenía forma de guardar cambios en la
                    // descripción aunque el formulario la mostrara.
                    rol.setDescripcion(rolDetails.getDescripcion());
                    return rolRepository.save(rol);
                })
                .orElse(null);
    }

    public void deleteRol(Integer id) {
        rolRepository.deleteById(id);
    }

    public Rol assignPermissionsToRol(Integer rolId, List<Integer> permisoIds) {
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        List<Permiso> permisos = permisoRepository.findAllById(permisoIds);

        // Escalada de privilegios: un admin de club NO puede darse permisos de plataforma
        // (gestionar_clubes, catalogos globales, rubrica global).
        if (TenantContext.get() != Club.PLATAFORMA_ID
                && permisos.stream().anyMatch(p -> RolesPlantilla.SOLO_PLATAFORMA.contains(p.getNombre()))) {
            throw new RuntimeException("Hay permisos reservados a la plataforma que no se pueden asignar a un rol de club");
        }

        rol.setPermisos(new HashSet<>(permisos));

        return rolRepository.save(rol);
    }
}