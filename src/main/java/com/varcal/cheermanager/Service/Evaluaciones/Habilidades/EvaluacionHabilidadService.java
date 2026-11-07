package com.varcal.cheermanager.Service.Evaluaciones.Habilidades;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.DeportistaHabilidad;
import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.EvaluacionHabilidad;
import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.EvaluacionHabilidadDriver;
import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.HabilidadCatalogo;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.DeportistaHabilidadRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.EvaluacionHabilidadDriverRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.EvaluacionHabilidadRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.HabilidadCatalogoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

/**
 * C1: catalogo de habilidades (glosario N1-N4) y evaluacion de una habilidad aplicando los drivers que le
 * corresponden. El puntaje es el valor inicial de la categoria menos 0.1 por cada nivel de deduccion de cada driver.
 */
@Service
public class EvaluacionHabilidadService {

    public static final String INICIAL = "INICIAL";
    public static final String EN_DESARROLLO = "EN_DESARROLLO";
    public static final String DOMINADA = "DOMINADA";
    /** Proporcion del valor inicial desde la que la habilidad se considera dominada / en desarrollo. */
    static final BigDecimal UMBRAL_DOMINADA = new BigDecimal("0.85");
    static final BigDecimal UMBRAL_EN_DESARROLLO = new BigDecimal("0.60");

    private final HabilidadCatalogoRepository catalogo;
    private final DeportistaHabilidadRepository estados;
    private final EvaluacionHabilidadRepository evaluaciones;
    private final EvaluacionHabilidadDriverRepository drivers;
    private final DeportistaRepository deportistas;

    public EvaluacionHabilidadService(HabilidadCatalogoRepository catalogo, DeportistaHabilidadRepository estados,
            EvaluacionHabilidadRepository evaluaciones, EvaluacionHabilidadDriverRepository drivers,
            DeportistaRepository deportistas) {
        this.catalogo = catalogo;
        this.estados = estados;
        this.evaluaciones = evaluaciones;
        this.drivers = drivers;
        this.deportistas = deportistas;
    }

    public record Deduccion(String driver, Integer nivel) {
    }

    public record Solicitud(Integer deportistaId, Integer habilidadId, LocalDate fecha, String observaciones,
            List<Deduccion> deducciones) {
    }

    public record Resultado(Integer evaluacionId, BigDecimal valorInicial, BigDecimal totalDeducciones,
            BigDecimal puntaje, String estado) {
    }

    public record EstadoHabilidad(HabilidadCatalogo habilidad, String estado, BigDecimal ultimoPuntaje,
            BigDecimal mejorPuntaje, LocalDate fechaUltima, Integer totalEvaluaciones) {
    }

    public record ProgresoNivel(String nivel, long totalCatalogo, long evaluadas, long dominadas) {
    }

    public record EstadoDeportista(List<EstadoHabilidad> habilidades, List<ProgresoNivel> porNivel) {
    }

    public record ItemHistorial(Integer id, LocalDate fecha, BigDecimal valorInicial, BigDecimal totalDeducciones,
            BigDecimal puntaje, String observaciones, List<Deduccion> deducciones) {
    }

    /** Habilidades activas, filtradas (cualquier filtro puede ir nulo). */
    public List<HabilidadCatalogo> listarCatalogo(String nivel, String categoria, String estilo) {
        return catalogo.findByActivoTrueOrderById().stream()
                .filter(h -> nivel == null || nivel.isBlank() || h.getNivel().equalsIgnoreCase(nivel))
                .filter(h -> categoria == null || categoria.isBlank() || h.getCategoria().equalsIgnoreCase(categoria))
                .filter(h -> estilo == null || estilo.isBlank() || estilo.equalsIgnoreCase(h.getEstilo()))
                .collect(Collectors.toList());
    }

    public List<DriversHabilidad.Driver> driversDe(String categoria) {
        return DriversHabilidad.de(categoria == null ? "" : categoria.toUpperCase());
    }

