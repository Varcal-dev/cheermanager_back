package com.varcal.cheermanager.repository.Evaluaciones.Rutina;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Rutina.EscalonDriverNivel;

public interface EscalonDriverNivelRepository extends JpaRepository<EscalonDriverNivel, Integer> {
    List<EscalonDriverNivel> findByDriverIdAndNivelIdOrderByOrden(Integer driverId, Integer nivelId);
}