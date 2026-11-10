package com.varcal.cheermanager.Service.Horario_Asistencia;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Horario_Asistencia.AsignacionEspacio;
import com.varcal.cheermanager.Models.Horario_Asistencia.Espacio;
import com.varcal.cheermanager.Models.Horario_Asistencia.Espacio.Tipo;
import com.varcal.cheermanager.Models.Horario_Asistencia.HorarioEntrenamiento;
import com.varcal.cheermanager.Models.Horario_Asistencia.Sede;
import com.varcal.cheermanager.repository.Horario_Asistencia.AsignacionEspacioRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.EspacioRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioEntrenamientoRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.SedeRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;

/**
 * C11: sedes y espacios. Cada horario de entrenamiento de un grupo puede tener un espacio; un espacio admite
 * un numero maximo de grupos a la vez (grupos_simultaneos) y avisa si el grupo supera su capacidad.
 */
@Service
public class SedeService {

    public record SedeSolicitud(String nombre, String direccion, String ciudad, String notas, Boolean activa) {
    }

    public record EspacioSolicitud(String nombre, Tipo tipo, Integer capacidad, Integer gruposSimultaneos, Boolean activo) {
    }

    public record EspacioVista(Integer id, Integer sedeId, String nombre, Tipo tipo, Integer capacidad,
            int gruposSimultaneos, boolean activo, int usos) {
    }

    public record SedeVista(Integer id, String nombre, String direccion, String ciudad, String notas, boolean activa,
            List<EspacioVista> espacios) {
    }

    public record Uso(Integer asignacionId, Integer horarioEntrenamientoId, Integer grupoId, String grupo, int dia,
            String diaNombre, LocalTime horaInicio, LocalTime horaFin) {
    }

    public record Ocupacion(Integer espacioId, String espacio, String sede, int gruposSimultaneos, List<Uso> usos) {
    }

    public record SinEspacio(Integer horarioEntrenamientoId, Integer grupoId, String grupo, int dia, String diaNombre,
            LocalTime horaInicio, LocalTime horaFin) {
    }

    public record Tablero(List<Ocupacion> espacios, List<SinEspacio> sinEspacio) {
    }

    public record Asignacion(Integer asignacionId, Integer espacioId, List<String> advertencias) {
    }

    private final SedeRepository sedes;
    private final EspacioRepository espacios;
    private final AsignacionEspacioRepository asignaciones;
    private final HorarioEntrenamientoRepository horarios;
    private final DeportistaPerteneceGrupoRepository membresias;

    public SedeService(SedeRepository sedes, EspacioRepository espacios, AsignacionEspacioRepository asignaciones,
            HorarioEntrenamientoRepository horarios, DeportistaPerteneceGrupoRepository membresias) {
        this.sedes = sedes;
        this.espacios = espacios;
        this.asignaciones = asignaciones;
        this.horarios = horarios;
        this.membresias = membresias;
    }

    // ---------------- Sedes y espacios ----------------

    public List<SedeVista> listar() {
        Map<Integer, Integer> usos = new HashMap<>();
        for (AsignacionEspacio a : asignaciones.findAll()) usos.merge(a.getEspacioId(), 1, Integer::sum);
        List<SedeVista> out = new ArrayList<>();
        for (Sede s : sedes.findAllByOrderByNombreAsc()) {
            List<EspacioVista> es = espacios.findBySedeIdOrderByNombreAsc(s.getId()).stream().map(e -> vista(e, usos.getOrDefault(e.getId(), 0))).toList();
            out.add(new SedeVista(s.getId(), s.getNombre(), s.getDireccion(), s.getCiudad(), s.getNotas(), s.isActiva(), es));
        }
        return out;
    }

