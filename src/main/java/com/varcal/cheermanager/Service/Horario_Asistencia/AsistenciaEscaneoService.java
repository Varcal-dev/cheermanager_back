package com.varcal.cheermanager.Service.Horario_Asistencia;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Horario_Asistencia.AsistenciaDTO;
import com.varcal.cheermanager.Models.Horario_Asistencia.Asistencia;
import com.varcal.cheermanager.Models.Horario_Asistencia.EstadoAsistencia;
import com.varcal.cheermanager.Service.Carnet.CarnetService;
import com.varcal.cheermanager.Service.Clubes.SuscripcionService;
import com.varcal.cheermanager.repository.Horario_Asistencia.AsistenciaRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.EstadoAsistenciaRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioEntrenamientoRepository;
import com.varcal.cheermanager.repository.Org_dep.DeportistaPerteneceGrupoRepository;

/**
 * Marca "Presente" al escanear el carnet (codigo QR firmado) de un deportista en un horario.
 * Reutiliza AsistenciaService.registrar, que ya actualiza en vez de duplicar (deportista + horario + fecha).
 */
@Service
public class AsistenciaEscaneoService {

    public record Resultado(boolean registrado, boolean yaRegistrado, String nombre, List<String> grupos,
            String mensaje, String advertencia) {
    }

    @Autowired
    private CarnetService carnetService;
    @Autowired
    private AsistenciaService asistenciaService;
    @Autowired
    private AsistenciaRepository asistenciaRepository;
    @Autowired
    private EstadoAsistenciaRepository estadoRepository;
    @Autowired
    private HorarioEntrenamientoRepository horarioEntrenamientoRepository;
    @Autowired
    private DeportistaPerteneceGrupoRepository membresias;

    @Transactional
    public Resultado escanear(String codigo, Integer horarioId, LocalDate fecha) {
        if (horarioId == null) {
            throw new IllegalArgumentException("Elige el horario de la sesión.");
        }
        LocalDate dia = fecha != null ? fecha : SuscripcionService.hoy();
        if (dia.isAfter(SuscripcionService.hoy())) {
            throw new IllegalArgumentException("No se puede marcar asistencia de una fecha futura.");
        }

        // Lanza IllegalArgumentException si el codigo esta alterado o es de otro club.
        CarnetService.Carnet carnet = carnetService.verificar(codigo);

        if (!carnet.vigente()) {
            return new Resultado(false, false, carnet.nombre(), carnet.grupos(),
                    "Carnet no vigente (estado: " + carnet.estado() + "). No se marcó la asistencia.", null);
        }

        EstadoAsistencia presente = estadoRepository.findByEstado("Presente")
                .orElseThrow(() -> new IllegalStateException("No existe el estado de asistencia «Presente»."));

        Optional<Asistencia> previa = asistenciaRepository
                .findByDeportistaIdAndHorarioIdAndFecha(carnet.deportistaId(), horarioId, dia);
        if (previa.isPresent() && previa.get().getEstadoAsistencia() != null
                && presente.getId().equals(previa.get().getEstadoAsistencia().getId())) {
            return new Resultado(true, true, carnet.nombre(), carnet.grupos(),
                    "Ya estaba marcado como presente en esta sesión.", null);
        }

        AsistenciaDTO dto = new AsistenciaDTO();
        dto.setDeportistaId(carnet.deportistaId());
        dto.setHorarioId(horarioId);
        dto.setFecha(dia);
        dto.setEstadoAsistenciaId(presente.getId());
        asistenciaService.registrar(dto);

        // Aviso (no bloqueo): puede ser una clase de reposicion o un visitante, pero el entrenador debe verlo.
        Set<Integer> gruposDelHorario = horarioEntrenamientoRepository.findByHorarioId(horarioId).stream()
                .map(he -> he.getGrupoEntrenamiento().getId()).collect(Collectors.toSet());
        boolean pertenece = membresias.findByDeportistaIdAndFechaFinIsNull(carnet.deportistaId()).stream()
                .anyMatch(m -> gruposDelHorario.contains(m.getGrupo().getId()));
        String advertencia = pertenece || gruposDelHorario.isEmpty() ? null
                : "Este deportista no pertenece a ningún grupo de este horario.";

        return new Resultado(true, false, carnet.nombre(), carnet.grupos(),
                previa.isPresent() ? "Asistencia corregida a presente." : "Asistencia registrada.", advertencia);
    }
}
