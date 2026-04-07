package com.varcal.cheermanager.repository.Historiales;

import java.time.LocalDate;
import java.util.List;

public interface HistorialMedicoRepository extends JpaRepository<HistorialMedico, Integer> {
    Page<HistorialMedico> findByPersonaId(Integer personaId, Pageable pageable);
    List<HistorialMedico> findByPersonaIdAndFechaRegistroAfterAndGravedadIsNotNull(Integer personaId, LocalDate fecha);
}