    @Transactional
    public Sede crearSede(SedeSolicitud s) {
        String nombre = texto(s == null ? null : s.nombre(), 2, 100, "El nombre");
        if (sedes.existsByNombreIgnoreCase(nombre)) throw new IllegalStateException("Ya existe una sede con ese nombre");
        Sede n = new Sede();
        n.setNombre(nombre);
        n.setDireccion(opcional(s.direccion(), 200));
        n.setCiudad(opcional(s.ciudad(), 80));
        n.setNotas(opcional(s.notas(), 300));
        return sedes.save(n);
    }

    @Transactional
    public Sede actualizarSede(Integer id, SedeSolicitud s) {
        Sede sede = sedes.findById(id).orElseThrow(() -> new IllegalArgumentException("Sede no encontrada"));
        if (s == null) throw new IllegalArgumentException("Nada que actualizar");
        if (s.nombre() != null) {
            String nombre = texto(s.nombre(), 2, 100, "El nombre");
            boolean otra = sedes.findAllByOrderByNombreAsc().stream().anyMatch(x -> !x.getId().equals(id) && x.getNombre().equalsIgnoreCase(nombre));
            if (otra) throw new IllegalStateException("Ya existe una sede con ese nombre");
            sede.setNombre(nombre);
        }
        if (s.direccion() != null) sede.setDireccion(opcional(s.direccion(), 200));
        if (s.ciudad() != null) sede.setCiudad(opcional(s.ciudad(), 80));
        if (s.notas() != null) sede.setNotas(opcional(s.notas(), 300));
        if (s.activa() != null) {
            if (!s.activa() && sede.isActiva()) {
                for (Espacio e : espacios.findBySedeIdOrderByNombreAsc(id)) {
                    if (!asignaciones.findByEspacioId(e.getId()).isEmpty()) {
                        throw new IllegalStateException("La sede tiene horarios asignados en \"" + e.getNombre() + "\": reasignalos antes de desactivarla");
                    }
                }
            }
            sede.setActiva(s.activa());
        }
        return sedes.save(sede);
    }

    @Transactional
    public Espacio crearEspacio(Integer sedeId, EspacioSolicitud s) {
        Sede sede = sedes.findById(sedeId).orElseThrow(() -> new IllegalArgumentException("Sede no encontrada"));
        if (!sede.isActiva()) throw new IllegalStateException("La sede esta desactivada");
        String nombre = texto(s == null ? null : s.nombre(), 2, 100, "El nombre");
        if (s.tipo() == null) throw new IllegalArgumentException("Indica el tipo de espacio");
        if (espacios.existsBySedeIdAndNombreIgnoreCase(sedeId, nombre)) throw new IllegalStateException("Ya existe un espacio con ese nombre en la sede");
        Espacio e = new Espacio();
        e.setSedeId(sedeId);
        e.setNombre(nombre);
        e.setTipo(s.tipo());
        e.setCapacidad(capacidad(s.capacidad()));
        e.setGruposSimultaneos(simultaneos(s.gruposSimultaneos() == null ? 1 : s.gruposSimultaneos()));
        return espacios.save(e);
    }

    @Transactional
    public Espacio actualizarEspacio(Integer id, EspacioSolicitud s) {
        Espacio e = espacios.findById(id).orElseThrow(() -> new IllegalArgumentException("Espacio no encontrado"));
        if (s == null) throw new IllegalArgumentException("Nada que actualizar");
        List<Uso> usos = usosDe(id);
        if (s.nombre() != null) {
            String nombre = texto(s.nombre(), 2, 100, "El nombre");
            boolean otro = espacios.findBySedeIdOrderByNombreAsc(e.getSedeId()).stream().anyMatch(x -> !x.getId().equals(id) && x.getNombre().equalsIgnoreCase(nombre));
            if (otro) throw new IllegalStateException("Ya existe un espacio con ese nombre en la sede");
            e.setNombre(nombre);
        }
        if (s.tipo() != null) e.setTipo(s.tipo());
        if (s.capacidad() != null) e.setCapacidad(capacidad(s.capacidad()));
        if (s.gruposSimultaneos() != null) {
            int nuevo = simultaneos(s.gruposSimultaneos());
            if (maxSimultaneos(usos) > nuevo) {
                throw new IllegalArgumentException("Hoy ya hay " + maxSimultaneos(usos) + " grupos a la vez en este espacio: reasigna antes de bajar el limite");
            }
            e.setGruposSimultaneos(nuevo);
        }
        if (s.activo() != null) {
            if (!s.activo() && !usos.isEmpty()) throw new IllegalStateException("El espacio tiene horarios asignados: reasignalos antes de desactivarlo");
            e.setActivo(s.activo());
        }
        return espacios.save(e);
    }

