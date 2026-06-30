package com.varcal.cheermanager.repository.Historiales;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Historiales.HistorialPlanesDeportista;

public interface HistorialPlanesDeportistaRepository extends JpaRepository<HistorialPlanesDeportista, Integer> {
    List<HistorialPlanesDeportista> findByDeportistaIdOrderByFechaInicioDesc(Integer deportistaId);

    // El "plan vigente" de un deportista es el registro de su historial que
    // todavía no tiene fechaFin (nadie lo cerró aún).
    Optional<HistorialPlanesDeportista> findByDeportistaIdAndFechaFinIsNull(Integer deportistaId);
}