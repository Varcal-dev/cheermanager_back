package com.varcal.cheermanager.Service.Auth;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.security.AutorizacionCache;
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
        Usuario guardado = userRepository.save(usuario);
        AutorizacionCache.olvidarUsuario(guardado.getId());
        return guardado;
    }

    public Usuario actualizar(Integer id, Usuario usuarioDetails) {
        Usuario usuario = userRepository.findById((long) id).orElse(null);
        if (usuario == null) {
            return null;
        }
        usuario.setUsername(usuarioDetails.getUsername());
        usuario.setEmail(usuarioDetails.getEmail());
        usuario.setActivo(usuarioDetails.getActivo());
        Usuario guardado = userRepository.save(usuario);
        AutorizacionCache.olvidarUsuario(id);
        return guardado;
    }

    public void eliminar(Integer id) {
        userRepository.deleteById((long) id);
        AutorizacionCache.olvidarUsuario(id);
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
        Usuario guardado = userRepository.save(usuario);
        AutorizacionCache.olvidarUsuario(usuarioId);
        return guardado;
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
        Usuario guardado = userRepository.save(usuario);
        AutorizacionCache.olvidarUsuario(usuarioId);
        return guardado;
    }

    /**
     * Obtener todos los permisos del usuario (desde rol único o múltiples roles)
     */
    public Set<String> obtenerPermisos(Integer usuarioId) {
        return permisosSiExiste(usuarioId).orElse(Set.of());
    }

    /**
     * Permisos del usuario, o vacio si NO existe o esta inactivo. Sin cache: es siempre lo que hay en la base de datos
     * (login, perfil...). Para decidir el acceso en cada peticion se usa {@link #permisosVigentes(int)}.
     */
    public java.util.Optional<Set<String>> permisosSiExiste(Integer usuarioId) {
        Usuario usuario = userRepository.findById((long) usuarioId).orElse(null);
        if (usuario == null) {
            return java.util.Optional.empty();
        }

        Set<String> permisos = new java.util.HashSet<>();

        // Obtener permisos del rol único (si existe)
        if (usuario.getRol() != null && usuario.getRol().getPermisos() != null) {
            permisos.addAll(usuario.getRol().getPermisos().stream()
                    .map(p -> p.getNombre())
                    .collect(Collectors.toSet()));
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

        return java.util.Optional.of(permisos);
    }

    /**
     * Para decidir el acceso en CADA peticion (PERF-02): igual que {@link #permisosSiExiste} pero con un cache de
     * {@link AutorizacionCache#TTL_MS} ms por club y usuario. Vacio = el usuario no existe o esta inactivo. Sin club en
     * contexto no se usa el cache.
     */
    public java.util.Optional<Set<String>> permisosVigentes(int usuarioId) {
        if (!com.varcal.cheermanager.tenant.TenantContext.isSet()) {
            return permisosSiExiste(usuarioId);
        }
        return AutorizacionCache.permisos(com.varcal.cheermanager.tenant.TenantContext.get(), usuarioId,
                () -> permisosSiExiste(usuarioId));
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