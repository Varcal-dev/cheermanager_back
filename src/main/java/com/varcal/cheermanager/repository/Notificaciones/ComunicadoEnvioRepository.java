package com.varcal.cheermanager.repository.Notificaciones;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Notificaciones.ComunicadoEnvio;

public interface ComunicadoEnvioRepository extends JpaRepository<ComunicadoEnvio, Integer> {
    List<ComunicadoEnvio> findByComunicadoIdOrderByIdAsc(Integer comunicadoId);
}
