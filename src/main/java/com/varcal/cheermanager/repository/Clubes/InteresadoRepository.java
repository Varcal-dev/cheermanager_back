package com.varcal.cheermanager.repository.Clubes;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Clubes.Interesado;

public interface InteresadoRepository extends JpaRepository<Interesado, Integer> {
    List<Interesado> findAllByOrderByFechaCreacionDescIdDesc();
    boolean existsByContactoCorreoIgnoreCaseAndDeportistaNombreIgnoreCase(String correo, String deportista);
}
