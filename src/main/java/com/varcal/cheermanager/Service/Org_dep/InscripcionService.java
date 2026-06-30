package com.varcal.cheermanager.Service.Org_dep;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

@Service
public class InscripcionService {

    @Autowired
    private InscripcionRepository inscripcionRepository;

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

    // @Transactional: crea la Inscripcion y, a partir de ella, la Factura
    // correspondiente. Si la generación de la factura falla (ej. plan sin
    // valor válido), la inscripción tampoco debe quedar guardada — de lo
    // contrario tendríamos deportistas "inscritos" sin nada que cobrarles.
    @Transactional
    public Inscripcion crearInscripcion(InscripcionDto dto) {
        Inscripcion inscripcion = new Inscripcion();

        Deportista deportista = deportistaRepository.findById(dto.getDeportistaId())
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado"));

        PlanPago planPago = planPagoRepository.findById(dto.getPlanPagoId())
                .orElseThrow(() -> new RuntimeException("Plan de pago no encontrado"));

        inscripcion.setDeportista(deportista);
        inscripcion.setPlanPago(planPago);
        inscripcion.setFechaInscripcion(dto.getFechaInscripcion());
        inscripcion.setFechaVencimiento(dto.getFechaVencimiento());
        inscripcion.setEstado(Inscripcion.EstadoInscripcion.valueOf(dto.getEstado()));

        Inscripcion guardada = inscripcionRepository.save(inscripcion);

        // Generar automáticamente la factura correspondiente a esta inscripción,
        // usando el valor del plan de pago elegido (+ descuento de convenio si aplica).
        generarFacturaDesdeInscripcion(guardada, planPago);

        return guardada;
    }

    // Construye y guarda la Factura asociada a una inscripción recién creada.
    private void generarFacturaDesdeInscripcion(Inscripcion inscripcion, PlanPago planPago) {
        Deportista deportista = inscripcion.getDeportista();
        ResultadoCalculoFactura calculo = calcularTotalFactura(planPago, deportista, inscripcion.getFechaInscripcion());

        Factura factura = new Factura();
        factura.setNumeroFactura(generarNumeroFactura());
        factura.setFechaEmision(inscripcion.getFechaInscripcion());
        factura.setPersona(deportista.getPersona());
        factura.setDescripcion(calculo.descripcion);
        factura.setDescuento(calculo.descuentoAplicado);
        factura.setTotal(calculo.total);
        factura.setInscripcion(inscripcion);

        facturaRepository.save(factura);
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
    //   - el deportista tiene convenioId
    //   - el Convenio existe
    //   - la fecha de referencia cae dentro de la vigencia del Convenio
    //     (fechaInicio/fechaFin, si están definidas)
    //   - el Descuento del convenio está activo=true
    //   - la fecha de referencia cae dentro de la vigencia del Descuento
    private ResultadoCalculoFactura calcularTotalFactura(PlanPago planPago, Deportista deportista, LocalDate fechaReferencia) {
        BigDecimal valorBase = planPago.getValorMensual();
        StringBuilder descripcion = new StringBuilder("Inscripción - Plan: " + planPago.getNombre());

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
    // @Transactional: lee la inscripción, la factura asociada (si existe) y
    // guarda ambas como una sola unidad — no queremos que la inscripción
    // quede con el plan nuevo pero la factura con el total viejo a medio camino.
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

        Inscripcion guardada = inscripcionRepository.save(existente);

        // Si cambió el plan, recalculamos el total de la factura asociada (si
        // la tiene) para que no quede desincronizada con el plan vigente, y
        // dejamos constancia del cambio en HistorialPlanesDeportista (cierra
        // el registro de plan vigente anterior y abre uno nuevo).
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
        facturaRepository.findByInscripcionId(inscripcion.getId()).ifPresent(factura -> {

            // No recalculamos si la factura ya tiene pagos en estado "Pagado":
            // cambiar el total por debajo de un pago ya confirmado dejaría el
            // saldo en un estado contradictorio (ej. pagaron $80.000 de una
            // factura que ahora "vale" $60.000). En ese caso, el ajuste de
            // precio debe hacerse manualmente (nota crédito / factura nueva),
            // no de forma automática y silenciosa.
            boolean tienePagosConfirmados = pagoRepository.findByFacturaId(factura.getId()).stream()
                    .anyMatch(p -> p.getEstado() == EstadoPago.Pagado);

            if (tienePagosConfirmados) {
                throw new RuntimeException(
                        "No se puede cambiar el plan de pago: la factura " + factura.getNumeroFactura()
                                + " ya tiene pagos registrados. Anula o ajusta los pagos antes de cambiar el plan.");
            }

            ResultadoCalculoFactura calculo = calcularTotalFactura(
                    inscripcion.getPlanPago(), inscripcion.getDeportista(), factura.getFechaEmision());

            factura.setDescripcion(calculo.descripcion);
            factura.setDescuento(calculo.descuentoAplicado);
            factura.setTotal(calculo.total);
            facturaRepository.save(factura);
        });
    }

    // ===== CABO 3: qué pasa con la factura al eliminar la inscripción =====
    //
    // @Transactional: borra (o desvincula) la factura asociada y luego la
    // inscripción, como una sola unidad.
    @Transactional
    public void eliminarInscripcion(Integer id) {
        if (!inscripcionRepository.existsById(id)) {
            throw new RuntimeException("Inscripción no encontrada con ID: " + id);
        }

        facturaRepository.findByInscripcionId(id).ifPresent(factura -> {
            boolean tienePagos = !pagoRepository.findByFacturaId(factura.getId()).isEmpty();

            if (tienePagos) {
                // La factura ya tiene pagos (confirmados o pendientes) asociados:
                // no la borramos para no perder ese historial financiero. Se
                // desvincula de la inscripción (que va a desaparecer) pero la
                // factura y sus pagos quedan intactos para contabilidad.
                factura.setInscripcion(null);
                facturaRepository.save(factura);
            } else {
                // Sin pagos: es seguro eliminar la factura junto con la inscripción.
                facturaRepository.delete(factura);
            }
        });

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

    // Devuelve la factura que se generó automáticamente al crear esta inscripción.
    public Factura obtenerFacturaDeInscripcion(Integer inscripcionId) {
        if (!inscripcionRepository.existsById(inscripcionId)) {
            throw new RuntimeException("Inscripción no encontrada con ID: " + inscripcionId);
        }
        return facturaRepository.findByInscripcionId(inscripcionId)
                .orElseThrow(() -> new RuntimeException(
                        "Esta inscripción no tiene una factura asociada (puede ser una inscripción antigua, previa a este cambio)"));
    }
}