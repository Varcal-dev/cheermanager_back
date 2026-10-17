package com.varcal.cheermanager.repository.Personas;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.varcal.cheermanager.Models.Personas.Deportista;

@Repository
public interface DeportistaRepository extends JpaRepository<Deportista, Integer> {

    @Query(value = "SELECT d.id, p.id, p.nombre, p.apellidos, p.direccion, p.telefono, " +
            "p.fecha_nacimiento, p.genero_id, g.genero, " +
            "d.altura, d.peso, d.fecha_inscripcion, d.contacto_emergencia, " +
            "d.estado_id, ep.estado, " +
            "d.nivel_actual_id, n.nombre, " +
            "d.convenio_id, c.nombre_empresa " +
            "FROM deportistas d " +
            "LEFT JOIN personas p ON p.id = d.persona_id " +
            "LEFT JOIN generos g ON g.id = p.genero_id " +
            "LEFT JOIN estados_persona ep ON ep.id = d.estado_id " +
            "LEFT JOIN niveles n ON n.id = d.nivel_actual_id AND n.club_id = d.club_id " +
            "LEFT JOIN convenios c ON c.id = d.convenio_id AND c.club_id = d.club_id " +
            "WHERE d.club_id = :#{T(com.varcal.cheermanager.tenant.TenantContext).require()}", nativeQuery = true)
    List<Object[]> obtenerDeportistasConDetalles();

    @Query(value = "SELECT DISTINCT d.* FROM deportistas d " +
            "LEFT JOIN estados_persona ep ON d.estado_id = ep.id " +
            "LEFT JOIN grupo_deportista gg ON d.id = gg.deportista_id " +
            "WHERE d.club_id = :#{T(com.varcal.cheermanager.tenant.TenantContext).require()} " +
            "AND (:estadoId IS NULL OR d.estado_id = :estadoId) " +
            "AND (:nivelId IS NULL OR d.nivel_actual_id = :nivelId) " +
            "AND (:grupoId IS NULL OR gg.grupo_entrenamiento_id = :grupoId)", nativeQuery = true)
    List<Deportista> findByFiltros(Integer estadoId, Integer nivelId, Integer grupoId);

    @NonNull
    Optional<Deportista> findById(Integer id);

}
