package com.varcal.cheermanager.Controller.Persona;

import jakarta.servlet.http.HttpSession;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;
import com.varcal.cheermanager.DTO.UsuarioDTO;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.PersonaService;
import com.varcal.cheermanager.Service.Auth.AuthService;

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
    private PersonaRepository personaRepository;

     @PostMapping("/registrar")
    public ResponseEntity<?> registrarUsuario(@RequestBody UsuarioDTO usuarioDTO) {
        try {
            // Verificar que la persona asociada exista
            Persona persona = personaRepository.findById(usuarioDTO.getPersonaId())
                    .orElseThrow(() -> new RuntimeException("Persona no encontrada con el ID: " + usuarioDTO.getPersonaId()));

            // Registrar el usuario
            Usuario usuario = personaService.registrarUsuario(
                    persona,
                    usuarioDTO.getUsername(),
                    usuarioDTO.getEmail(),
                    usuarioDTO.getPassword(),
                    usuarioDTO.getRolId()
            );

            return ResponseEntity.ok(usuario); // Devolver el usuario registrado
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error al registrar el usuario: " + e.getMessage());
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

    @GetMapping("/permisos")
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
}
