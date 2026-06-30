package com.varcal.cheermanager.repository.Funcionalidad;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Funcionalidad.LogAcceso;

public interface LogAccesoRepository extends JpaRepository<LogAcceso, Integer> {

    Page<LogAcceso> findByUsuarioIdOrderByFechaDesc(Integer usuarioId, Pageable pageable);

    Page<LogAcceso> findByAccionOrderByFechaDesc(String accion, Pageable pageable);

    List<LogAcceso> findByEmailIntentoAndFechaAfter(String emailIntento, LocalDateTime desde);

    Page<LogAcceso> findByFechaBetweenOrderByFechaDesc(LocalDateTime desde, LocalDateTime hasta, Pageable pageable);
}