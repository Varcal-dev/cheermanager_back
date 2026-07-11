package com.varcal.cheermanager.Service.Dashboard;

import java.time.LocalDateTime;
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
        dto.setAltasRecientes(contarMovimientos(diasAltasBajas, true));
        dto.setBajasRecientes(contarMovimientos(diasAltasBajas, false));
        dto.setMovimientosRecientes(listarMovimientos(diasAltasBajas));

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

    private long contarMovimientos(int dias, boolean altas) {
        LocalDateTime desde = LocalDateTime.now().minusDays(dias);
        return historialDeportistaEstadoRepository.findAllByOrderByFechaCambioDesc().stream()
                .filter(h -> h.getFechaCambio().isAfter(desde))
                .filter(h -> esAlta(h) == altas)
                .count();
    }

    private List<MovimientoEstadoDTO> listarMovimientos(int dias) {
        LocalDateTime desde = LocalDateTime.now().minusDays(dias);
        return historialDeportistaEstadoRepository.findAllByOrderByFechaCambioDesc().stream()
                .filter(h -> h.getFechaCambio().isAfter(desde))
                .map(this::toMovimientoDTO)
                .collect(Collectors.toList());
    }

    // "Alta" = el estado nuevo se llama "Activo" (mismo supuesto de texto
    // literal que ya usa el resto del sistema, ej. HistorialMedicoService,
    // el % de asistencia, etc.) — cualquier otro estado nuevo se cuenta
    // como "baja" (Inactivo, Retirado, Suspendido...).
    private boolean esAlta(HistorialDeportistaEstado h) {
        return h.getEstadoNuevo() != null && "Activo".equalsIgnoreCase(h.getEstadoNuevo().getEstado());
    }

    private MovimientoEstadoDTO toMovimientoDTO(HistorialDeportistaEstado h) {
        MovimientoEstadoDTO dto = new MovimientoEstadoDTO();
        dto.setDeportistaId(h.getDeportista().getId());
        dto.setNombreCompleto(h.getDeportista().getPersona().getNombre() + " " + h.getDeportista().getPersona().getApellidos());
        dto.setEstadoAnterior(h.getEstadoAnterior() != null ? h.getEstadoAnterior().getEstado() : null);
        dto.setEstadoNuevo(h.getEstadoNuevo() != null ? h.getEstadoNuevo().getEstado() : null);
        dto.setFechaCambio(h.getFechaCambio());
        dto.setEsAlta(esAlta(h));
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