    @Transactional
    public Resultado evaluar(Solicitud s, Integer usuarioId) {
        if (s == null || s.deportistaId() == null || s.habilidadId() == null) {
            throw new IllegalArgumentException("Indique el deportista y la habilidad a evaluar.");
        }
        Deportista deportista = deportistas.findById(s.deportistaId())
                .orElseThrow(() -> new IllegalArgumentException("Deportista no encontrado."));
        HabilidadCatalogo habilidad = catalogo.findById(s.habilidadId())
                .filter(h -> Boolean.TRUE.equals(h.getActivo()))
                .orElseThrow(() -> new IllegalArgumentException("Habilidad no encontrada."));
        LocalDate fecha = s.fecha() == null ? LocalDate.now() : s.fecha();
        if (fecha.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de la evaluación no puede ser futura.");
        }
        String obs = s.observaciones() == null || s.observaciones().isBlank() ? null : s.observaciones().trim();
        if (obs != null && obs.length() > 500) {
            throw new IllegalArgumentException("Las observaciones no pueden pasar de 500 caracteres.");
        }

        List<DriversHabilidad.Driver> permitidos = DriversHabilidad.de(habilidad.getCategoria());
        Map<String, Integer> niveles = new LinkedHashMap<>();
        if (s.deducciones() != null) {
            for (Deduccion d : s.deducciones()) {
                if (d == null || d.driver() == null || d.nivel() == null) {
                    throw new IllegalArgumentException("Cada deducción debe indicar el driver y su nivel.");
                }
                DriversHabilidad.Driver def = permitidos.stream().filter(p -> p.nombre().equals(d.driver()))
                        .findFirst().orElseThrow(() -> new IllegalArgumentException(
                                "El driver " + d.driver() + " no aplica a esta habilidad."));
                if (niveles.containsKey(d.driver())) {
                    throw new IllegalArgumentException("El driver " + d.driver() + " está repetido.");
                }
                if (d.nivel() < 0 || d.nivel() > def.maxNivel()) {
                    throw new IllegalArgumentException("El driver " + def.etiqueta() + " admite deducción de 0 a "
                            + def.maxNivel() + ".");
                }
                niveles.put(d.driver(), d.nivel());
            }
        }

        BigDecimal inicial = DriversHabilidad.valorInicial(habilidad.getCategoria());
        int suma = niveles.values().stream().mapToInt(Integer::intValue).sum();
        BigDecimal total = DriversHabilidad.PASO.multiply(BigDecimal.valueOf(suma));
        BigDecimal puntaje = inicial.subtract(total).max(BigDecimal.ZERO).setScale(1);

        EvaluacionHabilidad ev = new EvaluacionHabilidad();
        ev.setDeportista(deportista);
        ev.setHabilidad(habilidad);
        ev.setFecha(fecha);
        ev.setUsuarioId(usuarioId);
        ev.setValorInicial(inicial);
        ev.setTotalDeducciones(total.setScale(1));
        ev.setPuntaje(puntaje);
        ev.setObservaciones(obs);
        ev = evaluaciones.save(ev);
        for (Map.Entry<String, Integer> e : niveles.entrySet()) {
            EvaluacionHabilidadDriver r = new EvaluacionHabilidadDriver();
            r.setEvaluacionHabilidad(ev);
            r.setNombreDriver(e.getKey());
            r.setNivelDeduccion(e.getValue());
            drivers.save(r);
        }

        String estado = estadoPara(puntaje, inicial);
        DeportistaHabilidad dh = estados.findByDeportistaIdAndHabilidadId(deportista.getId(), habilidad.getId())
                .orElseGet(() -> {
                    DeportistaHabilidad nuevo = new DeportistaHabilidad();
                    nuevo.setDeportista(deportista);
                    nuevo.setHabilidad(habilidad);
                    nuevo.setMejorPuntaje(BigDecimal.ZERO);
                    nuevo.setFechaUltima(LocalDate.MIN);
                    nuevo.setTotalEvaluaciones(0);
                    return nuevo;
                });
        dh.setTotalEvaluaciones(dh.getTotalEvaluaciones() + 1);
        dh.setMejorPuntaje(dh.getMejorPuntaje().max(puntaje));
        // El estado refleja la evaluacion mas reciente; una evaluacion con fecha anterior no lo pisa.
        if (!fecha.isBefore(dh.getFechaUltima())) {
            dh.setFechaUltima(fecha);
            dh.setUltimoPuntaje(puntaje);
            dh.setEstado(estado);
        }
        estados.save(dh);
        return new Resultado(ev.getId(), inicial, total.setScale(1), puntaje, dh.getEstado());
    }

