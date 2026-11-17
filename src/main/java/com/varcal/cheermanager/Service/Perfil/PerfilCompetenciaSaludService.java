package com.varcal.cheermanager.Service.Perfil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.ResultadoCompetencia;
import com.varcal.cheermanager.Models.Eventos.RosterCompetencia;
import com.varcal.cheermanager.Models.Eventos.RosterIntegrante;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Lesion;
import com.varcal.cheermanager.Models.Personas.Lesion.Restriccion;
import com.varcal.cheermanager.Service.Eventos.RosterService;
import com.varcal.cheermanager.Service.Persona.LesionService;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Eventos.ResultadoCompetenciaRepository;
import com.varcal.cheermanager.repository.Eventos.RosterCompetenciaRepository;
import com.varcal.cheermanager.repository.Eventos.RosterIntegranteRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

/**
 * Perfil del deportista, segunda entrega: secciones Competencia (roster, resultados y aptitud para el proximo evento)
 * y Salud (estado y diagnostico). Solo lectura, sin tablas nuevas.
 *
 * Salud: la familia (portal) ve el estado (restriccion vigente) y el diagnostico de la lesion abierta, SIN los
 * seguimientos ni el historial; el personal con ver_lesiones ve ademas los seguimientos y las lesiones ya dadas de alta.
 * Cada seccion tiene su propio endpoint para que el perfil la cargue aparte (la aptitud recorre documentos y lesiones
 * de todo el club) y para que cada una exija su propio permiso.
 */
@Service
@Transactional(readOnly = true)
public class PerfilCompetenciaSaludService {

    static final int MAX_PARTICIPACIONES = 10;

    // ---------- Competencia ----------

    /** rol: TITULAR | SUPLENTE; rosterEstado: BORRADOR | CONFIRMADO. Resultado = el del GRUPO en ese evento (no del atleta). */
    public record Participacion(Integer eventoId, String evento, LocalDate fecha, Integer grupoId, String grupo,
            String rol, String posicion, String rosterEstado, Integer posicionGrupo, BigDecimal puntaje,
            String premio) {
    }

    /** problemas: los mismos avisos del roster (C2); apto = ninguno de ellos bloquea. */
    public record ProximoEvento(Integer eventoId, String evento, LocalDate fecha, String grupo, String rol,
            String posicion, String rosterEstado, boolean apto, List<RosterService.Problema> problemas) {
    }

    /** eventos y podios cuentan solo los eventos YA realizados en que fue titular de un roster CONFIRMADO. */
    public record DatosCompetencia(int eventos, int podios, ProximoEvento proximo,
            List<Participacion> participaciones) {
    }

    // ---------- Salud ----------

    public record NotaSalud(LocalDate fecha, String nota, String estado, String restriccion) {
    }

    /** seguimientos es null para la familia. */
    public record LesionVigente(Integer id, String zona, String descripcion, String gravedad, String restriccion,
            String estado, LocalDate fechaLesion, LocalDate fechaEstimadaRetorno, boolean retornoAtrasado,
            List<NotaSalud> seguimientos) {
    }

    public record LesionCerrada(Integer id, String zona, String descripcion, String gravedad, LocalDate fechaLesion,
            LocalDate fechaAlta, int dias) {
    }

    /** restriccion: SIN_ENTRENAR | MODIFICADO | SIN_RESTRICCION. historial es null para la familia. */
    public record DatosSalud(String restriccion, List<LesionVigente> vigentes, List<LesionCerrada> historial) {
    }

    private final DeportistaRepository deportistas;
    private final RosterIntegranteRepository integrantes;
    private final RosterCompetenciaRepository rosters;
    private final EventoRepository eventos;
    private final GrupoEntrenamientoRepository grupos;
    private final ResultadoCompetenciaRepository resultados;
    private final RosterService rosterService;
    private final LesionService lesionService;

    public PerfilCompetenciaSaludService(DeportistaRepository deportistas, RosterIntegranteRepository integrantes,
            RosterCompetenciaRepository rosters, EventoRepository eventos, GrupoEntrenamientoRepository grupos,
            ResultadoCompetenciaRepository resultados, RosterService rosterService, LesionService lesionService) {
        this.deportistas = deportistas;
        this.integrantes = integrantes;
        this.rosters = rosters;
        this.eventos = eventos;
        this.grupos = grupos;
        this.resultados = resultados;
        this.rosterService = rosterService;
        this.lesionService = lesionService;
    }

    // Puntos de extension (las pruebas los reemplazan: estos servicios son clases, no interfaces).

    protected LocalDate hoy() {
        return com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy();
    }

    protected List<RosterService.Problema> problemasDe(Integer deportistaId, LocalDate fecha) {
        return rosterService.problemasDe(deportistaId, fecha);
    }

    protected List<LesionService.Item> lesionesDe(Integer deportistaId) {
        return lesionService.historial(deportistaId);
    }

    protected Restriccion restriccionDe(Integer deportistaId) {
        return lesionService.restriccionVigente(deportistaId);
    }

