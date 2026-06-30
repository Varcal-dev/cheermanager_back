package com.varcal.cheermanager.repository.Evaluaciones.Rutina;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Evaluaciones.Rutina.TablaCantidadNivel;

public interface TablaCantidadNivelRepository extends JpaRepository<TablaCantidadNivel, Integer> {

    List<TablaCantidadNivel> findByNivelId(Integer nivelId);

    // Encuentra la fila cuyo rango [rangoMinAtletas, rangoMaxAtletas] contiene
    // la cantidad real de atletas de la evaluación. Es la consulta central
    // que usan todas las calculadoras de Construcciones y Gimnasia.
    @Query("SELECT t FROM TablaCantidadNivel t WHERE t.nivel.id = :nivelId AND t.tabla = :tabla "
            + "AND :cantidadAtletas BETWEEN t.rangoMinAtletas AND t.rangoMaxAtletas")
    Optional<TablaCantidadNivel> findRangoAplicable(@Param("nivelId") Integer nivelId,
            @Param("tabla") String tabla, @Param("cantidadAtletas") Integer cantidadAtletas);
}