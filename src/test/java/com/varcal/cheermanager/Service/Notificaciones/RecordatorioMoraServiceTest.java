package com.varcal.cheermanager.Service.Notificaciones;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.varcal.cheermanager.DTO.Notificaciones.RecordatorioConfigDTO;
import com.varcal.cheermanager.DTO.Notificaciones.RecordatorioMoraRequestDTO;
import com.varcal.cheermanager.DTO.Notificaciones.RecordatorioMoraResultadoDTO;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Funcionalidad.Notificacion;
import com.varcal.cheermanager.Models.Funcionalidad.RecordatorioConfig;
import com.varcal.cheermanager.Models.Funcionalidad.RecordatorioEnvio;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.DeportistaAcudiente;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Funcionalidad.NotificacionRepository;
import com.varcal.cheermanager.repository.Funcionalidad.RecordatorioConfigRepository;
import com.varcal.cheermanager.repository.Funcionalidad.RecordatorioEnvioRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.testsupport.Fakes;
import com.varcal.cheermanager.testsupport.Fakes.Repo;
import com.varcal.cheermanager.testsupport.MundoFinanciero;

/** Recordatorios de mora: un mensaje por persona, frecuencia por canal y un canal que falla no frena a los otros. */
class RecordatorioMoraServiceTest {

    static class CorreoFalso extends CorreoSender {
        final boolean disponible;
        boolean falla = false;
        final List<String> enviados = new ArrayList<>();
        CorreoFalso(boolean disponible) { super(null, "x@x.co"); this.disponible = disponible; }
        @Override public boolean disponible() { return disponible; }
        @Override public void enviar(String para, String asunto, String texto) {
            if (falla) throw new RuntimeException("SMTP caido");
            enviados.add(para + "|" + texto);
        }
    }

    static class WhatsAppFalso extends WhatsAppSender {
        final boolean disponible;
        final List<String> enviados = new ArrayList<>();
        WhatsAppFalso(boolean disponible) { super("", "", "", "es", "v20.0"); this.disponible = disponible; }
        @Override public boolean disponible() { return disponible; }
        @Override public void enviarPlantilla(String tel, List<String> vars) { enviados.add(tel); }
    }

    static class Escenario {
        final MundoFinanciero w = new MundoFinanciero();
        final List<Notificacion> notificaciones = new ArrayList<>();
        final List<RecordatorioEnvio> envios = new ArrayList<>();
        final List<Object[]> ultimos = new ArrayList<>(); // {facturaId, canal, LocalDateTime}
        final RecordatorioConfig config = new RecordatorioConfig();
        final List<Usuario> usuarios = new ArrayList<>();
        Acudiente acudiente;
        final CorreoFalso correo;
        final WhatsAppFalso whatsapp;
        final RecordatorioMoraService servicio;

        Escenario(boolean smtp, boolean waApi) {
            correo = new CorreoFalso(smtp);
            whatsapp = new WhatsAppFalso(waApi);
            // Ana (persona 1) con usuario del portal y correo; su acudiente tiene correo y celular.
            Persona ana = w.persona(1, "Ana");
            ana.setTelefono("3001234567");
            Usuario u = new Usuario();
            u.setId(5); u.setUsername("ana"); u.setEmail("ana@mail.co"); u.setPersona(ana);
            usuarios.add(u);
            acudiente = new Acudiente();
            acudiente.setEmail("mama@mail.co");
            acudiente.setTelefono("3109876543");
            Deportista dep = new Deportista(); dep.setId(1); dep.setPersona(ana);
            DeportistaAcudiente v = new DeportistaAcudiente(); v.setDeportista(dep); v.setAcudiente(acudiente);

            Repo userRepo = new Repo()
                    .on("findByPersona", a -> usuarios.stream().filter(x -> x.getPersona() == a[0]).findFirst())
                    .on("findById", a -> Optional.empty());
            Repo depRepo = new Repo().on("findByPersonaId",
                    a -> (Integer) a[0] == 1 ? Optional.of(dep) : Optional.empty());
            Repo vincRepo = new Repo().on("findByDeportistaId", a -> List.of(v));
            Repo notifRepo = new Repo().on("save", a -> { notificaciones.add((Notificacion) a[0]); return a[0]; });
            Repo cfgRepo = new Repo()
                    .on("findAll", a -> List.of(config))
                    .on("save", a -> a[0]);
            Repo envRepo = new Repo()
                    .on("ultimosEnvios", a -> ultimos)
                    .on("save", a -> { envios.add((RecordatorioEnvio) a[0]); return a[0]; });
            servicio = new RecordatorioMoraService(w.facturaRepository, w.facturaService, w.personaRepository,
                    Fakes.proxy(DeportistaRepository.class, depRepo),
                    Fakes.proxy(DeportistaAcudienteRepository.class, vincRepo),
                    Fakes.proxy(UserRepository.class, userRepo),
                    Fakes.proxy(NotificacionRepository.class, notifRepo),
                    Fakes.proxy(RecordatorioConfigRepository.class, cfgRepo),
                    Fakes.proxy(RecordatorioEnvioRepository.class, envRepo),
                    correo, whatsapp);
        }

