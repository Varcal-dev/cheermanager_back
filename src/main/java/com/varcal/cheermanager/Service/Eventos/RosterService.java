package com.varcal.cheermanager.Service.Eventos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Eventos.GrupoEvento;
import com.varcal.cheermanager.Models.Eventos.RosterCompetencia;
import com.varcal.cheermanager.Models.Eventos.RosterCompetencia.Estado;
import com.varcal.cheermanager.Models.Eventos.RosterIntegrante;
import com.varcal.cheermanager.Models.Eventos.RosterIntegrante.Rol;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.DocumentoDeportista;
import com.varcal.cheermanager.Models.Personas.DocumentoDeportista.Tipo;
import com.varcal.cheermanager.Models.Personas.Lesion;
import com.varcal.cheermanager.Models.Personas.Lesion.Restriccion;
import com.varcal.cheermanager.repository.Eventos.GrupoEventoRepository;
import com.varcal.cheermanager.repository.Eventos.RosterCompetenciaRepository;
import com.varcal.cheermanager.repository.Eventos.RosterIntegranteRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Personas.DocumentoDeportistaRepository;
import com.varcal.cheermanager.repository.Personas.LesionRepository;

/**
 * C2: roster de competencia. Quien compite (titulares) y quien reemplaza (suplentes) por un grupo en un evento.
 * Cruza lesiones (C3) y documentos (C4) para avisar quien no esta apto. Solo se muestra el hecho
 * ("lesion vigente, no entrena"), no el detalle de salud.
 */
@Service
public class RosterService {

    public record Problema(String tipo, boolean bloquea, String detalle) {
    }

    public record Integrante(Integer id, Integer deportistaId, String nombre, Rol rol, String posicion,
            List<Problema> problemas) {
    }

    public record Candidato(Integer deportistaId, String nombre, List<Problema> problemas) {
    }

    public record Vista(Integer eventoId, String evento, LocalDate fecha, Integer grupoId, String grupo,
            String estado, Integer cupoMaximo, int titulares, int suplentes, int titularesBloqueados,
            boolean confirmable, List<Integrante> integrantes, List<Candidato> candidatos) {
    }

    public record Opcion(Integer eventoId, String evento, LocalDate fecha, Integer grupoId, String grupo,
            String estado, int titulares, int suplentes) {
    }

    private final RosterCompetenciaRepository rosters;
    private final RosterIntegranteRepository integrantes;
    private final GrupoEventoRepository gruposEvento;
    private final DeportistaPerteneceGrupoRepository membresias;
    private final DocumentoDeportistaRepository documentos;
    private final LesionRepository lesiones;

    public RosterService(RosterCompetenciaRepository rosters, RosterIntegranteRepository integrantes,
            GrupoEventoRepository gruposEvento, DeportistaPerteneceGrupoRepository membresias,
            DocumentoDeportistaRepository documentos, LesionRepository lesiones) {
        this.rosters = rosters;
        this.integrantes = integrantes;
        this.gruposEvento = gruposEvento;
        this.membresias = membresias;
        this.documentos = documentos;
        this.lesiones = lesiones;
    }

    protected LocalDate hoy() {
        return LocalDate.now();
    }

    protected LocalDateTime ahora() {
        return LocalDateTime.now();
    }

    /** Pares evento-grupo inscritos, con el estado de su roster. */
    public List<Opcion> opciones() {
        Map<String, RosterCompetencia> porPar = new HashMap<>();
        Map<Integer, int[]> conteo = new HashMap<>();
        for (RosterCompetencia r : rosters.findAll()) {
            porPar.put(r.getEventoId() + "|" + r.getGrupoId(), r);
            int[] c = new int[2];
            for (RosterIntegrante i : integrantes.findByRosterIdOrderByRolAscIdAsc(r.getId())) {
                c[i.getRol() == Rol.TITULAR ? 0 : 1]++;
            }
            conteo.put(r.getId(), c);
        }
        List<Opcion> out = new ArrayList<>();
        for (GrupoEvento ge : gruposEvento.findAll()) {
            RosterCompetencia r = porPar.get(ge.getEvento().getId() + "|" + ge.getGrupoEntrenamiento().getId());
            int[] c = r == null ? new int[2] : conteo.get(r.getId());
            out.add(new Opcion(ge.getEvento().getId(), ge.getEvento().getNombre(), ge.getEvento().getFecha(),
                    ge.getGrupoEntrenamiento().getId(), ge.getGrupoEntrenamiento().getNombre(),
                    r == null ? "SIN_ROSTER" : r.getEstado().name(), c[0], c[1]));
        }
        out.sort(Comparator.comparing((Opcion o) -> o.fecha() == null ? LocalDate.MAX : o.fecha()).thenComparing(Opcion::grupo));
        return out;
    }

