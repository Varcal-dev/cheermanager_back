package com.varcal.cheermanager.Service.Persona;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Personas.CertificacionEntrenador;
import com.varcal.cheermanager.Models.Personas.CertificacionEntrenador.Tipo;
import com.varcal.cheermanager.Models.Personas.Entrenador;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Persona.CertificacionEntrenadorService.Alertas;
import com.varcal.cheermanager.Service.Persona.CertificacionEntrenadorService.Estado;
import com.varcal.cheermanager.Service.Persona.CertificacionEntrenadorService.Solicitud;
import com.varcal.cheermanager.repository.Personas.CertificacionEntrenadorRepository;
import com.varcal.cheermanager.repository.Personas.EntrenadorRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C10: certificaciones de entrenadores con vencimiento. */
class CertificacionEntrenadorServiceTest {

    final LocalDate hoy = LocalDate.of(2026, 11, 8);
    final List<CertificacionEntrenador> certs = new ArrayList<>();
    final List<Entrenador> entrenadores = new ArrayList<>();

    final CertificacionEntrenadorService service = new CertificacionEntrenadorService(
            Fakes.proxy(CertificacionEntrenadorRepository.class, new Repo()
                    .on("save", a -> {
                        CertificacionEntrenador c = (CertificacionEntrenador) a[0];
                        c.setId(certs.size() + 1);
                        certs.add(c);
                        return c;
                    })
                    .on("findAllByOrderByIdDesc", a -> {
                        List<CertificacionEntrenador> r = new ArrayList<>(certs);
                        java.util.Collections.reverse(r);
                        return r;
                    })
                    .on("findByEntrenadorIdOrderByIdDesc", a -> {
                        List<CertificacionEntrenador> r = new ArrayList<>();
                        for (CertificacionEntrenador c : certs) if (c.getEntrenadorId().equals(a[0])) r.add(0, c);
                        return r;
                    })),
            Fakes.proxy(EntrenadorRepository.class, new Repo()
                    .on("findAll", a -> entrenadores)
                    .on("findById", a -> entrenadores.stream().filter(e -> e.getId().equals(a[0])).findFirst()))) {
        @Override
        protected LocalDate hoy() {
            return hoy;
        }
    };

    void entrenador(int id, String nombre, String estado) {
        Persona p = new Persona();
        p.setNombre(nombre);
        p.setApellidos("Z");
        EstadoPersona e = new EstadoPersona();
        e.setEstado(estado);
        Entrenador t = new Entrenador();
        t.setId(id);
        t.setPersona(p);
        t.setEstado(e);
        entrenadores.add(t);
    }

    void reiniciar() {
        certs.clear();
        entrenadores.clear();
        entrenador(1, "Leo", "Activo");
        entrenador(2, "Mia", "Activo");
        entrenador(3, "Out", "Inactivo");
    }

    @Test
    void sinRegistroSoloActivosYRequeridas() {
        reiniciar();
        Alertas a = service.alertas(30, false);
        assertEquals(4, a.sinRegistro()); // 2 activos x (primeros auxilios + seguridad)
        assertTrue(a.items().stream().noneMatch(i -> i.entrenadorId() == 3));
    }

    @Test
    void clasificaYElUltimoRegistroVale() {
        reiniciar();
        service.registrar(1, new Solicitud(Tipo.PRIMEROS_AUXILIOS, null, "Cruz Roja", null, hoy.minusDays(3)), 9);
        service.registrar(1, new Solicitud(Tipo.PRIMEROS_AUXILIOS, null, "Cruz Roja", null, hoy.plusDays(365)), 9); // renovada
        service.registrar(1, new Solicitud(Tipo.SEGURIDAD_DEPORTIVA, null, null, null, hoy.plusDays(10)), 9);
        service.registrar(2, new Solicitud(Tipo.PRIMEROS_AUXILIOS, null, null, null, hoy.minusDays(1)), 9);
        service.registrar(2, new Solicitud(Tipo.SEGURIDAD_DEPORTIVA, null, null, null, hoy.plusDays(200)), 9);
        Alertas a = service.alertas(30, false);
        assertEquals(1, a.vencidas());
        assertEquals(1, a.porVencer());
        assertEquals(0, a.sinRegistro());
        assertEquals(Estado.VENCIDA, a.items().get(0).estado());
        assertEquals(4, service.alertas(30, true).items().size());
        assertEquals(2, service.historial(1).stream().filter(c -> c.tipo() == Tipo.PRIMEROS_AUXILIOS).count());
    }

    @Test
    void cursosDeFederacionSeDistinguenPorNombre() {
        reiniciar();
        service.registrar(1, new Solicitud(Tipo.CURSO_FEDERACION, "Juez nivel 1", "Federacion", null, hoy.minusDays(1)), 9);
        service.registrar(1, new Solicitud(Tipo.CURSO_FEDERACION, "Stunt avanzado", "Federacion", null, null), 9); // sin vencimiento
        Alertas a = service.alertas(30, false);
        assertEquals(1, a.vencidas()); // solo el curso vencido; el otro no vence
        assertTrue(a.items().stream().anyMatch(i -> "Juez nivel 1".equals(i.certificacion())));
    }

    @Test
    void validaRegistro() {
        reiniciar();
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1, new Solicitud(null, null, null, null, hoy), 9));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(99, new Solicitud(Tipo.RCP, null, null, null, hoy), 9));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1, new Solicitud(Tipo.RCP, null, null, null, null), 9));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1, new Solicitud(Tipo.OTRA, null, null, null, hoy), 9)); // sin nombre
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1, new Solicitud(Tipo.RCP, null, null, hoy.plusDays(1), hoy.plusDays(5)), 9));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(1, new Solicitud(Tipo.RCP, null, null, hoy, hoy.minusDays(1)), 9));
        assertThrows(IllegalArgumentException.class, () -> service.alertas(999, false));
        assertTrue(certs.isEmpty());
    }
}
