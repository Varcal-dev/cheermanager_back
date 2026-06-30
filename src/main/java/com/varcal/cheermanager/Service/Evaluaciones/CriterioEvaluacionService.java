package com.varcal.cheermanager.Service.Evaluaciones;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Evaluaciones.CriterioEvaluacionDTO;
import com.varcal.cheermanager.DTO.Evaluaciones.CriterioEvaluacionResponseDTO;
import com.varcal.cheermanager.Models.Evaluaciones.CriterioEvaluacion;
import com.varcal.cheermanager.Models.Evaluaciones.TipoCategoria;
import com.varcal.cheermanager.repository.Evaluaciones.CriterioEvaluacionRepository;
import com.varcal.cheermanager.repository.Evaluaciones.TipoCategoriaRepository;

@Service
public class CriterioEvaluacionService {

    @Autowired
    private CriterioEvaluacionRepository criterioEvaluacionRepository;

    @Autowired
    private TipoCategoriaRepository tipoCategoriaRepository;

    public CriterioEvaluacionResponseDTO crear(CriterioEvaluacionDTO dto) {
        TipoCategoria categoria = tipoCategoriaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + dto.getCategoriaId()));

        CriterioEvaluacion criterio = new CriterioEvaluacion();
        criterio.setNombre(dto.getNombre());
        criterio.setCategoria(categoria);

        return toResponseDTO(criterioEvaluacionRepository.save(criterio));
    }

    public List<CriterioEvaluacionResponseDTO> listar() {
        return criterioEvaluacionRepository.findAll().stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public List<CriterioEvaluacionResponseDTO> listarPorCategoria(Integer categoriaId) {
        if (!tipoCategoriaRepository.existsById(categoriaId)) {
            throw new RuntimeException("Categoría no encontrada con ID: " + categoriaId);
        }
        return criterioEvaluacionRepository.findByCategoriaId(categoriaId).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    public CriterioEvaluacionResponseDTO actualizar(Integer id, CriterioEvaluacionDTO dto) {
        CriterioEvaluacion criterio = criterioEvaluacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Criterio no encontrado con ID: " + id));

        if (dto.getNombre() != null) {
            criterio.setNombre(dto.getNombre());
        }
        if (dto.getCategoriaId() != null) {
            TipoCategoria categoria = tipoCategoriaRepository.findById(dto.getCategoriaId())
                    .orElseThrow(() -> new RuntimeException("Categoría no encontrada con ID: " + dto.getCategoriaId()));
            criterio.setCategoria(categoria);
        }

        return toResponseDTO(criterioEvaluacionRepository.save(criterio));
    }

    public void eliminar(Integer id) {
        if (!criterioEvaluacionRepository.existsById(id)) {
            throw new RuntimeException("Criterio no encontrado con ID: " + id);
        }
        criterioEvaluacionRepository.deleteById(id);
    }

    private CriterioEvaluacionResponseDTO toResponseDTO(CriterioEvaluacion c) {
        CriterioEvaluacionResponseDTO dto = new CriterioEvaluacionResponseDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setCategoriaId(c.getCategoria().getId());
        dto.setCategoria(c.getCategoria().getCategoria());
        return dto;
    }
}