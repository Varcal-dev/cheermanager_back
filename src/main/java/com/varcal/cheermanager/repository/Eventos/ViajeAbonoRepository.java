package com.varcal.cheermanager.repository.Eventos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Eventos.ViajeAbono;

public interface ViajeAbonoRepository extends JpaRepository<ViajeAbono, Integer> {
    List<ViajeAbono> findByParticipanteIdOrderByIdAsc(Integer participanteId);
}
