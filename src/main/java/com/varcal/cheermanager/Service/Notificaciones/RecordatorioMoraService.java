package com.varcal.cheermanager.Service.Notificaciones;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.DTO.Notificaciones.CanalesDisponiblesDTO;
import com.varcal.cheermanager.DTO.Notificaciones.RecordatorioConfigDTO;
import com.varcal.cheermanager.DTO.Notificaciones.RecordatorioMoraRequestDTO;
import com.varcal.cheermanager.DTO.Notificaciones.RecordatorioMoraResultadoDTO;
import com.varcal.cheermanager.DTO.Notificaciones.RecordatorioMoraResultadoDTO.Aviso;
import com.varcal.cheermanager.DTO.Notificaciones.RecordatorioMoraResultadoDTO.CanalAviso;
import com.varcal.cheermanager.DTO.Notificaciones.RecordatorioMoraResultadoDTO.Enlace;
import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Funcionalidad.Notificacion;
import com.varcal.cheermanager.Models.Funcionalidad.RecordatorioConfig;
import com.varcal.cheermanager.Models.Funcionalidad.RecordatorioEnvio;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Funcionalidad.NotificacionRepository;
import com.varcal.cheermanager.repository.Funcionalidad.RecordatorioConfigRepository;
import com.varcal.cheermanager.repository.Funcionalidad.RecordatorioEnvioRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;

/**
 * Recordatorios de mora por tres canales:
 *  - SISTEMA: notificacion dentro del portal para el deportista y sus acudientes con usuario (el estandar);
 *  - CORREO: a los correos del usuario del deportista y de sus acudientes (requiere SMTP);
 *  - WHATSAPP: por la API de Meta si esta configurada; si no, enlaces wa.me para enviar a mano.
 *
 * Reglas: un solo mensaje por destinatario con TODAS las facturas vencidas de esa persona; no se repite el
 * aviso de la misma factura por el mismo canal antes de dias_entre_avisos (salvo forzar); cada intento queda
 * en recordatorios_envios. Un canal que falla no impide los otros.
 */
@Service
public class RecordatorioMoraService {

    private static final Logger log = LoggerFactory.getLogger(RecordatorioMoraService.class);
    private static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public static final String SISTEMA = "SISTEMA";
    public static final String CORREO = "CORREO";
    public static final String WHATSAPP = "WHATSAPP";

    private final FacturaRepository facturaRepository;
    private final FacturaService facturaService;
    private final PersonaRepository personaRepository;
    private final DeportistaRepository deportistaRepository;
    private final DeportistaAcudienteRepository vinculoRepository;
    private final UserRepository userRepository;
    private final NotificacionRepository notificacionRepository;
    private final RecordatorioConfigRepository configRepository;
    private final RecordatorioEnvioRepository envioRepository;
    private final CorreoSender correoSender;
    private final WhatsAppSender whatsAppSender;

    public RecordatorioMoraService(FacturaRepository facturaRepository, FacturaService facturaService,
                                   PersonaRepository personaRepository, DeportistaRepository deportistaRepository,
                                   DeportistaAcudienteRepository vinculoRepository, UserRepository userRepository,
                                   NotificacionRepository notificacionRepository,
                                   RecordatorioConfigRepository configRepository,
                                   RecordatorioEnvioRepository envioRepository,
                                   CorreoSender correoSender, WhatsAppSender whatsAppSender) {
        this.facturaRepository = facturaRepository;
        this.facturaService = facturaService;
        this.personaRepository = personaRepository;
        this.deportistaRepository = deportistaRepository;
        this.vinculoRepository = vinculoRepository;
        this.userRepository = userRepository;
        this.notificacionRepository = notificacionRepository;
        this.configRepository = configRepository;
        this.envioRepository = envioRepository;
        this.correoSender = correoSender;
        this.whatsAppSender = whatsAppSender;
    }

    // ---------- Configuracion ----------

    @Transactional
    public RecordatorioConfig obtenerConfig() {
        return configRepository.findAll().stream().findFirst()
                .orElseGet(() -> configRepository.save(new RecordatorioConfig()));
    }