        Factura enMora(int personaId, String total) {
            return w.factura(personaId, total, com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().minusDays(5));
        }
    }

    static RecordatorioMoraRequestDTO req(boolean sistema, boolean correo, boolean whatsapp) {
        RecordatorioMoraRequestDTO r = new RecordatorioMoraRequestDTO();
        r.setSistema(sistema); r.setCorreo(correo); r.setWhatsapp(whatsapp);
        return r;
    }

    @Test
    void unSoloMensajePorPersonaConTodasSusFacturasVencidas() {
        Escenario e = new Escenario(true, false);
        e.enMora(1, "100000");
        e.enMora(1, "50000");
        RecordatorioMoraResultadoDTO r = e.servicio.enviar(req(true, false, false), 9);
        assertEquals(2, r.facturasEnMora());
        assertEquals(1, r.personas());
        assertEquals(1, e.notificaciones.size()); // un mensaje, no dos
        assertTrue(e.notificaciones.get(0).getMensaje().contains("2"));
        assertEquals(2, e.envios.size());          // pero queda trazado por cada factura
        assertEquals(9, (int) e.envios.get(0).getEnviadoPorUsuarioId());
        assertFalse(e.envios.get(0).getAutomatico());
    }

    @Test
    void sinCanalSeleccionadoUsaElSistema() {
        Escenario e = new Escenario(true, false);
        e.enMora(1, "100000");
        e.servicio.enviar(req(false, false, false), 9);
        assertEquals(1, e.notificaciones.size());
    }

    @Test
    void previsualizarNoEnviaNiRegistraNada() {
        Escenario e = new Escenario(true, false);
        e.enMora(1, "100000");
        RecordatorioMoraResultadoDTO r = e.servicio.previsualizar(req(true, true, false));
        assertTrue(r.simulacion());
        assertEquals(0, e.notificaciones.size());
        assertEquals(0, e.correo.enviados.size());
        assertEquals(0, e.envios.size());
        assertEquals("PROGRAMADO", r.avisos().get(0).canales().get(0).estado());
    }

    @Test
    void noRepiteElAvisoAntesDeLosDiasEntreAvisosPeroSiForzado() {
        Escenario e = new Escenario(true, false);
        Factura f = e.enMora(1, "100000");
        e.ultimos.add(new Object[] { f.getId(), "SISTEMA", LocalDateTime.now().minusDays(2) }); // config: 7 dias
        RecordatorioMoraResultadoDTO r = e.servicio.enviar(req(true, false, false), 9);
        assertEquals(1, r.omitidosPorFrecuencia());
        assertEquals(0, e.notificaciones.size());

        RecordatorioMoraRequestDTO forzar = req(true, false, false);
        forzar.setForzar(true);
        e.servicio.enviar(forzar, 9);
        assertEquals(1, e.notificaciones.size());
    }

    @Test
    void avisaDeNuevoSiYaPasaronLosDiasEntreAvisos() {
        Escenario e = new Escenario(true, false);
        Factura f = e.enMora(1, "100000");
        e.ultimos.add(new Object[] { f.getId(), "SISTEMA", LocalDateTime.now().minusDays(8) });
        e.servicio.enviar(req(true, false, false), 9);
        assertEquals(1, e.notificaciones.size());
    }

    @Test
    void lafrecuenciaEsPorCanal() {
        Escenario e = new Escenario(true, false);
        Factura f = e.enMora(1, "100000");
        e.ultimos.add(new Object[] { f.getId(), "SISTEMA", LocalDateTime.now().minusDays(1) });
        e.servicio.enviar(req(true, true, false), 9);
        assertEquals(0, e.notificaciones.size()); // sistema ya avisado
        assertTrue(e.correo.enviados.size() > 0);  // correo nunca avisado
    }

    @Test
    void unCanalQueFallaNoImpideLosOtrosYQuedaComoFallido() {
        Escenario e = new Escenario(true, false);
        e.enMora(1, "100000");
        e.correo.falla = true;
        RecordatorioMoraResultadoDTO r = e.servicio.enviar(req(true, true, false), 9);
        assertEquals(1, e.notificaciones.size());
        assertTrue(r.fallidos().getOrDefault("CORREO", 0) > 0);
        assertEquals(1, (int) r.enviados().get("SISTEMA"));
        assertTrue(e.envios.stream().anyMatch(x -> "FALLIDO".equals(x.getEstado()) && "CORREO".equals(x.getCanal())));
    }

    @Test
    void correoSinSmtpSeAdviertePeroElRestoSigue() {
        Escenario e = new Escenario(false, false);
        e.enMora(1, "100000");
        RecordatorioMoraResultadoDTO r = e.servicio.enviar(req(true, true, false), 9);
        assertFalse(r.advertencias().isEmpty());
        assertEquals(1, e.notificaciones.size());
        assertEquals(0, e.correo.enviados.size());
    }

