package com.varcal.cheermanager.repository.Personas;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Personas.EstadoPersona;

public interface EstadoPersonaRepository extends JpaRepository<EstadoPersona, Integer> {
}