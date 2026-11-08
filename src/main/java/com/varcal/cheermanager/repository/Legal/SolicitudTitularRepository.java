
package com.varcal.cheermanager.repository.Legal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Legal.SolicitudTitular;

public interface SolicitudTitularRepository extends JpaRepository<SolicitudTitular, Integer> {
    List<SolicitudTitular> findAllByOrderByFechaRecepcionDescIdDesc();
}
