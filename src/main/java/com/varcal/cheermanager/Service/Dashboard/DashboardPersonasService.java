package com.varcal.cheermanager.Service.Dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Dashboard.ResumenPersonasDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenPersonasDTO.EntrenadorResumenDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenPersonasDTO.MovimientoEstadoDTO;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Entrenador;
import com.varcal.cheermanager.Models.Personas.Genero;
import com.varcal.cheermanager.Models.Personas.HistorialDeportistaEstado;
import com.varcal.cheermanager.Service.Historiales.HistorialMedicoService;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.EntrenadorGrupoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.EntrenadorRepository;
import com.varcal.cheermanager.repository.Personas.GeneroRepository;
import com.varcal.cheermanager.repository.Personas.HistorialDeportistaEstadoRepository;

@Service
public class DashboardPersonasService {

    @Autowired
    private DeportistaRepository deportistaRepository;

    @Autowired
    private EntrenadorRepository entrenadorRepository;

    @Autowired
    private EntrenadorGrupoRepository entrenadorGrupoRepository;

    @Autowired
    private GeneroRepository generoRepository;

    @Autowired
    private HistorialMedicoService historialMedicoService;

    @Autowired
    private HistorialDeportistaEstadoRepository historialDeportistaEstadoRepository;

    @Autowired
    private DeportistaPerteneceGrupoRepository deportistaPerteneceGrupoRepository;

    public ResumenPersonasDTO obtenerResumen(int diasAltasBajas) {
        ResumenPersonasDTO dto = new ResumenPersonasDTO();

        List<Deportista> deportistas = deportistaRepository.findAll();
        dto.setTotalDeportistas(deportistas.size());
        dto.setDeportistasPorEstado(agruparPorEstado(deportistas));
        dto.setDeportistasPorGenero(agruparPorGenero(deportistas));
        // Antes era un rango de edad genérico (6-8, 9-11...) inventado por
        // el sistema. Se reemplaza por la división FEDECOLCHEER real
        // (Tiny/Mini/Youth/Junior/Senior/Open...) del grupo en el que cada
        // deportista está inscrito hoy, usando el esquema Division/
        // CategoriaNivel/GrupoEntrenamiento que ya existía en el proyecto.
        dto.setDeportistasPorDivision(agruparPorDivision(deportistas));
        dto.setDeportistasConCondicionMedicaActiva(contarCondicionesActivas(deportistas));

        // Altas y bajas: usa HistorialDeportistaEstado, que ya existía
        // (Model + Repository) pero no estaba conectado a ningún dashboard.
        List<MovimientoEstadoDTO> movimientos = listarMovimientos(diasAltasBajas, deportistas);
        dto.setAltasRecientes(movimientos.stream().filter(m -> "ALTA".equals(m.getTipo())).count());
        dto.setBajasRecientes(movimientos.stream().filter(m -> "BAJA".equals(m.getTipo())).count());
        dto.setMovimientosRecientes(movimientos);

        List<Entrenador> entrenadores = entrenadorRepository.findAll();
        dto.setTotalEntrenadores(entrenadores.size());
        dto.setEntrenadores(entrenadores.stream().map(this::toEntrenadorResumen).collect(Collectors.toList()));

        return dto;
    }

    private Map<String, Long> agruparPorEstado(List<Deportista> deportistas) {
        Map<String, Long> conteo = new HashMap<>();
        for (Deportista d : deportistas) {
            String estado = d.getEstado() != null ? d.getEstado().getEstado() : "Sin estado";
            conteo.merge(estado, 1L, Long::sum);
        }
        return conteo;
    }

    private Map<String, Long> agruparPorGenero(List<Deportista> deportistas) {
        Map<Integer, String> catalogo = generoRepository.findAll().stream()
                .collect(Collectors.toMap(Genero::getId, Genero::getGenero));

        Map<String, Long> conteo = new HashMap<>();
        for (Deportista d : deportistas) {
            Integer generoId = d.getPersona().getGeneroId();
            String nombre = generoId != null ? catalogo.getOrDefault(generoId, "Desconocido") : "Sin especificar";
            conteo.merge(nombre, 1L, Long::sum);
        }
        return conteo;
    }

    // Un deportista puede pertenecer a más de un grupo (crossover) — se
    // cuenta en CADA división de sus grupos activos, no solo en una. Un
    // deportista sin grupo asignado cae en "Sin grupo asignado".
    private Map<String, Long> agruparPorDivision(List<Deportista> deportistas) {
        Map<String, Long> conteo = new HashMap<>();

        for (Deportista d : deportistas) {
            List<DeportistaPerteneceGrupo> pertenencias = deportistaPerteneceGrupoRepository.findByDeportistaIdAndFechaFinIsNull(d.getId());

            if (pertenencias == null || pertenencias.isEmpty()) {
                conteo.merge("Sin grupo asignado", 1L, Long::sum);
                continue;
            }

            for (DeportistaPerteneceGrupo p : pertenencias) {
                var categoriaNivel = p.getGrupo().getCategoriaNivel();
                String etiqueta = categoriaNivel != null && categoriaNivel.getDivision() != null
                        ? categoriaNivel.getDivision().getNombre()
                        : "Sin división configurada";
                conteo.merge(etiqueta, 1L, Long::sum);
            }
        }
        return conteo;
    }

