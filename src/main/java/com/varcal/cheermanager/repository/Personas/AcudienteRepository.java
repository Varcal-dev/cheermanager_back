package com.varcal.cheermanager.repository.Personas;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Personas.Acudiente;

public interface AcudienteRepository extends JpaRepository<Acudiente, Integer> {
    Optional<Acudiente> findByNumeroDocumento(String numeroDocumento);

    Optional<Acudiente> findByUsuarioId(Integer usuarioId);
}