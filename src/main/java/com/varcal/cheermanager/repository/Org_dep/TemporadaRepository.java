package com.varcal.cheermanager.repository.Org_dep;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Org_dep.Temporada;

public interface TemporadaRepository extends JpaRepository<Temporada, Integer> {
    Optional<Temporada> findFirstByActivaTrue();

    List<Temporada> findByActivaTrue();

    boolean existsByNombre(String nombre);

    List<Temporada> findAllByOrderByFechaInicioDesc();
}
