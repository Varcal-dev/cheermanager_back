package com.varcal.cheermanager.repository.Financiero;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.varcal.cheermanager.Models.Financiero.DocumentoElectronico;

public interface DocumentoElectronicoRepository extends JpaRepository<DocumentoElectronico, Integer> {
    List<DocumentoElectronico> findTop100ByOrderByIdDesc();

    List<DocumentoElectronico> findByFacturaIdOrderByIdDesc(Integer facturaId);

    Optional<DocumentoElectronico> findByReferencia(String referencia);
}
