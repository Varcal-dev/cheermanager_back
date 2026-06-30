package com.varcal.cheermanager.repository.Org_dep;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Org_dep.DiaSemana;

public interface DiaSemanaRepository extends JpaRepository<DiaSemana, Integer> {
    Optional<DiaSemana> findByDia(String dia);
}