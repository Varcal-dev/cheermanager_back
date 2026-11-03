package com.varcal.cheermanager.Service.Clubes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.Models.Clubes.Club;
import com.varcal.cheermanager.Models.Clubes.EstadoSuscripcion;
import com.varcal.cheermanager.Models.Clubes.PlanSaas;
import com.varcal.cheermanager.Models.Clubes.SuscripcionPago;
import com.varcal.cheermanager.Models.Clubes.VigenciaSuscripcion;
import com.varcal.cheermanager.repository.Clubes.ClubRepository;
import com.varcal.cheermanager.repository.Clubes.PlanSaasRepository;
import com.varcal.cheermanager.repository.Clubes.SuscripcionPagoRepository;
import com.varcal.cheermanager.tenant.TenantContext;
import com.varcal.cheermanager.tenant.TenantLookupRepository;

/**
 * Plan y suscripcion de cada club: consulta, limites, cobro manual (renovacion) y ajustes del superadmin.
 *
 * - Un club con la suscripcion vencida queda en SOLO LECTURA (lo aplica JwtTokenFilter con VigenciaSuscripcion).
 * - El limite de deportistas se exige al registrar o reactivar un deportista (exigirCupoDeportista).
 * - Las operaciones de administracion las protege el controller con el permiso gestionar_clubes.
 */
@Service
public class SuscripcionService {

    /** Los vencimientos se cuentan en hora de Colombia, igual que las tareas programadas. */
    public static final ZoneId ZONA = ZoneId.of("America/Bogota");

    public record PlanDTO(Integer id, String codigo, String nombre, Integer maxDeportistas, BigDecimal precioMensual) {
        static PlanDTO de(PlanSaas p) {
            return new PlanDTO(p.getId(), p.getCodigo(), p.getNombre(), p.getMaxDeportistas(), p.getPrecioMensual());
        }
    }

    public record Resumen(Integer clubId, String clubNombre, String slug, Boolean clubActivo,
            Integer planId, String planCodigo, String planNombre, BigDecimal precioMensual,
            String estadoSuscripcion, String vigencia, LocalDate vigenteHasta, Long diasRestantes, Integer diasGracia,
            Integer limiteDeportistas, Integer limiteDelPlan, Integer limiteEfectivo, long deportistasActivos,
            String notas) {
    }

    /** Campos null = sin cambio. sinVencimiento=true borra la fecha; quitarLimite=true vuelve al limite del plan. */
    public record ActualizarRequest(Integer planId, String estadoSuscripcion, LocalDate vigenteHasta,
            Boolean sinVencimiento, Integer diasGracia, Integer limiteDeportistas, Boolean quitarLimite, String notas) {
    }

    public record PagoRequest(BigDecimal monto, Integer meses, String metodo, String referencia, LocalDate fechaPago,
            String nota) {
    }

    private final ClubRepository clubRepository;
    private final PlanSaasRepository planRepository;
    private final SuscripcionPagoRepository pagoRepository;
    private final TenantLookupRepository lookup;

