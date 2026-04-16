package com.varcal.cheermanager.repository.Historiales;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import com.varcal.cheermanager.Models.Historiales.HistorialMedico;

public interface HistorialMedicoRepository extends JpaRepository<HistorialMedico, Integer> {
    Page<HistorialMedico> findByPersonaId(Integer personaId, org.springframework.data.domain.Pageable pageable);
    List<HistorialMedico> findByPersonaIdAndFechaRegistroAfterAndGravedadIsNotNull(Integer personaId, LocalDate fecha);
}
