package com.varcal.cheermanager.Service.Clubes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Models.Clubes.Interesado;
import com.varcal.cheermanager.Models.Clubes.Interesado.Estado;
import com.varcal.cheermanager.Models.Clubes.Interesado.Tipo;
import com.varcal.cheermanager.Service.Legal.LegalService;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.repository.Clubes.InteresadoRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.tenant.TenantContext;

/**
 * C6: pre-inscripcion publica por club. El club se identifica por su slug. Guarda la prueba del consentimiento
 * (IP, fecha, version) y ofrece clase de prueba, inscripcion y lista de espera con posicion.
 */
@Service
public class InteresadoService {

    public static final int MAX_POR_IP = 5;
    public static final long VENTANA_MS = 60L * 60 * 1000;
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$");
    private static final Set<String> TRAMPAS = Set.of("website", "sitio", "hp", "honeypot", "company_url");

    public record Actualizacion(Estado estado, Integer grupoId, LocalDate fechaPrueba, String notas) {
    }

    public record Fila(Integer id, Tipo tipo, Estado estado, String contactoNombre, String contactoCorreo,
            String contactoTelefono, String deportistaNombre, LocalDate deportistaNacimiento, int edad,
            String experiencia, String mensaje, Integer grupoId, LocalDate fechaPrueba, String notas,
            LocalDateTime fechaCreacion, Integer posicionEspera) {
    }

    public record Resumen(Map<Estado, Integer> porEstado, int enEspera, List<Fila> items) {
    }

    private final InteresadoRepository repo;
    private final ClubRepository clubes;
    private final GrupoEntrenamientoRepository grupos;
    private final Map<String, Deque<Long>> intentos = new ConcurrentHashMap<>();

    public InteresadoService(InteresadoRepository repo, ClubRepository clubes, GrupoEntrenamientoRepository grupos) {
        this.repo = repo;
        this.clubes = clubes;
        this.grupos = grupos;
    }

    protected LocalDate hoy() { return LocalDate.now(); }
    protected LocalDateTime ahora() { return LocalDateTime.now(); }
    protected long ahoraMs() { return System.currentTimeMillis(); }

    /** @return true si se guardo; false si se descarto en silencio (bot o repetido). */
    public boolean registrarPublico(String slug, Map<String, Object> body, String ip) {
        if (body == null) throw new IllegalArgumentException("Solicitud vacia");
        for (String t : TRAMPAS) if (texto(body.get(t)) != null) return false;
        limitar(ip);
        Club club = slug == null ? null : clubes.findBySlug(slug.toLowerCase()).orElse(null);
        if (club == null || !Boolean.TRUE.equals(club.getActivo()) || club.getId() == Club.PLATAFORMA_ID) {
            throw new IllegalArgumentException("Club no encontrado");
        }

        Tipo tipo = tipo(texto(body.get("tipo")));
        String contacto = recortar(texto(body.get("contactoNombre")), 120);
        String correo = recortar(texto(body.get("contactoCorreo")), 150);
        String tel = recortar(texto(body.get("contactoTelefono")), 30);
        String dep = recortar(texto(body.get("deportistaNombre")), 120);
        if (contacto == null || contacto.length() < 2) throw new IllegalArgumentException("Escribe tu nombre");
        if (correo == null || !CORREO.matcher(correo).matches()) throw new IllegalArgumentException("Escribe un correo valido");
        if (tel == null || tel.replaceAll("\\D", "").length() < 7) throw new IllegalArgumentException("Escribe un telefono de contacto");
        if (dep == null || dep.length() < 2) throw new IllegalArgumentException("Escribe el nombre del deportista");
        LocalDate nac = fecha(texto(body.get("deportistaNacimiento")));
        int edad = nac.until(hoy()).getYears();
        if (nac.isAfter(hoy()) || edad < 3 || edad > 60) throw new IllegalArgumentException("Revisa la fecha de nacimiento del deportista");

        String vigente = LegalService.VERSIONES_VIGENTES.get(LegalService.POLITICA);
        if (!Boolean.TRUE.equals(body.get("consentimiento")) && !"true".equalsIgnoreCase(String.valueOf(body.get("consentimiento")))) {
            throw new IllegalArgumentException("Debes autorizar el tratamiento de los datos para que el club te contacte");
        }
        String declarada = texto(body.get("versionConsentimiento"));
        if (declarada != null && !declarada.equals(vigente)) {
            throw new IllegalArgumentException("Los textos legales cambiaron. Recarga la pagina e intenta de nuevo.");
        }

        Interesado i = new Interesado();
        i.setTipo(tipo);
        i.setEstado(tipo == Tipo.LISTA_ESPERA ? Estado.EN_ESPERA : Estado.NUEVO);
        i.setContactoNombre(contacto);
        i.setContactoCorreo(correo.toLowerCase());
        i.setContactoTelefono(tel);
        i.setDeportistaNombre(dep);
        i.setDeportistaNacimiento(nac);
        i.setExperiencia(recortar(texto(body.get("experiencia")), 300));
        i.setMensaje(recortar(texto(body.get("mensaje")), 500));
        i.setVersionConsentimiento(vigente);
        i.setFechaConsentimiento(ahora());
        i.setIp(recortar(ip, 64));
        i.setFechaCreacion(ahora());
        i.setFechaActualizacion(ahora());
        return TenantContext.<Boolean>runAs(club.getId(), () -> {
            if (repo.existsByContactoCorreoIgnoreCaseAndDeportistaNombreIgnoreCase(i.getContactoCorreo(), dep)) return false;
            repo.save(i);
            return true;
        });
    }

