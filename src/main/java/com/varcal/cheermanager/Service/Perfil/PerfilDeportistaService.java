package com.varcal.cheermanager.Service.Perfil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.DTO.Financiero.PagoResponseDTO;
import com.varcal.cheermanager.Models.Evaluaciones.Evaluacion;
import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.EvaluacionHabilidad;
import com.varcal.cheermanager.Models.Financiero.PlanPago;
import com.varcal.cheermanager.Models.Org_dep.Nivel;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.DocumentoDeportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Service.Evaluaciones.Habilidades.EvaluacionHabilidadService;
import com.varcal.cheermanager.Service.Financiero.FacturaService;
import com.varcal.cheermanager.Service.Financiero.PagoService;
import com.varcal.cheermanager.Service.Persona.DocumentoDeportistaService;
import com.varcal.cheermanager.Service.Ranking.RankingService;
import com.varcal.cheermanager.repository.Evaluaciones.EvaluacionRepository;
import com.varcal.cheermanager.repository.Evaluaciones.Habilidades.EvaluacionHabilidadRepository;
import com.varcal.cheermanager.repository.Financiero.PlanPagoRepository;
import com.varcal.cheermanager.repository.Historiales.HistorialPlanesDeportistaRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.AsistenciaRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;
import com.varcal.cheermanager.repository.Org_dep.NivelRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;
import com.varcal.cheermanager.repository.Personas.DocumentoDeportistaRepository;

/**
 * Perfil 360 del deportista: estado, facturacion, asistencia y evolucion tecnica en una sola respuesta.
 * Solo lectura y sin tablas nuevas: todo se calcula con los servicios y repositorios que ya existen.
 *
 * Tiene dos puertas:
 *  - {@link #paraClub}: personal del club. Cada seccion llega null si el rol no tiene el permiso de esa seccion;
 *    la evolucion incluye la posicion en el ranking de habilidades aprendidas (C12).
 *  - {@link #paraPortal}: deportista o acudiente (el PortalService ya valido que el deportista sea suyo).
 *    La facturacion solo la ve el acudiente o el deportista mayor de edad, y NUNCA hay comparaciones con otros atletas.
 */
@Service
@Transactional(readOnly = true)
public class PerfilDeportistaService {

    public static final int MAYORIA_EDAD = 18;
    static final int MESES_HISTORIA = 6;
    static final int DIAS_AVISO_DOCUMENTOS = 30;
    static final int MAX_FILAS = 5;
    static final int MAX_PUNTOS_EVOLUCION = 12;

    // ---------- Respuesta ----------

    /** Cada seccion salvo estado puede ser null (sin permiso o no aplica); notaFacturacion explica por que no hay facturacion en el portal. */
    public record Perfil(DatosEstado estado, DatosFacturacion facturacion, DatosAsistencia asistencia,
            DatosEvolucion evolucion, String notaFacturacion) {
    }

    /** tipo: CERTIFICADO_MEDICO | EPS | SEGURO | LICENCIA; estado: VIGENTE | POR_VENCER | VENCIDO | SIN_REGISTRO. */
    public record Vencimiento(String tipo, String estado, LocalDate fechaVencimiento, Integer diasRestantes,
            String entidad) {
    }

    public record DatosEstado(Integer deportistaId, String nombre, String estado, String nivel, List<String> grupos,
            String planPago, LocalDate fechaRegistro, Integer antiguedadMeses, Integer edad,
            List<Vencimiento> vencimientos) {
    }

    public record FacturaPendiente(Integer id, String numero, String concepto, String descripcion,
            LocalDate fechaVencimiento, BigDecimal saldo, String estadoCuenta, Long diasMora) {
    }

    public record PagoReciente(Integer id, LocalDate fecha, String numeroFactura, String metodo, BigDecimal monto,
            String estado) {
    }

    /** ultimosPagos es null si el rol no tiene ver_pago. */
    public record DatosFacturacion(BigDecimal saldoPendiente, int facturasEnMora, BigDecimal montoEnMora,
            LocalDate proximoVencimiento, List<FacturaPendiente> pendientes, List<PagoReciente> ultimosPagos) {
    }

