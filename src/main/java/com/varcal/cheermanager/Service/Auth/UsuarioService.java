package com.varcal.cheermanager.Service.Auth;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.repository.Auth.RolRepository;
import com.varcal.cheermanager.repository.Auth.UserRepository;

@Service
public class UsuarioService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolRepository rolRepository;

    public Usuario obtenerPorId(Integer id) {
        return userRepository.findById((long) id).orElse(null);
    }

    public Usuario obtenerPorEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    public List<Usuario> listarTodos() {
        return userRepository.findAll();
    }

    public Usuario crear(Usuario usuario) {
        return userRepository.save(usuario);
    }

    public Usuario actualizar(Integer id, Usuario usuarioDetails) {
        Usuario usuario = userRepository.findById((long) id).orElse(null);
        if (usuario == null) {
            return null;
        }
        usuario.setUsername(usuarioDetails.getUsername());
        usuario.setEmail(usuarioDetails.getEmail());
        usuario.setActivo(usuarioDetails.getActivo());
        return userRepository.save(usuario);
    }

    public void eliminar(Integer id) {
        userRepository.deleteById((long) id);
    }

    /**
     * Asignar un rol único al usuario (compatibilidad con rol_id)
     */
    public Usuario asignarRol(Integer usuarioId, Integer rolId) {
        Usuario usuario = userRepository.findById((long) usuarioId).orElse(null);
        if (usuario == null) {
            return null;
        }

        Rol rol = rolRepository.findById(rolId).orElse(null);
        if (rol == null) {
            return null;
        }

        usuario.setRol(rol);
        return userRepository.save(usuario);
    }

    /**
     * Asignar múltiples roles al usuario
     */
    public Usuario asignarRoles(Integer usuarioId, Set<Integer> rolIds) {
        Usuario usuario = userRepository.findById((long) usuarioId).orElse(null);
        if (usuario == null) {
            return null;
        }

        Set<Rol> roles = rolIds.stream()
                .map(id -> rolRepository.findById(id).orElse(null))
                .filter(rol -> rol != null)
                .collect(Collectors.toSet());

        usuario.setRoles(roles);
        return userRepository.save(usuario);
    }

    /**
     * Obtener todos los permisos del usuario (desde rol único o múltiples roles)
     */
    public Set<String> obtenerPermisos(Integer usuarioId) {
        Usuario usuario = userRepository.findById((long) usuarioId).orElse(null);
        if (usuario == null) {
            return Set.of();
        }

        Set<String> permisos = Set.of();

        // Obtener permisos del rol único (si existe)
        if (usuario.getRol() != null && usuario.getRol().getPermisos() != null) {
            permisos = usuario.getRol().getPermisos().stream()
                    .map(p -> p.getNombre())
                    .collect(Collectors.toSet());
        }

        // Agregar permisos de múltiples roles
        if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
            usuario.getRoles().forEach(rol -> {
                if (rol.getPermisos() != null) {
                    rol.getPermisos().forEach(permiso -> {
                        permisos.add(permiso.getNombre());
                    });
                }
            });
        }

        return permisos;
    }

    /**
     * Obtener todos los roles del usuario
     */
    public Set<Rol> obtenerRoles(Integer usuarioId) {
        Usuario usuario = userRepository.findById((long) usuarioId).orElse(null);
        if (usuario == null) {
            return Set.of();
        }

        Set<Rol> roles = Set.of();

        // Agregar rol único si existe
        if (usuario.getRol() != null) {
            roles = Set.of(usuario.getRol());
        }

        // Agregar múltiples roles
        if (usuario.getRoles() != null && !usuario.getRoles().isEmpty()) {
            roles = usuario.getRoles();
        }

        return roles;
    }
}
