package com.varcal.cheermanager.Service.Auth;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.RolConConteoDTO;
import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.repository.Auth.RolRepository;

@Service
public class RolService {
    private final RolRepository rolRepository;

    public RolService(RolRepository rolRepository) {
        this.rolRepository = rolRepository;
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
        return rolRepository.save(rol);
    }

    public Rol modificarRol(Integer id, Rol rolDetails) {
        return rolRepository.findById(id)
            .map(rol -> {
                rol.setNombre(rolDetails.getNombre());
                return rolRepository.save(rol);
            })
            .orElse(null);
    }

    public void deleteRol(Integer id) {
        rolRepository.deleteById(id);
    }
}