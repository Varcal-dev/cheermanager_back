package com.varcal.cheermanager.Service.Notificaciones;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Auth.Usuario;
import com.varcal.cheermanager.Models.Funcionalidad.Notificacion;
import com.varcal.cheermanager.Models.Notificaciones.Comunicado;
import com.varcal.cheermanager.Models.Notificaciones.ComunicadoEnvio;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Acudiente;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Auth.UserRepository;
import com.varcal.cheermanager.repository.Funcionalidad.NotificacionRepository;
import com.varcal.cheermanager.repository.Notificaciones.ComunicadoEnvioRepository;
import com.varcal.cheermanager.repository.Notificaciones.ComunicadoRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaAcudienteRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

/** C7: comunicados del club a las familias de un grupo (o de todo el club), por portal y por correo. */
@Service
public class ComunicadoService {

    static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    static final int MAX_CORREOS = 500;

    public record Solicitud(Integer grupoId, String asunto, String mensaje, Boolean sistema, Boolean correo) {
    }

    public record Destinatarios(Integer grupoId, String grupo, int deportistas, int correos, int usuariosPortal,
            int sinContacto, boolean correoDisponible) {
    }

    public record Resultado(Integer comunicadoId, int deportistas, int notificados, int correosEnviados,
            int correosFallidos, int sinContacto, List<String> fallidos) {
    }

    private record Audiencia(List<Deportista> deportistas, Set<String> correos, Map<Integer, Usuario> usuarios,
            int sinContacto) {
    }

    private final ComunicadoRepository comunicados;
    private final ComunicadoEnvioRepository envios;
    private final GrupoEntrenamientoRepository grupos;
    private final DeportistaPerteneceGrupoRepository membresias;
    private final DeportistaRepository deportistas;
    private final DeportistaAcudienteRepository vinculos;
    private final UserRepository usuarios;
    private final NotificacionRepository notificaciones;
    private final CorreoSender correo;

    public ComunicadoService(ComunicadoRepository comunicados, ComunicadoEnvioRepository envios,
            GrupoEntrenamientoRepository grupos, DeportistaPerteneceGrupoRepository membresias,
            DeportistaRepository deportistas, DeportistaAcudienteRepository vinculos, UserRepository usuarios,
            NotificacionRepository notificaciones, CorreoSender correo) {
        this.comunicados = comunicados;
        this.envios = envios;
        this.grupos = grupos;
        this.membresias = membresias;
        this.deportistas = deportistas;
        this.vinculos = vinculos;
        this.usuarios = usuarios;
        this.notificaciones = notificaciones;
        this.correo = correo;
    }

    protected LocalDateTime ahora() {
        return LocalDateTime.now();
    }

    /** Cuantas personas recibirian el comunicado, sin enviar nada. */
    public Destinatarios previsualizar(Integer grupoId) {
        String nombre = grupoId == null ? "Todo el club" : grupo(grupoId).getNombre();
        Audiencia a = audiencia(grupoId);
        return new Destinatarios(grupoId, nombre, a.deportistas().size(), a.correos().size(), a.usuarios().size(),
                a.sinContacto(), correo.disponible());
    }

