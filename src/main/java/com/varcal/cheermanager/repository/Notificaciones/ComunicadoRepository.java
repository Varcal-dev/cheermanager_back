package com.varcal.cheermanager.repository.Notificaciones;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Notificaciones.Comunicado;

public interface ComunicadoRepository extends JpaRepository<Comunicado, Integer> {
    List<Comunicado> findTop50ByOrderByFechaCreacionDescIdDesc();
}
