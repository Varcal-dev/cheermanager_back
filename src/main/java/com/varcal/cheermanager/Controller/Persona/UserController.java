package com.varcal.cheermanager.Controller.Persona;

import jakarta.servlet.http.HttpSession;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;
import com.varcal.cheermanager.DTO.Ath.RolConConteoDTO;
import com.varcal.cheermanager.DTO.Ath.UsuarioDTO;
import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Personas.Genero;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Auth.AuthService;
import com.varcal.cheermanager.Service.Auth.RolService;
import com.varcal.cheermanager.Service.Persona.GeneroService;
import com.varcal.cheermanager.Service.Persona.PersonaService;
import com.varcal.cheermanager.Utils.RequiresPermission;
import com.varcal.cheermanager.repository.Auth.RolRepository;

@RestController
@RequestMapping("/api/usuario")
public class UserController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PersonaService personaService;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private GeneroService generoService;

    @Autowired
    private RolService rolService;

    @PostMapping("/registrar")
    @RequiresPermission("crear_usuario")
    public ResponseEntity<?> registrarUsuario(@RequestBody UsuarioDTO usuarioDTO) {
        try {
            Persona persona = null;

            // Caso: persona existente
            if (usuarioDTO.getPersonaId() != null) {
                persona = personaRepository.findById(usuarioDTO.getPersonaId())
                        .orElseThrow(() -> new RuntimeException(
                                "Persona no encontrada con el ID: " + usuarioDTO.getPersonaId()));
            }

            // Caso: datos de persona nueva explícitamente enviados
            else if (usuarioDTO.getNombre() != null && usuarioDTO.getApellidos() != null) {
                persona = new Persona();
                persona.setNombre(usuarioDTO.getNombre());
                persona.setApellidos(usuarioDTO.getApellidos());
                persona.setDireccion(usuarioDTO.getDireccion());
                persona.setTelefono(usuarioDTO.getTelefono());
                persona.setFechaNacimiento(usuarioDTO.getFechaNacimiento());
                persona.setGeneroId(usuarioDTO.getGeneroId());
                persona = personaRepository.save(persona);
            }

            // Si no hay persona, el valor queda en null — se permite
            // Registrar el usuario
            Usuario usuario = personaService.registrarUsuario(
                    persona,
                    usuarioDTO.getUsername(),
                    usuarioDTO.getEmail(),
                    usuarioDTO.getPassword(),
                    usuarioDTO.getRolId());

            return ResponseEntity.ok(usuario); // Devolver el usuario registrado
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al registrar el usuario: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @RequiresPermission("ver_usuario")
    public ResponseEntity<?> obtenerUsuarioPorId(@PathVariable Integer id) {
        try {
            Usuario usuario = userRepository.findById(id.longValue())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con el ID: " + id));
            return ResponseEntity.ok(new UsuarioDTO(usuario)); // Mapear al DTO
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al obtener el usuario: " + e.getMessage());
        }
    }

    @GetMapping("/listar")
    @RequiresPermission("ver_usuarios")
    public ResponseEntity<?> listarUsuarios() {
        try {
            List<UsuarioDTO> usuarios = userRepository.findAll().stream()
                    .map(UsuarioDTO::new) // Mapear cada usuario al DTO
                    .collect(Collectors.toList());
            return ResponseEntity.ok(usuarios);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al listar los usuarios: " + e.getMessage());
        }
    }

    @PutMapping("/actualizar/{id}")
    @RequiresPermission("modificar_usuario")
    public ResponseEntity<?> actualizarUsuario(@PathVariable Integer id, @RequestBody UsuarioDTO usuarioDTO) {
        try {
            Usuario usuario = userRepository.findById(id.longValue())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado con el ID: " + id));

            // Actualizar los datos del usuario
            usuario.setUsername(usuarioDTO.getUsername());
            usuario.setEmail(usuarioDTO.getEmail());
            if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isEmpty()) {
                usuario.setPasswordHash(BCrypt.hashpw(usuarioDTO.getPassword(), BCrypt.gensalt())); // Hashear la nueva
                                                                                                    // contraseña
            }
            if (usuarioDTO.getRolId() != null) {
                Rol rol = rolRepository.findById(usuarioDTO.getRolId())
                        .orElseThrow(() -> new RuntimeException(
                                "El rol con ID '" + usuarioDTO.getRolId() + "' no existe en la base de datos"));
                usuario.setRol(rol);
            }

            Usuario usuarioActualizado = userRepository.save(usuario);
            return ResponseEntity.ok(new UsuarioDTO(usuarioActualizado)); // Devolver el usuario actualizado
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al actualizar el usuario: " + e.getMessage());
        }
    }

    @DeleteMapping("/eliminar/{id}")
    @RequiresPermission("eliminar_usuario")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Integer id) {
        try {
            if (!userRepository.existsById(id.longValue())) {
                return ResponseEntity.status(404).body("Usuario no encontrado con el ID: " + id);
            }
            userRepository.deleteById(id.longValue());
            return ResponseEntity.ok("Usuario eliminado exitosamente");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al eliminar el usuario: " + e.getMessage());
        }
    }

    // metodos para permisos ==========================

    @GetMapping("/permisos")
    //@RequiresPermission("ver_permisos")
    public ResponseEntity<?> getPermisos(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body("Usuario no autenticado");
        }

        try {
            Set<String> permisos = authService.getPermisosUsuario(userId);
            return ResponseEntity.ok(permisos);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.status(401).body("No hay usuario autenticado");
        }

        // Obtener el usuario desde la base de datos y mapearlo al DTO
        return userRepository.findById((long) userId)
                .map(user -> ResponseEntity.ok(new UsuarioDTO(user))) // Usar el DTO
                .orElseGet(() -> ResponseEntity.status(404).body(null));
    }

    @GetMapping("/genero/listar")
    public ResponseEntity<List<Genero>> listarGeneros() {
        return ResponseEntity.ok(generoService.listar());
    }

    @GetMapping("/rol/listar")
    //@RequiresPermission("ver_roles")
    public ResponseEntity<List<Rol>> listarRoles() {
        return ResponseEntity.ok(rolService.listar());
    }

    @GetMapping("/rol/listar-con-usuarios")
    public ResponseEntity<List<RolConConteoDTO>> listarConUsuarios() {
        return ResponseEntity.ok(rolService.listarRolesConConteo());
    }

}
