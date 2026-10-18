package com.varcal.cheermanager.repository.Consentimientos;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.varcal.cheermanager.Models.Consentimientos.Consentimiento;
import com.varcal.cheermanager.Models.Consentimientos.Consentimiento.Tipo;
import com.varcal.cheermanager.Models.Personas.Deportista;

public interface ConsentimientoRepository extends JpaRepository<Consentimiento, Integer> {

    boolean existsByTitularIdAndTipoAndFechaRevocadoIsNull(Integer titularId, Tipo tipo);

    boolean existsByAcudienteId(Integer acudienteId);

    List<Consentimiento> findByTitularIdOrderByFechaOtorgadoDesc(Integer titularId);

    /** Deportistas sin un consentimiento vigente (no revocado) de ese tipo. */
    @Query("SELECT d FROM Deportista d JOIN FETCH d.persona WHERE NOT EXISTS ("
            + "SELECT 1 FROM Consentimiento c WHERE c.titular = d.persona AND c.tipo = :tipo "
            + "AND c.fechaRevocado IS NULL)")
    List<Deportista> deportistasSinConsentimiento(@Param("tipo") Tipo tipo);
}