    public Vista vista(Integer eventoId, Integer grupoId) {
        GrupoEvento par = par(eventoId, grupoId);
        Optional<RosterCompetencia> r = rosters.findByEventoIdAndGrupoId(eventoId, grupoId);
        LocalDate ref = fechaReferencia(par);
        Evaluador ev = new Evaluador(ref);

        List<RosterIntegrante> ints = r.map(x -> integrantes.findByRosterIdOrderByRolAscIdAsc(x.getId())).orElse(List.of());
        Map<Integer, Integrante> ya = new HashMap<>();
        List<Integrante> filas = new ArrayList<>();
        int tit = 0, sup = 0, bloq = 0;
        for (RosterIntegrante i : ints) {
            Deportista d = miembro(grupoId, i.getDeportistaId());
            List<Problema> ps = ev.problemas(i.getDeportistaId());
            Integrante fila = new Integrante(i.getId(), i.getDeportistaId(), d == null ? "(ya no esta en el grupo)" : nombre(d), i.getRol(), i.getPosicion(), ps);
            filas.add(fila);
            ya.put(i.getDeportistaId(), fila);
            if (i.getRol() == Rol.TITULAR) {
                tit++;
                if (hayBloqueo(ps) || d == null) bloq++;
            } else {
                sup++;
            }
        }
        List<Candidato> cands = new ArrayList<>();
        for (DeportistaPerteneceGrupo m : membresias.findByGrupoIdAndFechaFinIsNull(grupoId)) {
            Deportista d = m.getDeportista();
            if (d == null || ya.containsKey(d.getId())) continue;
            cands.add(new Candidato(d.getId(), nombre(d), ev.problemas(d.getId())));
        }
        cands.sort(Comparator.comparing(Candidato::nombre, String.CASE_INSENSITIVE_ORDER));
        Integer cupo = r.map(RosterCompetencia::getCupoMaximo).orElse(null);
        boolean confirmable = tit > 0 && bloq == 0 && (cupo == null || tit <= cupo);
        return new Vista(eventoId, par.getEvento().getNombre(), par.getEvento().getFecha(), grupoId,
                par.getGrupoEntrenamiento().getNombre(), r.map(x -> x.getEstado().name()).orElse("SIN_ROSTER"),
                cupo, tit, sup, bloq, confirmable, filas, cands);
    }

    @Transactional
    public Vista agregar(Integer eventoId, Integer grupoId, Integer deportistaId, Rol rol, String posicion, int usuarioId) {
        GrupoEvento par = par(eventoId, grupoId);
        if (rol == null) throw new IllegalArgumentException("Indica si es titular o suplente");
        Deportista d = miembro(grupoId, deportistaId);
        if (d == null) throw new IllegalArgumentException("El deportista no pertenece a este grupo");
        RosterCompetencia r = rosters.findByEventoIdAndGrupoId(eventoId, grupoId).orElseGet(() -> nuevo(eventoId, grupoId, usuarioId));
        exigirBorrador(r);
        List<RosterIntegrante> ints = integrantes.findByRosterIdOrderByRolAscIdAsc(r.getId());
        if (ints.stream().anyMatch(i -> i.getDeportistaId().equals(deportistaId))) {
            throw new IllegalStateException("Ya esta en el roster: cambia su rol en vez de agregarlo de nuevo");
        }
        if (rol == Rol.TITULAR) {
            validarTitular(r, ints, deportistaId, par);
        }
        RosterIntegrante i = new RosterIntegrante();
        i.setRosterId(r.getId());
        i.setDeportistaId(deportistaId);
        i.setRol(rol);
        i.setPosicion(posicion(posicion));
        i.setFechaAgregado(ahora());
        integrantes.save(i);
        return vista(eventoId, grupoId);
    }

    @Transactional
    public Vista cambiarRol(Integer integranteId, Rol rol, String posicion) {
        RosterIntegrante i = integrantes.findById(integranteId).orElseThrow(() -> new IllegalArgumentException("Integrante no encontrado"));
        RosterCompetencia r = rosters.findById(i.getRosterId()).orElseThrow(() -> new IllegalArgumentException("Roster no encontrado"));
        exigirBorrador(r);
        if (rol != null && rol != i.getRol()) {
            if (rol == Rol.TITULAR) {
                List<RosterIntegrante> ints = integrantes.findByRosterIdOrderByRolAscIdAsc(r.getId());
                validarTitular(r, ints, i.getDeportistaId(), par(r.getEventoId(), r.getGrupoId()));
            }
            i.setRol(rol);
        }
        if (posicion != null) i.setPosicion(posicion(posicion));
        integrantes.save(i);
        return vista(r.getEventoId(), r.getGrupoId());
    }

