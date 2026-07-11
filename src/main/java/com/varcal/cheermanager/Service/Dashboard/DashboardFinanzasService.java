package com.varcal.cheermanager.Service.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Dashboard.ResumenFinanzasDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenFinanzasDTO.MorosidadPorPlanDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenFinanzasDTO.TendenciaMensualDTO;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;
import com.varcal.cheermanager.repository.Inventario.VentaRepository;

@Service
public class DashboardFinanzasService {

    private static final DateTimeFormatter FORMATO_MES = DateTimeFormatter.ofPattern("yyyy-MM");

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    public ResumenFinanzasDTO obtenerResumen() {
        ResumenFinanzasDTO dto = new ResumenFinanzasDTO();

        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate finMes = hoy.withDayOfMonth(hoy.lengthOfMonth());

        dto.setRecaudoMesActual(pagoRepository.sumarMontoPagadoEnRango(inicioMes, finMes));

        List<Factura> facturas = facturaRepository.findAll();
        calcularCarteraYFacturadoDelMes(dto, facturas, inicioMes, finMes);
        dto.setMorosidadPorPlan(calcularMorosidadPorPlan(facturas));

        BigDecimal ventasTienda = ventaRepository.findByFechaBetweenOrderByFechaDesc(inicioMes, finMes).stream()
                .map(v -> v.getTotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        dto.setIngresoTiendaMesActual(ventasTienda);

        dto.setTendenciaUltimos6Meses(calcularTendencia());

        return dto;
    }

    // Recorre las facturas una sola vez para calcular en el mismo paso:
    // cartera pendiente total, cuántas tienen saldo, cuánto se facturó este
    // mes, y cuánto de lo facturado este mes tiene descuento aplicado.
    private void calcularCarteraYFacturadoDelMes(ResumenFinanzasDTO dto, List<Factura> facturas,
            LocalDate inicioMes, LocalDate finMes) {
        BigDecimal pendienteTotal = BigDecimal.ZERO;
        long facturasConSaldo = 0;
        BigDecimal facturadoMes = BigDecimal.ZERO;
        BigDecimal facturadoConDescuentoMes = BigDecimal.ZERO;

        for (Factura f : facturas) {
            BigDecimal pagado = pagoRepository.sumarMontoPagadoPorFactura(f.getId());
            BigDecimal saldo = f.getTotal().subtract(pagado);
            if (saldo.signum() > 0) {
                pendienteTotal = pendienteTotal.add(saldo);
                facturasConSaldo++;
            }

            boolean esDelMes = !f.getFechaEmision().isBefore(inicioMes) && !f.getFechaEmision().isAfter(finMes);
            if (esDelMes) {
                facturadoMes = facturadoMes.add(f.getTotal());
                if (f.getDescuento() != null) {
                    facturadoConDescuentoMes = facturadoConDescuentoMes.add(f.getTotal());
                }
            }
        }

        dto.setCarteraPendienteTotal(pendienteTotal);
        dto.setFacturasConSaldoPendiente(facturasConSaldo);
        dto.setFacturadoMesActual(facturadoMes);
        // Nota: esto es "cuánto se facturó a clientes que tenían descuento
        // aplicado", NO "cuánto se dejó de cobrar por el descuento" — calcular
        // eso último exigiría reconstruir el precio de lista antes del
        // descuento, y no hay suficiente info en Factura para hacerlo sin
        // suponer cómo se aplicó el porcentaje al crearla.
        dto.setIngresoConDescuentoMesActual(facturadoConDescuentoMes);
    }

    private List<MorosidadPorPlanDTO> calcularMorosidadPorPlan(List<Factura> facturas) {
        Map<String, long[]> conteoPorPlan = new HashMap<>(); // [totalFacturas, conSaldoPendiente]

        for (Factura f : facturas) {
            String nombrePlan = (f.getInscripcion() != null && f.getInscripcion().getPlanPago() != null)
                    ? f.getInscripcion().getPlanPago().getNombre()
                    : "Sin plan asociado";

            BigDecimal pagado = pagoRepository.sumarMontoPagadoPorFactura(f.getId());
            boolean tieneSaldo = f.getTotal().subtract(pagado).signum() > 0;

            long[] contadores = conteoPorPlan.computeIfAbsent(nombrePlan, k -> new long[2]);
            contadores[0]++;
            if (tieneSaldo) {
                contadores[1]++;
            }
        }

        List<MorosidadPorPlanDTO> resultado = new ArrayList<>();
        for (Map.Entry<String, long[]> entry : conteoPorPlan.entrySet()) {
            MorosidadPorPlanDTO item = new MorosidadPorPlanDTO();
            item.setNombrePlan(entry.getKey());
            item.setTotalFacturas(entry.getValue()[0]);
            item.setFacturasConSaldoPendiente(entry.getValue()[1]);
            item.setPorcentajeMorosidad(entry.getValue()[0] > 0
                    ? redondear(entry.getValue()[1] * 100.0 / entry.getValue()[0])
                    : null);
            resultado.add(item);
        }
        return resultado;
    }

    // Recaudo mes a mes de los últimos 6 meses completos (incluyendo el
    // actual) — sirve para ver la tendencia sin necesitar una fecha de
    // vencimiento de factura, que no existe en el modelo actual.
    private List<TendenciaMensualDTO> calcularTendencia() {
        List<TendenciaMensualDTO> tendencia = new ArrayList<>();
        LocalDate hoy = LocalDate.now();

        for (int i = 5; i >= 0; i--) {
            LocalDate mesRef = hoy.minusMonths(i);
            LocalDate inicio = mesRef.withDayOfMonth(1);
            LocalDate fin = mesRef.withDayOfMonth(mesRef.lengthOfMonth());

            TendenciaMensualDTO item = new TendenciaMensualDTO();
            item.setMes(inicio.format(FORMATO_MES));
            item.setRecaudado(pagoRepository.sumarMontoPagadoEnRango(inicio, fin));
            tendencia.add(item);
        }
        return tendencia;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}