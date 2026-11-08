package com.varcal.cheermanager.repository.Financiero;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Financiero.PedidoUniforme;

public interface PedidoUniformeRepository extends JpaRepository<PedidoUniforme, Integer> {
    List<PedidoUniforme> findAllByOrderByFechaCreacionDescIdDesc();
}