    /** mes: "yyyy-MM". porcentaje es null si ese mes no tuvo sesiones. */
    public record MesAsistencia(String mes, int presentes, int total, Integer porcentaje) {
    }

    public record DatosAsistencia(Integer porcentajeMes, int presentesMes, int totalMes, Integer porcentaje90d,
            int presentes90d, int total90d, int tardes90d, int ausentes90d, int rachaActual,
            List<MesAsistencia> meses) {
    }

    public record PuntoEvolucion(LocalDate fecha, double promedio) {
    }

    public record PromedioCategoria(String categoria, double promedio, int evaluaciones) {
    }

    public record AvanceNivel(String nivel, long totalCatalogo, long evaluadas, long dominadas, int porcentaje) {
    }

    public record ResumenHabilidades(int dominadas, int enDesarrollo, int evaluadas, long totalCatalogo,
            String nivelEnCurso, Integer avanceNivelEnCurso, List<AvanceNivel> porNivel, int aprendidasAnio) {
    }

    public record AprendidasMes(String mes, int cantidad) {
    }

    /** Solo para el personal del club. posicion es null si aun no aprendio ninguna habilidad en el anio. */
    public record Posicion(int anio, int aprendidas, Integer posicion, int deportistasConAprendizaje) {
    }

    public record DatosEvolucion(Double promedioGeneral, int evaluaciones, List<PuntoEvolucion> serie,
            List<PromedioCategoria> porCategoria, ResumenHabilidades habilidades, List<AprendidasMes> aprendidasPorMes,
            Posicion posicion) {
    }

    private record Acceso(boolean facturacion, boolean pagos, boolean asistencia, boolean evolucion,
            boolean ranking) {
    }

    // ---------- Dependencias ----------

    private final DeportistaRepository deportistas;
    private final DeportistaPerteneceGrupoRepository membresias;
    private final NivelRepository niveles;
    private final HistorialPlanesDeportistaRepository historialPlanes;
    private final PlanPagoRepository planes;
    private final DocumentoDeportistaRepository documentos;
    private final AsistenciaRepository asistencias;
    private final EvaluacionRepository evaluaciones;
    private final EvaluacionHabilidadRepository evaluacionesHabilidad;
    private final EvaluacionHabilidadService habilidadService;
    private final RankingService rankingService;
    private final FacturaService facturaService;
    private final PagoService pagoService;

    public PerfilDeportistaService(DeportistaRepository deportistas, DeportistaPerteneceGrupoRepository membresias,
            NivelRepository niveles, HistorialPlanesDeportistaRepository historialPlanes, PlanPagoRepository planes,
            DocumentoDeportistaRepository documentos, AsistenciaRepository asistencias,
            EvaluacionRepository evaluaciones, EvaluacionHabilidadRepository evaluacionesHabilidad,
            EvaluacionHabilidadService habilidadService, RankingService rankingService,
            FacturaService facturaService, PagoService pagoService) {
        this.deportistas = deportistas;
        this.membresias = membresias;
        this.niveles = niveles;
        this.historialPlanes = historialPlanes;
        this.planes = planes;
        this.documentos = documentos;
        this.asistencias = asistencias;
        this.evaluaciones = evaluaciones;
        this.evaluacionesHabilidad = evaluacionesHabilidad;
        this.habilidadService = habilidadService;
        this.rankingService = rankingService;
        this.facturaService = facturaService;
        this.pagoService = pagoService;
    }

    // Puntos de extension (las pruebas los reemplazan: estos servicios son clases, no interfaces).

    protected LocalDate hoy() {
        return com.varcal.cheermanager.Service.Clubes.SuscripcionService.hoy();
    }

    protected List<FacturaResponseDTO> facturasDe(Integer personaId) {
        return facturaService.listarPorPersona(personaId);
    }

    protected List<PagoResponseDTO> pagosDe(Integer personaId) {
        return pagoService.listarPorPersona(personaId);
    }

