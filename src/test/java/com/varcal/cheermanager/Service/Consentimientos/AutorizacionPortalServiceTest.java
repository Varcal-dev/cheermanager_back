package com.varcal.cheermanager.Service.Consentimientos;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Consentimientos.Consentimiento.Tipo;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Personas.AcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** L3: firma desde el portal. Solo el acudiente vinculado firma por un menor; tratamiento obligatorio; sin duplicar. */
class AutorizacionPortalServiceTest {

    static class ConsentFalso extends ConsentimientoService {
        final Set<String> vigentes = new HashSet<>();
        final List<OtorgarRequest> otorgados = new ArrayList<>();

        ConsentFalso() {
            super(null, null, null, null, null, false);
        }

        @Override
        public boolean tieneVigente(Integer personaId, Tipo tipo) { return vigentes.contains(personaId + ":" + tipo); }

        @Override
        public Resumen otorgar(OtorgarRequest req) {
            otorgados.add(req);
            vigentes.add(req.personaId() + ":" + req.tipo());
            return null;
        }
    }

    static Persona persona(int id, LocalDate nac) {
        Persona p = new Persona();
        p.setId(id);
        p.setNombre("N" + id);
        p.setApellidos("A");
        p.setFechaNacimiento(nac);
        return p;
    }

    static Deportista deportista(int id, Persona p) {
        Deportista d = new Deportista();
        d.setId(id);
        d.setPersona(p);
        return d;
    }

    // Usuario 10 = acudiente (id 5) de la menor (deportista 1, persona 100). Usuario 20 = adulta con su propia cuenta.
    Persona menor;
    Persona adulta;
    ConsentFalso consent;

    AutorizacionPortalService servicio() {
        menor = persona(100, com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(10));
        adulta = persona(200, com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(25));
        consent = new ConsentFalso();
        Acudiente ac = new Acudiente();
        ac.setId(5);
        Usuario uAcudiente = new Usuario();
        uAcudiente.setId(10);
        Usuario uAdulta = new Usuario();
        uAdulta.setId(20);
        uAdulta.setPersona(adulta);
        Usuario uOtro = new Usuario();
        uOtro.setId(30);
        Deportista dMenor = deportista(1, menor), dAdulta = deportista(2, adulta);

        Repo usuarios = new Repo().on("findById", a -> {
            long id = (Long) a[0];
            return Optional.ofNullable(id == 10 ? uAcudiente : id == 20 ? uAdulta : id == 30 ? uOtro : null);
        });
        Repo acudientes = new Repo().on("findByUsuarioId", a -> Optional.ofNullable(((Integer) a[0]) == 10 ? ac : null));
        Repo vinculos = new Repo()
                .on("deportistasDeAcudiente", a -> List.of(dMenor))
                .on("acudienteDePersona", a -> ((Integer) a[0]) == 5 && ((Integer) a[1]) == 100);
        Repo deportistas = new Repo().on("findByPersonaId", a -> Optional.ofNullable(((Integer) a[0]) == 200 ? dAdulta : null));
        return new AutorizacionPortalService(Fakes.proxy(UserRepository.class, usuarios),
                Fakes.proxy(DeportistaRepository.class, deportistas), Fakes.proxy(AcudienteRepository.class, acudientes),
                Fakes.proxy(DeportistaAcudienteRepository.class, vinculos), consent);
    }

    @Test
    void acudiente_ve_a_su_menor_y_puede_firmar() {
        AutorizacionPortalService s = servicio();
        List<AutorizacionPortalService.Item> items = s.mias(10);
        assertEquals(1, items.size());
        assertTrue(items.get(0).menor());
        assertTrue(items.get(0).puedeFirmar());
        assertEquals(3, items.get(0).tipos().size());
    }