    protected List<LesionService.Nota> seguimientosDe(Integer lesionId) {
        return lesionService.detalle(lesionId).seguimientos();
    }

    // ---------- Competencia ----------

    /** El llamador ya valido el acceso (permiso del club o deportista propio en el portal). */
    public DatosCompetencia competencia(Integer deportistaId) {
        existe(deportistaId);
        LocalDate hoy = hoy();
        List<Participacion> pasadas = new ArrayList<>();
        // Proximo = el evento futuro mas cercano en que figura; se guarda junto a su rol/roster para armar el aviso.
        Participacion proxima = null;
        int compitio = 0, podios = 0;
        for (RosterIntegrante i : integrantes.findByDeportistaId(deportistaId)) {
            RosterCompetencia r = rosters.findById(i.getRosterId()).orElse(null);
            Evento e = r == null ? null : eventos.findById(r.getEventoId()).orElse(null);
            if (e == null) {
                continue;
            }
            GrupoEntrenamiento g = grupos.findById(r.getGrupoId()).orElse(null);
            ResultadoCompetencia res = resultados.findByEventoIdAndGrupoEntrenamientoId(e.getId(), r.getGrupoId())
                    .orElse(null);
            String premio = res == null || res.getPremio() == null ? null
                    : (res.getPremio().getPremio() != null ? res.getPremio().getPremio()
                            : res.getPremio().getDescripcion());
            Participacion p = new Participacion(e.getId(), e.getNombre(), e.getFecha(), r.getGrupoId(),
                    g == null ? null : g.getNombre(), i.getRol().name(), i.getPosicion(), r.getEstado().name(),
                    res == null ? null : res.getPosicion(), res == null ? null : res.getPuntaje(), premio);
            boolean futuro = e.getFecha() == null || e.getFecha().isAfter(hoy);
            if (futuro) {
                if (proxima == null || fechaOrden(p).isBefore(fechaOrden(proxima))) {
                    proxima = p;
                }
                continue;
            }
            pasadas.add(p);
            if (i.getRol() == RosterIntegrante.Rol.TITULAR && r.getEstado() == RosterCompetencia.Estado.CONFIRMADO) {
                compitio++;
                if (p.posicionGrupo() != null && p.posicionGrupo() <= 3) {
                    podios++;
                }
            }
        }
        pasadas.sort(Comparator.comparing(Participacion::fecha, Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder()))
                .reversed());
        ProximoEvento proximo = null;
        if (proxima != null) {
            List<RosterService.Problema> ps = problemasDe(deportistaId, proxima.fecha());
            proximo = new ProximoEvento(proxima.eventoId(), proxima.evento(), proxima.fecha(), proxima.grupo(),
                    proxima.rol(), proxima.posicion(), proxima.rosterEstado(),
                    ps.stream().noneMatch(RosterService.Problema::bloquea), ps);
        }
        List<Participacion> recientes = pasadas.size() > MAX_PARTICIPACIONES
                ? new ArrayList<>(pasadas.subList(0, MAX_PARTICIPACIONES)) : pasadas;
        return new DatosCompetencia(compitio, podios, proximo, recientes);
    }

    private static LocalDate fechaOrden(Participacion p) {
        return p.fecha() == null ? LocalDate.MAX : p.fecha();
    }

    // ---------- Salud ----------

    /** @param detalleClub true = personal con ver_lesiones (seguimientos e historial); false = familia (estado y diagnostico). */
    public DatosSalud salud(Integer deportistaId, boolean detalleClub) {
        existe(deportistaId);
        List<LesionVigente> vigentes = new ArrayList<>();
        List<LesionCerrada> cerradas = new ArrayList<>();
        for (LesionService.Item l : lesionesDe(deportistaId)) {
            if (l.estado() != Lesion.Estado.ALTA) {
                List<NotaSalud> notas = null;
                if (detalleClub) {
                    notas = seguimientosDe(l.id()).stream()
                            .map(n -> new NotaSalud(n.fecha(), n.nota(), nombre(n.estado()), nombre(n.restriccion())))
                            .toList();
                }
                vigentes.add(new LesionVigente(l.id(), l.zona().name(), l.descripcion(), l.gravedad().name(),
                        l.restriccion().name(), l.estado().name(), l.fechaLesion(), l.fechaEstimadaRetorno(),
                        l.retornoAtrasado(), notas));
            } else if (detalleClub) {
                cerradas.add(new LesionCerrada(l.id(), l.zona().name(), l.descripcion(), l.gravedad().name(),
                        l.fechaLesion(), l.fechaAlta(), l.dias()));
            }
        }
        return new DatosSalud(restriccionDe(deportistaId).name(), vigentes, detalleClub ? cerradas : null);
    }

    // ---------- utilidades ----------

    private void existe(Integer deportistaId) {
        if (deportistas.findById(deportistaId).isEmpty()) {
            throw new IllegalArgumentException("Deportista no encontrado");
        }
    }

    private static String nombre(Enum<?> e) {
        return e == null ? null : e.name();
    }
}
