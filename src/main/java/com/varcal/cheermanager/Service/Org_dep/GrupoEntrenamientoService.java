package com.varcal.cheermanager.Service.Org_dep;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Org_dep.CandidatoGrupoDTO;
import com.varcal.cheermanager.DTO.Org_dep.CandidatosGrupoResponseDTO;
import com.varcal.cheermanager.DTO.Org_dep.DeportistaInscritoDTO;
import com.varcal.cheermanager.DTO.Org_dep.GrupoElegibleDTO;
import com.varcal.cheermanager.DTO.Org_dep.GruposElegiblesResponseDTO;
import com.varcal.cheermanager.DTO.Persona.AsignacionEntrenadorDTO;
import com.varcal.cheermanager.Models.Org_dep.CategoriaNivel;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenador;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Entrenador;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenadorRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.EntrenadorRepository;

@Service
public class GrupoEntrenamientoService {

    @Autowired
    private GrupoEntrenamientoRepository grupoEntrenamientoRepository;

    @Autowired
    private DeportistaPerteneceGrupoRepository deportistaPerteneceGrupoRepository;

    @Autowired
    private InscripcionService inscripcionService;

    @Autowired
    private DeportistaRepository deportistaRepository;

    @Autowired
    private GrupoEntrenadorRepository grupoEntrenadorRepository;

    @Autowired
    private EntrenadorRepository entrenadorRepository;

    public GrupoEntrenamiento crearGrupoEntrenamiento(GrupoEntrenamiento grupo) throws Exception {
        try {
            return grupoEntrenamientoRepository.save(grupo);
        } catch (Exception e) {
            // Captura otras excepciones inesperadas
            throw new Exception("Error al crear el grupo de entrenamiento.", e);
        }
    }

    public void GrupoService(GrupoEntrenamientoRepository grupoRepository) {
        this.grupoEntrenamientoRepository = grupoRepository;
    }

    // Se mantiene por compatibilidad (llama al mismo SP de siempre), pero el
    // front ya no lo usa para pintar la lista de candidatos: ver
    // obtenerCandidatosGrupo, que resuelve todo en una sola llamada y con
    // motivo estructurado en vez de un string suelto.
    public String verificarElegibilidadDeportista(Integer grupoId, Integer deportistaId) {
        return grupoEntrenamientoRepository.verificarElegibilidadDeportista(deportistaId, grupoId);
    }

    public String agregarDeportistaAGrupo(Integer deportistaId, Integer grupoId, String observaciones) {
        if (deportistaPerteneceGrupoRepository.existsByDeportistaIdAndFechaFinIsNull(deportistaId)) {
            throw new RuntimeException("El deportista ya pertenece activamente a un grupo");
        }

        GrupoEntrenamiento grupo = grupoEntrenamientoRepository.findById(grupoId)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado"));

        DeportistaPerteneceGrupo membresia = new DeportistaPerteneceGrupo();
        membresia.setGrupo(grupo);
        membresia.setDeportista(deportista);
        // fechaFin queda null → membresía activa
        deportistaPerteneceGrupoRepository.save(membresia);

        return "Deportista agregado correctamente al grupo";
    }

    @Transactional
    public String asignarEntrenadorAGrupo(AsignacionEntrenadorDTO dto) {
        try {
            if (grupoEntrenadorRepository.existsByEntrenadorIdAndGrupoIdAndFechaFinIsNull(
                    dto.getEntrenadorId(), dto.getGrupoId())) {
                throw new RuntimeException("El entrenador ya está asignado activamente a este grupo");
            }

            GrupoEntrenamiento grupo = grupoEntrenamientoRepository.findById(dto.getGrupoId())
                    .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
            Entrenador entrenador = entrenadorRepository.findById(dto.getEntrenadorId())
                    .orElseThrow(() -> new RuntimeException("Entrenador no encontrado"));

            GrupoEntrenador asignacion = new GrupoEntrenador();
            asignacion.setGrupo(grupo);
            asignacion.setEntrenador(entrenador);
            asignacion.setFechaInicio(LocalDate.parse(dto.getFechaInicio()));
            asignacion.setFechaFin(
                    dto.getFechaFin() != null && !dto.getFechaFin().isEmpty()
                            ? LocalDate.parse(dto.getFechaFin())
                            : null);
            asignacion.setRol(dto.getRol());

            grupoEntrenadorRepository.save(asignacion);

            return "Entrenador asignado correctamente al grupo";
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            return "Error al asignar entrenador: " + e.getMessage();
        }
    }

