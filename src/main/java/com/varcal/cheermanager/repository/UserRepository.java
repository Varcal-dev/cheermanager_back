package com.varcal.cheermanager.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Auth.Usuario;

import java.util.Optional;

public interface UserRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email);
}