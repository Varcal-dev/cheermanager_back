package com.varcal.cheermanager.Service.Financiero;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.DTO.Financiero.TipoPlanPagoDTO;
import com.varcal.cheermanager.Models.Financiero.TipoPlanPago;
import com.varcal.cheermanager.repository.Financiero.PlanPagoRepository;
import com.varcal.cheermanager.repository.Financiero.TipoPlanPagoRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class TipoPlanPagoService {

  @Autowired
  private TipoPlanPagoRepository repo;

  @Autowired
  private PlanPagoRepository planPagoRepository;

  public List<TipoPlanPago> listarTodos() {
    return repo.findAll();
  }

  public List<TipoPlanPago> listarPorCategoria(TipoPlanPago.Categoria categoria) {
    return repo.findByCategoria(categoria);
  }

  // ===== CRUD administrable desde /tipos-planes-pago =====
  // Antes solo existía lectura (findAll/findByCategoria) para llenar el
  // dropdown al crear un plan; agregar un tipo nuevo (ej. "Mensualidad
  // Trimestral") requería insertarlo directo en la base de datos.

  public List<TipoPlanPagoDTO> listarTodosDTO(TipoPlanPago.Categoria categoria) {
    List<TipoPlanPago> tipos = categoria != null ? repo.findByCategoria(categoria) : repo.findAll();
    return tipos.stream().map(this::toDTO).collect(Collectors.toList());
  }

  public TipoPlanPagoDTO obtenerPorId(Integer id) {
    return toDTO(buscarOFallar(id));
  }

  public TipoPlanPagoDTO crear(TipoPlanPagoDTO dto) {
    validarCategoria(dto.getCategoria());

    TipoPlanPago tipo = new TipoPlanPago();
    aplicarDatos(tipo, dto);

    return toDTO(repo.save(tipo));
  }

  public TipoPlanPagoDTO actualizar(Integer id, TipoPlanPagoDTO dto) {
    TipoPlanPago tipo = buscarOFallar(id);
    validarCategoria(dto.getCategoria());

    aplicarDatos(tipo, dto);

    return toDTO(repo.save(tipo));
  }

  public void eliminar(Integer id) {
    buscarOFallar(id);

    long planesAsociados = planPagoRepository.countByTipoPlanId(id);
    if (planesAsociados > 0) {
      throw new IllegalStateException(
          "Este tipo tiene " + planesAsociados
              + " plan(es) asociado(s), no se puede eliminar. Desactívalo o reasigna esos planes primero.");
    }

    repo.deleteById(id);
  }

  private TipoPlanPago buscarOFallar(Integer id) {
    return repo.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Tipo de plan no encontrado"));
  }

  private void validarCategoria(String categoria) {
    if (categoria == null) {
      throw new IllegalArgumentException("La categoría es obligatoria");
    }
    try {
      TipoPlanPago.Categoria.valueOf(categoria.toLowerCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Categoría inválida: debe ser 'inscripcion' o 'mensualidad'");
    }
  }

  private void aplicarDatos(TipoPlanPago tipo, TipoPlanPagoDTO dto) {
    tipo.setNombre(dto.getNombre());
    tipo.setCategoria(TipoPlanPago.Categoria.valueOf(dto.getCategoria().toLowerCase()));
    tipo.setDuracionMeses(dto.getDuracionMeses());
    tipo.setDescripcion(dto.getDescripcion());
  }

  private TipoPlanPagoDTO toDTO(TipoPlanPago tipo) {
    TipoPlanPagoDTO dto = new TipoPlanPagoDTO();
    dto.setId(tipo.getId());
    dto.setNombre(tipo.getNombre());
    dto.setCategoria(tipo.getCategoria() != null ? tipo.getCategoria().name() : null);
    dto.setDuracionMeses(tipo.getDuracionMeses());
    dto.setDescripcion(tipo.getDescripcion());
    dto.setPlanesAsociados(planPagoRepository.countByTipoPlanId(tipo.getId()));
    return dto;
  }
}