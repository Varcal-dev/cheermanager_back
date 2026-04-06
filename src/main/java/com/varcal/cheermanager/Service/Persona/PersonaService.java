package com.varcal.cheermanager.Service.Persona;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.varcal.cheermanager.DTO.Persona.DeportistaDTO;
import com.varcal.cheermanager.DTO.Persona.EntrenadorDTO;
import com.varcal.cheermanager.DTO.Persona.PersonaDTO;
import com.varcal.cheermanager.DTO.Persona.DeportistaVistaDTO;
import com.varcal.cheermanager.DTO.Persona.DeportistaPerfilCompletoDTO;
import com.varcal.cheermanager.DTO.Persona.VincularDeportistaUsuarioDTO;
import com.varcal.cheermanager.DTO.Persona.ValidarDocumentoDTO;
import com.varcal.cheermanager.DTO.Persona.HistorialDeportistaEstadoDTO;
import com.varcal.cheermanager.DTO.Persona.UploadFotoDTO;
import com.varcal.cheermanager.Models.Auth.Rol;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Models.Personas.HistorialDeportistaEstado;
import com.varcal.cheermanager.Utils.UsernameUtils;
import com.varcal.cheermanager.Models.Personas.Entrenador;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.repository.Auth.RolRepository;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;
import com.varcal.cheermanager.repository.Personas.EntrenadorRepository;
import com.varcal.cheermanager.repository.Personas.EstadoPersonaRepository;
import com.varcal.cheermanager.repository.Personas.HistorialDeportistaEstadoRepository;
import com.varcal.cheermanager.Service.File.FileStorageService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

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

    @Autowired
    private HistorialDeportistaEstadoRepository historialDeportistaEstadoRepository;

    @Autowired
    private FileStorageService fileStorageService;

    public String generarUsername(String nombre, String apellidos) {

        String primerNombre = UsernameUtils.primerToken(nombre);
        String primerApellido = UsernameUtils.primerToken(apellidos);

        String base = UsernameUtils.normalizar(primerNombre)
                + "." + UsernameUtils.normalizar(primerApellido);

        long count = userRepository.countByUsernameStartingWith(base);
        String username = base + "_" + (count + 1);
        return username;
    }

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
        // Validar documento único
        validarDocumentoEnRegistro(deportistaDTO.getNumeroDocumento());

        // Registrar la persona
        Persona persona = new Persona();
        persona.setNombre(deportistaDTO.getNombre());
        persona.setApellidos(deportistaDTO.getApellidos());
        persona.setNumeroDocumento(deportistaDTO.getNumeroDocumento());
        persona.setTipoDocumento(deportistaDTO.getTipoDocumento());
        persona.setDireccion(deportistaDTO.getDireccion());
        persona.setTelefono(deportistaDTO.getTelefono());
        persona.setFechaNacimiento(deportistaDTO.getFechaNacimiento());
        persona.setGeneroId(deportistaDTO.getGeneroId());
        Persona personaGuardada = personaRepository.save(persona);

        // Registrar el deportista
        Deportista deportista = new Deportista();
        deportista.setPersona(personaGuardada);
        EstadoPersona estado = estadoPersonaRepository.findById(deportistaDTO.getEstadoId())
                .orElseThrow(() -> new RuntimeException("Estado no encontrado"));
        deportista.setEstado(estado);
        deportista.setAltura(deportistaDTO.getAltura());
        deportista.setPeso(deportistaDTO.getPeso());
        deportista.setNivelActualId(deportistaDTO.getNivelActualId());
        deportista.setFechaRegistro(LocalDate.now());
        deportista.setContactoEmergencia(deportistaDTO.getContactoEmergencia());
        deportista.setConvenioId(deportistaDTO.getConvenioId());
        Deportista deportistaGuardado = deportistaRepository.save(deportista);

        // Registrar al deportista como usuario
        String username = generarUsername(
                deportistaDTO.getNombre(),
                deportistaDTO.getApellidos());
        String email = username + "@cheermanager.com"; // Generar un email ficticio o usar uno proporcionado
        String password = "0000"; // Generar una contraseña por defecto o usar una proporcionada
        // Integer rolId = 9; // ID del rol para deportista
        registrarUsuario(personaGuardada, username, email, password, 9);

        return deportistaGuardado;
    }

    // Método para modificar un deportista
    public Deportista modificarDeportista(Integer id, DeportistaDTO deportistaDTO) {
        return deportistaRepository.findById(id).map(deportista -> {
            Persona persona = deportista.getPersona();

            // Validar documento único si está siendo modificado
            if (deportistaDTO.getNumeroDocumento() != null && !deportistaDTO.getNumeroDocumento().isEmpty()) {
                validarDocumentoEnModificacion(persona.getId(), deportistaDTO.getNumeroDocumento());
            }

            persona.setNombre(deportistaDTO.getNombre());
            persona.setApellidos(deportistaDTO.getApellidos());
            persona.setNumeroDocumento(deportistaDTO.getNumeroDocumento());
            persona.setTipoDocumento(deportistaDTO.getTipoDocumento());
            persona.setDireccion(deportistaDTO.getDireccion());
            persona.setTelefono(deportistaDTO.getTelefono());
            persona.setFechaNacimiento(deportistaDTO.getFechaNacimiento());
            persona.setGeneroId(deportistaDTO.getGeneroId());
            personaRepository.save(persona);

            // Registrar cambio de estado si es que hay
            EstadoPersona estadoAnterior = deportista.getEstado();
            EstadoPersona estado = estadoPersonaRepository.findById(deportistaDTO.getEstadoId())
                    .orElseThrow(() -> new RuntimeException("Estado no encontrado"));

            if (!estado.getId().equals(estadoAnterior.getId())) {
                registrarCambioEstado(deportista, estadoAnterior, estado, null, null);
            }

            deportista.setEstado(estado);
            deportista.setAltura(deportistaDTO.getAltura());
            deportista.setPeso(deportistaDTO.getPeso());
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

    // Método para listar deportistas con filtros (estado, nivel, grupo)
    public List<DeportistaVistaDTO> listarDeportistasConFiltros(Integer estadoId, Integer nivelId, Integer grupoId) {
        List<Deportista> deportistas = deportistaRepository.findByFiltros(estadoId, nivelId, grupoId);

        if (deportistas.isEmpty()) {
            return List.of();
        }

        // Mapear entidades a DTOs
        return deportistas.stream().map(deportista -> {
            DeportistaVistaDTO dto = new DeportistaVistaDTO();
            Persona persona = deportista.getPersona();

            dto.setDeportistaId(deportista.getId());
            dto.setPersonaId(persona.getId());
            dto.setNombre(persona.getNombre());
            dto.setApellidos(persona.getApellidos());
            dto.setDireccion(persona.getDireccion());
            dto.setTelefono(persona.getTelefono());
            dto.setFechaNacimiento(persona.getFechaNacimiento());
            dto.setGeneroId(persona.getGeneroId());
            dto.setAltura(deportista.getAltura() != null ? deportista.getAltura().floatValue() : null);
            dto.setPeso(deportista.getPeso() != null ? deportista.getPeso().floatValue() : null);
            dto.setFechaRegistro(deportista.getFechaRegistro());
            dto.setContactoEmergencia(deportista.getContactoEmergencia());

            if (deportista.getEstado() != null) {
                dto.setEstadoId(deportista.getEstado().getId());
                dto.setEstadoNombre(deportista.getEstado().getEstado());
            }

            dto.setNivelActualId(deportista.getNivelActualId());
            dto.setConvenioId(deportista.getConvenioId());

            return dto;
        }).collect(Collectors.toList());
    }

    // Método para obtener el perfil completo del deportista
    public DeportistaPerfilCompletoDTO obtenerPerfilCompleto(Integer deportistaId) {
        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + deportistaId));

        DeportistaPerfilCompletoDTO perfil = new DeportistaPerfilCompletoDTO();
        Persona persona = deportista.getPersona();

        // Datos personales
        perfil.setDeportistaId(deportista.getId());
        perfil.setPersonaId(persona.getId());
        perfil.setNombre(persona.getNombre());
        perfil.setApellidos(persona.getApellidos());
        perfil.setDireccion(persona.getDireccion());
        perfil.setTelefono(persona.getTelefono());
        perfil.setFechaNacimiento(persona.getFechaNacimiento());
        perfil.setGeneroId(persona.getGeneroId());

        // Datos deportivos
        perfil.setAltura(deportista.getAltura() != null ? deportista.getAltura().floatValue() : null);
        perfil.setPeso(deportista.getPeso() != null ? deportista.getPeso().floatValue() : null);
        perfil.setFechaRegistro(deportista.getFechaRegistro());
        perfil.setContactoEmergencia(deportista.getContactoEmergencia());

        if (deportista.getEstado() != null) {
            perfil.setEstadoId(deportista.getEstado().getId());
            perfil.setEstadoNombre(deportista.getEstado().getEstado());
        }

        perfil.setNivelActualId(deportista.getNivelActualId());
        perfil.setConvenioId(deportista.getConvenioId());

        // Información del usuario vinculado
        Usuario usuario = userRepository.findByPersona(persona).orElse(null);
        if (usuario != null) {
            perfil.setUsuarioId(usuario.getId());
            perfil.setUsername(usuario.getUsername());
            perfil.setEmail(usuario.getEmail());
            perfil.setUsuarioActivo(usuario.getActivo());
            perfil.setUltimoAcceso(usuario.getUltimoAcceso());

            // Roles del usuario
            List<String> roleNames = usuario.getRoles() != null ? usuario.getRoles().stream()
                    .map(Rol::getNombre)
                    .collect(Collectors.toList()) : List.of();
            perfil.setRoles(roleNames);

            // Permisos del usuario
            List<String> permisos = usuario.getRoles() != null ? usuario.getRoles().stream()
                    .flatMap(rol -> rol.getPermisos() != null ? rol.getPermisos().stream() : java.util.stream.Stream.empty())
                    .map(p -> p.getNombre())
                    .distinct()
                    .collect(Collectors.toList()) : List.of();
            perfil.setPermisos(permisos);
        }

        return perfil;
    }

    // Método para vincular un deportista con un usuario existente
    public void vincularDeportistaConUsuario(Integer deportistaId, Integer usuarioId) {
        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + deportistaId));

        Usuario usuario = userRepository.findById(usuarioId.longValue())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + usuarioId));

        // Validar que el usuario no esté ya vinculado a otra persona
        if (usuario.getPersona() != null) {
            throw new RuntimeException("El usuario " + usuario.getUsername() + " ya está vinculado a otra persona");
        }

        // Validar que el deportista no esté ya vinculado a otro usuario
        Usuario usuarioExistente = userRepository.findByPersona(deportista.getPersona()).orElse(null);
        if (usuarioExistente != null && !usuarioExistente.getId().equals(usuarioId)) {
            throw new RuntimeException("El deportista ya está vinculado al usuario " + usuarioExistente.getUsername());
        }

        // Vincular
        usuario.setPersona(deportista.getPersona());
        userRepository.save(usuario);
    }

    // Método para desvinculcar un deportista de su usuario
    public void desvinculcarDeportistaDeUsuario(Integer deportistaId) {
        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + deportistaId));

        Usuario usuario = userRepository.findByPersona(deportista.getPersona())
                .orElseThrow(() -> new RuntimeException("El deportista no tiene un usuario vinculado"));

        // Desvinculcar
        usuario.setPersona(null);
        userRepository.save(usuario);
    }

    // Método para obtener el usuario vinculado a un deportista
    public Usuario obtenerUsuarioDeDeportista(Integer deportistaId) {
        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + deportistaId));

        return userRepository.findByPersona(deportista.getPersona())
                .orElseThrow(() -> new RuntimeException("El deportista no tiene un usuario vinculado"));
    }

    // Método para validar si un documento de identidad está disponible (único)
    public ValidarDocumentoDTO validarDocumentoUnico(String numeroDocumento) {
        if (numeroDocumento == null || numeroDocumento.trim().isEmpty()) {
            return new ValidarDocumentoDTO(false, "El número de documento no puede estar vacío");
        }

        java.util.Optional<Persona> persona = personaRepository.findByNumeroDocumento(numeroDocumento);
        if (persona.isPresent()) {
            return new ValidarDocumentoDTO(false, "El documento " + numeroDocumento + " ya está registrado");
        }

        return new ValidarDocumentoDTO(true, "El documento " + numeroDocumento + " está disponible");
    }

    // Validación en el registro de deportista
    private void validarDocumentoEnRegistro(String numeroDocumento) {
        if (numeroDocumento != null && !numeroDocumento.trim().isEmpty()) {
            java.util.Optional<Persona> persona = personaRepository.findByNumeroDocumento(numeroDocumento);
            if (persona.isPresent()) {
                throw new RuntimeException("El documento " + numeroDocumento + " ya está registrado en el sistema");
            }
        }
    }

    // Validación en la modificación de deportista
    private void validarDocumentoEnModificacion(Integer personaId, String numeroDocumento) {
        if (numeroDocumento != null && !numeroDocumento.trim().isEmpty()) {
            java.util.Optional<Persona> persona = personaRepository.findByNumeroDocumento(numeroDocumento);
            if (persona.isPresent() && !persona.get().getId().equals(personaId)) {
                throw new RuntimeException("El documento " + numeroDocumento + " ya está registrado en otro deportista");
            }
        }
    }

    // Método para registrar cambios de estado en el historial
    private void registrarCambioEstado(Deportista deportista, EstadoPersona estadoAnterior,
                                       EstadoPersona estadoNuevo, Integer usuarioId, String motivo) {
        // No registrar si el estado no cambió
        if (estadoAnterior != null && estadoAnterior.getId().equals(estadoNuevo.getId())) {
            return;
        }

        HistorialDeportistaEstado historial = new HistorialDeportistaEstado();
        historial.setDeportista(deportista);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setFechaCambio(LocalDateTime.now());
        historial.setUsuarioId(usuarioId);
        historial.setMotivoCambio(motivo);
        historialDeportistaEstadoRepository.save(historial);
    }

    // Método para obtener el historial de cambios de estado de un deportista
    public List<HistorialDeportistaEstadoDTO> obtenerHistorialEstadoDeportista(Integer deportistaId) {
        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + deportistaId));

        List<HistorialDeportistaEstado> historial = historialDeportistaEstadoRepository
                .findByDeportistaIdOrderByFechaCambioDesc(deportistaId);

        return historial.stream().map(h -> {
            HistorialDeportistaEstadoDTO dto = new HistorialDeportistaEstadoDTO();
            dto.setId(h.getId());
            dto.setDeportistaId(h.getDeportista().getId());
            dto.setNombreDeportista(h.getDeportista().getPersona().getNombre());
            dto.setApellidosDeportista(h.getDeportista().getPersona().getApellidos());

            if (h.getEstadoAnterior() != null) {
                dto.setEstadoAnteriorId(h.getEstadoAnterior().getId());
                dto.setEstadoAnteriorNombre(h.getEstadoAnterior().getEstado());
            }

            dto.setEstadoNuevoId(h.getEstadoNuevo().getId());
            dto.setEstadoNuevoNombre(h.getEstadoNuevo().getEstado());
            dto.setFechaCambio(h.getFechaCambio());
            dto.setUsuarioId(h.getUsuarioId());

            if (h.getUsuarioId() != null) {
                java.util.Optional<Usuario> usuario = userRepository.findById(h.getUsuarioId().longValue());
                if (usuario.isPresent()) {
                    dto.setUsuarioUsername(usuario.get().getUsername());
                }
            }

            dto.setMotivoCambio(h.getMotivoCambio());
            return dto;
        }).collect(Collectors.toList());
    }

    // Método para obtener todo el historial de cambios de estado
    public List<HistorialDeportistaEstadoDTO> obtenerHistorialEstadoGlobal() {
        List<HistorialDeportistaEstado> historial = historialDeportistaEstadoRepository.findAllByOrderByFechaCambioDesc();

        return historial.stream().map(h -> {
            HistorialDeportistaEstadoDTO dto = new HistorialDeportistaEstadoDTO();
            dto.setId(h.getId());
            dto.setDeportistaId(h.getDeportista().getId());
            dto.setNombreDeportista(h.getDeportista().getPersona().getNombre());
            dto.setApellidosDeportista(h.getDeportista().getPersona().getApellidos());

            if (h.getEstadoAnterior() != null) {
                dto.setEstadoAnteriorId(h.getEstadoAnterior().getId());
                dto.setEstadoAnteriorNombre(h.getEstadoAnterior().getEstado());
            }

            dto.setEstadoNuevoId(h.getEstadoNuevo().getId());
            dto.setEstadoNuevoNombre(h.getEstadoNuevo().getEstado());
            dto.setFechaCambio(h.getFechaCambio());
            dto.setUsuarioId(h.getUsuarioId());

            if (h.getUsuarioId() != null) {
                java.util.Optional<Usuario> usuario = userRepository.findById(h.getUsuarioId().longValue());
                if (usuario.isPresent()) {
                    dto.setUsuarioUsername(usuario.get().getUsername());
                }
            }

            dto.setMotivoCambio(h.getMotivoCambio());
            return dto;
        }).collect(Collectors.toList());
    }

    // Método para guardar foto de perfil del deportista
    public UploadFotoDTO guardarFotoDeportista(Integer deportistaId, MultipartFile archivo) {
        try {
            Deportista deportista = deportistaRepository.findById(deportistaId)
                    .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + deportistaId));

            // Eliminar foto anterior si existe
            if (deportista.getFotoPerfil() != null && !deportista.getFotoPerfil().isEmpty()) {
                try {
                    fileStorageService.eliminarFoto(deportista.getFotoPerfil());
                } catch (Exception e) {
                    // Log del error pero continuar
                }
            }

            // Guardar nueva foto
            String rutaFoto = fileStorageService.guardarFoto(archivo, "deportistas");

            // Actualizar deportista con la ruta de la foto
            deportista.setFotoPerfil(rutaFoto);
            deportistaRepository.save(deportista);

            return new UploadFotoDTO(
                    true,
                    "Foto guardada exitosamente",
                    archivo.getOriginalFilename(),
                    "/api/deportistas/" + deportistaId + "/foto"
            );
        } catch (Exception e) {
            return new UploadFotoDTO(
                    false,
                    "Error al guardar la foto: " + e.getMessage(),
                    null,
                    null
            );
        }
    }

    // Método para obtener la foto del deportista
    public byte[] obtenerFotoDeportista(Integer deportistaId) throws Exception {
        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + deportistaId));

        if (deportista.getFotoPerfil() == null || deportista.getFotoPerfil().isEmpty()) {
            throw new RuntimeException("El deportista no tiene foto de perfil");
        }

        return fileStorageService.descargarFoto(deportista.getFotoPerfil());
    }

    // Método para eliminar foto del deportista
    public void eliminarFotoDeportista(Integer deportistaId) throws Exception {
        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + deportistaId));

        if (deportista.getFotoPerfil() != null && !deportista.getFotoPerfil().isEmpty()) {
            fileStorageService.eliminarFoto(deportista.getFotoPerfil());
            deportista.setFotoPerfil(null);
            deportistaRepository.save(deportista);
        }
    }

    // Método para eliminar un deportista
    public void eliminarDeportista(Integer deportistaId) {

        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado"));

        // Cambiar estado: obtener la entidad EstadoPersona con ID 2 (Retirado) y setearla
        EstadoPersona estadoRetirado = estadoPersonaRepository.findById(2)
                .orElseThrow(() -> new RuntimeException("Estado 'Retirado' no encontrado"));

        // Registrar el cambio de estado en el historial
        registrarCambioEstado(deportista, deportista.getEstado(), estadoRetirado, null, "Deportista eliminado del sistema");

        deportista.setEstado(estadoRetirado);
        deportistaRepository.save(deportista);

        // Desactivar usuario
        Usuario usuario = userRepository.findByPersona(deportista.getPersona())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setActivo(false);
        userRepository.save(usuario);
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
        entrenador.setEspecializacion(entrenadorDTO.getEspecializacion());
        // entrenador.setEstadoId(entrenadorDTO.getEstadoId());
        // Setear el estado usando la relación
        EstadoPersona estado = estadoPersonaRepository.findById(entrenadorDTO.getEstadoId())
                .orElseThrow(() -> new RuntimeException("Estado no encontrado"));
        entrenador.setEstado(estado);
        Entrenador entrenadorGuardado = entrenadorRepository.save(entrenador);

        // Registrar al entrenador como usuario
        String username = generarUsername(
                entrenadorDTO.getNombre(),
                entrenadorDTO.getApellidos());
        String email = username + "@cheermanager.com"; // Generar un email ficticio o usar uno proporcionado
        String password = "0000"; // Generar una contraseña por defecto o usar una proporcionada
        Integer rolId = entrenadorDTO.getRolIdE(); // ID del rol para entrenador
        registrarUsuario(personaGuardada, username, email, password, rolId);

        return entrenadorGuardado;
    }

    // Método para modificar un entrenador
    public Entrenador modificarEntrenador(Integer id, EntrenadorDTO dto) {

        return entrenadorRepository.findById(id).map(entrenador -> {

            Persona persona = entrenador.getPersona();

            if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
                persona.setNombre(dto.getNombre());
            }

            if (dto.getApellidos() != null && !dto.getApellidos().isBlank()) {
                persona.setApellidos(dto.getApellidos());
            }

            if (dto.getDireccion() != null) {
                persona.setDireccion(dto.getDireccion());
            }

            if (dto.getTelefono() != null) {
                persona.setTelefono(dto.getTelefono());
            }

            if (dto.getFechaNacimiento() != null) {
                persona.setFechaNacimiento(dto.getFechaNacimiento());
            }

            if (dto.getGeneroId() != null) {
                persona.setGeneroId(dto.getGeneroId());
            }

            personaRepository.save(persona);

            if (dto.getFechaContratacion() != null) {
                entrenador.setFechaContratacion(dto.getFechaContratacion());
            }

            if (dto.getEstadoId() != null) {
                EstadoPersona estado = estadoPersonaRepository.findById(dto.getEstadoId())
                        .orElseThrow(() -> new RuntimeException("Estado no encontrado"));
                entrenador.setEstado(estado);
            }

            return entrenadorRepository.save(entrenador);
        }).orElseThrow(() -> new RuntimeException("Entrenador no encontrado"));
    }

    // Método para listar todos los entrenadores
    public List<Entrenador> listarEntrenadores() {
        return entrenadorRepository.findAll();
    }

    // Método para eliminar un entrenador
    public void eliminarEntrenador(Integer id) {
        Entrenador entrenador = entrenadorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Entrenador no encontrado"));

        // Cambiar estado: obtener la entidad EstadoPersona con ID 2 (Retirado) y
        // setearla
        EstadoPersona estadoRetirado = estadoPersonaRepository.findById(2)
                .orElseThrow(() -> new RuntimeException("Estado 'Retirado' no encontrado"));
        entrenador.setEstado(estadoRetirado);
        entrenadorRepository.save(entrenador);

        // Desactivar usuario
        Usuario usuario = userRepository.findByPersona(entrenador.getPersona())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        usuario.setActivo(false);
        userRepository.save(usuario);
    }

}