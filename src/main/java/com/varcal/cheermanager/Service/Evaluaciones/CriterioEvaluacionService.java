package com.varcal.cheermanager.Service.Evaluaciones;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.varcal.cheermanager.DTO.Evaluaciones.CriterioEvaluacionDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.CriterioEvaluacionResponseDTO;
import com.varcal.cheermanager.Models.Evaluaciones.CriterioEvaluacion;
import com.varcal.cheermanager.Models.Evaluaciones.TipoCategoria;
import com.varcal.cheermanager.repository.Evaluaciones.CriterioEvaluacionRepository;
import com.varcal.cheermanager.repository.Evaluaciones.EvaluacionRepository;
import com.varcal.cheermanager.repository.Evaluaciones.TipoCategoriaRepository;

/**
 * CRUD de criterios de evaluacion (por club). Errores:
 *  - IllegalArgumentException -> dato invalido (400)
 *  - IllegalStateException    -> choca con una regla o con datos existentes (409)
 *  - RuntimeException         -> no encontrado (404)
 */
@Service
public class CriterioEvaluacionService {

    private static final int NOMBRE_MAX = 150;

    @Autowired
    private CriterioEvaluacionRepository criterioEvaluacionRepository;

    @Autowired
    private TipoCategoriaRepository tipoCategoriaRepository;

    @Autowired
    private EvaluacionRepository evaluacionRepository;

    @Transactional
    public CriterioEvaluacionResponseDTO crear(CriterioEvaluacionDTO dto) {
        String nombre = validarNombre(dto.getNombre());
        if (dto.getCategoriaId() == null) {
            throw new IllegalArgumentException("La categoria es obligatoria");
        }
        TipoCategoria categoria = tipoCategoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + dto.getCategoriaId()));
        if (criterioEvaluacionRepository.existsByCategoriaIdAndNombreIgnoreCase(categoria.getId(), nombre)) {
            throw new IllegalStateException("Ya existe un criterio \"" + nombre + "\" en la categoria " + categoria.getCategoria());
        }

        CriterioEvaluacion criterio = new CriterioEvaluacion();
        criterio.setNombre(nombre);
        criterio.setCategoria(categoria);
        return toResponseDTO(criterioEvaluacionRepository.save(criterio), false);
    }

    public List<CriterioEvaluacionResponseDTO> listar() {
        Set<Integer> enUso = new HashSet<>(evaluacionRepository.findCriterioIdsEnUso());
        return criterioEvaluacionRepository.findAll().stream()
                .map(c -> toResponseDTO(c, enUso.contains(c.getId())))
                .collect(Collectors.toList());
    }

    public List<CriterioEvaluacionResponseDTO> listarPorCategoria(Integer categoriaId) {
        if (!tipoCategoriaRepository.existsById(categoriaId)) {
            throw new RuntimeException("Categoría no encontrada con ID: " + categoriaId);
        }
        Set<Integer> enUso = new HashSet<>(evaluacionRepository.findCriterioIdsEnUso());
        return criterioEvaluacionRepository.findByCategoriaId(categoriaId).stream()
                .map(c -> toResponseDTO(c, enUso.contains(c.getId())))
                .collect(Collectors.toList());
    }

    @Transactional
    public CriterioEvaluacionResponseDTO actualizar(Integer id, CriterioEvaluacionDTO dto) {
        CriterioEvaluacion criterio = criterioEvaluacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Criterio no encontrado con ID: " + id));
        boolean enUso = evaluacionRepository.existsByCriterioEvaluacionId(id);

        String nombre = dto.getNombre() != null ? validarNombre(dto.getNombre()) : criterio.getNombre();
        TipoCategoria categoria = criterio.getCategoria();
        if (dto.getCategoriaId() != null && !dto.getCategoriaId().equals(categoria.getId())) {
            if (enUso) {
                throw new IllegalStateException("Este criterio ya tiene evaluaciones registradas: no se puede mover de categoria "
                        + "(cambiaria el significado del historial). Crea uno nuevo en la otra categoria.");
            }
            categoria = tipoCategoriaRepository.findById(dto.getCategoriaId())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + dto.getCategoriaId()));
        }
        if (criterioEvaluacionRepository.existsByCategoriaIdAndNombreIgnoreCaseAndIdNot(categoria.getId(), nombre, id)) {
            throw new IllegalStateException("Ya existe un criterio \"" + nombre + "\" en la categoria " + categoria.getCategoria());
        }

        criterio.setNombre(nombre);
        criterio.setCategoria(categoria);
        return toResponseDTO(criterioEvaluacionRepository.save(criterio), enUso);
    }

    @Transactional
    public void eliminar(Integer id) {
        if (!criterioEvaluacionRepository.existsById(id)) {
            throw new RuntimeException("Criterio no encontrado con ID: " + id);
        }
        if (evaluacionRepository.existsByCriterioEvaluacionId(id)) {
            throw new IllegalStateException("Este criterio ya tiene evaluaciones registradas y no se puede eliminar. "
                    + "Si ya no lo usas, deja de seleccionarlo al evaluar.");
        }
        criterioEvaluacionRepository.deleteById(id);
    }

    private String validarNombre(String nombre) {
        String n = nombre == null ? "" : nombre.trim();
        if (n.isEmpty()) {
            throw new IllegalArgumentException("El nombre del criterio es obligatorio");
        }
        if (n.length() > NOMBRE_MAX) {
            throw new IllegalArgumentException("El nombre no puede pasar de " + NOMBRE_MAX + " caracteres");
        }
        return n;
    }

    private CriterioEvaluacionResponseDTO toResponseDTO(CriterioEvaluacion c, boolean enUso) {
        CriterioEvaluacionResponseDTO dto = new CriterioEvaluacionResponseDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setCategoriaId(c.getCategoria().getId());
        dto.setCategoria(c.getCategoria().getCategoria());
        dto.setEnUso(enUso);
        return dto;
    }
}