    public Resumen listar() {
        LocalDate hoy = hoy();
        List<Interesado> todos = repo.findAllByOrderByFechaCreacionDescIdDesc();
        Map<Estado, Integer> por = new EnumMap<>(Estado.class);
        for (Estado e : Estado.values()) por.put(e, 0);
        for (Interesado i : todos) por.merge(i.getEstado(), 1, Integer::sum);

        // Posicion en la espera: orden de llegada dentro del mismo grupo (o la espera general si aun no tiene grupo).
        Map<Integer, Integer> posicion = new java.util.HashMap<>();
        Map<Object, Integer> contador = new java.util.HashMap<>();
        todos.stream().filter(i -> i.getEstado() == Estado.EN_ESPERA)
                .sorted(Comparator.comparing(Interesado::getFechaCreacion).thenComparing(Interesado::getId))
                .forEach(i -> posicion.put(i.getId(), contador.merge(i.getGrupoId() == null ? "general" : i.getGrupoId(), 1, Integer::sum)));

        List<Fila> filas = new ArrayList<>();
        for (Interesado i : todos) {
            filas.add(new Fila(i.getId(), i.getTipo(), i.getEstado(), i.getContactoNombre(), i.getContactoCorreo(),
                    i.getContactoTelefono(), i.getDeportistaNombre(), i.getDeportistaNacimiento(),
                    i.getDeportistaNacimiento().until(hoy).getYears(), i.getExperiencia(), i.getMensaje(), i.getGrupoId(),
                    i.getFechaPrueba(), i.getNotas(), i.getFechaCreacion(), posicion.get(i.getId())));
        }
        return new Resumen(por, por.get(Estado.EN_ESPERA), filas);
    }

    @Transactional
    public Fila actualizar(Integer id, Actualizacion a) {
        Interesado i = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Interesado no encontrado"));
        if (a == null) throw new IllegalArgumentException("Nada que actualizar");
        Estado nuevo = a.estado() == null ? i.getEstado() : a.estado();
        LocalDate prueba = a.fechaPrueba() != null ? a.fechaPrueba() : i.getFechaPrueba();
        if (a.fechaPrueba() != null && a.fechaPrueba().isBefore(hoy()) && nuevo == Estado.PRUEBA_AGENDADA) {
            throw new IllegalArgumentException("La clase de prueba no puede quedar en una fecha pasada");
        }
        if (nuevo == Estado.PRUEBA_AGENDADA && prueba == null) {
            throw new IllegalArgumentException("Indica la fecha de la clase de prueba");
        }
        if (a.grupoId() != null && !grupos.existsById(a.grupoId())) throw new IllegalArgumentException("Grupo no encontrado");
        i.setEstado(nuevo);
        if (a.grupoId() != null) i.setGrupoId(a.grupoId());
        if (a.fechaPrueba() != null) i.setFechaPrueba(a.fechaPrueba());
        if (a.notas() != null) i.setNotas(recortar(a.notas(), 500));
        i.setFechaActualizacion(ahora());
        repo.save(i);
        return listar().items().stream().filter(f -> Objects.equals(f.id(), id)).findFirst().orElseThrow();
    }

    private void limitar(String ip) {
        long ahora = ahoraMs();
        Deque<Long> cola = intentos.computeIfAbsent(ip == null ? "?" : ip, k -> new ArrayDeque<>());
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

    private static Tipo tipo(String t) {
        try {
            return Tipo.valueOf(t == null ? "" : t.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Elige si quieres clase de prueba, inscripcion o lista de espera");
        }
    }

    private static LocalDate fecha(String s) {
        try {
            return LocalDate.parse(s);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Escribe la fecha de nacimiento del deportista (aaaa-mm-dd)");
        }
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
}
