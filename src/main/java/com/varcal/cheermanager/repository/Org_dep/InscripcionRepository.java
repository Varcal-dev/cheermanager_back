package com.varcal.cheermanager.repository.Org_dep;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.varcal.cheermanager.Models.Org_dep.Inscripcion;

@Repository
public interface InscripcionRepository extends JpaRepository<Inscripcion, Integer> {
    List<Inscripcion> findByDeportistaId(Long deportistaId);

    @Query(value = "SELECT * FROM vista_inscripciones_detalle", nativeQuery = true)
    List<Object[]> obtenerVistaInscripcionesDetalle();

    @Query(value = "SELECT * FROM vista_deportistas_no_inscritos", nativeQuery = true)
    List<Object[]> obtenerVistaDeportistasNoInscritos();

}
