package com.varcal.cheermanager.repository.Evaluaciones.Habilidades;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Evaluaciones.Habilidades.HabilidadCatalogo;

public interface HabilidadCatalogoRepository extends JpaRepository<HabilidadCatalogo, Integer> {
    List<HabilidadCatalogo> findByActivoTrueOrderById();
}