    // ===== Candidatos de un grupo, en una sola llamada =====
    //
    // Antes el front pedía la lista completa de inscritos y, por cada uno,
    // un GET individual a /verificar-elegibilidad (N+1: con 60 inscritos,
    // 60 requests solo para abrir el modal). Acá se resuelve todo junto:
    //
    // 1. Se parte de deportistas con inscripción "Activa" y SIN grupo
    // asignado actualmente (ya descarta de entrada a quien no puede ser
    // candidato, en vez de preguntarle al SP uno por uno).
    // 2. Se calcula el cupo actual del grupo contando membresías activas en
    // grupo_deportista (fechaFin null).
    // 3. Se valida el rango de año de nacimiento de la categoría del grupo
    // (año_nacimiento_min/max) y el cupo máximo (cantidad_max), ambos ya
    // existentes en categorias_nivel pero sin usar en ningún lado del
    // código hasta ahora.
    //
    // El resultado ya viene separado en elegibles/no-elegibles y con motivo
    // legible, para que el front no tenga que interpretar texto libre.
    public CandidatosGrupoResponseDTO obtenerCandidatosGrupo(Integer grupoId) {
        GrupoEntrenamiento grupo = grupoEntrenamientoRepository.findById(grupoId)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));

        CategoriaNivel categoria = grupo.getCategoriaNivel();
        Integer añoMin = categoria != null ? categoria.getAñoNacimientoMin() : null;
        Integer añoMax = categoria != null ? categoria.getAñoNacimientoMax() : null;
        Integer cupoMaximo = categoria != null ? categoria.getCantidadMax() : null;

        int cupoActual = deportistaPerteneceGrupoRepository.findByGrupoIdAndFechaFinIsNull(grupoId).size();
        boolean grupoLleno = cupoMaximo != null && cupoActual >= cupoMaximo;

        List<CandidatoGrupoDTO> elegibles = new ArrayList<>();
        List<CandidatoGrupoDTO> noElegibles = new ArrayList<>();

        List<DeportistaInscritoDTO> candidatos = inscripcionService.obtenerDeportistasInscritosDTO().stream()
                .filter(d -> "Activa".equalsIgnoreCase(d.getEstadoInscripcion()))
                .filter(d -> d.getGrupoEntrenamiento() == null || d.getGrupoEntrenamiento().isBlank())
                .toList();

        for (DeportistaInscritoDTO d : candidatos) {
            Integer añoNacimiento = d.getFechaNacimiento() != null ? d.getFechaNacimiento().getYear() : null;
            Integer edad = d.getFechaNacimiento() != null
                    ? Period.between(d.getFechaNacimiento(), LocalDate.now()).getYears()
                    : null;

            String motivo = calcularMotivoNoElegible(añoNacimiento, añoMin, añoMax, grupoLleno, cupoActual,
                    cupoMaximo);

            CandidatoGrupoDTO dto = new CandidatoGrupoDTO(
                    d.getDeportistaId(),
                    d.getNombre() + " " + d.getApellidos(),
                    d.getGenero(),
                    d.getNivel(),
                    d.getFechaNacimiento(),
                    edad,
                    motivo == null,
                    motivo);

            if (motivo == null) {
                elegibles.add(dto);
            } else {
                noElegibles.add(dto);
            }
        }

        elegibles.sort(Comparator.comparing(CandidatoGrupoDTO::getNombreCompleto));
        noElegibles.sort(Comparator.comparing(CandidatoGrupoDTO::getNombreCompleto));

        return new CandidatosGrupoResponseDTO(grupo.getNombre(), cupoActual, cupoMaximo, elegibles, noElegibles);
    }

    // Agregar en GrupoEntrenamientoService.java (junto a obtenerCandidatosGrupo)

    // ===== Grupos elegibles para un deportista, en una sola llamada =====
    //
    // Reverso de obtenerCandidatosGrupo: en vez de "¿quién entra en este
    // grupo?", resuelve "¿en qué grupos entra este deportista?". Reusa la
    // misma regla (año de nacimiento de la categoría + cupo del grupo) para
    // que las dos vistas nunca queden desincronizadas si el criterio cambia.
    public GruposElegiblesResponseDTO obtenerGruposElegiblesParaDeportista(Integer deportistaId) {
        DeportistaInscritoDTO deportista = inscripcionService.obtenerDeportistasInscritosDTO().stream()
                .filter(d -> d.getDeportistaId().equals(deportistaId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "El deportista no tiene una inscripción activa"));

        if (deportista.getGrupoEntrenamiento() != null && !deportista.getGrupoEntrenamiento().isBlank()) {
            throw new RuntimeException(
                    "El deportista ya pertenece al grupo: " + deportista.getGrupoEntrenamiento());
        }

        Integer añoNacimiento = deportista.getFechaNacimiento() != null
                ? deportista.getFechaNacimiento().getYear()
                : null;

        List<GrupoElegibleDTO> elegibles = new ArrayList<>();
        List<GrupoElegibleDTO> noElegibles = new ArrayList<>();

        for (GrupoEntrenamiento grupo : grupoEntrenamientoRepository.findAll()) {
            CategoriaNivel categoria = grupo.getCategoriaNivel();
            Integer añoMin = categoria != null ? categoria.getAñoNacimientoMin() : null;
            Integer añoMax = categoria != null ? categoria.getAñoNacimientoMax() : null;
            Integer cupoMaximo = categoria != null ? categoria.getCantidadMax() : null;

            int cupoActual = deportistaPerteneceGrupoRepository
                    .findByGrupoIdAndFechaFinIsNull(grupo.getId()).size();
            boolean grupoLleno = cupoMaximo != null && cupoActual >= cupoMaximo;

            String motivo = calcularMotivoNoElegible(añoNacimiento, añoMin, añoMax, grupoLleno,
                    cupoActual, cupoMaximo);

            GrupoElegibleDTO dto = new GrupoElegibleDTO(
                    grupo.getId(),
                    grupo.getNombre(),
                    grupo.getTipoGrupo() != null ? grupo.getTipoGrupo().getNombre() : null,
                    categoria != null ? categoria.getNombre() : null,
                    cupoActual,
                    cupoMaximo,
                    motivo == null,
                    motivo);

            if (motivo == null) {
                elegibles.add(dto);
            } else {
                noElegibles.add(dto);
            }
        }

        elegibles.sort(Comparator.comparing(GrupoElegibleDTO::getNombreGrupo));
        noElegibles.sort(Comparator.comparing(GrupoElegibleDTO::getNombreGrupo));

        return new GruposElegiblesResponseDTO(
                deportista.getDeportistaId(),
                deportista.getNombre() + " " + deportista.getApellidos(),
                elegibles,
                noElegibles);
    }

    private String calcularMotivoNoElegible(Integer añoNacimiento, Integer añoMin, Integer añoMax,
            boolean grupoLleno, int cupoActual, Integer cupoMaximo) {
        if (grupoLleno) {
            return "Cupo lleno (" + cupoActual + "/" + cupoMaximo + ")";
        }
        if (añoNacimiento != null && añoMin != null && añoNacimiento < añoMin) {
            return "Nacido en " + añoNacimiento + ": fuera del rango de esta categoría (desde " + añoMin + ")";
        }
        if (añoNacimiento != null && añoMax != null && añoNacimiento > añoMax) {
            return "Nacido en " + añoNacimiento + ": fuera del rango de esta categoría (hasta " + añoMax + ")";
        }
        return null;
    }

    // ===== Quitar deportista de un grupo =====
    //
    // Cierra la membresía (fechaFin = hoy) en vez de borrar la fila, para
    // conservar el historial de a qué grupos perteneció el deportista y
    // cuándo (historialGrupos en Deportista ya depende de esto). Al cerrarla,
    // el deportista vuelve a aparecer como candidato disponible para
    // cualquier grupo (incluido el mismo) en obtenerCandidatosGrupo.
    @Transactional
    public String quitarDeportistaDeGrupo(Integer grupoId, Integer deportistaId) {
        DeportistaPerteneceGrupo membresia = deportistaPerteneceGrupoRepository
                .findByGrupoIdAndDeportistaIdAndFechaFinIsNull(grupoId, deportistaId)
                .orElseThrow(() -> new RuntimeException("El deportista no pertenece actualmente a este grupo"));

        membresia.setFechaFin(LocalDate.now());
        deportistaPerteneceGrupoRepository.save(membresia);

        return "Deportista removido del grupo correctamente";
    }
}