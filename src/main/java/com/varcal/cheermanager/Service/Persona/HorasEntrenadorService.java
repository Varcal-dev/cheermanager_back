package com.varcal.cheermanager.Service.Persona;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Horario_Asistencia.HorarioEntrenamiento;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenador;
import com.varcal.cheermanager.Models.Personas.Entrenador;
import com.varcal.cheermanager.Models.Personas.HoraEntrenador;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioEntrenamientoRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenadorRepository;
import com.varcal.cheermanager.repository.Personas.EntrenadorRepository;
import com.varcal.cheermanager.repository.Personas.HoraEntrenadorRepository;

/** C10: horas dictadas por entrenador (manuales o precargadas desde los horarios de sus grupos). */
@Service
public class HorasEntrenadorService {

    static final BigDecimal MAX_HORAS_DIA = new BigDecimal("12");
    static final int MAX_DIAS_PRECARGA = 62;

    public record Solicitud(Integer entrenadorId, LocalDate fecha, BigDecimal horas, Integer grupoId,
            String descripcion) {
    }

    public record Registro(Integer id, Integer entrenadorId, String entrenador, Integer grupoId, LocalDate fecha,
            BigDecimal horas, String descripcion, String origen) {
    }

    public record TotalEntrenador(Integer entrenadorId, String entrenador, BigDecimal horas, int sesiones) {
    }

    public record Periodo(LocalDate desde, LocalDate hasta, BigDecimal totalHoras, List<TotalEntrenador> totales,
            List<Registro> registros) {
    }

    public record Precarga(int creadas, int omitidas, BigDecimal horas) {
    }

    private final HoraEntrenadorRepository repo;
    private final EntrenadorRepository entrenadorRepository;
    private final GrupoEntrenadorRepository grupoEntrenadorRepository;
    private final HorarioEntrenamientoRepository horarioEntrenamientoRepository;

    public HorasEntrenadorService(HoraEntrenadorRepository repo, EntrenadorRepository entrenadorRepository,
            GrupoEntrenadorRepository grupoEntrenadorRepository,
            HorarioEntrenamientoRepository horarioEntrenamientoRepository) {
        this.repo = repo;
        this.entrenadorRepository = entrenadorRepository;
        this.grupoEntrenadorRepository = grupoEntrenadorRepository;
        this.horarioEntrenamientoRepository = horarioEntrenamientoRepository;
    }

    protected LocalDate hoy() {
        return LocalDate.now();
    }

    @Transactional
    public Registro registrar(Solicitud s, int usuarioId) {
        if (s.entrenadorId() == null || entrenadorRepository.findById(s.entrenadorId()).isEmpty()) {
            throw new IllegalArgumentException("Entrenador no encontrado");
        }
        if (s.fecha() == null || s.fecha().isAfter(hoy())) {
            throw new IllegalArgumentException("La fecha es obligatoria y no puede ser futura");
        }
        BigDecimal horas = s.horas() == null ? null : s.horas().setScale(2, RoundingMode.HALF_UP);
        if (horas == null || horas.signum() <= 0 || horas.compareTo(MAX_HORAS_DIA) > 0) {
            throw new IllegalArgumentException("Las horas deben estar entre 0.01 y 12");
        }
        String desc = s.descripcion() == null || s.descripcion().isBlank() ? null : s.descripcion().trim();
        if (desc != null && desc.length() > 200) {
            throw new IllegalArgumentException("La descripcion no puede superar 200 caracteres");
        }
        HoraEntrenador h = nueva(s.entrenadorId(), s.grupoId(), s.fecha(), horas, desc, "MANUAL", usuarioId);
        return registro(repo.save(h), nombre(entrenadorRepository.findById(s.entrenadorId()).get()));
    }

    @Transactional
    public void eliminar(Integer id) {
        if (repo.findById(id).isEmpty()) {
            throw new IllegalArgumentException("Registro no encontrado");
        }
        repo.deleteById(id);
    }

