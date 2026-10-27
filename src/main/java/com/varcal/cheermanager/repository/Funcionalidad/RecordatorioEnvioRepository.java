package com.varcal.cheermanager.repository.Funcionalidad;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Funcionalidad.RecordatorioEnvio;

public interface RecordatorioEnvioRepository extends JpaRepository<RecordatorioEnvio, Integer> {

    /** Ultimo aviso ENVIADO por factura y canal: filas [facturaId, canal, ultimaFecha]. */
    @Query("SELECT e.facturaId, e.canal, MAX(e.fecha) FROM RecordatorioEnvio e "
            + "WHERE e.estado = 'ENVIADO' AND e.facturaId IN :facturaIds GROUP BY e.facturaId, e.canal")
    List<Object[]> ultimosEnvios(@Param("facturaIds") Collection<Integer> facturaIds);

    List<RecordatorioEnvio> findAllByOrderByFechaDesc(Pageable pageable);

    long countByFechaAfter(LocalDateTime desde);
}
