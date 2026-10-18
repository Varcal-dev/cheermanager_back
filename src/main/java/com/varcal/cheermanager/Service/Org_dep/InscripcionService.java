package com.varcal.cheermanager.Service.Org_dep;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Financiero.ResumenGeneracionCobrosDTO;
import com.varcal.cheermanager.DTO.Org_dep.DeportistaInscritoDTO;
import com.varcal.cheermanager.DTO.Org_dep.DeportistaNoInscritoDTO;
import com.varcal.cheermanager.DTO.Org_dep.InscripcionDetalleDto;
import com.varcal.cheermanager.DTO.Org_dep.InscripcionDto;
import com.varcal.cheermanager.DTO.Org_dep.InscripcionSimpleDTO;
import com.varcal.cheermanager.Models.Financiero.Convenio;
import com.varcal.cheermanager.Models.Financiero.Descuento;
import com.varcal.cheermanager.Models.Financiero.EstadoPago;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Financiero.PlanPago;
import com.varcal.cheermanager.Models.Org_dep.Inscripcion;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Service.Historiales.HistorialPlanesDeportistaService;
import com.varcal.cheermanager.repository.Financiero.ConvenioRepository;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;
import com.varcal.cheermanager.repository.Financiero.PlanPagoRepository;
import com.varcal.cheermanager.repository.Org_dep.InscripcionRepository;
import com.varcal.cheermanager.repository.Org_dep.TemporadaRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

@Service
public class InscripcionService {

    @Autowired
    private InscripcionRepository inscripcionRepository;

    @Autowired
    private TemporadaRepository temporadaRepository;

    @Autowired
    private DeportistaRepository deportistaRepository;

    @Autowired
    private PlanPagoRepository planPagoRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private ConvenioRepository convenioRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private HistorialPlanesDeportistaService historialPlanesDeportistaService;

    public List<Object[]> obtenerDetalleInscripciones() {
        return inscripcionRepository.obtenerVistaInscripcionesDetalle();
    }

    public Inscripcion obtenerPorId(Integer id) {
        return inscripcionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Inscripción no encontrada"));
    }

    // @Transactional: crea la Inscripcion y, a partir de ella, la(s) Factura(s)
    // correspondientes. Si algo falla en el camino, la inscripción tampoco
    // debe quedar guardada — de lo contrario tendríamos deportistas
    // "inscritos" sin nada que cobrarles (o cobrados a medias).
    @Transactional
    public Inscripcion crearInscripcion(InscripcionDto dto) {
        Inscripcion inscripcion = new Inscripcion();

        Deportista deportista = deportistaRepository.findById(dto.getDeportistaId())
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado"));

        PlanPago planInscripcion = planPagoRepository.findById(dto.getPlanPagoId())
                .orElseThrow(() -> new RuntimeException("Plan de pago no encontrado"));

        inscripcion.setDeportista(deportista);
        inscripcion.setPlanPago(planInscripcion);
        inscripcion.setFechaInscripcion(dto.getFechaInscripcion());
        inscripcion.setFechaVencimiento(dto.getFechaVencimiento());
        inscripcion.setEstado(Inscripcion.EstadoInscripcion.valueOf(dto.getEstado()));
        // Queda asociada a la temporada activa del club (si hay una).
        temporadaRepository.findFirstByActivaTrue().ifPresent(inscripcion::setTemporada);

        // Plan de mensualidad (opcional). Si se indica, la inscripción queda
        // vinculada a él para que el proceso mensual de facturación (más
        // abajo) sepa que debe generarle cobros recurrentes.
        if (dto.getPlanMensualidadId() != null) {
            PlanPago planMensualidad = planPagoRepository.findById(dto.getPlanMensualidadId())
                    .orElseThrow(() -> new RuntimeException("Plan de mensualidad no encontrado"));
            inscripcion.setPlanMensualidad(planMensualidad);
        }

        Inscripcion guardada = inscripcionRepository.save(inscripcion);

        // Factura #1: cobro único de inscripción (periodo = null, marca que
        // es un cobro de una sola vez, no recurrente).
        generarFacturaParaPlan(guardada, planInscripcion, guardada.getFechaInscripcion(), null, "Inscripción");

        // Factura #2 (si se eligió plan de mensualidad): el primer mes queda
        // facturado desde ya, para no dejarlo sin cobrar hasta que corra el
        // primer proceso mensual.
        if (guardada.getPlanMensualidad() != null) {
            String periodoActual = YearMonth.from(guardada.getFechaInscripcion()).toString();
            generarFacturaParaPlan(guardada, guardada.getPlanMensualidad(), guardada.getFechaInscripcion(),
                    periodoActual, "Mensualidad");
        }

        return guardada;
    }

