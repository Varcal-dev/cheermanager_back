package com.varcal.cheermanager.Service.Ranking;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.EvaluacionHabilidad;
import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.ResultadoCompetencia;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Org_dep.Temporada;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Eventos.ResultadoCompetenciaRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.EvaluacionHabilidadRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.TemporadaRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

/** C12: ranking e historial de resultados por temporada y por atleta, y ranking de habilidades aprendidas. Solo lectura. */
@Service
public class RankingService {

    /** Mismo criterio que la evaluacion de habilidades (C1): dominada = puntaje >= 85% del valor inicial. */
    static final BigDecimal UMBRAL_DOMINADA = new BigDecimal("0.85");

    public record ResultadoFila(Integer eventoId, String evento, LocalDate fecha, Integer grupoId, String grupo,
            Integer posicion, BigDecimal puntaje, String premio) {
    }

    public record RankingGrupo(Integer grupoId, String grupo, int eventos, Integer mejorPosicion, int oros, int podios,
            BigDecimal mejorPuntaje, BigDecimal promedioPuntaje) {
    }

    public record Temporada_(Integer id, String nombre, LocalDate desde, LocalDate hasta) {
    }

    public record RankingTemporada(Temporada_ temporada, List<RankingGrupo> grupos, List<ResultadoFila> resultados) {
    }

    public record HistorialDeportista(Integer deportistaId, String nombre, int eventos, int podios,
            List<ResultadoFila> resultados) {
    }

    public record Aprendida(Integer habilidadId, String codigo, String nombre, String nivel, String categoria,
            LocalDate fecha) {
    }

    public record AprendizajeFila(Integer deportistaId, String nombre, int cantidad, List<Aprendida> habilidades) {
    }

    public record RankingHabilidades(int anio, Integer mes, int totalClub, int[] porMes, List<AprendizajeFila> filas) {
    }

    private final ResultadoCompetenciaRepository resultados;
    private final TemporadaRepository temporadas;
    private final DeportistaRepository deportistas;
    private final DeportistaPerteneceGrupoRepository membresias;
    private final EvaluacionHabilidadRepository evaluaciones;

    public RankingService(ResultadoCompetenciaRepository resultados, TemporadaRepository temporadas,
            DeportistaRepository deportistas, DeportistaPerteneceGrupoRepository membresias,
            EvaluacionHabilidadRepository evaluaciones) {
        this.resultados = resultados;
        this.temporadas = temporadas;
        this.deportistas = deportistas;
        this.membresias = membresias;
        this.evaluaciones = evaluaciones;
    }

    protected LocalDate hoy() {
        return com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy();
    }

    public List<Temporada_> listarTemporadas() {
        return temporadas.findAllByOrderByFechaInicioDesc().stream().map(RankingService::temp).toList();
    }

    /** temporadaId nulo = la temporada activa (o la mas reciente si ninguna esta activa). */
    public RankingTemporada rankingTemporada(Integer temporadaId) {
        Temporada t = elegir(temporadaId);
        List<ResultadoFila> filas = new ArrayList<>();
        for (ResultadoCompetencia r : resultados.findAll()) {
            Evento e = r.getEvento();
            if (e == null || e.getFecha() == null || r.getGrupoEntrenamiento() == null
                    || e.getFecha().isBefore(t.getFechaInicio()) || e.getFecha().isAfter(t.getFechaFin())) {
                continue;
            }
            filas.add(fila(r));
        }
        filas.sort(Comparator.comparing(ResultadoFila::fecha).reversed().thenComparing(ResultadoFila::grupo));
        return new RankingTemporada(temp(t), agrupar(filas), filas);
    }

    /**
     * Resultados de los grupos a los que pertenece (o pertenecio) el deportista. Los resultados se registran por
     * grupo, no por atleta, y la pertenencia no guarda fecha de inicio: se cuentan los eventos posteriores o iguales
     * a la fecha en que salio del grupo (o todos, si sigue en el).
     */
    public HistorialDeportista historialDeportista(Integer deportistaId) {
        Deportista d = deportistas.findById(deportistaId)
                .orElseThrow(() -> new IllegalArgumentException("Deportista no encontrado"));
        List<DeportistaPerteneceGrupo> propias = new ArrayList<>();
        for (DeportistaPerteneceGrupo m : membresias.findAll()) {
            if (m.getDeportista() != null && deportistaId.equals(m.getDeportista().getId()) && m.getGrupo() != null) {
                propias.add(m);
            }
        }
        List<ResultadoFila> filas = new ArrayList<>();
        for (ResultadoCompetencia r : resultados.findAll()) {
            Evento e = r.getEvento();
            if (e == null || r.getGrupoEntrenamiento() == null) {
                continue;
            }
            Integer gid = r.getGrupoEntrenamiento().getId();
            boolean estuvo = propias.stream().anyMatch(m -> m.getGrupo().getId().equals(gid)
                    && (m.getFechaFin() == null || e.getFecha() == null || !e.getFecha().isAfter(m.getFechaFin())));
            if (estuvo) {
                filas.add(fila(r));
            }
        }
        filas.sort(Comparator.comparing((ResultadoFila f) -> f.fecha() == null ? LocalDate.MIN : f.fecha()).reversed());
        int podios = (int) filas.stream().filter(f -> f.posicion() != null && f.posicion() <= 3).count();
        return new HistorialDeportista(deportistaId, nombre(d.getPersona()), filas.size(), podios, filas);
    }

