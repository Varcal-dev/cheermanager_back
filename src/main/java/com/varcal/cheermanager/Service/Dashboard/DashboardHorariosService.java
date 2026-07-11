package com.varcal.cheermanager.Service.Dashboard;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Dashboard.ResumenHorariosDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenHorariosDTO.AsistenciaPorGrupoDTO;
import com.varcal.cheermanager.DTO.Dashboard.ResumenHorariosDTO.DeportistaEnRiesgoDTO;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Horario_Asistencia.AsistenciaRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

@Service
public class DashboardHorariosService {

    // "Presente" es el nombre literal esperado en la tabla estados_asistencia
    // — mismo supuesto que ya usa el resto del sistema (ej. % de asistencia
    // individual) — si el estado se renombra algún día, esto se rompe en
    // silencio.
    private static final String ESTADO_PRESENTE = "Presente";
    private static final double UMBRAL_RIESGO = 70.0;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private GrupoEntrenamientoRepository grupoEntrenamientoRepository;

    @Autowired
    private DeportistaRepository deportistaRepository;

    public ResumenHorariosDTO obtenerResumen(int diasAnalizados) {
        LocalDate hasta = LocalDate.now();
        LocalDate desde = hasta.minusDays(diasAnalizados);

        ResumenHorariosDTO resumen = new ResumenHorariosDTO();
        resumen.setDiasAnalizados(diasAnalizados);
        resumen.setAsistenciaPorGrupo(calcularAsistenciaPorGrupo(desde, hasta));
        resumen.setDeportistasEnRiesgo(calcularDeportistasEnRiesgo(desde, hasta));

        long totalGeneral = asistenciaRepository.contarTotalEnRango(desde, hasta);
        long presentesGeneral = asistenciaRepository.contarPorEstadoEnRango(desde, hasta, ESTADO_PRESENTE);
        resumen.setPorcentajeAsistenciaGeneral(totalGeneral > 0 ? redondear(presentesGeneral * 100.0 / totalGeneral) : null);

        return resumen;
    }

    private List<AsistenciaPorGrupoDTO> calcularAsistenciaPorGrupo(LocalDate desde, LocalDate hasta) {
        List<AsistenciaPorGrupoDTO> resultado = new ArrayList<>();

        for (GrupoEntrenamiento grupo : grupoEntrenamientoRepository.findAll()) {
            long total = asistenciaRepository.contarTotalPorGrupoEnRango(grupo.getId(), desde, hasta);
            long presentes = asistenciaRepository.contarPorGrupoYEstadoEnRango(grupo.getId(), desde, hasta, ESTADO_PRESENTE);

            AsistenciaPorGrupoDTO dto = new AsistenciaPorGrupoDTO();
            dto.setGrupoId(grupo.getId());
            dto.setNombreGrupo(grupo.getNombre());
            dto.setTotalRegistros(total);
            dto.setPresentes(presentes);
            dto.setPorcentaje(total > 0 ? redondear(presentes * 100.0 / total) : null);
            resultado.add(dto);
        }

        // Grupos sin ningún registro en el rango no aportan nada al
        // dashboard — se omiten en vez de mostrarlos en 0%, que sería
        // engañoso (no es que no asistieron, es que no hay dato).
        resultado.removeIf(g -> g.getTotalRegistros() == 0);
        resultado.sort(Comparator.comparing(AsistenciaPorGrupoDTO::getNombreGrupo));
        return resultado;
    }

    // Recorre todos los deportistas — a la escala de un club (decenas a
    // pocos cientos de atletas) esto es aceptable en memoria; si el club
    // crece mucho, es candidato a resolverse con una sola query agregada
    // en vez de N llamadas al repository por deportista.
    private List<DeportistaEnRiesgoDTO> calcularDeportistasEnRiesgo(LocalDate desde, LocalDate hasta) {
        List<Deportista> todos = deportistaRepository.findByFiltros(null, null, null);
        List<DeportistaEnRiesgoDTO> enRiesgo = new ArrayList<>();

        for (Deportista d : todos) {
            long total = asistenciaRepository.countByDeportistaIdAndFechaBetween(d.getId(), desde, hasta);
            if (total == 0) {
                continue; // sin registros en el rango, no hay nada que evaluar todavía
            }

            long presentes = asistenciaRepository.contarPorDeportistaYEstadoEnRango(d.getId(), desde, hasta, ESTADO_PRESENTE);
            double porcentaje = presentes * 100.0 / total;

            if (porcentaje < UMBRAL_RIESGO) {
                DeportistaEnRiesgoDTO item = new DeportistaEnRiesgoDTO();
                item.setDeportistaId(d.getId());
                item.setNombreCompleto(d.getPersona().getNombre() + " " + d.getPersona().getApellidos());
                item.setPorcentaje(redondear(porcentaje));
                enRiesgo.add(item);
            }
        }

        enRiesgo.sort(Comparator.comparing(DeportistaEnRiesgoDTO::getPorcentaje));
        return enRiesgo;
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0; // 2 decimales
    }
}