    // Construye y guarda una Factura para un plan (de inscripción o de
    // mensualidad) asociado a una inscripción. `periodo` va en null para
    // cobros únicos (inscripción) y en formato "yyyy-MM" para mensualidad.
    private Factura generarFacturaParaPlan(Inscripcion inscripcion, PlanPago plan, LocalDate fechaEmision,
            String periodo, String etiquetaConcepto) {
        Deportista deportista = inscripcion.getDeportista();
        ResultadoCalculoFactura calculo = calcularTotalFactura(plan, deportista, fechaEmision, etiquetaConcepto);

        Factura factura = new Factura();
        factura.setNumeroFactura(generarNumeroFactura());
        factura.setFechaEmision(fechaEmision);
        factura.setPersona(deportista.getPersona());
        factura.setDescripcion(calculo.descripcion);
        factura.setDescuento(calculo.descuentoAplicado);
        factura.setTotal(calculo.total);
        factura.setInscripcion(inscripcion);
        factura.setPeriodo(periodo);

        return facturaRepository.save(factura);
    }

    // ===== Facturación mensual de mensualidades =====
    //
    // Recorre todas las inscripciones activas con plan de mensualidad y le
    // genera la factura del período indicado a cada una — salvo que ya
    // tenga una para ese mismo período (así correr esto dos veces en el
    // mismo mes no duplica cobros).
    //
    // Es un proceso disparado manualmente (botón en el frontend), no un cron
    // automático: así el club conserva control y visibilidad de cuándo se
    // generan los cobros, en vez de que ocurra en segundo plano sin que
    // nadie lo vea. Si más adelante se quiere automatizar con @Scheduled,
    // este mismo método es el que se llamaría desde el job.
    @Transactional
    public ResumenGeneracionCobrosDTO generarCobrosMensualidad(String periodo) {
        YearMonth periodoParseado;
        try {
            periodoParseado = YearMonth.parse(periodo);
        } catch (Exception e) {
            throw new RuntimeException("Formato de período inválido. Usa yyyy-MM, ej: 2026-08");
        }

        LocalDate fechaEmision = periodoParseado.atDay(1);
        String periodoStr = periodoParseado.toString();

        List<Inscripcion> inscripcionesConMensualidad = inscripcionRepository
                .findByEstadoAndPlanMensualidadIsNotNull(Inscripcion.EstadoInscripcion.Activa);

        ResumenGeneracionCobrosDTO resumen = new ResumenGeneracionCobrosDTO();
        resumen.setPeriodo(periodoStr);

        for (Inscripcion inscripcion : inscripcionesConMensualidad) {
            boolean yaFacturado = facturaRepository
                    .findByInscripcionIdAndPeriodo(inscripcion.getId(), periodoStr)
                    .isPresent();

            if (yaFacturado) {
                resumen.setYaFacturadas(resumen.getYaFacturadas() + 1);
                continue;
            }

            try {
                generarFacturaParaPlan(inscripcion, inscripcion.getPlanMensualidad(), fechaEmision,
                        periodoStr, "Mensualidad");
                resumen.setFacturasGeneradas(resumen.getFacturasGeneradas() + 1);
            } catch (Exception e) {
                resumen.getErrores().add("Inscripción #" + inscripcion.getId() + ": " + e.getMessage());
            }
        }

        return resumen;
    }

