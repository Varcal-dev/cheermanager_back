package com.varcal.cheermanager.repository.Org_dep;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Org_dep.DeportistaPerteneceGrupo;

public interface DeportistaPerteneceGrupoRepository extends JpaRepository<DeportistaPerteneceGrupo, Integer> {
    boolean existsByDeportistaIdAndFechaFinIsNull(Integer deportistaId);

    // Deportistas activos de un grupo (sin fechaFin = todavía pertenecen).
    // Necesario para que el entrenador sepa a quién pasarle lista al
    // registrar asistencia masiva de un grupo, y también para calcular el
    // cupo actual del grupo en obtenerCandidatosGrupo.
    List<DeportistaPerteneceGrupo> findByGrupoIdAndFechaFinIsNull(Integer grupoId);

    // Grupos activos de UN deportista — necesario para el dashboard de
    // Personas (distribución por división FEDECOLCHEER real, no un rango de
    // edad inventado). Solo membresías vigentes (fechaFin null), igual
    // criterio que el método de arriba.
    List<DeportistaPerteneceGrupo> findByDeportistaIdAndFechaFinIsNull(Integer deportistaId);

    // Agregado: la membresía activa puntual de UN deportista en UN grupo,
    // necesaria para "quitar" (cerrar) esa membresía sin tocar el resto de
    // su historial ni el de otros deportistas del grupo.
    Optional<DeportistaPerteneceGrupo> findByGrupoIdAndDeportistaIdAndFechaFinIsNull(Integer grupoId,
            Integer deportistaId);
}