    @Transactional
    public RecordatorioConfig actualizarConfig(RecordatorioConfigDTO dto) {
        RecordatorioConfig c = obtenerConfig();
        if (dto.getSistema() != null) c.setCanalSistema(dto.getSistema());
        if (dto.getCorreo() != null) c.setCanalCorreo(dto.getCorreo());
        if (dto.getWhatsapp() != null) c.setCanalWhatsapp(dto.getWhatsapp());
        if (dto.getAutomatico() != null) c.setAutomatico(dto.getAutomatico());
        if (dto.getDiasEntreAvisos() != null) {
            if (dto.getDiasEntreAvisos() < 1 || dto.getDiasEntreAvisos() > 90) {
                throw new IllegalArgumentException("Los dias entre avisos deben estar entre 1 y 90");
            }
            c.setDiasEntreAvisos(dto.getDiasEntreAvisos());
        }
        if (Boolean.TRUE.equals(c.getAutomatico()) && !c.getCanalSistema() && !c.getCanalCorreo() && !c.getCanalWhatsapp()) {
            throw new IllegalArgumentException("Para el envio automatico elige al menos un canal");
        }
        return configRepository.save(c);
    }

    public CanalesDisponiblesDTO canalesDisponibles() {
        return new CanalesDisponiblesDTO(true, correoSender.disponible(), whatsAppSender.disponible());
    }

    public List<RecordatorioEnvio> historial(int limite) {
        return envioRepository.findAllByOrderByFechaDesc(PageRequest.of(0, Math.max(1, Math.min(limite, 500))));
    }

    /** Correo de prueba para verificar el SMTP desde la pantalla de configuracion. */
    public void probarCorreo(String para) {
        if (para == null || !EMAIL.matcher(para.trim()).matches()) {
            throw new IllegalArgumentException("Correo invalido");
        }
        if (!correoSender.disponible()) {
            throw new IllegalStateException("SMTP no configurado en el servidor (falta SPRING_MAIL_HOST)");
        }
        correoSender.enviar(para.trim(), "Prueba de correo - CheerManager",
                "Si recibes este mensaje, el correo de CheerManager esta bien configurado.");
    }

    // ---------- Envio ----------

    /** Lo que ejecuta la tarea diaria para el club actual; no hace nada si el club no activo el envio automatico. */
    @Transactional
    public RecordatorioMoraResultadoDTO ejecutarAutomatico() {
        RecordatorioConfig c = obtenerConfig();
        if (!Boolean.TRUE.equals(c.getAutomatico())) {
            return null;
        }
        RecordatorioMoraRequestDTO req = new RecordatorioMoraRequestDTO();
        req.setSistema(c.getCanalSistema());
        req.setCorreo(c.getCanalCorreo());
        req.setWhatsapp(c.getCanalWhatsapp());
        return procesar(req, false, null, true);
    }

    @Transactional
    public RecordatorioMoraResultadoDTO previsualizar(RecordatorioMoraRequestDTO req) {
        return procesar(req, true, null, false);
    }

    @Transactional
    public RecordatorioMoraResultadoDTO enviar(RecordatorioMoraRequestDTO req, Integer usuarioId) {
        return procesar(req, false, usuarioId, false);
    }

