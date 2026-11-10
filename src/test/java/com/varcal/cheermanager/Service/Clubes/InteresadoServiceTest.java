package com.varcal.cheermanager.Service.Clubes;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Models.Clubes.Interesado;
import com.varcal.cheermanager.Models.Clubes.Interesado.Estado;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.repository.Clubes.InteresadoRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C6: pre-inscripcion publica, clase de prueba y lista de espera. */
class InteresadoServiceTest {

    final List<Interesado> guardados = new ArrayList<>();
    long reloj = 1_000_000L;
    int minutos = 0;
    final Club ratels = club(2, "ratels-all-stars", true);
    final Club inactivo = club(3, "cerrado", false);

    static Club club(int id, String slug, boolean activo) {
        Club c = new Club(); c.setId(id); c.setSlug(slug); c.setNombre(slug); c.setActivo(activo); return c;
    }

    InteresadoService service = nuevo();

    InteresadoService nuevo() {
        return new InteresadoService(
                Fakes.proxy(InteresadoRepository.class, new Repo()
                        .on("existsByContactoCorreoIgnoreCaseAndDeportistaNombreIgnoreCase", a -> guardados.stream()
                                .anyMatch(i -> i.getContactoCorreo().equalsIgnoreCase((String) a[0]) && i.getDeportistaNombre().equalsIgnoreCase((String) a[1])))
                        .on("save", a -> { Interesado i = (Interesado) a[0]; if (i.getId() == null) { i.setId(guardados.size() + 1); guardados.add(i); } return i; })
                        .on("findById", a -> guardados.stream().filter(i -> i.getId().equals(a[0])).findFirst())
                        .on("findAllByOrderByFechaCreacionDescIdDesc", a -> {
                            List<Interesado> r = new ArrayList<>(guardados); java.util.Collections.reverse(r); return r; })),
                Fakes.proxy(ClubRepository.class, new Repo().on("findBySlug", a ->
                        "ratels-all-stars".equals(a[0]) ? Optional.of(ratels) : "cerrado".equals(a[0]) ? Optional.of(inactivo) : Optional.empty())),
                Fakes.proxy(GrupoEntrenamientoRepository.class, new Repo().on("existsById", a -> ((Integer) a[0]) == 5))) {
            @Override protected LocalDate hoy() { return LocalDate.of(2026, 11, 9); }
            @Override protected LocalDateTime ahora() { return LocalDateTime.of(2026, 11, 9, 10, minutos++); }
            @Override protected long ahoraMs() { return reloj; }
        };
    }

    void reiniciar() { guardados.clear(); reloj = 1_000_000L; minutos = 0; service = nuevo(); }

    Map<String, Object> valido(String tipo, String correo, String dep) {
        Map<String, Object> b = new HashMap<>();
        b.put("tipo", tipo); b.put("contactoNombre", "Maria Lopez"); b.put("contactoCorreo", correo);
        b.put("contactoTelefono", "300 123 4567"); b.put("deportistaNombre", dep); b.put("deportistaNacimiento", "2014-05-20");
        b.put("consentimiento", true); b.put("versionConsentimiento", "2026-11");
        return b;
    }

    @Test
    void registraEnElClubDelSlugConPruebaDelConsentimiento() {
        reiniciar();
        assertTrue(service.registrarPublico("ratels-all-stars", valido("CLASE_PRUEBA", "Maria@x.com", "Sofia"), "190.1.1.1"));
        Interesado i = guardados.get(0);
        assertEquals(Estado.NUEVO, i.getEstado());
        assertEquals("maria@x.com", i.getContactoCorreo());
        assertEquals("190.1.1.1", i.getIp());
        assertEquals("2026-11", i.getVersionConsentimiento());
        assertEquals(12, service.listar().items().get(0).edad());
    }

    @Test
    void clubInexistenteOInactivoSeRechaza() {
        reiniciar();
        assertThrows(IllegalArgumentException.class, () -> service.registrarPublico("no-existe", valido("INSCRIPCION", "a@x.com", "Sofia"), "1.1.1.1"));
        assertThrows(IllegalArgumentException.class, () -> service.registrarPublico("cerrado", valido("INSCRIPCION", "a@x.com", "Sofia"), "1.1.1.1"));
        assertTrue(guardados.isEmpty());
    }

