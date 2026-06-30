package com.varcal.cheermanager.Service.Horario_Asistencia;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Horario_Asistencia.AsistenciaDTO;
import com.varcal.cheermanager.DTO.Horario_Asistencia.AsistenciaResponseDTO;
import com.varcal.cheermanager.DTO.Horario_Asistencia.RegistroAsistenciaMasivoDTO;
import com.varcal.cheermanager.Models.Horario_Asistencia.Asistencia;
import com.varcal.cheermanager.Models.Horario_Asistencia.EstadoAsistencia;
import com.varcal.cheermanager.Models.Horario_Asistencia.Horario;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Horario_Asistencia.AsistenciaRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.EstadoAsistenciaRepository;
import com.varcal.cheermanager.repository.Horario_Asistencia.HorarioRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

@Service
public class AsistenciaService {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private EstadoAsistenciaRepository estadoAsistenciaRepository;

    @Autowired
    private DeportistaRepository deportistaRepository;

    // Registro de un solo deportista. Si ya existe un registro para ese
    // deportista+horario+fecha, lo actualiza en vez de duplicarlo (útil si el
    // entrenador se equivocó marcando a alguien y quiere corregirlo).
    @Transactional
    public AsistenciaResponseDTO registrar(AsistenciaDTO dto) {
        Deportista deportista = deportistaRepository.findById(dto.getDeportistaId())
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + dto.getDeportistaId()));

        Horario horario = horarioRepository.findById(dto.getHorarioId())
                .orElseThrow(() -> new RuntimeException("Horario no encontrado con ID: " + dto.getHorarioId()));

        EstadoAsistencia estado = estadoAsistenciaRepository.findById(dto.getEstadoAsistenciaId())
                .orElseThrow(() -> new RuntimeException("Estado de asistencia no encontrado con ID: " + dto.getEstadoAsistenciaId()));

        Asistencia asistencia = asistenciaRepository
                .findByDeportistaIdAndHorarioIdAndFecha(dto.getDeportistaId(), dto.getHorarioId(), dto.getFecha())
                .orElseGet(Asistencia::new);

        asistencia.setDeportista(deportista);
        asistencia.setHorario(horario);
        asistencia.setFecha(dto.getFecha());
        asistencia.setEstadoAsistencia(estado);

        Asistencia guardada = asistenciaRepository.save(asistencia);
        return toResponseDTO(guardada);
    }

    // El caso de uso real del día a día: el entrenador toma lista de todo el
    // grupo de una sola vez. Por cada deportista del request, crea o
    // actualiza su registro de asistencia (mismo criterio de "no duplicar"
    // que el registro individual). Si un deportista falla (ej. ID
    // inexistente), no se detiene todo el lote — se reporta en errores y se
    // sigue con el resto, para que un solo error de digitación no le impida
    // al entrenador guardar la asistencia de los demás.
    @Transactional
    public ResultadoRegistroMasivo registrarMasivo(RegistroAsistenciaMasivoDTO dto) {
        Horario horario = horarioRepository.findById(dto.getHorarioId())
                .orElseThrow(() -> new RuntimeException("Horario no encontrado con ID: " + dto.getHorarioId()));

        ResultadoRegistroMasivo resultado = new ResultadoRegistroMasivo();

        for (RegistroAsistenciaMasivoDTO.RegistroIndividual r : dto.getRegistros()) {
            try {
                Deportista deportista = deportistaRepository.findById(r.getDeportistaId())
                        .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + r.getDeportistaId()));

                EstadoAsistencia estado = estadoAsistenciaRepository.findById(r.getEstadoAsistenciaId())
                        .orElseThrow(() -> new RuntimeException("Estado de asistencia no encontrado con ID: " + r.getEstadoAsistenciaId()));

                Asistencia asistencia = asistenciaRepository
                        .findByDeportistaIdAndHorarioIdAndFecha(r.getDeportistaId(), dto.getHorarioId(), dto.getFecha())
                        .orElseGet(Asistencia::new);

                asistencia.setDeportista(deportista);
                asistencia.setHorario(horario);
                asistencia.setFecha(dto.getFecha());
                asistencia.setEstadoAsistencia(estado);

                Asistencia guardada = asistenciaRepository.save(asistencia);
                resultado.getRegistrados().add(toResponseDTO(guardada));
            } catch (RuntimeException e) {
                resultado.getErrores().put(r.getDeportistaId(), e.getMessage());
            }
        }

        return resultado;
    }

    public List<AsistenciaResponseDTO> listarPorGrupoYFecha(Integer grupoId, LocalDate fecha) {
        return asistenciaRepository.findByGrupoIdAndFecha(grupoId, fecha).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<AsistenciaResponseDTO> listarPorDeportista(Integer deportistaId) {
        if (!deportistaRepository.existsById(deportistaId)) {
            throw new RuntimeException("Deportista no encontrado con ID: " + deportistaId);
        }
        return asistenciaRepository.findByDeportistaIdOrderByFechaDesc(deportistaId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<AsistenciaResponseDTO> listarPorDeportistaYRango(Integer deportistaId, LocalDate desde, LocalDate hasta) {
        return asistenciaRepository.findByDeportistaIdAndFechaBetweenOrderByFechaDesc(deportistaId, desde, hasta).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Porcentaje de asistencias con estado "Presente" sobre el total de
    // registros del deportista en el rango. Si no hay ningún registro en el
    // rango, devuelve 0 en vez de lanzar división por cero.
    public double calcularPorcentajeAsistencia(Integer deportistaId, LocalDate desde, LocalDate hasta) {
        long total = asistenciaRepository.countByDeportistaIdAndFechaBetween(deportistaId, desde, hasta);
        if (total == 0) {
            return 0.0;
        }
        long presentes = asistenciaRepository.contarPorDeportistaYEstadoEnRango(deportistaId, desde, hasta, "Presente");
        return (presentes * 100.0) / total;
    }

    public void eliminar(Integer id) {
        if (!asistenciaRepository.existsById(id)) {
            throw new RuntimeException("Registro de asistencia no encontrado con ID: " + id);
        }
        asistenciaRepository.deleteById(id);
    }

    private AsistenciaResponseDTO toResponseDTO(Asistencia a) {
        AsistenciaResponseDTO dto = new AsistenciaResponseDTO();
        dto.setId(a.getId());
        dto.setDeportistaId(a.getDeportista().getId());
        dto.setNombreDeportista(a.getDeportista().getPersona().getNombre() + " "
                + a.getDeportista().getPersona().getApellidos());
        dto.setHorarioId(a.getHorario().getId());
        dto.setFecha(a.getFecha());
        dto.setEstadoAsistenciaId(a.getEstadoAsistencia().getId());
        dto.setEstadoAsistencia(a.getEstadoAsistencia().getEstado());
        return dto;
    }

    // Resultado del registro masivo: separa lo que sí se guardó de lo que
    // falló, para que el frontend pueda mostrarle al entrenador exactamente
    // qué deportistas necesitan corrección sin perder lo que ya se guardó bien.
    public static class ResultadoRegistroMasivo {
        private final List<AsistenciaResponseDTO> registrados = new java.util.ArrayList<>();
        private final java.util.Map<Integer, String> errores = new java.util.HashMap<>();

        public List<AsistenciaResponseDTO> getRegistrados() {
            return registrados;
        }

        public java.util.Map<Integer, String> getErrores() {
            return errores;
        }
    }
}