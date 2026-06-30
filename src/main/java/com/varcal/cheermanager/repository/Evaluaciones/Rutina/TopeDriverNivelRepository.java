package com.varcal.cheermanager.repository.Evaluaciones.Rutina;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TopeDriverNivel;

public interface TopeDriverNivelRepository extends JpaRepository<TopeDriverNivel, Integer> {
    Optional<TopeDriverNivel> findByDriverIdAndNivelId(Integer driverId, Integer nivelId);
}