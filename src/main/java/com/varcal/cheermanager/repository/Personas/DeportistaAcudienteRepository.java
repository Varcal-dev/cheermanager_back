package com.varcal.cheermanager.repository.Personas;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Personas.DeportistaAcudiente;

public interface DeportistaAcudienteRepository extends JpaRepository<DeportistaAcudiente, Integer> {

    @Query("SELECT da FROM DeportistaAcudiente da JOIN FETCH da.acudiente WHERE da.deportista.id = :deportistaId")
    List<DeportistaAcudiente> findByDeportistaId(@Param("deportistaId") Integer deportistaId);

    boolean existsByDeportistaIdAndAcudienteId(Integer deportistaId, Integer acudienteId);

    @Query("SELECT COUNT(da) > 0 FROM DeportistaAcudiente da WHERE da.acudiente.id = :acudienteId "
            + "AND da.deportista.persona.id = :personaId")
    boolean acudienteDePersona(@Param("acudienteId") Integer acudienteId, @Param("personaId") Integer personaId);

    @Query("SELECT da.deportista FROM DeportistaAcudiente da WHERE da.acudiente.id = :acudienteId")
    List<com.varcal.cheermanager.Models.Personas.Deportista> deportistasDeAcudiente(@Param("acudienteId") Integer acudienteId);

    long countByAcudienteId(Integer acudienteId);

    void deleteByAcudienteId(Integer acudienteId);

    /** Deportistas nacidos despues de {@code limite} (menores) que no tienen ningun acudiente. */
    @Query("SELECT COUNT(d) FROM Deportista d WHERE d.persona.fechaNacimiento > :limite "
            + "AND NOT EXISTS (SELECT 1 FROM DeportistaAcudiente da WHERE da.deportista = d)")
    long contarMenoresSinAcudiente(@Param("limite") LocalDate limite);
}