package com.varcal.cheermanager.repository.Horario_Asistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Horario_Asistencia.Espacio;

public interface EspacioRepository extends JpaRepository<Espacio, Integer> {
    List<Espacio> findBySedeIdOrderByNombreAsc(Integer sedeId);
    boolean existsBySedeIdAndNombreIgnoreCase(Integer sedeId, String nombre);
}
