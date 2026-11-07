package com.varcal.cheermanager.repository.Evaluaciones.Habilidades;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.DeportistaHabilidad;

public interface DeportistaHabilidadRepository extends JpaRepository<DeportistaHabilidad, Integer> {
    List<DeportistaHabilidad> findByDeportistaId(Integer deportistaId);

    Optional<DeportistaHabilidad> findByDeportistaIdAndHabilidadId(Integer deportistaId, Integer habilidadId);
}
