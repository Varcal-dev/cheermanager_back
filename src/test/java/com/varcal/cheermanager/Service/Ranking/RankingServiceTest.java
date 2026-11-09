package com.varcal.cheermanager.Service.Ranking;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.EvaluacionHabilidad;
import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.HabilidadCatalogo;
import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.PremioEvento;
import com.varcal.cheermanager.Models.Eventos.ResultadoCompetencia;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Org_dep.Temporada;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Ranking.RankingService.HistorialDeportista;
import com.varcal.cheermanager.Service.Ranking.RankingService.RankingHabilidades;
import com.varcal.cheermanager.Service.Ranking.RankingService.RankingTemporada;
import com.varcal.cheermanager.repository.Eventos.ResultadoCompetenciaRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.EvaluacionHabilidadRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.TemporadaRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C12: ranking e historial de resultados y ranking de habilidades aprendidas. */
class RankingServiceTest {

    final List<ResultadoCompetencia> res = new ArrayList<>();
    final List<Temporada> temps = new ArrayList<>();
    final List<Deportista> deps = new ArrayList<>();
    final List<DeportistaPerteneceGrupo> membresias = new ArrayList<>();
    final List<EvaluacionHabilidad> evals = new ArrayList<>();

    final RankingService service = new RankingService(
            Fakes.proxy(ResultadoCompetenciaRepository.class, new Repo().on("findAll", a -> res)),
            Fakes.proxy(TemporadaRepository.class, new Repo()
                    .on("findAllByOrderByFechaInicioDesc", a -> temps)
                    .on("findFirstByActivaTrue", a -> temps.stream().filter(t -> Boolean.TRUE.equals(t.getActiva())).findFirst())
                    .on("findById", a -> temps.stream().filter(t -> t.getId().equals(a[0])).findFirst())),
            Fakes.proxy(DeportistaRepository.class, new Repo()
                    .on("findById", a -> deps.stream().filter(d -> d.getId().equals(a[0])).findFirst())),
            Fakes.proxy(DeportistaPerteneceGrupoRepository.class, new Repo().on("findAll", a -> membresias)),
            Fakes.proxy(EvaluacionHabilidadRepository.class, new Repo().on("findAllByOrderByFechaAscIdAsc", a -> evals))) {
        @Override
        protected LocalDate hoy() {
            return LocalDate.of(2026, 11, 8);
        }
    };

    void reiniciar() {
        res.clear(); temps.clear(); deps.clear(); membresias.clear(); evals.clear();
        Temporada t = new Temporada();
        t.setId(1); t.setNombre("2026"); t.setFechaInicio(LocalDate.of(2026, 1, 1)); t.setFechaFin(LocalDate.of(2026, 12, 31)); t.setActiva(true);
        temps.add(t);
    }

    GrupoEntrenamiento grupo(int id, String n) {
        GrupoEntrenamiento g = new GrupoEntrenamiento();
        g.setId(id); g.setNombre(n);
        return g;
    }

    void resultado(int eventoId, String evento, LocalDate fecha, GrupoEntrenamiento g, Integer pos, String puntaje, String premio) {
        Evento e = new Evento();
        e.setId(eventoId); e.setNombre(evento); e.setFecha(fecha);
        ResultadoCompetencia r = new ResultadoCompetencia();
        r.setEvento(e); r.setGrupoEntrenamiento(g); r.setPosicion(pos);
        r.setPuntaje(puntaje == null ? null : new BigDecimal(puntaje));
        if (premio != null) { PremioEvento p = new PremioEvento(); p.setPremio(premio); r.setPremio(p); }
        res.add(r);
    }

    Deportista dep(int id, String nombre) {
        Persona p = new Persona();
        p.setNombre(nombre); p.setApellidos("X");
        Deportista d = new Deportista();
        d.setId(id); d.setPersona(p);
        deps.add(d);
        return d;
    }

    void eval(Deportista d, int habId, String nombreHab, LocalDate fecha, String puntaje, String inicial) {
        HabilidadCatalogo h = new HabilidadCatalogo();
        h.setId(habId); h.setCodigo("H" + habId); h.setNombre(nombreHab); h.setNivel("N2"); h.setCategoria("GIMNASIA");
        EvaluacionHabilidad e = new EvaluacionHabilidad();
        e.setDeportista(d); e.setHabilidad(h); e.setFecha(fecha);
        e.setPuntaje(new BigDecimal(puntaje)); e.setValorInicial(new BigDecimal(inicial));
        evals.add(e);
    }

    @Test
    void rankingDeLaTemporadaOrdenaPorOrosYPodios() {
        reiniciar();
        GrupoEntrenamiento a = grupo(1, "Black Elite"), b = grupo(2, "Wild Prep");
        resultado(10, "Copa 1", LocalDate.of(2026, 3, 1), a, 1, "90.5", "Oro");
        resultado(11, "Copa 2", LocalDate.of(2026, 6, 1), a, 3, "80.0", null);
        resultado(10, "Copa 1", LocalDate.of(2026, 3, 1), b, 2, "85", null);
        resultado(12, "Copa 3", LocalDate.of(2026, 7, 1), b, 1, "92", null);
        resultado(13, "Copa vieja", LocalDate.of(2025, 5, 1), b, 1, "99", null); // otra temporada
        RankingTemporada r = service.rankingTemporada(null);
        assertEquals("2026", r.temporada().nombre());
        assertEquals(4, r.resultados().size());
        assertEquals(2, r.grupos().size());
        var primero = r.grupos().get(0);
        assertEquals("Wild Prep", primero.grupo()); // 1 oro, 2 podios, promedio 88.50 vs Black: 1 oro, 2 podios, 85.25
        assertEquals(1, primero.oros());
        assertEquals(2, primero.podios());
        assertEquals(new BigDecimal("88.50"), primero.promedioPuntaje());
        assertEquals(new BigDecimal("92"), primero.mejorPuntaje());
        assertEquals(1, primero.mejorPosicion());
        assertEquals("Oro", r.resultados().stream().filter(x -> x.premio() != null).findFirst().get().premio());
        assertEquals(LocalDate.of(2026, 7, 1), r.resultados().get(0).fecha()); // mas reciente primero
    }

