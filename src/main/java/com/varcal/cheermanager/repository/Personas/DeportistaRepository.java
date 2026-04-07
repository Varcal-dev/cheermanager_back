package com.varcal.cheermanager.repository.Personas;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.varcal.cheermanager.Models.Personas.Deportista;

@Repository
public interface DeportistaRepository extends JpaRepository<Deportista, Integer> {

    @Query(value = "SELECT * FROM vista_info_deportistas", nativeQuery = true)
    List<Object[]> obtenerDeportistasConDetalles();

    @Query(value = "SELECT DISTINCT d.* FROM deportistas d " +
            "LEFT JOIN estado_persona ep ON d.estado_id = ep.id " +
            "LEFT JOIN grupo_deportista gg ON d.id = gg.deportista_id " +
            "WHERE (:estadoId IS NULL OR d.estado_id = :estadoId) " +
            "AND (:nivelId IS NULL OR d.nivel_actual_id = :nivelId) " +
            "AND (:grupoId IS NULL OR gg.grupo_entrenamiento_id = :grupoId)",
            nativeQuery = true)
    List<Deportista> findByFiltros(Integer estadoId, Integer nivelId, Integer grupoId);

    @SuppressWarnings("null")
    Optional<Deportista> findById(Integer id);

}