package com.varcal.cheermanager.Service.Inventario;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Inventario.CategoriaProductoDTO;
import com.varcal.cheermanager.Models.Inventario.CategoriaProducto;
import com.varcal.cheermanager.repository.Inventario.CategoriaProductoRepository;

@Service
public class CategoriaProductoService {

    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    public List<CategoriaProducto> listarTodas() {
        return categoriaProductoRepository.findAll();
    }

    public CategoriaProducto obtenerPorId(Integer id) {
        return categoriaProductoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Categoría de producto no encontrada con ID: " + id));
    }

    public CategoriaProducto crear(CategoriaProductoDTO dto) {
        String codigo = validarCodigo(dto.getCodigo());

        categoriaProductoRepository.findByCodigo(codigo).ifPresent(c -> {
            throw new RuntimeException("Ya existe una categoría con el código '" + codigo + "'");
        });
        categoriaProductoRepository.findByNombre(dto.getNombre()).ifPresent(c -> {
            throw new RuntimeException("Ya existe una categoría llamada '" + dto.getNombre() + "'");
        });

        CategoriaProducto categoria = new CategoriaProducto();
        categoria.setNombre(dto.getNombre());
        categoria.setCodigo(codigo);
        categoria.setDescripcion(dto.getDescripcion());
        return categoriaProductoRepository.save(categoria);
    }

    public CategoriaProducto actualizar(Integer id, CategoriaProductoDTO dto) {
        CategoriaProducto categoria = obtenerPorId(id);
        String codigo = validarCodigo(dto.getCodigo());

        categoriaProductoRepository.findByCodigo(codigo)
                .filter(c -> !c.getId().equals(id))
                .ifPresent(c -> { throw new RuntimeException("Ya existe otra categoría con el código '" + codigo + "'"); });

        categoria.setNombre(dto.getNombre());
        categoria.setCodigo(codigo);
        categoria.setDescripcion(dto.getDescripcion());
        return categoriaProductoRepository.save(categoria);
    }

    public void eliminar(Integer id) {
        if (!categoriaProductoRepository.existsById(id)) {
            throw new RuntimeException("Categoría de producto no encontrada con ID: " + id);
        }
        categoriaProductoRepository.deleteById(id);
    }

    // El formato del SKU (RAT-[CAT]-...) exige exactamente 3 letras
    // mayúsculas para el segmento de categoría — se normaliza y valida acá,
    // en el único lugar donde se crean/editan categorías.
    private String validarCodigo(String codigo) {
        if (codigo == null || !codigo.matches("[A-Za-z]{3}")) {
            throw new RuntimeException("El código de categoría debe tener exactamente 3 letras (ej. ROP, CAL, DIG)");
        }
        return codigo.toUpperCase();
    }
}