    static String estadoPara(BigDecimal puntaje, BigDecimal inicial) {
        if (puntaje.compareTo(inicial.multiply(UMBRAL_DOMINADA)) >= 0) return DOMINADA;
        if (puntaje.compareTo(inicial.multiply(UMBRAL_EN_DESARROLLO)) >= 0) return EN_DESARROLLO;
        return INICIAL;
    }

    /** Habilidades evaluadas del deportista y avance por nivel (cuantas dominadas de cuantas hay en el glosario). */
    public EstadoDeportista estadoDeDeportista(Integer deportistaId) {
        deportistas.findById(deportistaId).orElseThrow(() -> new IllegalArgumentException("Deportista no encontrado."));
        List<HabilidadCatalogo> todas = catalogo.findByActivoTrueOrderById();
        List<DeportistaHabilidad> mias = estados.findByDeportistaId(deportistaId);
        Set<Integer> activas = todas.stream().map(HabilidadCatalogo::getId).collect(Collectors.toSet());
        List<EstadoHabilidad> lista = new ArrayList<>();
        for (DeportistaHabilidad d : mias) {
            if (!activas.contains(d.getHabilidad().getId())) continue;
            lista.add(new EstadoHabilidad(d.getHabilidad(), d.getEstado(), d.getUltimoPuntaje(), d.getMejorPuntaje(),
                    d.getFechaUltima(), d.getTotalEvaluaciones()));
        }
        Map<String, long[]> acum = new LinkedHashMap<>();
        for (HabilidadCatalogo h : todas) acum.computeIfAbsent(h.getNivel(), k -> new long[3])[0]++;
        for (EstadoHabilidad e : lista) {
            long[] a = acum.get(e.habilidad().getNivel());
            a[1]++;
            if (DOMINADA.equals(e.estado())) a[2]++;
        }
        List<ProgresoNivel> niveles = new ArrayList<>();
        acum.forEach((n, a) -> niveles.add(new ProgresoNivel(n, a[0], a[1], a[2])));
        return new EstadoDeportista(lista, niveles);
    }

    public List<ItemHistorial> historial(Integer deportistaId, Integer habilidadId) {
        List<EvaluacionHabilidad> evs = evaluaciones
                .findByDeportistaIdAndHabilidadIdOrderByFechaDescIdDesc(deportistaId, habilidadId);
        if (evs.isEmpty()) return new ArrayList<>();
        Map<Integer, List<Deduccion>> porEval = new LinkedHashMap<>();
        for (EvaluacionHabilidadDriver r : drivers.findByEvaluacionHabilidadIdIn(
                new HashSet<>(evs.stream().map(EvaluacionHabilidad::getId).toList()))) {
            porEval.computeIfAbsent(r.getEvaluacionHabilidad().getId(), k -> new ArrayList<>())
                    .add(new Deduccion(r.getNombreDriver(), r.getNivelDeduccion()));
        }
        return evs.stream().map(e -> new ItemHistorial(e.getId(), e.getFecha(), e.getValorInicial(),
                e.getTotalDeducciones(), e.getPuntaje(), e.getObservaciones(),
                porEval.getOrDefault(e.getId(), new ArrayList<>()))).collect(Collectors.toList());
    }
}