    /**
     * Quien aprendio mas habilidades en el periodo. "Aprender" = la primera evaluacion en que el deportista llega a
     * dominada (>= 85% del valor inicial) esa habilidad; cuenta en el mes de esa evaluacion.
     */
    public RankingHabilidades rankingHabilidades(Integer anio, Integer mes) {
        int a = anio == null ? hoy().getYear() : anio;
        if (a < 2000 || a > 2100) {
            throw new IllegalArgumentException("Anio invalido");
        }
        if (mes != null && (mes < 1 || mes > 12)) {
            throw new IllegalArgumentException("El mes va de 1 a 12");
        }
        Set<String> yaVistas = new HashSet<>();
        Map<Integer, List<Aprendida>> porDeportista = new LinkedHashMap<>();
        Map<Integer, String> nombres = new LinkedHashMap<>();
        int[] porMes = new int[12];
        for (EvaluacionHabilidad ev : evaluaciones.findAllByOrderByFechaAscIdAsc()) {
            if (ev.getDeportista() == null || ev.getHabilidad() == null || ev.getValorInicial() == null
                    || ev.getValorInicial().signum() <= 0 || ev.getPuntaje() == null) {
                continue;
            }
            boolean dominada = ev.getPuntaje().compareTo(ev.getValorInicial().multiply(UMBRAL_DOMINADA)) >= 0;
            String clave = ev.getDeportista().getId() + "|" + ev.getHabilidad().getId();
            if (!dominada || !yaVistas.add(clave)) {
                continue; // solo cuenta la primera vez que la domina
            }
            LocalDate f = ev.getFecha();
            if (f.getYear() != a) {
                continue;
            }
            porMes[f.getMonthValue() - 1]++;
            if (mes != null && f.getMonthValue() != mes) {
                continue;
            }
            Integer did = ev.getDeportista().getId();
            nombres.putIfAbsent(did, nombre(ev.getDeportista().getPersona()));
            var h = ev.getHabilidad();
            porDeportista.computeIfAbsent(did, k -> new ArrayList<>())
                    .add(new Aprendida(h.getId(), h.getCodigo(), h.getNombre(), h.getNivel(), h.getCategoria(), f));
        }
        List<AprendizajeFila> filas = new ArrayList<>();
        int total = 0;
        for (var en : porDeportista.entrySet()) {
            filas.add(new AprendizajeFila(en.getKey(), nombres.get(en.getKey()), en.getValue().size(), en.getValue()));
            total += en.getValue().size();
        }
        filas.sort(Comparator.comparingInt(AprendizajeFila::cantidad).reversed()
                .thenComparing(AprendizajeFila::nombre));
        return new RankingHabilidades(a, mes, total, porMes, filas);
    }

    // ---- internos ----

    private Temporada elegir(Integer id) {
        if (id != null) {
            return temporadas.findById(id).orElseThrow(() -> new IllegalArgumentException("Temporada no encontrada"));
        }
        return temporadas.findFirstByActivaTrue().or(() -> temporadas.findAllByOrderByFechaInicioDesc().stream().findFirst())
                .orElseThrow(() -> new IllegalArgumentException("No hay temporadas registradas"));
    }

    static List<RankingGrupo> agrupar(List<ResultadoFila> filas) {
        Map<Integer, List<ResultadoFila>> porGrupo = new LinkedHashMap<>();
        for (ResultadoFila f : filas) {
            porGrupo.computeIfAbsent(f.grupoId(), k -> new ArrayList<>()).add(f);
        }
        List<RankingGrupo> r = new ArrayList<>();
        for (List<ResultadoFila> g : porGrupo.values()) {
            Integer mejor = null;
            int oros = 0, podios = 0;
            BigDecimal max = null, suma = BigDecimal.ZERO;
            int conPuntaje = 0;
            for (ResultadoFila f : g) {
                if (f.posicion() != null) {
                    mejor = mejor == null ? f.posicion() : Math.min(mejor, f.posicion());
                    if (f.posicion() == 1) oros++;
                    if (f.posicion() <= 3) podios++;
                }
                if (f.puntaje() != null) {
                    max = max == null || f.puntaje().compareTo(max) > 0 ? f.puntaje() : max;
                    suma = suma.add(f.puntaje());
                    conPuntaje++;
                }
            }
            BigDecimal prom = conPuntaje == 0 ? null : suma.divide(BigDecimal.valueOf(conPuntaje), 2, RoundingMode.HALF_UP);
            r.add(new RankingGrupo(g.get(0).grupoId(), g.get(0).grupo(), g.size(), mejor, oros, podios, max, prom));
        }
        r.sort(Comparator.comparingInt(RankingGrupo::oros).reversed()
                .thenComparing(Comparator.comparingInt(RankingGrupo::podios).reversed())
                .thenComparing(g -> g.promedioPuntaje() == null ? BigDecimal.ZERO : g.promedioPuntaje(), Comparator.reverseOrder())
                .thenComparing(RankingGrupo::grupo));
        return r;
    }

    private static ResultadoFila fila(ResultadoCompetencia r) {
        Evento e = r.getEvento();
        String premio = r.getPremio() == null ? null
                : (r.getPremio().getPremio() != null ? r.getPremio().getPremio() : r.getPremio().getDescripcion());
        return new ResultadoFila(e.getId(), e.getNombre(), e.getFecha(), r.getGrupoEntrenamiento().getId(),
                r.getGrupoEntrenamiento().getNombre(), r.getPosicion(), r.getPuntaje(), premio);
    }

    private static Temporada_ temp(Temporada t) {
        return new Temporada_(t.getId(), t.getNombre(), t.getFechaInicio(), t.getFechaFin());
    }

    private static String nombre(Persona p) {
        return p == null ? "" : p.getNombre() + " " + p.getApellidos();
    }
}
