package com.varcal.cheermanager.Service.Perfil;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.ResultadoCompetencia;
import com.varcal.cheermanager.Models.Eventos.RosterCompetencia;
import com.varcal.cheermanager.Models.Eventos.RosterIntegrante;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Lesion.Estado;
import com.varcal.cheermanager.Models.Personas.Lesion.Gravedad;
import com.varcal.cheermanager.Models.Personas.Lesion.Restriccion;
import com.varcal.cheermanager.Models.Personas.Lesion.Zona;
import com.varcal.cheermanager.Service.Eventos.RosterService.Problema;
import com.varcal.cheermanager.Service.Perfil.PerfilCompetenciaSaludService.DatosCompetencia;
import com.varcal.cheermanager.Service.Perfil.PerfilCompetenciaSaludService.DatosSalud;
import com.varcal.cheermanager.Service.Persona.LesionService;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Eventos.ResultadoCompetenciaRepository;
import com.varcal.cheermanager.repository.Eventos.RosterCompetenciaRepository;
import com.varcal.cheermanager.repository.Eventos.RosterIntegranteRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** Perfil del deportista, segunda entrega: competencia (roster y resultados) y salud (estado y diagnostico). */
class PerfilCompetenciaSaludServiceTest {

    static final LocalDate HOY = LocalDate.of(2026, 11, 16);

    final List<RosterIntegrante> ints = new ArrayList<>();
    final List<RosterCompetencia> rosters = new ArrayList<>();
    final List<Evento> eventos = new ArrayList<>();
    final List<ResultadoCompetencia> resultados = new ArrayList<>();
    final List<LesionService.Item> lesiones = new ArrayList<>();
    List<Problema> problemas = List.of();
    Restriccion restriccion = Restriccion.SIN_RESTRICCION;

    final PerfilCompetenciaSaludService service = new PerfilCompetenciaSaludService(
            Fakes.proxy(DeportistaRepository.class,
                    new Repo().on("findById", a -> Optional.ofNullable(((Integer) a[0]) == 7 ? new Deportista() : null))),
            Fakes.proxy(RosterIntegranteRepository.class, new Repo().on("findByDeportistaId", a -> ints)),
            Fakes.proxy(RosterCompetenciaRepository.class, new Repo().on("findById",
                    a -> rosters.stream().filter(r -> r.getId().equals(a[0])).findFirst())),
            Fakes.proxy(EventoRepository.class, new Repo().on("findById",
                    a -> eventos.stream().filter(e -> e.getId().equals(a[0])).findFirst())),
            Fakes.proxy(GrupoEntrenamientoRepository.class, new Repo().on("findById", a -> {
                GrupoEntrenamiento g = new GrupoEntrenamiento();
                g.setId((Integer) a[0]);
                g.setNombre("Senior N3");
                return Optional.of(g);
            })),
            Fakes.proxy(ResultadoCompetenciaRepository.class, new Repo().on("findByEventoIdAndGrupoEntrenamientoId",
                    a -> resultados.stream().filter(r -> r.getEvento().getId().equals(a[0])).findFirst())),
            null, null) {
        @Override
        protected LocalDate hoy() {
            return HOY;
        }

        @Override
        protected List<Problema> problemasDe(Integer deportistaId, LocalDate fecha) {
            return problemas;
        }

        @Override
        protected List<LesionService.Item> lesionesDe(Integer deportistaId) {
            return lesiones;
        }

        @Override
        protected Restriccion restriccionDe(Integer deportistaId) {
            return restriccion;
        }

        @Override
        protected List<LesionService.Nota> seguimientosDe(Integer lesionId) {
            return List.of(new LesionService.Nota(1, HOY.minusDays(2), "Reposo y hielo", Estado.ACTIVA, Restriccion.SIN_ENTRENAR));
        }
    };

    // ---- ayudantes ----

    /** Evento + roster + integrante. fecha relativa a HOY en dias. */
    void participacion(int id, int dias, RosterIntegrante.Rol rol, RosterCompetencia.Estado estadoRoster, Integer posicionGrupo) {
        Evento e = new Evento();
        e.setId(id);
        e.setNombre("Evento " + id);
        e.setFecha(HOY.plusDays(dias));
        eventos.add(e);
        RosterCompetencia r = new RosterCompetencia();
        r.setId(id);
        r.setEventoId(id);
        r.setGrupoId(3);
        r.setEstado(estadoRoster);
        rosters.add(r);
        RosterIntegrante i = new RosterIntegrante();
        i.setRosterId(id);
        i.setDeportistaId(7);
        i.setRol(rol);
        ints.add(i);
        if (posicionGrupo != null) {
            ResultadoCompetencia res = new ResultadoCompetencia();
            res.setEvento(e);
            res.setPosicion(posicionGrupo);
            res.setPuntaje(new BigDecimal("85.5"));
            resultados.add(res);
        }
    }

