package com.varcal.cheermanager.repository.Personas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.varcal.cheermanager.Models.Personas.Persona;

@Repository
public interface PersonaRepository extends JpaRepository<Persona, Integer> {

     List<Persona> findByNombre(String nombre);
}