    public SuscripcionService(ClubRepository clubRepository, PlanSaasRepository planRepository,
            SuscripcionPagoRepository pagoRepository, TenantLookupRepository lookup) {
        this.clubRepository = clubRepository;
        this.planRepository = planRepository;
        this.pagoRepository = pagoRepository;
        this.lookup = lookup;
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZONA);
    }

    // ------------------------------------------------------------------ consulta

    public List<PlanDTO> planes() {
        return planRepository.findByActivoTrueOrderByOrdenAsc().stream().map(PlanDTO::de).toList();
    }

    /** Una fila por club (para la pantalla del superadmin). */
    public List<Resumen> resumenes() {
        Map<Integer, PlanSaas> planes = planRepository.findAll().stream()
                .collect(Collectors.toMap(PlanSaas::getId, Function.identity()));
        return clubRepository.findAll().stream()
                .map(c -> armar(c, c.getPlanId() == null ? null : planes.get(c.getPlanId()), true)).toList();
    }

    public Resumen resumen(Integer clubId) {
        return armar(club(clubId), true);
    }

    /** Lo que ve el propio club (sin notas internas del superadmin). */
    public Resumen resumenDelClubActual() {
        return armar(club(TenantContext.require()), false);
    }

    public List<SuscripcionPago> pagos(Integer clubId) {
        club(clubId);
        return pagoRepository.findByClubIdOrderByFechaPagoDescIdDesc(clubId);
    }

    private Resumen armar(Club c, boolean conNotas) {
        PlanSaas plan = c.getPlanId() == null ? null : planRepository.findById(c.getPlanId()).orElse(null);
        return armar(c, plan, conNotas);
    }

    private Resumen armar(Club c, PlanSaas plan, boolean conNotas) {
        LocalDate hoy = hoy();
        VigenciaSuscripcion vigencia = VigenciaSuscripcion.de(c, hoy);
        Long dias = c.getVigenteHasta() == null ? null : ChronoUnit.DAYS.between(hoy, c.getVigenteHasta());
        Integer delPlan = plan == null ? null : plan.getMaxDeportistas();
        Integer efectivo = c.getLimiteDeportistas() != null ? c.getLimiteDeportistas() : delPlan;
        return new Resumen(c.getId(), c.getNombre(), c.getSlug(), c.getActivo(),
                plan == null ? null : plan.getId(), plan == null ? null : plan.getCodigo(),
                plan == null ? null : plan.getNombre(), plan == null ? null : plan.getPrecioMensual(),
                c.getEstadoSuscripcion() == null ? null : c.getEstadoSuscripcion().name(), vigencia.name(),
                c.getVigenteHasta(), dias, c.getDiasGracia(), c.getLimiteDeportistas(), delPlan, efectivo,
                lookup.contarDeportistasActivos(c.getId()), conNotas ? c.getNotasSuscripcion() : null);
    }

    // ------------------------------------------------------------------ limites

    /** Cupos libres de deportistas activos en el club actual; null = sin limite. Nunca negativo. */
    public Integer cupoDisponible() {
        int clubId = TenantContext.require();
        Club c = club(clubId);
        Integer limite = c.getLimiteDeportistas();
        if (limite == null && c.getPlanId() != null) {
            limite = planRepository.findById(c.getPlanId()).map(PlanSaas::getMaxDeportistas).orElse(null);
        }
        if (limite == null) {
            return null;
        }
        return (int) Math.max(0, limite - lookup.contarDeportistasActivos(clubId));
    }

    /**
     * Se llama al registrar o reactivar un deportista. Lanza un error de negocio (400 con mensaje legible) si el
     * club ya tiene tantos deportistas activos como permite su plan.
     */
    public void exigirCupoDeportista() {
        int clubId = TenantContext.require();
        Club c = club(clubId);
        Integer limite = c.getLimiteDeportistas();
        if (limite == null && c.getPlanId() != null) {
            limite = planRepository.findById(c.getPlanId()).map(PlanSaas::getMaxDeportistas).orElse(null);
        }
        if (limite == null) {
            return;
        }
        long activos = lookup.contarDeportistasActivos(clubId);
        if (activos >= limite) {
            throw new RuntimeException("Tu plan permite hasta " + limite + " deportistas activos y el club ya tiene "
                    + activos + ". Retira a algún deportista o pide ampliar el plan.");
        }
    }

    // ------------------------------------------------------------------ administracion (superadmin)

    @Transactional
    public Resumen actualizar(Integer clubId, ActualizarRequest req) {
        Club c = editable(clubId);
        if (req.planId() != null) {
            planRepository.findById(req.planId()).orElseThrow(() -> new RuntimeException("El plan no existe"));
            c.setPlanId(req.planId());
        }
        if (req.estadoSuscripcion() != null) {
            try {
                c.setEstadoSuscripcion(EstadoSuscripcion.valueOf(req.estadoSuscripcion().trim().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("Estado de suscripción no válido (PRUEBA, ACTIVA o CANCELADA)");
            }
        }
        if (Boolean.TRUE.equals(req.sinVencimiento())) {
            c.setVigenteHasta(null);
        } else if (req.vigenteHasta() != null) {
            c.setVigenteHasta(req.vigenteHasta());
        }
        if (req.diasGracia() != null) {
            if (req.diasGracia() < 0 || req.diasGracia() > 60) {
                throw new RuntimeException("Los días de gracia deben estar entre 0 y 60");
            }
            c.setDiasGracia(req.diasGracia());
        }
        if (Boolean.TRUE.equals(req.quitarLimite())) {
            c.setLimiteDeportistas(null);
        } else if (req.limiteDeportistas() != null) {
            if (req.limiteDeportistas() < 1) {
                throw new RuntimeException("El límite de deportistas debe ser al menos 1");
            }
            c.setLimiteDeportistas(req.limiteDeportistas());
        }
        if (req.notas() != null) {
            String n = req.notas().trim();
            c.setNotasSuscripcion(n.isEmpty() ? null : n.substring(0, Math.min(n.length(), 500)));
        }
        clubRepository.save(c);
        return resumen(clubId);
    }

    /**
     * Registra un cobro y extiende la vigencia: el periodo nuevo empieza al dia siguiente del vencimiento actual
     * (o hoy, si ya vencio o no tenia fecha) y dura "meses". Deja la suscripcion ACTIVA.
     */
    @Transactional
    public Resumen registrarPago(Integer clubId, PagoRequest req, String registradoPor) {
        Club c = editable(clubId);
        if (req.meses() == null || req.meses() < 1 || req.meses() > 24) {
            throw new RuntimeException("Los meses deben estar entre 1 y 24");
        }
        if (req.monto() == null || req.monto().signum() < 0) {
            throw new RuntimeException("El monto es obligatorio y no puede ser negativo");
        }
        LocalDate hoy = hoy();
        LocalDate desde = (c.getVigenteHasta() != null && !c.getVigenteHasta().isBefore(hoy))
                ? c.getVigenteHasta().plusDays(1)
                : hoy;
        LocalDate hasta = desde.plusMonths(req.meses()).minusDays(1);

        SuscripcionPago p = new SuscripcionPago();
        p.setClubId(clubId);
        p.setFechaPago(req.fechaPago() != null ? req.fechaPago() : hoy);
        p.setMonto(req.monto());
        p.setMeses(req.meses());
        p.setMetodo(recortar(req.metodo(), 40));
        p.setReferencia(recortar(req.referencia(), 120));
        p.setNota(recortar(req.nota(), 300));
        p.setPeriodoDesde(desde);
        p.setPeriodoHasta(hasta);
        p.setRegistradoPor(recortar(registradoPor, 80));
        pagoRepository.save(p);

        c.setEstadoSuscripcion(EstadoSuscripcion.ACTIVA);
        c.setVigenteHasta(hasta);
        clubRepository.save(c);
        return resumen(clubId);
    }

    // ------------------------------------------------------------------ utilidades

    private Club club(Integer id) {
        return clubRepository.findById(id).orElseThrow(() -> new RuntimeException("Club no encontrado"));
    }

    private Club editable(Integer id) {
        if (id == Club.PLATAFORMA_ID) {
            throw new RuntimeException("El club de la plataforma no tiene suscripción");
        }
        return club(id);
    }

    private static String recortar(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > max ? t.substring(0, max) : t;
    }
}
