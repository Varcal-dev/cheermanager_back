package com.varcal.cheermanager.Service.Dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Dashboard.ResumenInicioDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenInicioDTO.ProximoEventoDTO;
import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Financiero.Factura;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Service.Historiales.HistorialMedicoService;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Financiero.PagoRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.AsistenciaRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

@Service
public class DashboardInicioService {

    private static final String ESTADO_PRESENTE = "Presente";
    private static final String ESTADO_ACTIVO = "Activo";

    @Autowired
    private DeportistaRepository deportistaRepository;

    @Autowired
    private GrupoEntrenamientoRepository grupoEntrenamientoRepository;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private PagoRepository pagoRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private HistorialMedicoService historialMedicoService;

    public ResumenInicioDTO obtenerResumen() {
        ResumenInicioDTO dto = new ResumenInicioDTO();

        List<Deportista> deportistas = deportistaRepository.findAll();
        dto.setDeportistasActivos((int) deportistas.stream()
                .filter(d -> d.getEstado() != null && ESTADO_ACTIVO.equalsIgnoreCase(d.getEstado().getEstado()))
                .count());

        dto.setTotalGrupos((int) grupoEntrenamientoRepository.count());

        dto.setProximoEvento(calcularProximoEvento());

        calcularCartera(dto);

        LocalDate hoy = LocalDate.now();
        LocalDate hace7Dias = hoy.minusDays(7);
        long total = asistenciaRepository.contarTotalEnRango(hace7Dias, hoy);
        long presentes = asistenciaRepository.contarPorEstadoEnRango(hace7Dias, hoy, ESTADO_PRESENTE);
        dto.setPorcentajeAsistenciaUltimos7Dias(total > 0 ? redondear(presentes * 100.0 / total) : null);

        dto.setAlertasCondicionMedicaActiva((int) deportistas.stream()
                .filter(d -> historialMedicoService.tieneCondicionActiva(d.getPersona().getId()))
                .count());

        return dto;
    }

    private ProximoEventoDTO calcularProximoEvento() {
        List<Evento> proximos = eventoRepository.findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate.now());
        if (proximos.isEmpty()) {
            return null;
        }
        Evento e = proximos.get(0);

        ProximoEventoDTO dto = new ProximoEventoDTO();
        dto.setEventoId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setFecha(e.getFecha());
        dto.setDiasRestantes(ChronoUnit.DAYS.between(LocalDate.now(), e.getFecha()));
        return dto;
    }

    // Recorre todas las facturas para sumar el saldo pendiente real
    // (total - pagado), reutilizando PagoRepository.sumarMontoPagadoPorFactura
    // en vez de duplicar esa lógica — a la escala de un club esto es
    // aceptable en memoria.
    private void calcularCartera(ResumenInicioDTO dto) {
        List<Factura> facturas = facturaRepository.findAll();
        BigDecimal pendienteTotal = BigDecimal.ZERO;
        long facturasConSaldo = 0;

        for (Factura f : facturas) {
            BigDecimal pagado = pagoRepository.sumarMontoPagadoPorFactura(f.getId());
            BigDecimal saldo = f.getTotal().subtract(pagado);
            if (saldo.signum() > 0) {
                pendienteTotal = pendienteTotal.add(saldo);
                facturasConSaldo++;
            }
        }

        dto.setCarteraPendienteTotal(pendienteTotal);
        dto.setFacturasConSaldoPendiente(facturasConSaldo);
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}