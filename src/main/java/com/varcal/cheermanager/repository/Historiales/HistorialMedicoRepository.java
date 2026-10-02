package com.varcal.cheermanager.repository.Historiales;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import com.varcal.cheermanager.Models.Historiales.HistorialMedico;

// JpaSpecificationExecutor agregado para soportar el listado global filtrado
// (GET /api/historial-medico) que necesita la vista de club completo — antes
// solo se podía consultar "por persona".
public interface HistorialMedicoRepository extends JpaRepository<HistorialMedico, Integer>,
        JpaSpecificationExecutor<HistorialMedico> {
    Page<HistorialMedico> findByPersonaId(Integer personaId, org.springframework.data.domain.Pageable pageable);
    List<HistorialMedico> findByPersonaIdAndFechaRegistroAfterAndGravedadIsNotNull(Integer personaId, LocalDate fecha);
}