    @Test
    void validaDatosYConsentimiento() {
        reiniciar();
        Map<String, Object> a = valido("INSCRIPCION", "a@x.com", "Sofia"); a.put("consentimiento", false);
        assertThrows(IllegalArgumentException.class, () -> service.registrarPublico("ratels-all-stars", a, "1.1.1.1"));
        Map<String, Object> b = valido("OTRA", "a@x.com", "Sofia");
        assertThrows(IllegalArgumentException.class, () -> service.registrarPublico("ratels-all-stars", b, "1.1.1.1"));
        Map<String, Object> c = valido("INSCRIPCION", "a@x.com", "Sofia"); c.put("deportistaNacimiento", "2026-12-01");
        assertThrows(IllegalArgumentException.class, () -> service.registrarPublico("ratels-all-stars", c, "1.1.1.1"));
        Map<String, Object> d = valido("INSCRIPCION", "a@x.com", "Sofia"); d.put("contactoTelefono", "123");
        assertThrows(IllegalArgumentException.class, () -> service.registrarPublico("ratels-all-stars", d, "1.1.1.1"));
        Map<String, Object> e = valido("INSCRIPCION", "a@x.com", "Sofia"); e.put("versionConsentimiento", "2020-01");
        assertThrows(IllegalArgumentException.class, () -> service.registrarPublico("ratels-all-stars", e, "1.1.1.1"));
        assertTrue(guardados.isEmpty());
    }

    @Test
    void trampaYRepetidoNoGuardan() {
        reiniciar();
        Map<String, Object> t = valido("INSCRIPCION", "a@x.com", "Sofia"); t.put("website", "http://spam");
        assertFalse(service.registrarPublico("ratels-all-stars", t, "1.1.1.1"));
        assertTrue(service.registrarPublico("ratels-all-stars", valido("INSCRIPCION", "a@x.com", "Sofia"), "2.2.2.2"));
        assertFalse(service.registrarPublico("ratels-all-stars", valido("CLASE_PRUEBA", "A@X.com", "SOFIA"), "3.3.3.3"));
        assertEquals(1, guardados.size());
    }

    @Test
    void limitePorIp() {
        reiniciar();
        for (int i = 0; i < InteresadoService.MAX_POR_IP; i++) service.registrarPublico("ratels-all-stars", valido("INSCRIPCION", "u" + i + "@x.com", "Dep" + i), "9.9.9.9");
        assertThrows(InteresadoService.TooManyRequests.class, () -> service.registrarPublico("ratels-all-stars", valido("INSCRIPCION", "otro@x.com", "Otro"), "9.9.9.9"));
        reloj += InteresadoService.VENTANA_MS + 1;
        assertTrue(service.registrarPublico("ratels-all-stars", valido("INSCRIPCION", "otro@x.com", "Otro"), "9.9.9.9"));
    }

    @Test
    void listaDeEsperaConPosicionPorOrdenDeLlegada() {
        reiniciar();
        service.registrarPublico("ratels-all-stars", valido("LISTA_ESPERA", "a@x.com", "Ana"), "1.1.1.1");
        service.registrarPublico("ratels-all-stars", valido("LISTA_ESPERA", "b@x.com", "Bea"), "1.1.1.2");
        service.registrarPublico("ratels-all-stars", valido("LISTA_ESPERA", "c@x.com", "Cami"), "1.1.1.3");
        service.registrarPublico("ratels-all-stars", valido("CLASE_PRUEBA", "d@x.com", "Dani"), "1.1.1.4");
        InteresadoService.Resumen r = service.listar();
        assertEquals(3, r.enEspera());
        assertEquals(1, r.porEstado().get(Estado.NUEVO));
        for (InteresadoService.Fila f : r.items()) {
            if (f.deportistaNombre().equals("Ana")) assertEquals(1, f.posicionEspera());
            if (f.deportistaNombre().equals("Cami")) assertEquals(3, f.posicionEspera());
            if (f.deportistaNombre().equals("Dani")) assertNull(f.posicionEspera());
        }
        service.actualizar(1, new InteresadoService.Actualizacion(Estado.INSCRITO, null, null, null)); // Ana sale de la espera
        InteresadoService.Resumen r2 = service.listar();
        for (InteresadoService.Fila f : r2.items()) if (f.deportistaNombre().equals("Cami")) assertEquals(2, f.posicionEspera());
    }

    @Test
    void agendarPruebaExigeFechaFuturaYGrupoValido() {
        reiniciar();
        service.registrarPublico("ratels-all-stars", valido("CLASE_PRUEBA", "a@x.com", "Ana"), "1.1.1.1");
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(1, new InteresadoService.Actualizacion(Estado.PRUEBA_AGENDADA, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(1, new InteresadoService.Actualizacion(Estado.PRUEBA_AGENDADA, null, LocalDate.of(2026, 11, 1), null)));
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(1, new InteresadoService.Actualizacion(null, 99, null, null)));
        InteresadoService.Fila f = service.actualizar(1, new InteresadoService.Actualizacion(Estado.PRUEBA_AGENDADA, 5, LocalDate.of(2026, 11, 20), "Llevar ropa comoda"));
        assertEquals(Estado.PRUEBA_AGENDADA, f.estado());
        assertEquals(5, f.grupoId());
        assertEquals("Llevar ropa comoda", f.notas());
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(77, new InteresadoService.Actualizacion(Estado.CONTACTADO, null, null, null)));
    }
}