    @Transactional
    public Resultado enviar(Solicitud s, int usuarioId) {
        String asunto = s.asunto() == null ? "" : s.asunto().trim();
        String mensaje = s.mensaje() == null ? "" : s.mensaje().trim();
        boolean porSistema = Boolean.TRUE.equals(s.sistema());
        boolean porCorreo = Boolean.TRUE.equals(s.correo());
        if (asunto.length() < 3 || asunto.length() > 150) {
            throw new IllegalArgumentException("El asunto debe tener entre 3 y 150 caracteres");
        }
        if (mensaje.length() < 5 || mensaje.length() > 3000) {
            throw new IllegalArgumentException("El mensaje debe tener entre 5 y 3000 caracteres");
        }
        if (!porSistema && !porCorreo) {
            throw new IllegalArgumentException("Elige al menos un canal: portal o correo");
        }
        if (porCorreo && !correo.disponible()) {
            throw new IllegalStateException("El correo (SMTP) no esta configurado en el servidor");
        }
        String nombreGrupo = s.grupoId() == null ? null : grupo(s.grupoId()).getNombre();
        Audiencia a = audiencia(s.grupoId());
        if (a.deportistas().isEmpty()) {
            throw new IllegalStateException("No hay deportistas activos en el destino elegido");
        }
        if (porCorreo && a.correos().size() > MAX_CORREOS) {
            throw new IllegalArgumentException("Son " + a.correos().size() + " correos; el maximo por envio es " + MAX_CORREOS
                    + ". Envia por grupos.");
        }

        Comunicado c = new Comunicado();
        c.setGrupoId(s.grupoId());
        c.setAsunto(asunto);
        c.setMensaje(mensaje);
        c.setCanalSistema(porSistema);
        c.setCanalCorreo(porCorreo);
        c.setDeportistas(a.deportistas().size());
        c.setSinContacto(a.sinContacto());
        c.setCreadoPor(usuarioId);
        c.setFechaCreacion(ahora());
        c = comunicados.save(c);

        List<ComunicadoEnvio> registro = new ArrayList<>();
        int notificados = 0, enviados = 0, fallidos = 0;
        List<String> correosFallidos = new ArrayList<>();
        String encabezado = nombreGrupo == null ? "" : "[" + nombreGrupo + "] ";

        if (porSistema) {
            for (Usuario u : a.usuarios().values()) {
                Notificacion n = new Notificacion();
                n.setUsuario(u);
                n.setMensaje(asunto + "\n" + mensaje);
                notificaciones.save(n);
                notificados++;
                registro.add(envio(c.getId(), "SISTEMA", u.getUsername(), "ENVIADO", null));
            }
        }
        if (porCorreo) {
            String texto = mensaje + "\n\n--\nMensaje del club enviado a traves de CheerManager.";
            for (String destino : a.correos()) {
                try {
                    correo.enviar(destino, encabezado + asunto, texto);
                    enviados++;
                    registro.add(envio(c.getId(), "CORREO", destino, "ENVIADO", null));
                } catch (RuntimeException e) {
                    fallidos++;
                    correosFallidos.add(destino);
                    String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                    registro.add(envio(c.getId(), "CORREO", destino, "FALLIDO", m.length() > 200 ? m.substring(0, 200) : m));
                }
            }
        }
        c.setNotificados(notificados);
        c.setCorreosEnviados(enviados);
        c.setCorreosFallidos(fallidos);
        comunicados.save(c);
        envios.saveAll(registro);
        return new Resultado(c.getId(), a.deportistas().size(), notificados, enviados, fallidos, a.sinContacto(), correosFallidos);
    }

    public List<Comunicado> historial() {
        return comunicados.findTop50ByOrderByFechaCreacionDescIdDesc();
    }

    public List<ComunicadoEnvio> detalle(Integer comunicadoId) {
        return envios.findByComunicadoIdOrderByIdAsc(comunicadoId);
    }

    // ---- internos ----

    private GrupoEntrenamiento grupo(Integer id) {
        return grupos.findById(id).orElseThrow(() -> new IllegalArgumentException("Grupo no encontrado"));
    }

    /** Deportistas activos del grupo (o del club) y, por cada uno, su usuario y el de sus acudientes + correos. */
    private Audiencia audiencia(Integer grupoId) {
        List<Deportista> lista = new ArrayList<>();
        if (grupoId == null) {
            lista.addAll(deportistas.findAll());
        } else {
            for (var m : membresias.findByGrupoIdAndFechaFinIsNull(grupoId)) {
                if (m.getDeportista() != null) {
                    lista.add(m.getDeportista());
                }
            }
        }
        lista.removeIf(d -> d.getEstado() == null || !"Activo".equalsIgnoreCase(d.getEstado().getEstado()));
        Set<String> correos = new LinkedHashSet<>();
        Map<Integer, Usuario> usuariosPortal = new LinkedHashMap<>();
        int sinContacto = 0;
        for (Deportista d : lista) {
            boolean tenia = false;
            if (d.getPersona() != null) {
                var u = usuarios.findByPersona(d.getPersona());
                if (u.isPresent()) {
                    usuariosPortal.putIfAbsent(u.get().getId(), u.get());
                    tenia |= agregarCorreo(correos, u.get().getEmail());
                    tenia = true;
                }
            }
            for (var v : vinculos.findByDeportistaId(d.getId())) {
                Acudiente a = v.getAcudiente();
                if (a == null) {
                    continue;
                }
                tenia |= agregarCorreo(correos, a.getEmail());
                if (a.getUsuarioId() != null) {
                    var u = usuarios.findById(a.getUsuarioId().longValue());
                    if (u.isPresent()) {
                        usuariosPortal.putIfAbsent(u.get().getId(), u.get());
                        tenia |= agregarCorreo(correos, u.get().getEmail());
                        tenia = true;
                    }
                }
            }
            if (!tenia) {
                sinContacto++;
            }
        }
        return new Audiencia(lista, correos, usuariosPortal, sinContacto);
    }

    private static boolean agregarCorreo(Set<String> set, String email) {
        if (email != null && EMAIL.matcher(email.trim()).matches()) {
            set.add(email.trim().toLowerCase(Locale.ROOT));
            return true;
        }
        return false;
    }

    private static ComunicadoEnvio envio(Integer comunicadoId, String canal, String destino, String estado, String detalle) {
        ComunicadoEnvio e = new ComunicadoEnvio();
        e.setComunicadoId(comunicadoId);
        e.setCanal(canal);
        e.setDestino(destino.length() > 160 ? destino.substring(0, 160) : destino);
        e.setEstado(estado);
        e.setDetalle(detalle);
        return e;
    }
}