    protected EvaluacionHabilidadService.EstadoDeportista habilidadesDe(Integer deportistaId) {
        return habilidadService.estadoDeDeportista(deportistaId);
    }

    protected RankingService.RankingHabilidades rankingHabilidadesAnioActual() {
        return rankingService.rankingHabilidades(null, null);
    }

    // ---------- Puertas ----------

    /** Personal del club: el controlador ya exigio ver_deportista; cada seccion pide ademas su permiso. */
    public Perfil paraClub(Integer deportistaId, Set<String> permisos) {
        Deportista d = deportistas.findById(deportistaId)
                .orElseThrow(() -> new IllegalArgumentException("Deportista no encontrado"));
        boolean verEvaluaciones = permisos.contains("ver_evaluaciones") || permisos.contains("ver_evaluacion");
        Acceso acceso = new Acceso(permisos.contains("ver_factura"), permisos.contains("ver_pago"),
                permisos.contains("ver_asistencia"), verEvaluaciones, permisos.contains("ver_evaluaciones"));
        return construir(d, acceso, null);
    }

    /** Portal: el deportista ya fue validado como del usuario (o de su acudiente) por el PortalService. */
    public Perfil paraPortal(Deportista d, boolean usuarioEsAcudiente) {
        boolean factura = puedeVerFacturacion(usuarioEsAcudiente, d.getPersona().getFechaNacimiento(), hoy());
        Acceso acceso = new Acceso(factura, factura, true, true, false);
        return construir(d, acceso,
                factura ? null : "La facturación la consulta el acudiente (o el deportista cuando cumpla " + MAYORIA_EDAD
                        + " años).");
    }

    /**
     * Regla de negocio: la facturacion la ve el acudiente y el deportista mayor de edad. Sin fecha de nacimiento
     * se trata como menor (ante la duda, no se muestra).
     */
    public static boolean puedeVerFacturacion(boolean usuarioEsAcudiente, LocalDate nacimiento, LocalDate hoy) {
        if (usuarioEsAcudiente) {
            return true;
        }
        return nacimiento != null && Period.between(nacimiento, hoy).getYears() >= MAYORIA_EDAD;
    }

    private Perfil construir(Deportista d, Acceso a, String notaFacturacion) {
        LocalDate hoy = hoy();
        return new Perfil(estado(d, hoy), a.facturacion() ? facturacion(d, a.pagos(), hoy) : null,
                a.asistencia() ? asistencia(d, hoy) : null, a.evolucion() ? evolucion(d, hoy, a.ranking()) : null,
                notaFacturacion);
    }

    // ---------- Estado ----------

    private DatosEstado estado(Deportista d, LocalDate hoy) {
        Persona p = d.getPersona();
        List<String> grupos = membresias.findByDeportistaIdAndFechaFinIsNull(d.getId()).stream()
                .filter(m -> m.getGrupo() != null).map(m -> m.getGrupo().getNombre()).toList();
        String nivel = d.getNivelActualId() == null ? null
                : niveles.findById(d.getNivelActualId()).map(Nivel::getNombre).orElse(null);
        String plan = historialPlanes.findByDeportistaIdAndFechaFinIsNull(d.getId())
                .flatMap(h -> planes.findById(h.getPlanPagoId())).map(PlanPago::getNombre).orElse(null);
        Integer antiguedad = d.getFechaRegistro() == null ? null
                : (int) Math.max(0, Period.between(d.getFechaRegistro(), hoy).toTotalMonths());
        Integer edad = p.getFechaNacimiento() == null ? null : Period.between(p.getFechaNacimiento(), hoy).getYears();
        return new DatosEstado(d.getId(), p.getNombre() + " " + p.getApellidos(),
                d.getEstado() != null ? d.getEstado().getEstado() : null, nivel, grupos, plan, d.getFechaRegistro(),
                antiguedad, edad, vencimientos(d.getId(), hoy));
    }