    // ===== CABO 1: descuento de convenio aplicado automáticamente =====
    //
    // Combina el descuento propio del PlanPago con el descuento del Convenio
    // del deportista (si tiene uno vigente). Se combinan multiplicativamente
    // (no se suman los porcentajes): si el plan ya tiene 10% y el convenio
    // tiene 20%, el resultado NO es 30% de descuento sobre el valor base, sino
    // aplicar primero uno y luego el otro sobre lo que queda. Es la forma
    // correcta de combinar dos descuentos independientes sin que, al sumar
    // porcentajes, dos descuentos grandes terminen regalando más de lo que el
    // club decidió en cada uno por separado.
    //
    // El descuento de convenio solo se aplica si:
    // - el deportista tiene convenioId
    // - el Convenio existe
    // - la fecha de referencia cae dentro de la vigencia del Convenio
    // (fechaInicio/fechaFin, si están definidas)
    // - el Descuento del convenio está activo=true
    // - la fecha de referencia cae dentro de la vigencia del Descuento
    private ResultadoCalculoFactura calcularTotalFactura(PlanPago planPago, Deportista deportista,
            LocalDate fechaReferencia, String etiquetaConcepto) {
        BigDecimal valorBase = planPago.getValorMensual();
        StringBuilder descripcion = new StringBuilder(etiquetaConcepto + " - Plan: " + planPago.getNombre());

        // 1. Descuento propio del plan
        BigDecimal totalConDescuentoPlan = aplicarPorcentaje(valorBase, planPago.getDescuentoPorcentaje());
        if (esDescuentoPositivo(planPago.getDescuentoPorcentaje())) {
            descripcion.append(" (descuento de plan ").append(planPago.getDescuentoPorcentaje()).append("%)");
        }

        // 2. Descuento de convenio del deportista, si tiene uno vigente
        Descuento descuentoConvenioAplicado = null;
        BigDecimal totalFinal = totalConDescuentoPlan;

        Convenio convenio = obtenerConvenioVigente(deportista, fechaReferencia);
        if (convenio != null) {
            Descuento descuento = convenio.getDescuento();
            if (esDescuentoVigente(descuento, fechaReferencia)) {
                totalFinal = aplicarPorcentaje(totalConDescuentoPlan, descuento.getPorcentaje());
                descuentoConvenioAplicado = descuento;
                descripcion.append(" + convenio '").append(convenio.getNombreEmpresa())
                        .append("' (").append(descuento.getPorcentaje()).append("%)");
            }
        }

        totalFinal = totalFinal.setScale(2, RoundingMode.HALF_UP);

        ResultadoCalculoFactura resultado = new ResultadoCalculoFactura();
        resultado.total = totalFinal;
        resultado.descuentoAplicado = descuentoConvenioAplicado;
        resultado.descripcion = descripcion.toString();
        return resultado;
    }

    private Convenio obtenerConvenioVigente(Deportista deportista, LocalDate fechaReferencia) {
        if (deportista.getConvenioId() == null) {
            return null;
        }
        return convenioRepository.findById(deportista.getConvenioId())
                .filter(c -> dentroDeRango(fechaReferencia, c.getFechaInicio(), c.getFechaFin()))
                .orElse(null);
    }

    private boolean esDescuentoVigente(Descuento descuento, LocalDate fechaReferencia) {
        if (descuento == null || !Boolean.TRUE.equals(descuento.getActivo())) {
            return false;
        }
        return dentroDeRango(fechaReferencia, descuento.getFechaInicio(), descuento.getFechaFin());
    }

    // fechaInicio/fechaFin nulas se tratan como "sin límite" en ese extremo.
    private boolean dentroDeRango(LocalDate fecha, LocalDate inicio, LocalDate fin) {
        if (fecha == null) {
            return true;
        }
        boolean despuesDeInicio = (inicio == null) || !fecha.isBefore(inicio);
        boolean antesDeFin = (fin == null) || !fecha.isAfter(fin);
        return despuesDeInicio && antesDeFin;
    }

