package com.varcal.cheermanager.repository.Evaluaciones;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Evaluaciones.Evaluacion;

public interface EvaluacionRepository extends JpaRepository<Evaluacion, Integer> {

    // Un criterio con evaluaciones registradas no se borra ni cambia de categoria (rompe el historial).
    boolean existsByCriterioEvaluacionId(Integer criterioId);

    @Query("SELECT DISTINCT e.criterioEvaluacion.id FROM Evaluacion e")
    List<Integer> findCriterioIdsEnUso();

    List<Evaluacion> findByDeportistaIdOrderByFechaDesc(Integer deportistaId);

    List<Evaluacion> findByDeportistaIdAndCriterioEvaluacionIdOrderByFechaAsc(Integer deportistaId, Integer criterioId);

    List<Evaluacion> findByDeportistaIdAndFechaBetweenOrderByFechaDesc(Integer deportistaId, LocalDate desde, LocalDate hasta);

    // Última evaluación de un deportista para cada criterio de una categoría
    // (ej. "Gimnasia"), para construir una foto del estado técnico actual sin
    // traer todo el historial completo.
    @Query("SELECT e FROM Evaluacion e WHERE e.deportista.id = :deportistaId "
            + "AND e.criterioEvaluacion.categoria.id = :categoriaId "
            + "AND e.fecha = (SELECT MAX(e2.fecha) FROM Evaluacion e2 "
            + "WHERE e2.deportista.id = e.deportista.id AND e2.criterioEvaluacion.id = e.criterioEvaluacion.id)")
    List<Evaluacion> findUltimaEvaluacionPorCategoria(@Param("deportistaId") Integer deportistaId,
            @Param("categoriaId") Integer categoriaId);

    @Query("SELECT AVG(e.puntajeObtenido) FROM Evaluacion e WHERE e.deportista.id = :deportistaId "
            + "AND e.criterioEvaluacion.id = :criterioId")
    Double promedioPorCriterio(@Param("deportistaId") Integer deportistaId, @Param("criterioId") Integer criterioId);

    // ---- S7: pantalla de Evaluaciones ----
    boolean existsByDeportistaIdAndCriterioEvaluacionIdAndFecha(Integer deportistaId, Integer criterioId, LocalDate fecha);

    boolean existsByDeportistaIdAndCriterioEvaluacionIdAndFechaAndIdNot(Integer deportistaId, Integer criterioId,
            LocalDate fecha, Integer id);

    /**
     * Busqueda paginada con filtros opcionales (null = sin filtro). grupoId = deportistas que HOY pertenecen al grupo;
     * texto = patron LIKE ya en minusculas sobre "nombre apellidos".
     */
    @Query(value = "SELECT e FROM Evaluacion e JOIN FETCH e.deportista d JOIN FETCH d.persona p "
            + "JOIN FETCH e.criterioEvaluacion c JOIN FETCH c.categoria cat "
            + "WHERE (:deportistaId IS NULL OR d.id = :deportistaId) "
            + "AND (:criterioId IS NULL OR c.id = :criterioId) "
            + "AND (:categoriaId IS NULL OR cat.id = :categoriaId) "
            + "AND (:desde IS NULL OR e.fecha >= :desde) AND (:hasta IS NULL OR e.fecha <= :hasta) "
            + "AND (:grupoId IS NULL OR d.id IN (SELECT m.deportista.id FROM DeportistaPerteneceGrupo m "
            + "WHERE m.grupo.id = :grupoId AND m.fechaFin IS NULL)) "
            + "AND (:texto IS NULL OR LOWER(CONCAT(p.nombre, ' ', p.apellidos)) LIKE :texto) "
            + "ORDER BY e.fecha DESC, e.id DESC",
            countQuery = "SELECT COUNT(e) FROM Evaluacion e JOIN e.deportista d JOIN d.persona p "
                    + "JOIN e.criterioEvaluacion c JOIN c.categoria cat "
                    + "WHERE (:deportistaId IS NULL OR d.id = :deportistaId) "
                    + "AND (:criterioId IS NULL OR c.id = :criterioId) "
                    + "AND (:categoriaId IS NULL OR cat.id = :categoriaId) "
                    + "AND (:desde IS NULL OR e.fecha >= :desde) AND (:hasta IS NULL OR e.fecha <= :hasta) "
                    + "AND (:grupoId IS NULL OR d.id IN (SELECT m.deportista.id FROM DeportistaPerteneceGrupo m "
                    + "WHERE m.grupo.id = :grupoId AND m.fechaFin IS NULL)) "
                    + "AND (:texto IS NULL OR LOWER(CONCAT(p.nombre, ' ', p.apellidos)) LIKE :texto)")
    Page<Evaluacion> buscar(@Param("deportistaId") Integer deportistaId, @Param("grupoId") Integer grupoId,
            @Param("categoriaId") Integer categoriaId, @Param("criterioId") Integer criterioId,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta, @Param("texto") String texto,
            Pageable pageable);

    /** Promedio por criterio (filas: criterioId, nombre, categoria, promedio, evaluaciones, deportistas distintos). */
    @Query("SELECT c.id, c.nombre, cat.categoria, AVG(e.puntajeObtenido), COUNT(e), COUNT(DISTINCT d.id) "
            + "FROM Evaluacion e JOIN e.deportista d JOIN e.criterioEvaluacion c JOIN c.categoria cat "
            + "WHERE (:categoriaId IS NULL OR cat.id = :categoriaId) "
            + "AND (:desde IS NULL OR e.fecha >= :desde) AND (:hasta IS NULL OR e.fecha <= :hasta) "
            + "AND (:grupoId IS NULL OR d.id IN (SELECT m.deportista.id FROM DeportistaPerteneceGrupo m "
            + "WHERE m.grupo.id = :grupoId AND m.fechaFin IS NULL)) "
            + "GROUP BY c.id, c.nombre, cat.categoria ORDER BY cat.categoria, c.nombre")
    List<Object[]> promediosPorCriterio(@Param("grupoId") Integer grupoId, @Param("categoriaId") Integer categoriaId,
            @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);
}
