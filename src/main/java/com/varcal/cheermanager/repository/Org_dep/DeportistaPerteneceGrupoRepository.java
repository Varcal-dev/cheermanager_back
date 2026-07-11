package com.varcal.cheermanager.repository.Org_dep;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;

public interface DeportistaPerteneceGrupoRepository extends JpaRepository<DeportistaPerteneceGrupo, Integer> {
    boolean existsByDeportistaIdAndFechaFinIsNull(Integer deportistaId);

    // Deportistas activos de un grupo (sin fechaFin = todavía pertenecen).
    // Necesario para que el entrenador sepa a quién pasarle lista al
    // registrar asistencia masiva de un grupo.
    List<DeportistaPerteneceGrupo> findByGrupoIdAndFechaFinIsNull(Integer grupoId);

    // Grupos activos de UN deportista — necesario para el dashboard de
    // Personas (distribución por división FEDECOLCHEER real, no un rango de
    // edad inventado). Solo membresías vigentes (fechaFin null), igual
    // criterio que el método de arriba.
    List<DeportistaPerteneceGrupo> findByDeportistaIdAndFechaFinIsNull(Integer deportistaId);
}