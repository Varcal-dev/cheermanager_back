package com.varcal.cheermanager.repository.Personas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.varcal.cheermanager.Models.Personas.HistorialDeportistaEstado;

@Repository
public interface HistorialDeportistaEstadoRepository extends JpaRepository<HistorialDeportistaEstado, Integer> {

    List<HistorialDeportistaEstado> findByDeportistaIdOrderByFechaCambioDesc(Integer deportistaId);

    List<HistorialDeportistaEstado> findAllByOrderByFechaCambioDesc();
}
