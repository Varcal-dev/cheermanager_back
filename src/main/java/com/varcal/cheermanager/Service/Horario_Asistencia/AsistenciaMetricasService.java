package com.varcal.cheermanager.Service.Horario_Asistencia;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Horario_Asistencia.Asistencia;
import com.varcal.cheermanager.Models.Horario_Asistencia.HorarioEntrenamiento;
import com.varcal.cheermanager.repository.Horario_Asistencia.AsistenciaRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioEntrenamientoRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;

/**
 * Metricas detalladas de asistencia para la pantalla Asistencias (mas completas que el resumen del inicio de Horarios).
 * "Asistio" = estado Presente, igual que el resto del sistema. Se calcula en memoria: el rango es de semanas/meses
 * y un club tiene decenas o pocos cientos de deportistas.
 */
@Service
public class AsistenciaMetricasService {

    private static final String ESTADO_PRESENTE = "Presente";
    private static final double UMBRAL_RIESGO = 70.0;
    private static final int MIN_REGISTROS = 3; // menos que esto no es estadistica, es anecdota
    private static final int TOP = 8;
    private static final String[] DIAS = { "", "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo" };

    public record PorEstado(String estado, long total, Double porcentaje) {
    }

    public record PorGrupo(Integer grupoId, String nombre, long total, long presentes, Double porcentaje, long sesiones) {
    }

    public record PorDia(int dia, String nombre, long total, long presentes, Double porcentaje) {
    }

    public record Semana(LocalDate inicio, long total, long presentes, Double porcentaje) {
    }

    public record PorDeportista(Integer deportistaId, String nombre, long total, long presentes, Double porcentaje,
            int ausenciasSeguidas) {
    }

    public record Metricas(LocalDate desde, LocalDate hasta, long totalRegistros, long presentes, Double porcentaje,
            long sesiones, long deportistas, List<PorEstado> porEstado, List<PorGrupo> porGrupo, List<PorDia> porDiaSemana,
            List<Semana> tendenciaSemanal, List<PorDeportista> enRiesgo, List<PorDeportista> masConstantes) {
    }

    @Autowired
    private AsistenciaRepository asistencias;
    @Autowired
    private HorarioEntrenamientoRepository horariosGrupo;
    @Autowired
    private DeportistaPerteneceGrupoRepository membresias;

    private static Double pct(long parte, long total) {
        return total > 0 ? Math.round(parte * 10000.0 / total) / 100.0 : null;
    }

    private record GrupoRef(Integer id, String nombre) {
    }

    private static class Acum {
        long total;
        long presentes;

        void add(boolean presente) {
            total++;
            if (presente) {
                presentes++;
            }
        }
    }

    @Transactional(readOnly = true)
    public Metricas calcular(LocalDate desde, LocalDate hasta, Integer grupoId) {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la de fin");
        }
        // horario -> grupos que lo usan
        Map<Integer, List<GrupoRef>> gruposDeHorario = new HashMap<>();
        for (HorarioEntrenamiento he : horariosGrupo.findAll()) {
            if (he.getHorario() == null || he.getGrupoEntrenamiento() == null) {
                continue;
            }
            gruposDeHorario.computeIfAbsent(he.getHorario().getId(), k -> new ArrayList<>())
                    .add(new GrupoRef(he.getGrupoEntrenamiento().getId(), he.getGrupoEntrenamiento().getNombre()));
        }
        // deportista -> ids de grupos vigentes (para elegir el grupo cuando un horario lo comparten varios)
        Map<Integer, Set<Integer>> gruposDeDeportista = new HashMap<>();
        for (Object[] f : membresias.gruposVigentesPorDeportista()) {
            gruposDeDeportista.computeIfAbsent((Integer) f[0], k -> new HashSet<>()).add((Integer) f[2]);
        }

        Map<String, Acum> porEstado = new TreeMap<>();
        Map<Integer, Acum> porGrupo = new HashMap<>();
        Map<Integer, String> nombreGrupo = new HashMap<>();
        Map<Integer, Set<String>> sesionesGrupo = new HashMap<>();
        Map<Integer, Acum> porDia = new TreeMap<>();
        Map<LocalDate, Acum> porSemana = new TreeMap<>();
        Map<Integer, Acum> porDeportista = new HashMap<>();
        Map<Integer, String> nombreDeportista = new HashMap<>();
        Map<Integer, List<Asistencia>> registrosDeportista = new HashMap<>();
        Set<String> sesiones = new HashSet<>();
        long total = 0;
        long presentes = 0;