    @Test
    void sinTemporadasFalla() {
        reiniciar();
        temps.clear();
        assertThrows(IllegalArgumentException.class, () -> service.rankingTemporada(null));
        assertThrows(IllegalArgumentException.class, () -> service.rankingTemporada(99));
    }

    @Test
    void historialDelDeportistaPorSusGrupos() {
        reiniciar();
        GrupoEntrenamiento a = grupo(1, "Black Elite"), b = grupo(2, "Wild Prep");
        Deportista d = dep(5, "Ana");
        DeportistaPerteneceGrupo actual = new DeportistaPerteneceGrupo();
        actual.setDeportista(d); actual.setGrupo(a);
        DeportistaPerteneceGrupo anterior = new DeportistaPerteneceGrupo();
        anterior.setDeportista(d); anterior.setGrupo(b); anterior.setFechaFin(LocalDate.of(2026, 4, 1));
        membresias.add(actual); membresias.add(anterior);
        resultado(10, "Copa 1", LocalDate.of(2026, 3, 1), a, 1, "90", null);
        resultado(10, "Copa 1", LocalDate.of(2026, 3, 1), b, 2, "85", null);   // estaba en Wild Prep ese dia
        resultado(12, "Copa 3", LocalDate.of(2026, 7, 1), b, 1, "92", null);   // ya salio de Wild Prep
        resultado(11, "Copa 2", LocalDate.of(2026, 6, 1), grupo(3, "Otro"), 1, "70", null); // nunca fue de ahi
        HistorialDeportista h = service.historialDeportista(5);
        assertEquals("Ana X", h.nombre());
        assertEquals(2, h.eventos());
        assertEquals(2, h.podios());
        assertThrows(IllegalArgumentException.class, () -> service.historialDeportista(99));
    }

    @Test
    void habilidadesAprendidasPorMesYAnio() {
        reiniciar();
        Deportista ana = dep(1, "Ana"), bea = dep(2, "Bea");
        // Ana: H1 domina en marzo (primera vez >=85%: 3.5/4.0 = 87.5%), H2 en marzo, H3 en octubre
        eval(ana, 1, "Habilidad 1", LocalDate.of(2026, 2, 10), "2.0", "4.0");   // 50%: no
        eval(ana, 1, "Habilidad 1", LocalDate.of(2026, 3, 5), "3.5", "4.0");    // 87.5%: aprendida en marzo
        eval(ana, 1, "Habilidad 1", LocalDate.of(2026, 5, 5), "4.0", "4.0");    // ya la tenia: no cuenta otra vez
        eval(ana, 2, "Habilidad 2", LocalDate.of(2026, 3, 20), "4.0", "4.0");
        eval(ana, 3, "Habilidad 3", LocalDate.of(2026, 10, 2), "1.8", "2.0");   // 90% (lanzamientos)
        // Bea: una en marzo, una en 2025
        eval(bea, 1, "Habilidad 1", LocalDate.of(2026, 3, 8), "3.6", "4.0");
        eval(bea, 4, "Habilidad 4", LocalDate.of(2025, 12, 8), "4.0", "4.0");
        // una que no llega al umbral
        eval(bea, 5, "Habilidad 5", LocalDate.of(2026, 3, 9), "3.3", "4.0");    // 82.5%: no

        RankingHabilidades anio = service.rankingHabilidades(2026, null);
        assertEquals(4, anio.totalClub());
        assertEquals("Ana X", anio.filas().get(0).nombre());
        assertEquals(3, anio.filas().get(0).cantidad());
        assertEquals(1, anio.filas().get(1).cantidad());
        assertEquals(3, anio.porMes()[2]); // marzo: Ana x2 + Bea x1
        assertEquals(1, anio.porMes()[9]); // octubre

        RankingHabilidades marzo = service.rankingHabilidades(2026, 3);
        assertEquals(3, marzo.totalClub());
        assertEquals(2, marzo.filas().get(0).cantidad());
        assertEquals(1, marzo.filas().get(1).cantidad());
        assertEquals(LocalDate.of(2026, 3, 5), marzo.filas().get(0).habilidades().get(0).fecha());

        assertEquals(1, service.rankingHabilidades(2025, null).totalClub());
        assertEquals(0, service.rankingHabilidades(null, null).filas().size() - 2); // sin anio = el actual (2026)
        assertThrows(IllegalArgumentException.class, () -> service.rankingHabilidades(2026, 13));
        assertThrows(IllegalArgumentException.class, () -> service.rankingHabilidades(1800, null));
    }
}
