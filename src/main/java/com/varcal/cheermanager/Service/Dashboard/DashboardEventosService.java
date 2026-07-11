package com.varcal.cheermanager.Service.Dashboard;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Dashboard.ResumenEventosDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenEventosDTO.PremioGanadoDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenEventosDTO.ProximoEventoResumenDTO;
import com.varcal.cheermanager.Models.Eventos.Evento;
import com.varcal.cheermanager.Models.Eventos.ResultadoCompetencia;
import com.varcal.cheermanager.repository.Eventos.EventoRepository;
import com.varcal.cheermanager.repository.Eventos.GrupoEventoRepository;
import com.varcal.cheermanager.repository.Eventos.ResultadoCompetenciaRepository;

@Service
public class DashboardEventosService {

    private static final int TOP_PODIO = 3;

    @Autowired
    private EventoRepository eventoRepository;

    @Autowired
    private GrupoEventoRepository grupoEventoRepository;

    @Autowired
    private ResultadoCompetenciaRepository resultadoCompetenciaRepository;

    public ResumenEventosDTO obtenerResumen() {
        ResumenEventosDTO dto = new ResumenEventosDTO();

        dto.setProximosEventos(eventoRepository.findByFechaGreaterThanEqualOrderByFechaAsc(LocalDate.now()).stream()
                .map(this::toProximoEventoDTO)
                .collect(Collectors.toList()));

        // Recorre todos los resultados registrados — a la escala de un club
        // (una temporada tiene decenas de resultados, no miles) esto es
        // perfectamente aceptable sin necesitar una query de conteo aparte.
        List<ResultadoCompetencia> todosLosResultados = resultadoCompetenciaRepository.findAll();

        dto.setPodiosTemporada(todosLosResultados.stream()
                .filter(r -> r.getPosicion() != null && r.getPosicion() <= TOP_PODIO)
                .count());

        dto.setPremiosGanados(todosLosResultados.stream()
                .filter(r -> r.getPremio() != null)
                .map(this::toPremioGanadoDTO)
                .collect(Collectors.toList()));

        return dto;
    }

    private ProximoEventoResumenDTO toProximoEventoDTO(Evento e) {
        ProximoEventoResumenDTO dto = new ProximoEventoResumenDTO();
        dto.setEventoId(e.getId());
        dto.setNombre(e.getNombre());
        dto.setFecha(e.getFecha());
        dto.setGruposInscritos(grupoEventoRepository.findByEventoId(e.getId()).size());
        return dto;
    }

    private PremioGanadoDTO toPremioGanadoDTO(ResultadoCompetencia r) {
        PremioGanadoDTO dto = new PremioGanadoDTO();
        dto.setEventoId(r.getEvento().getId());
        dto.setNombreEvento(r.getEvento().getNombre());
        dto.setFechaEvento(r.getEvento().getFecha());
        dto.setNombreGrupo(r.getGrupoEntrenamiento() != null ? r.getGrupoEntrenamiento().getNombre() : null);
        dto.setPosicion(r.getPosicion());
        dto.setPuntaje(r.getPuntaje());
        dto.setDescripcionPremio(r.getPremio().getDescripcion());
        return dto;
    }
}