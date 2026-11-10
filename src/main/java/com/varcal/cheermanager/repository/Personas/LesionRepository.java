package com.varcal.cheermanager.repository.Personas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Personas.Lesion;
import com.varcal.cheermanager.Models.Personas.Lesion.Estado;
import com.varcal.cheermanager.Models.Personas.Lesion.Zona;

public interface LesionRepository extends JpaRepository<Lesion, Integer> {
    List<Lesion> findAllByOrderByFechaLesionDescIdDesc();
    List<Lesion> findByEstadoNotOrderByFechaLesionDescIdDesc(Estado estado);
    List<Lesion> findByDeportistaIdOrderByFechaLesionDescIdDesc(Integer deportistaId);
    boolean existsByDeportistaIdAndZonaAndEstadoNot(Integer deportistaId, Zona zona, Estado estado);
}