    @Transactional
    public Vista quitar(Integer integranteId) {
        RosterIntegrante i = integrantes.findById(integranteId).orElseThrow(() -> new IllegalArgumentException("Integrante no encontrado"));
        RosterCompetencia r = rosters.findById(i.getRosterId()).orElseThrow(() -> new IllegalArgumentException("Roster no encontrado"));
        exigirBorrador(r);
        integrantes.deleteById(integranteId);
        return vista(r.getEventoId(), r.getGrupoId());
    }

    @Transactional
    public Vista cupo(Integer eventoId, Integer grupoId, Integer cupo, int usuarioId) {
        par(eventoId, grupoId);
        if (cupo != null && (cupo < 1 || cupo > 100)) throw new IllegalArgumentException("El cupo va de 1 a 100 titulares");
        RosterCompetencia r = rosters.findByEventoIdAndGrupoId(eventoId, grupoId).orElseGet(() -> nuevo(eventoId, grupoId, usuarioId));
        exigirBorrador(r);
        long tit = integrantes.findByRosterIdOrderByRolAscIdAsc(r.getId()).stream().filter(i -> i.getRol() == Rol.TITULAR).count();
        if (cupo != null && tit > cupo) throw new IllegalArgumentException("Ya hay " + tit + " titulares: quita o pasa a suplente antes de bajar el cupo");
        r.setCupoMaximo(cupo);
        rosters.save(r);
        return vista(eventoId, grupoId);
    }

    @Transactional
    public Vista confirmar(Integer eventoId, Integer grupoId) {
        Vista v = vista(eventoId, grupoId);
        RosterCompetencia r = rosters.findByEventoIdAndGrupoId(eventoId, grupoId).orElseThrow(() -> new IllegalArgumentException("Aun no hay roster para confirmar"));
        exigirBorrador(r);
        if (v.titulares() == 0) throw new IllegalArgumentException("Agrega al menos un titular");
        if (v.titularesBloqueados() > 0) throw new IllegalArgumentException("Hay " + v.titularesBloqueados() + " titular(es) con bloqueo (lesion, documento vencido o fuera del grupo): resuelvelos o pasalos a suplente");
        if (r.getCupoMaximo() != null && v.titulares() > r.getCupoMaximo()) throw new IllegalArgumentException("Hay mas titulares que el cupo");
        r.setEstado(Estado.CONFIRMADO);
        r.setFechaConfirmacion(ahora());
        rosters.save(r);
        return vista(eventoId, grupoId);
    }

    @Transactional
    public Vista reabrir(Integer eventoId, Integer grupoId) {
        RosterCompetencia r = rosters.findByEventoIdAndGrupoId(eventoId, grupoId).orElseThrow(() -> new IllegalArgumentException("Aun no hay roster"));
        if (r.getEstado() != Estado.CONFIRMADO) throw new IllegalStateException("El roster ya esta en borrador");
        r.setEstado(Estado.BORRADOR);
        r.setFechaConfirmacion(null);
        rosters.save(r);
        return vista(eventoId, grupoId);
    }

    private void validarTitular(RosterCompetencia r, List<RosterIntegrante> ints, Integer deportistaId, GrupoEvento par) {
        long tit = ints.stream().filter(i -> i.getRol() == Rol.TITULAR).count();
        if (r.getCupoMaximo() != null && tit >= r.getCupoMaximo()) {
            throw new IllegalStateException("El roster ya tiene el cupo de " + r.getCupoMaximo() + " titulares");
        }
        List<Problema> ps = new Evaluador(fechaReferencia(par)).problemas(deportistaId);
        if (hayBloqueo(ps)) {
            String motivos = ps.stream().filter(Problema::bloquea).map(Problema::detalle).reduce((a, b) -> a + "; " + b).orElse("");
            throw new IllegalArgumentException("No puede ser titular: " + motivos + ". Puedes dejarlo como suplente.");
        }
    }

    private RosterCompetencia nuevo(Integer eventoId, Integer grupoId, int usuarioId) {
        RosterCompetencia r = new RosterCompetencia();
        r.setEventoId(eventoId);
        r.setGrupoId(grupoId);
        r.setCreadoPor(usuarioId);
        r.setFechaCreacion(ahora());
        return rosters.save(r);
    }

