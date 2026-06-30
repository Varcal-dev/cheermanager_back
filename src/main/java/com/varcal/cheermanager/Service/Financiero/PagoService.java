package com.varcal.cheermanager.Service.Financiero;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Financiero.PagoDTO;
import com.varcal.cheermanager.DTO.Financiero.PagoResponseDTO;
import com.varcal.cheermanager.Models.Financiero.EstadoPago;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Financiero.MetodoPago;
import com.varcal.cheermanager.Models.Financiero.Pago;
import com.varcal.cheermanager.Models.Financiero.TipoPago;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.MetodoPagoRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;
import com.varcal.cheermanager.repository.Financiero.TipoPagoRepository;

@Service
public class PagoService {

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private TipoPagoRepository tipoPagoRepository;

    @Autowired
    private MetodoPagoRepository metodoPagoRepository;

    // @Transactional: lee el saldo actual de la factura y luego guarda el pago.
    // Ambas operaciones deben verse como una unidad para evitar que dos pagos
    // simultáneos sobre la misma factura se cuelen sin validar correctamente
    // el saldo (carrera entre el cálculo y el guardado).
    @Transactional
    public PagoResponseDTO registrarPago(PagoDTO dto) {
        Factura factura = facturaRepository.findById(dto.getFacturaId())
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con ID: " + dto.getFacturaId()));

        TipoPago tipoPago = tipoPagoRepository.findById(dto.getTipoPagoId())
                .orElseThrow(() -> new RuntimeException("Tipo de pago no encontrado con ID: " + dto.getTipoPagoId()));

        MetodoPago metodoPago = metodoPagoRepository.findById(dto.getMetodoPagoId())
                .orElseThrow(() -> new RuntimeException("Método de pago no encontrado con ID: " + dto.getMetodoPagoId()));

        if (dto.getMonto() == null || dto.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto del pago debe ser mayor a cero");
        }

        EstadoPago estado;
        try {
            estado = EstadoPago.valueOf(dto.getEstado());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RuntimeException("Estado de pago inválido: " + dto.getEstado()
                    + ". Valores permitidos: Pagado, Pendiente, Vencido");
        }

        // Si el pago se registra como "Pagado", no permitir que la suma de pagos
        // pagados supere el total de la factura (evita sobrepago por error de digitación).
        if (estado == EstadoPago.Pagado) {
            BigDecimal yaPagado = pagoRepository.sumarMontoPagadoPorFactura(factura.getId());
            BigDecimal nuevoTotalPagado = yaPagado.add(dto.getMonto());
            if (nuevoTotalPagado.compareTo(factura.getTotal()) > 0) {
                BigDecimal disponible = factura.getTotal().subtract(yaPagado);
                throw new RuntimeException(
                        "El pago excede el saldo pendiente de la factura. Saldo disponible: " + disponible);
            }
        }

        Pago pago = new Pago();
        pago.setFactura(factura);
        pago.setTipoPago(tipoPago);
        pago.setMetodoPago(metodoPago);
        pago.setFecha(dto.getFecha());
        pago.setEstado(estado);
        pago.setMonto(dto.getMonto());

        Pago guardado = pagoRepository.save(pago);
        return toResponseDTO(guardado);
    }

    public List<PagoResponseDTO> listar() {
        return pagoRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public PagoResponseDTO obtenerPorId(Integer id) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con ID: " + id));
        return toResponseDTO(pago);
    }

    public List<PagoResponseDTO> listarPorFactura(Integer facturaId) {
        if (!facturaRepository.existsById(facturaId)) {
            throw new RuntimeException("Factura no encontrada con ID: " + facturaId);
        }
        return pagoRepository.findByFacturaId(facturaId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<PagoResponseDTO> listarPorPersona(Integer personaId) {
        return pagoRepository.findByPersonaId(personaId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // @Transactional: lee el pago, valida estado nuevo y guarda; se mantiene
    // como unidad por el mismo motivo que registrarPago.
    @Transactional
    public PagoResponseDTO actualizarEstado(Integer id, String nuevoEstado) {
        Pago pago = pagoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado con ID: " + id));

        EstadoPago estado;
        try {
            estado = EstadoPago.valueOf(nuevoEstado);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RuntimeException("Estado de pago inválido: " + nuevoEstado
                    + ". Valores permitidos: Pagado, Pendiente, Vencido");
        }

        // Misma validación de sobrepago si se está marcando como "Pagado" ahora.
        if (estado == EstadoPago.Pagado && pago.getEstado() != EstadoPago.Pagado) {
            BigDecimal yaPagado = pagoRepository.sumarMontoPagadoPorFactura(pago.getFactura().getId());
            BigDecimal nuevoTotalPagado = yaPagado.add(pago.getMonto());
            if (nuevoTotalPagado.compareTo(pago.getFactura().getTotal()) > 0) {
                BigDecimal disponible = pago.getFactura().getTotal().subtract(yaPagado);
                throw new RuntimeException(
                        "No se puede marcar como Pagado: excede el saldo pendiente de la factura. Saldo disponible: "
                                + disponible);
            }
        }

        pago.setEstado(estado);
        Pago guardado = pagoRepository.save(pago);
        return toResponseDTO(guardado);
    }

    public void eliminar(Integer id) {
        if (!pagoRepository.existsById(id)) {
            throw new RuntimeException("Pago no encontrado con ID: " + id);
        }
        pagoRepository.deleteById(id);
    }

    // Saldo pendiente de una factura: total de la factura menos lo ya pagado
    // (solo pagos en estado "Pagado"). Lo usa el frontend para saber cuánto le
    // falta cobrar a una persona.
    public BigDecimal calcularSaldoPendiente(Integer facturaId) {
        Factura factura = facturaRepository.findById(facturaId)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con ID: " + facturaId));
        BigDecimal yaPagado = pagoRepository.sumarMontoPagadoPorFactura(facturaId);
        return factura.getTotal().subtract(yaPagado);
    }

    private PagoResponseDTO toResponseDTO(Pago pago) {
        PagoResponseDTO dto = new PagoResponseDTO();
        dto.setId(pago.getId());

        Factura factura = pago.getFactura();
        dto.setFacturaId(factura.getId());
        dto.setNumeroFactura(factura.getNumeroFactura());
        dto.setTotalFactura(factura.getTotal());

        dto.setTipoPagoId(pago.getTipoPago().getId());
        dto.setTipoPago(pago.getTipoPago().getTipo());

        dto.setMetodoPagoId(pago.getMetodoPago().getId());
        dto.setMetodoPago(pago.getMetodoPago().getMetodo());

        dto.setFecha(pago.getFecha());
        dto.setEstado(pago.getEstado().name());
        dto.setMonto(pago.getMonto());

        BigDecimal yaPagado = pagoRepository.sumarMontoPagadoPorFactura(factura.getId());
        dto.setSaldoPendienteFactura(factura.getTotal().subtract(yaPagado));

        return dto;
    }
}