    private boolean esDescuentoPositivo(BigDecimal porcentaje) {
        return porcentaje != null && porcentaje.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal aplicarPorcentaje(BigDecimal valor, BigDecimal porcentajeDescuento) {
        if (!esDescuentoPositivo(porcentajeDescuento)) {
            return valor;
        }
        BigDecimal factor = porcentajeDescuento.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal montoDescuento = valor.multiply(factor);
        return valor.subtract(montoDescuento);
    }

    private static class ResultadoCalculoFactura {
        BigDecimal total;
        Descuento descuentoAplicado;
        String descripcion;
    }

    // Genera un número de factura único con formato FAC-{año}-{secuencial}.
    private String generarNumeroFactura() {
        String prefijo = "FAC-" + Year.now().getValue() + "-";
        long siguiente = facturaRepository.countByNumeroFacturaStartingWith(prefijo) + 1;

        String numero;
        do {
            numero = prefijo + String.format("%04d", siguiente);
            siguiente++;
        } while (facturaRepository.findByNumeroFactura(numero).isPresent());

        return numero;
    }

    // ===== CABO 2: si cambia el plan de pago, se recalcula la factura =====
    //
    // @Transactional: lee la inscripción, la factura de inscripción asociada
    // (si existe) y guarda ambas como una sola unidad.
    @Transactional
    public Inscripcion actualizarInscripcion(Integer id, Inscripcion nuevaInscripcion) {
        Inscripcion existente = obtenerPorId(id);

        boolean cambioPlan = nuevaInscripcion.getPlanPago() != null
                && !nuevaInscripcion.getPlanPago().getId().equals(existente.getPlanPago().getId());

        existente.setFechaInscripcion(nuevaInscripcion.getFechaInscripcion());
        existente.setFechaVencimiento(nuevaInscripcion.getFechaVencimiento());
        existente.setEstado(nuevaInscripcion.getEstado());

        if (nuevaInscripcion.getPlanPago() != null) {
            PlanPago plan = planPagoRepository.findById(
                    nuevaInscripcion.getPlanPago().getId())
                    .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
            existente.setPlanPago(plan);
        }

        // Cambio de plan de mensualidad: no requiere recalcular nada
        // retroactivamente (a diferencia del plan de inscripción). Las
        // facturas de mensualidad ya generadas quedan como están; el
        // siguiente cobro mensual que se genere usará el plan nuevo, que es
        // el comportamiento correcto para un cargo recurrente hacia adelante.
        if (nuevaInscripcion.getPlanMensualidad() != null) {
            PlanPago planMensualidad = planPagoRepository.findById(
                    nuevaInscripcion.getPlanMensualidad().getId())
                    .orElseThrow(() -> new RuntimeException("Plan de mensualidad no encontrado"));
            existente.setPlanMensualidad(planMensualidad);
        }

        Inscripcion guardada = inscripcionRepository.save(existente);

        // Si cambió el plan de inscripción, recalculamos el total de esa
        // factura específica (si la tiene) para que no quede desincronizada,
        // y dejamos constancia del cambio en HistorialPlanesDeportista.
        if (cambioPlan) {
            recalcularFacturaDeInscripcion(guardada);
            historialPlanesDeportistaService.registrarCambioDePlan(
                    guardada.getDeportista().getId(),
                    guardada.getPlanPago().getId(),
                    LocalDate.now(),
                    "Cambio de plan en inscripción #" + guardada.getId());
        }

        return guardada;
    }

    private void recalcularFacturaDeInscripcion(Inscripcion inscripcion) {
        // Una inscripción puede tener varias facturas (inscripción +
        // mensualidades). La factura de INSCRIPCIÓN es la única con
        // periodo == null — es la que hay que recalcular acá, nunca las de
        // mensualidad.
        facturaRepository.findByInscripcionId(inscripcion.getId()).stream()
                .filter(f -> f.getPeriodo() == null)
                .findFirst()
                .ifPresent(factura -> {

                    // No recalculamos si la factura ya tiene pagos en estado
                    // "Pagado": cambiar el total por debajo de un pago ya
                    // confirmado dejaría el saldo en un estado contradictorio.
                    // En ese caso, el ajuste debe hacerse manualmente.
                    boolean tienePagosConfirmados = pagoRepository.findByFacturaId(factura.getId()).stream()
                            .anyMatch(p -> p.getEstado() == EstadoPago.Pagado);

                    if (tienePagosConfirmados) {
                        throw new RuntimeException(
                                "No se puede cambiar el plan de pago: la factura " + factura.getNumeroFactura()
                                        + " ya tiene pagos registrados. Anula o ajusta los pagos antes de cambiar el plan.");
                    }

                    ResultadoCalculoFactura calculo = calcularTotalFactura(
                            inscripcion.getPlanPago(), inscripcion.getDeportista(), factura.getFechaEmision(),
                            "Inscripción");

                    factura.setDescripcion(calculo.descripcion);
                    factura.setDescuento(calculo.descuentoAplicado);
                    factura.setTotal(calculo.total);
                    facturaRepository.save(factura);
                });
    }

    // ===== CABO 3: qué pasa con la(s) factura(s) al eliminar la inscripción =====
    //
    // @Transactional: recorre TODAS las facturas asociadas (inscripción +
    // mensualidades ya generadas) y las borra o desvincula, junto con la
    // inscripción, como una sola unidad.
    @Transactional
    public void eliminarInscripcion(Integer id) {
        if (!inscripcionRepository.existsById(id)) {
            throw new RuntimeException("Inscripción no encontrada con ID: " + id);
        }

        List<Factura> facturas = facturaRepository.findByInscripcionId(id);
        for (Factura factura : facturas) {
            boolean tienePagos = !pagoRepository.findByFacturaId(factura.getId()).isEmpty();

            if (tienePagos) {
                // La factura ya tiene pagos (confirmados o pendientes)
                // asociados: no la borramos para no perder ese historial
                // financiero. Se desvincula de la inscripción (que va a
                // desaparecer) pero la factura y sus pagos quedan intactos.
                factura.setInscripcion(null);
                facturaRepository.save(factura);
            } else {
                // Sin pagos: es seguro eliminar la factura junto con la inscripción.
                facturaRepository.delete(factura);
            }
        }

        inscripcionRepository.deleteById(id);
    }

    public List<InscripcionSimpleDTO> listarInscripcionesSimples() {
        return inscripcionRepository.findAll().stream().map(ins -> {
            InscripcionSimpleDTO dto = new InscripcionSimpleDTO();
            dto.setId(ins.getId().intValue());
            dto.setDeportista(ins.getDeportista().getPersona().getNombre() + " "
                    + ins.getDeportista().getPersona().getApellidos());
            dto.setFechaInscripcion(ins.getFechaInscripcion());
            dto.setPlanPago(ins.getPlanPago().getTipoPlan().getNombre());
            dto.setEstado(ins.getEstado().name());
            return dto;
        }).collect(Collectors.toList());
    }

    public List<Object[]> obtenerDeportistasNoInscritos() {
        return inscripcionRepository.obtenerVistaDeportistasNoInscritos();
    }

    public List<Object[]> obtenerDeportistasInscritos() {
        return inscripcionRepository.obtenerVistaDeportistasInscritos();
    }

    // Devuelve TODAS las facturas generadas para esta inscripción: la de
    // inscripción (periodo null) y cada mensualidad ya facturada (periodo
    // yyyy-MM). Antes devolvía solo una, porque solo podía existir una.
    public List<Factura> obtenerFacturasDeInscripcion(Integer inscripcionId) {
        if (!inscripcionRepository.existsById(inscripcionId)) {
            throw new RuntimeException("Inscripción no encontrada con ID: " + inscripcionId);
        }
        return facturaRepository.findByInscripcionId(inscripcionId);
    }

    public List<InscripcionDetalleDto> obtenerDetalleInscripcionesDTO() {
        return obtenerDetalleInscripciones().stream()
                .map(this::mapToDetalleDto)
                .collect(Collectors.toList());
    }

    public List<DeportistaNoInscritoDTO> obtenerDeportistasNoInscritosDTO() {
        return obtenerDeportistasNoInscritos().stream()
                .map(this::mapToNoInscritoDto)
                .collect(Collectors.toList());
    }

    public List<DeportistaInscritoDTO> obtenerDeportistasInscritosDTO() {
        return obtenerDeportistasInscritos().stream()
                .map(this::mapToInscritoDto)
                .collect(Collectors.toList());
    }

    // vista_inscripciones_detalle: inscripcion_id, deportista_id, deportista,
    // nivel,
    // fecha_inscripcion, fecha_vencimiento, estado, plan_pago, grupo_asignado,
    // documentos_completos
    private InscripcionDetalleDto mapToDetalleDto(Object[] row) {
        InscripcionDetalleDto dto = new InscripcionDetalleDto();
        dto.setInscripcionId(toLong(row[0]));
        dto.setDeportistaId(toLong(row[1]));
        dto.setDeportista((String) row[2]);
        dto.setNivel((String) row[3]);
        dto.setFechaInscripcion(toLocalDate(row[4]));
        dto.setFechaVencimiento(toLocalDate(row[5]));
        dto.setEstado((String) row[6]);
        dto.setPlanPago((String) row[7]);
        dto.setGrupoAsignado((String) row[8]);
        dto.setDocumentosCompletos(toBoolean(row[9]));
        return dto;
    }

    // vista_deportistas_no_inscritos: deportista_id, nombre_completo, nivel
    private DeportistaNoInscritoDTO mapToNoInscritoDto(Object[] row) {
        DeportistaNoInscritoDTO dto = new DeportistaNoInscritoDTO();
        dto.setDeportistaId(toInteger(row[0]));
        dto.setNombreCompleto((String) row[1]);
        dto.setNivel((String) row[2]);
        return dto;
    }

    // vista_deportistas_inscritos: deportista_id, nombre, apellidos, telefono,
    // fecha_nacimiento, genero, nivel, fecha_inscripcion, fecha_vencimiento,
    // estado_inscripcion, plan_pago, tipo_plan, valor_mensual, estado_deportista,
    // grupo_entrenamiento
    private DeportistaInscritoDTO mapToInscritoDto(Object[] row) {
        DeportistaInscritoDTO dto = new DeportistaInscritoDTO();
        dto.setDeportistaId(toInteger(row[0]));
        dto.setNombre((String) row[1]);
        dto.setApellidos((String) row[2]);
        dto.setTelefono((String) row[3]);
        dto.setFechaNacimiento(toLocalDate(row[4]));
        dto.setGenero((String) row[5]);
        dto.setNivel((String) row[6]);
        dto.setFechaInscripcion(toLocalDate(row[7]));
        dto.setFechaVencimiento(toLocalDate(row[8]));
        dto.setEstadoInscripcion((String) row[9]);
        dto.setPlanPago((String) row[10]);
        dto.setTipoPlan((String) row[11]);
        dto.setValorMensual(toBigDecimal(row[12]));
        dto.setEstadoDeportista((String) row[13]);
        dto.setGrupoEntrenamiento((String) row[14]);
        return dto;
    }

    private Integer toInteger(Object value) {
        return value == null ? null : ((Number) value).intValue();
    }

    private Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null)
            return null;
        if (value instanceof BigDecimal)
            return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }

    private LocalDate toLocalDate(Object value) {
        if (value == null)
            return null;
        if (value instanceof LocalDate)
            return (LocalDate) value;
        if (value instanceof java.sql.Date)
            return ((java.sql.Date) value).toLocalDate();
        return LocalDate.parse(value.toString());
    }

    private Boolean toBoolean(Object value) {
        if (value == null)
            return null;
        if (value instanceof Boolean)
            return (Boolean) value;
        if (value instanceof Number)
            return ((Number) value).intValue() != 0;
        return Boolean.parseBoolean(value.toString());
    }
}
