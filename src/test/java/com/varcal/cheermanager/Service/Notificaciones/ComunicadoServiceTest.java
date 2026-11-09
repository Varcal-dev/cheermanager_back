package com.varcal.cheermanager.Service.Notificaciones;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Funcionalidad.Notificacion;
import com.varcal.cheermanager.Models.Notificaciones.Comunicado;
import com.varcal.cheermanager.Models.Notificaciones.ComunicadoEnvio;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.DeportistaAcudiente;
import com.varcal.cheermanager.Models.Personas.EstadoPersona;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Notificaciones.ComunicadoService.Resultado;
import com.varcal.cheermanager.Service.Notificaciones.ComunicadoService.Solicitud;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Funcionalidad.NotificacionRepository;
import com.varcal.cheermanager.repository.Notificaciones.ComunicadoEnvioRepository;
import com.varcal.cheermanager.repository.Notificaciones.ComunicadoRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;

/** C7: comunicados por portal y correo + calendarios .ics. */
class ComunicadoServiceTest {

    final List<Comunicado> guardados = new ArrayList<>();
    final List<ComunicadoEnvio> envios = new ArrayList<>();
    final List<Notificacion> notis = new ArrayList<>();
    final List<String> correos = new ArrayList<>();
    final List<String> fallan = new ArrayList<>();
    boolean smtp = true;

    final List<Deportista> miembros = new ArrayList<>();
    final List<DeportistaAcudiente> vinculos = new ArrayList<>();
    final List<Usuario> usuariosDeDeportistas = new ArrayList<>();
    final List<Usuario> usuariosPorId = new ArrayList<>();
    final GrupoEntrenamiento grupo = new GrupoEntrenamiento();

    final CorreoSender sender = new CorreoSender(null, "club@x.co") {
        @Override public boolean disponible() { return smtp; }
        @Override public void enviar(String para, String asunto, String texto) {
            if (fallan.contains(para)) throw new IllegalStateException("rechazado");
            correos.add(para + "|" + asunto);
        }
    };

    final ComunicadoService service = new ComunicadoService(
            Fakes.proxy(ComunicadoRepository.class, new Repo().on("save", a -> {
                Comunicado c = (Comunicado) a[0];
                if (c.getId() == null) { c.setId(guardados.size() + 1); guardados.add(c); }
                return c;
            })),
            Fakes.proxy(ComunicadoEnvioRepository.class, new Repo().on("saveAll", a -> { envios.addAll((List<ComunicadoEnvio>) a[0]); return a[0]; })),
            Fakes.proxy(GrupoEntrenamientoRepository.class, new Repo().on("findById", a -> Optional.of(grupo))),
            Fakes.proxy(DeportistaPerteneceGrupoRepository.class, new Repo().on("findByGrupoIdAndFechaFinIsNull", a -> {
                List<DeportistaPerteneceGrupo> r = new ArrayList<>();
                for (Deportista d : miembros) { DeportistaPerteneceGrupo m = new DeportistaPerteneceGrupo(); m.setDeportista(d); r.add(m); }
                return r;
            })),
            Fakes.proxy(DeportistaRepository.class, new Repo().on("findAll", a -> miembros)),
            Fakes.proxy(DeportistaAcudienteRepository.class, new Repo().on("findByDeportistaId", a -> {
                List<DeportistaAcudiente> r = new ArrayList<>();
                for (DeportistaAcudiente v : vinculos) if (v.getDeportista().getId().equals(a[0])) r.add(v);
                return r;
            })),
            Fakes.proxy(UserRepository.class, new Repo()
                    .on("findByPersona", a -> usuariosDeDeportistas.stream().filter(u -> u.getPersona() == a[0]).findFirst())
                    .on("findById", a -> usuariosPorId.stream().filter(u -> u.getId().longValue() == (Long) a[0]).findFirst())),
            Fakes.proxy(NotificacionRepository.class, new Repo().on("save", a -> { notis.add((Notificacion) a[0]); return a[0]; })),
            sender) {
        @Override protected LocalDateTime ahora() { return LocalDateTime.of(2026, 11, 8, 12, 0); }
    };

    Deportista dep(int id, String estado) {
        Persona p = new Persona(); p.setNombre("D" + id); p.setApellidos("X");
        EstadoPersona e = new EstadoPersona(); e.setEstado(estado);
        Deportista d = new Deportista(); d.setId(id); d.setPersona(p); d.setEstado(e);
        miembros.add(d);
        return d;
    }

    void acudiente(Deportista d, String email, Integer usuarioId) {
        Acudiente a = new Acudiente(); a.setEmail(email); a.setUsuarioId(usuarioId);
        DeportistaAcudiente v = new DeportistaAcudiente(); v.setDeportista(d); v.setAcudiente(a);
        vinculos.add(v);
    }

    Usuario usuario(int id, String username, String email) {
        Usuario u = new Usuario(); u.setId(id); u.setUsername(username); u.setEmail(email);
        return u;
    }

