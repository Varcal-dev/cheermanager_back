package com.varcal.cheermanager.Service.Financiero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Financiero.CobroEventoLoteDTO;
import com.varcal.cheermanager.DTO.Financiero.CobroEventoLoteResultadoDTO;
import com.varcal.cheermanager.DTO.Financiero.CobroEventoLoteResultadoDTO.Linea;
import com.varcal.cheermanager.DTO.Financiero.FacturaManualDTO;
import com.varcal.cheermanager.DTO.Financiero.FacturaResponseDTO;
import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.GrupoEvento;
import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.Models.Personas.Persona;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Eventos.GrupoEventoRepository;
import com.varcal.cheermanager.repository.Financiero.FacturaRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;

/**
 * Cobro de un evento por lote: una factura concepto EVENTO por cada deportista de los grupos
 * inscritos en el evento. Reglas:
 *  - solo deportistas con membresia VIGENTE en el grupo (sin fecha fin);
 *  - se omiten los deportistas Retirados o Inactivos;
 *  - un deportista en varios grupos del evento se cobra UNA sola vez;
 *  - quien ya tiene un cobro vigente (no anulado) de ese evento se omite: se puede repetir
 *    la operacion sin duplicar (p. ej. para cobrar a quien entro despues al grupo);
 *  - todo en una transaccion, con el evento bloqueado: o se crean todas o ninguna.
 */
@Service
public class CobroEventoService {

    private final EventoRepository eventoRepository;
    private final GrupoEventoRepository grupoEventoRepository;
    private final DeportistaPerteneceGrupoRepository membresiaRepository;
    private final FacturaRepository facturaRepository;
    private final FacturaService facturaService;

    public CobroEventoService(EventoRepository eventoRepository,
                              GrupoEventoRepository grupoEventoRepository,
                              DeportistaPerteneceGrupoRepository membresiaRepository,
                              FacturaRepository facturaRepository,
                              FacturaService facturaService) {
        this.eventoRepository = eventoRepository;
        this.grupoEventoRepository = grupoEventoRepository;
        this.membresiaRepository = membresiaRepository;
        this.facturaRepository = facturaRepository;
        this.facturaService = facturaService;
    }

    @Transactional
    public CobroEventoLoteResultadoDTO cobrar(Integer eventoId, CobroEventoLoteDTO dto) {
        if (dto == null || dto.getMonto() == null || dto.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El monto por deportista debe ser mayor a cero");
        }
        boolean simular = Boolean.TRUE.equals(dto.getSimular());
        BigDecimal monto = dto.getMonto().setScale(2, RoundingMode.HALF_UP);

        Evento evento = eventoRepository.findByIdForUpdate(eventoId)
                .orElseThrow(() -> new RuntimeException("Evento no encontrado"));

        List<GrupoEvento> gruposEvento = grupoEventoRepository.findByEventoId(eventoId);
        if (gruposEvento.isEmpty()) {
            throw new RuntimeException("El evento no tiene grupos inscritos: inscribe primero los grupos que participan");
        }
        if (dto.getGrupoIds() != null && !dto.getGrupoIds().isEmpty()) {
            Set<Integer> inscritos = new HashSet<>();
            gruposEvento.forEach(ge -> inscritos.add(ge.getGrupoEntrenamiento().getId()));
            for (Integer gid : dto.getGrupoIds()) {
                if (!inscritos.contains(gid)) {
                    throw new RuntimeException("El grupo " + gid + " no esta inscrito en este evento");
                }
            }
            gruposEvento = gruposEvento.stream()
                    .filter(ge -> dto.getGrupoIds().contains(ge.getGrupoEntrenamiento().getId()))
                    .toList();
        }

        LocalDate hoy = LocalDate.now();
        LocalDate vencimiento = dto.getFechaVencimiento() != null
                ? dto.getFechaVencimiento() : FacturaService.calcularVencimiento(hoy);
        if (vencimiento.isBefore(hoy)) {
            throw new RuntimeException("El vencimiento no puede ser anterior a hoy");
        }
        String descripcion = dto.getDescripcion() != null && !dto.getDescripcion().isBlank()
                ? dto.getDescripcion().trim() : "Evento: " + evento.getNombre();

        // Un deportista por persona, con el primer grupo del evento en que aparece.
        Map<Integer, Object[]> porPersona = new LinkedHashMap<>();
        for (GrupoEvento ge : gruposEvento) {
            String nombreGrupo = ge.getGrupoEntrenamiento().getNombre();
            for (DeportistaPerteneceGrupo m : membresiaRepository
                    .findByGrupoIdAndFechaFinIsNull(ge.getGrupoEntrenamiento().getId())) {
                Deportista d = m.getDeportista();
                if (d == null || d.getPersona() == null) continue;
                porPersona.putIfAbsent(d.getPersona().getId(), new Object[] { d, nombreGrupo });
            }
        }

        Set<Integer> yaCobrados = new HashSet<>(facturaRepository.findPersonaIdsConCobroDeEvento(eventoId));

        List<Linea> cobrados = new ArrayList<>();
        List<Linea> omitidos = new ArrayList<>();
        int yaCobradosCount = 0;
        for (Map.Entry<Integer, Object[]> e : porPersona.entrySet()) {
            Deportista d = (Deportista) e.getValue()[0];
            String grupo = (String) e.getValue()[1];
            Persona p = d.getPersona();
            String nombre = (p.getNombre() + " " + (p.getApellidos() == null ? "" : p.getApellidos())).trim();
            String estado = d.getEstado() == null || d.getEstado().getEstado() == null
                    ? "" : d.getEstado().getEstado().trim();

            if (estado.equalsIgnoreCase("Retirado") || estado.equalsIgnoreCase("Inactivo")) {
                omitidos.add(new Linea(p.getId(), nombre, grupo, null, "Deportista " + estado.toLowerCase()));
                continue;
            }
            if (yaCobrados.contains(p.getId())) {
                yaCobradosCount++;
                omitidos.add(new Linea(p.getId(), nombre, grupo, null, "Ya tiene un cobro vigente de este evento"));
                continue;
            }
            if (simular) {
                cobrados.add(new Linea(p.getId(), nombre, grupo, null, null));
                continue;
            }
            FacturaManualDTO f = new FacturaManualDTO();
            f.setPersonaId(p.getId());
            f.setConcepto("EVENTO");
            f.setEventoId(eventoId);
            f.setDescripcion(descripcion);
            f.setTotal(monto);
            f.setFechaEmision(hoy);
            f.setFechaVencimiento(vencimiento);
            FacturaResponseDTO creada = facturaService.crearManual(f);
            cobrados.add(new Linea(p.getId(), nombre, grupo, creada.getNumeroFactura(), null));
        }

        BigDecimal total = monto.multiply(BigDecimal.valueOf(cobrados.size()));
        return new CobroEventoLoteResultadoDTO(simular, evento.getId(), evento.getNombre(), monto,
                cobrados.size(), total, yaCobradosCount, cobrados, omitidos);
    }
}