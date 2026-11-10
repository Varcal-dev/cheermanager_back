package com.varcal.cheermanager.Service.Eventos;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.GrupoEvento;
import com.varcal.cheermanager.Models.Eventos.RosterCompetencia;
import com.varcal.cheermanager.Models.Eventos.RosterIntegrante;
import com.varcal.cheermanager.Models.Eventos.RosterIntegrante.Rol;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.DocumentoDeportista;
import com.varcal.cheermanager.Models.Personas.DocumentoDeportista.Tipo;
import com.varcal.cheermanager.Models.Personas.Lesion;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Eventos.GrupoEventoRepository;
import com.varcal.cheermanager.repository.Eventos.RosterCompetenciaRepository;
import com.varcal.cheermanager.repository.Eventos.RosterIntegranteRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Personas.DocumentoDeportistaRepository;
import com.varcal.cheermanager.repository.Personas.LesionRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C2: roster de competencia con aptitud por lesion y documentos. */
class RosterServiceTest {

    final LocalDate fechaEvento = LocalDate.of(2026, 12, 5);
    final List<RosterCompetencia> rosters = new ArrayList<>();
    final List<RosterIntegrante> ints = new ArrayList<>();
    final List<DeportistaPerteneceGrupo> miembros = new ArrayList<>();
    final List<DocumentoDeportista> docs = new ArrayList<>();
    final List<Lesion> lesiones = new ArrayList<>();
    final List<GrupoEvento> gruposEvento = new ArrayList<>();

    final RosterService service = new RosterService(
            Fakes.proxy(RosterCompetenciaRepository.class, new Repo()
                    .on("save", a -> { RosterCompetencia r = (RosterCompetencia) a[0]; if (r.getId() == null) { r.setId(rosters.size() + 1); rosters.add(r); } return r; })
                    .on("findAll", a -> rosters)
                    .on("findById", a -> rosters.stream().filter(r -> r.getId().equals(a[0])).findFirst())
                    .on("findByEventoIdAndGrupoId", a -> rosters.stream().filter(r -> r.getEventoId().equals(a[0]) && r.getGrupoId().equals(a[1])).findFirst())),
            Fakes.proxy(RosterIntegranteRepository.class, new Repo()
                    .on("save", a -> { RosterIntegrante i = (RosterIntegrante) a[0]; if (i.getId() == null) { i.setId(ints.size() + 100); ints.add(i); } return i; })
                    .on("findById", a -> ints.stream().filter(i -> i.getId().equals(a[0])).findFirst())
                    .on("deleteById", a -> { ints.removeIf(i -> i.getId().equals(a[0])); return null; })
                    .on("findByRosterIdOrderByRolAscIdAsc", a -> ints.stream().filter(i -> i.getRosterId().equals(a[0])).toList())),
            Fakes.proxy(GrupoEventoRepository.class, new Repo()
                    .on("findAll", a -> gruposEvento)
                    .on("findByEventoId", a -> gruposEvento.stream().filter(g -> g.getEvento().getId().equals(a[0])).toList())),
            Fakes.proxy(DeportistaPerteneceGrupoRepository.class, new Repo()
                    .on("findByGrupoIdAndFechaFinIsNull", a -> miembros.stream().filter(m -> m.getGrupo().getId().equals(a[0])).toList())),
            Fakes.proxy(DocumentoDeportistaRepository.class, new Repo().on("findAllByOrderByIdDesc", a -> {
                List<DocumentoDeportista> r = new ArrayList<>(docs); java.util.Collections.reverse(r); return r; })),
            Fakes.proxy(LesionRepository.class, new Repo().on("findByEstadoNotOrderByFechaLesionDescIdDesc", a -> lesiones.stream().filter(l -> l.getEstado() != a[0]).toList()))) {
        @Override protected LocalDate hoy() { return LocalDate.of(2026, 11, 9); }
        @Override protected LocalDateTime ahora() { return LocalDateTime.of(2026, 11, 9, 10, 0); }
    };

    void reiniciar() {
        rosters.clear(); ints.clear(); miembros.clear(); docs.clear(); lesiones.clear(); gruposEvento.clear();
        Evento ev = new Evento(); ev.setId(1); ev.setNombre("Nacional"); ev.setFecha(fechaEvento);
        GrupoEntrenamiento g = new GrupoEntrenamiento(); g.setId(5); g.setNombre("Elite N3");
        GrupoEvento ge = new GrupoEvento(); ge.setEvento(ev); ge.setGrupoEntrenamiento(g);
        gruposEvento.add(ge);
        for (int id = 1; id <= 4; id++) {
            Persona p = new Persona(); p.setId(100 + id); p.setNombre("Dep" + id); p.setApellidos("X");
            Deportista d = new Deportista(); d.setId(id); d.setPersona(p);
            DeportistaPerteneceGrupo m = new DeportistaPerteneceGrupo(); m.setGrupo(g); m.setDeportista(d);
            miembros.add(m);
            doc(id, Tipo.CERTIFICADO_MEDICO, fechaEvento.plusDays(30));
            doc(id, Tipo.EPS, null);
        }
    }

    void doc(int dep, Tipo t, LocalDate vence) {
        DocumentoDeportista d = new DocumentoDeportista(); d.setDeportistaId(dep); d.setTipo(t); d.setFechaVencimiento(vence); docs.add(d);
    }