    private RecordatorioMoraResultadoDTO procesar(RecordatorioMoraRequestDTO req, boolean simular,
                                                  Integer usuarioId, boolean automatico) {
        Set<String> canales = new LinkedHashSet<>();
        if (Boolean.TRUE.equals(req.getSistema())) canales.add(SISTEMA);
        if (Boolean.TRUE.equals(req.getCorreo())) canales.add(CORREO);
        if (Boolean.TRUE.equals(req.getWhatsapp())) canales.add(WHATSAPP);
        if (canales.isEmpty()) canales.add(SISTEMA); // el estandar
        boolean forzar = Boolean.TRUE.equals(req.getForzar());
        int dias = obtenerConfig().getDiasEntreAvisos();

        List<String> advertencias = new ArrayList<>();
        if (canales.contains(CORREO) && !correoSender.disponible()) {
            advertencias.add("El correo no esta disponible: falta configurar el SMTP del servidor.");
            canales.remove(CORREO);
        }

        LocalDate hoy = LocalDate.now(BOGOTA);
        List<FacturaResponseDTO> enMora = facturaRepository.findEnMora(hoy).stream()
                .map(facturaService::toResponseDTO)
                .filter(f -> req.getFacturaIds() == null || req.getFacturaIds().isEmpty()
                        || req.getFacturaIds().contains(f.getId()))
                .toList();

        // Ultimo aviso ENVIADO por (factura, canal) para respetar la frecuencia.
        Map<String, LocalDateTime> ultimo = new HashMap<>();
        if (!enMora.isEmpty() && !forzar) {
            List<Integer> ids = enMora.stream().map(FacturaResponseDTO::getId).toList();
            for (Object[] fila : envioRepository.ultimosEnvios(ids)) {
                ultimo.put(fila[0] + "|" + fila[1], (LocalDateTime) fila[2]);
            }
        }
        LocalDateTime limite = LocalDateTime.now(BOGOTA).minusDays(dias);

        Map<Integer, List<FacturaResponseDTO>> porPersona = new LinkedHashMap<>();
        for (FacturaResponseDTO f : enMora) {
            porPersona.computeIfAbsent(f.getPersona().getId(), k -> new ArrayList<>()).add(f);
        }

        Map<String, Integer> enviados = new LinkedHashMap<>();
        Map<String, Integer> fallidos = new LinkedHashMap<>();
        List<Aviso> avisos = new ArrayList<>();
        List<Enlace> enlaces = new ArrayList<>();
        int omitidos = 0;

        for (Map.Entry<Integer, List<FacturaResponseDTO>> e : porPersona.entrySet()) {
            Integer personaId = e.getKey();
            List<FacturaResponseDTO> facturas = e.getValue();
            FacturaResponseDTO.PersonaResumenDTO p = facturas.get(0).getPersona();
            String nombre = ((p.getNombre() == null ? "" : p.getNombre()) + " "
                    + (p.getApellidos() == null ? "" : p.getApellidos())).trim();
            Contactos ct = resolverContactos(personaId);
            List<CanalAviso> resultadoCanales = new ArrayList<>();
            List<FacturaResponseDTO> incluidas = new ArrayList<>();

            for (String canal : canales) {
                List<FacturaResponseDTO> elegibles = new ArrayList<>();
                for (FacturaResponseDTO f : facturas) {
                    LocalDateTime ult = ultimo.get(f.getId() + "|" + canal);
                    if (forzar || ult == null || ult.isBefore(limite)) {
                        elegibles.add(f);
                    } else {
                        omitidos++;
                    }
                }
                if (elegibles.isEmpty()) continue;
                for (FacturaResponseDTO f : elegibles) {
                    if (!incluidas.contains(f)) incluidas.add(f);
                }

                String texto = construirMensaje(nombre, elegibles);
                BigDecimal saldo = saldoTotal(elegibles);

                switch (canal) {
                    case SISTEMA -> {
                        if (ct.usuarios.isEmpty()) {
                            resultadoCanales.add(new CanalAviso(canal, null, "SIN_DESTINO",
                                    "Ni el deportista ni sus acudientes tienen usuario del portal"));
                        }
                        for (Usuario u : ct.usuarios) {
                            String estado = simular ? "PROGRAMADO" : "ENVIADO";
                            String detalle = null;
                            if (!simular) {
                                try {
                                    Notificacion n = new Notificacion();
                                    n.setUsuario(u);
                                    n.setMensaje(texto);
                                    notificacionRepository.save(n);
                                } catch (Exception ex) {
                                    estado = "FALLIDO";
                                    detalle = recortar(ex.getMessage());
                                }
                            }
                            registrar(simular, elegibles, personaId, canal, u.getUsername(), estado, detalle,
                                    usuarioId, automatico, enviados, fallidos);
                            resultadoCanales.add(new CanalAviso(canal, u.getUsername(), estado, detalle));
                        }
                    }
                    case CORREO -> {
                        if (ct.emails.isEmpty()) {
                            resultadoCanales.add(new CanalAviso(canal, null, "SIN_DESTINO",
                                    "No hay correo registrado (usuario o acudientes)"));
                        }
                        for (String email : ct.emails) {
                            String estado = simular ? "PROGRAMADO" : "ENVIADO";
                            String detalle = null;
                            if (!simular) {
                                try {
                                    correoSender.enviar(email,
                                            "Recordatorio de pago - " + elegibles.size() + " factura(s) vencida(s)", texto);
                                } catch (Exception ex) {
                                    estado = "FALLIDO";
                                    detalle = recortar(ex.getMessage());
                                    log.warn("Recordatorio de mora: fallo el correo a {}: {}", email, ex.getMessage());
                                }
                            }
                            registrar(simular, elegibles, personaId, canal, email, estado, detalle,
                                    usuarioId, automatico, enviados, fallidos);
                            resultadoCanales.add(new CanalAviso(canal, email, estado, detalle));
                        }
                    }
                    case WHATSAPP -> {
                        if (ct.telefonos.isEmpty()) {
                            resultadoCanales.add(new CanalAviso(canal, null, "SIN_DESTINO",
                                    "No hay telefono valido registrado"));
                        }
                        for (String tel : ct.telefonos) {
                            if (whatsAppSender.disponible()) {
                                String estado = simular ? "PROGRAMADO" : "ENVIADO";
                                String detalle = null;
                                if (!simular) {
                                    try {
                                        whatsAppSender.enviarPlantilla(tel, List.of(nombre,
                                                String.valueOf(elegibles.size()), dinero(saldo)));
                                    } catch (Exception ex) {
                                        estado = "FALLIDO";
                                        detalle = recortar(ex.getMessage());
                                        log.warn("Recordatorio de mora: fallo WhatsApp a {}: {}", tel, ex.getMessage());
                                    }
                                }
                                registrar(simular, elegibles, personaId, canal, tel, estado, detalle,
                                        usuarioId, automatico, enviados, fallidos);
                                resultadoCanales.add(new CanalAviso(canal, tel, estado, detalle));
                            } else {
                                // Sin API: enlace wa.me para enviarlo a mano. No se registra como enviado.
                                enlaces.add(new Enlace(nombre, tel,
                                        "https://wa.me/" + tel + "?text=" + URLEncoder.encode(texto, StandardCharsets.UTF_8)));
                                resultadoCanales.add(new CanalAviso(canal, tel, "MANUAL",
                                        "WhatsApp automatico no configurado: usa el enlace"));
                            }
                        }
                    }
                    default -> { }
                }
            }
            if (!resultadoCanales.isEmpty()) {
                avisos.add(new Aviso(personaId, nombre, incluidas.size(), saldoTotal(incluidas), resultadoCanales));
            }
        }

        if (canales.contains(WHATSAPP) && !whatsAppSender.disponible() && !enlaces.isEmpty()) {
            advertencias.add("WhatsApp automatico no esta configurado: se generaron enlaces para enviar cada mensaje a mano.");
        }
        return new RecordatorioMoraResultadoDTO(simular, enMora.size(), porPersona.size(), omitidos,
                enviados, fallidos, avisos, enlaces, advertencias);
    }

