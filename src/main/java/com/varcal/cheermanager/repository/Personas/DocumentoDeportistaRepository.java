package com.varcal.cheermanager.repository.Personas;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Personas.DocumentoDeportista;

public interface DocumentoDeportistaRepository extends JpaRepository<DocumentoDeportista, Integer> {
    List<DocumentoDeportista> findByDeportistaIdOrderByIdDesc(Integer deportistaId);

    List<DocumentoDeportista> findAllByOrderByIdDesc();
}