    LesionService.Item lesion(int id, Estado estado, Restriccion restr) {
        return new LesionService.Item(id, 7, "Ana Pérez", HOY.minusDays(10), Zona.RODILLA, "Esguince de rodilla",
                Gravedad.MODERADA, restr, estado, HOY.plusDays(14), estado == Estado.ALTA ? HOY.minusDays(1) : null,
                false, null, 10, false);
    }

    // ---- competencia ----

    @Test
    void competencia_cuenta_solo_eventos_pasados_como_titular_en_roster_confirmado() {
        participacion(1, -60, RosterIntegrante.Rol.TITULAR, RosterCompetencia.Estado.CONFIRMADO, 2); // podio
        participacion(2, -30, RosterIntegrante.Rol.TITULAR, RosterCompetencia.Estado.CONFIRMADO, 5); // sin podio
        participacion(3, -20, RosterIntegrante.Rol.SUPLENTE, RosterCompetencia.Estado.CONFIRMADO, 1); // suplente: no cuenta
        participacion(4, -10, RosterIntegrante.Rol.TITULAR, RosterCompetencia.Estado.BORRADOR, 1); // borrador: no cuenta
        DatosCompetencia c = service.competencia(7);
        assertEquals(2, c.eventos());
        assertEquals(1, c.podios());
        assertEquals(4, c.participaciones().size());
        assertEquals(4, c.participaciones().get(0).eventoId(), "la mas reciente primero");
        assertEquals("Senior N3", c.participaciones().get(0).grupo());
        assertNull(c.proximo());
    }

    @Test
    void competencia_elige_el_proximo_evento_mas_cercano_y_dice_si_esta_apto() {
        participacion(1, 40, RosterIntegrante.Rol.TITULAR, RosterCompetencia.Estado.BORRADOR, null);
        participacion(2, 12, RosterIntegrante.Rol.SUPLENTE, RosterCompetencia.Estado.CONFIRMADO, null);
        problemas = List.of(new Problema("CERTIFICADO_MEDICO", true, "Sin certificado medico"),
                new Problema("EPS", false, "Sin EPS registrada"));
        DatosCompetencia c = service.competencia(7);
        assertEquals(2, c.proximo().eventoId());
        assertEquals("SUPLENTE", c.proximo().rol());
        assertFalse(c.proximo().apto(), "un problema que bloquea la deja no apta");
        assertEquals(2, c.proximo().problemas().size());
        assertTrue(c.participaciones().isEmpty(), "los eventos futuros no van al historial");

        problemas = List.of(new Problema("EPS", false, "Sin EPS registrada"));
        assertTrue(service.competencia(7).proximo().apto(), "un aviso que no bloquea sigue siendo apta");
    }

    @Test
    void competencia_sin_roster_devuelve_todo_en_cero() {
        DatosCompetencia c = service.competencia(7);
        assertEquals(0, c.eventos());
        assertEquals(0, c.podios());
        assertNull(c.proximo());
        assertTrue(c.participaciones().isEmpty());
    }

    // ---- salud ----

    @Test
    void salud_para_la_familia_muestra_estado_y_diagnostico_sin_seguimientos_ni_historial() {
        restriccion = Restriccion.SIN_ENTRENAR;
        lesiones.add(lesion(1, Estado.ACTIVA, Restriccion.SIN_ENTRENAR));
        lesiones.add(lesion(2, Estado.ALTA, Restriccion.SIN_RESTRICCION));
        DatosSalud s = service.salud(7, false);
        assertEquals("SIN_ENTRENAR", s.restriccion());
        assertEquals(1, s.vigentes().size());
        assertEquals("Esguince de rodilla", s.vigentes().get(0).descripcion());
        assertEquals("RODILLA", s.vigentes().get(0).zona());
        assertNull(s.vigentes().get(0).seguimientos(), "la familia no ve los seguimientos clinicos");
        assertNull(s.historial(), "ni las lesiones ya dadas de alta");
    }

    @Test
    void salud_para_el_club_incluye_seguimientos_e_historial() {
        restriccion = Restriccion.MODIFICADO;
        lesiones.add(lesion(1, Estado.EN_RETORNO, Restriccion.MODIFICADO));
        lesiones.add(lesion(2, Estado.ALTA, Restriccion.SIN_RESTRICCION));
        DatosSalud s = service.salud(7, true);
        assertEquals("MODIFICADO", s.restriccion());
        assertEquals(1, s.vigentes().get(0).seguimientos().size());
        assertEquals("Reposo y hielo", s.vigentes().get(0).seguimientos().get(0).nota());
        assertEquals(1, s.historial().size());
        assertEquals(2, s.historial().get(0).id());
    }

    @Test
    void salud_sin_lesiones_es_sin_restriccion() {
        DatosSalud s = service.salud(7, false);
        assertEquals("SIN_RESTRICCION", s.restriccion());
        assertTrue(s.vigentes().isEmpty());
    }

    @Test
    void deportista_inexistente_falla_en_las_dos_secciones() {
        assertThrows(IllegalArgumentException.class, () -> service.competencia(99));
        assertThrows(IllegalArgumentException.class, () -> service.salud(99, true));
    }
}
