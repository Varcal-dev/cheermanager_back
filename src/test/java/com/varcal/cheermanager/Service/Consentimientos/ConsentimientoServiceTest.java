package com.varcal.cheermanager.Service.Consentimientos;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.varcal.cheermanager.Models.Consentimientos.Consentimiento;
import com.varcal.cheermanager.Models.Consentimientos.Consentimiento.Tipo;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Consentimientos.ConsentimientoService.OtorgarRequest;
import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.repository.Consentimientos.ConsentimientoRepository;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.repository.Personas.AcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;

class ConsentimientoServiceTest {

    ConsentimientoRepository repo = mock(ConsentimientoRepository.class);
    PersonaRepository personas = mock(PersonaRepository.class);
    AcudienteRepository acudientes = mock(AcudienteRepository.class);
    DeportistaAcudienteRepository vinculos = mock(DeportistaAcudienteRepository.class);
    ClubRepository clubes = mock(ClubRepository.class);
    ConsentimientoService service;

    @BeforeEach
    void setUp() {
        service = new ConsentimientoService(repo, personas, acudientes, vinculos, clubes, false);
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("u", null, List.of());
        auth.setDetails(Map.of("userId", 7));
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(repo.save(any(Consentimiento.class))).thenAnswer(i -> i.getArgument(0));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private Persona persona(LocalDate nacimiento) {
        Persona p = new Persona();
        p.setId(10);
        p.setFechaNacimiento(nacimiento);
        when(personas.findById(10)).thenReturn(Optional.of(p));
        return p;
    }

    private OtorgarRequest req(Integer acudienteId) {
        return new OtorgarRequest(10, Tipo.DATOS_SALUD, "2026-01", "papel", acudienteId, null);
    }

    @Test
    void menorSinAcudienteSeRechaza() {
        persona(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(12));
        assertThrows(RuntimeException.class, () -> service.otorgar(req(null)));
        verify(repo, never()).save(any());
    }

    @Test
    void menorConAcudienteNoVinculadoSeRechaza() {
        persona(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(12));
        Acudiente a = new Acudiente();
        a.setId(5);
        when(acudientes.findById(5)).thenReturn(Optional.of(a));
        when(vinculos.acudienteDePersona(5, 10)).thenReturn(false);
        assertThrows(RuntimeException.class, () -> service.otorgar(req(5)));
    }

    @Test
    void menorConAcudienteVinculadoSeAcepta() {
        persona(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(12));
        Acudiente a = new Acudiente();
        a.setId(5);
        when(acudientes.findById(5)).thenReturn(Optional.of(a));
        when(vinculos.acudienteDePersona(5, 10)).thenReturn(true);
        var r = service.otorgar(req(5));
        assertTrue(r.vigente());
        assertEquals("PAPEL", r.canal());
        assertEquals(5, r.acudienteId());
    }

    @Test
    void mayorDeEdadAutorizaElMismoSinAcudiente() {
        persona(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(30));
        assertNull(service.otorgar(req(null)).acudienteId());
    }

    @Test
    void sinFechaDeNacimientoNoSeAdivina() {
        persona(null);
        assertThrows(RuntimeException.class, () -> service.otorgar(req(null)));
    }

    @Test
    void noSeDuplicaUnConsentimientoVigente() {
        persona(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(30));
        when(repo.existsByTitularIdAndTipoAndFechaRevocadoIsNull(10, Tipo.DATOS_SALUD)).thenReturn(true);
        assertThrows(RuntimeException.class, () -> service.otorgar(req(null)));
    }

    @Test
    void canalInvalidoSeRechaza() {
        persona(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(30));
        assertThrows(RuntimeException.class,
                () -> service.otorgar(new OtorgarRequest(10, Tipo.DATOS_SALUD, "2026-01", "telepatia", null, null)));
    }

    @Test
    void revocarDosVecesFalla() {
        Persona p = persona(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(30));
        Consentimiento c = new Consentimiento();
        c.setId(1);
        c.setTitular(p);
        c.setTipo(Tipo.DATOS_SALUD);
        when(repo.findById(1)).thenReturn(Optional.of(c));
        assertFalse(service.revocar(1, "ya no").vigente());
        assertThrows(RuntimeException.class, () -> service.revocar(1, "otra vez"));
    }

    @Test
    void exigenciaDeSaludSoloSeAplicaSiEstaActivada() {
        when(repo.existsByTitularIdAndTipoAndFechaRevocadoIsNull(10, Tipo.DATOS_SALUD)).thenReturn(false);
        assertDoesNotThrow(() -> service.exigirConsentimientoSalud(10)); // desactivada

        ConsentimientoService estricto = new ConsentimientoService(repo, personas, acudientes, vinculos, clubes, true);
        assertThrows(RuntimeException.class, () -> estricto.exigirConsentimientoSalud(10));
        when(repo.existsByTitularIdAndTipoAndFechaRevocadoIsNull(10, Tipo.DATOS_SALUD)).thenReturn(true);
        assertDoesNotThrow(() -> estricto.exigirConsentimientoSalud(10));
    }

    @Test
    void mayoriaDeEdadSeCalculaPorFecha() {
        assertTrue(ConsentimientoService.esMenor(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(17).minusDays(364)));
        assertFalse(ConsentimientoService.esMenor(com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusYears(18)));
    }

    @Test
    void elClubActivaLaExigenciaSoloSinPendientes() {
        TenantContext.set(2);
        try {
            Club club = new Club();
            club.setId(2);
            when(clubes.findById(2)).thenReturn(Optional.of(club));
            when(repo.deportistasSinConsentimiento(Tipo.DATOS_SALUD)).thenReturn(List.of());
            when(repo.existsByTitularIdAndTipoAndFechaRevocadoIsNull(10, Tipo.DATOS_SALUD)).thenReturn(false);

            assertFalse(service.exigencia().activa());
            assertFalse(service.configurarExigencia(false).activa());

            assertTrue(service.configurarExigencia(true).activa());
            assertTrue(club.getExigirConsentimientoSalud());
            assertThrows(RuntimeException.class, () -> service.exigirConsentimientoSalud(10));

            assertFalse(service.configurarExigencia(false).activa());
            service.exigirConsentimientoSalud(10);
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void noSePuedeActivarConPendientes() {
        TenantContext.set(2);
        try {
            Club club = new Club();
            when(clubes.findById(2)).thenReturn(Optional.of(club));
            com.varcal.cheermanager.Models.Personas.Deportista d = new com.varcal.cheermanager.Models.Personas.Deportista();
            Persona p = new Persona();
            p.setId(10);
            p.setNombre("A");
            p.setApellidos("B");
            d.setId(1);
            d.setPersona(p);
            when(repo.deportistasSinConsentimiento(Tipo.DATOS_SALUD)).thenReturn(List.of(d));
            assertThrows(IllegalArgumentException.class, () -> service.configurarExigencia(true));
            assertFalse(club.getExigirConsentimientoSalud());
        } finally {
            TenantContext.clear();
        }
    }
}