    /** Mismo criterio que C4: vale el ultimo registro de cada tipo; certificado medico y EPS se exigen aunque falten. */
    private List<Vencimiento> vencimientos(Integer deportistaId, LocalDate hoy) {
        Map<DocumentoDeportista.Tipo, DocumentoDeportista> ultimo = new EnumMap<>(DocumentoDeportista.Tipo.class);
        for (DocumentoDeportista doc : documentos.findByDeportistaIdOrderByIdDesc(deportistaId)) {
            if (doc.getTipo() != null) {
                ultimo.putIfAbsent(doc.getTipo(), doc);
            }
        }
        List<Vencimiento> res = new ArrayList<>();
        for (DocumentoDeportista.Tipo t : DocumentoDeportista.Tipo.values()) {
            DocumentoDeportista doc = ultimo.get(t);
            if (doc == null && !DocumentoDeportistaService.REQUERIDOS_POR_DEFECTO.contains(t)) {
                continue;
            }
            DocumentoDeportistaService.Estado e = DocumentoDeportistaService.estado(doc, hoy, DIAS_AVISO_DOCUMENTOS);
            LocalDate fv = doc == null ? null : doc.getFechaVencimiento();
            res.add(new Vencimiento(t.name(), e.name(), fv, fv == null ? null : (int) ChronoUnit.DAYS.between(hoy, fv),
                    doc == null ? null : doc.getEntidad()));
        }
        return res;
    }

    // ---------- Facturacion ----------

