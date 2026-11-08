package com.varcal.cheermanager.repository.Personas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Personas.CertificacionEntrenador;

public interface CertificacionEntrenadorRepository extends JpaRepository<CertificacionEntrenador, Integer> {
    List<CertificacionEntrenador> findByEntrenadorIdOrderByIdDesc(Integer entrenadorId);

    List<CertificacionEntrenador> findAllByOrderByIdDesc();
}