    private int contarCondicionesActivas(List<Deportista> deportistas) {
        return (int) deportistas.stream()
                .filter(d -> historialMedicoService.tieneCondicionActiva(d.getPersona().getId()))
                .count();
    }

    private static final ZoneId ZONA = ZoneId.of("America/Bogota");

    /**
     * Movimientos del periodo, del mas reciente al mas antiguo.
     * ALTA = deportista nuevo (registro) o que vuelve tras estar Retirado; BAJA = pasa a Retirado; CAMBIO = cualquier
     * otro cambio de estado (Lesionado, Suspendido...), que se muestra pero no cuenta como alta ni baja.
     * Los deportistas registrados antes de que el registro dejara su fila inicial en el historial se toman de su
     * fecha de registro, para que sus altas tampoco se pierdan.
     */
    private List<MovimientoEstadoDTO> listarMovimientos(int dias, List<Deportista> deportistas) {
        LocalDateTime desde = LocalDateTime.now(ZONA).minusDays(dias);
        List<HistorialDeportistaEstado> historial = historialDeportistaEstadoRepository.findAllByOrderByFechaCambioDesc();

        java.util.Set<Integer> conFilaDeRegistro = historial.stream()
                .filter(h -> h.getEstadoAnterior() == null)
                .map(h -> h.getDeportista().getId())
                .collect(Collectors.toSet());

        List<MovimientoEstadoDTO> lista = historial.stream()
                .filter(h -> h.getFechaCambio().isAfter(desde))
                .map(this::toMovimientoDTO)
                .collect(Collectors.toCollection(java.util.ArrayList::new));

        for (Deportista d : deportistas) {
            LocalDate registro = d.getFechaRegistro();
            if (registro == null || conFilaDeRegistro.contains(d.getId()) || !registro.atStartOfDay().isAfter(desde)) {
                continue;
            }
            MovimientoEstadoDTO m = new MovimientoEstadoDTO();
            m.setDeportistaId(d.getId());
            m.setNombreCompleto(d.getPersona().getNombre() + " " + d.getPersona().getApellidos());
            m.setEstadoAnterior(null);
            m.setEstadoNuevo(d.getEstado() != null ? d.getEstado().getEstado() : null);
            m.setFechaCambio(registro.atStartOfDay());
            m.setTipo("ALTA");
            m.setEsAlta(true);
            m.setMotivo("Registro del deportista");
            lista.add(m);
        }
        lista.sort((a, b) -> b.getFechaCambio().compareTo(a.getFechaCambio()));
        return lista;
    }

    private static boolean esRetirado(com.varcal.cheermanager.Models.Personas.EstadoPersona e) {
        return e != null && "Retirado".equalsIgnoreCase(e.getEstado());
    }

    private static String tipoDe(HistorialDeportistaEstado h) {
        if (h.getEstadoAnterior() == null) {
            return "ALTA"; // primer registro del deportista
        }
        if (esRetirado(h.getEstadoNuevo()) && !esRetirado(h.getEstadoAnterior())) {
            return "BAJA";
        }
        if (esRetirado(h.getEstadoAnterior()) && !esRetirado(h.getEstadoNuevo())) {
            return "ALTA"; // reingreso
        }
        return "CAMBIO";
    }

    private MovimientoEstadoDTO toMovimientoDTO(HistorialDeportistaEstado h) {
        MovimientoEstadoDTO dto = new MovimientoEstadoDTO();
        dto.setDeportistaId(h.getDeportista().getId());
        dto.setNombreCompleto(h.getDeportista().getPersona().getNombre() + " " + h.getDeportista().getPersona().getApellidos());
        dto.setEstadoAnterior(h.getEstadoAnterior() != null ? h.getEstadoAnterior().getEstado() : null);
        dto.setEstadoNuevo(h.getEstadoNuevo() != null ? h.getEstadoNuevo().getEstado() : null);
        dto.setFechaCambio(h.getFechaCambio());
        String tipo = tipoDe(h);
        dto.setTipo(tipo);
        dto.setEsAlta("ALTA".equals(tipo));
        dto.setMotivo(h.getMotivoCambio());
        return dto;
    }

    private EntrenadorResumenDTO toEntrenadorResumen(Entrenador e) {
        EntrenadorResumenDTO dto = new EntrenadorResumenDTO();
        dto.setEntrenadorId(e.getId());
        dto.setNombreCompleto(e.getPersona().getNombre() + " " + e.getPersona().getApellidos());
        dto.setEspecializacion(e.getEspecializacion());
        dto.setEstado(e.getEstado() != null ? e.getEstado().getEstado() : "Sin estado");
        dto.setGruposACargo(entrenadorGrupoRepository.countGruposByEntrenadorId(e.getId()));
        return dto;
    }
}