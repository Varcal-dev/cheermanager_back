package com.varcal.cheermanager.Service.Legal;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Legal.SolicitudTitular;
import com.varcal.cheermanager.Models.Legal.SolicitudTitular.Estado;
import com.varcal.cheermanager.Models.Legal.SolicitudTitular.Tipo;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Legal.SolicitudTitularService.Solicitud;
import com.varcal.cheermanager.Service.Legal.SolicitudTitularService.Vista;
import com.varcal.cheermanager.repository.Legal.SolicitudTitularRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** L8: plazos de la Ley 1581 en dias habiles colombianos y ciclo de vida de una solicitud. */
class SolicitudTitularServiceTest {

    final List<SolicitudTitular> guardadas = new ArrayList<>();
    LocalDate hoy = LocalDate.of(2026, 10, 29);

    final SolicitudTitularService service = new SolicitudTitularService(
            Fakes.proxy(SolicitudTitularRepository.class, new Repo()
                    .on("save", a -> {
                        SolicitudTitular s = (SolicitudTitular) a[0];
                        if (s.getId() == null) {
                            s.setId(guardadas.size() + 1);
                            guardadas.add(s);
                        }
                        return s;
                    })
                    .on("findById", a -> guardadas.stream().filter(s -> s.getId().equals(a[0])).findFirst())
                    .on("findAllByOrderByFechaRecepcionDescIdDesc", a -> guardadas)),
            Fakes.proxy(PersonaRepository.class, new Repo()
                    .on("findById", a -> a[0].equals(5) ? Optional.of(new Persona()) : Optional.empty()))) {
        @Override
        protected LocalDate hoy() {
            return hoy;
        }
    };

    void reiniciar() {
        guardadas.clear();
        hoy = LocalDate.of(2026, 10, 29);
    }

    Solicitud consulta() {
        return new Solicitud(Tipo.CONSULTA, "Ana Perez", "123", "ana@x.co", 5, "correo", "Quiero saber que datos tienen de mi hija", null);
    }

    @Test
    void festivosDeColombia2026() {
        reiniciar();
        var f = DiasHabiles.festivos(2026);
        assertTrue(f.contains(LocalDate.of(2026, 4, 2)));  // jueves santo (Pascua 5-abr)
        assertTrue(f.contains(LocalDate.of(2026, 4, 3)));  // viernes santo
        assertTrue(f.contains(LocalDate.of(2026, 5, 18))); // Ascension
        assertTrue(f.contains(LocalDate.of(2026, 6, 8)));  // Corpus Christi
        assertTrue(f.contains(LocalDate.of(2026, 6, 15))); // Sagrado Corazon
        assertTrue(f.contains(LocalDate.of(2026, 11, 2))); // Todos los Santos (1-nov domingo)
        assertTrue(f.contains(LocalDate.of(2026, 11, 16))); // Independencia de Cartagena
        assertTrue(f.contains(LocalDate.of(2026, 12, 8)));
        assertFalse(f.contains(LocalDate.of(2026, 11, 11)));
    }

    @Test
    void consultaVenceALos10DiasHabiles() {
        reiniciar();
        Vista v = service.registrar(consulta(), 7);
        assertEquals(LocalDate.of(2026, 11, 13), v.fechaLimite());
        assertEquals("CORREO", v.canal());
        assertEquals(10, v.diasHabilesRestantes());
        assertFalse(v.vencida());
    }

    @Test
    void reclamoVenceALos15DiasHabiles() {
        reiniciar();
        Solicitud s = new Solicitud(Tipo.SUPRESION, "Ana Perez", null, null, null, "presencial", "Borren los datos de mi hija", null);
        assertEquals(LocalDate.of(2026, 11, 23), service.registrar(s, 7).fechaLimite());
    }

    @Test
    void validaDatosDeEntrada() {
        reiniciar();
        assertThrows(IllegalArgumentException.class, () -> service.registrar(
                new Solicitud(null, "Ana", null, null, null, "CORREO", "texto largo", null), 1));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(
                new Solicitud(Tipo.CONSULTA, "An", null, null, null, "CORREO", "texto largo", null), 1));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(
                new Solicitud(Tipo.CONSULTA, "Ana Perez", null, null, null, "FAX", "texto largo", null), 1));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(
                new Solicitud(Tipo.CONSULTA, "Ana Perez", null, null, 99, "CORREO", "texto largo", null), 1));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(
                new Solicitud(Tipo.CONSULTA, "Ana Perez", null, null, null, "CORREO", "texto largo", hoy.plusDays(1)), 1));
        assertTrue(guardadas.isEmpty());
    }

    @Test
    void unaSolaProrrogaAntesDeVencer() {
        reiniciar();
        Vista v = service.registrar(consulta(), 7);
        Vista p = service.prorrogar(v.id(), "Se requiere consultar al contador");
        assertTrue(p.prorrogada());
        assertEquals(LocalDate.of(2026, 11, 23), p.fechaLimite()); // +5 habiles desde 13-nov
        assertThrows(IllegalStateException.class, () -> service.prorrogar(v.id(), "otra vez mas"));
    }

    @Test
    void noSeProrrogaVencidaNiRespondida() {
        reiniciar();
        Vista v = service.registrar(consulta(), 7);
        hoy = LocalDate.of(2026, 11, 17);
        assertThrows(IllegalStateException.class, () -> service.prorrogar(v.id(), "ya es tarde"));
        assertTrue(service.listar(null).get(0).vencida());
        assertTrue(service.listar(null).get(0).diasHabilesRestantes() < 0);
        Vista r = service.responder(v.id(), "Se envio el reporte de datos", 8);
        assertEquals(Estado.RESPONDIDA, r.estado());
        assertEquals(Boolean.FALSE, r.respondidaEnPlazo());
        assertFalse(r.vencida());
        assertThrows(IllegalStateException.class, () -> service.prorrogar(v.id(), "respondida ya"));
        assertThrows(IllegalStateException.class, () -> service.responder(v.id(), "otra respuesta", 8));
    }

    @Test
    void cicloEnTramiteYRespuestaEnPlazo() {
        reiniciar();
        Vista v = service.registrar(consulta(), 7);
        assertEquals(Estado.EN_TRAMITE, service.ponerEnTramite(v.id()).estado());
        assertThrows(IllegalStateException.class, () -> service.ponerEnTramite(v.id()));
        assertThrows(IllegalArgumentException.class, () -> service.responder(v.id(), "no", 8));
        Vista r = service.responder(v.id(), "Se entrego copia de los datos", 8);
        assertEquals(Boolean.TRUE, r.respondidaEnPlazo());
        assertEquals(1, service.listar(Estado.RESPONDIDA).size());
        assertEquals(0, service.listar(Estado.RECIBIDA).size());
    }
}