    private static void exigirBorrador(RosterCompetencia r) {
        if (r.getEstado() == Estado.CONFIRMADO) throw new IllegalStateException("El roster esta confirmado: reabrelo para editarlo");
    }

    private GrupoEvento par(Integer eventoId, Integer grupoId) {
        return gruposEvento.findByEventoId(eventoId).stream()
                .filter(g -> g.getGrupoEntrenamiento() != null && g.getGrupoEntrenamiento().getId().equals(grupoId)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ese grupo no esta inscrito en el evento"));
    }

    private LocalDate fechaReferencia(GrupoEvento par) {
        LocalDate f = par.getEvento().getFecha();
        return f != null ? f : hoy();
    }

    private Deportista miembro(Integer grupoId, Integer deportistaId) {
        return membresias.findByGrupoIdAndFechaFinIsNull(grupoId).stream()
                .map(DeportistaPerteneceGrupo::getDeportista).filter(d -> d != null && d.getId().equals(deportistaId)).findFirst().orElse(null);
    }

    private static boolean hayBloqueo(List<Problema> ps) {
        return ps.stream().anyMatch(Problema::bloquea);
    }

    private static String nombre(Deportista d) {
        return d.getPersona().getNombre() + " " + d.getPersona().getApellidos();
    }

    private static String posicion(String p) {
        if (p == null || p.isBlank()) return null;
        String t = p.trim();
        if (t.length() > 40) throw new IllegalArgumentException("La posicion admite maximo 40 caracteres");
        return t;
    }

    /** Evalua aptitud de un deportista a la fecha del evento con los datos cargados una sola vez. */
    private class Evaluador {
        private final LocalDate ref;
        private final Map<String, DocumentoDeportista> ultimo = new HashMap<>();
        private final Map<Integer, Restriccion> lesion = new HashMap<>();

        Evaluador(LocalDate ref) {
            this.ref = ref;
            for (DocumentoDeportista d : documentos.findAllByOrderByIdDesc()) {
                ultimo.putIfAbsent(d.getDeportistaId() + "|" + d.getTipo(), d);
            }
            for (Lesion l : lesiones.findByEstadoNotOrderByFechaLesionDescIdDesc(Lesion.Estado.ALTA)) {
                lesion.merge(l.getDeportistaId(), l.getRestriccion(), (a, b) -> a == Restriccion.SIN_ENTRENAR || b == Restriccion.SIN_ENTRENAR ? Restriccion.SIN_ENTRENAR : Restriccion.MODIFICADO);
            }
        }

        List<Problema> problemas(Integer depId) {
            List<Problema> ps = new ArrayList<>();
            Restriccion r = lesion.get(depId);
            if (r == Restriccion.SIN_ENTRENAR) ps.add(new Problema("LESION", true, "Lesion vigente: no entrena"));
            else if (r == Restriccion.MODIFICADO) ps.add(new Problema("LESION", false, "Lesion en retorno: entrenamiento modificado"));

            DocumentoDeportista cert = ultimo.get(depId + "|" + Tipo.CERTIFICADO_MEDICO);
            if (cert == null) ps.add(new Problema("CERTIFICADO_MEDICO", true, "Sin certificado medico"));
            else if (vencido(cert)) ps.add(new Problema("CERTIFICADO_MEDICO", true, "Certificado medico vencido (" + cert.getFechaVencimiento() + ")"));

            DocumentoDeportista lic = ultimo.get(depId + "|" + Tipo.LICENCIA);
            if (lic != null && vencido(lic)) ps.add(new Problema("LICENCIA", true, "Licencia de federacion vencida (" + lic.getFechaVencimiento() + ")"));

            DocumentoDeportista eps = ultimo.get(depId + "|" + Tipo.EPS);
            if (eps == null) ps.add(new Problema("EPS", false, "Sin EPS registrada"));
            else if (vencido(eps)) ps.add(new Problema("EPS", false, "EPS vencida (" + eps.getFechaVencimiento() + ")"));

            DocumentoDeportista seg = ultimo.get(depId + "|" + Tipo.SEGURO);
            if (seg != null && vencido(seg)) ps.add(new Problema("SEGURO", false, "Seguro vencido (" + seg.getFechaVencimiento() + ")"));
            return ps;
        }

        private boolean vencido(DocumentoDeportista d) {
            return d.getFechaVencimiento() != null && d.getFechaVencimiento().isBefore(ref);
        }
    }
}