    @Test
    void whatsappSinApiGeneraEnlacesYNoLosMarcaEnviados() {
        Escenario e = new Escenario(true, false);
        e.enMora(1, "100000");
        RecordatorioMoraResultadoDTO r = e.servicio.enviar(req(false, false, true), 9);
        assertFalse(r.enlacesWhatsapp().isEmpty());
        assertTrue(r.enlacesWhatsapp().get(0).url().startsWith("https://wa.me/57"));
        assertEquals(0, e.envios.size());
        assertEquals(0, e.whatsapp.enviados.size());
        assertTrue(r.avisos().get(0).canales().stream().allMatch(c -> c.estado().equals("MANUAL")));
    }

    @Test
    void whatsappConApiEnviaAlNumeroDelDeportistaYDelAcudienteSinRepetir() {
        Escenario e = new Escenario(true, true);
        e.enMora(1, "100000");
        e.servicio.enviar(req(false, false, true), 9);
        assertEquals(List.of("573001234567", "573109876543"), e.whatsapp.enviados);
    }

    @Test
    void telefonosInvalidosNoSeEnviana() {
        Escenario e = new Escenario(true, true);
        e.w.personas.get(1).setTelefono("12345");
        e.acudiente.setTelefono("(602) 555 1234"); // fijo: no se asume
        e.enMora(1, "100000");
        RecordatorioMoraResultadoDTO r = e.servicio.enviar(req(false, false, true), 9);
        assertEquals(0, e.whatsapp.enviados.size());
        assertEquals("SIN_DESTINO", r.avisos().get(0).canales().get(0).estado());
    }

    @Test
    void facturasPagadasAnuladasOAlDiaNoGeneranAviso() {
        Escenario e = new Escenario(true, false);
        Factura anulada = e.enMora(1, "100000");
        anulada.setAnulada(true);
        e.w.factura(1, "100000", com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy().plusDays(3)); // aun no vence
        RecordatorioMoraResultadoDTO r = e.servicio.enviar(req(true, false, false), 9);
        assertEquals(0, r.facturasEnMora());
        assertEquals(0, e.notificaciones.size());
    }

    @Test
    void soloAvisaLasFacturasPedidas() {
        Escenario e = new Escenario(true, false);
        Factura a = e.enMora(1, "100000");
        e.enMora(1, "50000");
        RecordatorioMoraRequestDTO r = req(true, false, false);
        r.setFacturaIds(List.of(a.getId()));
        RecordatorioMoraResultadoDTO res = e.servicio.enviar(r, 9);
        assertEquals(1, res.facturasEnMora());
    }

    @Test
    void automaticoSoloCorreSiElClubLoActivoYMarcaLosEnviosComoAutomaticos() {
        Escenario e = new Escenario(true, false);
        e.enMora(1, "100000");
        assertNull(e.servicio.ejecutarAutomatico());
        assertEquals(0, e.notificaciones.size());
        e.config.setAutomatico(true);
        assertNotNull(e.servicio.ejecutarAutomatico());
        assertEquals(1, e.notificaciones.size());
        assertTrue(e.envios.get(0).getAutomatico());
        assertNull(e.envios.get(0).getEnviadoPorUsuarioId());
    }

    @Test
    void configuracionValidaDiasYExigeCanalParaElAutomatico() {
        Escenario e = new Escenario(true, false);
        RecordatorioConfigDTO malo = new RecordatorioConfigDTO();
        malo.setDiasEntreAvisos(0);
        assertThrows(IllegalArgumentException.class, () -> e.servicio.actualizarConfig(malo));
        malo.setDiasEntreAvisos(91);
        assertThrows(IllegalArgumentException.class, () -> e.servicio.actualizarConfig(malo));

        RecordatorioConfigDTO sinCanal = new RecordatorioConfigDTO();
        sinCanal.setSistema(false); sinCanal.setCorreo(false); sinCanal.setWhatsapp(false); sinCanal.setAutomatico(true);
        assertThrows(IllegalArgumentException.class, () -> e.servicio.actualizarConfig(sinCanal));
    }

    @Test
    void probarCorreoValidaDireccionYSmtp() {
        Escenario sinSmtp = new Escenario(false, false);
        assertThrows(IllegalArgumentException.class, () -> sinSmtp.servicio.probarCorreo("no-es-correo"));
        assertThrows(IllegalStateException.class, () -> sinSmtp.servicio.probarCorreo("a@b.co"));
        Escenario conSmtp = new Escenario(true, false);
        conSmtp.servicio.probarCorreo(" a@b.co ");
        assertEquals(1, conSmtp.correo.enviados.size());
        assertTrue(conSmtp.correo.enviados.get(0).startsWith("a@b.co|"));
    }
}
