package com.varcal.cheermanager.Service.Evaluaciones;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Evaluaciones.EvaluacionDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.EvaluacionResponseDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.RegistroEvaluacionMasivoDTO;
import com.varcal.cheermanager.Models.Evaluaciones.CriterioEvaluacion;
import com.varcal.cheermanager.Models.Evaluaciones.Evaluacion;
import com.varcal.cheermanager.Models.Personas.Deportista;
import com.varcal.cheermanager.repository.Evaluaciones.CriterioEvaluacionRepository;
import com.varcal.cheermanager.repository.Evaluaciones.EvaluacionRepository;
import com.varcal.cheermanager.repository.Personas.DeportistaRepository;

@Service
public class EvaluacionService {

    @Autowired
    private EvaluacionRepository evaluacionRepository;

    @Autowired
    private CriterioEvaluacionRepository criterioEvaluacionRepository;

    @Autowired
    private DeportistaRepository deportistaRepository;

    public EvaluacionResponseDTO registrar(EvaluacionDTO dto) {
        Deportista deportista = deportistaRepository.findById(dto.getDeportistaId())
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + dto.getDeportistaId()));

        CriterioEvaluacion criterio = criterioEvaluacionRepository.findById(dto.getCriterioId())
                .orElseThrow(() -> new RuntimeException("Criterio no encontrado con ID: " + dto.getCriterioId()));

        validarPuntaje(dto.getPuntajeObtenido());

        Evaluacion evaluacion = new Evaluacion();
        evaluacion.setDeportista(deportista);
        evaluacion.setCriterioEvaluacion(criterio);
        evaluacion.setFecha(dto.getFecha() != null ? dto.getFecha() : LocalDate.now());
        evaluacion.setPuntajeObtenido(dto.getPuntajeObtenido());

        return toResponseDTO(evaluacionRepository.save(evaluacion));
    }

    // Caso de uso real: el entrenador evalúa a un deportista en varios
    // criterios de una sola sesión (ej. Gimnasia: flexibilidad, fuerza,
    // control). Igual que con la asistencia masiva, un error en un criterio
    // del lote no bloquea el resto.
    @Transactional
    public ResultadoRegistroMasivo registrarMasivo(RegistroEvaluacionMasivoDTO dto) {
        Deportista deportista = deportistaRepository.findById(dto.getDeportistaId())
                .orElseThrow(() -> new RuntimeException("Deportista no encontrado con ID: " + dto.getDeportistaId()));

        LocalDate fecha = dto.getFecha() != null ? dto.getFecha() : LocalDate.now();
        ResultadoRegistroMasivo resultado = new ResultadoRegistroMasivo();

        for (RegistroEvaluacionMasivoDTO.RegistroIndividual r : dto.getRegistros()) {
            try {
                CriterioEvaluacion criterio = criterioEvaluacionRepository.findById(r.getCriterioId())
                        .orElseThrow(() -> new RuntimeException("Criterio no encontrado con ID: " + r.getCriterioId()));

                validarPuntaje(r.getPuntajeObtenido());

                Evaluacion evaluacion = new Evaluacion();
                evaluacion.setDeportista(deportista);
                evaluacion.setCriterioEvaluacion(criterio);
                evaluacion.setFecha(fecha);
                evaluacion.setPuntajeObtenido(r.getPuntajeObtenido());

                Evaluacion guardada = evaluacionRepository.save(evaluacion);
                resultado.getRegistradas().add(toResponseDTO(guardada));
            } catch (RuntimeException e) {
                resultado.getErrores().put(r.getCriterioId(), e.getMessage());
            }
        }

        return resultado;
    }

    public List<EvaluacionResponseDTO> listarPorDeportista(Integer deportistaId) {
        if (!deportistaRepository.existsById(deportistaId)) {
            throw new RuntimeException("Deportista no encontrado con ID: " + deportistaId);
        }
        return evaluacionRepository.findByDeportistaIdOrderByFechaDesc(deportistaId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Progresión de un deportista en un criterio específico a lo largo del
    // tiempo (ej. para graficar la evolución de su puntaje en "Flexibilidad").
    public List<EvaluacionResponseDTO> progresionPorCriterio(Integer deportistaId, Integer criterioId) {
        return evaluacionRepository.findByDeportistaIdAndCriterioEvaluacionIdOrderByFechaAsc(deportistaId, criterioId)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<EvaluacionResponseDTO> listarPorDeportistaYRango(Integer deportistaId, LocalDate desde, LocalDate hasta) {
        return evaluacionRepository.findByDeportistaIdAndFechaBetweenOrderByFechaDesc(deportistaId, desde, hasta)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    // Última "foto" del deportista en todos los criterios de una categoría
    // (ej. todos los de "Gimnasia"), para un perfil técnico actual.
    public List<EvaluacionResponseDTO> ultimaEvaluacionPorCategoria(Integer deportistaId, Integer categoriaId) {
        return evaluacionRepository.findUltimaEvaluacionPorCategoria(deportistaId, categoriaId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public Double promedioPorCriterio(Integer deportistaId, Integer criterioId) {
        Double promedio = evaluacionRepository.promedioPorCriterio(deportistaId, criterioId);
        return promedio != null ? promedio : 0.0;
    }

    public void eliminar(Integer id) {
        if (!evaluacionRepository.existsById(id)) {
            throw new RuntimeException("Evaluación no encontrada con ID: " + id);
        }
        evaluacionRepository.deleteById(id);
    }

    // El puntaje no tiene una escala definida en el modelo (es un Integer
    // libre), así que solo se valida que no sea negativo. Si tu escala real
    // es, por ejemplo, 0-10 o 0-100, dime y agrego ese límite superior.
    private void validarPuntaje(Integer puntaje) {
        if (puntaje == null || puntaje < 0) {
            throw new RuntimeException("El puntaje obtenido debe ser un número mayor o igual a cero");
        }
    }

    private EvaluacionResponseDTO toResponseDTO(Evaluacion e) {
        EvaluacionResponseDTO dto = new EvaluacionResponseDTO();
        dto.setId(e.getId());
        dto.setDeportistaId(e.getDeportista().getId());
        dto.setNombreDeportista(e.getDeportista().getPersona().getNombre() + " "
                + e.getDeportista().getPersona().getApellidos());
        dto.setCriterioId(e.getCriterioEvaluacion().getId());
        dto.setNombreCriterio(e.getCriterioEvaluacion().getNombre());
        dto.setCategoria(e.getCriterioEvaluacion().getCategoria().getCategoria());
        dto.setFecha(e.getFecha());
        dto.setPuntajeObtenido(e.getPuntajeObtenido());
        return dto;
    }

    public static class ResultadoRegistroMasivo {
        private final List<EvaluacionResponseDTO> registradas = new java.util.ArrayList<>();
        private final java.util.Map<Integer, String> errores = new java.util.HashMap<>();

        public List<EvaluacionResponseDTO> getRegistradas() {
            return registradas;
        }

        public java.util.Map<Integer, String> getErrores() {
            return errores;
        }
    }
}