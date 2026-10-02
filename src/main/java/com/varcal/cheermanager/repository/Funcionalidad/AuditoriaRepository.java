package com.varcal.cheermanager.repository.Funcionalidad;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.varcal.cheermanager.Models.Funcionalidad.Auditoria;

// Auditoria.java (Model) existía sin ningún Repository — no había forma de
// leer los registros de auditoría desde código. Ahora además implementa
// JpaSpecificationExecutor para poder combinar filtros (tabla + acción +
// usuario + rango de fechas + texto) sin explotar en decenas de métodos
// findByXAndYAndZ...
public interface AuditoriaRepository extends JpaRepository<Auditoria, Integer>,
        JpaSpecificationExecutor<Auditoria> {

    List<Auditoria> findByFechaAfterOrderByFechaDesc(LocalDateTime desde, Pageable pageable);

    // Alimenta el filtro "Tabla" del front sin tener que mantener una lista
    // fija a mano cada vez que se audita una entidad nueva.
    @Query("SELECT DISTINCT a.tablaAfectada FROM Auditoria a ORDER BY a.tablaAfectada")
    List<String> listarTablasAfectadas();
}