    // ---------------- Asignacion de espacio a horarios ----------------

    @Transactional
    public Asignacion asignar(Integer horarioEntrenamientoId, Integer espacioId) {
        HorarioEntrenamiento he = horarios.findById(horarioEntrenamientoId).orElseThrow(() -> new IllegalArgumentException("Horario de grupo no encontrado"));
        Espacio e = espacios.findById(espacioId).orElseThrow(() -> new IllegalArgumentException("Espacio no encontrado"));
        Sede sede = sedes.findById(e.getSedeId()).orElseThrow(() -> new IllegalArgumentException("Sede no encontrada"));
        if (!e.isActivo() || !sede.isActiva()) throw new IllegalStateException("El espacio o su sede estan desactivados");
        LocalTime ini = he.getHorario().getHoraInicio();
        LocalTime fin = he.getHorario().getHoraFin();
        if (ini == null || fin == null) throw new IllegalArgumentException("El horario no tiene hora de inicio y de fin");

        List<String> choques = new ArrayList<>();
        for (Uso u : usosDe(espacioId)) {
            if (u.horarioEntrenamientoId().equals(horarioEntrenamientoId)) continue; // reasignacion del mismo horario
            if (u.dia() == dia(he) && ini.isBefore(u.horaFin()) && u.horaInicio().isBefore(fin)) choques.add(u.grupo());
        }
        if (choques.size() >= e.getGruposSimultaneos()) {
            throw new IllegalStateException("\"" + e.getNombre() + "\" ya esta ocupado en ese horario por: " + String.join(", ", choques)
                    + (e.getGruposSimultaneos() > 1 ? " (limite " + e.getGruposSimultaneos() + " grupos a la vez)" : ""));
        }
        List<String> avisos = new ArrayList<>();
        if (e.getCapacidad() != null) {
            int miembros = membresias.findByGrupoIdAndFechaFinIsNull(he.getGrupoEntrenamiento().getId()).size();
            if (miembros > e.getCapacidad()) avisos.add("El grupo tiene " + miembros + " deportistas y el espacio admite " + e.getCapacidad());
        }
        AsignacionEspacio a = asignaciones.findByHorarioEntrenamientoId(horarioEntrenamientoId).orElseGet(AsignacionEspacio::new);
        a.setHorarioEntrenamientoId(horarioEntrenamientoId);
        a.setEspacioId(espacioId);
        asignaciones.save(a);
        return new Asignacion(a.getId(), espacioId, avisos);
    }

    @Transactional
    public void quitarAsignacion(Integer horarioEntrenamientoId) {
        AsignacionEspacio a = asignaciones.findByHorarioEntrenamientoId(horarioEntrenamientoId)
                .orElseThrow(() -> new IllegalArgumentException("Ese horario no tiene espacio asignado"));
        asignaciones.deleteById(a.getId());
    }

