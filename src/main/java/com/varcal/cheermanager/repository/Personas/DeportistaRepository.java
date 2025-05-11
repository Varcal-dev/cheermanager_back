package com.varcal.cheermanager.repository.Personas;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.varcal.cheermanager.Models.Personas.Deportista;

@Repository
public interface DeportistaRepository extends JpaRepository<Deportista, Integer> {

    @Query(value = "SELECT * FROM vista_info_deportistas", nativeQuery = true)
    List<Object[]> obtenerDeportistasConDetalles();

}