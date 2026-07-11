package com.varcal.cheermanager.Service.Dashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Dashboard.ResumenEvaluacionDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenEvaluacionDTO.ProgresionGrupoDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenEvaluacionDTO.ProgresionGrupoDTO.PuntoProgresionDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenEvaluacionDTO.UltimoPuntajeGrupoDTO;
import com.varcal.cheermanager.Models.Evaluaciones.Rutina.EvaluacionRutina;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.repository.Evaluaciones.Rutina.EvaluacionRutinaRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;

@Service
public class DashboardEvaluacionService {

    @Autowired
    private EvaluacionRutinaRepository evaluacionRutinaRepository;

    @Autowired
    private GrupoEntrenamientoRepository grupoEntrenamientoRepository;

    // Último puntaje registrado por cada grupo que tenga al menos una
    // evaluación — grupos sin ninguna quedan fuera de la lista (no aparecen
    // con puntaje 0, que sería engañoso).
    public ResumenEvaluacionDTO obtenerResumen() {
        List<UltimoPuntajeGrupoDTO> ultimos = new ArrayList<>();

        for (GrupoEntrenamiento grupo : grupoEntrenamientoRepository.findAll()) {
            List<EvaluacionRutina> historico = evaluacionRutinaRepository.findByGrupoIdOrderByFechaDesc(grupo.getId());
            if (historico.isEmpty()) {
                continue;
            }
            EvaluacionRutina masReciente = historico.get(0);

            UltimoPuntajeGrupoDTO dto = new UltimoPuntajeGrupoDTO();
            dto.setGrupoId(grupo.getId());
            dto.setNombreGrupo(grupo.getNombre());
            dto.setNivel(masReciente.getNivel().getNombre());
            dto.setFecha(masReciente.getFecha());
            dto.setEvento(masReciente.getEvento());
            dto.setPuntajeTotal(masReciente.getPuntajeTotal());
            ultimos.add(dto);
        }

        ResumenEvaluacionDTO resumen = new ResumenEvaluacionDTO();
        resumen.setUltimosPuntajesPorGrupo(ultimos);
        return resumen;
    }

    // Línea de tiempo completa de un grupo — la base del KPI de "progresión
    // de puntaje evento-a-evento" que se propuso en el diseño original.
    public ProgresionGrupoDTO obtenerProgresion(Integer grupoId) {
        GrupoEntrenamiento grupo = grupoEntrenamientoRepository.findById(grupoId)
                .orElseThrow(() -> new RuntimeException("Grupo no encontrado con ID: " + grupoId));

        List<EvaluacionRutina> historico = evaluacionRutinaRepository.findByGrupoIdOrderByFechaDesc(grupoId);

        ProgresionGrupoDTO dto = new ProgresionGrupoDTO();
        dto.setGrupoId(grupo.getId());
        dto.setNombreGrupo(grupo.getNombre());
        dto.setPuntos(historico.stream().map(e -> {
            PuntoProgresionDTO punto = new PuntoProgresionDTO();
            punto.setFecha(e.getFecha());
            punto.setEvento(e.getEvento());
            punto.setEsEventoOficial(e.getEventoOficial() != null);
            punto.setPuntajeTotal(e.getPuntajeTotal());
            return punto;
        }).collect(Collectors.toList()));

        return dto;
    }
}