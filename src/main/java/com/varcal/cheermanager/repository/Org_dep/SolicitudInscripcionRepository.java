package com.varcal.cheermanager.repository.Org_dep;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Org_dep.SolicitudInscripcion;
import com.varcal.cheermanager.Models.Org_dep.SolicitudInscripcion.Estado;

public interface SolicitudInscripcionRepository extends JpaRepository<SolicitudInscripcion, Integer> {
    List<SolicitudInscripcion> findAllByOrderByFechaSolicitudDesc();

    List<SolicitudInscripcion> findByEstadoOrderByFechaSolicitudDesc(Estado estado);

    List<SolicitudInscripcion> findByDeportistaIdOrderByFechaSolicitudDesc(Integer deportistaId);

    boolean existsByDeportistaIdAndTemporadaIdAndEstadoIn(Integer deportistaId, Integer temporadaId,
            Collection<Estado> estados);
}
