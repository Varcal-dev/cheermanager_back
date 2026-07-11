package com.varcal.cheermanager.Service.Org_dep;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Org_dep.CategoriaNivelDTO;
import com.varcal.cheermanager.DTO.Org_dep.NivelDetalleDTO;
import com.varcal.cheermanager.Models.Org_dep.CategoriaNivel;
import com.varcal.cheermanager.Models.Org_dep.Nivel;
import com.varcal.cheermanager.repository.Org_dep.CategoriaNivelRepository;
import com.varcal.cheermanager.repository.Org_dep.EstadoCategoriaNivelRepository;
import com.varcal.cheermanager.repository.Org_dep.NivelRepository;

@Service
public class NivelService {

    @Autowired
    private NivelRepository nivelRepository;

    @Autowired
    private CategoriaNivelRepository categoriaNivelRepository;

    @SuppressWarnings("unused")
    @Autowired
    private EstadoCategoriaNivelRepository estadoCategoriaNivelRepository;


    // Crear nivel
    public Nivel crearNivel(Nivel nivel) {
        return nivelRepository.save(nivel);
    }

    // Obtener todos los niveles
    public List<Nivel> obtenerTodosLosNiveles() {
        return nivelRepository.findAll();
    }

    // Obtener un nivel por su ID
    public Nivel obtenerNivelPorId(Integer id) {
        return nivelRepository.findById(id).orElseThrow(() -> new RuntimeException("Nivel no encontrado"));
    }

    // Actualizar un nivel
    public Nivel actualizarNivel(Integer id, Nivel nivelActualizado) {
        Nivel nivelExistente = obtenerNivelPorId(id);
        nivelExistente.setNombre(nivelActualizado.getNombre());
        return nivelRepository.save(nivelExistente);
    }

    // Eliminar un nivel
    public void eliminarNivel(Integer id) {
        Nivel nivelExistente = obtenerNivelPorId(id);
        nivelRepository.delete(nivelExistente);
    }

    public List<NivelDetalleDTO> obtenerNivelesDetallado() {
        List<Nivel> niveles = nivelRepository.findAll();

        return niveles.stream().map(nivel -> {
            NivelDetalleDTO dto = new NivelDetalleDTO();
            dto.setId(nivel.getId());
            dto.setNombre(nivel.getNombre());

            List<CategoriaNivel> categorias = categoriaNivelRepository.findByNivelId(nivel.getId());

            List<CategoriaNivelDTO> categoriasDTO = categorias.stream().map(cat -> {
                CategoriaNivelDTO catDto = new CategoriaNivelDTO();
                catDto.setId(cat.getId());
                catDto.setNombre(cat.getNombre());
                catDto.setDivision(cat.getDivision().getNombre());

                return catDto;
            }).toList();

            dto.setCategorias(categoriasDTO);
            return dto;
        }).toList();
        
    }

}
