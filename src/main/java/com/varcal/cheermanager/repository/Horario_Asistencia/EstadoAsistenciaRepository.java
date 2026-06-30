package com.varcal.cheermanager.repository.Horario_Asistencia;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Horario_Asistencia.EstadoAsistencia;

public interface EstadoAsistenciaRepository extends JpaRepository<EstadoAsistencia, Integer> {
    Optional<EstadoAsistencia> findByEstado(String estado);
}