    // ---------- Apoyo ----------

    private record Contactos(List<Usuario> usuarios, Set<String> emails, Set<String> telefonos) {
    }

    /** Deportista (su usuario y telefono) + acudientes (usuario, correo y telefono), sin repetir. */
    private Contactos resolverContactos(Integer personaId) {
        Map<Integer, Usuario> usuarios = new LinkedHashMap<>();
        Set<String> emails = new LinkedHashSet<>();
        Set<String> telefonos = new LinkedHashSet<>();

        Persona persona = personaRepository.findById(personaId).orElse(null);
        if (persona != null) {
            userRepository.findByPersona(persona).ifPresent(u -> {
                usuarios.put(u.getId(), u);
                agregarEmail(emails, u.getEmail());
            });
            agregarTelefono(telefonos, persona.getTelefono());
        }
        deportistaRepository.findByPersonaId(personaId).ifPresent(d -> {
            for (var vinculo : vinculoRepository.findByDeportistaId(d.getId())) {
                Acudiente a = vinculo.getAcudiente();
                agregarEmail(emails, a.getEmail());
                agregarTelefono(telefonos, a.getTelefono());
                if (a.getUsuarioId() != null) {
                    userRepository.findById(a.getUsuarioId().longValue()).ifPresent(u -> {
                        usuarios.put(u.getId(), u);
                        agregarEmail(emails, u.getEmail());
                    });
                }
            }
        });
        return new Contactos(new ArrayList<>(usuarios.values()), emails, telefonos);
    }