    @Test
    void firma_las_tres_casillas_como_acudiente_digital_con_ip() {
        AutorizacionPortalService s = servicio();
        s.firmar(10, 1, new AutorizacionPortalService.Firma(true, true, true), "190.1.1.1");
        assertEquals(3, consent.otorgados.size());
        for (ConsentimientoService.OtorgarRequest r : consent.otorgados) {
            assertEquals("DIGITAL", r.canal());
            assertEquals(5, r.acudienteId());
            assertEquals(100, r.personaId());
            assertEquals("2026-11", r.versionTexto());
            assertTrue(r.observaciones().contains("190.1.1.1"));
        }
    }

    @Test
    void tratamiento_es_obligatorio_si_no_existia() {
        AutorizacionPortalService s = servicio();
        assertThrows(IllegalArgumentException.class,
                () -> s.firmar(10, 1, new AutorizacionPortalService.Firma(false, true, true), "1.1.1.1"));
        assertTrue(consent.otorgados.isEmpty());
    }

    @Test
    void salud_e_imagen_son_opcionales() {
        AutorizacionPortalService s = servicio();
        s.firmar(10, 1, new AutorizacionPortalService.Firma(true, false, false), null);
        assertEquals(1, consent.otorgados.size());
        assertEquals(Tipo.TRATAMIENTO_DATOS, consent.otorgados.get(0).tipo());
    }

    @Test
    void firmar_de_nuevo_no_duplica_lo_vigente_y_permite_completar() {
        AutorizacionPortalService s = servicio();
        s.firmar(10, 1, new AutorizacionPortalService.Firma(true, false, false), null);
        s.firmar(10, 1, new AutorizacionPortalService.Firma(false, true, false), null);
        s.firmar(10, 1, new AutorizacionPortalService.Firma(true, true, false), null);
        assertEquals(2, consent.otorgados.size());
    }

    @Test
    void usuario_ajeno_no_puede_firmar_por_un_deportista_que_no_es_suyo() {
        AutorizacionPortalService s = servicio();
        assertThrows(IllegalArgumentException.class,
                () -> s.firmar(30, 1, new AutorizacionPortalService.Firma(true, true, true), null));
        assertTrue(consent.otorgados.isEmpty());
    }

    @Test
    void adulta_firma_ella_misma_sin_acudiente() {
        AutorizacionPortalService s = servicio();
        s.firmar(20, 2, new AutorizacionPortalService.Firma(true, true, false), null);
        assertEquals(2, consent.otorgados.size());
        assertNull(consent.otorgados.get(0).acudienteId());
    }

    @Test
    void menor_con_su_propia_cuenta_no_puede_firmar() {
        AutorizacionPortalService s = servicio();
        // Usuario 20 no es acudiente de la menor ni es ella: no la ve. Y la menor propia (sin acudiente) no firma.
        Persona menorPropia = persona(300, com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(12));
        Usuario u = new Usuario();
        u.setId(40);
        u.setPersona(menorPropia);
        Repo usuarios = new Repo().on("findById", a -> Optional.of(u));
        Repo acudientes = new Repo().on("findByUsuarioId", a -> Optional.empty());
        Repo deportistas = new Repo().on("findByPersonaId", a -> Optional.of(deportista(3, menorPropia)));
        AutorizacionPortalService s2 = new AutorizacionPortalService(Fakes.proxy(UserRepository.class, usuarios),
                Fakes.proxy(DeportistaRepository.class, deportistas), Fakes.proxy(AcudienteRepository.class, acudientes),
                Fakes.proxy(DeportistaAcudienteRepository.class, new Repo()), consent);
        assertFalse(s2.mias(40).get(0).puedeFirmar());
        assertThrows(IllegalArgumentException.class,
                () -> s2.firmar(40, 3, new AutorizacionPortalService.Firma(true, true, true), null));
        assertTrue(consent.otorgados.isEmpty());
    }

    @Test
    void sin_fecha_de_nacimiento_no_se_puede_firmar() {
        AutorizacionPortalService s = servicio();
        menor.setFechaNacimiento(null);
        assertFalse(s.mias(10).get(0).puedeFirmar());
        assertThrows(IllegalArgumentException.class,
                () -> s.firmar(10, 1, new AutorizacionPortalService.Firma(true, true, true), null));
    }
}
