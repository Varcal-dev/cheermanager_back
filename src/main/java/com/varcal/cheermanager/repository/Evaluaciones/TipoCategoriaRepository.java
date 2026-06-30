package com.varcal.cheermanager.repository.Evaluaciones;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.TipoCategoria;

public interface TipoCategoriaRepository extends JpaRepository<TipoCategoria, Integer> {
    Optional<TipoCategoria> findByCategoria(String categoria);
}