    void reiniciar() {
        guardados.clear(); envios.clear(); notis.clear(); correos.clear(); fallan.clear(); smtp = true;
        miembros.clear(); vinculos.clear(); usuariosDeDeportistas.clear(); usuariosPorId.clear();
        grupo.setId(3); grupo.setNombre("Black Elite");
    }

    @Test
    void previsualizaSinRepetirNiContarInactivos() {
        reiniciar();
        Deportista a = dep(1, "Activo"), b = dep(2, "Activo");
        dep(3, "Retirado");
        acudiente(a, "Mama@x.co", null);
        acudiente(b, "mama@x.co", null);           // mismo correo (dos hijas): una sola vez
        Deportista c = dep(4, "Activo");            // sin ningun contacto
        var r = service.previsualizar(3);
        assertEquals(3, r.deportistas());
        assertEquals(1, r.correos());
        assertEquals(1, r.sinContacto());
        assertTrue(r.correoDisponible());
        assertEquals("Black Elite", r.grupo());
    }

    @Test
    void enviaPorPortalYCorreoYRegistraFallos() {
        reiniciar();
        Deportista a = dep(1, "Activo"), b = dep(2, "Activo");
        acudiente(a, "mama@x.co", 50);
        acudiente(b, "papa@x.co", null);
        usuariosPorId.add(usuario(50, "mama", "mama@x.co"));
        fallan.add("papa@x.co");
        Resultado r = service.enviar(new Solicitud(3, " Ensayo extra ", "El sabado a las 8 am", true, true), 9);
        assertEquals(2, r.deportistas());
        assertEquals(1, r.notificados());
        assertEquals(1, r.correosEnviados());
        assertEquals(1, r.correosFallidos());
        assertEquals(List.of("papa@x.co"), r.fallidos());
        assertEquals("mama@x.co|[Black Elite] Ensayo extra", correos.get(0));
        assertEquals("Ensayo extra\nEl sabado a las 8 am", notis.get(0).getMensaje());
        assertEquals(1, guardados.size());
        assertEquals(1, guardados.get(0).getCorreosFallidos());
        assertEquals(3, envios.size()); // 1 portal + 2 correos
        assertEquals(1, envios.stream().filter(e -> e.getEstado().equals("FALLIDO")).count());
    }

    @Test
    void validaYExigeSmtpParaCorreo() {
        reiniciar();
        Deportista a = dep(1, "Activo");
        acudiente(a, "mama@x.co", null);
        assertThrows(IllegalArgumentException.class, () -> service.enviar(new Solicitud(3, "ab", "mensaje largo", true, false), 9));
        assertThrows(IllegalArgumentException.class, () -> service.enviar(new Solicitud(3, "Asunto", "hola", true, false), 9));
        assertThrows(IllegalArgumentException.class, () -> service.enviar(new Solicitud(3, "Asunto", "mensaje largo", false, false), 9));
        smtp = false;
        assertThrows(IllegalStateException.class, () -> service.enviar(new Solicitud(3, "Asunto", "mensaje largo", false, true), 9));
        smtp = true;
        miembros.clear();
        assertThrows(IllegalStateException.class, () -> service.enviar(new Solicitud(3, "Asunto", "mensaje largo", true, true), 9));
        assertTrue(guardados.isEmpty());
        assertTrue(correos.isEmpty());
    }

    @Test
    void icsEscapaYPlegaLineas() {
        assertEquals("a\\, b\\; c\\nd", IcsBuilder.escapar("a, b; c\nd"));
        StringBuilder sb = new StringBuilder();
        IcsBuilder.linea(sb, "SUMMARY:" + "ñ".repeat(100));
        for (String linea : sb.toString().split("\r\n")) {
            assertTrue(linea.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= 75, "linea larga: " + linea.length());
        }
        assertTrue(sb.toString().contains("\r\n "));
        String semanal = IcsBuilder.eventoSemanal("u@c", LocalDateTime.of(2026, 11, 8, 12, 0), LocalDate.of(2026, 11, 10),
                LocalTime.of(18, 0), LocalTime.of(20, 0), "TU", "Entrenamiento Black, Elite", null);
        assertTrue(semanal.contains("DTSTART;TZID=America/Bogota:20261110T180000"));
        assertTrue(semanal.contains("RRULE:FREQ=WEEKLY;BYDAY=TU"));
        assertTrue(semanal.contains("SUMMARY:Entrenamiento Black\\, Elite"));
        assertTrue(semanal.contains("DTSTAMP:20261108T120000Z"));
        String dia = IcsBuilder.eventoDiaCompleto("e@c", LocalDateTime.of(2026, 11, 8, 12, 0), LocalDate.of(2026, 11, 21), "Copa", "Bogota", null);
        assertTrue(dia.contains("DTSTART;VALUE=DATE:20261121"));
        assertTrue(dia.contains("DTEND;VALUE=DATE:20261122"));
        String cal = IcsBuilder.calendario("Mi cal", List.of(semanal, dia));
        assertTrue(cal.startsWith("BEGIN:VCALENDAR\r\n"));
        assertTrue(cal.endsWith("END:VCALENDAR\r\n"));
        assertTrue(cal.contains("TZID:America/Bogota"));
    }
}
