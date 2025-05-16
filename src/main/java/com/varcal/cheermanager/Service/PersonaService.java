package com.varcal.cheermanager.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Persona.DeportistaDTO;
import com.varcal.cheermanager.DTO.Persona.EntrenadorDTO;
import com.varcal.cheermanager.DTO.Persona.PersonaDTO;
import com.varcal.cheermanager.DTO.Persona.DeportistaVistaDTO;
import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Models.Personas.Entrenador;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.repository.Auth.RolRepository;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;
import com.varcal.cheermanager.repository.Personas.EntrenadorRepository;
import com.varcal.cheermanager.repository.Personas.EstadoPersonaRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.time.LocalDate;

@Service
public class PersonaService {

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PersonaRepository personaRepository;

    @Autowired
    private DeportistaRepository deportistaRepository;

    @Autowired
    private EntrenadorRepository entrenadorRepository;

    @Autowired
    private EstadoPersonaRepository estadoPersonaRepository;

    // Método para registrar un nuevo usuario ======================================
    public Usuario registrarUsuario(Persona persona, String username, String email, String password, Integer rolId) {
        // Verificar si el username o email ya existen
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("El nombre de usuario ya está en uso");
        }
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("El correo electrónico ya está en uso");
        }

        // Obtener el rol correspondiente
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new RuntimeException("El rol con ID '" + rolId + "' no existe en la base de datos"));

        // Crear el usuario
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt())); // Hashear la contraseña
        usuario.setEmail(email);
        usuario.setRol(rol);
        usuario.setActivo(true);
        usuario.setPersona(persona);
        usuario.setUltimoAcceso(null); // Inicialmente no hay acceso
        return userRepository.save(usuario);
    }

    // Método para registrar una nueva persona
    // ======================================
    // Este método recibe un DTO de Persona y lo convierte a una entidad Persona
    public Persona registrarPersona(PersonaDTO personaDTO) {
        // Mapear el DTO a la entidad Persona
        Persona persona = new Persona();
        persona.setNombre(personaDTO.getNombre());
        persona.setDireccion(personaDTO.getDireccion());
        persona.setTelefono(personaDTO.getTelefono());
        persona.setFechaNacimiento(personaDTO.getFechaNacimiento());
        persona.setGeneroId(personaDTO.getGeneroId());

        // Guardar la persona en la base de datos
        return personaRepository.save(persona);
    }

    public Persona modificarPersona(Integer id, PersonaDTO personaDTO) {
        return personaRepository.findById(id).map(persona -> {
            persona.setNombre(personaDTO.getNombre());
            persona.setDireccion(personaDTO.getDireccion());
            persona.setTelefono(personaDTO.getTelefono());
            persona.setFechaNacimiento(personaDTO.getFechaNacimiento());
            persona.setGeneroId(personaDTO.getGeneroId());
            return personaRepository.save(persona);
        }).orElseThrow(() -> new RuntimeException("Persona no encontrada con el ID: " + id));
    }

    public List<Persona> listarPersonas() {
        return personaRepository.findAll();
    }

    public List<Persona> buscarPersonasByNombre(String nombre) {
        return personaRepository.findByNombre(nombre); // Assuming you have this method in your repository
    }

    public void eliminarPersona(Integer id) {
        if (!personaRepository.existsById(id)) {
            throw new RuntimeException("Persona no encontrada con el ID: " + id);
        }
        personaRepository.deleteById(id);
    }

    // Método para registrar un deportista
    // ==========================================
    // Este método recibe un DTO de Deportista y lo convierte a una entidad
    // Deportista
    public Deportista registrarDeportista(DeportistaDTO deportistaDTO) {
        // Registrar la persona
        Persona persona = new Persona();
        persona.setNombre(deportistaDTO.getNombre());
        persona.setApellidos(deportistaDTO.getApellidos());
        persona.setDireccion(deportistaDTO.getDireccion());
        persona.setTelefono(deportistaDTO.getTelefono());
        persona.setFechaNacimiento(deportistaDTO.getFechaNacimiento());
        persona.setGeneroId(deportistaDTO.getGeneroId());
        Persona personaGuardada = personaRepository.save(persona);

        // Registrar el deportista
        Deportista deportista = new Deportista();
        deportista.setPersona(personaGuardada);
        deportista.setEstadoId(deportistaDTO.getEstadoId());
        deportista.setAltura(deportistaDTO.getAltura());
        deportista.setPeso(deportistaDTO.getPeso());
        deportista.setNivelActualId(deportistaDTO.getNivelActualId());
        deportista.setFechaRegistro(deportistaDTO.getFechaRegistro());
        deportista.setContactoEmergencia(deportistaDTO.getContactoEmergencia());
        deportista.setConvenioId(deportistaDTO.getConvenioId());
        Deportista deportistaGuardado = deportistaRepository.save(deportista);

        // Registrar al deportista como usuario
        String username = deportistaDTO.getNombre().toLowerCase() + "." + deportistaDTO.getApellidos().toLowerCase();
        String email = username + "@cheermanager.com"; // Generar un email ficticio o usar uno proporcionado
        String password = "0000"; // Generar una contraseña por defecto o usar una proporcionada
        Integer rolId = 2; // ID del rol para deportista
        registrarUsuario(personaGuardada, username, email, password, rolId);

        return deportistaGuardado;
    }

    // Método para modificar un deportista
    public Deportista modificarDeportista(Integer id, DeportistaDTO deportistaDTO) {
        return deportistaRepository.findById(id).map(deportista -> {
            Persona persona = deportista.getPersona();
            persona.setNombre(deportistaDTO.getNombre());
            persona.setDireccion(deportistaDTO.getDireccion());
            persona.setTelefono(deportistaDTO.getTelefono());
            persona.setFechaNacimiento(deportistaDTO.getFechaNacimiento());
            persona.setGeneroId(deportistaDTO.getGeneroId());
            personaRepository.save(persona);

            deportista.setEstadoId(deportistaDTO.getEstadoId());
            deportista.setNivelActualId(deportistaDTO.getNivelActualId());
            deportista.setFechaRegistro(deportistaDTO.getFechaRegistro());
            deportista.setContactoEmergencia(deportistaDTO.getContactoEmergencia());
            deportista.setConvenioId(deportistaDTO.getConvenioId());
            return deportistaRepository.save(deportista);
        }).orElseThrow(() -> new RuntimeException("Deportista no encontrado con el ID: " + id));
    }

    // Método para listar todos los deportistas
    public List<Deportista> listarDeportistas() {
        return deportistaRepository.findAll();
    }

    // Método para listar deportistas con detalles
    public List<DeportistaVistaDTO> listarDeportistasConDetalles() {
        List<Object[]> resultados = deportistaRepository.obtenerDeportistasConDetalles();

        // Mapear los resultados al DTO
        return resultados.stream().map(obj -> {
            DeportistaVistaDTO dto = new DeportistaVistaDTO();
            dto.setDeportistaId((Integer) obj[0]);
            dto.setPersonaId((Integer) obj[1]);
            dto.setNombre((String) obj[2]);
            dto.setApellidos((String) obj[3]);
            dto.setDireccion((String) obj[4]);
            dto.setTelefono((String) obj[5]);

            // Convertir java.sql.Date a java.time.LocalDate
            dto.setFechaNacimiento(obj[6] != null ? ((java.sql.Date) obj[6]).toLocalDate() : null);
            dto.setGeneroId((Integer) obj[7]);
            dto.setGenero((String) obj[8]);

            // Nuevos campos: altura y peso
            dto.setAltura(obj[9] != null ? ((Number) obj[9]).floatValue() : null);
            dto.setPeso(obj[10] != null ? ((Number) obj[10]).floatValue() : null);

            dto.setFechaRegistro(obj[11] != null ? ((java.sql.Date) obj[11]).toLocalDate() : null);
            dto.setContactoEmergencia((String) obj[12]);
            dto.setEstadoId((Integer) obj[13]);
            dto.setEstadoNombre((String) obj[14]);
            dto.setNivelActualId((Integer) obj[15]);
            dto.setNivelNombre((String) obj[16]);
            dto.setConvenioId((Integer) obj[17]);
            dto.setConvenioNombre((String) obj[18]);
            return dto;
        }).collect(Collectors.toList());
    }

    // Método para eliminar un deportista
    public void eliminarDeportista(Integer id) {
        if (!deportistaRepository.existsById(id)) {
            throw new RuntimeException("Deportista no encontrado con el ID: " + id);
        }
        deportistaRepository.deleteById(id);
    }

    // Método para registrar un entrenador
    // ==========================================
    // Este método recibe un DTO de Entrenador y lo convierte a una entidad
    // Entrenador
    public Entrenador registrarEntrenador(EntrenadorDTO entrenadorDTO) {
        // Registrar la persona
        Persona persona = new Persona();
        persona.setNombre(entrenadorDTO.getNombre());
        persona.setApellidos(entrenadorDTO.getApellidos());
        persona.setDireccion(entrenadorDTO.getDireccion());
        persona.setTelefono(entrenadorDTO.getTelefono());
        persona.setFechaNacimiento(entrenadorDTO.getFechaNacimiento());
        persona.setGeneroId(entrenadorDTO.getGeneroId());
        Persona personaGuardada = personaRepository.save(persona);

        // Registrar el entrenador
        Entrenador entrenador = new Entrenador();
        entrenador.setPersona(personaGuardada);
        entrenador.setFechaContratacion(entrenadorDTO.getFechaContratacion());
        entrenador.setFechaContratacion(entrenadorDTO.getFechaContratacion());
        // entrenador.setEstadoId(entrenadorDTO.getEstadoId());
        // Setear el estado usando la relación
        EstadoPersona estado = estadoPersonaRepository.findById(entrenadorDTO.getEstadoId())
                .orElseThrow(() -> new RuntimeException("Estado no encontrado"));
        entrenador.setEstado(estado);
        Entrenador entrenadorGuardado = entrenadorRepository.save(entrenador);

        // Registrar al entrenador como usuario
        String username = entrenadorDTO.getNombre().toLowerCase() + "." + entrenadorDTO.getApellidos().toLowerCase();
        String email = username + "@cheermanager.com"; // Generar un email ficticio o usar uno proporcionado
        String password = "0000"; // Generar una contraseña por defecto o usar una proporcionada
        Integer rolId = 3; // ID del rol para entrenador
        registrarUsuario(personaGuardada, username, email, password, rolId);

        return entrenadorGuardado;
    }

    // Método para modificar un entrenador
    public Entrenador modificarEntrenador(Integer id, EntrenadorDTO entrenadorDTO) {
        return entrenadorRepository.findById(id).map(entrenador -> {
            Persona persona = entrenador.getPersona();
            persona.setApellidos(entrenadorDTO.getApellidos());
            persona.setNombre(entrenadorDTO.getNombre());
            persona.setDireccion(entrenadorDTO.getDireccion());
            persona.setTelefono(entrenadorDTO.getTelefono());
            persona.setFechaNacimiento(entrenadorDTO.getFechaNacimiento());
            persona.setGeneroId(entrenadorDTO.getGeneroId());
            personaRepository.save(persona);

            entrenador.setFechaContratacion(entrenadorDTO.getFechaContratacion());
            // entrenador.setEstadoId(entrenadorDTO.getEstadoId());
            // Setear el estado usando la relación
            EstadoPersona estado = estadoPersonaRepository.findById(entrenadorDTO.getEstadoId())
                    .orElseThrow(() -> new RuntimeException("Estado no encontrado"));
            entrenador.setEstado(estado);
            return entrenadorRepository.save(entrenador);
        }).orElseThrow(() -> new RuntimeException("Entrenador no encontrado con el ID: " + id));
    }

    // Método para listar todos los entrenadores
    public List<Entrenador> listarEntrenadores() {
        return entrenadorRepository.findAll();
    }

    // Método para eliminar un entrenador
    public void eliminarEntrenador(Integer id) {
        if (!entrenadorRepository.existsById(id)) {
            throw new RuntimeException("Entrenador no encontrado con el ID: " + id);
        }
        entrenadorRepository.deleteById(id);
    }

}