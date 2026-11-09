package com.varcal.cheermanager.repository.Legal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Legal.PreRegistro;

public interface PreRegistroRepository extends JpaRepository<PreRegistro, Integer> {
    boolean existsByCorreoIgnoreCase(String correo);
    List<PreRegistro> findAllByOrderByFechaConsentimientoDescIdDesc();
}