    void lesion(int dep, Lesion.Restriccion r) {
        Lesion l = new Lesion(); l.setDeportistaId(dep); l.setEstado(Lesion.Estado.ACTIVA); l.setRestriccion(r); lesiones.add(l);
    }

    @Test
    void agregaTitularesYSuplentesDelGrupo() {
        reiniciar();
        service.agregar(1, 5, 1, Rol.TITULAR, "Flyer", 7);
        RosterService.Vista v = service.agregar(1, 5, 2, Rol.SUPLENTE, null, 7);
        assertEquals(1, v.titulares());
        assertEquals(1, v.suplentes());
        assertEquals(2, v.candidatos().size());
        assertEquals("BORRADOR", v.estado());
        assertThrows(IllegalStateException.class, () -> service.agregar(1, 5, 1, Rol.TITULAR, null, 7)); // repetido
        assertThrows(IllegalArgumentException.class, () -> service.agregar(1, 5, 77, Rol.TITULAR, null, 7)); // no es del grupo
        assertThrows(IllegalArgumentException.class, () -> service.agregar(2, 5, 1, Rol.TITULAR, null, 7)); // grupo no inscrito
    }

    @Test
    void lesionSinEntrenarYCertificadoVencidoBloqueanComoTitular() {
        reiniciar();
        lesion(1, Lesion.Restriccion.SIN_ENTRENAR);
        assertThrows(IllegalArgumentException.class, () -> service.agregar(1, 5, 1, Rol.TITULAR, null, 7));
        docs.removeIf(d -> d.getDeportistaId() == 2 && d.getTipo() == Tipo.CERTIFICADO_MEDICO);
        doc(2, Tipo.CERTIFICADO_MEDICO, fechaEvento.minusDays(1)); // vence antes del evento
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.agregar(1, 5, 2, Rol.TITULAR, null, 7));
        assertTrue(e.getMessage().contains("Certificado medico vencido"));
        // como suplente si se permite, mostrando el problema
        RosterService.Vista v = service.agregar(1, 5, 1, Rol.SUPLENTE, null, 7);
        assertTrue(v.integrantes().get(0).problemas().get(0).bloquea());
    }

    @Test
    void lesionEnRetornoYEpsFaltanteSoloAvisan() {
        reiniciar();
        lesion(1, Lesion.Restriccion.MODIFICADO);
        docs.removeIf(d -> d.getDeportistaId() == 1 && d.getTipo() == Tipo.EPS);
        RosterService.Vista v = service.agregar(1, 5, 1, Rol.TITULAR, null, 7);
        assertEquals(1, v.titulares());
        assertEquals(2, v.integrantes().get(0).problemas().size());
        assertTrue(v.integrantes().get(0).problemas().stream().noneMatch(RosterService.Problema::bloquea));
    }

    @Test
    void cupoMaximoLimitaTitulares() {
        reiniciar();
        service.cupo(1, 5, 2, 7);
        service.agregar(1, 5, 1, Rol.TITULAR, null, 7);
        service.agregar(1, 5, 2, Rol.TITULAR, null, 7);
        assertThrows(IllegalStateException.class, () -> service.agregar(1, 5, 3, Rol.TITULAR, null, 7));
        service.agregar(1, 5, 3, Rol.SUPLENTE, null, 7); // suplentes no cuentan
        assertThrows(IllegalStateException.class, () -> service.cambiarRol(ints.get(2).getId(), Rol.TITULAR, null));
        assertThrows(IllegalArgumentException.class, () -> service.cupo(1, 5, 1, 7)); // ya hay 2 titulares
    }

    @Test
    void confirmarReabrirYBloqueoSobreviniente() {
        reiniciar();
        assertThrows(IllegalArgumentException.class, () -> service.confirmar(1, 5)); // sin roster
        service.agregar(1, 5, 1, Rol.TITULAR, null, 7);
        service.agregar(1, 5, 2, Rol.TITULAR, null, 7);
        RosterService.Vista c = service.confirmar(1, 5);
        assertEquals("CONFIRMADO", c.estado());
        assertThrows(IllegalStateException.class, () -> service.agregar(1, 5, 3, Rol.SUPLENTE, null, 7));
        assertThrows(IllegalStateException.class, () -> service.quitar(ints.get(0).getId()));
        lesion(2, Lesion.Restriccion.SIN_ENTRENAR); // se lesiona despues de confirmar
        RosterService.Vista v = service.vista(1, 5);
        assertEquals(1, v.titularesBloqueados());
        assertFalse(v.confirmable());
        assertEquals("BORRADOR", service.reabrir(1, 5).estado());
        assertThrows(IllegalArgumentException.class, () -> service.confirmar(1, 5));
        service.cambiarRol(ints.get(1).getId(), Rol.SUPLENTE, "Reemplazo");
        assertEquals("CONFIRMADO", service.confirmar(1, 5).estado());
    }

    @Test
    void quitarYOpciones() {
        reiniciar();
        service.agregar(1, 5, 1, Rol.TITULAR, null, 7);
        service.agregar(1, 5, 2, Rol.SUPLENTE, null, 7);
        List<RosterService.Opcion> ops = service.opciones();
        assertEquals(1, ops.size());
        assertEquals(1, ops.get(0).titulares());
        assertEquals(1, ops.get(0).suplentes());
        assertEquals("BORRADOR", ops.get(0).estado());
        service.quitar(ints.get(0).getId());
        assertEquals(0, service.vista(1, 5).titulares());
    }
}
