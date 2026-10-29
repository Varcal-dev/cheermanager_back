package com.varcal.cheermanager.Service.Financiero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Comunes.PaginaDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaManualDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.Models.Financiero.EstadoPago;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Financiero.Pago;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.Utils.AuthenticatedUserUtil;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;
import com.varcal.cheermanager.repository.Personas.PersonaRepository;

@Service
public class FacturaService {

    /** Plazo de pago: la factura vence a los 10 dias de emitida; despues esta EN MORA. */
    public static final int DIAS_PARA_PAGAR = 10;

    /** Conceptos permitidos en un cobro MANUAL (los demas los pone el sistema). */
    public static final Set<String> CONCEPTOS_MANUALES = Set.of("UNIFORME", "COMPETENCIA", "EVENTO", "OTRO");

    private final FacturaRepository facturaRepository;
    private final PersonaRepository personaRepository;
    private final PagoRepository pagoRepository;
    private final EventoRepository eventoRepository;

    public FacturaService(FacturaRepository facturaRepository,
                          PersonaRepository personaRepository,
                          PagoRepository pagoRepository,
                          EventoRepository eventoRepository) {
        this.facturaRepository = facturaRepository;
        this.personaRepository = personaRepository;
        this.pagoRepository = pagoRepository;
        this.eventoRepository = eventoRepository;
    }

    /** Fecha limite de pago para una factura emitida en la fecha dada. */
    public static LocalDate calcularVencimiento(LocalDate fechaEmision) {
        return fechaEmision.plusDays(DIAS_PARA_PAGAR);
    }

    // ---------- Cobro manual (uniforme, competencia, evento, otro) ----------

    @Transactional
    public FacturaResponseDTO crearManual(FacturaManualDTO dto) {
        if (dto.getPersonaId() == null) {
            throw new RuntimeException("Indica a quien se le cobra (personaId)");
        }
        Persona persona = personaRepository.findById(dto.getPersonaId())
                .orElseThrow(() -> new RuntimeException("Persona no encontrada"));

        String concepto = dto.getConcepto() == null ? "" : dto.getConcepto().trim().toUpperCase();
        if (!CONCEPTOS_MANUALES.contains(concepto)) {
            throw new RuntimeException("Concepto invalido. Valores permitidos: " + String.join(", ", CONCEPTOS_MANUALES));
        }
        if (dto.getDescripcion() == null || dto.getDescripcion().isBlank()) {
            throw new RuntimeException("La descripcion del cobro es obligatoria");
        }
        if (dto.getTotal() == null || dto.getTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El total debe ser mayor a cero");
        }
        if (dto.getEventoId() != null && !eventoRepository.existsById(dto.getEventoId())) {
            throw new RuntimeException("Evento no encontrado");
        }

        LocalDate emision = dto.getFechaEmision() != null ? dto.getFechaEmision() : LocalDate.now();
        LocalDate vencimiento = dto.getFechaVencimiento() != null ? dto.getFechaVencimiento() : calcularVencimiento(emision);
        if (vencimiento.isBefore(emision)) {
            throw new RuntimeException("El vencimiento no puede ser anterior a la emision");
        }

        Factura factura = new Factura();
        factura.setNumeroFactura(generarNumeroFactura("FAC-" + Year.of(emision.getYear()) + "-"));
        factura.setFechaEmision(emision);
        factura.setFechaVencimiento(vencimiento);
        factura.setPersona(persona);
        factura.setConcepto(concepto);
        factura.setEventoId(dto.getEventoId());
        factura.setDescripcion(dto.getDescripcion().trim());
        factura.setTotal(dto.getTotal().setScale(2, java.math.RoundingMode.HALF_UP));
        return toResponseDTO(facturaRepository.save(factura));
    }

    // ---------- Anulacion (las facturas NO se borran) ----------

    /**
     * Anula una factura con motivo. Reglas:
     *  - no se puede anular si tiene pagos en estado "Pagado": primero se anulan esos pagos;
     *  - los pagos Pendiente/Vencido que tenga se anulan junto con ella.
     */
    @Transactional
    public FacturaResponseDTO anular(Integer id, String motivo) {
        String m = validarMotivo(motivo);
        Factura factura = facturaRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada"));
        if (Boolean.TRUE.equals(factura.getAnulada())) {
            throw new RuntimeException("La factura " + factura.getNumeroFactura() + " ya esta anulada");
        }

        List<Pago> pagos = pagoRepository.findByFacturaId(id);
        boolean hayPagados = pagos.stream().anyMatch(p -> p.getEstado() == EstadoPago.Pagado);
        if (hayPagados) {
            throw new RuntimeException("La factura " + factura.getNumeroFactura()
                    + " tiene pagos confirmados. Anula primero esos pagos (con su motivo) y luego la factura.");
        }

        Integer usuarioId = usuarioActualId();
        LocalDateTime ahora = LocalDateTime.now();
        for (Pago p : pagos) {
            if (p.getEstado() != EstadoPago.Anulado) {
                p.setEstado(EstadoPago.Anulado);
                p.setMotivoAnulacion("Factura anulada: " + m);
                p.setFechaAnulacion(ahora);
                p.setAnuladoPorUsuarioId(usuarioId);
                pagoRepository.save(p);
            }
        }

        marcarAnulada(factura, m, usuarioId, ahora);
        return toResponseDTO(facturaRepository.save(factura));
    }