        for (Asistencia a : asistencias.enRangoConDetalle(desde, hasta)) {
            GrupoRef grupo = grupoDe(a, gruposDeHorario, gruposDeDeportista);
            if (grupoId != null && (grupo == null || !grupoId.equals(grupo.id()))) {
                continue;
            }
            boolean presente = ESTADO_PRESENTE.equalsIgnoreCase(a.getEstadoAsistencia().getEstado());
            total++;
            if (presente) {
                presentes++;
            }
            porEstado.computeIfAbsent(a.getEstadoAsistencia().getEstado(), k -> new Acum()).add(presente);
            if (grupo != null) {
                porGrupo.computeIfAbsent(grupo.id(), k -> new Acum()).add(presente);
                nombreGrupo.put(grupo.id(), grupo.nombre());
                sesionesGrupo.computeIfAbsent(grupo.id(), k -> new HashSet<>()).add(a.getHorario().getId() + "|" + a.getFecha());
            }
            sesiones.add((grupo != null ? grupo.id() : 0) + "|" + a.getHorario().getId() + "|" + a.getFecha());
            porDia.computeIfAbsent(a.getFecha().getDayOfWeek().getValue(), k -> new Acum()).add(presente);
            porSemana.computeIfAbsent(a.getFecha().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), k -> new Acum())
                    .add(presente);
            Integer did = a.getDeportista().getId();
            porDeportista.computeIfAbsent(did, k -> new Acum()).add(presente);
            registrosDeportista.computeIfAbsent(did, k -> new ArrayList<>()).add(a);
            nombreDeportista.computeIfAbsent(did, k -> a.getDeportista().getPersona().getNombre() + " "
                    + a.getDeportista().getPersona().getApellidos());
        }

        final long totalFinal = total;
        List<PorEstado> estados = new ArrayList<>();
        porEstado.forEach((e, ac) -> estados.add(new PorEstado(e, ac.total, pct(ac.total, totalFinal))));
        estados.sort(Comparator.comparingLong(PorEstado::total).reversed());

        List<PorGrupo> grupos = new ArrayList<>();
        porGrupo.forEach((id, ac) -> grupos.add(new PorGrupo(id, nombreGrupo.get(id), ac.total, ac.presentes,
                pct(ac.presentes, ac.total), sesionesGrupo.get(id).size())));
        grupos.sort(Comparator.comparing(PorGrupo::nombre, String.CASE_INSENSITIVE_ORDER));

        List<PorDia> dias = new ArrayList<>();
        porDia.forEach((d, ac) -> dias.add(new PorDia(d, DIAS[d], ac.total, ac.presentes, pct(ac.presentes, ac.total))));

        List<Semana> semanas = new ArrayList<>();
        porSemana.forEach((ini, ac) -> semanas.add(new Semana(ini, ac.total, ac.presentes, pct(ac.presentes, ac.total))));

        List<PorDeportista> todos = new ArrayList<>();
        porDeportista.forEach((id, ac) -> {
            if (ac.total >= MIN_REGISTROS) {
                todos.add(new PorDeportista(id, nombreDeportista.get(id), ac.total, ac.presentes, pct(ac.presentes, ac.total),
                        rachaSinAsistir(registrosDeportista.get(id))));
            }
        });
        List<PorDeportista> enRiesgo = new ArrayList<>(todos.stream().filter(d -> d.porcentaje() < UMBRAL_RIESGO)
                .sorted(Comparator.comparing(PorDeportista::porcentaje).thenComparing(PorDeportista::nombre)).limit(TOP).toList());
        List<PorDeportista> constantes = new ArrayList<>(todos.stream().filter(d -> d.porcentaje() >= UMBRAL_RIESGO)
                .sorted(Comparator.comparing(PorDeportista::porcentaje).reversed().thenComparing(
                        Comparator.comparingLong(PorDeportista::total).reversed()))
                .limit(TOP).toList());

        return new Metricas(desde, hasta, total, presentes, pct(presentes, total), sesiones.size(), porDeportista.size(),
                estados, grupos, dias, semanas, enRiesgo, constantes);
    }

    /** Grupo al que se atribuye el registro: entre los del horario, preferir uno en el que el deportista esta hoy. */
    private static GrupoRef grupoDe(Asistencia a, Map<Integer, List<GrupoRef>> gruposDeHorario,
            Map<Integer, Set<Integer>> gruposDeDeportista) {
        List<GrupoRef> candidatos = gruposDeHorario.get(a.getHorario().getId());
        if (candidatos == null || candidatos.isEmpty()) {
            return null;
        }
        Set<Integer> suyos = gruposDeDeportista.getOrDefault(a.getDeportista().getId(), Set.of());
        for (GrupoRef g : candidatos) {
            if (suyos.contains(g.id())) {
                return g;
            }
        }
        return candidatos.get(0);
    }

    /** Cuantos entrenamientos seguidos lleva sin asistir, contando desde el ultimo registro hacia atras. */
    private static int rachaSinAsistir(List<Asistencia> registros) {
        List<Asistencia> orden = new ArrayList<>(registros);
        orden.sort(Comparator.comparing(Asistencia::getFecha).reversed());
        int racha = 0;
        for (Asistencia a : orden) {
            if (ESTADO_PRESENTE.equalsIgnoreCase(a.getEstadoAsistencia().getEstado())) {
                break;
            }
            racha++;
        }
        return racha;
    }
}
