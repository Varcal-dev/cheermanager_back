package com.varcal.cheermanager.Service.Financiero;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.varcal.cheermanager.Models.Financiero.TipoPlanPago;
import com.varcal.cheermanager.repository.Financiero.TipoPlanPagoRepository;

@Service
public class TipoPlanPagoService {
  
  @Autowired
  private TipoPlanPagoRepository repo;

  public List<TipoPlanPago> listarTodos() {
    return repo.findAll();
  }
}
