package com.varcal.cheermanager.repository.Clubes;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Clubes.Club;

public interface ClubRepository extends JpaRepository<Club, Integer> {
    Optional<Club> findBySlug(String slug);
    boolean existsBySlug(String slug);
    boolean existsByIdAndActivoTrue(Integer id);
}
