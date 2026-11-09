package com.varcal.cheermanager.Service.Legal;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Models.Legal.PreRegistro;
import com.varcal.cheermanager.repository.Legal.PreRegistroRepository;
import com.varcal.cheermanager.tenant.TenantContext;

/**
 * Pre-registro publico desde la landing. Guarda la prueba del consentimiento (IP, fecha y version, como L1).
 * Defensas: campo trampa, limite por IP y correo repetido (se responde igual para no revelar quien ya esta).
 */
@Service
public class PreRegistroService {

    public static final int MAX_POR_IP = 5;
    public static final long VENTANA_MS = 60L * 60 * 1000;
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Set<String> ESTADOS = Set.of("NUEVO", "CONTACTADO", "DESCARTADO");
    private static final Set<String> TRAMPAS = Set.of("website", "sitio", "empresa_web", "hp", "honeypot", "company_url");

    private final PreRegistroRepository repository;
    private final Map<String, Deque<Long>> intentos = new ConcurrentHashMap<>();

    public PreRegistroService(PreRegistroRepository repository) {
        this.repository = repository;
    }

    protected LocalDateTime ahora() { return LocalDateTime.now(); }
    protected long ahoraMs() { return System.currentTimeMillis(); }

    /** @return true si el pre-registro se guardo; false si se descarto en silencio (bot o correo repetido). */
    public boolean registrar(Map<String, Object> body, String ip) {
        if (body == null) throw new IllegalArgumentException("Solicitud vacia");
        for (String t : TRAMPAS) {
            if (texto(body.get(t)) != null) return false; // un humano no ve ese campo
        }
        limitar(ip);

        String nombre = recortar(primero(body, "nombre", "name", "nombreCompleto"), 120);
        String correo = recortar(primero(body, "correo", "email"), 150);
        if (nombre == null || nombre.length() < 2) throw new IllegalArgumentException("Escribe tu nombre");
        if (correo == null || !CORREO.matcher(correo).matches()) throw new IllegalArgumentException("Escribe un correo valido");

        String vigente = LegalService.VERSIONES_VIGENTES.get(LegalService.POLITICA);
        if (!consentimiento(body)) throw new IllegalArgumentException("Debes aceptar el tratamiento de tus datos para pre-registrarte");
        String declarada = texto(body.get("versionConsentimiento") != null ? body.get("versionConsentimiento") : body.get("version"));
        if (declarada != null && !declarada.equals(vigente)) {
            throw new IllegalArgumentException("Los textos legales cambiaron. Recarga la pagina e intenta de nuevo.");
        }

        PreRegistro p = new PreRegistro();
        p.setNombre(nombre);
        p.setCorreo(correo.toLowerCase());
        p.setTelefono(recortar(primero(body, "telefono", "phone", "whatsapp", "celular"), 30));
        p.setClub(recortar(primero(body, "club", "nombreClub", "clubName"), 150));
        p.setCiudad(recortar(primero(body, "ciudad", "city"), 100));
        p.setDeportistas(entero(primero(body, "deportistas", "numDeportistas", "athletes")));
        p.setRol(recortar(primero(body, "rol", "role", "cargo"), 80));
        p.setMensaje(recortar(primero(body, "mensaje", "message", "comentarios"), 1000));
        p.setDatosJson(recortar(crudo(body), 4000));
        p.setVersionConsentimiento(vigente);
        p.setFechaConsentimiento(ahora());
        p.setIp(recortar(ip, 64));

        return TenantContext.<Boolean>runAs(Club.PLATAFORMA_ID, () -> {
            if (repository.existsByCorreoIgnoreCase(p.getCorreo())) return false;
            repository.save(p);
            return true;
        });
    }

    public List<PreRegistro> listar() {
        return TenantContext.<List<PreRegistro>>runAs(Club.PLATAFORMA_ID, repository::findAllByOrderByFechaConsentimientoDescIdDesc);
    }

    public PreRegistro actualizar(Integer id, String estado, String notas) {
        if (estado == null || !ESTADOS.contains(estado)) throw new IllegalArgumentException("Estado invalido: usa NUEVO, CONTACTADO o DESCARTADO");
        return TenantContext.<PreRegistro>runAs(Club.PLATAFORMA_ID, () -> {
            PreRegistro p = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Pre-registro no encontrado"));
            p.setEstado(estado);
            p.setNotas(recortar(notas, 500));
            return repository.save(p);
        });
    }

    private void limitar(String ip) {
        String clave = ip == null ? "?" : ip;
        long ahora = ahoraMs();
        Deque<Long> cola = intentos.computeIfAbsent(clave, k -> new ArrayDeque<>());
        synchronized (cola) {
            while (!cola.isEmpty() && ahora - cola.peekFirst() > VENTANA_MS) cola.pollFirst();
            if (cola.size() >= MAX_POR_IP) throw new TooManyRequests();
            cola.addLast(ahora);
        }
        if (intentos.size() > 5000) intentos.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    /** El controlador la traduce a 429. */
    public static class TooManyRequests extends RuntimeException {
        public TooManyRequests() { super("Demasiados intentos. Intenta de nuevo en una hora."); }
    }

    private static boolean consentimiento(Map<String, Object> b) {
        for (String k : new String[] { "consentimiento", "acepta", "aceptaPolitica", "consent" }) {
            Object v = b.get(k);
            if (Boolean.TRUE.equals(v) || "true".equalsIgnoreCase(String.valueOf(v))) return true;
        }
        return false;
    }

    private static String primero(Map<String, Object> b, String... claves) {
        for (String k : claves) {
            String t = texto(b.get(k));
            if (t != null) return t;
        }
        return null;
    }

    private static String texto(Object o) {
        if (o == null) return null;
        String s = String.valueOf(o).trim();
        return s.isEmpty() ? null : s;
    }

    private static String recortar(String s, int max) {
        if (s == null) return null;
        s = s.replaceAll("[\\p{Cntrl}&&[^\\n]]", "").trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static Integer entero(String s) {
        if (s == null) return null;
        try {
            int n = Integer.parseInt(s.replaceAll("\\D", ""));
            return n < 0 || n > 100000 ? null : n;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String crudo(Map<String, Object> b) {
        StringBuilder sb = new StringBuilder("{");
        b.entrySet().stream().limit(30).forEach(e -> {
            if (sb.length() > 1) sb.append(',');
            sb.append('"').append(esc(e.getKey())).append("\":\"").append(esc(String.valueOf(e.getValue()))).append('"');
        });
        return sb.append('}').toString();
    }

    private static String esc(String s) {
        String t = s.length() > 300 ? s.substring(0, 300) : s;
        return t.replace("\\", "\\\\").replace("\"", "\\\"").replaceAll("[\\p{Cntrl}]", " ");
    }
}