    /** Uso interno (p. ej. al eliminar una inscripcion): anula sin tocar pagos; el llamador ya verifico que no hay. */
    public void marcarAnulada(Factura factura, String motivo, Integer usuarioId, LocalDateTime cuando) {
        factura.setAnulada(true);
        factura.setMotivoAnulacion(motivo);
        factura.setFechaAnulacion(cuando);
        factura.setAnuladaPorUsuarioId(usuarioId);
    }

    public static String validarMotivo(String motivo) {
        String m = motivo == null ? "" : motivo.trim();
        if (m.length() < 5) {
            throw new RuntimeException("Indica el motivo de la anulacion (minimo 5 caracteres)");
        }
        return m.length() > 300 ? m.substring(0, 300) : m;
    }

    /** Id del usuario autenticado, o null si no hay (p. ej. procesos automaticos). */
    public static Integer usuarioActualId() {
        try {
            return AuthenticatedUserUtil.getCurrentUserId().intValue();
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ---------- Numeracion ----------

    // Recibe el prefijo completo (ej. "FAC-2026-" para inscripciones/mensualidades/manuales,
    // "FAC-VTA-2026-" para ventas de tienda) para que las series nunca choquen entre si.
    public String generarNumeroFactura(String prefijo) {
        long siguiente = facturaRepository.countByNumeroFacturaStartingWith(prefijo) + 1;

        String numero;
        do {
            numero = prefijo + String.format("%04d", siguiente);
            siguiente++;
        } while (facturaRepository.findByNumeroFactura(numero).isPresent());

        return numero;
    }

    // Atajo con el prefijo por defecto ("FAC-VTA-<año>-") para las ventas de tienda.
    public String generarNumeroFacturaVenta() {
        return generarNumeroFactura("FAC-VTA-" + Year.now().getValue() + "-");
    }

    // ---------- Consultas (devuelven DTO plano) ----------

    public List<Factura> listar() {
        return facturaRepository.findAll();
    }

    public Optional<Factura> buscarPorId(Integer id) {
        return facturaRepository.findById(id);
    }

    public List<FacturaResponseDTO> listarDTO() {
        return toResponseDTOs(facturaRepository.findAllConDetalle());
    }

    public Optional<FacturaResponseDTO> buscarDTOPorId(Integer id) {
        return facturaRepository.findById(id).map(this::toResponseDTO);
    }

    /** Facturas vencidas con saldo pendiente, las mas antiguas primero. */
    public List<FacturaResponseDTO> listarEnMora() {
        return toResponseDTOs(facturaRepository.findEnMora(LocalDate.now()));
    }

    public List<FacturaResponseDTO> listarPorPersona(Integer personaId) {
        return toResponseDTOs(facturaRepository.findByPersonaIdOrderByFechaEmisionDesc(personaId));
    }

    private static final Set<String> ESTADOS_FILTRO =
            Set.of("TODOS", "PAGADA", "ABONO_PARCIAL", "PENDIENTE", "EN_MORA", "ANULADA");

    /**
     * Listado paginado con filtros. Todos los filtros son opcionales:
     * q (numero, descripcion o nombre), estado (TODOS|PAGADA|ABONO_PARCIAL|PENDIENTE|EN_MORA|ANULADA),
     * concepto (INSCRIPCION|MENSUALIDAD|UNIFORME|...), y rango de fecha de emision.
     */
    @Transactional(readOnly = true)
    public PaginaDTO<FacturaResponseDTO> listarPagina(String q, String estado, String concepto,
                                                      LocalDate desde, LocalDate hasta,
                                                      int pagina, int tamano) {
        String estadoN = (estado == null || estado.isBlank()) ? "TODOS" : estado.trim().toUpperCase();
        if (!ESTADOS_FILTRO.contains(estadoN)) {
            throw new IllegalArgumentException("Estado no valido: " + estado);
        }
        String conceptoN = (concepto == null || concepto.isBlank()) ? "TODOS" : concepto.trim().toUpperCase();
        String qN = q == null ? "" : q.trim();
        LocalDate d = desde != null ? desde : LocalDate.of(1900, 1, 1);
        LocalDate h = hasta != null ? hasta : LocalDate.of(2999, 12, 31);
        if (d.isAfter(h)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'");
        }
        int size = Math.min(Math.max(tamano, 1), 100);
        int page = Math.max(pagina, 0);

        var resultado = facturaRepository.buscarPagina(qN, estadoN, conceptoN, d, h, LocalDate.now(),
                PageRequest.of(page, size));
        return PaginaDTO.de(resultado, this::toResponseDTOs);
    }

    /** Totales de cartera para las tarjetas de arriba (no dependen de la pagina ni de los filtros). */
    @Transactional(readOnly = true)
    public Map<String, Object> resumenCartera() {
        List<FacturaResponseDTO> mora = listarEnMora();
        BigDecimal totalMora = mora.stream()
                .map(FacturaResponseDTO::getSaldoPendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("facturasEnMora", mora.size(), "totalEnMora", totalMora);
    }

    /** Igual que toResponseDTO pero para muchas facturas: UNA consulta de pagos en vez de una por factura. */
    public List<FacturaResponseDTO> toResponseDTOs(Collection<Factura> facturas) {
        if (facturas.isEmpty()) {
            return List.of();
        }
        Map<Integer, BigDecimal> pagado = pagoRepository.pagadoPorFacturas(
                facturas.stream().map(Factura::getId).toList());
        return facturas.stream()
                .map(f -> construirDTO(f, pagado.getOrDefault(f.getId(), BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    /** Estado de cuenta de una factura (reglas en un solo lugar: lo usan la lista, el portal y los reportes). */
    public FacturaResponseDTO toResponseDTO(Factura factura) {
        return construirDTO(factura, pagoRepository.sumarMontoPagadoPorFactura(factura.getId()));
    }

    private FacturaResponseDTO construirDTO(Factura factura, BigDecimal pagado) {
        FacturaResponseDTO dto = new FacturaResponseDTO();
        dto.setId(factura.getId());
        dto.setNumeroFactura(factura.getNumeroFactura());
        dto.setFechaEmision(factura.getFechaEmision());
        dto.setDescripcion(factura.getDescripcion());
        dto.setTotal(factura.getTotal());
        dto.setFechaVencimiento(factura.getFechaVencimiento());
        dto.setConcepto(factura.getConcepto());
        dto.setEventoId(factura.getEventoId());
        boolean anulada = Boolean.TRUE.equals(factura.getAnulada());
        dto.setAnulada(anulada);
        dto.setMotivoAnulacion(factura.getMotivoAnulacion());

        if (factura.getPersona() != null) {
            FacturaResponseDTO.PersonaResumenDTO persona = new FacturaResponseDTO.PersonaResumenDTO();
            persona.setId(factura.getPersona().getId());
            persona.setNombre(factura.getPersona().getNombre());
            persona.setApellidos(factura.getPersona().getApellidos());
            dto.setPersona(persona);
        }

        if (factura.getDescuento() != null) {
            FacturaResponseDTO.DescuentoResumenDTO descuento = new FacturaResponseDTO.DescuentoResumenDTO();
            descuento.setId(factura.getDescuento().getId());
            descuento.setNombre(factura.getDescuento().getNombre());
            descuento.setDescripcion(factura.getDescuento().getDescripcion());
            descuento.setPorcentaje(factura.getDescuento().getPorcentaje());
            descuento.setActivo(factura.getDescuento().getActivo());
            dto.setDescuento(descuento);
        }

        if (factura.getInscripcion() != null) {
            dto.setInscripcionId(factura.getInscripcion().getId());
        }

        BigDecimal saldo = factura.getTotal().subtract(pagado);
        dto.setTotalPagado(pagado);
        dto.setSaldoPendiente(anulada ? BigDecimal.ZERO : saldo.max(BigDecimal.ZERO));

        LocalDate hoy = LocalDate.now();
        if (anulada) {
            dto.setEstadoCuenta("ANULADA");
        } else if (saldo.signum() <= 0) {
            dto.setEstadoCuenta("PAGADA");
        } else if (factura.getFechaVencimiento() != null && hoy.isAfter(factura.getFechaVencimiento())) {
            dto.setEstadoCuenta("EN_MORA");
            dto.setDiasMora(ChronoUnit.DAYS.between(factura.getFechaVencimiento(), hoy));
        } else if (pagado.signum() > 0) {
            dto.setEstadoCuenta("ABONO_PARCIAL");
        } else {
            dto.setEstadoCuenta("PENDIENTE");
        }
        return dto;
    }
}