    private static void agregarEmail(Set<String> set, String email) {
        if (email != null && EMAIL.matcher(email.trim()).matches()) {
            set.add(email.trim().toLowerCase(Locale.ROOT));
        }
    }

    /** Colombia: 10 digitos que empiezan por 3 -> se antepone 57. Devuelve formato internacional sin '+'. */
    private static void agregarTelefono(Set<String> set, String telefono) {
        if (telefono == null) return;
        String d = telefono.replaceAll("\\D", "");
        if (d.length() == 10 && d.startsWith("3")) d = "57" + d;
        if (d.length() == 12 && d.startsWith("57")) set.add(d);
        // Otros formatos no se asumen: es mejor no enviar que enviar a un numero equivocado.
    }

    private void registrar(boolean simular, List<FacturaResponseDTO> facturas, Integer personaId, String canal,
                           String destino, String estado, String detalle, Integer usuarioId, boolean automatico,
                           Map<String, Integer> enviados, Map<String, Integer> fallidos) {
        if (simular) return;
        for (FacturaResponseDTO f : facturas) {
            RecordatorioEnvio r = new RecordatorioEnvio();
            r.setFacturaId(f.getId());
            r.setPersonaId(personaId);
            r.setCanal(canal);
            r.setDestino(destino);
            r.setEstado(estado);
            r.setDetalle(detalle);
            r.setFecha(LocalDateTime.now(BOGOTA));
            r.setAutomatico(automatico);
            r.setEnviadoPorUsuarioId(usuarioId);
            envioRepository.save(r);
        }
        (estado.equals("ENVIADO") ? enviados : fallidos).merge(canal, 1, Integer::sum);
    }

    private static BigDecimal saldoTotal(List<FacturaResponseDTO> facturas) {
        return facturas.stream().map(f -> f.getSaldoPendiente() == null ? BigDecimal.ZERO : f.getSaldoPendiente())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String dinero(BigDecimal valor) {
        return String.format(Locale.forLanguageTag("es-CO"), "$%,.0f", valor);
    }

    private static String construirMensaje(String nombre, List<FacturaResponseDTO> facturas) {
        StringBuilder sb = new StringBuilder();
        sb.append("Recordatorio de pago: ").append(nombre).append(" tiene ").append(facturas.size())
                .append(facturas.size() == 1 ? " factura vencida" : " facturas vencidas")
                .append(" por un saldo de ").append(dinero(saldoTotal(facturas))).append(".\n");
        for (FacturaResponseDTO f : facturas) {
            sb.append("- ").append(f.getNumeroFactura());
            if (f.getDescripcion() != null && !f.getDescripcion().isBlank()) {
                sb.append(" (").append(f.getDescripcion()).append(")");
            }
            sb.append(": ").append(dinero(f.getSaldoPendiente() == null ? BigDecimal.ZERO : f.getSaldoPendiente()));
            if (f.getDiasMora() != null) sb.append(", ").append(f.getDiasMora()).append(" dias de mora");
            sb.append("\n");
        }
        sb.append("Si ya realizaste el pago, ignora este mensaje.");
        return sb.toString();
    }

    private static String recortar(String s) {
        if (s == null) return null;
        return s.length() > 300 ? s.substring(0, 300) : s;
    }
}
