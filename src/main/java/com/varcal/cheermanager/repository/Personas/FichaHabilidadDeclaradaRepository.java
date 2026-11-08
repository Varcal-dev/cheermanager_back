package com.varcal.cheermanager.repository.Personas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Personas.FichaHabilidadDeclarada;

public interface FichaHabilidadDeclaradaRepository extends JpaRepository<FichaHabilidadDeclarada, Integer> {
    List<FichaHabilidadDeclarada> findByDeportistaId(Integer deportistaId);
}
