package com.varcal.cheermanager.repository.Horario_Asistencia;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Horario_Asistencia.Sede;

public interface SedeRepository extends JpaRepository<Sede, Integer> {
    List<Sede> findAllByOrderByNombreAsc();
    boolean existsByNombreIgnoreCase(String nombre);
}
