package com.varcal.cheermanager.Service.Legal;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Legal.PreRegistro;
import com.varcal.cheermanager.repository.Legal.PreRegistroRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** Pre-registro publico de la landing. */
class PreRegistroServiceTest {

    final List<PreRegistro> guardados = new ArrayList<>();
    long reloj = 1_000_000L;

    PreRegistroService service = nuevo();

    PreRegistroService nuevo() {
        return new PreRegistroService(Fakes.proxy(PreRegistroRepository.class, new Repo()
                .on("existsByCorreoIgnoreCase", a -> guardados.stream().anyMatch(p -> p.getCorreo().equalsIgnoreCase((String) a[0])))
                .on("save", a -> { PreRegistro p = (PreRegistro) a[0]; if (p.getId() == null) { p.setId(guardados.size() + 1); guardados.add(p); } return p; })
                .on("findById", a -> guardados.stream().filter(p -> p.getId().equals(a[0])).findFirst())
                .on("findAllByOrderByFechaConsentimientoDescIdDesc", a -> guardados))) {
            @Override protected LocalDateTime ahora() { return LocalDateTime.of(2026, 11, 9, 9, 0); }
            @Override protected long ahoraMs() { return reloj; }
        };
    }

    void reiniciar() { guardados.clear(); reloj = 1_000_000L; service = nuevo(); }

    Map<String, Object> valido() {
        Map<String, Object> b = new HashMap<>();
        b.put("nombre", "Ana Perez"); b.put("correo", "Ana@Club.com"); b.put("telefono", "3001234567");
        b.put("club", "Club X"); b.put("deportistas", "120"); b.put("consentimiento", true); b.put("versionConsentimiento", "2026-11");
        return b;
    }

    @Test
    void guardaConPruebaDelConsentimiento() {
        reiniciar();
        assertTrue(service.registrar(valido(), "190.1.2.3"));
        PreRegistro p = guardados.get(0);
        assertEquals("ana@club.com", p.getCorreo());
        assertEquals("190.1.2.3", p.getIp());
        assertEquals("2026-11", p.getVersionConsentimiento());
        assertEquals(LocalDateTime.of(2026, 11, 9, 9, 0), p.getFechaConsentimiento());
        assertEquals(120, p.getDeportistas());
        assertEquals("NUEVO", p.getEstado());
        assertTrue(p.getDatosJson().contains("\"club\":\"Club X\""));
    }

    @Test
    void sinConsentimientoOCorreoMaloSeRechaza() {
        reiniciar();
        Map<String, Object> a = valido(); a.put("consentimiento", false);
        assertThrows(IllegalArgumentException.class, () -> service.registrar(a, "1.1.1.1"));
        Map<String, Object> b = valido(); b.put("correo", "no-es-correo");
        assertThrows(IllegalArgumentException.class, () -> service.registrar(b, "1.1.1.1"));
        Map<String, Object> c = valido(); c.remove("nombre");
        assertThrows(IllegalArgumentException.class, () -> service.registrar(c, "1.1.1.1"));
        assertTrue(guardados.isEmpty());
    }

    @Test
    void versionVencidaPideRecargar() {
        reiniciar();
        Map<String, Object> b = valido(); b.put("versionConsentimiento", "2025-01");
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.registrar(b, "1.1.1.1"));
        assertTrue(e.getMessage().contains("Recarga"));
    }

    @Test
    void campoTrampaDescartaEnSilencio() {
        reiniciar();
        Map<String, Object> b = valido(); b.put("website", "http://spam");
        assertFalse(service.registrar(b, "1.1.1.1"));
        assertTrue(guardados.isEmpty());
    }

    @Test
    void correoRepetidoNoDuplicaNiRevela() {
        reiniciar();
        assertTrue(service.registrar(valido(), "1.1.1.1"));
        Map<String, Object> otra = valido(); otra.put("correo", "ANA@club.com");
        assertFalse(service.registrar(otra, "2.2.2.2"));
        assertEquals(1, guardados.size());
    }

    @Test
    void limitePorIpYSeLibera() {
        reiniciar();
        for (int i = 0; i < PreRegistroService.MAX_POR_IP; i++) {
            Map<String, Object> b = valido(); b.put("correo", "u" + i + "@x.com");
            service.registrar(b, "9.9.9.9");
        }
        Map<String, Object> mas = valido(); mas.put("correo", "otro@x.com");
        assertThrows(PreRegistroService.TooManyRequests.class, () -> service.registrar(mas, "9.9.9.9"));
        service.registrar(mas, "8.8.8.8"); // otra IP no se afecta
        reloj += PreRegistroService.VENTANA_MS + 1;
        assertTrue(service.registrar(valido(), "9.9.9.9") || guardados.size() > 0); // pasada la ventana ya no lanza TooManyRequests
    }

    @Test
    void actualizarEstado() {
        reiniciar();
        service.registrar(valido(), "1.1.1.1");
        assertEquals("CONTACTADO", service.actualizar(1, "CONTACTADO", "llamar el lunes").getEstado());
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(1, "XX", null));
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(99, "NUEVO", null));
    }
}
