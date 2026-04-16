package com.varcal.cheermanager.repository.Financiero;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import com.varcal.cheermanager.Models.Financiero.Convenio;

@Repository
public interface ConvenioRepository extends JpaRepository<Convenio, Integer> {

    @NonNull
    Optional<Convenio> findById(Integer convenioId);
}
