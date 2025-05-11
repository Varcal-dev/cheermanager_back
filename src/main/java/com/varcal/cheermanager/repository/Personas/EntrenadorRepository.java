package com.varcal.cheermanager.repository.Personas;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.varcal.cheermanager.Models.Personas.Entrenador;

@Repository
public interface EntrenadorRepository extends JpaRepository<Entrenador, Integer> {
}