    public Periodo periodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("Indica un periodo valido (desde <= hasta)");
        }
        Map<Integer, String> nombres = new HashMap<>();
        for (Entrenador e : entrenadorRepository.findAll()) {
            nombres.put(e.getId(), nombre(e));
        }
        List<Registro> registros = new ArrayList<>();
        Map<Integer, BigDecimal> horas = new LinkedHashMap<>();
        Map<Integer, Integer> sesiones = new HashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        for (HoraEntrenador h : repo.findByFechaBetweenOrderByFechaDescIdDesc(desde, hasta)) {
            registros.add(registro(h, nombres.getOrDefault(h.getEntrenadorId(), "#" + h.getEntrenadorId())));
            horas.merge(h.getEntrenadorId(), h.getHoras(), BigDecimal::add);
            sesiones.merge(h.getEntrenadorId(), 1, Integer::sum);
            total = total.add(h.getHoras());
        }
        List<TotalEntrenador> totales = horas.entrySet().stream()
                .map(en -> new TotalEntrenador(en.getKey(), nombres.getOrDefault(en.getKey(), "#" + en.getKey()),
                        en.getValue(), sesiones.get(en.getKey())))
                .sorted((a, b) -> b.horas().compareTo(a.horas()))
                .toList();
        return new Periodo(desde, hasta, total, totales, registros);
    }

    /**
     * Crea las horas que corresponden a los horarios de los grupos de cada entrenador activo en el rango. No
     * duplica: si ya hay un registro del mismo entrenador, grupo y fecha, lo omite. Despues se corrige a mano
     * lo que no se dicto (una clase cancelada, un reemplazo).
     */
    @Transactional
    public Precarga precargar(LocalDate desde, LocalDate hasta, int usuarioId) {
        LocalDate hoy = hoy();
        if (desde == null || hasta == null || hasta.isBefore(desde)) {
            throw new IllegalArgumentException("Indica un periodo valido (desde <= hasta)");
        }
        if (hasta.isAfter(hoy)) {
            throw new IllegalArgumentException("No se precargan fechas futuras");
        }
        if (desde.plusDays(MAX_DIAS_PRECARGA).isBefore(hasta)) {
            throw new IllegalArgumentException("El rango maximo es de " + MAX_DIAS_PRECARGA + " dias");
        }
        Set<String> existentes = new HashSet<>();
        for (HoraEntrenador h : repo.findByFechaBetweenOrderByFechaDescIdDesc(desde, hasta)) {
            existentes.add(h.getEntrenadorId() + "|" + h.getGrupoId() + "|" + h.getFecha());
        }
        Map<Integer, List<HorarioEntrenamiento>> horariosDeGrupo = new HashMap<>();
        List<GrupoEntrenador> asignaciones = grupoEntrenadorRepository.findAll();
        int creadas = 0, omitidas = 0;
        BigDecimal total = BigDecimal.ZERO;
        for (GrupoEntrenador ge : asignaciones) {
            Entrenador e = ge.getEntrenador();
            if (e == null || e.getEstado() == null || !"Activo".equalsIgnoreCase(e.getEstado().getEstado())
                    || ge.getGrupo() == null) {
                continue;
            }
            List<HorarioEntrenamiento> horarios = horariosDeGrupo.computeIfAbsent(ge.getGrupo().getId(),
                    horarioEntrenamientoRepository::findByGrupoEntrenamientoId);
            for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
                if ((ge.getFechaInicio() != null && d.isBefore(ge.getFechaInicio()))
                        || (ge.getFechaFin() != null && d.isAfter(ge.getFechaFin()))) {
                    continue;
                }
                BigDecimal delDia = BigDecimal.ZERO;
                for (HorarioEntrenamiento he : horarios) {
                    var hr = he.getHorario();
                    if (hr.getDia() != null && hr.getDia().getId() == d.getDayOfWeek().getValue()
                            && hr.getHoraInicio() != null && hr.getHoraFin() != null
                            && hr.getHoraFin().isAfter(hr.getHoraInicio())) {
                        long min = Duration.between(hr.getHoraInicio(), hr.getHoraFin()).toMinutes();
                        delDia = delDia.add(BigDecimal.valueOf(min).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP));
                    }
                }
                if (delDia.signum() == 0) {
                    continue;
                }
                String clave = e.getId() + "|" + ge.getGrupo().getId() + "|" + d;
                if (!existentes.add(clave)) {
                    omitidas++;
                    continue;
                }
                repo.save(nueva(e.getId(), ge.getGrupo().getId(), d, delDia, "Horario del grupo " + ge.getGrupo().getNombre(),
                        "HORARIO", usuarioId));
                creadas++;
                total = total.add(delDia);
            }
        }
        return new Precarga(creadas, omitidas, total);
    }

    private HoraEntrenador nueva(Integer entrenadorId, Integer grupoId, LocalDate fecha, BigDecimal horas,
            String desc, String origen, int usuarioId) {
        HoraEntrenador h = new HoraEntrenador();
        h.setEntrenadorId(entrenadorId);
        h.setGrupoId(grupoId);
        h.setFecha(fecha);
        h.setHoras(horas);
        h.setDescripcion(desc);
        h.setOrigen(origen);
        h.setRegistradoPor(usuarioId);
        return h;
    }

    private static Registro registro(HoraEntrenador h, String nombre) {
        return new Registro(h.getId(), h.getEntrenadorId(), nombre, h.getGrupoId(), h.getFecha(), h.getHoras(),
                h.getDescripcion(), h.getOrigen());
    }

    private static String nombre(Entrenador e) {
        Persona p = e.getPersona();
        return p.getNombre() + " " + p.getApellidos();
    }
}
