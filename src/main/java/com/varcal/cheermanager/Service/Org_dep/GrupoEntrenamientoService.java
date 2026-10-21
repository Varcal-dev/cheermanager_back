package com.varcal.cheermanager.Service.Org_dep;

import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Org_dep.CandidatoGrupoDTO;
import com.varcal.cheermanager.DTO.Org_dep.CandidatosGrupoResponseDTO;
import com.varcal.cheermanager.DTO.Org_dep.DeportistaInscritoDTO;
import com.varcal.cheermanager.DTO.Org_dep.GrupoElegibleDTO;
import com.varcal.cheermanager.DTO.Org_dep.GruposElegiblesResponseDTO;
import com.varcal.cheermanager.DTO.Persona.AsignacionEntrenadorDTO;
import com.varcal.cheermanager.DTO.Persona.AsignacionEntrenadorResponseDTO;
import com.varcal.cheermanager.DTO.Persona.AsignacionEntrenadorUpdateDTO;
import com.varcal.cheermanager.DTO.Persona.HorarioEntrenadorResponseDTO;
import com.varcal.cheermanager.Models.Horario_Asistencia.HorarioEntrenamiento;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioEntrenamientoRepository;
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
    private HorarioEntrenamientoRepository horarioEntrenamientoRepository;

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
        // Antes llamaba a una funcion de MySQL (verificar_elegibilidad_deportista_grupo) que no estaba en los
        // scripts del proyecto: una base nueva fallaba con "FUNCTION does not exist". Ahora se calcula aqui,
        // con las mismas reglas que /candidatos (cupo y rango de ano de nacimiento de la categoria).
        GrupoEntrenamiento grupo = grupoEntrenamientoRepository.findById(grupoId)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado"));
        Deportista deportista = deportistaRepository.findById(deportistaId)
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado"));

        if (deportistaPerteneceGrupoRepository.existsByDeportistaIdAndFechaFinIsNull(deportistaId)) {
            return "El deportista ya pertenece activamente a un grupo";
        }
        CategoriaNivel categoria = grupo.getCategoriaNivel();
        Integer cupoMaximo = categoria != null ? categoria.getCantidadMax() : null;
        int cupoActual = deportistaPerteneceGrupoRepository.findByGrupoIdAndFechaFinIsNull(grupoId).size();
        boolean lleno = cupoMaximo != null && cupoActual >= cupoMaximo;
        java.time.LocalDate nac = deportista.getPersona().getFechaNacimiento();
        String motivo = calcularMotivoNoElegible(nac != null ? nac.getYear() : null,
                categoria != null ? categoria.getAñoNacimientoMin() : null,
                categoria != null ? categoria.getAñoNacimientoMax() : null, lleno, cupoActual, cupoMaximo);
        return motivo == null ? "Elegible" : motivo;
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

    // ===== Historial de asignaciones entrenador-grupo (panel en entrenadores/page.tsx) =====

    public List<AsignacionEntrenadorResponseDTO> listarAsignacionesPorEntrenador(Integer entrenadorId) {
        return grupoEntrenadorRepository.findByEntrenadorIdOrderByFechaInicioDesc(entrenadorId).stream()
                .map(this::toAsignacionResponseDTO)
                .collect(Collectors.toList());
    }

    // Edita rol y/o fechaFin de una asignación existente. No se permite
    // reasignar el entrenador o el grupo desde acá — si el entrenador
    // cambió de grupo, es una asignación nueva, no una edición de esta.
    public AsignacionEntrenadorResponseDTO actualizarAsignacionEntrenador(Integer asignacionId,
            AsignacionEntrenadorUpdateDTO dto) {
        GrupoEntrenador asignacion = grupoEntrenadorRepository.findById(asignacionId)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada"));

        if (dto.getRol() != null && !dto.getRol().isBlank()) {
            asignacion.setRol(dto.getRol());
        }
        // fechaFin explícita en null limpia el campo (permite "reabrir" una
        // asignación que se había cerrado por error); string vacío se trata
        // igual que null por si el frontend manda "" en vez de omitir el campo.
        asignacion.setFechaFin(
                dto.getFechaFin() != null && !dto.getFechaFin().isBlank()
                        ? LocalDate.parse(dto.getFechaFin())
                        : null);

        return toAsignacionResponseDTO(grupoEntrenadorRepository.save(asignacion));
    }

    // Mismo criterio que quitarDeportistaDeGrupo: no se borra el registro,
    // se cierra con fechaFin = hoy. Conserva el historial de quién entrenó
    // a quién y cuándo.
    public String finalizarAsignacionEntrenador(Integer asignacionId) {
        GrupoEntrenador asignacion = grupoEntrenadorRepository.findById(asignacionId)
                .orElseThrow(() -> new RuntimeException("Asignación no encontrada"));

        if (asignacion.getFechaFin() != null) {
            throw new RuntimeException("Esta asignación ya estaba finalizada");
        }

        asignacion.setFechaFin(LocalDate.now());
        grupoEntrenadorRepository.save(asignacion);

        return "Asignación finalizada correctamente";
    }

    private AsignacionEntrenadorResponseDTO toAsignacionResponseDTO(GrupoEntrenador a) {
        AsignacionEntrenadorResponseDTO dto = new AsignacionEntrenadorResponseDTO();
        dto.setId(a.getId());
        dto.setGrupoId(a.getGrupo().getId());
        dto.setGrupoNombre(a.getGrupo().getNombre());
        dto.setRol(a.getRol());
        dto.setFechaInicio(a.getFechaInicio());
        dto.setFechaFin(a.getFechaFin());
        return dto;
    }

    // ===== Panel "Horarios" en entrenadores/page.tsx =====
    //
    // No es una entidad propia: es la unión de las asignaciones ACTIVAS de
    // este entrenador (GrupoEntrenador, fechaFin == null) con los bloques de
    // horario reales de cada uno de esos grupos (Horario, vía
    // HorarioEntrenamiento). Un mismo grupo puede tener varios bloques
    // (ej. lunes y miércoles), así que un entrenador con 1 grupo puede
    // aparecer con varias filas acá — todas comparten el mismo asignacionId.
    public List<HorarioEntrenadorResponseDTO> listarHorarioPorEntrenador(Integer entrenadorId) {
        List<HorarioEntrenadorResponseDTO> resultado = new ArrayList<>();

        List<GrupoEntrenador> activas = grupoEntrenadorRepository.findByEntrenadorIdOrderByFechaInicioDesc(entrenadorId)
                .stream()
                .filter(a -> a.getFechaFin() == null)
                .collect(Collectors.toList());

        for (GrupoEntrenador asignacion : activas) {
            List<HorarioEntrenamiento> bloques = horarioEntrenamientoRepository
                    .findByGrupoEntrenamientoId(asignacion.getGrupo().getId());

            for (HorarioEntrenamiento bloque : bloques) {
                HorarioEntrenadorResponseDTO dto = new HorarioEntrenadorResponseDTO();
                dto.setAsignacionId(asignacion.getId());
                dto.setGrupoId(asignacion.getGrupo().getId());
                dto.setGrupoNombre(asignacion.getGrupo().getNombre());
                dto.setRol(asignacion.getRol());
                dto.setDia(bloque.getHorario().getDia().getDia());
                dto.setHoraInicio(bloque.getHorario().getHoraInicio());
                dto.setHoraFin(bloque.getHorario().getHoraFin());
                resultado.add(dto);
            }
        }

        return resultado;
    }
}