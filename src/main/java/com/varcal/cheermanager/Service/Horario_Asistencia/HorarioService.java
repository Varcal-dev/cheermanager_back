package com.varcal.cheermanager.Service.Horario_Asistencia;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Horario_Asistencia.AsignarHorarioGrupoDTO;
import com.varcal.cheermanager.DTO.Horario_Asistencia.HorarioDTO;
import com.varcal.cheermanager.DTO.Horario_Asistencia.HorarioResponseDTO;
import com.varcal.cheermanager.Models.Horario_Asistencia.Horario;
import com.varcal.cheermanager.Models.Horario_Asistencia.HorarioEntrenamiento;
import com.varcal.cheermanager.Models.Org_dep.DiaSemana;
import com.varcal.cheermanager.Models.Org_dep.GrupoEntrenamiento;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioEntrenamientoRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioRepository;
import com.varcal.cheermanager.repository.Org_dep.DiaSemanaRepository;
import com.varcal.cheermanager.repository.Org_dep.GrupoEntrenamientoRepository;

@Service
public class HorarioService {

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private HorarioEntrenamientoRepository horarioEntrenamientoRepository;

    @Autowired
    private DiaSemanaRepository diaSemanaRepository;

    @Autowired
    private GrupoEntrenamientoRepository grupoEntrenamientoRepository;

    public Horario crear(HorarioDTO dto) {
        DiaSemana dia = diaSemanaRepository.findById(dto.getDiaId())
                .orElseThrow(() -> new RuntimeException("Día no encontrado con ID: " + dto.getDiaId()));

        if (dto.getHoraInicio() != null && dto.getHoraFin() != null
                && !dto.getHoraInicio().isBefore(dto.getHoraFin())) {
            throw new RuntimeException("La hora de inicio debe ser anterior a la hora de fin");
        }

        Horario horario = new Horario();
        horario.setDia(dia);
        horario.setHoraInicio(dto.getHoraInicio());
        horario.setHoraFin(dto.getHoraFin());

        return horarioRepository.save(horario);
    }

    public Horario actualizar(Integer id, HorarioDTO dto) {
        Horario horario = horarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado con ID: " + id));

        if (dto.getDiaId() != null) {
            DiaSemana dia = diaSemanaRepository.findById(dto.getDiaId())
                    .orElseThrow(() -> new RuntimeException("Día no encontrado con ID: " + dto.getDiaId()));
            horario.setDia(dia);
        }
        if (dto.getHoraInicio() != null) {
            horario.setHoraInicio(dto.getHoraInicio());
        }
        if (dto.getHoraFin() != null) {
            horario.setHoraFin(dto.getHoraFin());
        }

        if (horario.getHoraInicio() != null && horario.getHoraFin() != null
                && !horario.getHoraInicio().isBefore(horario.getHoraFin())) {
            throw new RuntimeException("La hora de inicio debe ser anterior a la hora de fin");
        }

        return horarioRepository.save(horario);
    }

    public List<Horario> listar() {
        return horarioRepository.findAll();
    }

    public HorarioResponseDTO obtenerPorId(Integer id) {
        Horario horario = horarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Horario no encontrado con ID: " + id));
        return toResponseDTO(horario);
    }

    public List<HorarioResponseDTO> listarConGrupos() {
        return horarioRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<Horario> listarPorGrupo(Integer grupoId) {
        if (!grupoEntrenamientoRepository.existsById(grupoId)) {
            throw new RuntimeException("Grupo de entrenamiento no encontrado con ID: " + grupoId);
        }
        return horarioRepository.findByGrupoEntrenamientoId(grupoId);
    }

    // Vincula un Horario existente a un GrupoEntrenamiento existente. No
    // valida cruces de horario entre grupos distintos a propósito: un mismo
    // bloque de horario puede usarse para más de un grupo (ej. dos niveles
    // que entrenan en gimnasios distintos a la misma hora). Si en tu caso eso
    // nunca debería pasar, lo agregamos como validación aparte.
    public HorarioEntrenamiento asignarAGrupo(AsignarHorarioGrupoDTO dto) {
        Horario horario = horarioRepository.findById(dto.getHorarioId())
                .orElseThrow(() -> new RuntimeException("Horario no encontrado con ID: " + dto.getHorarioId()));

        GrupoEntrenamiento grupo = grupoEntrenamientoRepository.findById(dto.getGrupoEntrenamientoId())
                .orElseThrow(() -> new RuntimeException("Grupo de entrenamiento no encontrado con ID: " + dto.getGrupoEntrenamientoId()));

        if (horarioEntrenamientoRepository.existsByHorarioIdAndGrupoEntrenamientoId(horario.getId(), grupo.getId())) {
            throw new RuntimeException("Este horario ya está asignado a este grupo");
        }

        HorarioEntrenamiento he = new HorarioEntrenamiento();
        he.setHorario(horario);
        he.setGrupoEntrenamiento(grupo);

        return horarioEntrenamientoRepository.save(he);
    }

    public void desasignarDeGrupo(Integer horarioEntrenamientoId) {
        if (!horarioEntrenamientoRepository.existsById(horarioEntrenamientoId)) {
            throw new RuntimeException("Asignación de horario no encontrada con ID: " + horarioEntrenamientoId);
        }
        horarioEntrenamientoRepository.deleteById(horarioEntrenamientoId);
    }

    // @Transactional: si el horario tiene asignaciones a grupos
    // (HorarioEntrenamiento), las elimina primero y luego el horario, como
    // una sola unidad — evita dejar registros huérfanos apuntando a un
    // horario_id que ya no existe.
    @Transactional
    public void eliminar(Integer id) {
        if (!horarioRepository.existsById(id)) {
            throw new RuntimeException("Horario no encontrado con ID: " + id);
        }
        List<HorarioEntrenamiento> asignaciones = horarioEntrenamientoRepository.findByHorarioId(id);
        if (!asignaciones.isEmpty()) {
            horarioEntrenamientoRepository.deleteAll(asignaciones);
        }
        horarioRepository.deleteById(id);
    }

    private HorarioResponseDTO toResponseDTO(Horario horario) {
        HorarioResponseDTO dto = new HorarioResponseDTO();
        dto.setId(horario.getId());
        dto.setDiaId(horario.getDia().getId());
        dto.setDia(horario.getDia().getDia());
        dto.setHoraInicio(horario.getHoraInicio());
        dto.setHoraFin(horario.getHoraFin());

        List<HorarioEntrenamiento> asignaciones = horarioEntrenamientoRepository.findByHorarioId(horario.getId());
        dto.setGrupos(asignaciones.stream().map(he -> {
            HorarioResponseDTO.GrupoAsociado g = new HorarioResponseDTO.GrupoAsociado();
            g.setHorarioEntrenamientoId(he.getId());
            g.setGrupoId(he.getGrupoEntrenamiento().getId());
            g.setNombreGrupo(he.getGrupoEntrenamiento().getNombre());
            return g; 
        }).collect(Collectors.toList()));

        return dto;
    }
}