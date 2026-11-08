package com.varcal.cheermanager.repository.Financiero;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Financiero.PedidoUniformeCobro;

public interface PedidoUniformeCobroRepository extends JpaRepository<PedidoUniformeCobro, Integer> {
    List<PedidoUniformeCobro> findByPedidoId(Integer pedidoId);
}