    private DatosFacturacion facturacion(Deportista d, boolean conPagos, LocalDate hoy) {
        Integer personaId = d.getPersona().getId();
        BigDecimal saldo = BigDecimal.ZERO;
        BigDecimal montoMora = BigDecimal.ZERO;
        int enMora = 0;
        List<FacturaResponseDTO> conSaldo = new ArrayList<>();
        for (FacturaResponseDTO f : facturasDe(personaId)) {
            if (Boolean.TRUE.equals(f.getAnulada())) {
                continue;
            }
            BigDecimal s = f.getSaldoPendiente() == null ? BigDecimal.ZERO : f.getSaldoPendiente();
            if (s.signum() <= 0) {
                continue;
            }
            saldo = saldo.add(s);
            conSaldo.add(f);
            if ("EN_MORA".equals(f.getEstadoCuenta())) {
                enMora++;
                montoMora = montoMora.add(s);
            }
        }
        conSaldo.sort(Comparator.comparing(FacturaResponseDTO::getFechaVencimiento,
                Comparator.nullsLast(Comparator.<LocalDate>naturalOrder())));
        LocalDate proximo = conSaldo.stream().filter(f -> !"EN_MORA".equals(f.getEstadoCuenta()))
                .map(FacturaResponseDTO::getFechaVencimiento).filter(Objects::nonNull).filter(v -> !v.isBefore(hoy))
                .min(Comparator.<LocalDate>naturalOrder()).orElse(null);
        List<FacturaPendiente> pendientes = conSaldo.stream().limit(MAX_FILAS)
                .map(f -> new FacturaPendiente(f.getId(), f.getNumeroFactura(), f.getConcepto(), f.getDescripcion(),
                        f.getFechaVencimiento(), f.getSaldoPendiente(), f.getEstadoCuenta(), f.getDiasMora()))
                .toList();
        List<PagoReciente> pagos = null;
        if (conPagos) {
            pagos = pagosDe(personaId).stream().filter(p -> !"Anulado".equals(p.getEstado()))
                    .sorted(Comparator.comparing(PagoResponseDTO::getFecha,
                            Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder())).reversed())
                    .limit(MAX_FILAS).map(p -> new PagoReciente(p.getId(), p.getFecha(), p.getNumeroFactura(),
                            p.getMetodoPago(), p.getMonto(), p.getEstado()))
                    .toList();
        }
        return new DatosFacturacion(saldo, enMora, montoMora, proximo, pendientes, pagos);
    }

    // ---------- Asistencia ----------

    /**
     * El porcentaje cuenta solo "Presente" sobre todos los registros, igual que el resto del portal. La racha cuenta
     * sesiones seguidas a las que asistio (Presente o Tarde), de la mas reciente hacia atras; un "Justificado" ni suma
     * ni la rompe, un "Ausente" la corta.
     */
    private DatosAsistencia asistencia(Deportista d, LocalDate hoy) {
        YearMonth actual = YearMonth.from(hoy);
        YearMonth primero = actual.minusMonths(MESES_HISTORIA - 1);
        Map<YearMonth, int[]> porMes = new LinkedHashMap<>();
        for (int i = 0; i < MESES_HISTORIA; i++) {
            porMes.put(primero.plusMonths(i), new int[2]);
        }
        LocalDate desde90 = hoy.minusDays(90);
        int presentes90 = 0, total90 = 0, tardes90 = 0, ausentes90 = 0, racha = 0;
        boolean rachaViva = true;
        for (var a : asistencias.findByDeportistaIdAndFechaBetweenOrderByFechaDesc(d.getId(), primero.atDay(1), hoy)) {
            String e = a.getEstadoAsistencia() == null ? null : a.getEstadoAsistencia().getEstado();
            boolean presente = "Presente".equals(e);
            int[] mes = porMes.get(YearMonth.from(a.getFecha()));
            if (mes != null) {
                mes[1]++;
                if (presente) {
                    mes[0]++;
                }
            }
            if (!a.getFecha().isBefore(desde90)) {
                total90++;
                if (presente) presentes90++;
                if ("Tarde".equals(e)) tardes90++;
                if ("Ausente".equals(e)) ausentes90++;
            }
            if (rachaViva) {
                if (presente || "Tarde".equals(e)) {
                    racha++;
                } else if (!"Justificado".equals(e)) {
                    rachaViva = false;
                }
            }
        }
        List<MesAsistencia> meses = new ArrayList<>();
        porMes.forEach((ym, v) -> meses.add(new MesAsistencia(ym.toString(), v[0], v[1], pct(v[0], v[1]))));
        int[] delMes = porMes.get(actual);
        return new DatosAsistencia(pct(delMes[0], delMes[1]), delMes[0], delMes[1], pct(presentes90, total90),
                presentes90, total90, tardes90, ausentes90, racha, meses);
    }

    // ---------- Evolucion tecnica ----------

    private DatosEvolucion evolucion(Deportista d, LocalDate hoy, boolean conRanking) {
        TreeMap<LocalDate, List<Integer>> porFecha = new TreeMap<>();
        Map<String, List<Integer>> porCategoria = new LinkedHashMap<>();
        List<Integer> todos = new ArrayList<>();
        for (Evaluacion e : evaluaciones.findByDeportistaIdOrderByFechaDesc(d.getId())) {
            if (e.getPuntajeObtenido() == null || e.getFecha() == null) {
                continue;
            }
            int v = e.getPuntajeObtenido();
            String cat = e.getCriterioEvaluacion() != null && e.getCriterioEvaluacion().getCategoria() != null
                    ? e.getCriterioEvaluacion().getCategoria().getCategoria() : "Sin categoría";
            porFecha.computeIfAbsent(e.getFecha(), k -> new ArrayList<>()).add(v);
            porCategoria.computeIfAbsent(cat, k -> new ArrayList<>()).add(v);
            todos.add(v);
        }
        List<PuntoEvolucion> puntos = new ArrayList<>();
        porFecha.forEach((f, vs) -> puntos.add(new PuntoEvolucion(f, redondear(media(vs)))));
        List<PuntoEvolucion> serie = puntos.size() > MAX_PUNTOS_EVOLUCION
                ? new ArrayList<>(puntos.subList(puntos.size() - MAX_PUNTOS_EVOLUCION, puntos.size())) : puntos;
        List<PromedioCategoria> categorias = new ArrayList<>();
        porCategoria.forEach((c, vs) -> categorias.add(new PromedioCategoria(c, redondear(media(vs)), vs.size())));

        int aprendidasAnio = 0;
        YearMonth actual = YearMonth.from(hoy);
        Map<YearMonth, Integer> porMes = new LinkedHashMap<>();
        for (int i = MESES_HISTORIA - 1; i >= 0; i--) {
            porMes.put(actual.minusMonths(i), 0);
        }
        // "Aprender" = la primera evaluacion en que llega a dominada (>= 85% del valor inicial), como en el ranking C12.
        Set<Integer> yaVistas = new HashSet<>();
        for (EvaluacionHabilidad ev : evaluacionesHabilidad.findByDeportistaIdOrderByFechaAscIdAsc(d.getId())) {
            if (ev.getHabilidad() == null || ev.getValorInicial() == null || ev.getValorInicial().signum() <= 0
                    || ev.getPuntaje() == null || ev.getFecha() == null) {
                continue;
            }
            boolean dominada = ev.getPuntaje()
                    .compareTo(ev.getValorInicial().multiply(EvaluacionHabilidadService.UMBRAL_DOMINADA)) >= 0;
            if (!dominada || !yaVistas.add(ev.getHabilidad().getId())) {
                continue;
            }
            porMes.computeIfPresent(YearMonth.from(ev.getFecha()), (k, n) -> n + 1);
            if (ev.getFecha().getYear() == hoy.getYear()) {
                aprendidasAnio++;
            }
        }
        List<AprendidasMes> aprendidasPorMes = new ArrayList<>();
        porMes.forEach((ym, n) -> aprendidasPorMes.add(new AprendidasMes(ym.toString(), n)));

        Posicion posicion = null;
        if (conRanking) {
            RankingService.RankingHabilidades r = rankingHabilidadesAnioActual();
            int mias = r.filas().stream().filter(f -> d.getId().equals(f.deportistaId()))
                    .mapToInt(RankingService.AprendizajeFila::cantidad).findFirst().orElse(0);
            Integer lugar = mias == 0 ? null : 1 + (int) r.filas().stream().filter(f -> f.cantidad() > mias).count();
            posicion = new Posicion(r.anio(), mias, lugar, r.filas().size());
        }
        return new DatosEvolucion(todos.isEmpty() ? null : redondear(media(todos)), todos.size(), serie, categorias,
                habilidades(d.getId(), aprendidasAnio), aprendidasPorMes, posicion);
    }

    private ResumenHabilidades habilidades(Integer deportistaId, int aprendidasAnio) {
        EvaluacionHabilidadService.EstadoDeportista est = habilidadesDe(deportistaId);
        int dominadas = (int) est.habilidades().stream()
                .filter(h -> EvaluacionHabilidadService.DOMINADA.equals(h.estado())).count();
        int enDesarrollo = (int) est.habilidades().stream()
                .filter(h -> EvaluacionHabilidadService.EN_DESARROLLO.equals(h.estado())).count();
        List<AvanceNivel> porNivel = est.porNivel().stream()
                .sorted(Comparator.comparing(EvaluacionHabilidadService.ProgresoNivel::nivel))
                .map(n -> new AvanceNivel(n.nivel(), n.totalCatalogo(), n.evaluadas(), n.dominadas(),
                        n.totalCatalogo() == 0 ? 0 : (int) Math.round(n.dominadas() * 100.0 / n.totalCatalogo())))
                .toList();
        long totalCatalogo = porNivel.stream().mapToLong(AvanceNivel::totalCatalogo).sum();
        // Nivel en curso: el primero (N1..N4) que todavia no tiene todas sus habilidades dominadas.
        AvanceNivel enCurso = porNivel.stream().filter(n -> n.dominadas() < n.totalCatalogo()).findFirst()
                .orElse(porNivel.isEmpty() ? null : porNivel.get(porNivel.size() - 1));
        return new ResumenHabilidades(dominadas, enDesarrollo, est.habilidades().size(), totalCatalogo,
                enCurso == null ? null : enCurso.nivel(), enCurso == null ? null : enCurso.porcentaje(), porNivel,
                aprendidasAnio);
    }

    // ---------- utilidades ----------

    private static Integer pct(int parte, int total) {
        return total == 0 ? null : (int) Math.round(parte * 100.0 / total);
    }

    private static double media(List<Integer> xs) {
        return xs.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    private static double redondear(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
