package com.varcal.cheermanager.repository.Personas;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Personas.FichaDeportiva;

public interface FichaDeportivaRepository extends JpaRepository<FichaDeportiva, Integer> {
    Optional<FichaDeportiva> findByDeportistaId(Integer deportistaId);

    List<FichaDeportiva> findByDeportistaIdIn(Collection<Integer> deportistaIds);
}