    /** Vista semanal: ocupacion por espacio y horarios de grupo que aun no tienen espacio. */
    public Tablero tablero() {
        Map<Integer, Sede> porSede = new HashMap<>();
        for (Sede s : sedes.findAllByOrderByNombreAsc()) porSede.put(s.getId(), s);
        List<Ocupacion> out = new ArrayList<>();
        for (Sede s : sedes.findAllByOrderByNombreAsc()) {
            for (Espacio e : espacios.findBySedeIdOrderByNombreAsc(s.getId())) {
                if (!e.isActivo()) continue;
                out.add(new Ocupacion(e.getId(), e.getNombre(), s.getNombre(), e.getGruposSimultaneos(), usosDe(e.getId())));
            }
        }
        java.util.Set<Integer> asignados = new java.util.HashSet<>();
        for (AsignacionEspacio a : asignaciones.findAll()) asignados.add(a.getHorarioEntrenamientoId());
        List<SinEspacio> sin = new ArrayList<>();
        for (HorarioEntrenamiento he : horarios.findAll()) {
            if (asignados.contains(he.getId())) continue;
            sin.add(new SinEspacio(he.getId(), he.getGrupoEntrenamiento().getId(), he.getGrupoEntrenamiento().getNombre(),
                    dia(he), he.getHorario().getDia().getDia(), he.getHorario().getHoraInicio(), he.getHorario().getHoraFin()));
        }
        sin.sort(Comparator.comparingInt(SinEspacio::dia).thenComparing(SinEspacio::horaInicio, Comparator.nullsLast(Comparator.naturalOrder())));
        return new Tablero(out, sin);
    }

    // ---------------- Internos ----------------

    private List<Uso> usosDe(Integer espacioId) {
        List<Uso> usos = new ArrayList<>();
        for (AsignacionEspacio a : asignaciones.findByEspacioId(espacioId)) {
            HorarioEntrenamiento he = horarios.findById(a.getHorarioEntrenamientoId()).orElse(null);
            if (he == null || he.getHorario().getHoraInicio() == null || he.getHorario().getHoraFin() == null) continue;
            usos.add(new Uso(a.getId(), he.getId(), he.getGrupoEntrenamiento().getId(), he.getGrupoEntrenamiento().getNombre(), dia(he),
                    he.getHorario().getDia().getDia(), he.getHorario().getHoraInicio(), he.getHorario().getHoraFin()));
        }
        usos.sort(Comparator.comparingInt(Uso::dia).thenComparing(Uso::horaInicio));
        return usos;
    }

    /** Maximo de grupos que coinciden a la vez entre los usos dados. */
    static int maxSimultaneos(List<Uso> usos) {
        int max = usos.isEmpty() ? 0 : 1;
        for (Uso a : usos) {
            int c = 0;
            for (Uso b : usos) {
                if (a.dia() == b.dia() && a.horaInicio().isBefore(b.horaFin()) && b.horaInicio().isBefore(a.horaFin())) c++;
            }
            max = Math.max(max, c);
        }
        return max;
    }

    private static int dia(HorarioEntrenamiento he) {
        return he.getHorario().getDia() == null ? 0 : he.getHorario().getDia().getId();
    }

    private static EspacioVista vista(Espacio e, int usos) {
        return new EspacioVista(e.getId(), e.getSedeId(), e.getNombre(), e.getTipo(), e.getCapacidad(), e.getGruposSimultaneos(), e.isActivo(), usos);
    }

    private static Integer capacidad(Integer c) {
        if (c == null) return null;
        if (c < 1 || c > 1000) throw new IllegalArgumentException("La capacidad va de 1 a 1000");
        return c;
    }

    private static int simultaneos(int n) {
        if (n < 1 || n > 10) throw new IllegalArgumentException("Los grupos simultaneos van de 1 a 10");
        return n;
    }

    private static String texto(String s, int min, int max, String campo) {
        String t = s == null ? "" : s.replaceAll("[\\p{Cntrl}]", "").trim();
        if (t.length() < min) throw new IllegalArgumentException(campo + " es obligatorio (minimo " + min + " caracteres)");
        if (t.length() > max) throw new IllegalArgumentException(campo + " admite maximo " + max + " caracteres");
        return t;
    }

    private static String opcional(String s, int max) {
        if (s == null || s.isBlank()) return null;
        String t = s.replaceAll("[\\p{Cntrl}&&[^\\n]]", "").trim();
        return t.length() > max ? t.substring(0, max) : t;
    }
}
